package com.university.marketplace.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.common.runWhenOnline
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceGray
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow
import kotlinx.coroutines.launch

private val AuthSupportingTextColor = MarketplaceDark.copy(alpha = 0.68f)
private val AuthPlaceholderTextColor = MarketplaceDark.copy(alpha = 0.42f)
private val AuthDividerColor = MarketplaceDark.copy(alpha = 0.16f)
private val AuthBorderColor = MarketplaceDark.copy(alpha = 0.22f)
private val AuthDecorativeHeaderColor = MarketplaceDark.copy(alpha = 0.1f)

@Composable
fun SignInScreen(
    isOnline: Boolean,
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onAuthenticated: () -> Unit
) {
    AuthRoute(
        isOnline = isOnline,
        title = "Login",
        subtitle = "Access your academic marketplace dashboard.",
        primaryActionLabel = "LOGIN",
        secondaryPrompt = "Don't have an account?",
        secondaryActionLabel = "Register",
        showNameField = false,
        viewModel = viewModel,
        onPrimaryAction = { name, email, password, persistSession ->
            viewModel.signIn(
                email = email,
                password = password,
                persistSession = persistSession
            )
        },
        onSecondaryAction = onNavigateToSignUp,
        onAuthenticated = onAuthenticated
    )
}

@Composable
fun SignUpScreen(
    isOnline: Boolean,
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    onAuthenticated: () -> Unit
) {
    AuthRoute(
        isOnline = isOnline,
        title = "Create account",
        subtitle = "Join the academic marketplace with your university email.",
        primaryActionLabel = "SIGN UP",
        secondaryPrompt = "Already have an account?",
        secondaryActionLabel = "Login",
        showNameField = true,
        viewModel = viewModel,
        onPrimaryAction = { name, email, password, persistSession ->
            viewModel.signUp(
                name = name,
                email = email,
                password = password,
                persistSession = persistSession
            )
        },
        onSecondaryAction = onNavigateToSignIn,
        onAuthenticated = onAuthenticated
    )
}

@Composable
private fun AuthRoute(
    isOnline: Boolean,
    title: String,
    subtitle: String,
    primaryActionLabel: String,
    secondaryPrompt: String,
    secondaryActionLabel: String,
    showNameField: Boolean,
    viewModel: AuthViewModel,
    onPrimaryAction: (String, String, String, Boolean) -> Unit,
    onSecondaryAction: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val offlineBannerController = rememberOfflineBannerController(isOnline)

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var persistSession by rememberSaveable { mutableStateOf(true) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.authenticatedUser?.id) {
        if (uiState.authenticatedUser != null) {
            onAuthenticated()
            viewModel.consumeAuthentication()
        }
    }

    Scaffold(
        containerColor = MarketplaceWhite,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MarketplaceWhite)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                BrandHeader()
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MarketplaceDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuthSupportingTextColor
                )
                Spacer(modifier = Modifier.height(28.dp))
                OfflineBanner(
                    isOnline = isOnline,
                    offlineBannerController = offlineBannerController
                )
                if (!isOnline && !offlineBannerController.isDismissed) {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (showNameField) {
                    AuthFieldLabel(text = "FULL NAME")
                    OutlinedAuthField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationMessage = null
                        },
                        placeholder = "Alex Johnson",
                        keyboardType = KeyboardType.Text
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                AuthFieldLabel(text = "UNIVERSITY EMAIL")
                OutlinedAuthField(
                    value = email,
                    onValueChange = {
                        email = it
                        validationMessage = null
                    },
                    placeholder = "student@university.edu",
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthFieldLabel(text = "PASSWORD")
                    if (!showNameField) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuthSupportingTextColor
                        )
                    }
                }
                OutlinedAuthField(
                    value = password,
                    onValueChange = {
                        password = it
                        validationMessage = null
                    },
                    placeholder = "••••••••",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChanged = {
                        passwordVisible = !passwordVisible
                    }
                )
                Spacer(modifier = Modifier.height(18.dp))

                RememberSessionToggle(
                    checked = persistSession,
                    onCheckedChange = { persistSession = it }
                )

                validationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB3261E)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val error = validateForm(
                            name = name,
                            email = email,
                            password = password,
                            requiresName = showNameField
                        )
                        if (error != null) {
                            validationMessage = error
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        } else {
                            runWhenOnline(isOnline, offlineBannerController) {
                                onPrimaryAction(name.trim(), email.trim(), password, persistSession)
                            }
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MarketplaceYellow,
                        contentColor = MarketplaceDark,
                        disabledContainerColor = MarketplaceYellow.copy(alpha = 0.6f),
                        disabledContentColor = MarketplaceDark.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MarketplaceDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = primaryActionLabel,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                AuthDivider(text = if (showNameField) "ACADEMIC ACCOUNT" else "ACADEMIC SIGN-IN")
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = secondaryPrompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MarketplaceDark
                    )
                    TextButton(onClick = onSecondaryAction) {
                        Text(
                            text = secondaryActionLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MarketplaceDark
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRIVACY POLICY",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuthSupportingTextColor
                    )
                    Text(
                        text = "TERMS OF SERVICE",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuthSupportingTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MarketplaceDark
            )
            Text(
                text = "SCHOLASTIC",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MarketplaceDark
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "MARKETPLACE",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = AuthDecorativeHeaderColor
        )
    }
}

@Composable
private fun AuthFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MarketplaceDark,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun OutlinedAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChanged: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = AuthPlaceholderTextColor
            )
        },
        singleLine = true,
        visualTransformation = when {
            !isPassword -> VisualTransformation.None
            passwordVisible -> VisualTransformation.None
            else -> PasswordVisualTransformation()
        },
        trailingIcon = {
            if (isPassword && onPasswordVisibilityChanged != null) {
                IconButton(onClick = onPasswordVisibilityChanged) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = null,
                        tint = AuthSupportingTextColor
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MarketplaceDark,
            unfocusedBorderColor = AuthBorderColor,
            focusedTextColor = MarketplaceDark,
            unfocusedTextColor = MarketplaceDark,
            focusedContainerColor = MarketplaceWhite,
            unfocusedContainerColor = MarketplaceWhite,
            cursorColor = MarketplaceDark
        )
    )
}

@Composable
private fun RememberSessionToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 1.dp,
                    color = MarketplaceGray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                )
                .background(
                    color = if (checked) MarketplaceYellow.copy(alpha = 0.35f) else MarketplaceWhite,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    color = MarketplaceDark,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            text = "Keep me signed in on this device",
            style = MaterialTheme.typography.bodyLarge,
            color = AuthSupportingTextColor
        )
    }
}

@Composable
private fun AuthDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AuthDividerColor
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = AuthSupportingTextColor,
            letterSpacing = 1.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AuthDividerColor
        )
    }
}

private fun validateForm(
    name: String,
    email: String,
    password: String,
    requiresName: Boolean
): String? {
    if (requiresName && name.trim().isEmpty()) {
        return "Please enter your full name."
    }
    if (email.trim().isEmpty()) {
        return "Please enter your university email."
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
        return "Please enter a valid email address."
    }
    if (password.length < 8) {
        return "Password must be at least 8 characters long."
    }
    return null
}
