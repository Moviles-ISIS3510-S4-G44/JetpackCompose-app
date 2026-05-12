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
import kotlinx.coroutines.launch
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
    viewModel: CreateListingViewModel? = null
) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCondition by rememberSaveable { mutableStateOf("Nuevo") }
    var location by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf("Select category") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    val uiState by (viewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(CreateListingUiState.Idle) })
    val categories by (viewModel?.categories?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    LaunchedEffect(uiState) {
        if (uiState is CreateListingUiState.Success) onBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(uiState) {
        if (uiState is CreateListingUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CreateListingUiState.Error).message)
            viewModel?.resetError()
        }
    }

    val isLoading = uiState is CreateListingUiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        val priceInt = price.toIntOrNull() ?: 0
                        val conditionApi = when (selectedCondition.lowercase()) {
                            "como nuevo" -> "refurbished"
                            "usado" -> "used"
                            else -> "new"
                        }
                        val rawImage = imageUri?.toString().orEmpty()
                        val images = if (rawImage.startsWith("http://") || rawImage.startsWith("https://")) {
                            listOf(rawImage)
                        } else {
                            emptyList()
                        }

                        if (selectedCategoryId.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Selecciona una categoría")
                            }
                            return@Button
                        }
                        if (title.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ingresa un título")
                            }
                            return@Button
                        }
                        if (priceInt <= 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ingresa un precio válido")
                            }
                            return@Button
                        }
                        if (location.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ingresa una ubicación")
                            }
                            return@Button
                        }
                        if (imageUri != null && images.isEmpty()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("La imagen local no se sube aún. Se enviará sin imagen.")
                            }
                        }
                        if (viewModel != null) {
                            viewModel.submit(
                                categoryId = selectedCategoryId,
                                title = title,
                                description = description,
                                price = priceInt,
                                condition = conditionApi,
                                images = images,
                                location = location
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = isOnline && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MarketplaceDark,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MarketplaceDark)
                        Spacer(Modifier.width(8.dp))
                        Text("Post Listing", color = MarketplaceDark, fontWeight = FontWeight.Bold)
                    }
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
                            CategoryDropdown(
                                categories = categories.map { it.id to it.name },
                                selectedName = selectedCategoryName,
                                expanded = categoryMenuExpanded,
                                onExpandChange = { categoryMenuExpanded = it },
                                onSelected = { id, name ->
                                    selectedCategoryId = id
                                    selectedCategoryName = name
                                    categoryMenuExpanded = false
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MarketplaceTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = "Location (e.g. Bogotá)"
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
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryDropdown(
                        categories = categories.map { it.id to it.name },
                        selectedName = selectedCategoryName,
                        expanded = categoryMenuExpanded,
                        onExpandChange = { categoryMenuExpanded = it },
                        onSelected = { id, name ->
                            selectedCategoryId = id
                            selectedCategoryName = name
                            categoryMenuExpanded = false
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MarketplaceTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "Location (e.g. Bogotá)"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Pair<String, String>>,
    selectedName: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelected: (String, String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandChange
    ) {
        TextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MarketplaceWhite,
                unfocusedContainerColor = MarketplaceWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            categories.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelected(id, name) }
                )
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
            val isSelected = selectedCondition.equals(option, ignoreCase = true)
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
