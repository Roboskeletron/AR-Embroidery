package ru.vsu.arembroidery.views

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_SENSOR
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.pose.Pose
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding
import ru.vsu.arembroidery.domain.Body
import ru.vsu.arembroidery.domain.PoseDebugOverlay
import ru.vsu.arembroidery.domain.createBodyOrNull

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
                MlKitAnalyzer(
                    listOf(viewModel.poseDetector),
                    COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(requireContext())
                ) { result ->
                    result.getValue(viewModel.poseDetector)?.let { pose ->
                        updateEmbroideryOverlay(pose)
                    }
                }
            )

            bindToLifecycle(viewLifecycleOwner)

            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
            previewResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        }
    }

    private fun updateEmbroideryOverlay(pose: Pose){
        binding.cameraPreview.apply {
            overlay.clear()
            overlay.add(PoseDebugOverlay(pose))
        }
//        val body = createBodyOrNull(pose)?.run {
//            updateEmbroideryPosition()
//            drawDebugMarkers()
//        }
//
//        binding.embroideryOverlay.visibility = if (body == null) View.GONE else View.VISIBLE
    }

    private fun Body.updateEmbroideryPosition() {
        srcPoints.put(0, 0, 0.0, 0.0)
        srcPoints.put(2, 0, 0.0, embroideryMat.height().toDouble())
        srcPoints.put(1, 0, embroideryMat.width().toDouble(), 0.0)
        srcPoints.put(3, 0, embroideryMat.width().toDouble(), embroideryMat.height().toDouble())

        val offsetDstPoints = getScaledAndOffsetBodyPoints(
            viewModel.embroideryWidth,
            viewModel.embroideryHeight,
            viewModel.embroideryCenterOffsetX,
            viewModel.embroideryCenterOffsetY
        )

        dstPoints.put(0, 0, offsetDstPoints[0].x.toDouble(), offsetDstPoints[0].y.toDouble())
        dstPoints.put(1, 0, offsetDstPoints[1].x.toDouble(), offsetDstPoints[1].y.toDouble())
        dstPoints.put(2, 0, offsetDstPoints[3].x.toDouble(), offsetDstPoints[3].y.toDouble())
        dstPoints.put(3, 0, offsetDstPoints[2].x.toDouble(), offsetDstPoints[2].y.toDouble())

        val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        Imgproc.warpPerspective(
            embroideryMat,
            warpedEmbroideryMat,
            transformMatrix,
            warpedEmbroideryMat!!.size(),
            Imgproc.INTER_CUBIC
        )

        val resultBitmap = createBitmap(binding.cameraPreview.width, binding.cameraPreview.height)
        Utils.matToBitmap(warpedEmbroideryMat, resultBitmap)

        binding.embroideryOverlay.setImageBitmap(resultBitmap)

        transformMatrix.release()
    }


    override fun onDestroy() {
        super.onDestroy()
        srcPoints.release()
        dstPoints.release()
        warpedEmbroideryMat?.release()
    }
}