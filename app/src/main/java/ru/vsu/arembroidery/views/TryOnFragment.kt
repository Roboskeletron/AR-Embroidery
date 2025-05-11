package ru.vsu.arembroidery.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding
import ru.vsu.arembroidery.domain.EmbroideryOverlay
import ru.vsu.arembroidery.domain.PoseDebugOverlay
import ru.vsu.arembroidery.domain.PoseDetectionAnalyzer

class TryOnFragment : Fragment() {

    companion object{
        private const val TAG = "TryOnFragment"
    }

    private lateinit var binding: FragmentTryOnBinding

    private val viewModel by viewModels<TryOnFragmentVM>()

    private var warpedEmbroideryMat: Mat? = null
    private lateinit var srcPoints: Mat
    private lateinit var dstPoints: Mat
    private lateinit var embroideryMat: Mat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTryOnBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadEmbroidery()

        startCamera()
    }

    private fun loadEmbroidery() {
        val embroideryBitmap = BitmapFactory.decodeResource(resources, R.drawable.example_texture)
        embroideryMat = Mat(embroideryBitmap.height, embroideryBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(embroideryBitmap, embroideryMat)

        srcPoints = Mat(4, 1, CvType.CV_32FC2).apply {
            put(0, 0, 0.0, 0.0)
            put(1, 0, embroideryMat.width().toDouble(), 0.0)
            put(2, 0, 0.0, embroideryMat.height().toDouble())
            put(3, 0, embroideryMat.width().toDouble(), embroideryMat.height().toDouble())
        }

        dstPoints = Mat(4, 1, CvType.CV_32FC2)
    }

    private fun startCamera() {
        binding.cameraPreview.controller = LifecycleCameraController(requireContext()).apply {
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(requireContext()),
                PoseDetectionAnalyzer(
                    viewModel.poseDetector,
                    ContextCompat.getMainExecutor(requireContext())
                ) { result ->
                    updateEmbroideryOverlay(result.pose, result.mappingMatrix)
                }
            )

            bindToLifecycle(viewLifecycleOwner)
        }

        binding.cameraPreview.addOnLayoutChangeListener{ layout, _, _, _, _, _, _, _, _ ->
            warpedEmbroideryMat?.release()
            warpedEmbroideryMat = null
            if (layout.width > 0) {
                warpedEmbroideryMat = Mat(
                    layout.height,
                    layout.width,
                    CvType.CV_32FC2,
                    Scalar(0.0, 0.0, 0.0, 0.0)
                )
            }
        }
    }

    private fun updateEmbroideryOverlay(pose: Pose, mappingMatrix: Matrix) {
        binding.cameraPreview.apply {
            overlay.clear()

            overlay.add(PoseDebugOverlay(pose) {
                it.map { poseLandmark ->
                    val points = floatArrayOf(poseLandmark.position.x, poseLandmark.position.y)
                    mappingMatrix.mapPoints(points)
                    PointF(points[0], points[1])
                }
            })

            createWarpedBitmap(pose, mappingMatrix)?.let {
                overlay.add(EmbroideryOverlay(it))
            }
        }
    }

    private fun PoseLandmark.transformPosition(mappingMatrix: Matrix) : PointF {
        val points = floatArrayOf(position.x, position.y)
        mappingMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    private fun createWarpedBitmap(
        pose: Pose,
        mappingMatrix: Matrix
    ) : Bitmap? {
        if (pose.allPoseLandmarks.isEmpty()) return null

        val previewView = binding.cameraPreview

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!.transformPosition(mappingMatrix)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!.transformPosition(mappingMatrix)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)!!.transformPosition(mappingMatrix)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!.transformPosition(mappingMatrix)

        dstPoints.apply {
            put(0, 0, leftShoulder.x.toDouble(), leftShoulder.y.toDouble())
            put(1, 0, rightShoulder.x.toDouble(), rightShoulder.y.toDouble())
            put(2, 0, leftHip.x.toDouble(), leftHip.y.toDouble())
            put(3, 0, rightHip.x.toDouble(), rightHip.y.toDouble())
        }

        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)



        Imgproc.warpPerspective(
            embroideryMat,
            warpedEmbroideryMat,
            perspectiveTransform,
            Size(previewView.width.toDouble(), previewView.height.toDouble())
        )

        return createBitmap(
            warpedEmbroideryMat!!.cols(),
            warpedEmbroideryMat!!.rows()
        ).apply {
            Utils.matToBitmap(warpedEmbroideryMat, this)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        srcPoints.release()
        dstPoints.release()
        warpedEmbroideryMat?.release()
        embroideryMat.release()
    }
}