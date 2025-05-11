package ru.vsu.arembroidery.domain

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PoseDebugOverlay(
    private val pose: Pose,
    private val transformPoint: (poseLandmarks: List<PoseLandmark>) -> List<PointF> = {
        it.map { poseLandmark ->
            poseLandmark.position
        }
    }
) : Drawable() {
    private val pointPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
    }

    var offsetX = 0f
    var offsetY = 0f


    override fun draw(canvas: Canvas) {
        pose.allPoseLandmarks.run {
            transformPoint(this)
        }.forEach { point ->
            canvas.drawCircle(
                point.x + offsetX,
                point.y + offsetY,
                10f,
                pointPaint
            )
        }
    }

    override fun setAlpha(alpha: Int) {
        pointPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        pointPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}