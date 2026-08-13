package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BookRequestEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.AllRequestsBottomSheet
import com.example.ui.components.BookRequestItemCard
import com.example.ui.components.NewBookRequestDialog
import com.example.ui.components.NotificationsBottomSheet
import com.example.ui.components.NotificationsIconButton
import com.example.ui.components.RequestChatBottomSheet
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: UserEntity?,
    authViewModel: AuthViewModel,
    libraryViewModel: LibraryViewModel,
    onNavigateAdminAdd: () -> Unit,
    onNavigateAdminManage: () -> Unit,
    onNavigateAdminUsers: () -> Unit
) {
    val isDarkTheme by libraryViewModel.isDarkTheme.collectAsState()
    val firebaseConfig by libraryViewModel.firebaseConfig.collectAsState()
    val storageUsage = libraryViewModel.getIsolatedStorageUsageFormatted()

    // Sincroniza ID do usuário logado no ViewModel
    LaunchedEffect(currentUser?.uid, currentUser?.role) {
        if (!currentUser?.uid.isNullOrBlank()) {
            libraryViewModel.setCurrentUser(currentUser!!.uid, currentUser.role == "admin")
        }
    }

    val userRequests by libraryViewModel.userBookRequests.collectAsState()
    val allBookRequests by libraryViewModel.allBookRequests.collectAsState()
    val userNotifications by libraryViewModel.userNotifications.collectAsState()
    val unreadNotifCount by libraryViewModel.unreadNotificationsCount.collectAsState()
    val selectedChatReqId by libraryViewModel.selectedChatRequestId.collectAsState()
    val chatMessages by libraryViewModel.selectedRequestMessages.collectAsState()

    var showFirebaseDialog by remember { mutableStateOf(false) }
    var showNewRequestDialog by remember { mutableStateOf(false) }
    var showNotifSheet by remember { mutableStateOf(false) }
    var showAllRequestsSheet by remember { mutableStateOf(false) }

    val requestsToDisplay = if (currentUser?.role == "admin") allBookRequests else userRequests
    val selectedRequestObj = requestsToDisplay.find { it.id == selectedChatReqId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil & Configurações", fontWeight = FontWeight.Bold) },
                actions = {
                    NotificationsIconButton(
                        unreadCount = unreadNotifCount,
                        onClick = { showNotifSheet = true }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cartão de Perfil
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (currentUser?.role == "admin") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (currentUser?.role == "admin") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (currentUser?.role == "admin") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentUser?.name ?: "Leitor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "email@exemplo.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (currentUser?.role == "admin") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (currentUser?.role == "admin") "ADMINISTRADOR DO SISTEMA" else "PERFIL LEITOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser?.role == "admin") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // SEÇÃO 1: SOLICITAÇÃO DE LIVROS / DOCUMENTOS (APENAS PARA LEITORES)
            if (currentUser?.role != "admin") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Solicitar Livro ou Documento",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Não encontrou a obra?",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showNewRequestDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pedir Livro")
                            }
                        }
                    }
                }
            }

            // SEÇÃO 2: PAINEL DE ACOMPANHAMENTO DE PEDIDOS E MINI CHAT
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (currentUser?.role == "admin") "Solicitações dos Leitores (${requestsToDisplay.size})" else "Meus Pedidos de Livros (${requestsToDisplay.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (requestsToDisplay.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma solicitação enviada até o momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Mostra no máximo os 2 mais recentes
                    val recentRequests = requestsToDisplay.take(2)
                    recentRequests.forEach { req ->
                        BookRequestItemCard(
                            request = req,
                            onClick = {
                                libraryViewModel.selectRequestForChat(req.id)
                            }
                        )
                    }

                    // Se houver mais de 2 solicitações, exibe o botão "Ver Todos"
                    if (requestsToDisplay.size > 2) {
                        OutlinedButton(
                            onClick = { showAllRequestsSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver Todos os Pedidos (${requestsToDisplay.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Opções do Painel Admin (se role == admin)
            if (currentUser?.role == "admin") {
                Text(
                    text = "Ferramentas do Administrador",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Cadastrar Novo Livro") },
                            supportingContent = { Text("Adicionar título, capa e link do Google Drive") },
                            leadingContent = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateAdminAdd() }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Gerenciar Acervo e Classes") },
                            supportingContent = { Text("Editar e excluir obras e gerenciar classes no Firestore") },
                            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateAdminManage() }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Monitorar Leitores Cadastrados") },
                            supportingContent = { Text("Visualizar coleção de usuários e UIDs do Firestore") },
                            leadingContent = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateAdminUsers() }
                        )
                    }
                }
            }


            // Opções Gerais e Preferências
            Text(
                text = "Preferências & Sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Modo Escuro / Claro") },
                        supportingContent = { Text(if (isDarkTheme) "Tema Escuro Ativo" else "Tema Claro Ativo") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { libraryViewModel.toggleDarkTheme() }
                            )
                        }
                    )
                    Divider()
                    if (currentUser?.role == "admin") {
                        ListItem(
                            headlineContent = { Text("Configurações do Firebase") },
                            supportingContent = { Text(firebaseConfig.getFormattedSummary().lines().first()) },
                            leadingContent = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6F00)) },
                            trailingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { showFirebaseDialog = true }
                        )
                        Divider()
                    }
                    ListItem(
                        headlineContent = { Text("Armazenamento Isolado de PDFs") },
                        supportingContent = { Text("Espaço ocupado por PDFs offline: $storageUsage") },
                        leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão de Logout
            Button(
                onClick = { authViewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal de Informações de Credenciais e Status do Firebase
    if (showFirebaseDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6F00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Credenciais do Firebase")
                }
            },
            text = {
                Column {
                    Text(
                        text = firebaseConfig.getFormattedSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Você pode alterar suas chaves editando o arquivo .env ou src/config/FirebaseConfig.kt do projeto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFirebaseDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Modal para criar nova solicitação de livro
    if (showNewRequestDialog && currentUser != null) {
        NewBookRequestDialog(
            onDismiss = { showNewRequestDialog = false },
            onSubmit = { title, author, year, message ->
                libraryViewModel.createBookRequest(
                    userId = currentUser.uid,
                    userName = currentUser.name,
                    userEmail = currentUser.email,
                    title = title,
                    author = author,
                    year = year,
                    message = message
                )
            }
        )
    }

    // Sheet de Chat/Acompanhamento da Solicitação
    selectedRequestObj?.let { req ->
        RequestChatBottomSheet(
            request = req,
            messages = chatMessages,
            currentUser = currentUser,
            onDismiss = { libraryViewModel.selectRequestForChat(null) },
            onSendMessage = { msgText ->
                if (currentUser != null) {
                    libraryViewModel.sendRequestMessage(
                        requestId = req.id,
                        senderId = currentUser.uid,
                        senderName = currentUser.name,
                        senderRole = currentUser.role,
                        messageText = msgText,
                        recipientUserId = req.userId,
                        bookTitle = req.title
                    )
                }
            },
            onUpdateStatus = { newStatus, adminComment ->
                libraryViewModel.updateBookRequestStatus(
                    requestId = req.id,
                    newStatus = newStatus,
                    recipientUserId = req.userId,
                    bookTitle = req.title,
                    adminComment = adminComment
                )
            }
        )
    }

    // Sheet de Notificações
    if (showNotifSheet) {
        NotificationsBottomSheet(
            notifications = userNotifications,
            onDismiss = { showNotifSheet = false },
            onMarkAsRead = { notifId ->
                libraryViewModel.markNotificationAsRead(notifId)
            },
            onMarkAllAsRead = {
                if (currentUser != null) {
                    libraryViewModel.markAllNotificationsAsRead(currentUser.uid, currentUser.role == "admin")
                }
            }
        )
    }

    // Sheet de Ver Todas as Solicitações
    if (showAllRequestsSheet) {
        AllRequestsBottomSheet(
            requests = requestsToDisplay,
            currentUser = currentUser,
            onDismiss = { showAllRequestsSheet = false },
            onRequestClick = { req ->
                showAllRequestsSheet = false
                libraryViewModel.selectRequestForChat(req.id)
            }
        )
    }
}

