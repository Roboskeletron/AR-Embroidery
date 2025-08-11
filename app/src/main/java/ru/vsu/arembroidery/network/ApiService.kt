package ru.vsu.arembroidery.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.LoginResponse
import ru.vsu.arembroidery.models.dto.UserRegistrationRequest
import ru.vsu.arembroidery.models.dto.UserRegistrationResponse

interface ApiService {
    
    @POST("api/v1/users")
    suspend fun registerUser(@Body request: UserRegistrationRequest): Response<UserRegistrationResponse>
    
    @POST("api/v1/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>
}