package ru.vsu.arembroidery.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap
import ru.vsu.arembroidery.models.dto.DesignItemResponse
import ru.vsu.arembroidery.models.dto.DesignTagResponse
import ru.vsu.arembroidery.models.dto.PaginatedResponse

interface ApiService {
    @GET("api/v1/designs")
    suspend fun getDesigns(@QueryMap allParams: Map<String, String>): Response<PaginatedResponse<DesignItemResponse>>

    @GET("api/v1/designs/{id}/tags")
    suspend fun getDesignTags(@Path("id") id: Int) : Response<List<DesignTagResponse>>
}
