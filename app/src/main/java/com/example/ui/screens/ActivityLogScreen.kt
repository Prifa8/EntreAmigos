package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityLogEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityLogScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.globalActivityLogs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Historial de Actividad",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Registro completo de gastos, comentarios y pagos",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Sin actividad",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay actividad registrada aún",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(logs) { log ->
                    ActivityLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(
    log: ActivityLogEntity,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (log.actionType) {
        "CREATE" -> when (log.entityType) {
            "MEMBER" -> Pair(Icons.Default.PersonAdd, EmeraldGreen)
            "GROUP" -> Pair(Icons.Default.GroupAdd, EmeraldGreen)
            else -> Pair(Icons.Default.AddCircle, EmeraldGreen)
        }
        "EDIT" -> Pair(Icons.Default.Edit, AmberYellow)
        "DELETE" -> Pair(Icons.Default.Delete, RoseCoral)
        "COMMENT" -> Pair(Icons.Default.Comment, PrimaryLight)
        "SETTLEMENT_INFORM" -> Pair(Icons.Default.Payment, AmberYellow)
        "SETTLEMENT_CONFIRM" -> Pair(Icons.Default.CheckCircle, EmeraldGreen)
        "CONFIG_CHANGE" -> Pair(Icons.Default.Settings, TextSecondary)
        else -> Pair(Icons.Default.History, TextSecondary)
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(log.createdAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
            .testTag("activity_log_item_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = log.actionType,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Por ${log.memberName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(TextMuted, CircleShape)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
