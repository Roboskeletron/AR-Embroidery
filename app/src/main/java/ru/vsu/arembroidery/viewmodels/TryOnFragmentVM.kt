package ru.vsu.arembroidery.viewmodels

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vsu.arembroidery.models.PoseAnalysisResult
import ru.vsu.arembroidery.usecases.CreateWarpedBitmapUseCase
import ru.vsu.arembroidery.usecases.TransformLandMarkUseCase
import ru.vsu.arembroidery.views.EmbroideryOverlay
import ru.vsu.arembroidery.views.PoseDebugOverlay
import java.time.LocalDateTime
import kotlin.math.abs

class TryOnFragmentVM(
    private val createWarpedBitmapUseCase: CreateWarpedBitmapUseCase,
    private val transformLandMarkUseCase: TransformLandMarkUseCase
) : ViewModel() {
    companion object {
        const val TAG = "TryOnFragment"
        const val PICTURE_PREFIX = "EmbroideryTryOn"
        const val PICTURE_RELATIVE_PATH = "DCIM/Embroidery Try On"
    }

    val embroideryOffsetX = MutableLiveData(0f)
    val embroideryOffsetY = MutableLiveData(0f)
    val previewWidth = MutableLiveData(0)
    val previewHeight = MutableLiveData(0)

    var scale = 0.5

    private var alignmentOffsetX = 0.0

    private var overlays: List<Drawable> = listOf()

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
        ).also { overlays = it }
    }

    fun takePicture(contentResolver: ContentResolver, bitmap: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        Canvas(bitmap).also { canvas ->
            overlays.forEach { it.draw(canvas) }
        }

        val picturesCollection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val pictureDetails = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "$PICTURE_PREFIX-${LocalDateTime.now()}.jpg"
            )
            put(MediaStore.Images.Media.RELATIVE_PATH, PICTURE_RELATIVE_PATH)
        }

        contentResolver.insert(picturesCollection, pictureDetails)?.let { uri ->
            contentResolver.openOutputStream(uri)?.apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                close()
            }
        }
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