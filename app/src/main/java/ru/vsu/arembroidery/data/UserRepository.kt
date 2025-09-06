package ru.vsu.arembroidery.data

import retrofit2.HttpException
import ru.vsu.arembroidery.models.dto.LoginRequest
import ru.vsu.arembroidery.models.dto.LoginResponse
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
    ): Result<Int> =
        runCatching {
            val registerRequest = UserRegistrationRequest(
                username = username,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
                roleId = roleId
            )

            val response = apiService.registerUser(registerRequest)

            if (response.isSuccessful) {
                response.body()!!
            }
            else {
                throw HttpException(response)
            }
        }

    suspend fun loginUser(email: String, password: String): Result<LoginResponse> =
        runCatching {
            val loginRequest = LoginRequest(email = email, password = password)
            val response = apiService.loginUser(loginRequest)

            if (response.isSuccessful) {
                response.body()!!
            }
            else {
                throw HttpException(response)
            }
        }
}
