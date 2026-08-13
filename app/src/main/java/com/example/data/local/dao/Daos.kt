package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RequestMessageEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDownloaded = 1 ORDER BY addedAt DESC")
    fun getDownloadedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE category = :category")
    suspend fun getBooksByCategory(category: String): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: String)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavoriteStatus(bookId: String, isFavorite: Boolean)

    @Query("UPDATE books SET isDownloaded = :isDownloaded, localFilePath = :localPath WHERE id = :bookId")
    suspend fun updateDownloadStatus(bookId: String, isDownloaded: Boolean, localPath: String?)

    @Query("UPDATE books SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateBookCategories(oldCategory: String, newCategory: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getReviewsForBook(bookId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgressForBook(bookId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observeProgressForBook(bookId: String): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isVisibleOnHome = 1 ORDER BY name ASC")
    fun getHomeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?
}

@Dao
interface BookRequestDao {
    @Query("SELECT * FROM book_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<BookRequestEntity>>

    @Query("SELECT * FROM book_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRequestsForUser(userId: String): Flow<List<BookRequestEntity>>

    @Query("SELECT * FROM book_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: String): BookRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: BookRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<BookRequestEntity>)

    @Query("UPDATE book_requests SET status = :status, updatedAt = :updatedAt WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM book_requests WHERE id = :requestId")
    suspend fun deleteRequest(requestId: String)
}

@Dao
interface RequestMessageDao {
    @Query("SELECT * FROM request_messages WHERE requestId = :requestId ORDER BY timestamp ASC")
    fun getMessagesForRequest(requestId: String): Flow<List<RequestMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RequestMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<RequestMessageEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE (userId = :userId OR userId = 'ALL' OR (:isAdmin = 1 AND userId = 'ADMIN')) ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String, isAdmin: Boolean): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (userId = :userId OR userId = 'ALL' OR (:isAdmin = 1 AND userId = 'ADMIN')) AND isRead = 0")
    fun getUnreadCountForUser(userId: String, isAdmin: Boolean): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1, readTimestamp = :now WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, readTimestamp = :now WHERE (userId = :userId OR userId = 'ALL' OR (:isAdmin = 1 AND userId = 'ADMIN')) AND isRead = 0")
    suspend fun markAllAsRead(userId: String, isAdmin: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notifications WHERE isRead = 1 AND readTimestamp IS NOT NULL AND (:now - readTimestamp) > 120000")
    suspend fun deleteExpiredReadNotifications(now: Long = System.currentTimeMillis())
}

