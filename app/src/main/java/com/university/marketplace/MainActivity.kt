package com.university.marketplace

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.marketplace.connectivity.AndroidConnectivityMonitor
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepositoryFactory
import com.university.marketplace.data.auth.UnauthorizedAuthException
import com.university.marketplace.di.DefaultAppContainer
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.map.MapViewScreen
import com.university.marketplace.ui.MarketplaceViewModelFactory
import com.university.marketplace.ui.auth.AuthViewModel
import com.university.marketplace.ui.auth.AuthViewModelFactory
import com.university.marketplace.ui.auth.SignInScreen
import com.university.marketplace.ui.auth.SignUpScreen
import com.university.marketplace.ui.home.CreateListingScreen
import com.university.marketplace.ui.home.CreateListingViewModel
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.ListingDetailViewModel
import com.university.marketplace.ui.home.ProductDetailScreen
import com.university.marketplace.ui.favorites.FavoritesScreen
import com.university.marketplace.ui.favorites.FavoritesViewModel
import com.university.marketplace.ui.profile.MyListingsViewModel
import com.university.marketplace.ui.profile.ProfileRoute
import com.university.marketplace.ui.profile.OtherUserProfileScreen
import com.university.marketplace.ui.profile.OtherUserProfileViewModel
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.ui.chat.ChatScreen
import com.university.marketplace.ui.chat.ChatViewModel
import com.university.marketplace.ui.chat.ChatViewModelFactory
import com.university.marketplace.ui.chat.ConversationListScreen
import com.university.marketplace.ui.chat.ConversationListViewModel
import com.university.marketplace.ui.purchases.PurchaseHistoryScreen
import com.university.marketplace.ui.purchases.PurchaseHistoryViewModel
import com.university.marketplace.ui.purchases.SalesHistoryScreen
import com.university.marketplace.ui.purchases.SalesHistoryViewModel
import com.university.marketplace.ui.theme.JetpackComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.data.api.NetworkModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container =
            (application as? MarketplaceApplication)?.container
                ?: DefaultAppContainer(applicationContext)
        setContent {
            JetpackComposeAppTheme {
                AppNavigation(container = container)
            }
        }
    }
}

@Composable
fun AppNavigation(container: com.university.marketplace.di.AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val connectivityMonitor = remember { AndroidConnectivityMonitor(context.applicationContext) }
    val isOnline by connectivityMonitor.isOnline.collectAsState(initial = connectivityMonitor.isCurrentlyOnline())
    val authRepository = remember { AuthRepositoryFactory.create(context.applicationContext) }
    val session by authRepository.sessionFlow.collectAsState(initial = null)
    val factory = remember(container, authRepository) { MarketplaceViewModelFactory(container, authRepository) }
    val coroutineScope = rememberCoroutineScope()
    val startDestination = if (authRepository.hasActiveSession()) "home" else "sign_in"
    
    val goToSignIn: () -> Unit = {
        navController.navigate("sign_in") {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    var uiMessage by rememberSaveable { mutableStateOf<String?>(null) }
    
    LaunchedEffect(session) {
        if (session == null) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != "sign_in" && currentRoute != "sign_up") {
                uiMessage = "Tu sesion ha expirado. Por favor inicia sesion nuevamente."
                goToSignIn()
            }
        }
    }
    
    val onUnauthorized: () -> Unit = {
        authRepository.clearSession()
    }
    
    val onLogout: () -> Unit = {
        coroutineScope.launch {
            runCatching { authRepository.logout() }
            goToSignIn()
        }
    }

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    var lastNotifiedOnline by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var uiMessageIsOnline by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var lastOfflineAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(isOnline) {
        val previous = lastNotifiedOnline
        lastNotifiedOnline = isOnline
        if (previous == null || previous == isOnline) return@LaunchedEffect

        val now = System.currentTimeMillis()
        if (!isOnline) {
            lastOfflineAt = now
            uiMessageIsOnline = false
            uiMessage = "Sin conexión. Algunas acciones requieren internet."
        } else {
            val offlineDuration = lastOfflineAt?.let { now - it } ?: 0L
            lastOfflineAt = null
            if (offlineDuration >= 2000L) {
                uiMessageIsOnline = true
                uiMessage = "Conexión restablecida"
            }
        }
    }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            delay(2800)
            uiMessage = null
        }
    }

    Box {
        NavHost(navController = navController, startDestination = startDestination) {
        composable("sign_in") {
            val authViewModel = viewModel<AuthViewModel>(
                factory = AuthViewModelFactory(authRepository, container.locationRepository, container.favoriteRepository)
            )
            SignInScreen(
                isOnline = isOnline,
                viewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate("sign_up")
                },
                onAuthenticated = {
                    navController.navigate("home") {
                        popUpTo("sign_in") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("sign_up") {
            val authViewModel = viewModel<AuthViewModel>(
                factory = AuthViewModelFactory(authRepository, container.locationRepository, container.favoriteRepository)
            )
            SignUpScreen(
                isOnline = isOnline,
                viewModel = authViewModel,
                onNavigateToSignIn = {
                    navController.popBackStack()
                },
                onAuthenticated = {
                    navController.navigate("home") {
                        popUpTo("sign_in") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("home") {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            LaunchedEffect(Unit) {
                if (!isOnline) return@LaunchedEffect
                try {
                    authRepository.getCurrentUser()
                } catch (_: UnauthorizedAuthException) {
                    onUnauthorized()
                } catch (error: AuthException) {
                    Log.w("MainActivity", "Failed to verify session on home.", error)
                } catch (error: Throwable) {
                    Log.w("MainActivity", "Unexpected error verifying session on home.", error)
                }
            }
            HomeMarketplaceScreen(
                viewModel = homeViewModel,
                onNavigateToProfile = {
                    navigateToTopLevel("profile")
                },
                onNavigateToDetail = { listingId ->
                    navController.navigate("map/$listingId")
                },
                onNavigateToSell = {
                    navController.navigate("create_listing")
                },
                onNavigateToPurchases = {
                    navigateToTopLevel("purchase_history")
                },
                onNavigateToMessages = {
                    navigateToTopLevel("conversations")
                },
                onNavigateToFavorites = {
                    navController.navigate("favorites")
                },
                isOnline = isOnline
            )
        }
        composable("profile") {
            val myListingsViewModel: MyListingsViewModel = viewModel(factory = factory)
            ProfileRoute(
                authRepository = authRepository,
                isOnline = isOnline,
                onNavigateHome = {
                    navigateToTopLevel("home")
                },
                onNavigateSell = {
                    navController.navigate("create_listing")
                },
                onLogout = onLogout,
                onUnauthorized = onUnauthorized,
                myListingsViewModel = myListingsViewModel,
                onNavigateToDetail = { id -> navController.navigate("product_detail/$id") },
                onNavigateToSales = { navController.navigate("sales_history") },
                onNavigateToPurchases = { navigateToTopLevel("purchase_history") },
                onNavigateToMessages = { navigateToTopLevel("conversations") }
            )
        }
        composable(
            route = "map/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {
                val mapViewModel: MapViewModel = viewModel(factory = factory)
                MapViewScreen(
                    isOnline = isOnline,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate("product_detail/$id")
                    },
                    viewModel = mapViewModel
                )
            }
        }
        composable("create_listing") {
            val createListingViewModel: CreateListingViewModel = viewModel(factory = factory)
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                isOnline = isOnline,
                viewModel = createListingViewModel
            )
        }
        composable(
            route = "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {
                val detailViewModel: ListingDetailViewModel = viewModel(factory = factory)
                ProductDetailScreen(
                    productId = productId,
                    isOnline = isOnline,
                    onBack = { navController.popBackStack() },
                    onMessageSeller = if (isOnline) {
                        {
                            coroutineScope.launch {
                                try {
                                    val conv = container.chatRepository.getOrCreateConversation(productId)
                                    navController.navigate(
                                        "chat/${conv.id}?name=${android.net.Uri.encode(conv.otherUserName)}"
                                    )
                                } catch (_: Exception) {
                                    uiMessage = "No fue posible abrir el chat en este momento"
                                }
                            }
                        }
                    } else null,
                    onNavigateToSellerProfile = { sellerId, sellerName ->
                        navController.navigate("other_profile/$sellerId?sellerName=${android.net.Uri.encode(sellerName)}")
                    },
                    viewModel = detailViewModel
                )
            }
        }
        composable(
            route = "other_profile/{sellerId}?sellerName={sellerName}",
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("sellerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            val sellerName = backStackEntry.arguments?.getString("sellerName") ?: ""
            val otherUserProfileViewModel: OtherUserProfileViewModel = viewModel(factory = factory)
            
            OtherUserProfileScreen(
                sellerId = sellerId,
                sellerName = sellerName,
                viewModel = otherUserProfileViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate("product_detail/$id") }
            )
        }
        composable("favorites") {
            val favoritesViewModel: FavoritesViewModel = viewModel(factory = factory)
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { id -> navController.navigate("product_detail/$id") },
                viewModel = favoritesViewModel
            )
        }
        composable("purchase_history") {
            val purchaseHistoryViewModel: PurchaseHistoryViewModel = viewModel(factory = factory)
            PurchaseHistoryScreen(
                isOnline = isOnline,
                onBack = { navController.popBackStack() },
                viewModel = purchaseHistoryViewModel
            )
        }
        composable("sales_history") {
            val salesHistoryViewModel: SalesHistoryViewModel = viewModel(factory = factory)
            SalesHistoryScreen(
                isOnline = isOnline,
                onBack = { navController.popBackStack() },
                viewModel = salesHistoryViewModel
            )
        }
        composable("conversations") {
            val conversationListViewModel: ConversationListViewModel = viewModel(factory = factory)
            ConversationListScreen(
                isOnline = isOnline,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId, otherUserName ->
                    navController.navigate("chat/$conversationId?name=${android.net.Uri.encode(otherUserName)}")
                },
                viewModel = conversationListViewModel
            )
        }
        composable(
            route = "chat/{conversationId}?name={name}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("name") ?: ""
            
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(
                    chatRepository = container.chatRepository,
                    wsClient = NetworkModule.createChatWebSocketClient(),
                    conversationId = conversationId,
                    token = NetworkModule.authSessionStorage.getAccessToken() ?: "",
                    currentUserId = NetworkModule.authSessionStorage.getCurrentUserId() ?: ""
                )
            )
            ChatScreen(
                otherUserName = otherUserName,
                onBack = { navController.popBackStack() },
                viewModel = chatViewModel
            )
        }
        }

        AnimatedVisibility(
            visible = uiMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            val isOnlineMessage = uiMessageIsOnline == true
            val bannerColor = if (isOnlineMessage) {
                Color(0xFF0F766E)
            } else {
                Color(0xFFB45309)
            }
            val bannerIcon = if (isOnlineMessage) Icons.Filled.Wifi else Icons.Filled.WifiOff

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = bannerColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = bannerIcon,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
                    Text(
                        text = uiMessage.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
