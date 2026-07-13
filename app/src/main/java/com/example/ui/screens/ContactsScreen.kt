package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupEntity
import com.example.data.MemberEntity
import com.example.data.DebtTransfer
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ContactInfo(
    val name: String,
    val email: String,
    val avatar: String,
    val sharedGroupCount: Int,
    val balance: Long, // net balance with the user in cents
    val groupsShared: List<GroupEntity>,
    val phone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.allGroups.collectAsState()
    val scope = rememberCoroutineScope()

    var contactsList by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("TODOS") } // TODOS, DEBEN, DEBO, SALDADOS

    var showAddContactDialog by remember { mutableStateOf(false) }

    // Aggregate contacts dynamically from DB
    LaunchedEffect(groups) {
        isLoading = true
        val tempContacts = mutableMapOf<String, MutableList<MemberEntity>>()
        val tempContactGroups = mutableMapOf<String, MutableSet<GroupEntity>>()

        groups.forEach { g ->
            val members = viewModel.getMembersSync(g.id)
            val userMember = members.find { it.isUser }
            members.forEach { m ->
                if (!m.isUser) {
                    tempContacts.getOrPut(m.name) { mutableListOf() }.add(m)
                    tempContactGroups.getOrPut(m.name) { mutableSetOf() }.add(g)
                }
            }
        }

        val calculatedContacts = tempContacts.map { (name, memberInstances) ->
            val firstInstance = memberInstances.first()
            val sharedGroups = tempContactGroups[name]?.toList() ?: emptyList()
            var netBalanceWithUser = 0L

            // Calculate exact debt relations
            sharedGroups.forEach { g ->
                val simplification = viewModel.getSimplificationResultSync(g.id, g.baseCurrency)
                val members = viewModel.getMembersSync(g.id)
                val userMember = members.find { it.isUser }
                if (userMember != null) {
                    // debts from user to this member (user owes them, user is in from, member is in to)
                    val userOwesThisMember = simplification.simplifiedDebts
                        .filter { it.fromMemberId == userMember.id && it.toMemberName.equals(name, ignoreCase = true) }
                        .sumOf { it.amount }
                    // debts from this member to user (member owes user, member is in from, user is in to)
                    val thisMemberOwesUser = simplification.simplifiedDebts
                        .filter { it.fromMemberName.equals(name, ignoreCase = true) && it.toMemberId == userMember.id }
                        .sumOf { it.amount }

                    netBalanceWithUser += (thisMemberOwesUser - userOwesThisMember)
                }
            }

            ContactInfo(
                name = name,
                email = firstInstance.email.ifEmpty { "${name.lowercase()}@ejemplo.com" },
                avatar = firstInstance.avatarEmoji,
                sharedGroupCount = sharedGroups.size,
                balance = netBalanceWithUser,
                groupsShared = sharedGroups,
                phone = firstInstance.phone
            )
        }

        contactsList = calculatedContacts.sortedByDescending { abs(it.balance) }
        isLoading = false
    }

    // Filter and search
    val filteredContacts = contactsList.filter { contact ->
        val matchesSearch = contact.name.contains(searchQuery, ignoreCase = true) || 
                            contact.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "DEBEN" -> contact.balance > 0
            "DEBO" -> contact.balance < 0
            "SALDADOS" -> contact.balance == 0L
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val totalToReceive = contactsList.filter { it.balance > 0 }.sumOf { it.balance }
    val totalToPay = contactsList.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    val netBalanceTotal = totalToReceive - totalToPay

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(BrandSurface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contactos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { showAddContactDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OnAccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_contact_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                HorizontalDivider(color = BrandBorder)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Balance total header banner (Linear inspired card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
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
                                .background(
                                    if (netBalanceTotal >= 0) SoftEmerald else SoftRose,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (netBalanceTotal >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (netBalanceTotal >= 0) EmeraldGreen else RoseCoral
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Balance total de contactos",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${if (netBalanceTotal >= 0) "+" else "-"}$${formatCents(abs(netBalanceTotal))}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalanceTotal >= 0) EmeraldGreen else RoseCoral
                            )
                        }
                    }
                }

                // Search & Filters row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar contacto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_search"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandSurface,
                            unfocusedContainerColor = BrandSurface,
                            focusedBorderColor = OnAccentBlue,
                            unfocusedBorderColor = BrandBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("TODOS", "DEBEN", "DEBO", "SALDADOS")
                    val filterLabels = listOf("Todos", "Te deben", "Debés", "Saldados")

                    filters.forEachIndexed { index, filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filterLabels[index]) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnAccentBlue,
                                selectedLabelColor = Color.White,
                                containerColor = BrandSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                selectedBorderColor = Color.Transparent,
                                borderColor = BrandBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OnAccentBlue)
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👤", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No se encontraron contactos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                "Agregá amigos para empezar a compartir gastos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredContacts) { contact ->
                            ContactItem(contact = contact) {
                                // Navigate to the first shared group or show shared groups
                                if (contact.groupsShared.isNotEmpty()) {
                                    viewModel.setActiveGroupId(contact.groupsShared.first().id)
                                    viewModel.navigateTo(Screen.GroupDetail(contact.groupsShared.first().id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var selectedGroupId by remember { mutableStateOf<Long?>(null) }
        var isError by remember { mutableStateOf(false) }

        if (groups.isNotEmpty() && selectedGroupId == null) {
            selectedGroupId = groups.first().id
        }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Agregar contacto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Para agregar un contacto, debés asignarlo a uno de tus grupos compartidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo (Opcional)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_email"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (Opcional)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_phone"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (groups.isNotEmpty()) {
                        Text("Grupo Compartido", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = BrandSurface)
                        ) {
                            Column {
                                groups.forEach { g ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedGroupId = g.id }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedGroupId == g.id,
                                            onClick = { selectedGroupId = g.id }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(g.imageEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(g.name, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Creá primero un grupo en la pestaña Grupos para poder añadir contactos.",
                            color = RoseCoral,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || selectedGroupId == null) {
                            isError = true
                        } else {
                            viewModel.addGroupMember(
                                groupId = selectedGroupId!!,
                                name = name,
                                email = email,
                                phone = phone,
                                emoji = listOf("👤", "🐱", "🦊", "🐼", "🦊", "🦁", "🐧").random()
                            )
                            showAddContactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnAccentBlue)
                ) {
                    Text("Agregar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ContactItem(
    contact: ContactInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("contact_item_${contact.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BrandBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(contact.avatar, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${contact.sharedGroupCount} grupo(s) compartido(s) • ${contact.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val balance = contact.balance
                when {
                    balance > 0 -> {
                        Text(
                            text = "Te debe",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$${formatCents(balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    balance < 0 -> {
                        Text(
                            text = "Le debés",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseCoral,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$${formatCents(abs(balance))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoseCoral
                        )
                    }
                    else -> {
                        Text(
                            text = "Saldado",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatCents(cents: Long): String {
    val major = cents / 100.0
    return String.format(java.util.Locale.US, "%,.2f", major)
}
