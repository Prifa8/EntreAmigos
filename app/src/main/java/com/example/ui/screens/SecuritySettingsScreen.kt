package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val faceIdEnabled by viewModel.faceIdEnabled.collectAsState()
    val fingerprintEnabled by viewModel.fingerprintEnabled.collectAsState()
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val autoLockTime by viewModel.autoLockTime.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.testTag("security_top_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual header card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SoftEmerald, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                "Seguridad y Accesibilidad",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                "Configurá métodos rápidos de autenticación",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Category: Biometría y Acceso
            item {
                Text(
                    "Biometría y acceso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Face ID
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Face ID", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Usar reconocimiento facial para desbloquear", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Switch(
                                checked = faceIdEnabled,
                                onCheckedChange = {
                                    viewModel.faceIdEnabled.value = it
                                    Toast.makeText(context, if (it) "Face ID Activado" else "Face ID Desactivado", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OnAccentBlue,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BrandBorder
                                )
                            )
                        }

                        HorizontalDivider(color = BrandBorder)

                        // Huella Digital
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Huella digital", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Usar sensor dactilar para autenticación rápida", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Switch(
                                checked = fingerprintEnabled,
                                onCheckedChange = {
                                    viewModel.fingerprintEnabled.value = it
                                    Toast.makeText(context, if (it) "Huella digital activada" else "Huella digital desactivada", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OnAccentBlue,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BrandBorder
                                )
                            )
                        }

                        HorizontalDivider(color = BrandBorder)

                        // Código PIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Código PIN de acceso", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = if (pinEnabled) "PIN configurado y activo" else "Configurá un PIN numérico de seguridad",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = pinEnabled,
                                onCheckedChange = {
                                    if (it) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.pinEnabled.value = false
                                        Toast.makeText(context, "PIN desactivado", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OnAccentBlue,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BrandBorder
                                )
                            )
                        }
                    }
                }
            }

            // Category: Bloqueo Automático
            item {
                Text(
                    "Configuración de bloqueo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Bloqueo automático", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val lockOptions = listOf("Inmediatamente", "Después de 1 minuto", "Después de 5 minutos", "Desactivado")
                        lockOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.autoLockTime.value = option }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (autoLockTime == option) OnAccentBlue else TextSecondary,
                                    fontWeight = if (autoLockTime == option) FontWeight.Bold else FontWeight.Normal
                                )
                                RadioButton(
                                    selected = autoLockTime == option,
                                    onClick = { viewModel.autoLockTime.value = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = OnAccentBlue,
                                        unselectedColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Dialog Setup
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                if (!pinEnabled) {
                    viewModel.pinEnabled.value = false
                }
            },
            title = { Text("Configurar PIN de seguridad", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ingresá un PIN numérico de 4 dígitos para proteger tu aplicación:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinValue = it },
                        label = { Text("PIN de 4 dígitos") },
                        modifier = Modifier.fillMaxWidth().testTag("pin_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            viewModel.pinEnabled.value = true
                            showPinDialog = false
                            Toast.makeText(context, "PIN Configurado correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnAccentBlue)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinDialog = false
                    viewModel.pinEnabled.value = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
