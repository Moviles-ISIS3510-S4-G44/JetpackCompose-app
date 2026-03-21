package com.university.marketplace.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.university.marketplace.ui.theme.* // Importa tus colores personalizados

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    isOnline: Boolean
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("New") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create New Listing", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarketplaceWhite)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        println("ANALYTICS: create_listing | Title: $title")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = isOnline,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = MarketplaceDark)
                    Spacer(Modifier.width(8.dp))
                    Text("Post Listing", color = MarketplaceDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MarketplaceBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Photos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MarketplaceWhite)
                        .clickable { galleryLauncher.launch("image/*") }
                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                            Text("ADD PHOTO", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Inputs
            MarketplaceTextField(value = title, onValueChange = { title = it }, label = "Title (e.g. Organic Chemistry)")
            Spacer(modifier = Modifier.height(12.dp))
            MarketplaceTextField(value = price, onValueChange = { price = it }, label = "$ Price")

            Spacer(modifier = Modifier.height(24.dp))

            // Selector
            Text("Condition", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ConditionToggleGroup(
                selectedCondition = selectedCondition,
                onConditionSelected = { selectedCondition = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            MarketplaceTextField(
                value = description,
                onValueChange = { description = it },
                label = "Item Description",
                singleLine = false,
                modifier = Modifier.height(120.dp)
            )

            if (!isOnline) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You are offline. Publishing is disabled until the connection is restored.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MarketplaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(label, color = Color.Gray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MarketplaceWhite,
            unfocusedContainerColor = MarketplaceWhite,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = singleLine
    )
}

@Composable
fun ConditionToggleGroup(selectedCondition: String, onConditionSelected: (String) -> Unit) {
    val options = listOf("New", "Like New", "Used")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F1F1))
    ) {
        options.forEach { option ->
            val isSelected = selectedCondition == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MarketplaceYellow else Color.Transparent)
                    .clickable { onConditionSelected(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MarketplaceDark
                )
            }
        }
    }
}