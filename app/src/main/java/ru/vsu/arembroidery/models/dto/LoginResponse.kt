package ru.vsu.arembroidery.models.dto

data class LoginResponse(
    val accessToken: String,
    val expiresAt: Int,
    val refreshToken: String,
    val role: String,
    val id: Int
)