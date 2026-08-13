package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.BibliotecaDigitalTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LibraryViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home_tab", "Início", Icons.Default.Home)
    object Favorites : BottomNavItem("favorites_tab", "Favoritos", Icons.Default.Favorite)
    object Offline : BottomNavItem("offline_tab", "Offline", Icons.Default.DownloadForOffline)
    object Profile : BottomNavItem("profile_tab", "Perfil", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkBookId = intent?.data?.lastPathSegment?.takeIf { it.isNotBlank() && it != "book" }

        setContent {
            val isDarkTheme by libraryViewModel.isDarkTheme.collectAsState()
            val toastMessage by libraryViewModel.toastMessage.collectAsState()

            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    libraryViewModel.clearToast()
                }
            }

            BibliotecaDigitalTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    authViewModel = authViewModel,
                    libraryViewModel = libraryViewModel,
                    deepLinkBookId = deepLinkBookId
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    libraryViewModel: LibraryViewModel,
    deepLinkBookId: String? = null
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // O aplicativo SEMPRE inicia com a Splash Screen animada ao abrir
    val startDestination = "splash"

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(authState) {
        // Não redireciona enquanto a Splash Screen estiver em exibição inicial
        if (currentDestination != "splash") {
            if (authState is AuthState.Authenticated) {
                navController.navigate("main_container") {
                    popUpTo(0) { inclusive = true }
                }
                if (!deepLinkBookId.isNullOrBlank()) {
                    navController.navigate("book_detail/$deepLinkBookId")
                }
            } else if (authState is AuthState.Idle) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    val targetRoute = if (authState is AuthState.Authenticated) "main_container" else "login"
                    navController.navigate(targetRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                    if (authState is AuthState.Authenticated && !deepLinkBookId.isNullOrBlank()) {
                        navController.navigate("book_detail/$deepLinkBookId")
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignup = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignupScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("main_container") {
            MainContainerScreen(
                currentUser = currentUser,
                authViewModel = authViewModel,
                libraryViewModel = libraryViewModel,
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onOpenPdfReader = { bookId -> navController.navigate("pdf_reader/$bookId") },
                onNavigateAdminAdd = { navController.navigate("admin_add_book") },
                onNavigateAdminManage = { navController.navigate("admin_manage_books") },
                onNavigateAdminUsers = { navController.navigate("admin_users") }
            )
        }

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                bookId = bookId,
                currentUser = currentUser,
                libraryViewModel = libraryViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenPdfReader = { id -> navController.navigate("pdf_reader/$id") }
            )
        }

        composable(
            route = "pdf_reader/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            PdfReaderScreen(
                bookId = bookId,
                libraryViewModel = libraryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("admin_add_book") {
            AdminAddBookScreen(
                libraryViewModel = libraryViewModel,
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.popBackStack()
                }
            )
        }

        composable("admin_manage_books") {
            AdminManageBooksScreen(
                libraryViewModel = libraryViewModel,
                onBackClick = { navController.popBackStack() },
                onAddNewBookClick = { navController.navigate("admin_add_book") },
                onLogout = {
                    authViewModel.logout()
                    navController.popBackStack()
                }
            )
        }

        composable("admin_users") {
            AdminUsersScreen(
                libraryViewModel = libraryViewModel,
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    currentUser: com.example.data.local.entity.UserEntity?,
    authViewModel: AuthViewModel,
    libraryViewModel: LibraryViewModel,
    onBookClick: (String) -> Unit,
    onOpenPdfReader: (String) -> Unit,
    onNavigateAdminAdd: () -> Unit,
    onNavigateAdminManage: () -> Unit,
    onNavigateAdminUsers: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavItem.Home.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Favorites,
        BottomNavItem.Offline,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    currentUser = currentUser,
                    libraryViewModel = libraryViewModel,
                    onBookClick = onBookClick,
                    onNavigateAdminAdd = onNavigateAdminAdd,
                    onLogout = { authViewModel.logout() }
                )
            }

            composable(BottomNavItem.Favorites.route) {
                FavoritesScreen(
                    libraryViewModel = libraryViewModel,
                    onBookClick = onBookClick
                )
            }

            composable(BottomNavItem.Offline.route) {
                OfflineBooksScreen(
                    libraryViewModel = libraryViewModel,
                    onOpenPdfReader = onOpenPdfReader
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    currentUser = currentUser,
                    authViewModel = authViewModel,
                    libraryViewModel = libraryViewModel,
                    onNavigateAdminAdd = onNavigateAdminAdd,
                    onNavigateAdminManage = onNavigateAdminManage,
                    onNavigateAdminUsers = onNavigateAdminUsers
                )
            }
        }
    }
}
