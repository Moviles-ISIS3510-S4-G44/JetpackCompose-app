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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.university.marketplace.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    isOnline: Boolean,
    onCreateListing: (title: String, price: Double, description: String, condition: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("Nuevo") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MarketplaceBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Fotos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

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
                            Text("AGREGAR FOTO", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MarketplaceTextField(value = title, onValueChange = { title = it }, label = "Título (ej. Química Orgánica)")
            Spacer(modifier = Modifier.height(12.dp))
            MarketplaceTextField(value = price, onValueChange = { price = it }, label = "$ Precio")

            Spacer(modifier = Modifier.height(24.dp))

            Text("Condición", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            ConditionToggleGroup(
                selectedCondition = selectedCondition,
                onConditionSelected = { selectedCondition = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            MarketplaceTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción del producto",
                singleLine = false,
                modifier = Modifier.height(120.dp)
            )

            if (!isOnline) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Estás sin conexión. Tu publicación se sincronizará automáticamente cuando recuperes internet.",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.bodySmall
                    )
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
