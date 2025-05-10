package ru.vsu.arembroidery.views

import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class TryOnFragmentVM : ViewModel() {
    val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    var embroideryCenterOffsetX = 0f
    var embroideryCenterOffsetY = 0f
    var embroideryWidth = 300f
    var embroideryHeight = 300f

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
    }
}