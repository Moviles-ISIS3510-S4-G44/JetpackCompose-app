package com.university.marketplace.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.auth.UnauthorizedAuthException
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.isWideScreen
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.MarketplaceBottomNavigation
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

private val AuthenticatedUserSaver = listSaver<AuthenticatedUser?, Any>(
    save = { user ->
        user?.let { listOf(it.id, it.name, it.email, it.rating) } ?: emptyList()
    },
    restore = { list ->
        if (list.size == 4) {
            AuthenticatedUser(
                id = list[0] as String,
                name = list[1] as String,
                email = list[2] as String,
                rating = list[3] as Int
            )
        } else {
            null
        }
    }
)

@Composable
fun ProfileRoute(
    authRepository: AuthRepository,
    isOnline: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateSell: () -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit,
    myListingsViewModel: MyListingsViewModel? = null,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    var user by rememberSaveable(stateSaver = AuthenticatedUserSaver) {
        mutableStateOf<AuthenticatedUser?>(null)
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            isLoading = false
            if (user == null) {
                errorMessage = "You are offline. Connect to the internet to load your profile."
            }
            return@LaunchedEffect
        }

        if (user != null) {
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        try {
            user = authRepository.getCurrentUser()
        } catch (_: UnauthorizedAuthException) {
            onUnauthorized()
        } catch (error: AuthException) {
            errorMessage = error.toUserFriendlyMessage()
        } catch (error: Throwable) {
            errorMessage = error.toUserFriendlyMessage()
        } finally {
            isLoading = false
        }
    }

    val myListingsUiState by (myListingsViewModel?.uiState?.collectAsState()
        ?: androidx.compose.runtime.remember { mutableStateOf(MyListingsUiState.Empty) })

    LaunchedEffect(isOnline) {
        if (isOnline) myListingsViewModel?.load()
    }

    ProfileScreen(
        isOnline = isOnline,
        user = user,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onNavigateHome = onNavigateHome,
        onNavigateSell = onNavigateSell,
        onLogout = onLogout,
        myListingsUiState = myListingsUiState,
        onNavigateToDetail = onNavigateToDetail
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
    onLogout: () -> Unit,
    myListingsUiState: MyListingsUiState = MyListingsUiState.Empty,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
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
                        ProfileContent(
                            user = user,
                            onLogoutRequested = { showLogoutDialog = true },
                            myListingsUiState = myListingsUiState,
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: AuthenticatedUser,
    onLogoutRequested: () -> Unit,
    myListingsUiState: MyListingsUiState = MyListingsUiState.Empty,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val wide = isWideScreen()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark
            )

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ProfileSummaryCard(
                        user = user,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileSessionCard(
                        onLogoutRequested = onLogoutRequested,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ProfileSummaryCard(
                    user = user,
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileSessionCard(
                    onLogoutRequested = onLogoutRequested,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MyListingsSection(
                uiState = myListingsUiState,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    user: AuthenticatedUser,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
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
}

@Composable
private fun ProfileSessionCard(
    onLogoutRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
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
                onClick = onLogoutRequested,
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

@Composable
private fun MyListingsSection(
    uiState: MyListingsUiState,
    onNavigateToDetail: ((String) -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "My Listings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState) {
                is MyListingsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                        color = MarketplaceDark
                    )
                }
                is MyListingsUiState.Empty -> {
                    Text(
                        text = "You have no listings yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MarketplaceDark.copy(alpha = 0.6f)
                    )
                }
                is MyListingsUiState.Error -> {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
                is MyListingsUiState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.listings.forEach { listing ->
                            MyListingRow(
                                listing = listing,
                                onClick = { onNavigateToDetail?.invoke(listing.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListingRow(listing: ListingUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = listing.imageUrl,
            contentDescription = listing.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$${listing.price.toInt()}",
                color = MarketplaceYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Surface(
            color = Color(0xFFF1F1F1),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = listing.condition.uppercase(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark
            )
        }
    }
}
