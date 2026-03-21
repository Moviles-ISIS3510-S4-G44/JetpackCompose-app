package com.university.marketplace.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.auth.UnauthorizedAuthException
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.home.MarketplaceBottomNavigation
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

@Composable
fun ProfileRoute(
    authRepository: AuthRepository,
    isOnline: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateSell: () -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit
) {
    var user by remember { mutableStateOf<AuthenticatedUser?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            isLoading = false
            if (user == null) {
                errorMessage = "You are offline. Connect to the internet to load your profile."
            }
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        try {
            user = authRepository.getCurrentUser()
        } catch (_: UnauthorizedAuthException) {
            onUnauthorized()
        } catch (error: AuthException) {
            errorMessage = error.message ?: "We could not load your profile."
        } finally {
            isLoading = false
        }
    }

    ProfileScreen(
        isOnline = isOnline,
        user = user,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onNavigateHome = onNavigateHome,
        onNavigateSell = onNavigateSell,
        onLogout = onLogout
    )
}

@Composable
private fun ProfileScreen(
    isOnline: Boolean,
    user: AuthenticatedUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateHome: () -> Unit,
    onNavigateSell: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val offlineBannerController = rememberOfflineBannerController(isOnline)

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Log out") },
            text = { Text(text = "Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(text = "Log out", color = MarketplaceDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            MarketplaceBottomNavigation(
                currentRoute = "profile",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateHome()
                        "create_listing" -> onNavigateSell()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MarketplaceBackground)
                .padding(padding)
        ) {
            OfflineBanner(
                isOnline = isOnline,
                offlineBannerController = offlineBannerController
            )
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MarketplaceDark
                        )
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MarketplaceDark.copy(alpha = 0.6f)
                            )
                            if (isOnline) {
                                Button(
                                    onClick = onLogout,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MarketplaceYellow,
                                        contentColor = MarketplaceDark
                                    )
                                ) {
                                    Text(text = "Go to sign in", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    user != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MarketplaceDark
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                            .background(MarketplaceYellow.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MarketplaceDark,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MarketplaceDark
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MarketplaceDark.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    ProfileInfoRow(
                                        label = "Account ID",
                                        value = user.id,
                                        monospaceValue = true
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    ProfileInfoRow(label = "Rating", value = user.rating.toString())
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "Session",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MarketplaceDark
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "You are signed in with your university account.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MarketplaceDark.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { showLogoutDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MarketplaceYellow,
                                            contentColor = MarketplaceDark
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(text = "Log out", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    monospaceValue: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MarketplaceDark.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (monospaceValue) FontFamily.Monospace else FontFamily.Default,
            color = MarketplaceDark
        )
    }
}
