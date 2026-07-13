package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroupById(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupByIdSync(id: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    fun getMembersForGroup(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY id ASC")
    suspend fun getMembersForGroupSync(groupId: Long): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND status = 'ACTIVE' ORDER BY expenseDate DESC")
    fun getExpensesForGroup(groupId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpensePayers(payers: List<ExpensePayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun deleteExpensePayersForExpense(expenseId: Long)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteExpenseSplitsForExpense(expenseId: Long)

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    fun getPayersForExpense(expenseId: Long): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpenseSync(expenseId: Long): List<ExpensePayerEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseSync(expenseId: Long): List<ExpenseSplitEntity>

    @Query("SELECT ep.* FROM expense_payers ep INNER JOIN expenses e ON ep.expenseId = e.id WHERE e.groupId = :groupId AND e.status = 'ACTIVE'")
    suspend fun getPayersForGroupSync(groupId: Long): List<ExpensePayerEntity>

    @Query("SELECT es.* FROM expense_splits es INNER JOIN expenses e ON es.expenseId = e.id WHERE e.groupId = :groupId AND e.status = 'ACTIVE'")
    suspend fun getSplitsForGroupSync(groupId: Long): List<ExpenseSplitEntity>

    @Transaction
    suspend fun saveExpenseWithDetails(
        expense: ExpenseEntity,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>
    ): Long {
        val expId = if (expense.id == 0L) {
            insertExpense(expense)
        } else {
            updateExpense(expense)
            deleteExpensePayersForExpense(expense.id)
            deleteExpenseSplitsForExpense(expense.id)
            expense.id
        }
        val updatedPayers = payers.map { it.copy(expenseId = expId) }
        val updatedSplits = splits.map { it.copy(expenseId = expId) }
        insertExpensePayers(updatedPayers)
        insertExpenseSplits(updatedSplits)
        return expId
    }
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY paymentDate DESC")
    fun getSettlementsForGroup(groupId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY paymentDate DESC")
    suspend fun getSettlementsForGroupSync(groupId: Long): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun getSettlementById(id: Long): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE expenseId = :expenseId ORDER BY createdAt ASC")
    fun getCommentsForExpense(expenseId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getActivityLogsForGroup(groupId: Long): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long
}
