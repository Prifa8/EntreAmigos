package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: MainViewModel,
    groupId: Long,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val group by viewModel.activeGroup.collectAsState()
    val members by viewModel.activeMembers.collectAsState()
    val expenses by viewModel.activeExpenses.collectAsState()
    val settlements by viewModel.activeSettlements.collectAsState()
    val logs by viewModel.activeLogs.collectAsState()
    val balances by viewModel.activeBalances.collectAsState()
    val simplification by viewModel.activeSimplification.collectAsState()

    var activeSubTab by remember { mutableStateOf(initialTab) }

    // Quick add member dialog state
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteName by remember { mutableStateOf("") }
    var inviteEmoji by remember { mutableStateOf("👤") }

    val subTabs = listOf("Gastos", "Balances", "Pagos", "Estadísticas", "Actividad")

    val grp = group
    if (grp == null) {
        Box(modifier = Modifier.fillMaxSize().background(BrandBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldGreen)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BrandSurfaceElevated, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = grp.imageEmoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = grp.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Invite members
                    IconButton(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.testTag("invite_member_button")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Invitar", tint = TextPrimary)
                    }

                    // Archive group
                    IconButton(
                        onClick = {
                            val activeD = simplification?.simplifiedDebts?.filter { it.amount > 0 } ?: emptyList()
                            if (activeD.isNotEmpty()) {
                                Toast.makeText(context, "No podés archivar el grupo si quedan deudas pendientes", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.archiveGroup(groupId, !grp.archived)
                                Toast.makeText(context, if (grp.archived) "Grupo reactivado" else "Grupo archivado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("archive_group_button")
                    ) {
                        Icon(
                            imageVector = if (grp.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archivar",
                            tint = if (grp.archived) EmeraldGreen else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        },
        floatingActionButton = {
            if (activeSubTab == 0 && !grp.archived) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.AddEditExpense(groupId)) },
                    containerColor = EmeraldGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Gasto")
                }
            }
        },
        containerColor = BrandBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBackground)
                .padding(padding)
        ) {
            // Group Description Banner
            if (grp.description.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = grp.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Scrollable Row of Tabs
            ScrollableTabRow(
                selectedTabIndex = activeSubTab,
                containerColor = BrandBackground,
                contentColor = EmeraldGreen,
                edgePadding = 16.dp,
                divider = { Divider(color = BrandSurfaceElevated) },
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                        color = EmeraldGreen
                    )
                }
            ) {
                subTabs.forEachIndexed { index, label ->
                    Tab(
                        selected = activeSubTab == index,
                        onClick = { activeSubTab = index },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeSubTab == index) EmeraldGreen else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("group_subtab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (activeSubTab) {
                    0 -> GastosTab(viewModel, expenses, grp)
                    1 -> BalancesTab(viewModel, balances, simplification, grp, members)
                    2 -> PagosTab(settlements, grp, members, viewModel)
                    3 -> EstadisticasTab(expenses, balances, grp)
                    4 -> ActividadTab(logs)
                }
            }
        }
    }

    // Invite Member Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Agregar Integrante", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Lucas, Fabrizio, Juan") },
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
                            .testTag("invite_name_input")
                    )

                    Text("Avatar / Emoji", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("👤", "🐱", "🐶", "🍕", "🎸", "🚲", "🎨", "🚀").forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (inviteEmoji == em) EmeraldGreen else BrandSurfaceElevated,
                                        CircleShape
                                    )
                                    .clickable { inviteEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inviteName.isNotBlank()) {
                            viewModel.addGroupMember(groupId, inviteName, emoji = inviteEmoji)
                            inviteName = ""
                            showInviteDialog = false
                            Toast.makeText(context, "Integrante agregado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_invite_button")
                ) {
                    Text("Agregar", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancelar", color = TextPrimary)
                }
            },
            containerColor = BrandSurface
        )
    }
}

// --- Nested Sub-Tabs Components ---

@Composable
fun GastosTab(
    viewModel: MainViewModel,
    expenses: List<ExpenseEntity>,
    group: GroupEntity
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No hay gastos registrados en este grupo.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Historial de Gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    val context = LocalContext.current
                    FilledTonalButton(
                        onClick = {
                            val csv = buildString {
                                append("Id,Descripcion,Importe,Categoria,Fecha,Notas\n")
                                expenses.forEach { exp ->
                                    val cleanDesc = exp.description.replace(",", ";")
                                    val cleanCat = exp.category.replace(",", ";")
                                    val cleanNotes = exp.notes.replace(",", ";").replace("\n", " ")
                                    val dateStr = dateFormatter.format(Date(exp.expenseDate))
                                    val amountStr = String.format(Locale.US, "%.2f", exp.totalAmount / 100.0)
                                    append("${exp.id},$cleanDesc,${group.baseCurrency} $amountStr,$cleanCat,$dateStr,$cleanNotes\n")
                                }
                            }
                            try {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Reporte de Gastos - ${group.name}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Exportar Reporte de Gastos"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldGreen.copy(alpha = 0.15f),
                            contentColor = EmeraldGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Exportar", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(expenses) { exp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.ExpenseDetail(group.id, exp.id)) }
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                        .testTag("expense_card_${exp.id}"),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category visual badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BrandSurfaceElevated, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (exp.category) {
                                    "Comida" -> "🍔"
                                    "Supermercado" -> "🛒"
                                    "Alojamiento" -> "🏨"
                                    "Transporte" -> "🚌"
                                    "Combustible" -> "⛽"
                                    "Servicios" -> "⚡"
                                    "Alquiler" -> "🏠"
                                    else -> "🏷️"
                                },
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${exp.category} • ${dateFormatter.format(Date(exp.expenseDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (exp.notes.contains("[Gasto Recurrente:")) {
                                    Box(
                                        modifier = Modifier
                                            .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("🔁 Recurrente", fontSize = 9.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (exp.notes.contains("[Multidivisa:")) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("🌎 Multidivisa", fontSize = 9.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Text(
                            text = formatCents(exp.totalAmount, group.baseCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalancesTab(
    viewModel: MainViewModel,
    balances: List<MemberBalance>,
    simplification: SimplificationResult?,
    group: GroupEntity,
    members: List<MemberEntity>
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var viewSimplified by remember { mutableStateOf(group.simplifyDebtsEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // --- Simplification toggle switch ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simplificar Deudas",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Optimiza las transferencias pendientes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = viewSimplified,
                            onCheckedChange = {
                                viewSimplified = it
                                viewModel.updateGroupDetails(
                                    groupId = group.id,
                                    name = group.name,
                                    description = group.description,
                                    type = group.type,
                                    emoji = group.imageEmoji,
                                    currency = group.baseCurrency,
                                    simplify = it
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BrandSurfaceElevated
                            ),
                            modifier = Modifier.testTag("tab_simplify_switch")
                        )
                    }

                    if (viewSimplified && simplification != null && simplification.avoidedCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BrandSurfaceElevated)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Redujimos ${simplification.beforeCount} deudas a ${simplification.afterCount} transferencias directas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // --- Members Net Balances ---
        item {
            Text(
                text = "Balances Individuales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(balances) { b ->
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
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = b.avatarEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = b.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Gastó ${formatCents(b.totalPaid, group.baseCurrency)} • Consumió ${formatCents(b.totalOwed, group.baseCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    val balanceColor = when {
                        b.netBalance > 0 -> EmeraldGreen
                        b.netBalance < 0 -> RoseCoral
                        else -> TextSecondary
                    }
                    val balanceSign = when {
                        b.netBalance > 0 -> "+"
                        else -> ""
                    }

                    Text(
                        text = "$balanceSign${formatCents(b.netBalance, group.baseCurrency)}",
                        color = balanceColor,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // --- Active Pending Transfers Title ---
        item {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transferencias para saldar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val activeTransfers = if (viewSimplified) {
                                simplification?.simplifiedDebts ?: emptyList()
                            } else {
                                simplification?.originalDebts ?: emptyList()
                            }
                            val shareText = buildString {
                                append("📊 *Resumen de Gastos: ${group.name}* 📊\n")
                                if (group.description.isNotBlank()) {
                                    append("${group.description}\n")
                                }
                                append("\n💸 *Transferencias para saldar:*\n")
                                if (activeTransfers.isEmpty()) {
                                    append("✅ ¡No hay transferencias pendientes! El grupo está al día.")
                                } else {
                                    activeTransfers.forEach { t ->
                                        append("• ${t.fromMemberEmoji} *${t.fromMemberName}* debe a ${t.toMemberEmoji} *${t.toMemberName}* -> *${formatCents(t.amount, group.baseCurrency)}*\n")
                                    }
                                }
                                append("\n📱 _Enviado desde EntreTodos App_")
                            }
                            
                            try {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                val chooserIntent = android.content.Intent.createChooser(
                                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    },
                                    "Compartir resumen"
                                )
                                context.startActivity(chooserIntent)
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldGreen.copy(alpha = 0.15f),
                            contentColor = EmeraldGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            val activeTransfers = if (viewSimplified) {
                                simplification?.simplifiedDebts ?: emptyList()
                            } else {
                                simplification?.originalDebts ?: emptyList()
                            }
                            val shareText = buildString {
                                append("📊 *Resumen de Gastos: ${group.name}* 📊\n")
                                if (group.description.isNotBlank()) {
                                    append("${group.description}\n")
                                }
                                append("\n💸 *Transferencias para saldar:*\n")
                                if (activeTransfers.isEmpty()) {
                                    append("✅ ¡No hay transferencias pendientes! El grupo está al día.")
                                } else {
                                    activeTransfers.forEach { t ->
                                        append("• ${t.fromMemberEmoji} *${t.fromMemberName}* debe a ${t.toMemberEmoji} *${t.toMemberName}* -> *${formatCents(t.amount, group.baseCurrency)}*\n")
                                    }
                                }
                                append("\n📱 _Enviado desde EntreTodos App_")
                            }
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(shareText))
                            Toast.makeText(context, "Resumen copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- Transfers list ---
        val activeTransfers = if (viewSimplified) {
            simplification?.simplifiedDebts ?: emptyList()
        } else {
            simplification?.originalDebts ?: emptyList()
        }

        if (activeTransfers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("¡No hay transferencias pendientes! El grupo está al día.", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(activeTransfers) { t ->
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = t.fromMemberEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = t.fromMemberName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp).padding(horizontal = 2.dp))
                                Text(text = t.toMemberEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = t.toMemberName, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCents(t.amount, group.baseCurrency),
                                fontWeight = FontWeight.Black,
                                color = RoseCoral,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val formattedAmount = formatCents(t.amount, group.baseCurrency)
                                    val debtText = "¡Hola! En el grupo '${group.name}', me debes $formattedAmount. ¡Gracias! 💸"
                                    
                                    // Copy to clipboard
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(debtText))
                                    Toast.makeText(context, "Recordatorio copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                    
                                    // Share to WhatsApp / System share
                                    try {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, debtText)
                                            setPackage("com.whatsapp")
                                        }
                                        context.startActivity(sendIntent)
                                    } catch (e: Exception) {
                                        val chooserIntent = android.content.Intent.createChooser(
                                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, debtText)
                                            },
                                            "Compartir recordatorio"
                                        )
                                        context.startActivity(chooserIntent)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .testTag("whatsapp_share_debt_${t.fromMemberId}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar por WhatsApp",
                                    tint = Color(0xFF128C7E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (!group.archived) {
                                Button(
                                    onClick = {
                                        viewModel.navigateTo(
                                            Screen.RegisterPayment(
                                                groupId = group.id,
                                                payerId = t.fromMemberId,
                                                receiverId = t.toMemberId,
                                                amount = t.amount
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp).testTag("register_payment_button_${t.fromMemberId}")
                                ) {
                                    Text("Saldar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PagosTab(
    settlements: List<SettlementEntity>,
    group: GroupEntity,
    members: List<MemberEntity>,
    viewModel: MainViewModel
) {
    if (settlements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No hay pagos registrados aún en este grupo.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            items(settlements) { s ->
                val payer = members.find { it.id == s.payerMemberId }
                val receiver = members.find { it.id == s.receiverMemberId }

                val payerName = payer?.name ?: "Alguien"
                val receiverName = receiver?.name ?: "Alguien"

                val payerAvatar = payer?.avatarEmoji ?: "👤"
                val receiverAvatar = receiver?.avatarEmoji ?: "👤"

                val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBorder, RoundedCornerShape(14.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = payerAvatar, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = payerName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.CheckCircle, contentDescription = "Paid to", tint = EmeraldGreen, modifier = Modifier.size(14.dp).padding(horizontal = 4.dp))
                                Text(text = receiverAvatar, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = receiverName, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Medio: ${s.paymentMethod} • ${dateFormatter.format(Date(s.paymentDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (s.notes.isNotBlank()) {
                                Text(
                                    text = "\"${s.notes}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = formatCents(s.amount, group.baseCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticasTab(
    expenses: List<ExpenseEntity>,
    balances: List<MemberBalance>,
    group: GroupEntity
) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No hay estadísticas disponibles.", color = TextSecondary)
            }
        }
        return
    }

    val totalSpent = expenses.sumOf { it.totalAmount }
    val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.totalAmount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // --- Total Spent Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "TOTAL GASTADO DEL GRUPO", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = formatCents(totalSpent, group.baseCurrency), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = EmeraldGreen)
                    Text(text = "${expenses.size} consumos registrados", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // --- Category Breakdown ---
        item {
            Text(text = "Gastos por Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(categoryTotals.toList().sortedByDescending { it.second }) { (category, amt) ->
            val pct = if (totalSpent > 0) (amt.toDouble() / totalSpent.toDouble() * 100) else 0.0
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
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = category, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(text = String.format(Locale.US, "%.1f%% del total", pct), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(text = formatCents(amt, group.baseCurrency), color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActividadTab(
    logs: List<ActivityLogEntity>
) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay movimientos registrados en este grupo.", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            items(logs) { log ->
                ActivityLogItem(log = log)
            }
        }
    }
}
