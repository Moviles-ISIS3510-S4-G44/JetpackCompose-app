package com.university.marketplace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
        val container = (application as? MarketplaceApplication)?.container ?: DefaultAppContainer()
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
    val startDestination = if (authRepository.hasActiveSession()) "home" else "sign_in"
    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }
    val navigateToSignIn: () -> Unit = {
        authRepository.clearSession()
        navController.navigate("sign_in") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(isOnline) {
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
                try {
                    authRepository.getCurrentUser()
                } catch (_: UnauthorizedAuthException) {
                    navigateToSignIn()
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
                    if (isOnline) {
                        navController.navigate("create_listing")
                    } else {
                        Toast.makeText(context, "Cannot create listing while offline", Toast.LENGTH_SHORT).show()
                    }
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
                    if (isOnline) {
                        navController.navigate("create_listing")
                    } else {
                        Toast.makeText(context, "Cannot create listing while offline", Toast.LENGTH_SHORT).show()
                    }
                },
                onLogout = navigateToSignIn,
                onUnauthorized = navigateToSignIn
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
        // Sell
        composable("create_listing") {
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                isOnline = isOnline
            )
        }
        // Detail Product
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
