package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.ExpensePayerEntity
import com.example.data.ExpenseSplitEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: MainViewModel,
    groupId: Long,
    expenseId: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val group by viewModel.activeGroup.collectAsState()
    val members by viewModel.activeMembers.collectAsState()

    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Comida") }
    var notes by remember { mutableStateOf("") }
    var wasAutodetected by remember { mutableStateOf(false) }

    // Automatic category autodetection on typing
    LaunchedEffect(description) {
        if (description.isNotBlank()) {
            val detected = com.example.data.GeminiService.getCategoryByHeuristics(description)
            if (detected != "Otros" && detected != selectedCategory) {
                selectedCategory = detected
                wasAutodetected = true
            }
        }
    }

    // Payer selection: single payer initially, or let them type. Let's make single-payer very fast to select!
    var selectedPayerId by remember { mutableStateOf(0L) }

    // Split Type: "EQUAL", "EXACT", "PERCENT", "SHARES"
    var splitType by remember { mutableStateOf("EQUAL") }

    // Custom split inputs per member: map of memberId to text input
    val customInputs = remember { mutableStateMapOf<Long, String>() }

    var selectedCurrency by remember { mutableStateOf("$") }
    var customExchangeRate by remember { mutableStateOf("1.0") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceInterval by remember { mutableStateOf("Mensual") } // Semanal, Mensual, Anual
    var isFetchingRate by remember { mutableStateOf(false) }
    var attachedReceiptUri by remember { mutableStateOf<String?>(null) }

    val receiptGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedReceiptUri = uri.toString()
            Toast.makeText(context, "📸 Comprobante seleccionado de la galería", Toast.LENGTH_SHORT).show()
        }
    }

    val receiptCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "receipt_img_${System.currentTimeMillis()}.jpg")
                val out = java.io.FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.close()
                attachedReceiptUri = file.absolutePath
                Toast.makeText(context, "📸 Foto de recibo guardada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val attachedReceiptBitmap = remember(attachedReceiptUri) {
        if (attachedReceiptUri == null) null
        else {
            try {
                val uriStr = attachedReceiptUri!!
                if (uriStr.startsWith("/")) {
                    BitmapFactory.decodeFile(uriStr)
                } else {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Returns estimated rate from selectedCurrency to baseCurrency
    fun getEstimatedRate(from: String, to: String): Double {
        if (from == to) return 1.0
        val usdRates = mapOf(
            "USD" to 1.0,
            "$" to 1.0,
            "ARS" to 950.0,
            "EUR" to 0.92,
            "CLP" to 940.0,
            "MXN" to 18.0,
            "COP" to 4000.0,
            "PEN" to 3.75,
            "UYU" to 40.0
        )
        val fromInUsd = usdRates[from] ?: 1.0
        val toInUsd = usdRates[to] ?: 1.0
        return toInUsd / fromInUsd
    }

    LaunchedEffect(group) {
        if (group != null && expenseId == 0L) {
            selectedCurrency = group!!.baseCurrency
        }
    }

    LaunchedEffect(selectedCurrency, group) {
        if (group != null) {
            val base = group!!.baseCurrency
            if (selectedCurrency == base) {
                customExchangeRate = "1.0"
            } else {
                // Instantly set fallback estimation first
                val estimate = getEstimatedRate(selectedCurrency, base)
                customExchangeRate = String.format(Locale.US, "%.4f", estimate)
                
                // Fetch real-time rate
                isFetchingRate = true
                val live = viewModel.fetchLiveExchangeRate(selectedCurrency, base)
                if (live != null) {
                    customExchangeRate = String.format(Locale.US, "%.4f", live)
                }
                isFetchingRate = false
            }
        }
    }

    val categories = listOf("Comida", "Supermercado", "Alojamiento", "Transporte", "Combustible", "Servicios", "Alquiler", "Otros")

    // Populate initial states
    LaunchedEffect(members) {
        if (members.isNotEmpty()) {
            if (selectedPayerId == 0L) {
                // Default to current user or first member
                val userMem = members.find { it.isUser } ?: members.first()
                selectedPayerId = userMem.id
            }
            members.forEach { m ->
                if (!customInputs.containsKey(m.id)) {
                    customInputs[m.id] = ""
                }
            }
        }
    }

    // Load existing expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != 0L) {
            val exp = viewModel.getExpenseById(expenseId)
            if (exp != null) {
                description = exp.description
                amountInput = String.format(Locale.US, "%.2f", exp.totalAmount / 100.0)
                selectedCategory = exp.category
                notes = exp.notes
                attachedReceiptUri = exp.proofUri

                // Parse original currency if in notes
                val notesStr = exp.notes
                if (notesStr.contains("[Multidivisa:")) {
                    try {
                        val firstLine = notesStr.lineSequence().firstOrNull { it.startsWith("[Multidivisa:") }
                        if (firstLine != null) {
                            val parts = firstLine.removePrefix("[Multidivisa:").removeSuffix("]").trim().split(" ")
                            if (parts.size >= 2) {
                                selectedCurrency = parts[1]
                                val rateIdx = parts.indexOf("T.C.")
                                if (rateIdx != -1 && rateIdx + 1 < parts.size) {
                                    customExchangeRate = parts[rateIdx + 1]
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // fallback
                    }
                }
                if (notesStr.contains("[Gasto Recurrente:")) {
                    isRecurring = true
                    try {
                        val intervalLine = notesStr.lineSequence().firstOrNull { it.startsWith("[Gasto Recurrente:") }
                        if (intervalLine != null) {
                            recurrenceInterval = intervalLine.removePrefix("[Gasto Recurrente:").removeSuffix("]").trim()
                        }
                    } catch (e: Exception) {
                        // fallback
                    }
                }

                val payers = viewModel.getPayersForExpenseSync(expenseId)
                if (payers.isNotEmpty()) {
                    selectedPayerId = payers.first().memberId
                }

                val splits = viewModel.getSplitsForExpenseSync(expenseId)
                if (splits.isNotEmpty()) {
                    splitType = splits.first().splitType
                    splits.forEach { s ->
                        customInputs[s.memberId] = when (splitType) {
                            "EXACT" -> String.format(Locale.US, "%.2f", s.amountOwed / 100.0)
                            "PERCENT" -> String.format(Locale.US, "%.1f", s.percentage)
                            "SHARES" -> String.format(Locale.US, "%.1f", s.shares)
                            else -> ""
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == 0L) "Agregar Gasto" else "Editar Gasto", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        },
        containerColor = BrandBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBackground)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- AI Ticket Assistant (OCR) Card ---
            var showAiDialog by remember { mutableStateOf(false) }
            var isScanning by remember { mutableStateOf(false) }
            var rawTextToParse by remember { mutableStateOf("") }

            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    isScanning = true
                    coroutineScope.launch {
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes()
                            if (bytes != null) {
                                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                val parsed = viewModel.parseTicketImageWithGemini(base64, mime)
                                if (parsed != null) {
                                    description = parsed.description
                                    amountInput = String.format(Locale.US, "%.2f", parsed.amount)
                                    selectedCategory = parsed.category
                                    if (parsed.notes.isNotBlank()) {
                                        notes = parsed.notes
                                    }
                                    showAiDialog = false
                                    Toast.makeText(context, "✨ ¡Ticket escaneado con éxito!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se pudo procesar la imagen con Gemini. Intenta pegando el texto.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al leer imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isScanning = false
                        }
                    }
                }
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    isScanning = true
                    coroutineScope.launch {
                        try {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            val bytes = outputStream.toByteArray()
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            val parsed = viewModel.parseTicketImageWithGemini(base64, "image/jpeg")
                            if (parsed != null) {
                                description = parsed.description
                                amountInput = String.format(Locale.US, "%.2f", parsed.amount)
                                selectedCategory = parsed.category
                                if (parsed.notes.isNotBlank()) {
                                    notes = parsed.notes
                                }
                                showAiDialog = false
                                Toast.makeText(context, "✨ ¡Foto escaneada con éxito!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No se pudo procesar la foto con Gemini. Intenta pegando el texto.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al procesar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isScanning = false
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✨", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Asistente de Tickets con IA",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        TextButton(
                            onClick = { showAiDialog = !showAiDialog },
                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGreen)
                        ) {
                            Text(if (showAiDialog) "Cerrar" else "Escanear / Pegar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showAiDialog) {
                        Text(
                            text = "Tomá una foto de tu ticket físico o cargá una de tu galería para que Gemini extraiga los detalles de forma inteligente:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Visual scanning option buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.15f), contentColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Cámara", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tomar Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.15f), contentColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = "Galería", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cargar Imagen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = BrandBorder.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "O pegá el texto descriptivo / comprobante del portapapeles:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Quick templates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("Super 🛒", "Coto Almacén\nFECHA: 12/07/2026\nArtículos:\n- Carne Asado: $5200\n- Carbón: $1200\n- Coca Cola: $1500\nTOTAL: $7900", "Supermercado"),
                                Triple("Sushi 🍣", "Kokoro Sushi Bar\nCENA DE AMIGOS\n1x Combo 30 piezas: $11500\n2x Cerveza Patagonia: $3400\nTOTAL: $14900\n¡Gracias por su visita!", "Comida"),
                                Triple("Nafta ⛽", "Estación YPF\nNafta Súper Infinitia: 12.5 litros\nTOTAL: $9250.00\nOperación aprobada", "Combustible")
                            ).forEach { (label, fullText, cat) ->
                                Box(
                                    modifier = Modifier
                                        .background(BrandBackground, RoundedCornerShape(8.dp))
                                        .border(1.dp, BrandBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            rawTextToParse = fullText
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = rawTextToParse,
                            onValueChange = { rawTextToParse = it },
                            placeholder = { Text("Pegá el texto de tu ticket, mail, WhatsApp o factura acá...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 5,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = BrandBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (rawTextToParse.isBlank()) {
                                    Toast.makeText(context, "Ingresá o seleccioná un texto para procesar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isScanning = true
                                coroutineScope.launch {
                                    val parsed = viewModel.parseTicketWithGemini(rawTextToParse)
                                    isScanning = false
                                    if (parsed != null) {
                                        description = parsed.description
                                        amountInput = String.format(Locale.US, "%.2f", parsed.amount)
                                        selectedCategory = parsed.category
                                        if (parsed.notes.isNotBlank()) {
                                            notes = parsed.notes
                                        }
                                        showAiDialog = false
                                        Toast.makeText(context, "✨ ¡Ticket procesado con éxito!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No se pudo procesar. Verificá tu conexión o ingresalo manualmente.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isScanning && rawTextToParse.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Procesando con Gemini...", color = Color.White, fontSize = 13.sp)
                            } else {
                                Text("Procesar Texto con Gemini 🪄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Small indicator of what it does
                        Text(
                            text = "💡 Podés autocompletar este gasto al instante sacando una foto, subiendo una imagen o pegando un ticket.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // --- Description ---
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                placeholder = { Text("Ej: Cena, Compra de carne, Nafta") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen,
                    unfocusedBorderColor = BrandSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_desc_input"),
                singleLine = true
            )

            // --- Amount ---
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Importe Total") },
                placeholder = { Text("0.00") },
                prefix = { Text("${group?.baseCurrency ?: "$"} ") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen,
                    unfocusedBorderColor = BrandSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true
            )

            // --- Currency Selection and Conversion (Multidivisa) ---
            val baseCurrency = group?.baseCurrency ?: "$"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Moneda del Gasto",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCurrency != baseCurrency) {
                        if (isFetchingRate) {
                            Text(
                                text = "🔄 Actualizando tasa...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3B82F6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "✨ Tasa actualizada en tiempo real",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(baseCurrency, "USD", "EUR", "ARS").distinct().forEach { curr ->
                        val isSel = selectedCurrency == curr
                        Box(
                            modifier = Modifier
                                .background(if (isSel) EmeraldGreen else BrandSurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { selectedCurrency = curr }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = curr,
                                color = if (isSel) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (selectedCurrency != baseCurrency) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customExchangeRate,
                            onValueChange = { customExchangeRate = it },
                            label = { Text("Tasa de Cambio (1 $selectedCurrency = ? $baseCurrency)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                focusedLabelColor = EmeraldGreen,
                                unfocusedBorderColor = BrandSurfaceElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    val rate = customExchangeRate.toDoubleOrNull() ?: 1.0
                    val enteredAmt = amountInput.toDoubleOrNull() ?: 0.0
                    val converted = enteredAmt * rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Equivalente registrado:",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "$baseCurrency ${String.format(Locale.US, "%.2f", converted)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            // --- Recurrent Expense Option (Gastos Recurrentes) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gasto Recurrente",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Para suscripciones (Netflix, Spotify, alquiler).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = BrandSurfaceElevated
                        )
                    )
                }

                if (isRecurring) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Semanal", "Mensual", "Anual").forEach { interval ->
                            val isSel = recurrenceInterval == interval
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) EmeraldGreen else BrandSurfaceElevated, RoundedCornerShape(8.dp))
                                    .clickable { recurrenceInterval = interval }
                                    .padding(vertical = 8.dp)
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = interval,
                                    color = if (isSel) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Category Selection ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Categoría", fontWeight = FontWeight.Bold, color = TextPrimary)
                if (wasAutodetected) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✨ IA Autodetectada",
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) EmeraldGreen else BrandSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { 
                                selectedCategory = cat
                                wasAutodetected = false
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- Who Paid ---
            Text(text = "¿Quién pagó?", fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { m ->
                    val isSelected = selectedPayerId == m.id
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else BrandSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPayerId = m.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = m.avatarEmoji, fontSize = 20.sp)
                            Text(
                                text = m.name,
                                color = if (isSelected) EmeraldGreen else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // --- Division Method ---
            Text(text = "Método de División", fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("EQUAL", "Igual"),
                    Pair("EXACT", "Exacto"),
                    Pair("PERCENT", "%"),
                    Pair("SHARES", "Partes")
                ).forEach { (type, label) ->
                    val isSelected = splitType == type
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) EmeraldGreen else BrandSurface,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { splitType = type }
                            .padding(vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // --- Custom Division Inputs per Member ---
            if (splitType != "EQUAL") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when (splitType) {
                                "EXACT" -> "Ingresar importes exactos por miembro:"
                                "PERCENT" -> "Ingresar porcentaje por miembro (debe sumar 100%):"
                                "SHARES" -> "Ingresar partes asignadas (ej: Ana 2 partes, Bruno 1 parte):"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        members.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = m.avatarEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = m.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }

                                val inputVal = customInputs[m.id] ?: ""
                                OutlinedTextField(
                                    value = inputVal,
                                    onValueChange = { customInputs[m.id] = it },
                                    placeholder = { Text("0") },
                                    suffix = {
                                        Text(
                                            text = when (splitType) {
                                                "EXACT" -> group?.baseCurrency ?: "$"
                                                "PERCENT" -> "%"
                                                "SHARES" -> "partes"
                                                else -> ""
                                            },
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BrandSurfaceElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(48.dp)
                                        .testTag("split_input_${m.id}"),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // --- Notes ---
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas o Comentarios Adicionales (Opcional)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    focusedLabelColor = EmeraldGreen,
                    unfocusedBorderColor = BrandSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_notes_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Receipt Photo Attachment Card ---
            var showReceiptZoomDialog by remember { mutableStateOf(false) }

            Text(
                text = "Foto de Recibo / Comprobante",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (attachedReceiptUri == null) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "No receipt",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Sin comprobante adjunto",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "Saca una foto del ticket físico o sube una imagen de la galería para que quede registrada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { receiptCameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f).testTag("receipt_camera_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tomar Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { receiptGalleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).testTag("receipt_gallery_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galería", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Display attached image preview
                        if (attachedReceiptBitmap != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable { showReceiptZoomDialog = true }
                                    .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = attachedReceiptBitmap.asImageBitmap(),
                                    contentDescription = "Receipt preview",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "✨ ¡Imagen de recibo adjuntada! Toca para ampliar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // Falling back if bitmap loading failed but URI exists
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandSurfaceElevated, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Archivo adjunto registrado",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayName = if (attachedReceiptUri!!.contains("/")) attachedReceiptUri!!.substringAfterLast("/") else "Imagen seleccionada"
                            Text(
                                text = "Comprobante: $displayName",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { attachedReceiptUri = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = RoseCoral),
                                modifier = Modifier.testTag("remove_receipt_button")
                            ) {
                                Text("Quitar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (showReceiptZoomDialog && attachedReceiptBitmap != null) {
                AlertDialog(
                    onDismissRequest = { showReceiptZoomDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showReceiptZoomDialog = false }) {
                            Text("Cerrar", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = { Text("Visualización del Recibo", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            Image(
                                bitmap = attachedReceiptBitmap.asImageBitmap(),
                                contentDescription = "Receipt Full",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    },
                    containerColor = BrandSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Save Action Button ---
            val enteredAmountDouble = amountInput.toDoubleOrNull() ?: 0.0
            val rateDouble = customExchangeRate.toDoubleOrNull() ?: 1.0
            val totalAmountDouble = enteredAmountDouble * rateDouble
            val totalAmountCents = (totalAmountDouble * 100).toLong()

            Button(
                onClick = {
                    if (description.isBlank() || totalAmountCents <= 0L) {
                        Toast.makeText(context, "Ingresá descripción e importe válidos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Compute splits according to splitType
                    val calculatedSplits = mutableListOf<ExpenseSplitEntity>()
                    when (splitType) {
                        "EQUAL" -> {
                            val count = members.size
                            if (count > 0) {
                                val baseOwed = totalAmountCents / count
                                var remainder = totalAmountCents % count
                                members.forEach { m ->
                                    val finalOwed = baseOwed + (if (remainder > 0L) { remainder--; 1L } else 0L)
                                    calculatedSplits.add(
                                        ExpenseSplitEntity(
                                            expenseId = expenseId,
                                            memberId = m.id,
                                            amountOwed = finalOwed,
                                            splitType = "EQUAL"
                                        )
                                    )
                                }
                            }
                        }
                        "EXACT" -> {
                            var sumCents = 0L
                            val inputsCents = members.map { m ->
                                val inputVal = customInputs[m.id]?.toDoubleOrNull() ?: 0.0
                                val inputCents = (inputVal * 100).toLong()
                                sumCents += inputCents
                                Pair(m.id, inputCents)
                            }
                            if (sumCents != totalAmountCents) {
                                Toast.makeText(
                                    context,
                                    "Error: Los importes ingresados (${formatCents(sumCents)}) no coinciden con el total (${formatCents(totalAmountCents)})",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            inputsCents.forEach { (mId, cents) ->
                                calculatedSplits.add(
                                    ExpenseSplitEntity(
                                        expenseId = expenseId,
                                        memberId = mId,
                                        amountOwed = cents,
                                        splitType = "EXACT"
                                    )
                                )
                            }
                        }
                        "PERCENT" -> {
                            var sumPercent = 0.0
                            val inputsPercent = members.map { m ->
                                val pct = customInputs[m.id]?.toDoubleOrNull() ?: 0.0
                                sumPercent += pct
                                Pair(m.id, pct)
                            }
                            if (sumPercent != 100.0) {
                                Toast.makeText(
                                    context,
                                    "Error: Los porcentajes ingresados deben sumar exactamente 100% (Suma actual: $sumPercent%)",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            // Calculate proportional cents
                            var allocated = 0L
                            inputsPercent.forEachIndexed { idx, (mId, pct) ->
                                val isLast = idx == inputsPercent.size - 1
                                val owed = if (isLast) {
                                    totalAmountCents - allocated
                                } else {
                                    val owedVal = (totalAmountCents * (pct / 100.0)).toLong()
                                    allocated += owedVal
                                    owedVal
                                }
                                calculatedSplits.add(
                                    ExpenseSplitEntity(
                                        expenseId = expenseId,
                                        memberId = mId,
                                        amountOwed = owed,
                                        percentage = pct,
                                        splitType = "PERCENT"
                                    )
                                )
                            }
                        }
                        "SHARES" -> {
                            var sumShares = 0.0
                            val inputsShares = members.map { m ->
                                val sh = customInputs[m.id]?.toDoubleOrNull() ?: 0.0
                                sumShares += sh
                                Pair(m.id, sh)
                            }
                            if (sumShares <= 0.0) {
                                Toast.makeText(context, "Error: Las partes asignadas deben ser mayores a cero", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Calculate proportional cents based on shares
                            var allocated = 0L
                            inputsShares.forEachIndexed { idx, (mId, sh) ->
                                val isLast = idx == inputsShares.size - 1
                                val owed = if (isLast) {
                                    totalAmountCents - allocated
                                } else {
                                    val owedVal = (totalAmountCents * (sh / sumShares)).toLong()
                                    allocated += owedVal
                                    owedVal
                                }
                                calculatedSplits.add(
                                    ExpenseSplitEntity(
                                        expenseId = expenseId,
                                        memberId = mId,
                                        amountOwed = owed,
                                        shares = sh,
                                        splitType = "SHARES"
                                    )
                                )
                            }
                        }
                    }

                    // Prepend or format metadata tags inside notes
                    var cleanNotes = notes.lineSequence()
                        .filter { !it.startsWith("[Multidivisa:") && !it.startsWith("[Gasto Recurrente:") }
                        .joinToString("\n")
                        .trim()

                    if (selectedCurrency != (group?.baseCurrency ?: "$")) {
                        val convertedNote = "[Multidivisa: ${amountInput} ${selectedCurrency} @ T.C. ${customExchangeRate}]"
                        cleanNotes = if (cleanNotes.isBlank()) convertedNote else "$convertedNote\n$cleanNotes"
                    }
                    if (isRecurring) {
                        val recNote = "[Gasto Recurrente: $recurrenceInterval]"
                        cleanNotes = if (cleanNotes.isBlank()) recNote else "$recNote\n$cleanNotes"
                    }

                    // Save action
                    val actorName = members.find { it.isUser }?.name ?: "Sistema"
                    val payers = listOf(
                        ExpensePayerEntity(
                            expenseId = expenseId,
                            memberId = selectedPayerId,
                            amountPaid = totalAmountCents
                        )
                    )

                    viewModel.saveExpenseWithDetails(
                        groupId = groupId,
                        expenseId = expenseId,
                        description = description,
                        totalAmount = totalAmountCents,
                        category = selectedCategory,
                        notes = cleanNotes,
                        payers = payers,
                        splits = calculatedSplits,
                        actorName = actorName,
                        proofUri = attachedReceiptUri
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_expense_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = description.isNotBlank() && totalAmountCents > 0L
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Gasto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
