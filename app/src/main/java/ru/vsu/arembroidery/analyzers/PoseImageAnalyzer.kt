package ru.vsu.arembroidery.analyzers

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetector

class PoseImageAnalyzer(
    private val poseDetector: PoseDetector,
    private val  onPoseDetected: (pose: Pose, width: Int, height: Int) -> Unit
) : ImageAnalysis.Analyzer {
    companion object {
        private  const val TAG = "PoseImageAnalyzer"
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                onPoseDetected(pose, imageProxy.width, imageProxy.height)
            }
            .addOnFailureListener { tr ->
                Log.e(TAG, "Failed to detect pose", tr)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}