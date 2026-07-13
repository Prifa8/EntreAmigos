package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: String = "AMIGOS", // VIAJE, HOGAR, PAREJA, AMIGOS, EVENTO, TRABAJO, OTRO
    val imageEmoji: String = "👥",
    val baseCurrency: String = "$",
    val simplifyDebtsEnabled: Boolean = true,
    val ownerId: Long = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val isUser: Boolean = false, // Is this the local app user?
    val avatarEmoji: String = "👤"
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val description: String,
    val totalAmount: Long, // Stored in cents (e.g. 10000 for 100.00)
    val currency: String = "$",
    val category: String = "Comida",
    val expenseDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val location: String = "",
    val status: String = "ACTIVE", // ACTIVE, DELETED
    val deletedAt: Long? = null,
    val deletedBy: String? = null
)

@Entity(tableName = "expense_payers")
data class ExpensePayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val amountPaid: Long // Stored in cents
)

@Entity(tableName = "expense_splits")
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val amountOwed: Long, // Stored in cents
    val percentage: Double = 0.0,
    val shares: Double = 0.0,
    val splitType: String = "EQUAL" // EQUAL, EXACT, PERCENT, SHARES, ADJUST
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val payerMemberId: Long,
    val receiverMemberId: Long,
    val amount: Long, // Stored in cents
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Efectivo", // Efectivo, Transferencia, Billetera MP, etc.
    val notes: String = "",
    val status: String = "CONFIRMED", // PENDING, CONFIRMED, REJECTED
    val proofUri: String? = null
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberName: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val memberName: String,
    val actionType: String, // CREATE, EDIT, DELETE, COMMENT, SETTLEMENT_INFORM, SETTLEMENT_CONFIRM, CONFIG_CHANGE
    val entityType: String, // GROUP, EXPENSE, SETTLEMENT, MEMBER
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
