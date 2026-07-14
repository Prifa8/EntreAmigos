package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupEntity
import com.example.data.DebtTransfer
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userAlias by viewModel.userAlias.collectAsState()
    val globalToReceive by viewModel.globalTotalToReceive.collectAsState()
    val globalToPay by viewModel.globalTotalToPay.collectAsState()
    val groups by viewModel.allGroups.collectAsState()
    val balanceMap by viewModel.groupBalancesMap.collectAsState()

    // Calculate overall balance
    val netBalance = globalToReceive - globalToPay

    // Gather pending debts where user is involved
    val userPendingDebts = remember(groups, balanceMap) {
        mutableListOf<Pair<GroupEntity, DebtTransfer>>()
    }

    // Reactively calculate simplified transfers per group involving the user
    var loadedDebts by remember { mutableStateOf(false) }
    LaunchedEffect(groups, balanceMap) {
        userPendingDebts.clear()
        groups.forEach { g ->
            val result = viewModel.getSimplificationResultSync(g.id, g.baseCurrency)
            val members = viewModel.getMembersSync(g.id)
            val userMem = members.find { it.isUser }
            if (userMem != null) {
                val groupUserDebts = result.simplifiedDebts.filter { 
                    it.fromMemberId == userMem.id || it.toMemberId == userMem.id 
                }
                groupUserDebts.forEach { d ->
                    userPendingDebts.add(Pair(g, d))
                }
            }
        }
        loadedDebts = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Greeting Header ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, $userAlias",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Controlá tus gastos compartidos hoy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        letterSpacing = 0.1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OnAccentBlue.copy(alpha = 0.08f))
                        .border(1.dp, BrandBorder, CircleShape)
                        .clickable { viewModel.selectTab(MainTab.PERFIL) }
                        .testTag("avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = userAlias.trim().firstOrNull()?.uppercase() ?: "P"
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnAccentBlue
                    )
                }
            }
        }

        // --- Financial Summary Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(1.dp, BrandBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "BALANCE NETO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val balanceSign = when {
                        netBalance > 0 -> "+"
                        else -> ""
                    }
                    val balanceColor = when {
                        netBalance > 0 -> EmeraldGreen
                        netBalance < 0 -> RoseCoral
                        else -> TextPrimary
                    }

                    Text(
                        text = "$balanceSign${formatCents(netBalance)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = balanceColor,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = BrandBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Te deben
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TE DEBEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCents(globalToReceive),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreen,
                                letterSpacing = 0.1.sp
                            )
                        }

                        // Debés
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEBÉS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCents(globalToPay),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoseCoral,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Recent Groups Title ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tus Grupos Activos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.selectTab(MainTab.GRUPOS) }
                ) {
                    Text("Ver todos", color = EmeraldGreen)
                }
            }
        }

        // --- Recent Groups List ---
        val activeGroups = groups.filter { !it.archived }
        if (activeGroups.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "Aún no tenés grupos creados.",
                    buttonText = "Crear un grupo",
                    onClick = { viewModel.navigateTo(Screen.AddGroup()) }
                )
            }
        } else {
            items(activeGroups.take(3)) { group ->
                val personalBalance = balanceMap[group.id] ?: 0L
                GroupCard(
                    group = group,
                    balance = personalBalance,
                    onClick = {
                        viewModel.setActiveGroupId(group.id)
                        viewModel.navigateTo(Screen.GroupDetail(group.id))
                    }
                )
            }
        }

        // --- Actionable Pending Debts Title ---
        item {
            Text(
                text = "Deudas Pendientes del Grupo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // --- Actionable Pending Debts List ---
        val userDebtsFiltered = userPendingDebts.filter { (_, debt) -> debt.amount > 0 }
        if (userDebtsFiltered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Saldado",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "¡Estás al día! No tenés deudas pendientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            items(userDebtsFiltered) { (group, debt) ->
                PendingDebtActionCard(
                    groupName = group.name,
                    groupEmoji = group.imageEmoji,
                    debt = debt,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun GroupCard(
    group: GroupEntity,
    balance: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
            .testTag("group_card_${group.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(BrandSurfaceElevated, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = group.imageEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (group.type) {
                        "VIAJE" -> "Viaje"
                        "HOGAR" -> "Hogar"
                        "PAREJA" -> "Pareja"
                        "AMIGOS" -> "Amigos"
                        "EVENTO" -> "Evento"
                        "TRABAJO" -> "Trabajo"
                        else -> "Grupo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val balanceColor = when {
                    balance > 0 -> EmeraldGreen
                    balance < 0 -> RoseCoral
                    else -> TextSecondary
                }
                val balanceText = when {
                    balance > 0 -> "Te deben"
                    balance < 0 -> "Debés"
                    else -> "Saldado"
                }

                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = if (balance == 0L) "—" else formatCents(abs(balance), group.baseCurrency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
fun PendingDebtActionCard(
    groupName: String,
    groupEmoji: String,
    debt: DebtTransfer,
    viewModel: MainViewModel
) {
    val isDebtor = debt.fromMemberName == viewModel.userAlias.collectAsState().value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = groupEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (isDebtor) RoseCoral.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isDebtor) "Debés" else "Te deben",
                        color = if (isDebtor) RoseCoral else EmeraldGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = debt.fromMemberEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = debt.fromMemberName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "to",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(horizontal = 4.dp)
                        )
                        Text(text = debt.toMemberEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = debt.toMemberName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCents(debt.amount, debt.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isDebtor) RoseCoral else EmeraldGreen
                    )
                }

                if (isDebtor) {
                    Button(
                        onClick = {
                            viewModel.setActiveGroupId(debt.fromMemberId) // switch to correct active group internally if needed
                            viewModel.navigateTo(
                                Screen.RegisterPayment(
                                    groupId = debt.fromMemberId, // will be resolved in RegisterPayment
                                    payerId = debt.fromMemberId,
                                    receiverId = debt.toMemberId,
                                    amount = debt.amount
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseCoral),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("pay_button_${debt.fromMemberId}")
                    ) {
                        Text("Saldar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Empty",
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun formatCents(cents: Long, symbol: String = "$"): String {
    val major = cents / 100.0
    return String.format(Locale.US, "%s %,.2f", symbol, major).replace(",", ".")
}
