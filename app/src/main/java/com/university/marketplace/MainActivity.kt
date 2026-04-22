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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.di.DefaultAppContainer
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
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.CreateListingScreen
import com.university.marketplace.ui.home.ListingDetailViewModel
import com.university.marketplace.ui.home.ProductDetailScreen
import com.university.marketplace.ui.profile.ProfileRoute
import com.university.marketplace.ui.theme.JetpackComposeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container =
            (application as? MarketplaceApplication)?.container
                ?: DefaultAppContainer(applicationContext)
        setContent {
            JetpackComposeAppTheme {
                AppNavigation(factory = MarketplaceViewModelFactory(container))
            }
        }
    }
}

@Composable
fun AppNavigation(factory: MarketplaceViewModelFactory) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val connectivityMonitor = remember { AndroidConnectivityMonitor(context.applicationContext) }
    val isOnline by connectivityMonitor.isOnline.collectAsState(initial = connectivityMonitor.isCurrentlyOnline())
    val authRepository = remember { AuthRepositoryFactory.create(context.applicationContext) }
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

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            // Optional: Subtle notification, but don't block UI
            // Toast.makeText(context, "Modo offline", Toast.LENGTH_SHORT).show()
        }
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
                isOnline = isOnline
            )
        }
        composable("profile") {
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
                onUnauthorized = onUnauthorized
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
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                isOnline = isOnline,
                onCreateListing = { _, _, _, _ ->
                    Toast.makeText(context, "Publicación creada correctamente.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
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
                    onBack = { navController.popBackStack() },
                    viewModel = detailViewModel
                )
            }
        }
    }
}
