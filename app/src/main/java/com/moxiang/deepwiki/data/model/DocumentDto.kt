package com.moxiang.deepwiki.data.model

data class DocumentDto(
    val repoFullName: String,
    val title: String,
    val content: String,
    val htmlUrl: String,
    val menuItems: List<WikiMenuDto> = emptyList()
)
