package ru.vsu.arembroidery.models.dto

data class UserRegistrationRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val roleId: Int
)