package ru.vsu.arembroidery.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import ru.vsu.arembroidery.models.DesignItem
import ru.vsu.arembroidery.network.ApiService

class DesignPagingSource(
    private val apiService: ApiService
) : PagingSource<Int, DesignItem>() {
    override fun getRefreshKey(state: PagingState<Int, DesignItem>): Int? {
        return  state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DesignItem> {
        val page = params.key ?: 1
        val designsResponse = apiService.getDesigns(mapOf(
            "page" to page.toString(),
            "size" to params.loadSize.toString()
        ))

        if (!designsResponse.isSuccessful){
            return LoadResult.Error(HttpException(designsResponse))
        }
        val designs = designsResponse.body()

        return LoadResult.Page(
            data = designs?.viewDtoList?.map { (id, name, fileId) -> DesignItem(id, name, fileId) } ?: listOf(),
            prevKey = if (page == 1) null else page - 1,
            nextKey = designs?.pageSize?.let {
                if (it < params.loadSize) null else page + 1
            }
        )
    }
}