package ru.vsu.arembroidery.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.LoginResponse
import ru.vsu.arembroidery.models.dto.UserRegistrationRequest

interface ApiService {

    @POST("/api/v1/users/register")
    suspend fun registerUser(@Body request: UserRegistrationRequest): Response<Int>

    @POST("/api/v1/accessToken")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>
}
