package ru.vsu.arembroidery.views

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import org.opencv.imgproc.Imgproc
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.FragmentTryOnBinding

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

        viewModel.pose.observe(viewLifecycleOwner) { pose ->
            pose?.let {
                updateEmbroideryOverlay(
                    it,
                    viewModel.imageDimensions.value!!.first,
                    viewModel.imageDimensions.value!!.second
                )
            }
        }

        viewModel.imageDimensions.observe (viewLifecycleOwner) {
            warpedEmbroideryMat?.release()
            if (it.first != warpedEmbroideryMat?.cols() ||
                it.second != warpedEmbroideryMat?.rows()) {
                warpedEmbroideryMat = Mat(it.second, it.first, CvType.CV_8UC4, Scalar(0.0, 0.0, 0.0, 0.0))
            }
        }

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
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.cameraPreview.surfaceProvider
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, viewModel.imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun updateEmbroideryOverlay(pose: Pose, width: Int, height: Int){
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            binding.embroideryOverlay.visibility = View.GONE
            return
        }

        binding.embroideryOverlay.visibility = View.VISIBLE

        srcPoints.put(0, 0, 0.0, 0.0)
        srcPoints.put(2, 0, 0.0, embroideryMat.height().toDouble())
        srcPoints.put(1, 0, embroideryMat.width().toDouble(), 0.0)
        srcPoints.put(3, 0, embroideryMat.width().toDouble(), embroideryMat.height().toDouble())

        dstPoints.put(0, 0, leftShoulder.position.x.toDouble(), leftShoulder.position.y.toDouble())
        dstPoints.put(1, 0, rightShoulder.position.x.toDouble(), rightShoulder.position.y.toDouble())
        dstPoints.put(2, 0, leftHip.position.x.toDouble(), leftHip.position.y.toDouble())
        dstPoints.put(3, 0, rightHip.position.x.toDouble(), rightHip.position.y.toDouble())

        val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        Imgproc.warpPerspective(
            embroideryMat,
            warpedEmbroideryMat,
            transformMatrix,
            warpedEmbroideryMat!!.size(),
            Imgproc.INTER_LINEAR
        )

        val resultBitmap = createBitmap(width, height)
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