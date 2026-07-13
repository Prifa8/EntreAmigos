package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Database and Repository initialization
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database)

        // ViewModel Factory Injection
        val factory = MainViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val primaryColorName by viewModel.primaryAppColor.collectAsState()
            val darkTheme = when (themeMode) {
                com.example.ui.ThemeMode.LIGHT -> false
                com.example.ui.ThemeMode.DARK -> true
                com.example.ui.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme, primaryColorName = primaryColorName) {
                MainAppEntry(viewModel)
            }
        }
    }
}

@Composable
fun MainAppEntry(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    var showQuickAddDialog by remember { mutableStateOf(false) }
    val groups by viewModel.allGroups.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground),
        floatingActionButton = {
            if (currentScreen is Screen.Main) {
                val activeGroups = groups.filter { !it.archived }
                FloatingActionButton(
                    onClick = {
                        if (activeGroups.isEmpty()) {
                            viewModel.navigateTo(Screen.AddGroup())
                        } else if (activeGroups.size == 1) {
                            val singleGroup = activeGroups.first()
                            viewModel.setActiveGroupId(singleGroup.id)
                            viewModel.navigateTo(Screen.AddEditExpense(singleGroup.id))
                        } else {
                            showQuickAddDialog = true
                        }
                    },
                    containerColor = OnAccentBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("main_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Gasto", tint = Color.White)
                }
            }
        },
        bottomBar = {
            if (currentScreen is Screen.Main) {
                Column {
                    HorizontalDivider(color = BrandBorder, thickness = 1.dp)
                    NavigationBar(
                        containerColor = BrandBackground,
                        contentColor = OnAccentBlue,
                        modifier = Modifier.testTag("main_bottom_nav"),
                        windowInsets = WindowInsets.navigationBars,
                        tonalElevation = 0.dp
                    ) {
                        // Home Tab
                        NavigationBarItem(
                            selected = currentTab == MainTab.INICIO,
                            onClick = { viewModel.selectTab(MainTab.INICIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.INICIO) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Inicio"
                                )
                            },
                            label = { Text("Inicio", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnAccentBlue,
                                selectedTextColor = OnAccentBlue,
                                indicatorColor = BrandSurfaceElevated,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("tab_home")
                        )

                        // Contactos Tab
                        NavigationBarItem(
                            selected = currentTab == MainTab.CONTACTOS,
                            onClick = { viewModel.selectTab(MainTab.CONTACTOS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.CONTACTOS) Icons.Filled.People else Icons.Outlined.People,
                                    contentDescription = "Contactos"
                                )
                            },
                            label = { Text("Contactos", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnAccentBlue,
                                selectedTextColor = OnAccentBlue,
                                indicatorColor = BrandSurfaceElevated,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("tab_contacts")
                        )

                        // Groups Tab
                        NavigationBarItem(
                            selected = currentTab == MainTab.GRUPOS,
                            onClick = { viewModel.selectTab(MainTab.GRUPOS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.GRUPOS) Icons.Filled.Group else Icons.Outlined.Group,
                                    contentDescription = "Grupos"
                                )
                            },
                            label = { Text("Grupos", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnAccentBlue,
                                selectedTextColor = OnAccentBlue,
                                indicatorColor = BrandSurfaceElevated,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("tab_groups")
                        )

                        // Activity Tab
                        NavigationBarItem(
                            selected = currentTab == MainTab.ACTIVIDAD,
                            onClick = { viewModel.selectTab(MainTab.ACTIVIDAD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.ACTIVIDAD) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "Actividad"
                                )
                            },
                            label = { Text("Actividad", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnAccentBlue,
                                selectedTextColor = OnAccentBlue,
                                indicatorColor = BrandSurfaceElevated,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("tab_activity")
                        )

                        // Profile Tab
                        NavigationBarItem(
                            selected = currentTab == MainTab.PERFIL,
                            onClick = { viewModel.selectTab(MainTab.PERFIL) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.PERFIL) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Perfil"
                                )
                            },
                            label = { Text("Perfil", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnAccentBlue,
                                selectedTextColor = OnAccentBlue,
                                indicatorColor = BrandSurfaceElevated,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("tab_profile")
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (currentScreen is Screen.Main) innerPadding.calculateBottomPadding() else 0.dp,
                    top = innerPadding.calculateTopPadding()
                ),
            color = BrandBackground
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    when (currentTab) {
                        MainTab.INICIO -> HomeScreen(viewModel)
                        MainTab.CONTACTOS -> ContactsScreen(viewModel)
                        MainTab.GRUPOS -> GroupsScreen(viewModel)
                        MainTab.ACTIVIDAD -> ActivityLogScreen(viewModel)
                        MainTab.PERFIL -> ProfileScreen(viewModel)
                    }
                }
                is Screen.GroupDetail -> {
                    GroupDetailScreen(viewModel, screen.groupId, screen.initialTab)
                }
                is Screen.AddEditExpense -> {
                    AddExpenseScreen(viewModel, screen.groupId, screen.expenseId)
                }
                is Screen.ExpenseDetail -> {
                    ExpenseDetailScreen(viewModel, screen.groupId, screen.expenseId)
                }
                is Screen.AddGroup -> {
                    AddGroupScreen(viewModel)
                }
                is Screen.RegisterPayment -> {
                    RegisterPaymentScreen(
                        viewModel = viewModel,
                        groupId = screen.groupId,
                        initialPayerId = screen.payerId,
                        initialReceiverId = screen.receiverId,
                        initialAmountCents = screen.amount
                    )
                }
                is Screen.NotificationsSettings -> {
                    NotificationsSettingsScreen(viewModel)
                }
                is Screen.SecuritySettings -> {
                    SecuritySettingsScreen(viewModel)
                }
                is Screen.AppearanceSettings -> {
                    AppearanceSettingsScreen(viewModel)
                }
            }
        }
    }

    if (showQuickAddDialog) {
        val activeGroups = groups.filter { !it.archived }
        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            title = {
                Text(
                    text = "Seleccionar Grupo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Seleccioná en qué grupo querés registrar el nuevo gasto:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        letterSpacing = 0.1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    activeGroups.forEach { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showQuickAddDialog = false
                                    viewModel.setActiveGroupId(group.id)
                                    viewModel.navigateTo(Screen.AddEditExpense(group.id))
                                }
                                .border(1.dp, BrandBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = BrandSurface),
                            shape = RoundedCornerShape(14.dp)
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
                                        text = group.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
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
                                        color = TextMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showQuickAddDialog = false }
                ) {
                    Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = BrandBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
