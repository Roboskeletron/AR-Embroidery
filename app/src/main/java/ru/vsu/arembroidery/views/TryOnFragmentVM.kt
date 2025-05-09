package ru.vsu.arembroidery.views

import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import ru.vsu.arembroidery.analyzers.PoseImageAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TryOnFragmentVM : ViewModel() {
    val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    val cameraExecutor : ExecutorService = Executors.newSingleThreadExecutor()

    val imageAnalyzer = ImageAnalysis.Builder()
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, PoseImageAnalyzer(poseDetector))
        }

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
        cameraExecutor.shutdown()
    }
}