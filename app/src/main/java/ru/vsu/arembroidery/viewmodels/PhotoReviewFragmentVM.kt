package ru.vsu.arembroidery.viewmodels

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PhotoReviewFragmentVM : ViewModel() {
    companion object {
        private const val TAG = "PhotoReviewFragmentVM"
    }

    private val _deleteEvent = Channel<Unit>()
    val deleteEvent = _deleteEvent.receiveAsFlow()

    fun deletePhoto(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.delete(uri, null, null)
                _deleteEvent.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete photo", e)
            }
        }
    }
}