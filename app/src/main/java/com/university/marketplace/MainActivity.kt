package com.university.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.university.marketplace.ui.home.HomeMarketplaceScreen
import com.university.marketplace.map.MapViewScreen
import com.university.marketplace.ui.theme.JetpackComposeAppTheme
import com.university.marketplace.data.FakeProductRepository
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.university.marketplace.ui.home.CreateListingScreen
import com.university.marketplace.ui.home.ProductDetailScreen

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
    val navController = rememberNavController()
    // Keeping this for other screens that still use the domain Product model for now
    val repository = FakeProductRepository()

    NavHost(navController = navController, startDestination = "home") {
        // Home
        composable("home") {
            HomeMarketplaceScreen(
                onNavigateToDetail = { productId ->
                    // Existing navigation was to map, keeping it as requested
                    navController.navigate("map/$productId")
                },
                onNavigateToSell = {
                    navController.navigate("create_listing")
                }
            )
        }
        // Map
        composable(
            route = "map/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {
                MapViewScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate("product_detail/$id")
                    }
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
                ProductDetailScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
