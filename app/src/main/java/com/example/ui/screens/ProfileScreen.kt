package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.ThemeMode
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userAlias by viewModel.userAlias.collectAsState()
    val userCbu by viewModel.userCbu.collectAsState()
    val userAliasMP by viewModel.userAliasMP.collectAsState()

    val groups by viewModel.allGroups.collectAsState()
    val globalToReceive by viewModel.globalTotalToReceive.collectAsState()
    val globalToPay by viewModel.globalTotalToPay.collectAsState()
    val netBalanceTotal = globalToReceive - globalToPay

    // Gather contact count dynamically
    var contactCount by remember { mutableStateOf(0) }
    LaunchedEffect(groups) {
        val uniqueContacts = mutableSetOf<String>()
        groups.forEach { g ->
            val members = viewModel.getMembersSync(g.id)
            members.forEach { m ->
                if (!m.isUser) uniqueContacts.add(m.name)
            }
        }
        contactCount = uniqueContacts.size
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Premium Profile Card Header ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(OnAccentBlue.copy(alpha = 0.1f), CircleShape)
                            .border(1.5.dp, OnAccentBlue.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = userAlias.trim().firstOrNull()?.uppercase() ?: "P"
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnAccentBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = userAlias,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "prilautarofabri@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Text(
                        text = "Registrado el 12 de Julio, 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OnAccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar perfil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Fast Statistics Summary Rows ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Groups count card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Grupos", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("${groups.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                }

                // Contacts count card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Contactos", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("$contactCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                }

                // Balance card
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Balance total", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(
                            text = "${if (netBalanceTotal >= 0) "+" else ""}$${formatCents(netBalanceTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (netBalanceTotal >= 0) EmeraldGreen else RoseCoral
                        )
                    }
                }
            }
        }

        // --- Account settings list grouped cleanly (Notion style) ---
        item {
            Text(
                text = "Acciones y Compartir",
                style = MaterialTheme.typography.titleSmall,
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
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.QrCodeScanner,
                        iconTint = OnAccentLavender,
                        label = "Escanear código QR",
                        onClick = { Toast.makeText(context, "Escáner QR activado (Simulado)", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Share,
                        iconTint = OnAccentLavender,
                        label = "Invitar amigos",
                        onClick = { Toast.makeText(context, "Enlace de invitación copiado al portapapeles", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }

        item {
            Text(
                text = "Configuración de la Aplicación",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            val themeMode by viewModel.themeMode.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tema de la aplicación",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Elegí si preferís usar un diseño claro, oscuro o adaptado al sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf(
                            Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "Claro"),
                            Triple(ThemeMode.DARK, Icons.Default.DarkMode, "Oscuro"),
                            Triple(ThemeMode.AUTO, Icons.Default.Settings, "Sistema")
                        )
                        modes.forEach { (mode, icon, label) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(
                                        if (isSelected) OnAccentBlue else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.themeMode.value = mode }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Notifications,
                        iconTint = OnAccentBlue,
                        label = "Notificaciones",
                        onClick = { viewModel.navigateTo(Screen.NotificationsSettings) }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        iconTint = OnAccentBlue,
                        label = "Seguridad",
                        onClick = { viewModel.navigateTo(Screen.SecuritySettings) }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Palette,
                        iconTint = OnAccentBlue,
                        label = "Apariencia",
                        onClick = { viewModel.navigateTo(Screen.AppearanceSettings) }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Language,
                        iconTint = OnAccentBlue,
                        label = "Idioma",
                        onClick = { viewModel.navigateTo(Screen.AppearanceSettings) }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.AttachMoney,
                        iconTint = OnAccentBlue,
                        label = "Moneda por defecto",
                        onClick = { Toast.makeText(context, "Moneda por defecto: Peso Argentino ($)", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }

        item {
            Text(
                text = "Datos y Soporte",
                style = MaterialTheme.typography.titleSmall,
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
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Download,
                        iconTint = EmeraldGreen,
                        label = "Exportar datos",
                        onClick = { Toast.makeText(context, "Exportando historial de gastos a formato CSV...", Toast.LENGTH_LONG).show() }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.HelpOutline,
                        iconTint = TextSecondary,
                        label = "Centro de ayuda",
                        onClick = { Toast.makeText(context, "Abriendo ayuda de EntreTodos...", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Info,
                        iconTint = TextSecondary,
                        label = "Acerca de la app",
                        onClick = { Toast.makeText(context, "EntreTodos Premium v2.4.0 • Hecho con cariño", Toast.LENGTH_LONG).show() }
                    )
                    HorizontalDivider(color = BrandBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Refresh,
                        iconTint = RoseCoral,
                        label = "Reiniciar Base de Datos (Mantenimiento)",
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }

        // --- Logout Button ---
        item {
            Button(
                onClick = { Toast.makeText(context, "Sesión cerrada (Simulado)", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = RoseCoral),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoseCoral.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = RoseCoral)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.Bold, color = RoseCoral)
            }
        }
    }

    // --- Edit Profile Dialog ---
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userAlias) }
        var tempCbu by remember { mutableStateOf(userCbu) }
        var tempAliasMp by remember { mutableStateOf(userAliasMP) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nombre de usuario") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name")
                    )

                    OutlinedTextField(
                        value = tempCbu,
                        onValueChange = { tempCbu = it },
                        label = { Text("CBU / CVU para reintegros") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_cbu")
                    )

                    OutlinedTextField(
                        value = tempAliasMp,
                        onValueChange = { tempAliasMp = it },
                        label = { Text("Alias de Mercado Pago") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_mp")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.userAlias.value = tempName
                        viewModel.userCbu.value = tempCbu
                        viewModel.userAliasMP.value = tempAliasMp
                        showEditProfileDialog = false
                        Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnAccentBlue)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Reset database warning dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Reiniciar base de datos?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción eliminará todos los gastos, grupos y pagos agregados manualmente, y reestablecerá los datos semilla iniciales.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.performHardReset()
                        Toast.makeText(context, "Base de datos restablecida correctamente", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseCoral)
                ) {
                    Text("Reiniciar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = BrandSurface
        )
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatCents(cents: Long): String {
    val major = abs(cents) / 100.0
    return String.format(java.util.Locale.US, "%,.2f", major)
}
