package com.university.marketplace.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceYellow

const val DefaultOfflineBannerMessage = "You are offline. This app requires an internet connection for this feature."

@Stable
class OfflineBannerController {
    var isDismissed by mutableStateOf(false)
        private set

    fun show() {
        isDismissed = false
    }

    fun dismiss() {
        isDismissed = true
    }

    fun reset() {
        isDismissed = false
    }
}

@Composable
fun rememberOfflineBannerController(isOnline: Boolean): OfflineBannerController {
    val controller = remember { OfflineBannerController() }
    var previousIsOnline by rememberSaveable { mutableStateOf(isOnline) }

    LaunchedEffect(isOnline) {
        if (!isOnline && previousIsOnline) {
            controller.show()
        }
        if (isOnline) {
            controller.reset()
        }
        previousIsOnline = isOnline
    }

    return controller
}

fun runWhenOnline(
    isOnline: Boolean,
    offlineBannerController: OfflineBannerController,
    action: () -> Unit
) {
    if (isOnline) {
        action()
    } else {
        offlineBannerController.show()
    }
}

@Composable
fun OfflineBanner(
    isOnline: Boolean,
    offlineBannerController: OfflineBannerController,
    modifier: Modifier = Modifier,
    message: String = DefaultOfflineBannerMessage
) {
    if (isOnline || offlineBannerController.isDismissed) {
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MarketplaceYellow)
            .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MarketplaceDark,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = offlineBannerController::dismiss,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss offline message",
                tint = MarketplaceDark,
                modifier = Modifier
                    .padding(2.dp)
            )
        }
    }
}
