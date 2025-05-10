package ru.vsu.arembroidery.domain

import android.graphics.PointF
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.hypot
import kotlin.math.min

data class Body(
    val leftShoulder: PointF,
    val rightShoulder: PointF,
    val leftHip: PointF,
    val rightHip: PointF
) {

    val bodyCenter by lazy {
        val shouldersCenter = PointF(
            (leftShoulder.x + rightShoulder.x) / 2f,
            (leftShoulder.y + rightShoulder.y) / 2f
        )

        val hipsCenter = PointF(
            (leftHip.x + rightHip.x) / 2f,
            (leftHip.y + rightHip.y) / 2f
        )

        PointF(
            (shouldersCenter.x + hipsCenter.x) / 2f,
            (shouldersCenter.y + hipsCenter.y) / 2f
        )
    }

    /**
     * Calculates four points that lie on the vectors from the original bodyCenter
     * to the original body vertices, scaled such that they form a rectangle
     * with the specified target width and height, centered at a given offset
     * from the original bodyCenter. Uses standard Kotlin/Android PointF.
     *
     * This method calculates a separate scaling factor for each vector based on
     * the desired distance from the target center to the corresponding corner
     * of the target rectangle.
     *
     * @param targetWidth The desired width of the resulting rectangle.
     * @param targetHeight The desired height of the resulting rectangle.
     * @param offsetX The horizontal offset for the center of the rectangle from bodyCenter.
     * @param offsetY The vertical offset for the center of the rectangle from bodyCenter.
     * @return A list of four PointF objects: [topLeft, topRight, bottomRight, bottomLeft],
     *         corresponding to scaled and offset left shoulder, right shoulder, right hip, and left hip respectively.
     */
    fun getScaledAndOffsetBodyPoints(
        targetWidth: Float,
        targetHeight: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): List<PointF> {
        // 1. Определяем целевой центр смещенного прямоугольника
        val targetCenterX = bodyCenter.x + offsetX
        val targetCenterY = bodyCenter.y + offsetY

        // 2. Определяем координаты углов целевого прямоугольника относительно его ЦЕЛЕВОГО центра
        // Порядок: TopLeft, TopRight, BottomRight, BottomLeft
        val targetCornersRelative = listOf(
            PointF(-targetWidth / 2f, -targetHeight / 2f),
            PointF(targetWidth / 2f, -targetHeight / 2f),
            PointF(targetWidth / 2f, targetHeight / 2f),
            PointF(-targetWidth / 2f, targetHeight / 2f)
        )

        // 3. Определяем исходные векторы от bodyCenter до вершин
        // Порядок: LeftShoulder, RightShoulder, RightHip, LeftHip
        val originalVectors = listOf(
            PointF(bodyCenter.x - leftShoulder.x, bodyCenter.y - leftShoulder.y),
            PointF(bodyCenter.x - rightShoulder.x, bodyCenter.y - rightShoulder.y),
            PointF(bodyCenter.x - rightHip.x, bodyCenter.y - rightHip.y),
            PointF(bodyCenter.x -leftHip.x, bodyCenter.y - leftHip.y)
        )

        // 4. Вычисляем ИСХОДНЫЕ длины векторов
        val originalLengths = originalVectors.map { hypot(it.x, it.y) }

        // 5. Вычисляем ЖЕЛАЕМЫЕ длины векторов (расстояние от ЦЕЛЕВОГО центра до углов ЦЕЛЕВОГО прямоугольника)
        val desiredLengths = targetCornersRelative.map { hypot(it.x, it.y) }

        // 6. Вычисляем индивидуальные коэффициенты масштабирования для каждого вектора
        val scales = originalLengths.zip(desiredLengths).map { (originalLen, desiredLen) ->
            if (originalLen > 1e-6) desiredLen / originalLen else 0f // Избегаем деления на ноль
        }

        // 7. Масштабируем исходные векторы
        val scaledVectors = originalVectors.zip(scales).map { (vector, scale) ->
            PointF(vector.x * scale, vector.y * scale)
        }

        // 8. Вычисляем координаты новых точек, добавляя масштабированные векторы к ЦЕЛЕВОМУ центру
        val scaledAndOffsetPoints = scaledVectors.map { scaledVector ->
            PointF(targetCenterX + scaledVector.x, targetCenterY + scaledVector.y)
        }

        return scaledAndOffsetPoints
    }
}

fun createBodyOrNull(
    pose: Pose
): Body? {
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

    if (anyNull(leftShoulder, rightShoulder, leftHip, rightHip)) return null
    if (anyLowConfidence(leftShoulder!!, rightShoulder!!, leftHip!!, rightHip!!)) return null

    return Body(
        leftShoulder = leftShoulder.position,
        rightShoulder = rightShoulder.position,
        leftHip = leftHip.position,
        rightHip = rightHip.position,
    )
}

private fun anyLowConfidence(vararg landmarks: PoseLandmark) =
    landmarks.any { it.inFrameLikelihood < 0.7f }

private fun anyNull(vararg landmarks: PoseLandmark?) =
    landmarks.any { it == null }
