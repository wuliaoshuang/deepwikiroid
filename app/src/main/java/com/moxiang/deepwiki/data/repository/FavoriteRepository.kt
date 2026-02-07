package com.moxiang.deepwiki.data.repository

import com.moxiang.deepwiki.data.local.dao.FavoriteDao
import com.moxiang.deepwiki.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    fun isFavorite(repoName: String): Flow<Boolean> {
        return favoriteDao.isFavorite(repoName)
    }

    suspend fun addFavorite(repoName: String, description: String, stars: Int) {
        val favorite = FavoriteEntity(repoName = repoName, description = description, stars = stars)
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(repoName: String) {
        favoriteDao.deleteFavorite(repoName)
    }
}
