package com.example.ui.screens

import androidx.compose.ui.layout.layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Notificaciones", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.testTag("notifications_top_bar")
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
            // Category: Grupos y contactos
            item {
                CategoryHeader(title = "Grupos y contactos")
            }
            item {
                SettingsCard {
                    NotificationRow(
                        label = "Alguien me agrega a un grupo",
                        pushFlow = viewModel.notifyGroupAddedPush,
                        emailFlow = viewModel.notifyGroupAddedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien me agrega como contacto",
                        pushFlow = viewModel.notifyContactAddedPush,
                        emailFlow = viewModel.notifyContactAddedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Me invitan a un grupo",
                        pushFlow = viewModel.notifyInviteGroupPush,
                        emailFlow = viewModel.notifyInviteGroupEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien abandona un grupo",
                        pushFlow = viewModel.notifyLeaveGroupPush,
                        emailFlow = viewModel.notifyLeaveGroupEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Cambia la configuración de un grupo",
                        pushFlow = viewModel.notifyGroupConfigChangedPush,
                        emailFlow = viewModel.notifyGroupConfigChangedEmail
                    )
                }
            }

            // Category: Gastos y pagos
            item {
                CategoryHeader(title = "Gastos y pagos")
            }
            item {
                SettingsCard {
                    NotificationRow(
                        label = "Se agrega un gasto",
                        pushFlow = viewModel.notifyExpenseAddedPush,
                        emailFlow = viewModel.notifyExpenseAddedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Un gasto es editado",
                        pushFlow = viewModel.notifyExpenseEditedPush,
                        emailFlow = viewModel.notifyExpenseEditedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Un gasto es eliminado",
                        pushFlow = viewModel.notifyExpenseDeletedPush,
                        emailFlow = viewModel.notifyExpenseDeletedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien comenta un gasto",
                        pushFlow = viewModel.notifyExpenseCommentPush,
                        emailFlow = viewModel.notifyExpenseCommentEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Un gasto vence",
                        pushFlow = viewModel.notifyExpenseDuePush,
                        emailFlow = viewModel.notifyExpenseDueEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien registra un pago",
                        pushFlow = viewModel.notifyPaymentRegisteredPush,
                        emailFlow = viewModel.notifyPaymentRegisteredEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien confirma un pago",
                        pushFlow = viewModel.notifyPaymentConfirmedPush,
                        emailFlow = viewModel.notifyPaymentConfirmedEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Alguien me recuerda una deuda",
                        pushFlow = viewModel.notifyPaymentReminderPush,
                        emailFlow = viewModel.notifyPaymentReminderEmail
                    )
                    HorizontalDivider(color = BrandBorder)
                    NotificationRow(
                        label = "Se simplifican las deudas del grupo",
                        pushFlow = viewModel.notifyDebtSimplifiedPush,
                        emailFlow = viewModel.notifyDebtSimplifiedEmail
                    )
                }
            }

            // Category: Resúmenes e informes
            item {
                CategoryHeader(title = "Resúmenes e informes")
            }
            item {
                SettingsCard {
                    SimpleSwitchRow(
                        label = "Resumen diario",
                        flow = viewModel.notifyDailySummary
                    )
                    HorizontalDivider(color = BrandBorder)
                    SimpleSwitchRow(
                        label = "Resumen semanal",
                        flow = viewModel.notifyWeeklySummary
                    )
                    HorizontalDivider(color = BrandBorder)
                    SimpleSwitchRow(
                        label = "Resumen mensual",
                        flow = viewModel.notifyMonthlySummary
                    )
                    HorizontalDivider(color = BrandBorder)
                    SimpleSwitchRow(
                        label = "Noticias de la aplicación",
                        flow = viewModel.notifyAppNews
                    )
                    HorizontalDivider(color = BrandBorder)
                    SimpleSwitchRow(
                        label = "Novedades y actualizaciones",
                        flow = viewModel.notifyAppUpdates
                    )
                    HorizontalDivider(color = BrandBorder)
                    SimpleSwitchRow(
                        label = "Consejos de uso",
                        flow = viewModel.notifyUsageTips
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun NotificationRow(
    label: String,
    pushFlow: MutableStateFlow<Boolean>,
    emailFlow: MutableStateFlow<Boolean>
) {
    val pushSelected by pushFlow.collectAsState()
    val emailSelected by emailFlow.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Push", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                Switch(
                    checked = pushSelected,
                    onCheckedChange = { pushFlow.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OnAccentBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BrandBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Email", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                Switch(
                    checked = emailSelected,
                    onCheckedChange = { emailFlow.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OnAccentBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BrandBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }
        }
    }
}

@Composable
fun SimpleSwitchRow(
    label: String,
    flow: MutableStateFlow<Boolean>
) {
    val selected by flow.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = selected,
            onCheckedChange = { flow.value = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OnAccentBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = BrandBorder
            )
        )
    }
}

// Helper extension to scale a component (useful for switches)
fun Modifier.scale(scale: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            (placeable.width * scale).toInt(),
            (placeable.height * scale).toInt()
        ) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
)
