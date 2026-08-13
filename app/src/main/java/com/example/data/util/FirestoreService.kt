package com.example.data.util

import com.example.data.config.FirebaseConfig
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RequestMessageEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FirestoreService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(config: FirebaseConfig): String {
        return "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents"
    }

    suspend fun fetchAllBooksFromFirestore(config: FirebaseConfig = FirebaseConfig()): List<BookEntity> = withContext(Dispatchers.IO) {
        val books = mutableListOf<BookEntity>()
        try {
            val url = "${getBaseUrl(config)}/books?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val id = getStringField(fields, "id").ifBlank {
                                docObj.optString("name").substringAfterLast("/")
                            }
                            val title = getStringField(fields, "title")
                            val author = getStringField(fields, "author")
                            val category = getStringField(fields, "category").ifBlank { "Ficção" }
                            val description = getStringField(fields, "description")
                            val coverUrl = getStringField(fields, "coverUrl")
                            val pdfUrl = getStringField(fields, "pdfUrl")
                            val directPdfUrl = getStringField(fields, "directPdfUrl").ifBlank {
                                DriveUrlConverter.convertToDirectDownloadLink(pdfUrl)
                            }
                            val ratingAverage = getDoubleField(fields, "ratingAverage", 5.0)
                            val ratingCount = getIntField(fields, "ratingCount", 1)
                            val viewsCount = getIntField(fields, "viewsCount", 0)

                            if (id.isNotBlank() && title.isNotBlank()) {
                                books.add(
                                    BookEntity(
                                        id = id,
                                        title = title,
                                        author = author,
                                        category = category,
                                        description = description,
                                        coverUrl = coverUrl,
                                        pdfUrl = pdfUrl,
                                        directPdfUrl = directPdfUrl,
                                        ratingAverage = ratingAverage,
                                        ratingCount = ratingCount,
                                        viewsCount = viewsCount
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        books
    }

    suspend fun saveBookToFirestore(book: BookEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/books/${book.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(book.id))
                put("title", createStringValue(book.title))
                put("author", createStringValue(book.author))
                put("category", createStringValue(book.category))
                put("description", createStringValue(book.description))
                put("coverUrl", createStringValue(book.coverUrl))
                put("pdfUrl", createStringValue(book.pdfUrl))
                put("directPdfUrl", createStringValue(book.directPdfUrl))
                put("ratingAverage", createDoubleValue(book.ratingAverage))
                put("ratingCount", createIntValue(book.ratingCount))
                put("viewsCount", createIntValue(book.viewsCount))
                put("addedAt", createIntValue(book.addedAt.toInt()))
            }

            val bodyJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteBookFromFirestore(bookId: String, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/books/$bookId?key=${config.apiKey}"
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchReviewsFromFirestore(bookId: String, config: FirebaseConfig = FirebaseConfig()): List<ReviewEntity> = withContext(Dispatchers.IO) {
        val reviews = mutableListOf<ReviewEntity>()
        try {
            val url = "${getBaseUrl(config)}/reviews?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val revBookId = getStringField(fields, "bookId")
                            if (revBookId == bookId) {
                                val id = getStringField(fields, "id").ifBlank { docObj.optString("name").substringAfterLast("/") }
                                val userId = getStringField(fields, "userId")
                                val userName = getStringField(fields, "userName")
                                val rating = getIntField(fields, "rating", 5)
                                val comment = getStringField(fields, "comment")

                                reviews.add(
                                    ReviewEntity(
                                        id = id,
                                        bookId = bookId,
                                        userId = userId,
                                        userName = userName,
                                        rating = rating,
                                        comment = comment
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        reviews
    }

    suspend fun saveReviewToFirestore(review: ReviewEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/reviews/${review.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(review.id))
                put("bookId", createStringValue(review.bookId))
                put("userId", createStringValue(review.userId))
                put("userName", createStringValue(review.userName))
                put("rating", createIntValue(review.rating))
                put("comment", createStringValue(review.comment))
                put("createdAt", createIntValue(review.timestamp.toInt()))
            }

            val bodyJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class UserWithPass(val user: UserEntity, val pass: String)

    suspend fun saveUserToFirestore(user: UserEntity, pass: String = "", config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/users/${user.uid}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("uid", createStringValue(user.uid))
                put("name", createStringValue(user.name))
                put("email", createStringValue(user.email.lowercase().trim()))
                put("role", createStringValue(user.role))
                put("password", createStringValue(pass))
                put("createdAt", createIntValue(user.createdAt.toInt()))
            }

            val bodyJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllUsersFromFirestore(config: FirebaseConfig = FirebaseConfig()): List<UserWithPass> = withContext(Dispatchers.IO) {
        val usersList = mutableListOf<UserWithPass>()
        try {
            val url = "${getBaseUrl(config)}/users?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val uid = getStringField(fields, "uid").ifBlank { docObj.optString("name").substringAfterLast("/") }
                            val name = getStringField(fields, "name")
                            val email = getStringField(fields, "email").lowercase().trim()
                            val role = getStringField(fields, "role").ifBlank { "reader" }
                            val pass = getStringField(fields, "password")
                            val createdAt = getIntField(fields, "createdAt", 0).toLong()

                            if (email.isNotBlank()) {
                                usersList.add(
                                    UserWithPass(
                                        user = UserEntity(
                                            uid = uid,
                                            name = name,
                                            email = email,
                                            role = role,
                                            createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis()
                                        ),
                                        pass = pass
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        usersList
    }

    suspend fun saveCategoryToFirestore(category: CategoryEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/categories/${category.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(category.id))
                put("name", createStringValue(category.name))
                put("isVisibleOnHome", JSONObject().put("booleanValue", category.isVisibleOnHome))
                put("createdAt", createIntValue(category.createdAt.toInt()))
            }

            val bodyJson = JSONObject().apply {
                put("fields", fieldsJson)
            }

            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllCategoriesFromFirestore(config: FirebaseConfig = FirebaseConfig()): List<CategoryEntity> = withContext(Dispatchers.IO) {
        val categoriesList = mutableListOf<CategoryEntity>()
        try {
            val url = "${getBaseUrl(config)}/categories?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val id = getStringField(fields, "id").ifBlank { docObj.optString("name").substringAfterLast("/") }
                            val name = getStringField(fields, "name")
                            val isVisible = fields.optJSONObject("isVisibleOnHome")?.optBoolean("booleanValue", true) ?: true
                            val createdAt = getIntField(fields, "createdAt", 0).toLong()

                            if (id.isNotBlank() && name.isNotBlank()) {
                                categoriesList.add(
                                    CategoryEntity(
                                        id = id,
                                        name = name,
                                        isVisibleOnHome = isVisible,
                                        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        categoriesList
    }

    suspend fun deleteCategoryFromFirestore(categoryId: String, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/categories/$categoryId?key=${config.apiKey}"
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SOLICITAÇÕES DE LIVROS ---
    suspend fun saveBookRequestToFirestore(requestObj: BookRequestEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/book_requests/${requestObj.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(requestObj.id))
                put("userId", createStringValue(requestObj.userId))
                put("userName", createStringValue(requestObj.userName))
                put("userEmail", createStringValue(requestObj.userEmail))
                put("title", createStringValue(requestObj.title))
                put("author", createStringValue(requestObj.author))
                put("year", createStringValue(requestObj.year))
                put("message", createStringValue(requestObj.message))
                put("status", createStringValue(requestObj.status))
                put("createdAt", createIntValue(requestObj.createdAt.toInt()))
                put("updatedAt", createIntValue(requestObj.updatedAt.toInt()))
            }
            val bodyJson = JSONObject().put("fields", fieldsJson)
            val request = Request.Builder().url(url).patch(bodyJson.toString().toRequestBody(jsonMediaType)).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllBookRequestsFromFirestore(config: FirebaseConfig = FirebaseConfig()): List<BookRequestEntity> = withContext(Dispatchers.IO) {
        val requestsList = mutableListOf<BookRequestEntity>()
        try {
            val url = "${getBaseUrl(config)}/book_requests?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val id = getStringField(fields, "id").ifBlank { docObj.optString("name").substringAfterLast("/") }
                            val userId = getStringField(fields, "userId")
                            val userName = getStringField(fields, "userName")
                            val userEmail = getStringField(fields, "userEmail")
                            val title = getStringField(fields, "title")
                            val author = getStringField(fields, "author")
                            val year = getStringField(fields, "year")
                            val message = getStringField(fields, "message")
                            val status = getStringField(fields, "status").ifBlank { "PENDING" }
                            val createdAt = getIntField(fields, "createdAt", 0).toLong()
                            val updatedAt = getIntField(fields, "updatedAt", 0).toLong()

                            if (id.isNotBlank() && title.isNotBlank()) {
                                requestsList.add(
                                    BookRequestEntity(
                                        id = id,
                                        userId = userId,
                                        userName = userName,
                                        userEmail = userEmail,
                                        title = title,
                                        author = author,
                                        year = year,
                                        message = message,
                                        status = status,
                                        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
                                        updatedAt = if (updatedAt > 0) updatedAt else System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        requestsList
    }

    // --- MENSAGENS DO MINI CHAT DAS SOLICITAÇÕES ---
    suspend fun saveRequestMessageToFirestore(msg: RequestMessageEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/request_messages/${msg.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(msg.id))
                put("requestId", createStringValue(msg.requestId))
                put("senderId", createStringValue(msg.senderId))
                put("senderName", createStringValue(msg.senderName))
                put("senderRole", createStringValue(msg.senderRole))
                put("message", createStringValue(msg.message))
                put("timestamp", createIntValue(msg.timestamp.toInt()))
            }
            val bodyJson = JSONObject().put("fields", fieldsJson)
            val request = Request.Builder().url(url).patch(bodyJson.toString().toRequestBody(jsonMediaType)).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchMessagesForRequestFromFirestore(requestId: String, config: FirebaseConfig = FirebaseConfig()): List<RequestMessageEntity> = withContext(Dispatchers.IO) {
        val messagesList = mutableListOf<RequestMessageEntity>()
        try {
            val url = "${getBaseUrl(config)}/request_messages?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val reqId = getStringField(fields, "requestId")
                            if (reqId == requestId) {
                                val id = getStringField(fields, "id").ifBlank { docObj.optString("name").substringAfterLast("/") }
                                val senderId = getStringField(fields, "senderId")
                                val senderName = getStringField(fields, "senderName")
                                val senderRole = getStringField(fields, "senderRole")
                                val message = getStringField(fields, "message")
                                val timestamp = getIntField(fields, "timestamp", 0).toLong()

                                messagesList.add(
                                    RequestMessageEntity(
                                        id = id,
                                        requestId = reqId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        senderRole = senderRole,
                                        message = message,
                                        timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        messagesList
    }

    // --- NOTIFICAÇÕES DO APLICATIVO ---
    suspend fun saveNotificationToFirestore(notification: NotificationEntity, config: FirebaseConfig = FirebaseConfig()): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl(config)}/notifications/${notification.id}?key=${config.apiKey}"
            val fieldsJson = JSONObject().apply {
                put("id", createStringValue(notification.id))
                put("userId", createStringValue(notification.userId))
                put("title", createStringValue(notification.title))
                put("message", createStringValue(notification.message))
                put("type", createStringValue(notification.type))
                put("targetId", createStringValue(notification.targetId))
                put("isRead", JSONObject().put("booleanValue", notification.isRead))
                if (notification.readTimestamp != null) {
                    put("readTimestamp", createIntValue(notification.readTimestamp.toInt()))
                }
                put("timestamp", createIntValue(notification.timestamp.toInt()))
            }
            val bodyJson = JSONObject().put("fields", fieldsJson)
            val request = Request.Builder().url(url).patch(bodyJson.toString().toRequestBody(jsonMediaType)).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchNotificationsFromFirestore(userId: String, isAdmin: Boolean = false, config: FirebaseConfig = FirebaseConfig()): List<NotificationEntity> = withContext(Dispatchers.IO) {
        val notificationsList = mutableListOf<NotificationEntity>()
        try {
            val url = "${getBaseUrl(config)}/notifications?key=${config.apiKey}"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyString)
                if (rootJson.has("documents")) {
                    val docsArray = rootJson.getJSONArray("documents")
                    for (i in 0 until docsArray.length()) {
                        val docObj = docsArray.getJSONObject(i)
                        val fields = docObj.optJSONObject("fields")
                        if (fields != null) {
                            val notifUserId = getStringField(fields, "userId")
                            if (notifUserId == userId || notifUserId == "ALL" || (isAdmin && notifUserId == "ADMIN")) {
                                val id = getStringField(fields, "id").ifBlank { docObj.optString("name").substringAfterLast("/") }
                                val title = getStringField(fields, "title")
                                val message = getStringField(fields, "message")
                                val type = getStringField(fields, "type")
                                val targetId = getStringField(fields, "targetId")
                                val isRead = fields.optJSONObject("isRead")?.optBoolean("booleanValue", false) ?: false
                                val readTs = getIntField(fields, "readTimestamp", 0).toLong()
                                val timestamp = getIntField(fields, "timestamp", 0).toLong()

                                notificationsList.add(
                                    NotificationEntity(
                                        id = id,
                                        userId = notifUserId,
                                        title = title,
                                        message = message,
                                        type = type,
                                        targetId = targetId,
                                        isRead = isRead,
                                        readTimestamp = if (readTs > 0) readTs else null,
                                        timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        notificationsList
    }


    // JSON Parsing Helpers for Firestore REST API
    private fun getStringField(fields: JSONObject, key: String): String {
        return fields.optJSONObject(key)?.optString("stringValue", "") ?: ""
    }

    private fun getIntField(fields: JSONObject, key: String, defaultVal: Int = 0): Int {
        val obj = fields.optJSONObject(key) ?: return defaultVal
        val intVal = obj.optInt("integerValue", -1)
        if (intVal != -1) return intVal
        val strVal = obj.optString("stringValue", "")
        return strVal.toIntOrNull() ?: defaultVal
    }

    private fun getDoubleField(fields: JSONObject, key: String, defaultVal: Double = 0.0): Double {
        val obj = fields.optJSONObject(key) ?: return defaultVal
        return obj.optDouble("doubleValue", obj.optDouble("stringValue", defaultVal))
    }

    private fun createStringValue(valStr: String): JSONObject {
        return JSONObject().put("stringValue", valStr)
    }

    private fun createIntValue(valInt: Int): JSONObject {
        return JSONObject().put("integerValue", valInt)
    }

    private fun createDoubleValue(valDbl: Double): JSONObject {
        return JSONObject().put("doubleValue", valDbl)
    }
}
