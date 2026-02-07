package com.moxiang.deepwiki.data.model

data class RepositoryDto(
    val fullName: String,
    val description: String,
    val stars: Int,
    val language: String?,
    val url: String
)
