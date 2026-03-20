package com.university.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.ui.MarketplaceViewModelFactory
import com.university.marketplace.map.MapViewScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.university.marketplace.ui.home.CreateListingScreen
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.ListingDetailViewModel
import com.university.marketplace.ui.home.ProductDetailScreen
import com.university.marketplace.ui.theme.JetpackComposeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MarketplaceApplication).container
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

    NavHost(navController = navController, startDestination = "home") {
        // Home
        composable("home") {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeMarketplaceScreen(
                onNavigateToDetail = { productId ->
                    // Existing navigation was to map, keeping it as requested
                    navController.navigate("map/$productId")
                },
                onNavigateToSell = {
                    navController.navigate("create_listing")
                },
                viewModel = homeViewModel
            )
        }
        // Map
        composable(
            route = "map/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {
                val mapViewModel: MapViewModel = viewModel(factory = factory)
                MapViewScreen(
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
            CreateListingScreen({ navController.popBackStack() })
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
