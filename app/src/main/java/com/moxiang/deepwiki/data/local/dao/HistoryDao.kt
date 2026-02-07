package com.moxiang.deepwiki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moxiang.deepwiki.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastVisited DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun clearAll(): Int

    @Query("DELETE FROM history WHERE repoName = :repoName")
    suspend fun deleteByRepo(repoName: String): Int
}
