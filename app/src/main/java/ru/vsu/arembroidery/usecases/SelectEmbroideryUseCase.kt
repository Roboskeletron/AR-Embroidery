package ru.vsu.arembroidery.usecases

import android.content.Context
import android.graphics.Bitmap
import ru.vsu.arembroidery.data.EmbroideryRepository

class SelectEmbroideryUseCase(
    private val embroideryRepository: EmbroideryRepository
) {
    operator fun invoke(context: Context, bitmap: Bitmap?){
        bitmap?.let { bitmap ->
            embroideryRepository.saveEmbroidery(context, bitmap)
        }
    }
}