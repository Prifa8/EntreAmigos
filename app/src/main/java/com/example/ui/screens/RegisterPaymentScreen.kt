package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
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
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPaymentScreen(
    viewModel: MainViewModel,
    groupId: Long,
    initialPayerId: Long,
    initialReceiverId: Long,
    initialAmountCents: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val group by viewModel.activeGroup.collectAsState()
    val members by viewModel.activeMembers.collectAsState()

    var selectedPayerId by remember { mutableStateOf(initialPayerId) }
    var selectedReceiverId by remember { mutableStateOf(initialReceiverId) }

    // If initial values are 0 (came from general Add), pick default members
    LaunchedEffect(members) {
        if (members.isNotEmpty()) {
            if (selectedPayerId == 0L || selectedPayerId == initialPayerId) {
                val foundPayer = members.find { it.id == initialPayerId } ?: members.first()
                selectedPayerId = foundPayer.id
            }
            if (selectedReceiverId == 0L || selectedReceiverId == initialReceiverId) {
                val foundReceiverId = members.find { it.id == initialReceiverId }?.id ?: members.getOrNull(1)?.id ?: members.first().id
                selectedReceiverId = foundReceiverId
            }
        }
    }

    // Set up amount input (as simple text decimal, e.g., "120.50")
    val initialAmountText = if (initialAmountCents > 0) String.format("%.2f", initialAmountCents / 100.0) else ""
    var amountInput by remember { mutableStateOf(initialAmountText) }

    var notes by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Transferencia") }
    var attachmentSimulated by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("Transferencia", "Efectivo", "Mercado Pago", "Otro")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Pago", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            // --- Payer Selector ---
            Text(
                text = "¿Quién pagó?",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { m ->
                    val isSelected = selectedPayerId == m.id
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) RoseCoral.copy(alpha = 0.15f) else BrandSurface,
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
                                color = if (isSelected) RoseCoral else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // --- Receiver Selector ---
            Text(
                text = "¿Quién recibió?",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { m ->
                    val isSelected = selectedReceiverId == m.id
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else BrandSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedReceiverId = m.id }
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

            // --- Amount input ---
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Importe a Transferir") },
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
                    .testTag("payment_amount_input"),
                singleLine = true
            )

            // --- Method Selection ---
            Text(
                text = "Medio de Pago",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = selectedMethod == method
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) EmeraldGreen else BrandSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMethod = method }
                            .padding(vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // --- Notes ---
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas / Comentario") },
                placeholder = { Text("Ej: Te mandé por Mercado Pago") },
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
                    .testTag("payment_notes_input")
            )

            // --- Proof Attachment Simulator ---
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            attachmentSimulated = "comprobante_pago.png"
                            Toast.makeText(context, "Comprobante de pago adjuntado con éxito", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (attachmentSimulated != null) Icons.Default.Description else Icons.Default.PhotoCamera,
                        contentDescription = "Attach proof",
                        tint = if (attachmentSimulated != null) EmeraldGreen else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (attachmentSimulated != null) "Comprobante adjuntado" else "Adjuntar comprobante",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (attachmentSimulated != null) "comprobante_pago.png (Click para cambiar)" else "Capturar pantalla o subir PDF",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Register Button ---
            val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
            val amountCents = (parsedAmount * 100).toLong()

            Button(
                onClick = {
                    if (amountCents > 0 && selectedPayerId != selectedReceiverId) {
                        viewModel.registerSettlement(
                            groupId = groupId,
                            payerId = selectedPayerId,
                            receiverId = selectedReceiverId,
                            amount = amountCents,
                            method = selectedMethod,
                            notes = notes,
                            proofUri = attachmentSimulated
                        )
                    } else if (selectedPayerId == selectedReceiverId) {
                        Toast.makeText(context, "No podés pagarte a vos mismo", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("register_payment_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = amountCents > 0 && selectedPayerId != selectedReceiverId
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Saldar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Pago", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
