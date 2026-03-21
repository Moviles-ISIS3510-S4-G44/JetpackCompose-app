package com.university.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.connectivity.AndroidConnectivityMonitor
import com.university.marketplace.data.FakeProductRepository
import com.university.marketplace.data.auth.AuthRepositoryFactory
import com.university.marketplace.data.auth.UnauthorizedAuthException
import com.university.marketplace.data.location.AndroidLocationRepository
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.map.MapViewModelFactory
import com.university.marketplace.map.MapViewScreen
import com.university.marketplace.ui.auth.AuthViewModel
import com.university.marketplace.ui.auth.AuthViewModelFactory
import com.university.marketplace.ui.auth.SignInScreen
import com.university.marketplace.ui.auth.SignUpScreen
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.HomeViewModelFactory
import com.university.marketplace.ui.profile.ProfileRoute
import com.university.marketplace.ui.theme.JetpackComposeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackComposeAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val productRepository = remember { FakeProductRepository() }
    val authRepository = remember { AuthRepositoryFactory.create(context.applicationContext) }
    val locationRepository = remember { AndroidLocationRepository(context.applicationContext) }
    val connectivityMonitor = remember { AndroidConnectivityMonitor(context.applicationContext) }
    val isOnline by connectivityMonitor.isOnline.collectAsState(
        initial = connectivityMonitor.isCurrentlyOnline()
    )
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
            val homeViewModel = viewModel<HomeViewModel>(
                factory = HomeViewModelFactory(productRepository)
            )
            LaunchedEffect(isOnline) {
                if (!isOnline) {
                    return@LaunchedEffect
                }
                try {
                    authRepository.getCurrentUser()
                } catch (_: UnauthorizedAuthException) {
                    navigateToSignIn()
                }
            }
            HomeMarketplaceScreen(
                isOnline = isOnline,
                viewModel = homeViewModel,
                onNavigateToProfile = {
                    navigateToTopLevel("profile")
                },
                onNavigateToMap = { product ->
                    navController.navigate("map/${product.id}")
                }
            )
        }
        composable("profile") {
            ProfileRoute(
                authRepository = authRepository,
                isOnline = isOnline,
                onNavigateHome = {
                    navigateToTopLevel("home")
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
                val mapViewModel = viewModel<MapViewModel>(
                    factory = MapViewModelFactory(
                        productId = productId,
                        productRepository = productRepository,
                        locationRepository = locationRepository
                    )
                )
                MapViewScreen(
                    isOnline = isOnline,
                    viewModel = mapViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
