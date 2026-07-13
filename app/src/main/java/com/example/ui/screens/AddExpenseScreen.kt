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
import com.example.data.ExpensePayerEntity
import com.example.data.ExpenseSplitEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: MainViewModel,
    groupId: Long,
    expenseId: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val group by viewModel.activeGroup.collectAsState()
    val members by viewModel.activeMembers.collectAsState()

    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Comida") }
    var notes by remember { mutableStateOf("") }

    // Payer selection: single payer initially, or let them type. Let's make single-payer very fast to select!
    var selectedPayerId by remember { mutableStateOf(0L) }

    // Split Type: "EQUAL", "EXACT", "PERCENT", "SHARES"
    var splitType by remember { mutableStateOf("EQUAL") }

    // Custom split inputs per member: map of memberId to text input
    val customInputs = remember { mutableStateMapOf<Long, String>() }

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

            // --- Category Selection ---
            Text(text = "Categoría", fontWeight = FontWeight.Bold, color = TextPrimary)
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
                            .clickable { selectedCategory = cat }
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

            Spacer(modifier = Modifier.height(24.dp))

            // --- Save Action Button ---
            val totalAmountDouble = amountInput.toDoubleOrNull() ?: 0.0
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
                        notes = notes,
                        payers = payers,
                        splits = calculatedSplits,
                        actorName = actorName
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
