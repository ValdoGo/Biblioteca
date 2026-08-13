package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.BookRequestDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.ReadingProgressDao
import com.example.data.local.dao.RequestMessageDao
import com.example.data.local.dao.ReviewDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RequestMessageEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        BookEntity::class,
        UserEntity::class,
        ReviewEntity::class,
        ReadingProgressEntity::class,
        CategoryEntity::class,
        BookRequestEntity::class,
        RequestMessageEntity::class,
        NotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun userDao(): UserDao
    abstract fun reviewDao(): ReviewDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bookRequestDao(): BookRequestDao
    abstract fun requestMessageDao(): RequestMessageDao
    abstract fun notificationDao(): NotificationDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biblioteca_digital_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
