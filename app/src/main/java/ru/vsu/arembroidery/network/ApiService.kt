package ru.vsu.arembroidery.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap
import ru.vsu.arembroidery.models.dto.DesignItemResponse
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.LoginResponse
import ru.vsu.arembroidery.models.dto.PaginatedResponse
import ru.vsu.arembroidery.models.dto.UserRegistrationRequest

interface ApiService {

    @POST("/api/v1/users/register")
    suspend fun registerUser(@Body request: UserRegistrationRequest): Response<Int>

    @POST("/api/v1/accessToken")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/v1/designs")
    suspend fun getDesigns(@QueryMap allParams: Map<String, String>): Response<PaginatedResponse<DesignItemResponse>>
}
