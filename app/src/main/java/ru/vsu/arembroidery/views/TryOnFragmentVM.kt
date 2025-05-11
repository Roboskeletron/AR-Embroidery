package ru.vsu.arembroidery.views

import androidx.lifecycle.MutableLiveData
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

    val embroideryOffsetX = MutableLiveData(0f)
    val embroideryOffsetY = MutableLiveData(0f)
    var embroideryScale = MutableLiveData(50f)

    var offsetX = 0.0
    var offsetY = 0.0
    var scale = 0.5

    var alignmentOffsetX = 0.0

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
    }
}