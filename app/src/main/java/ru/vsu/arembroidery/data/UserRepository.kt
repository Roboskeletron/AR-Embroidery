package ru.vsu.arembroidery.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.vsu.arembroidery.models.User
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.UserRegistrationRequest
import ru.vsu.arembroidery.network.ApiResult
import ru.vsu.arembroidery.network.ApiService

class UserRepository(private val apiService: ApiService) {

    suspend fun registerUser(
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        password: String,
        roleId: Int
    ): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val request = UserRegistrationRequest(
                username = username,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                roleId = roleId
            )

            val response = apiService.registerUser(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val user = User(
                        id = responseBody.data.id,
                        username = responseBody.data.username,
                        firstName = responseBody.data.firstName,
                        lastName = responseBody.data.lastName,
                        phoneNumber = responseBody.data.phoneNumber,
                        email = responseBody.data.email,
                        password = "", // Don't store password
                        roleId = responseBody.data.roleId
                    )
                    ApiResult.Success(user)
                } else {
                    ApiResult.Error("Registration failed: Empty response")
                }
            } else {
                ApiResult.Error("Registration failed: ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Registration failed: ${e.message}")
        }
    }

    suspend fun loginUser(email: String, password: String): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email = email, password = password)
            val response = apiService.loginUser(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val user = User(
                        id = responseBody.data.id,
                        username = responseBody.data.username,
                        firstName = responseBody.data.firstName,
                        lastName = responseBody.data.lastName,
                        phoneNumber = responseBody.data.phoneNumber,
                        email = responseBody.data.email,
                        password = "", // Don't store password
                        roleId = responseBody.data.roleId
                    )
                    ApiResult.Success(user)
                } else {
                    ApiResult.Error("Login failed: Empty response")
                }
            } else {
                ApiResult.Error("Login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Login failed: ${e.message}")
        }
    }
}