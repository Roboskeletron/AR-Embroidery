package ru.vsu.arembroidery.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import ru.vsu.arembroidery.data.DesignPagingSource

class DesignsFragmentVM(
    private val designPagingSource: DesignPagingSource
) : ViewModel() {
    val designsFlow = Pager(
        config = PagingConfig(pageSize =  25, enablePlaceholders = true),
        pagingSourceFactory = {designPagingSource }
    ).flow.cachedIn(viewModelScope)
}