package ru.vsu.arembroidery.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.vsu.arembroidery.models.User
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.UserRegistrationRequest
import ru.vsu.arembroidery.network.ApiService

class UserRepository(private val apiService: ApiService) {

    suspend fun registerUser(
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        roleId: Int
    ): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val request = UserRegistrationRequest(
                username = username,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
                roleId = roleId
            )

            val response = apiService.registerUser(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    User(
                        id = responseBody.data.id,
                        username = responseBody.data.username,
                        firstName = responseBody.data.firstName,
                        lastName = responseBody.data.lastName,
                        phoneNumber = responseBody.data.phoneNumber,
                        email = responseBody.data.email,
                        password = "", // Don't store password
                        roleId = responseBody.data.roleId
                    )
                } else {
                    throw Exception("Registration failed: Empty response")
                }
            } else {
                throw Exception("Registration failed: ${response.message()}")
            }
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val request = LoginRequest(email = email, password = password)
            val response = apiService.loginUser(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    User(
                        id = responseBody.data.id,
                        username = responseBody.data.username,
                        firstName = responseBody.data.firstName,
                        lastName = responseBody.data.lastName,
                        phoneNumber = responseBody.data.phoneNumber,
                        email = responseBody.data.email,
                        password = "", // Don't store password
                        roleId = responseBody.data.roleId
                    )
                } else {
                    throw Exception("Login failed: Empty response")
                }
            } else {
                throw Exception("Login failed: ${response.message()}")
            }
        }
    }
}