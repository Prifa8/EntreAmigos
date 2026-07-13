package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var simplifyDebts by remember { mutableStateOf(true) }
    var currency by remember { mutableStateOf("$") }
    var selectedType by remember { mutableStateOf("AMIGOS") }
    var selectedEmoji by remember { mutableStateOf("👥") }

    val groupTypes = listOf(
        Triple("VIAJE", "Viaje", "✈️"),
        Triple("HOGAR", "Hogar", "🏠"),
        Triple("PAREJA", "Pareja", "❤️"),
        Triple("AMIGOS", "Amigos", "🍻"),
        Triple("EVENTO", "Evento", "🎉"),
        Triple("TRABAJO", "Trabajo", "💼"),
        Triple("OTRO", "Personalizado", "⚙️")
    )

    val emojis = listOf("👥", "✈️", "🏠", "❤️", "🍻", "🎉", "💼", "🏔️", "🍕", "🚲", "🎨", "🎸", "🛒", "🚗", "🍿", "🍔")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Grupo", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Grupo") },
                placeholder = { Text("Ej: Viaje a Bariloche, Gastos del Depto") },
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
                    .testTag("group_name_input"),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (Opcional)") },
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
                    .testTag("group_desc_input")
            )

            // Currency selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moneda Principal",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.background(BrandSurface, RoundedCornerShape(12.dp)).padding(4.dp)
                ) {
                    listOf("$", "USD", "EUR").forEach { curr ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (currency == curr) EmeraldGreen else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { currency = curr }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = curr,
                                color = if (currency == curr) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Group Type Selection (Horizontal Row)
            Text(
                text = "Tipo de Grupo",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupTypes) { (type, label, emoji) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else BrandSurface,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedType = type
                                    selectedEmoji = emoji
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = emoji, fontSize = 20.sp)
                                Text(
                                    text = label,
                                    color = if (isSelected) EmeraldGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Emoji Picker
            Text(
                text = "Elegí un ícono / emoji",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojis) { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isSelected) EmeraldGreen else BrandSurface,
                                    CircleShape
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Simplify Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simplificar deudas",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Reorganiza las deudas para hacer la menor cantidad de transferencias.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = simplifyDebts,
                        onCheckedChange = { simplifyDebts = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = BrandSurfaceElevated
                        ),
                        modifier = Modifier.testTag("group_simplify_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createGroup(
                            name = name,
                            description = description,
                            type = selectedType,
                            emoji = selectedEmoji,
                            currency = currency,
                            simplify = simplifyDebts
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_group_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Grupo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
