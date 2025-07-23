package ru.vsu.arembroidery.views

import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import ru.vsu.arembroidery.models.PoseAnalysisResult
import ru.vsu.arembroidery.usecases.CreateWarpedBitmapUseCase
import ru.vsu.arembroidery.usecases.TransformLandMarkUseCase
import kotlin.math.abs

class TryOnFragmentVM(
    private val createWarpedBitmapUseCase: CreateWarpedBitmapUseCase,
    private val transformLandMarkUseCase: TransformLandMarkUseCase
) : ViewModel() {
    companion object {
        const val TAG = "TryOnFragment"
    }

    val embroideryOffsetX = MutableLiveData(0f)
    val embroideryOffsetY = MutableLiveData(0f)
    val previewWidth = MutableLiveData(0)
    val previewHeight = MutableLiveData(0)

    var scale = 0.5

    private var alignmentOffsetX = 0.0

    fun alignCenter(){
        embroideryOffsetX.value = 0f
        embroideryOffsetY.value = 0f
        Log.d(TAG, "Align center")
    }

    fun alignLeft(){
        embroideryOffsetX.value = -alignmentOffsetX.toFloat()
        embroideryOffsetY.value = 0f
        Log.d(TAG, "Align left")
    }

    fun alignRight(){
        embroideryOffsetX.value = alignmentOffsetX.toFloat()
        embroideryOffsetY.value = 0f
        Log.d(TAG, "Align right")
    }

    fun processPoseAnalysisResult(poseAnalysisResult: PoseAnalysisResult) : List<Drawable> {
        adjustAlignmentOffset(poseAnalysisResult.pose, poseAnalysisResult.mappingMatrix)

        return listOfNotNull(
            PoseDebugOverlay(poseAnalysisResult.pose) {
                it.map { transformLandMarkUseCase(it, poseAnalysisResult.mappingMatrix) }
            },
            createWarpedBitmapUseCase(
                poseAnalysisResult.pose,
                poseAnalysisResult.mappingMatrix,
                previewWidth.value ?: 0,
                previewHeight.value ?: 0,
                scale,
                embroideryOffsetX.value?.toDouble() ?: 0.0,
                embroideryOffsetY.value?.toDouble() ?: 0.0
            )?.let { EmbroideryOverlay(it) }
        )
    }

    private fun adjustAlignmentOffset(pose: Pose, mappingMatrix: Matrix) {
        if (pose.allPoseLandmarks.isEmpty()){
            alignmentOffsetX = 0.0
            return
        }
        val leftShoulder = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!, mappingMatrix)
        val rightShoulder = transformLandMarkUseCase(pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!, mappingMatrix)
        alignmentOffsetX = abs(leftShoulder.x - rightShoulder.x) / 2.0
    }
}