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
    val repository = FakeProductRepository()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeMarketplaceScreen(
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
            val product = repository.getProducts().find { it.id == productId }
            
            if (product != null) {
                MapViewScreen(
                    product = product,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
