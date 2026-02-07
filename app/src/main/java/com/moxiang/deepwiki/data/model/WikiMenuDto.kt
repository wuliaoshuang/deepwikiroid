package com.moxiang.deepwiki.data.model

data class WikiMenuDto(
    val title: String,
    val path: String, // Relative path, e.g., "/microsoft/vscode/1-overview"
    val isSelected: Boolean = false,
    val children: List<WikiMenuDto> = emptyList()
)
