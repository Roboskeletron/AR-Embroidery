package ru.vsu.arembroidery.usecases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.data.EmbroideryRepository
import ru.vsu.arembroidery.data.MatrixRepository

class LoadEmbroideryUseCase(
    private val embroideryRepository: EmbroideryRepository,
    private val matrixRepository: MatrixRepository
) {
    operator fun invoke(context: Context): Bitmap{
        val bitmap = embroideryRepository.loadEmbroidery(context) ?: BitmapFactory.decodeResource(context.resources, R.drawable.example_texture)

        val embroideryMat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, embroideryMat)
        matrixRepository.updateEmbroideryMat(embroideryMat)

        return bitmap
    }
}