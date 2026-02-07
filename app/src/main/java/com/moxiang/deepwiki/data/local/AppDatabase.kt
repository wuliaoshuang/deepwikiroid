package com.moxiang.deepwiki.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moxiang.deepwiki.data.local.dao.FavoriteDao
import com.moxiang.deepwiki.data.local.dao.HistoryDao
import com.moxiang.deepwiki.data.local.entity.FavoriteEntity
import com.moxiang.deepwiki.data.local.entity.HistoryEntity

@Database(entities = [FavoriteEntity::class, HistoryEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deepwiki_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE favorites ADD COLUMN stars INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS history (
                repoName TEXT NOT NULL PRIMARY KEY,
                description TEXT NOT NULL,
                stars INTEGER NOT NULL DEFAULT 0,
                lastVisited INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
