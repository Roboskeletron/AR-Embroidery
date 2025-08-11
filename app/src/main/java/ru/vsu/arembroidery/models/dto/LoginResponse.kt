package ru.vsu.arembroidery.models.dto

data class LoginResponse(
    val message: String,
    val data: UserData
)

data class UserData(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val email: String,
    val roleId: Int
)