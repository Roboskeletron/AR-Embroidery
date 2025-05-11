package ru.vsu.arembroidery.views

import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class TryOnFragmentVM : ViewModel() {
    val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
            .build()
    )

    var embroideryOffsetX = 0.0
    var embroideryOffsetY = 0.0
    var embroideryScale = 0.5

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
    }
}