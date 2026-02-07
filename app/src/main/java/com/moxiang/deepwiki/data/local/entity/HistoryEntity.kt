package com.moxiang.deepwiki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val repoName: String,
    val description: String,
    val stars: Int = 0,
    val lastVisited: Long = System.currentTimeMillis()
)

