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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.university.marketplace.ui.common.isWideScreen
import com.university.marketplace.ui.theme.*

private val UriSaver: Saver<Uri?, String> = Saver(
    save = { uri -> uri?.toString() ?: "" },
    restore = { value -> if (value.isEmpty()) null else Uri.parse(value) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    isOnline: Boolean,
    onCreateListing: (title: String, price: Double, description: String, condition: String) -> Unit = { _, _, _, _ -> }
) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCondition by rememberSaveable { mutableStateOf("New") }
    var imageUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear publicación", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarketplaceWhite)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        val parsedPrice = price.trim().replace(",", ".").toDoubleOrNull()
                        val normalizedTitle = title.trim()
                        val normalizedDescription = description.trim()
                        if (normalizedTitle.isBlank() || normalizedDescription.isBlank() || parsedPrice == null || parsedPrice <= 0.0) {
                            android.widget.Toast.makeText(
                                context,
                                "Completa título, precio y descripción válidos.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        onCreateListing(
                            normalizedTitle,
                            parsedPrice,
                            normalizedDescription,
                            selectedCondition.lowercase()
                        )
                        // Mock success for offline mode
                        if (!isOnline) {
                            android.widget.Toast.makeText(context, "Publicación guardada localmente", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    // ALLOW CREATING OFFLINE (Eventual connectivity)
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MarketplaceYellow,
                        contentColor = MarketplaceDark
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MarketplaceBackground),
            contentAlignment = Alignment.TopCenter
        ) {
            val wide = isWideScreen()
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            PhotoPickerSection(
                                imageUri = imageUri,
                                onPickImage = { galleryLauncher.launch("image/*") }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Condition",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ConditionToggleGroup(
                                selectedCondition = selectedCondition,
                                onConditionSelected = { selectedCondition = it }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            MarketplaceTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = "Title (e.g. Organic Chemistry)"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MarketplaceTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = "$ Price"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MarketplaceTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Item Description",
                                singleLine = false,
                                modifier = Modifier.height(160.dp)
                            )
                        }
                    }
                } else {
                    PhotoPickerSection(
                        imageUri = imageUri,
                        onPickImage = { galleryLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    MarketplaceTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title (e.g. Organic Chemistry)"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MarketplaceTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "$ Price"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Condition",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ConditionToggleGroup(
                        selectedCondition = selectedCondition,
                        onConditionSelected = { selectedCondition = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    MarketplaceTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Item Description",
                        singleLine = false,
                        modifier = Modifier.height(120.dp)
                    )
                }

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
}

@Composable
private fun PhotoPickerSection(
    imageUri: Uri?,
    onPickImage: () -> Unit
) {
    Text("Photos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MarketplaceWhite)
                .clickable { onPickImage() }
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
    val options = listOf("Nuevo", "Como nuevo", "Usado")
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
