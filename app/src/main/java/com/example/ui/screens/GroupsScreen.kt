package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Group
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
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun GroupsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.allGroups.collectAsState()
    val balanceMap by viewModel.groupBalancesMap.collectAsState()

    var showArchived by remember { mutableStateOf(false) }

    val filteredGroups = remember(groups, showArchived) {
        groups.filter { it.archived == showArchived }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.AddGroup()) },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("add_group_fab")
                    .padding(bottom = 70.dp) // elevate above navigation bar
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear Grupo",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = BrandBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Title
            Text(
                text = "Tus Grupos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Creá, uní y administrá tus círculos de gastos",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Custom sliding/pill tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { showArchived = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("active_groups_tab"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showArchived) BrandSurfaceElevated else Color.Transparent,
                        contentColor = if (!showArchived) TextPrimary else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Activos", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showArchived = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("archived_groups_tab"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showArchived) BrandSurfaceElevated else Color.Transparent,
                        contentColor = if (showArchived) TextPrimary else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null
                ) {
                    Text("Archivados", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Groups listing
            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.Archive else Icons.Default.Group,
                            contentDescription = "Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (showArchived) "No tenés grupos archivados" else "Aún no creaste ningún grupo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (!showArchived) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.navigateTo(Screen.AddGroup()) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Comenzar Ahora", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredGroups) { group ->
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
            }
        }
    }
}
