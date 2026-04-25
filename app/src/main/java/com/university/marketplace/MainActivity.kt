package com.university.marketplace

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.di.DefaultAppContainer
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepositoryFactory
import com.university.marketplace.data.auth.UnauthorizedAuthException
import com.university.marketplace.connectivity.AndroidConnectivityMonitor
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.map.MapViewScreen
import com.university.marketplace.ui.auth.AuthViewModel
import com.university.marketplace.ui.auth.AuthViewModelFactory
import com.university.marketplace.ui.auth.SignInScreen
import com.university.marketplace.ui.auth.SignUpScreen
import com.university.marketplace.ui.MarketplaceViewModelFactory
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.ui.home.CreateListingScreen
import com.university.marketplace.ui.home.CreateListingViewModel
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.ListingDetailViewModel
import com.university.marketplace.ui.home.ProductDetailScreen
import com.university.marketplace.ui.profile.MyListingsViewModel
import com.university.marketplace.ui.profile.ProfileRoute
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.ui.chat.ChatScreen
import com.university.marketplace.ui.chat.ChatViewModelFactory
import com.university.marketplace.ui.chat.ChatViewModel
import com.university.marketplace.ui.chat.ConversationListScreen
import com.university.marketplace.ui.chat.ConversationListViewModel
import com.university.marketplace.ui.purchases.PurchaseHistoryScreen
import com.university.marketplace.ui.purchases.PurchaseHistoryViewModel
import com.university.marketplace.ui.purchases.SalesHistoryScreen
import com.university.marketplace.ui.purchases.SalesHistoryViewModel
import com.university.marketplace.ui.theme.JetpackComposeAppTheme

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
    val factory = remember(container, authRepository) { MarketplaceViewModelFactory(container, authRepository) }
    val coroutineScope = rememberCoroutineScope()
    val startDestination = if (authRepository.hasActiveSession()) "home" else "sign_in"
    
    val goToSignIn: () -> Unit = {
        navController.navigate("sign_in") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
    
    val onUnauthorized: () -> Unit = {
        authRepository.clearSession()
        goToSignIn()
    }
    
    val onLogout: () -> Unit = {
        coroutineScope.launch {
            runCatching { authRepository.logout() }
            goToSignIn()
        }
    }

    var lastNotifiedOnline by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isOnline) {
        val previous = lastNotifiedOnline
        lastNotifiedOnline = isOnline
        if (previous == null || previous == isOnline) return@LaunchedEffect
        val message = if (isOnline) "Connection restored" else "No internet connection"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("sign_in") {
            val authViewModel = viewModel<AuthViewModel>(
                factory = AuthViewModelFactory(authRepository)
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
                factory = AuthViewModelFactory(authRepository)
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
                    // Restoration of Map flow: Navigate to Map first as requested
                    navController.navigate("map/$listingId")
                },
                onNavigateToSell = {
                    // Allowed even offline (Eventual connectivity)
                    navController.navigate("create_listing")
                },
                onNavigateToPurchases = {
                    navController.navigate("purchase_history")
                },
                onNavigateToMessages = {
                    navController.navigate("conversations")
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
                onNavigateToSales = { navController.navigate("sales_history") }
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
                                    Toast.makeText(context, "Could not start conversation", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else null,
                    viewModel = detailViewModel
                )
            }
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
                    navController.navigate(
                        "chat/$conversationId?name=${android.net.Uri.encode(otherUserName)}"
                    )
                },
                viewModel = conversationListViewModel
            )
        }
        composable(
            route = "chat/{conversationId}?name={otherUserName}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUserName") {
                    type = NavType.StringType
                    defaultValue = "Chat"
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "Chat"
            val token = remember { NetworkModule.authSessionStorage.getAccessToken().orEmpty() }
            val currentUserId = remember { NetworkModule.authSessionStorage.getCurrentUserId().orEmpty() }
            val chatVmFactory = remember(conversationId) {
                ChatViewModelFactory(
                    chatRepository = container.chatRepository,
                    wsClient = NetworkModule.createChatWebSocketClient(),
                    conversationId = conversationId,
                    token = token,
                    currentUserId = currentUserId
                )
            }
            val chatViewModel: ChatViewModel = viewModel(factory = chatVmFactory)
            ChatScreen(
                otherUserName = otherUserName,
                onBack = { navController.popBackStack() },
                viewModel = chatViewModel
            )
        }
    }
}
