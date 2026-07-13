package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppRepository(private val db: AppDatabase) {

    private val groupDao = db.groupDao()
    private val memberDao = db.memberDao()
    private val expenseDao = db.expenseDao()
    private val settlementDao = db.settlementDao()
    private val commentDao = db.commentDao()
    private val activityLogDao = db.activityLogDao()

    // --- Groups ---
    val allGroups: Flow<List<GroupEntity>> = groupDao.getAllGroups()

    fun getGroup(id: Long): Flow<GroupEntity?> = groupDao.getGroupById(id)

    suspend fun getGroupSync(id: Long): GroupEntity? = groupDao.getGroupByIdSync(id)

    suspend fun insertGroup(group: GroupEntity): Long {
        val id = groupDao.insertGroup(group)
        logActivity(
            groupId = id,
            memberName = "Sistema",
            actionType = "CREATE",
            entityType = "GROUP",
            description = "Grupo '${group.name}' creado"
        )
        return id
    }

    suspend fun updateGroup(group: GroupEntity) {
        groupDao.updateGroup(group)
        logActivity(
            groupId = group.id,
            memberName = "Sistema",
            actionType = "CONFIG_CHANGE",
            entityType = "GROUP",
            description = "Grupo '${group.name}' actualizado"
        )
    }

    suspend fun deleteGroup(group: GroupEntity) {
        groupDao.deleteGroup(group)
    }

    // --- Members ---
    fun getMembers(groupId: Long): Flow<List<MemberEntity>> = memberDao.getMembersForGroup(groupId)

    suspend fun getMembersSync(groupId: Long): List<MemberEntity> = memberDao.getMembersForGroupSync(groupId)

    suspend fun insertMember(member: MemberEntity): Long {
        val id = memberDao.insertMember(member)
        logActivity(
            groupId = member.groupId,
            memberName = "Sistema",
            actionType = "CREATE",
            entityType = "MEMBER",
            description = "Miembro '${member.name}' agregado"
        )
        return id
    }

    suspend fun insertMembers(members: List<MemberEntity>) {
        memberDao.insertMembers(members)
        if (members.isNotEmpty()) {
            val names = members.joinToString(", ") { it.name }
            logActivity(
                groupId = members.first().groupId,
                memberName = "Sistema",
                actionType = "CREATE",
                entityType = "MEMBER",
                description = "Miembros agregados: $names"
            )
        }
    }

    suspend fun updateMember(member: MemberEntity) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: MemberEntity) {
        memberDao.deleteMember(member)
        logActivity(
            groupId = member.groupId,
            memberName = "Sistema",
            actionType = "DELETE",
            entityType = "MEMBER",
            description = "Miembro '${member.name}' eliminado"
        )
    }

    // --- Expenses ---
    fun getExpenses(groupId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesForGroup(groupId)

    suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getExpenseById(id)

    suspend fun saveExpense(
        expense: ExpenseEntity,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
        actorName: String
    ): Long {
        val isNew = expense.id == 0L
        val id = expenseDao.saveExpenseWithDetails(expense, payers, splits)
        logActivity(
            groupId = expense.groupId,
            memberName = actorName,
            actionType = if (isNew) "CREATE" else "EDIT",
            entityType = "EXPENSE",
            description = "${if (isNew) "Agregó" else "Modificó"} gasto '${expense.description}' por ${formatCents(expense.totalAmount, expense.currency)}"
        )
        return id
    }

    suspend fun deleteExpense(expense: ExpenseEntity, actorName: String) {
        // Logical delete
        val deletedExpense = expense.copy(
            status = "DELETED",
            deletedAt = System.currentTimeMillis(),
            deletedBy = actorName
        )
        expenseDao.updateExpense(deletedExpense)
        logActivity(
            groupId = expense.groupId,
            memberName = actorName,
            actionType = "DELETE",
            entityType = "EXPENSE",
            description = "Eliminó gasto '${expense.description}'"
        )
    }

    suspend fun getPayersForExpenseSync(expenseId: Long): List<ExpensePayerEntity> =
        expenseDao.getPayersForExpenseSync(expenseId)

    suspend fun getSplitsForExpenseSync(expenseId: Long): List<ExpenseSplitEntity> =
        expenseDao.getSplitsForExpenseSync(expenseId)

    fun getCommentsForExpense(expenseId: Long): Flow<List<CommentEntity>> =
        commentDao.getCommentsForExpense(expenseId)

    suspend fun insertComment(comment: CommentEntity, groupId: Long) {
        commentDao.insertComment(comment)
        // Note: we fetch the expense to include its description in the activity log
        val expense = expenseDao.getExpenseById(comment.expenseId)
        logActivity(
            groupId = groupId,
            memberName = comment.memberName,
            actionType = "COMMENT",
            entityType = "EXPENSE",
            description = "Comentó en '${expense?.description ?: "Gasto"}': \"${comment.content}\""
        )
    }

    // --- Settlements ---
    fun getSettlements(groupId: Long): Flow<List<SettlementEntity>> = settlementDao.getSettlementsForGroup(groupId)

    suspend fun insertSettlement(settlement: SettlementEntity, actorName: String): Long {
        val id = settlementDao.insertSettlement(settlement)
        val payer = memberDao.getMemberById(settlement.payerMemberId)
        val receiver = memberDao.getMemberById(settlement.receiverMemberId)
        val payerName = payer?.name ?: "Alguien"
        val receiverName = receiver?.name ?: "Alguien"
        logActivity(
            groupId = settlement.groupId,
            memberName = actorName,
            actionType = "SETTLEMENT_INFORM",
            entityType = "SETTLEMENT",
            description = "Registró pago de $payerName a $receiverName por ${formatCents(settlement.amount, "$")} (${settlement.status})"
        )
        return id
    }

    suspend fun confirmSettlement(settlementId: Long, actorName: String) {
        val settlement = settlementDao.getSettlementById(settlementId)
        if (settlement != null) {
            val updated = settlement.copy(status = "CONFIRMED")
            settlementDao.updateSettlement(updated)
            val payer = memberDao.getMemberById(settlement.payerMemberId)
            val receiver = memberDao.getMemberById(settlement.receiverMemberId)
            val payerName = payer?.name ?: "Alguien"
            val receiverName = receiver?.name ?: "Alguien"
            logActivity(
                groupId = settlement.groupId,
                memberName = actorName,
                actionType = "SETTLEMENT_CONFIRM",
                entityType = "SETTLEMENT",
                description = "Confirmó pago de $payerName a $receiverName por ${formatCents(settlement.amount, "$")}"
            )
        }
    }

    suspend fun rejectSettlement(settlementId: Long, actorName: String) {
        val settlement = settlementDao.getSettlementById(settlementId)
        if (settlement != null) {
            val updated = settlement.copy(status = "REJECTED")
            settlementDao.updateSettlement(updated)
            val payer = memberDao.getMemberById(settlement.payerMemberId)
            val receiver = memberDao.getMemberById(settlement.receiverMemberId)
            val payerName = payer?.name ?: "Alguien"
            val receiverName = receiver?.name ?: "Alguien"
            logActivity(
                groupId = settlement.groupId,
                memberName = actorName,
                actionType = "SETTLEMENT_CONFIRM",
                entityType = "SETTLEMENT",
                description = "Rechazó pago de $payerName a $receiverName por ${formatCents(settlement.amount, "$")}"
            )
        }
    }

    // --- Activity Logs ---
    val allActivityLogs: Flow<List<ActivityLogEntity>> = activityLogDao.getAllActivityLogs()

    fun getActivityLogs(groupId: Long): Flow<List<ActivityLogEntity>> = activityLogDao.getActivityLogsForGroup(groupId)

    private suspend fun logActivity(
        groupId: Long,
        memberName: String,
        actionType: String,
        entityType: String,
        description: String
    ) {
        val log = ActivityLogEntity(
            groupId = groupId,
            memberName = memberName,
            actionType = actionType,
            entityType = entityType,
            description = description
        )
        activityLogDao.insertActivityLog(log)
    }

    // --- Calculation on Demand ---
    suspend fun getSimplificationResultSync(groupId: Long, currency: String): SimplificationResult {
        val members = memberDao.getMembersForGroupSync(groupId)
        val payers = expenseDao.getPayersForGroupSync(groupId)
        val splits = expenseDao.getSplitsForGroupSync(groupId)
        val settlements = settlementDao.getSettlementsForGroupSync(groupId)
        return DebtSimplifier.calculateSimplification(members, payers, splits, settlements, currency)
    }

    suspend fun getBalancesSync(groupId: Long, currency: String): List<MemberBalance> {
        val members = memberDao.getMembersForGroupSync(groupId)
        val payers = expenseDao.getPayersForGroupSync(groupId)
        val splits = expenseDao.getSplitsForGroupSync(groupId)
        val settlements = settlementDao.getSettlementsForGroupSync(groupId)
        return DebtSimplifier.calculateBalances(members, payers, splits, settlements, currency)
    }

    suspend fun clearAllData() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    private fun formatCents(cents: Long, symbol: String): String {
        val major = cents / 100.0
        return String.format("%s %.2f", symbol, major)
    }
}
