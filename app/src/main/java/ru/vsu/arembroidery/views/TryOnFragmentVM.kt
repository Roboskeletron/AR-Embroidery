package ru.vsu.arembroidery.views

import android.graphics.BitmapFactory
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import ru.vsu.arembroidery.analyzers.PoseImageAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TryOnFragmentVM : ViewModel() {
    private val _pose = MutableLiveData<Pose?>(null)
    val pose: LiveData<Pose?> = _pose

    private val _imageDimensions = MutableLiveData<Pair<Int, Int>>()
    val imageDimensions: LiveData<Pair<Int, Int>> = _imageDimensions

    val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val cameraExecutor : ExecutorService = Executors.newSingleThreadExecutor()

    val imageAnalyzer = ImageAnalysis.Builder()
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, PoseImageAnalyzer(
                poseDetector
            ) { pose, width, height ->
                _imageDimensions.postValue(Pair(width, height))
                _pose.postValue(pose)
            })
        }

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
        cameraExecutor.shutdown()
    }
}