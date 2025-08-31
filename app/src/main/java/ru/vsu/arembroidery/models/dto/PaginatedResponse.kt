package ru.vsu.arembroidery.models.dto

data class PaginatedResponse<T>(
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Int,
    val viewDtoList: List<T>
)
