package com.newsreader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.newsreader.app.data.db.dao.ArticleDao
import com.newsreader.app.data.db.dao.CategoryDao
import com.newsreader.app.data.db.dao.FeedDao
import com.newsreader.app.data.db.entity.ArticleEntity
import com.newsreader.app.data.db.entity.CategoryEntity
import com.newsreader.app.data.db.entity.FeedEntity

@Database(
    entities = [FeedEntity::class, ArticleEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "newsreader_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
