package ru.vsu.arembroidery.models.dto

data class LoginRequest(
    val email: String,
    val password: String
)