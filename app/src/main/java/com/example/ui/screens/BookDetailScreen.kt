package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.UserEntity
import com.example.data.util.DriveUrlConverter
import com.example.data.util.PdfDownloadManager
import com.example.data.util.PrintHelper
import com.example.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    currentUser: UserEntity?,
    libraryViewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onOpenPdfReader: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var book by remember { mutableStateOf<BookEntity?>(null) }
    var readingProgress by remember { mutableStateOf<ReadingProgressEntity?>(null) }
    val reviews by libraryViewModel.getReviewsForBook(bookId).collectAsState(initial = emptyList())
    val downloadProgressMap by libraryViewModel.downloadProgress.collectAsState()

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    LaunchedEffect(bookId) {
        libraryViewModel.booksList.collect { books ->
            book = books.find { it.id == bookId }
        }
    }

    LaunchedEffect(bookId) {
        readingProgress = libraryViewModel.getReadingProgress(bookId)
    }

    val currentBook = book
    val isDownloading = downloadProgressMap.containsKey(bookId)
    val progressValue = downloadProgressMap[bookId] ?: 0f

    if (currentBook == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header com Capa e Gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                AsyncImage(
                    model = currentBook.displayCoverUrl,
                    contentDescription = currentBook.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val shareLink = "https://biblioteca-c8737.firebaseapp.com/book/${currentBook.id}"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Confira o livro \"${currentBook.title}\" de ${currentBook.author} na Biblioteca Digital!\n\nAcesse no app pelo link: $shareLink"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Livro"))
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar Link", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    var localPdf = PdfDownloadManager.getDownloadedPdfFile(context, currentBook.id)
                                    if (localPdf == null || !PdfDownloadManager.isValidPdfFile(localPdf)) {
                                        localPdf = PdfDownloadManager.generateSamplePdfIfMissing(
                                            context,
                                            currentBook.id,
                                            currentBook.title,
                                            currentBook.author,
                                            currentBook.description
                                        )
                                    }
                                    PrintHelper.printPdfFile(context, localPdf, "Imprimir_${currentBook.title.replace(" ", "_")}")
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Imprimir Livro", tint = Color.White)
                        }

                        IconButton(
                            onClick = { libraryViewModel.toggleFavorite(currentBook) },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (currentBook.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favoritar",
                                tint = if (currentBook.isFavorite) Color.Red else Color.White
                            )
                        }
                    }
                }
            }

            // Informações Principais
            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentBook.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentBook.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Por ${currentBook.author}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Métricas (Avaliação e Downloads)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = String.format("%.1f", currentBook.ratingAverage), fontWeight = FontWeight.Bold)
                        }
                        Text(text = "${currentBook.ratingCount} avaliações", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.height(32.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (currentBook.isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (currentBook.isDownloaded) "Salvo Offline" else "Download Disponível",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cartão de Progresso de Leitura Anterior
                readingProgress?.let { prog ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continuar Leitura", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Página ${prog.lastPage} de ${prog.totalPages} (${prog.progressPercentage.toInt()}%)", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { prog.progressPercentage / 100f },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Botões de Ação Principal (Ler In-App e Baixar Offline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onOpenPdfReader(currentBook.id) },
                        modifier = Modifier.weight(1.3f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ler no Aplicativo", fontWeight = FontWeight.Bold)
                    }

                    if (currentBook.isDownloaded) {
                        OutlinedButton(
                            onClick = { libraryViewModel.removeDownload(currentBook) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir PDF", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { libraryViewModel.downloadBookForOffline(currentBook) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            enabled = !isDownloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Baixar Offline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Botões Secundários (Compartilhar Link do Livro e Imprimir)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareLink = "https://biblioteca-c8737.firebaseapp.com/book/${currentBook.id}"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Confira o livro \"${currentBook.title}\" de ${currentBook.author} na Biblioteca Digital!\n\nAcesse no app pelo link: $shareLink"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Livro"))
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar Link", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                var localPdf = PdfDownloadManager.getDownloadedPdfFile(context, currentBook.id)
                                if (localPdf == null || !PdfDownloadManager.isValidPdfFile(localPdf)) {
                                    localPdf = PdfDownloadManager.generateSamplePdfIfMissing(
                                        context,
                                        currentBook.id,
                                        currentBook.title,
                                        currentBook.author,
                                        currentBook.description
                                    )
                                }
                                PrintHelper.printPdfFile(context, localPdf, "Imprimir_${currentBook.title.replace(" ", "_")}")
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Imprimir Livro", fontSize = 12.sp)
                    }
                }

                AnimatedVisibility(visible = isDownloading) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Baixando PDF para o armazenamento privado isolado... ${(progressValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card informativo sobre o Leitor 100% Integrado no APK
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Leitor NATIVO Integrado no APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Lê o livro completo dentro do próprio app. Não precisa de navegador nem do Google Drive.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sinopse do Livro
                Text(
                    text = "Sinopse e Descrição",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentBook.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Seção de Avaliações e Comentários
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avaliações dos Leitores (${reviews.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    TextButton(onClick = { showReviewDialog = true }) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Avaliar Livro")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (reviews.isEmpty()) {
                    Text(
                        text = "Ainda não há avaliações para este livro. Seja o primeiro a opinar!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        reviews.forEach { rev ->
                            ReviewItemCard(review = rev)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Modal de Diálogo para Deixar Avaliação
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Avaliar \"${currentBook.title}\"") },
            text = {
                Column {
                    Text("Escolha uma nota de 1 a 5 estrelas:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { reviewRating = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star Estrelas",
                                    tint = if (star <= reviewRating) Color(0xFFFFB300) else Color.LightGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("Escreva seu comentário sobre a obra...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUser != null) {
                            libraryViewModel.addReview(
                                bookId = currentBook.id,
                                userId = currentUser.uid,
                                userName = currentUser.name,
                                rating = reviewRating,
                                comment = reviewComment
                            )
                        }
                        showReviewDialog = false
                        reviewComment = ""
                    }
                ) {
                    Text("Enviar Avaliação")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ReviewItemCard(review: ReviewEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (star <= review.rating) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
