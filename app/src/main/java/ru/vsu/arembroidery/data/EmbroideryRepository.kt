package ru.vsu.arembroidery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ru.vsu.arembroidery.models.DesignItem
import ru.vsu.arembroidery.models.dto.DesignTagResponse
import ru.vsu.arembroidery.network.ApiService
import java.io.File

class EmbroideryRepository(
    private val dataStore: DataStore<Preferences>,
    private val apiService: ApiService
) {
    companion object{
        private const val EMBROIDERY_FILENAME = "embroidery.tmp"
        private val DESIGN_ID = intPreferencesKey("DESIGN_ID")
    }

    fun saveEmbroidery(context: Context, bitmap: Bitmap, designId: Int) {
        getOrCreateEmbroideryFile(context).outputStream().also {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.close()
        }

        runBlocking(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[DESIGN_ID] = designId
            }
        }
    }

    fun loadEmbroidery(context: Context): Bitmap?  = getEmbroideryFileOrNull(context)?.let { file ->
        BitmapFactory.decodeStream(file.inputStream())
    }

    private suspend fun getDesignId() = dataStore.data.firstOrNull()?.get(DESIGN_ID)

    suspend fun getDesignTags(): List<DesignTagResponse> {
        val designId = getDesignId()

        designId?.let { id ->
            val response = apiService.getDesignTags(id)

            if (response.isSuccessful){
                return response.body() ?: emptyList()
            }
        }
        return emptyList()
    }

    private fun getOrCreateEmbroideryFile(context: Context): File = getEmbroideryFileOrNull(context) ?: File(context.filesDir,EMBROIDERY_FILENAME)

    private fun getEmbroideryFileOrNull(context: Context): File? = File(context.filesDir, EMBROIDERY_FILENAME).takeIf { it.exists() }
}