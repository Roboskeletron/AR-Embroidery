package ru.vsu.arembroidery.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class EmbroideryOverlay(
    private val warpedBitmap: Bitmap
) : Drawable() {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(warpedBitmap, 0f, 0f, null)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}