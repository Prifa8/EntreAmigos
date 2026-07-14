package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommentEntity
import com.example.data.ExpenseEntity
import com.example.data.ExpensePayerEntity
import com.example.data.ExpenseSplitEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    viewModel: MainViewModel,
    groupId: Long,
    expenseId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val group by viewModel.activeGroup.collectAsState()
    val members by viewModel.activeMembers.collectAsState()

    var expense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var payers by remember { mutableStateOf<List<ExpensePayerEntity>>(emptyList()) }
    var splits by remember { mutableStateOf<List<ExpenseSplitEntity>>(emptyList()) }
    val comments by viewModel.getCommentsForExpense(expenseId).collectAsState(initial = emptyList())

    var commentInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load expense data asynchronously
    LaunchedEffect(expenseId) {
        expense = viewModel.getExpenseById(expenseId)
        payers = viewModel.getPayersForExpenseSync(expenseId)
        splits = viewModel.getSplitsForExpenseSync(expenseId)
    }

    val exp = expense
    if (exp == null) {
        Box(modifier = Modifier.fillMaxSize().background(BrandBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldGreen)
        }
        return
    }

    val attachedReceiptBitmap = remember(exp.proofUri) {
        val uriStr = exp.proofUri
        if (uriStr == null) null
        else {
            try {
                if (uriStr.startsWith("/")) {
                    BitmapFactory.decodeFile(uriStr)
                } else {
                    val uri = android.net.Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    val payerName = members.find { it.id == payers.firstOrNull()?.memberId }?.name ?: "Alguien"
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(exp.expenseDate))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Gasto", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AddEditExpense(groupId, expenseId)) },
                        modifier = Modifier.testTag("edit_expense_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_expense_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = RoseCoral)
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
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // --- Big Visual Header ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandBorder, RoundedCornerShape(20.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = exp.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatCents(exp.totalAmount, exp.currency),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pagado por $payerName el $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            if (exp.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = BrandSurfaceElevated)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Notes",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = exp.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (exp.proofUri != null) {
                                var showReceiptZoomDialog by remember { mutableStateOf(false) }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = BrandSurfaceElevated)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "📸 Comprobante Adjunto (Toca para ampliar)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (attachedReceiptBitmap != null) {
                                     Card(
                                         shape = RoundedCornerShape(12.dp),
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .height(160.dp)
                                             .clickable { showReceiptZoomDialog = true }
                                             .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
                                     ) {
                                         Image(
                                             bitmap = attachedReceiptBitmap.asImageBitmap(),
                                             contentDescription = "Receipt photo",
                                             contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                     }
                                } else {
                                     Row(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .background(BrandSurfaceElevated, RoundedCornerShape(10.dp))
                                             .padding(12.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                                         Spacer(modifier = Modifier.width(8.dp))
                                         val fileDisplay = if (exp.proofUri!!.contains("/")) exp.proofUri!!.substringAfterLast("/") else "Imagen"
                                         Text(
                                             text = "Comprobante registrado: $fileDisplay",
                                             style = MaterialTheme.typography.bodySmall,
                                             color = TextSecondary
                                         )
                                     }
                                }

                                if (showReceiptZoomDialog && attachedReceiptBitmap != null) {
                                     AlertDialog(
                                         onDismissRequest = { showReceiptZoomDialog = false },
                                         confirmButton = {
                                             TextButton(onClick = { showReceiptZoomDialog = false }) {
                                                 Text("Cerrar", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                             }
                                         },
                                         title = { Text("Visualización del Recibo", fontWeight = FontWeight.Bold, color = TextPrimary) },
                                         text = {
                                             Box(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .height(320.dp)
                                             ) {
                                                 Image(
                                                     bitmap = attachedReceiptBitmap.asImageBitmap(),
                                                     contentDescription = "Receipt Full",
                                                     contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                     modifier = Modifier.fillMaxSize()
                                                 )
                                             }
                                         },
                                         containerColor = BrandSurface,
                                         shape = RoundedCornerShape(16.dp)
                                     )
                                }
                            }
                        }
                    }
                }

                // --- Splits Title ---
                item {
                    Text(
                        text = "Distribución del Gasto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // --- Splits List ---
                items(splits) { s ->
                    val member = members.find { it.id == s.memberId }
                    val name = member?.name ?: "Alguien"
                    val avatar = member?.avatarEmoji ?: "👤"
                    val owesText = formatCents(s.amountOwed, exp.currency)

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
                                Text(text = avatar, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = owesText, color = TextPrimary, fontWeight = FontWeight.Bold)
                                if (s.percentage > 0.0) {
                                    Text(text = "${s.percentage}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                } else if (s.shares > 0.0) {
                                    Text(text = "${s.shares} partes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // --- Comments Title ---
                item {
                    Text(
                        text = "Comentarios y Notas (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // --- Comments List ---
                if (comments.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No hay comentarios en este gasto. ¡Sé el primero!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(comments) { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }

            // --- Comment Input Field ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Escribí un comentario...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedBorderColor = BrandSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (commentInput.isNotBlank()) {
                            viewModel.addComment(expenseId, groupId, commentInput.trim())
                            commentInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(EmeraldGreen, CircleShape)
                        .testTag("send_comment_button"),
                    enabled = commentInput.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar este gasto?", color = TextPrimary) },
            text = { Text("Esta modificación lógica cambiará los balances de los ${members.size} integrantes involucrados en el grupo de gastos.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val currentUser = members.find { it.isUser }?.name ?: "Sistema"
                        viewModel.deleteExpense(exp, currentUser)
                        Toast.makeText(context, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar", color = RoseCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = TextPrimary)
                }
            },
            containerColor = BrandSurface
        )
    }
}

@Composable
fun CommentItem(
    comment: CommentEntity,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.memberName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )

                val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val formattedTime = dateFormatter.format(Date(comment.createdAt))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}
