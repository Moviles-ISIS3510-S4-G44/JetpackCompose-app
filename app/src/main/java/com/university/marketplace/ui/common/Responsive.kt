package com.university.marketplace.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun isWideScreen(threshold: Dp = 600.dp): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp >= threshold
}
