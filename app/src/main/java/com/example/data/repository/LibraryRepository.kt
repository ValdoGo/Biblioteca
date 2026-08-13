package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DEFAULT_BOOK_COVER_URL
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RequestMessageEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity
import com.example.data.util.DriveUrlConverter
import com.example.data.util.FirestoreService
import com.example.data.util.NotificationHelper
import com.example.data.util.PdfDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LibraryRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val bookDao = db.bookDao()
    private val userDao = db.userDao()
    private val reviewDao = db.reviewDao()
    private val progressDao = db.readingProgressDao()
    private val categoryDao = db.categoryDao()
    private val requestDao = db.bookRequestDao()
    private val messageDao = db.requestMessageDao()
    private val notificationDao = db.notificationDao()

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<BookEntity>> = bookDao.getFavoriteBooks()
    val downloadedBooks: Flow<List<BookEntity>> = bookDao.getDownloadedBooks()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val homeCategories: Flow<List<CategoryEntity>> = categoryDao.getHomeCategories()
    val allBookRequests: Flow<List<BookRequestEntity>> = requestDao.getAllRequests()

    fun getRequestsForUser(userId: String): Flow<List<BookRequestEntity>> {
        return requestDao.getRequestsForUser(userId)
    }

    fun getMessagesForRequest(requestId: String): Flow<List<RequestMessageEntity>> {
        return messageDao.getMessagesForRequest(requestId)
    }

    fun getNotificationsForUser(userId: String, isAdmin: Boolean = false): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(userId, isAdmin)
    }

    fun getUnreadNotificationsCount(userId: String, isAdmin: Boolean = false): Flow<Int> {
        return notificationDao.getUnreadCountForUser(userId, isAdmin)
    }


    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // 1. Sincronização e Inicialização de Categorias Acadêmicas
        val defaultAcademicCategories = listOf(
            CategoryEntity("cat_geral", "Geral", true),
            CategoryEntity("cat_eng", "Engenharia & Tecnologia", true),
            CategoryEntity("cat_exatas", "Ciências Exactas & Matemática", true),
            CategoryEntity("cat_direito", "Direito & Ciências Sociais", true),
            CategoryEntity("cat_economia", "Economia & Gestão", true),
            CategoryEntity("cat_medicina", "Medicina & Saúde", true),
            CategoryEntity("cat_educacao", "Educação & Filosofia", true)
        )

        val remoteCategories = FirestoreService.fetchAllCategoriesFromFirestore()
        if (remoteCategories.isNotEmpty()) {
            categoryDao.insertCategories(remoteCategories)
        } else {
            categoryDao.insertCategories(defaultAcademicCategories)
            for (cat in defaultAcademicCategories) {
                FirestoreService.saveCategoryToFirestore(cat)
            }
        }

        val driveUrlArteDeViver = "https://drive.google.com/file/d/1xTm_PY0F_j8APxSeOhnT5UbKg6pfs7GG/view?usp=drive_link"
        val directUrlArteDeViver = DriveUrlConverter.convertToDirectDownloadLink(driveUrlArteDeViver)

        val arteDeViverBook = BookEntity(
            id = "b_arte_de_viver",
            title = "A Arte de Viver",
            author = "Valdimiro Elidio",
            category = "Educação & Filosofia",
            description = "Uma obra inspiradora por Valdimiro Elidio sobre reflexões profundas da jornada humana, resiliência e as escolhas que moldam a nossa existência.",
            coverUrl = DEFAULT_BOOK_COVER_URL,
            pdfUrl = driveUrlArteDeViver,
            directPdfUrl = directUrlArteDeViver,
            ratingAverage = 5.0,
            ratingCount = 18,
            viewsCount = 1520
        )

        // Tenta buscar o acervo remoto no Firebase Firestore
        val remoteBooks = FirestoreService.fetchAllBooksFromFirestore()
        if (remoteBooks.isNotEmpty()) {
            bookDao.insertBooks(remoteBooks)
        } else {
            // Se o Firebase estiver vazio na primeira execução, sobe o acervo inicial para o Firebase
            val initialBooks = listOf(
                arteDeViverBook,
                BookEntity(
                    id = "b1",
                    title = "O Pequeno Príncipe",
                    author = "Antoine de Saint-Exupéry",
                    category = "Educação & Filosofia",
                    description = "Um clássico atemporal da literatura mundial que conta a história da amizade entre um piloto de avião e um garoto vindo de um asteroide distante.",
                    coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop",
                    pdfUrl = "https://drive.google.com/file/d/1sample_pequeno_principe/view",
                    directPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    ratingAverage = 4.9,
                    ratingCount = 38,
                    viewsCount = 1420
                ),
                BookEntity(
                    id = "b2",
                    title = "Clean Architecture em Kotlin",
                    author = "Robert C. Martin & Comunidade Android",
                    category = "Engenharia & Tecnologia",
                    description = "Guia prático sobre princípios SOLID, padrões de projeto modernos, Jetpack Compose e arquiteturas limpas escaláveis para Android.",
                    coverUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop",
                    pdfUrl = "https://drive.google.com/file/d/1sample_clean_arch/view",
                    directPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    ratingAverage = 4.8,
                    ratingCount = 25,
                    viewsCount = 980
                ),
                BookEntity(
                    id = "b3",
                    title = "Dom Casmurro",
                    author = "Machado de Assis",
                    category = "Geral",
                    description = "Uma das maiores obras da literatura brasileira. Bentinho relembra sua infância e juventude ao lado de Capitu, levantando o célebre mistério da dúvida.",
                    coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop",
                    pdfUrl = "https://drive.google.com/file/d/1sample_dom_casmurro/view",
                    directPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    ratingAverage = 4.7,
                    ratingCount = 42,
                    viewsCount = 1890
                ),
                BookEntity(
                    id = "b4",
                    title = "Inteligência Artificial e Futuro",
                    author = "Dra. Helena Vasconcelos",
                    category = "Ciências Exactas & Matemática",
                    description = "Exploração profunda sobre redes neurais, modelos generativos, ética no desenvolvimento de sistemas autônomos e os impactos no mercado.",
                    coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop",
                    pdfUrl = "https://drive.google.com/file/d/1sample_ia_futuro/view",
                    directPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    ratingAverage = 4.6,
                    ratingCount = 19,
                    viewsCount = 760
                ),
                BookEntity(
                    id = "b5",
                    title = "O Poder do Foco Diário",
                    author = "Carlos Eduardo Santos",
                    category = "Geral",
                    description = "Estratégias práticas e cientificamente comprovadas para desenvolver disciplina mental, eliminar distração digital e construir hábitos de alta performance.",
                    coverUrl = "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=600&auto=format&fit=crop",
                    pdfUrl = "https://drive.google.com/file/d/1sample_foco_diario/view",
                    directPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    ratingAverage = 4.5,
                    ratingCount = 31,
                    viewsCount = 1100
                )
            )

            bookDao.insertBooks(initialBooks)
            for (bk in initialBooks) {
                FirestoreService.saveBookToFirestore(bk)
            }
        }

        // Garante que 'A Arte de Viver' esteja gravado localmente
        if (bookDao.getBookById("b_arte_de_viver") == null) {
            bookDao.insertBook(arteDeViverBook)
            FirestoreService.saveBookToFirestore(arteDeViverBook)
        }

        // Usuários iniciais de exemplo (Admin & Leitor)
        val initialUsers = listOf(
            UserEntity(
                uid = "admin_master_101",
                name = "Administrador do Sistema",
                email = "admin@biblioteca.com",
                role = "admin"
            ),
            UserEntity(
                uid = "reader_user_202",
                name = "Lucas Mendes",
                email = "lucas.mendes@email.com",
                role = "reader"
            ),
            UserEntity(
                uid = "reader_user_203",
                name = "Mariana Oliveira",
                email = "mariana.o@email.com",
                role = "reader"
            )
        )
        for (u in initialUsers) {
            userDao.insertUser(u)
            FirestoreService.saveUserToFirestore(u, "123456")
        }

        // Avaliações de exemplo
        val sampleReviews = listOf(
            ReviewEntity(
                id = "r1",
                bookId = "b1",
                userId = "reader_user_202",
                userName = "Lucas Mendes",
                rating = 5,
                comment = "Leitura emocionante! Todo adulto deveria ler essa obra."
            ),
            ReviewEntity(
                id = "r2",
                bookId = "b2",
                userId = "reader_user_203",
                userName = "Mariana Oliveira",
                rating = 5,
                comment = "Excelente material de referência para desenvolvedores Kotlin."
            )
        )
        for (r in sampleReviews) {
            reviewDao.insertReview(r)
            FirestoreService.saveReviewToFirestore(r)
        }
    }

    suspend fun getBookById(bookId: String): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(bookId)
    }

    suspend fun addNewBookByAdmin(
        title: String,
        author: String,
        category: String,
        description: String,
        coverUrl: String,
        pdfUrl: String
    ): BookEntity = withContext(Dispatchers.IO) {
        val convertedPdfUrl = DriveUrlConverter.convertToDirectDownloadLink(pdfUrl)
        val newBook = BookEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            category = category,
            description = description,
            coverUrl = coverUrl.ifBlank { DEFAULT_BOOK_COVER_URL },
            pdfUrl = pdfUrl,
            directPdfUrl = convertedPdfUrl,
            ratingAverage = 5.0,
            ratingCount = 1,
            addedAt = System.currentTimeMillis()
        )
        bookDao.insertBook(newBook)

        // Sincroniza com o Firebase Firestore
        FirestoreService.saveBookToFirestore(newBook)

        // Dispara notificação do sistema Android
        NotificationHelper.sendNewBookNotification(context, title, author)

        // Salva notificação in-app para todos os usuários
        val notif = NotificationEntity(
            id = "notif_" + UUID.randomUUID().toString(),
            userId = "ALL",
            title = "📚 Novo Livro Adicionado ao Acervo",
            message = "O livro \"$title\" por $author já está disponível no catálogo!",
            type = "NEW_BOOK",
            targetId = newBook.id
        )
        notificationDao.insertNotification(notif)
        FirestoreService.saveNotificationToFirestore(notif)

        newBook
    }

    suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.updateBook(book)
        FirestoreService.saveBookToFirestore(book)
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        PdfDownloadManager.deleteDownloadedPdf(context, bookId)
        bookDao.deleteBookById(bookId)
        FirestoreService.deleteBookFromFirestore(bookId)
    }

    suspend fun toggleFavorite(bookId: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavoriteStatus(bookId, !currentStatus)
    }

    suspend fun downloadBookForOffline(
        bookId: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext Result.failure(Exception("Livro não encontrado"))

        val result = if (book.directPdfUrl.startsWith("http")) {
            PdfDownloadManager.downloadPdfToIsolatedStorage(context, bookId, book.directPdfUrl, onProgress)
        } else {
            Result.failure(Exception("URL inválida"))
        }

        val file = if (result.isSuccess) {
            result.getOrThrow()
        } else {
            // Gera um PDF sintético estilizado para leitura off-line garantida se o link externo expirar
            PdfDownloadManager.generateSamplePdfIfMissing(
                context = context,
                bookId = bookId,
                bookTitle = book.title,
                bookAuthor = book.author,
                description = book.description
            )
        }

        bookDao.updateDownloadStatus(bookId, true, file.absolutePath)
        NotificationHelper.sendDownloadCompleteNotification(context, book.title)
        Result.success(file)
    }

    suspend fun removeOfflineDownload(bookId: String) = withContext(Dispatchers.IO) {
        PdfDownloadManager.deleteDownloadedPdf(context, bookId)
        bookDao.updateDownloadStatus(bookId, false, null)
    }

    fun getReviewsForBook(bookId: String): Flow<List<ReviewEntity>> {
        return reviewDao.getReviewsForBook(bookId)
    }

    suspend fun syncReviewsFromFirestore(bookId: String) = withContext(Dispatchers.IO) {
        val remoteReviews = FirestoreService.fetchReviewsFromFirestore(bookId)
        for (rev in remoteReviews) {
            reviewDao.insertReview(rev)
        }
    }

    suspend fun addReview(bookId: String, userId: String, userName: String, rating: Int, comment: String) = withContext(Dispatchers.IO) {
        val review = ReviewEntity(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            userId = userId,
            userName = userName,
            rating = rating,
            comment = comment
        )
        reviewDao.insertReview(review)
        FirestoreService.saveReviewToFirestore(review)

        // Atualiza a média de avaliação do livro localmente e no Firebase
        val currentBook = bookDao.getBookById(bookId)
        if (currentBook != null) {
            val newRatingCount = currentBook.ratingCount + 1
            val newRatingAvg = ((currentBook.ratingAverage * currentBook.ratingCount) + rating) / newRatingCount
            val updatedBook = currentBook.copy(
                ratingAverage = newRatingAvg,
                ratingCount = newRatingCount
            )
            bookDao.updateBook(updatedBook)
            FirestoreService.saveBookToFirestore(updatedBook)
        }
    }

    suspend fun saveReadingProgress(bookId: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val percent = if (totalPages > 0) (page.toFloat() / totalPages.toFloat()) * 100f else 0f
        val progress = ReadingProgressEntity(
            bookId = bookId,
            lastPage = page,
            totalPages = totalPages,
            progressPercentage = percent,
            lastReadTimestamp = System.currentTimeMillis()
        )
        progressDao.saveProgress(progress)
    }

    suspend fun getReadingProgress(bookId: String): ReadingProgressEntity? = withContext(Dispatchers.IO) {
        progressDao.getProgressForBook(bookId)
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        userDao.getUserByEmail(cleanEmail)
    }

    suspend fun findAndAuthenticateUser(email: String, pass: String): UserEntity? = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()

        // 1. Tenta buscar todos os usuários remotos no Firebase Firestore e insere no banco local Room
        try {
            val remoteUsers = FirestoreService.fetchAllUsersFromFirestore()
            for (remote in remoteUsers) {
                userDao.insertUser(remote.user)
            }
            // Verifica nos remotos se o e-mail e a senha conferem
            val matchedRemote = remoteUsers.find { it.user.email.equals(cleanEmail, ignoreCase = true) }
            if (matchedRemote != null) {
                if (matchedRemote.pass.isEmpty() || matchedRemote.pass == pass) {
                    return@withContext matchedRemote.user
                } else {
                    throw Exception("Senha incorreta. Verifique os dados digitados.")
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("Senha incorreta") == true) throw e
            e.printStackTrace()
        }

        // 2. Se não encontrou no Firestore ou esteve off-line, consulta o banco de dados local
        val localUser = userDao.getUserByEmail(cleanEmail)
        if (localUser != null) {
            return@withContext localUser
        }

        null
    }

    suspend fun registerNewUser(uid: String, name: String, email: String, role: String, pass: String = ""): UserEntity = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        val user = UserEntity(
            uid = uid,
            name = name,
            email = cleanEmail,
            role = role
        )
        userDao.insertUser(user)
        FirestoreService.saveUserToFirestore(user, pass)
        user
    }

    suspend fun addCategory(name: String, isVisibleOnHome: Boolean = true): CategoryEntity = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val id = "cat_" + UUID.randomUUID().toString().take(8)
        val cat = CategoryEntity(id = id, name = cleanName, isVisibleOnHome = isVisibleOnHome)
        categoryDao.insertCategory(cat)
        FirestoreService.saveCategoryToFirestore(cat)
        cat
    }

    suspend fun updateCategoryVisibility(categoryId: String, isVisible: Boolean) = withContext(Dispatchers.IO) {
        val existing = categoryDao.getCategoryById(categoryId)
        if (existing != null) {
            val updated = existing.copy(isVisibleOnHome = isVisible)
            categoryDao.insertCategory(updated)
            FirestoreService.saveCategoryToFirestore(updated)
        }
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val existing = categoryDao.getCategoryById(categoryId)
        if (existing != null) {
            val defaultCategoryName = "Geral"

            // Mover todos os livros desta categoria para a categoria padrão "Geral"
            val affectedBooks = bookDao.getBooksByCategory(existing.name)
            for (bk in affectedBooks) {
                val updatedBk = bk.copy(category = defaultCategoryName)
                bookDao.updateBook(updatedBk)
                FirestoreService.saveBookToFirestore(updatedBk)
            }

            // Exclui a categoria do banco de dados local e do Firebase
            categoryDao.deleteCategoryById(categoryId)
            FirestoreService.deleteCategoryFromFirestore(categoryId)
        }
    }

    // --- MÉTODOS DE SOLICITAÇÃO DE LIVROS / DOCUMENTOS ---
    suspend fun createBookRequest(
        userId: String,
        userName: String,
        userEmail: String,
        title: String,
        author: String = "",
        year: String = "",
        message: String = ""
    ): BookRequestEntity = withContext(Dispatchers.IO) {
        val requestId = "req_" + UUID.randomUUID().toString().take(8)
        val request = BookRequestEntity(
            id = requestId,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            title = title.trim(),
            author = author.trim(),
            year = year.trim(),
            message = message.trim(),
            status = "PENDING"
        )

        requestDao.insertRequest(request)
        FirestoreService.saveBookRequestToFirestore(request)

        // Envia mensagem inicial se houver
        if (message.isNotBlank()) {
            val initMsg = RequestMessageEntity(
                id = "msg_" + UUID.randomUUID().toString().take(8),
                requestId = requestId,
                senderId = userId,
                senderName = userName,
                senderRole = "user",
                message = message.trim()
            )
            messageDao.insertMessage(initMsg)
            FirestoreService.saveRequestMessageToFirestore(initMsg)
        }

        // Notifica apenas os administradores sobre nova solicitação
        val adminNotif = NotificationEntity(
            id = "notif_" + UUID.randomUUID().toString(),
            userId = "ADMIN", // Apens os administradores receberão esta notificação
            title = "📩 Nova Solicitação de Livro",
            message = "$userName solicitou a obra \"${title.trim()}\".",
            type = "REQUEST_STATUS",
            targetId = requestId
        )
        notificationDao.insertNotification(adminNotif)
        FirestoreService.saveNotificationToFirestore(adminNotif)

        request
    }

    suspend fun updateBookRequestStatus(
        requestId: String,
        newStatus: String, // "ACCEPTED" ou "REJECTED"
        recipientUserId: String,
        bookTitle: String,
        adminComment: String = ""
    ): Unit = withContext(Dispatchers.IO) {
        requestDao.updateRequestStatus(requestId, newStatus)
        val req = requestDao.getRequestById(requestId)
        if (req != null) {
            val updatedReq = req.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            FirestoreService.saveBookRequestToFirestore(updatedReq)
        }

        val statusText = if (newStatus == "ACCEPTED") "aceita!" else "recusada."
        val statusTitle = if (newStatus == "ACCEPTED") "✅ Solicitação Aceita" else "❌ Solicitação Não Atendida"

        val notifMessage = if (adminComment.isNotBlank()) {
            "Sua solicitação para \"$bookTitle\" foi $statusText Resposta do Administrador: $adminComment"
        } else {
            "Sua solicitação para o livro \"$bookTitle\" foi $statusText"
        }

        // Gera notificação in-app para o leitor que solicitou
        val notif = NotificationEntity(
            id = "notif_" + UUID.randomUUID().toString(),
            userId = recipientUserId,
            title = statusTitle,
            message = notifMessage,
            type = "REQUEST_STATUS",
            targetId = requestId
        )
        notificationDao.insertNotification(notif)
        FirestoreService.saveNotificationToFirestore(notif)

        // Dispara notificação push/sistema do Android
        NotificationHelper.sendGenericNotification(context, statusTitle, notifMessage)
    }

    suspend fun sendRequestMessage(
        requestId: String,
        senderId: String,
        senderName: String,
        senderRole: String, // "user" ou "admin"
        messageText: String,
        recipientUserId: String,
        bookTitle: String
    ): RequestMessageEntity = withContext(Dispatchers.IO) {
        val msg = RequestMessageEntity(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            requestId = requestId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = messageText.trim()
        )
        messageDao.insertMessage(msg)
        FirestoreService.saveRequestMessageToFirestore(msg)

        // Se quem mandou for o Admin, gera notificação para o usuário
        if (senderRole == "admin" && recipientUserId.isNotBlank()) {
            val notif = NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString(),
                userId = recipientUserId,
                title = "💬 Nova Mensagem do Administrador",
                message = "Sobre o pedido de \"$bookTitle\": $messageText",
                type = "REQUEST_MESSAGE",
                targetId = requestId
            )
            notificationDao.insertNotification(notif)
            FirestoreService.saveNotificationToFirestore(notif)
        }

        msg
    }

    suspend fun syncRequestsFromFirestore() = withContext(Dispatchers.IO) {
        val remoteRequests = FirestoreService.fetchAllBookRequestsFromFirestore()
        if (remoteRequests.isNotEmpty()) {
            requestDao.insertRequests(remoteRequests)
        }
    }

    suspend fun syncMessagesForRequest(requestId: String) = withContext(Dispatchers.IO) {
        val remoteMsgs = FirestoreService.fetchMessagesForRequestFromFirestore(requestId)
        if (remoteMsgs.isNotEmpty()) {
            messageDao.insertMessages(remoteMsgs)
        }
    }

    suspend fun syncNotificationsFromFirestore(userId: String, isAdmin: Boolean = false) = withContext(Dispatchers.IO) {
        notificationDao.deleteExpiredReadNotifications()
        val remoteNotifs = FirestoreService.fetchNotificationsFromFirestore(userId, isAdmin)
        if (remoteNotifs.isNotEmpty()) {
            notificationDao.insertNotifications(remoteNotifs)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(notificationId, System.currentTimeMillis())
        notificationDao.deleteExpiredReadNotifications()
    }

    suspend fun markAllNotificationsAsRead(userId: String, isAdmin: Boolean = false) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId, isAdmin, System.currentTimeMillis())
        notificationDao.deleteExpiredReadNotifications()
    }
}

