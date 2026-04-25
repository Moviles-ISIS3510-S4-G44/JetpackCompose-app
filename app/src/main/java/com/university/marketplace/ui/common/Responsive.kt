package com.university.marketplace.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns true when the current window is at least [threshold] wide in density-independent pixels.
 * Used to switch screens to a two-pane / wider layout without relying on orientation alone so that
 * tablets in portrait also benefit, and rotation on a phone still falls back to the single-column
 * layout when appropriate.
 */
@Composable
fun isWideScreen(threshold: Dp = 600.dp): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp >= threshold
}
