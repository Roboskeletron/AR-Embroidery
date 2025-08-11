package ru.vsu.arembroidery.models

data class User(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val roleId: Int // 1: ADMIN, 2: DESIGNER, 3: USER
)