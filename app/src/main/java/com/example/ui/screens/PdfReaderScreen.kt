package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BookEntity
import com.example.data.util.PdfDownloadManager
import com.example.data.util.PrintHelper
import com.example.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    bookId: String,
    libraryViewModel: LibraryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var book by remember { mutableStateOf<BookEntity?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var totalPagesCount by remember { mutableStateOf(1) }
    var initialPage by remember { mutableStateOf(0) }

    var isLoadingPdf by remember { mutableStateOf(true) }
    var loadingStatusText by remember { mutableStateOf("Carregando e processando documento PDF...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var isReaderDarkMode by remember { mutableStateOf(false) }

    // Gestos de Zoom e Pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val renderMutex = remember { Mutex() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    val currentPageIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, (totalPagesCount - 1).coerceAtLeast(0)) }
    }

    // Salva progresso de leitura automaticamente conforme o leitor faz scroll
    LaunchedEffect(currentPageIndex, totalPagesCount) {
        if (!isLoadingPdf && totalPagesCount > 0) {
            libraryViewModel.saveReadingProgress(bookId, currentPageIndex + 1, totalPagesCount)
        }
    }

    // Carrega PDF e inicializa renderer
    LaunchedEffect(bookId) {
        val foundBook = libraryViewModel.booksList.value.find { it.id == bookId }
        book = foundBook

        if (foundBook != null) {
            val progress = libraryViewModel.getReadingProgress(bookId)
            val startPage = (progress?.lastPage ?: 1) - 1

            var localFile = PdfDownloadManager.getDownloadedPdfFile(context, bookId)

            if (localFile == null) {
                val targetUrl = foundBook.directPdfUrl.ifBlank { foundBook.pdfUrl }
                if (targetUrl.isNotBlank() && targetUrl.startsWith("http")) {
                    loadingStatusText = "Carregando o livro no leitor integrado..."
                    val downloadResult = PdfDownloadManager.downloadPdfToIsolatedStorage(context, bookId, targetUrl)
                    if (downloadResult.isSuccess) {
                        localFile = downloadResult.getOrNull()
                    }
                }
            }

            if (localFile == null || !PdfDownloadManager.isValidPdfFile(localFile)) {
                localFile = PdfDownloadManager.generateSamplePdfIfMissing(
                    context = context,
                    bookId = bookId,
                    bookTitle = foundBook.title,
                    bookAuthor = foundBook.author,
                    description = foundBook.description
                )
            }

            pdfFile = localFile

            try {
                withContext(Dispatchers.IO) {
                    val fileDescriptor = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    pdfRenderer = renderer
                    val pages = renderer.pageCount
                    totalPagesCount = if (pages > 0) pages else 1
                    val safeStartPage = if (startPage in 0 until totalPagesCount) startPage else 0
                    initialPage = safeStartPage
                }
                isLoadingPdf = false
            } catch (e: Exception) {
                try {
                    val generatedFile = PdfDownloadManager.generateSamplePdfIfMissing(
                        context = context,
                        bookId = bookId,
                        bookTitle = foundBook.title,
                        bookAuthor = foundBook.author,
                        description = foundBook.description
                    )
                    pdfFile = generatedFile
                    withContext(Dispatchers.IO) {
                        val fileDescriptor = ParcelFileDescriptor.open(generatedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(fileDescriptor)
                        pdfRenderer = renderer
                        val pages = renderer.pageCount
                        totalPagesCount = if (pages > 0) pages else 1
                        initialPage = 0
                    }
                    isLoadingPdf = false
                } catch (ex: Exception) {
                    errorMessage = "Erro ao carregar o livro no leitor embutido: ${ex.message}"
                    isLoadingPdf = false
                }
            }
        }
    }

    // Navega para a página inicial salva quando o carregamento é concluído
    LaunchedEffect(isLoadingPdf) {
        if (!isLoadingPdf && initialPage > 0) {
            listState.scrollToItem(initialPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Leitor PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Página ${currentPageIndex + 1} de $totalPagesCount (Scroll Contínuo)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            pdfFile?.let { file ->
                                PrintHelper.printPdfFile(context, file, "Imprimir_${book?.title?.replace(" ", "_") ?: "Livro"}")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir Livro")
                    }
                    IconButton(onClick = { isReaderDarkMode = !isReaderDarkMode }) {
                        Icon(
                            imageVector = if (isReaderDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Modo Leitura"
                        )
                    }
                    IconButton(onClick = { showPageJumpDialog = true }) {
                        Icon(Icons.Default.FindInPage, contentDescription = "Ir para página")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = currentPageIndex.toFloat(),
                        onValueChange = { targetVal ->
                            val page = targetVal.toInt().coerceIn(0, totalPagesCount - 1)
                            coroutineScope.launch {
                                listState.scrollToItem(page)
                            }
                        },
                        valueRange = 0f..(totalPagesCount - 1).coerceAtLeast(1).toFloat(),
                        steps = (totalPagesCount - 2).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(currentPageIndex - 1)
                                    }
                                }
                            },
                            enabled = currentPageIndex > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anterior")
                        }

                        Text(
                            text = "${((currentPageIndex + 1).toFloat() / totalPagesCount * 100).toInt()}% Lido",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                if (currentPageIndex < totalPagesCount - 1) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(currentPageIndex + 1)
                                    }
                                }
                            },
                            enabled = currentPageIndex < totalPagesCount - 1,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Próxima")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.NavigateNext, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isReaderDarkMode) Color(0xFF121212) else Color(0xFFE9ECEF)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoadingPdf -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(loadingStatusText, fontWeight = FontWeight.Medium)
                    }
                }

                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoadingPdf = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recarregar no Leitor Integrado")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onBackClick, shape = RoundedCornerShape(12.dp)) {
                            Text("Voltar ao Livro")
                        }
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Lista de Páginas do PDF em Scroll Vertical
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                },
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(totalPagesCount, key = { it }) { pageIdx ->
                                var bitmap by remember { mutableStateOf(renderedBitmaps[pageIdx]) }

                                LaunchedEffect(pageIdx, pdfRenderer) {
                                    if (bitmap == null) {
                                        val renderer = pdfRenderer
                                        if (renderer != null) {
                                            renderMutex.withLock {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        if (pageIdx in 0 until renderer.pageCount) {
                                                            val page = renderer.openPage(pageIdx)
                                                            val w = page.width * 2
                                                            val h = page.height * 2
                                                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                                            val canvas = android.graphics.Canvas(bmp)
                                                            canvas.drawColor(android.graphics.Color.WHITE)
                                                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                            page.close()
                                                            renderedBitmaps[pageIdx] = bmp
                                                            bitmap = bmp
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap!!.asImageBitmap(),
                                                contentDescription = "Página ${pageIdx + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer(
                                                        scaleX = scale,
                                                        scaleY = scale,
                                                        translationX = offsetX,
                                                        translationY = offsetY
                                                    )
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                if (scale > 1f) {
                                                                    scale = 1f
                                                                    offsetX = 0f
                                                                    offsetY = 0f
                                                                } else {
                                                                    scale = 2.5f
                                                                }
                                                            }
                                                        )
                                                    }
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Processando página ${pageIdx + 1}...", fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Bar de Controles de Zoom
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        scale = (kotlin.math.round((scale - 0.1f) * 10f) / 10f).coerceAtLeast(1f)
                                        if (scale == 1f) {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    },
                                    enabled = scale > 1f,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir Zoom", modifier = Modifier.size(20.dp))
                                }

                                Text(
                                    text = "${kotlin.math.round(scale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        scale = (kotlin.math.round((scale + 0.1f) * 10f) / 10f).coerceAtMost(4f)
                                    },
                                    enabled = scale < 4f,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar Zoom", modifier = Modifier.size(20.dp))
                                }

                                if (scale > 1f) {
                                    IconButton(
                                        onClick = {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = "Resetar Zoom", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPageJumpDialog) {
        AlertDialog(
            onDismissRequest = { showPageJumpDialog = false },
            title = { Text("Ir para Página") },
            text = {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = { jumpPageInput = it },
                    label = { Text("Número da página (1 - $totalPagesCount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPage = jumpPageInput.toIntOrNull()
                        if (targetPage != null && targetPage in 1..totalPagesCount) {
                            coroutineScope.launch {
                                listState.scrollToItem(targetPage - 1)
                            }
                        }
                        showPageJumpDialog = false
                        jumpPageInput = ""
                    }
                ) {
                    Text("Navegar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPageJumpDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
