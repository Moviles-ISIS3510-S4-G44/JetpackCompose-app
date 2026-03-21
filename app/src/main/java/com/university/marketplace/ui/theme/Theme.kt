package com.university.marketplace.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MarketplaceYellow,
    onPrimary = MarketplaceDark,
    secondary = MarketplaceDarkSecondary,
    onSecondary = MarketplaceWhite,
    background = MarketplaceBackground,
    onBackground = MarketplaceDark,
    surface = MarketplaceWhite,
    onSurface = MarketplaceDark,
    surfaceVariant = MarketplaceBackground,
    onSurfaceVariant = MarketplaceGray
)

@Composable
fun JetpackComposeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
