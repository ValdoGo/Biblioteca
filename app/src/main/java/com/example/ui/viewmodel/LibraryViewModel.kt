package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.FirebaseConfig
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RequestMessageEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.LibraryRepository
import com.example.data.util.PdfDownloadManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository(application)

    // Estados de Filtro e Busca
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _firebaseConfig = MutableStateFlow(FirebaseConfig())
    val firebaseConfig: StateFlow<FirebaseConfig> = _firebaseConfig.asStateFlow()

    // Lista de livros do acervo filtrada reativamente
    val booksList: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        _searchQuery,
        _selectedCategory
    ) { books, query, category ->
        books.filter { book ->
            val matchesSearch = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true)
            val matchesCategory = category == "Todos" || book.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteBooks: StateFlow<List<BookEntity>> = repository.favoriteBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedBooks: StateFlow<List<BookEntity>> = repository.downloadedBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsersList: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeCategories: StateFlow<List<CategoryEntity>> = repository.homeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookRequests: StateFlow<List<BookRequestEntity>> = repository.allBookRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUserId = MutableStateFlow<String>("")
    private val _currentUserIsAdmin = MutableStateFlow<Boolean>(false)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val userBookRequests: StateFlow<List<BookRequestEntity>> = _currentUserId
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) repository.getRequestsForUser(uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userNotifications: StateFlow<List<NotificationEntity>> = combine(_currentUserId, _currentUserIsAdmin) { uid, isAdmin ->
        Pair(uid, isAdmin)
    }.flatMapLatest { (uid, isAdmin) ->
        if (uid.isNotBlank()) repository.getNotificationsForUser(uid, isAdmin) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadNotificationsCount: StateFlow<Int> = combine(_currentUserId, _currentUserIsAdmin) { uid, isAdmin ->
        Pair(uid, isAdmin)
    }.flatMapLatest { (uid, isAdmin) ->
        if (uid.isNotBlank()) repository.getUnreadNotificationsCount(uid, isAdmin) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedChatRequestId = MutableStateFlow<String?>(null)
    val selectedChatRequestId: StateFlow<String?> = _selectedChatRequestId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedRequestMessages: StateFlow<List<RequestMessageEntity>> = _selectedChatRequestId
        .flatMapLatest { reqId ->
            if (!reqId.isNullOrBlank()) repository.getMessagesForRequest(reqId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setCurrentUser(userId: String, isAdmin: Boolean) {
        _currentUserId.value = userId
        _currentUserIsAdmin.value = isAdmin
        syncUserRequestsAndNotifications(userId, isAdmin)
    }

    fun syncUserRequestsAndNotifications(userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            repository.syncRequestsFromFirestore()
            repository.syncNotificationsFromFirestore(userId, isAdmin)
        }
    }

    fun createBookRequest(
        userId: String,
        userName: String,
        userEmail: String,
        title: String,
        author: String = "",
        year: String = "",
        message: String = ""
    ) {
        if (title.isBlank()) {
            _toastMessage.value = "O título da obra é obrigatório."
            return
        }
        viewModelScope.launch {
            repository.createBookRequest(userId, userName, userEmail, title, author, year, message)
            _toastMessage.value = "Solicitação enviada com sucesso! Você pode acompanhar o status no seu perfil."
        }
    }

    fun updateBookRequestStatus(
        requestId: String,
        newStatus: String,
        recipientUserId: String,
        bookTitle: String,
        adminComment: String = ""
    ) {
        viewModelScope.launch {
            repository.updateBookRequestStatus(requestId, newStatus, recipientUserId, bookTitle, adminComment)
            val text = if (newStatus == "ACCEPTED") "Solicitação aceita!" else "Solicitação recusada."
            _toastMessage.value = text
        }
    }

    fun selectRequestForChat(requestId: String?) {
        _selectedChatRequestId.value = requestId
        if (!requestId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.syncMessagesForRequest(requestId)
            }
        }
    }

    fun sendRequestMessage(
        requestId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        messageText: String,
        recipientUserId: String,
        bookTitle: String
    ) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            repository.sendRequestMessage(requestId, senderId, senderName, senderRole, messageText, recipientUserId, bookTitle)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead(userId: String, isAdmin: Boolean = false) {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(userId, isAdmin)
        }
    }


    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, book.isFavorite)
        }
    }

    fun downloadBookForOffline(book: BookEntity) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (book.id to 0.1f)
            val result = repository.downloadBookForOffline(book.id) { progress ->
                _downloadProgress.value = _downloadProgress.value + (book.id to progress)
            }
            _downloadProgress.value = _downloadProgress.value - book.id
            if (result.isSuccess) {
                _toastMessage.value = "✓ \"${book.title}\" baixado para leitura offline isolada!"
            } else {
                _toastMessage.value = "Erro ao baixar livro: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun removeDownload(book: BookEntity) {
        viewModelScope.launch {
            repository.removeOfflineDownload(book.id)
            _toastMessage.value = "Download de \"${book.title}\" removido do dispositivo."
        }
    }

    fun addNewBookByAdmin(
        title: String,
        author: String,
        category: String,
        description: String,
        coverUrl: String,
        pdfUrl: String,
        onComplete: () -> Unit
    ) {
        if (title.isBlank() || author.isBlank() || pdfUrl.isBlank()) {
            _toastMessage.value = "Título, Autor e Link do PDF são obrigatórios!"
            return
        }

        viewModelScope.launch {
            val newBook = repository.addNewBookByAdmin(
                title = title,
                author = author,
                category = category,
                description = description,
                coverUrl = coverUrl,
                pdfUrl = pdfUrl
            )
            _toastMessage.value = "🎉 Livro \"${newBook.title}\" cadastrado com sucesso!"
            onComplete()
        }
    }

    fun deleteBookByAdmin(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            _toastMessage.value = "Livro removido do acervo."
        }
    }

    fun createCategory(name: String) {
        if (name.isBlank()) {
            _toastMessage.value = "O nome da categoria não pode estar em branco."
            return
        }
        viewModelScope.launch {
            val cat = repository.addCategory(name)
            _toastMessage.value = "Categoria \"${cat.name}\" criada com sucesso!"
        }
    }

    fun toggleCategoryVisibility(categoryId: String, isVisible: Boolean) {
        viewModelScope.launch {
            repository.updateCategoryVisibility(categoryId, isVisible)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _toastMessage.value = "Categoria excluída. Livros associados foram movidos para \"Geral\"."
        }
    }

    fun getReviewsForBook(bookId: String): StateFlow<List<ReviewEntity>> {
        viewModelScope.launch {
            repository.syncReviewsFromFirestore(bookId)
        }
        return repository.getReviewsForBook(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addReview(bookId: String, userId: String, userName: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.addReview(bookId, userId, userName, rating, comment)
            _toastMessage.value = "Sua avaliação foi enviada com sucesso!"
        }
    }

    suspend fun saveReadingProgress(bookId: String, page: Int, totalPages: Int) {
        repository.saveReadingProgress(bookId, page, totalPages)
    }

    suspend fun getReadingProgress(bookId: String): ReadingProgressEntity? {
        return repository.getReadingProgress(bookId)
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun getIsolatedStorageUsageFormatted(): String {
        val bytes = PdfDownloadManager.getIsolatedStorageUsageBytes(getApplication())
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format("%.2f MB", mb)
    }
}
