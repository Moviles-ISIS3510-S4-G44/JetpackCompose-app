package com.university.marketplace.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.university.marketplace.ui.common.toUserFriendlyMessage
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.MarketplaceBottomNavigation
import com.university.marketplace.ui.theme.*

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
    onNavigateToDetail: ((String) -> Unit)? = null,
    onNavigateToSales: (() -> Unit)? = null
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
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSales = onNavigateToSales
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
    onNavigateToDetail: ((String) -> Unit)? = null,
    onNavigateToSales: (() -> Unit)? = null
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
                            color = MarketplaceYellow
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
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToSales = onNavigateToSales
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
    onNavigateToDetail: ((String) -> Unit)? = null,
    onNavigateToSales: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            ProfileHeader(user = user)
        }

        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileSummaryCard(user = user)
                
                if (onNavigateToSales != null) {
                    ProfileOptionItem(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        title = "My Sales",
                        subtitle = "Track your items sold",
                        onClick = onNavigateToSales
                    )
                }

                ProfileOptionItem(
                    icon = Icons.Default.Logout,
                    title = "Log Out",
                    subtitle = "Sign out from your account",
                    titleColor = Color.Red,
                    onClick = onLogoutRequested
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "My Listings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MarketplaceDark
                )
            }
        }

        when (myListingsUiState) {
            is MyListingsUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MarketplaceYellow)
                    }
                }
            }
            is MyListingsUiState.Success -> {
                if (myListingsUiState.listings.isEmpty()) {
                    item {
                        EmptyListingsMessage()
                    }
                } else {
                    items(myListingsUiState.listings) { listing ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            MyListingItem(
                                listing = listing,
                                onClick = { onNavigateToDetail?.invoke(listing.id) }
                            )
                        }
                    }
                }
            }
            is MyListingsUiState.Error -> {
                item {
                    Text(
                        text = myListingsUiState.message,
                        color = Color.Red,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ProfileHeader(user: AuthenticatedUser) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, MarketplaceBackground)
                )
            )
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MarketplaceWhite)
                    .border(4.dp, MarketplaceWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MarketplaceYellow
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark
            )
        }
    }
}

@Composable
private fun ProfileSummaryCard(user: AuthenticatedUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.3f))
            InfoRow(
                icon = Icons.Default.Star,
                label = "Seller Rating",
                value = "${user.rating}/5"
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MarketplaceBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MarketplaceYellow)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MarketplaceDark,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MarketplaceWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MarketplaceBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (titleColor == Color.Red) Color.Red else MarketplaceYellow)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = titleColor)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun MyListingItem(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = listing.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listing.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = "$${listing.price.toInt()}", color = MarketplaceYellow, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MarketplaceBackground,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = listing.condition.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun EmptyListingsMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You haven't posted anything yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
