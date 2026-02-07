package com.moxiang.deepwiki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val repoName: String,
    val description: String,
    val stars: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
