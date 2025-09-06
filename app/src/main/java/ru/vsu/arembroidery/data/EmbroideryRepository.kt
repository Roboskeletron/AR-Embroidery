package ru.vsu.arembroidery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

class EmbroideryRepository {
    companion object{
        const val EMBROIDERY_FILENAME = "embroidery.tmp"
    }

    fun saveEmbroidery(context: Context, bitmap: Bitmap) = getOrCreateEmbroideryFile(context).outputStream().also {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.close()
    }

    fun loadEmbroidery(context: Context): Bitmap?  = getEmbroideryFileOrNull(context)?.let { file ->
        BitmapFactory.decodeStream(file.inputStream())
    }

    private fun getOrCreateEmbroideryFile(context: Context): File = getEmbroideryFileOrNull(context) ?: File(context.filesDir,EMBROIDERY_FILENAME)

    private fun getEmbroideryFileOrNull(context: Context): File? = File(context.filesDir, EMBROIDERY_FILENAME).takeIf { it.exists() }
}