package ru.vsu.arembroidery.views

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.pose.Pose
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding
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
    private var debugMat: Mat? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTryOnBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        srcPoints = Mat(4, 1, CvType.CV_32FC2)
        dstPoints = Mat(4, 1, CvType.CV_32FC2)

        val embroideryBitmap = BitmapFactory.decodeResource(resources, R.drawable.example_texture)
        embroideryMat = Mat(embroideryBitmap.height, embroideryBitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(embroideryBitmap, embroideryMat)

        startCamera()
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
        }
    }

//    private fun Body.updateEmbroideryPosition() {
//        srcPoints.put(0, 0, 0.0, 0.0)
//        srcPoints.put(2, 0, 0.0, embroideryMat.height().toDouble())
//        srcPoints.put(1, 0, embroideryMat.width().toDouble(), 0.0)
//        srcPoints.put(3, 0, embroideryMat.width().toDouble(), embroideryMat.height().toDouble())
//
//        val offsetDstPoints = getScaledAndOffsetBodyPoints(
//            viewModel.embroideryWidth,
//            viewModel.embroideryHeight,
//            viewModel.embroideryCenterOffsetX,
//            viewModel.embroideryCenterOffsetY
//        )
//
//        dstPoints.put(0, 0, offsetDstPoints[0].x.toDouble(), offsetDstPoints[0].y.toDouble())
//        dstPoints.put(1, 0, offsetDstPoints[1].x.toDouble(), offsetDstPoints[1].y.toDouble())
//        dstPoints.put(2, 0, offsetDstPoints[3].x.toDouble(), offsetDstPoints[3].y.toDouble())
//        dstPoints.put(3, 0, offsetDstPoints[2].x.toDouble(), offsetDstPoints[2].y.toDouble())
//
//        val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
//
//        Imgproc.warpPerspective(
//            embroideryMat,
//            warpedEmbroideryMat,
//            transformMatrix,
//            warpedEmbroideryMat!!.size(),
//            Imgproc.INTER_CUBIC
//        )
//
//        val resultBitmap = createBitmap(binding.cameraPreview.width, binding.cameraPreview.height)
//        Utils.matToBitmap(warpedEmbroideryMat, resultBitmap)
//
//        binding.embroideryOverlay.setImageBitmap(resultBitmap)
//
//        transformMatrix.release()
//    }


    override fun onDestroy() {
        super.onDestroy()
        srcPoints.release()
        dstPoints.release()
        warpedEmbroideryMat?.release()
    }
}