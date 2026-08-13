package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_BOOK_COVER_URL = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop"

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val category: String,
    val description: String,
    val coverUrl: String,
    val pdfUrl: String,
    val directPdfUrl: String,
    val ratingAverage: Double = 4.5,
    val ratingCount: Int = 12,
    val viewsCount: Int = 0,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    val displayCoverUrl: String
        get() = if (coverUrl.isBlank()) DEFAULT_BOOK_COVER_URL else coverUrl
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val role: String = "reader", // "admin" ou "reader"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val lastPage: Int = 1,
    val totalPages: Int = 1,
    val progressPercentage: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isVisibleOnHome: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "book_requests")
data class BookRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val title: String,
    val author: String = "",
    val year: String = "",
    val message: String = "",
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "request_messages")
data class RequestMessageEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String, // "user" or "admin"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String, // "ALL", "ADMIN", or specific user's uid
    val title: String,
    val message: String,
    val type: String, // "NEW_BOOK", "REQUEST_STATUS", "REQUEST_MESSAGE"
    val targetId: String = "",
    val isRead: Boolean = false,
    val readTimestamp: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

