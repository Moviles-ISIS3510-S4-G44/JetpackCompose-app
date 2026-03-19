package com.university.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.data.FakeProductRepository
import com.university.marketplace.data.location.AndroidLocationRepository
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.map.MapViewModelFactory
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.HomeViewModelFactory
import com.university.marketplace.map.MapViewScreen
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
    val locationRepository = remember { AndroidLocationRepository(context.applicationContext) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val homeViewModel = viewModel<HomeViewModel>(
                factory = HomeViewModelFactory(productRepository)
            )
            HomeMarketplaceScreen(
                viewModel = homeViewModel,
                onNavigateToMap = { product ->
                    navController.navigate("map/${product.id}")
                }
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
                    viewModel = mapViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
