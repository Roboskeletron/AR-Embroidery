package ru.vsu.arembroidery.domain

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.TransformUtils.getRectToRect
import androidx.camera.core.impl.utils.TransformUtils.rotateRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.function.Consumer

class PoseDetectionAnalyzer(
    private val poseDetector: PoseDetector,
    private val executor: Executor,
    private val consumer: Consumer<PoseAnalysisResult>
) : ImageAnalysis.Analyzer {
    private var mSensorToTarget: Matrix? = null

    @SuppressLint("RestrictedApi")
    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image

        if (mediaImage == null) {
            image.close()
            return
        }

        val analysisToTarget = Matrix()

        val sensorToAnalysis = Matrix(image.imageInfo.sensorToBufferTransformMatrix)
        val sourceRect = RectF(0f, 0f, image.width.toFloat(), image.height.toFloat())
        val bufferRect = rotateRect(sourceRect, image.imageInfo.rotationDegrees)
        val analysisToMlKitRotation = getRectToRect(
            sourceRect, bufferRect,
            image.imageInfo.rotationDegrees
        )
        sensorToAnalysis.postConcat(analysisToMlKitRotation)
        sensorToAnalysis.invert(analysisToTarget)
        analysisToTarget.postConcat(mSensorToTarget)

        poseDetector.process(mediaImage, image.imageInfo.rotationDegrees, analysisToTarget)
            .addOnSuccessListener { pose ->
                executor.execute {
                    consumer.accept(PoseAnalysisResult(pose,analysisToTarget, image.width, image.height))
                }
            }
            .addOnCompleteListener {
                image.close()
            }
    }

    override fun updateTransform(matrix: Matrix?) {
        super.updateTransform(matrix)
        mSensorToTarget = matrix
    }

    override fun getTargetCoordinateSystem(): Int {
        return COORDINATE_SYSTEM_VIEW_REFERENCED
    }

    private fun Image.toBitmap(rotationDegrees: Int) : Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize).apply {
            yBuffer.get(this, 0, ySize)
            vBuffer.get(this, ySize, vSize)
            uBuffer.get(this, ySize + vSize, uSize)
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val rawBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())

        if (rotationDegrees == 0) return rawBitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(
            rawBitmap, 0, 0,
            rawBitmap.width, rawBitmap.height,
            matrix, true
        )
    }
}