package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Main : Screen // bottom tabs internally
    data class GroupDetail(val groupId: Long, val initialTab: Int = 0) : Screen
    data class AddEditExpense(val groupId: Long, val expenseId: Long = 0L) : Screen
    data class ExpenseDetail(val groupId: Long, val expenseId: Long) : Screen
    data class AddGroup(val groupId: Long = 0L) : Screen
    data class RegisterPayment(val groupId: Long, val payerId: Long, val receiverId: Long, val amount: Long = 0L) : Screen
    object NotificationsSettings : Screen
    object SecuritySettings : Screen
    object AppearanceSettings : Screen
}

enum class MainTab {
    INICIO, CONTACTOS, GRUPOS, ACTIVIDAD, PERFIL
}

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

class MainViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.INICIO)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val screenHistory = mutableListOf<Screen>()

    // --- Filter & Active States ---
    private val _activeGroupId = MutableStateFlow<Long?>(null)
    val activeGroupId: StateFlow<Long?> = _activeGroupId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- User Profile (Local mock profile for customization) ---
    val userAlias = MutableStateFlow("Fabrizio")
    val userCbu = MutableStateFlow("0000003100001234567890")
    val userAliasMP = MutableStateFlow("entretodos.mp")

    // --- Appearance Settings ---
    val themeMode = MutableStateFlow(ThemeMode.AUTO)
    val primaryAppColor = MutableStateFlow("Azul Stripe") // Azul Stripe, Verde Revolut, Slate Linear, Coral Monzo
    val textSizeMultiplier = MutableStateFlow(1.0f)
    val appLanguage = MutableStateFlow("Español")

    // --- Security Settings ---
    val faceIdEnabled = MutableStateFlow(false)
    val fingerprintEnabled = MutableStateFlow(false)
    val pinEnabled = MutableStateFlow(false)
    val autoLockTime = MutableStateFlow("Inmediatamente")

    // --- Notification Settings ---
    // Groups & Contacts
    val notifyGroupAddedPush = MutableStateFlow(true)
    val notifyGroupAddedEmail = MutableStateFlow(true)
    val notifyContactAddedPush = MutableStateFlow(true)
    val notifyContactAddedEmail = MutableStateFlow(false)
    val notifyInviteGroupPush = MutableStateFlow(true)
    val notifyInviteGroupEmail = MutableStateFlow(true)
    val notifyLeaveGroupPush = MutableStateFlow(true)
    val notifyLeaveGroupEmail = MutableStateFlow(false)
    val notifyGroupConfigChangedPush = MutableStateFlow(true)
    val notifyGroupConfigChangedEmail = MutableStateFlow(false)

    // Expenses
    val notifyExpenseAddedPush = MutableStateFlow(true)
    val notifyExpenseAddedEmail = MutableStateFlow(true)
    val notifyExpenseEditedPush = MutableStateFlow(true)
    val notifyExpenseEditedEmail = MutableStateFlow(false)
    val notifyExpenseDeletedPush = MutableStateFlow(true)
    val notifyExpenseDeletedEmail = MutableStateFlow(false)
    val notifyExpenseCommentPush = MutableStateFlow(true)
    val notifyExpenseCommentEmail = MutableStateFlow(true)
    val notifyExpenseDuePush = MutableStateFlow(true)
    val notifyExpenseDueEmail = MutableStateFlow(true)
    val notifyPaymentRegisteredPush = MutableStateFlow(true)
    val notifyPaymentRegisteredEmail = MutableStateFlow(true)
    val notifyPaymentConfirmedPush = MutableStateFlow(true)
    val notifyPaymentConfirmedEmail = MutableStateFlow(false)
    val notifyPaymentReminderPush = MutableStateFlow(true)
    val notifyPaymentReminderEmail = MutableStateFlow(true)
    val notifyDebtSimplifiedPush = MutableStateFlow(true)
    val notifyDebtSimplifiedEmail = MutableStateFlow(false)

    // Summaries
    val notifyDailySummary = MutableStateFlow(true)
    val notifyWeeklySummary = MutableStateFlow(true)
    val notifyMonthlySummary = MutableStateFlow(false)
    val notifyAppNews = MutableStateFlow(false)
    val notifyAppUpdates = MutableStateFlow(true)
    val notifyUsageTips = MutableStateFlow(false)

    // --- Reactive Data Flows ---
    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGroup: StateFlow<GroupEntity?> = _activeGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMembers: StateFlow<List<MemberEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMembers(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeExpenses: StateFlow<List<ExpenseEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getExpenses(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSettlements: StateFlow<List<SettlementEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSettlements(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLogs: StateFlow<List<ActivityLogEntity>> = _activeGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getActivityLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalActivityLogs: StateFlow<List<ActivityLogEntity>> = repository.allActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Calculated States (Balances & Simplifications) ---
    private val _activeBalances = MutableStateFlow<List<MemberBalance>>(emptyList())
    val activeBalances: StateFlow<List<MemberBalance>> = _activeBalances.asStateFlow()

    private val _activeSimplification = MutableStateFlow<SimplificationResult?>(null)
    val activeSimplification: StateFlow<SimplificationResult?> = _activeSimplification.asStateFlow()

    // --- Recent Activities & Global Financials across ALL Groups ---
    private val _globalTotalToReceive = MutableStateFlow(0L)
    val globalTotalToReceive: StateFlow<Long> = _globalTotalToReceive.asStateFlow()

    private val _globalTotalToPay = MutableStateFlow(0L)
    val globalTotalToPay: StateFlow<Long> = _globalTotalToPay.asStateFlow()

    private val _groupBalancesMap = MutableStateFlow<Map<Long, Long>>(emptyMap()) // groupId -> user balance in that group
    val groupBalancesMap: StateFlow<Map<Long, Long>> = _groupBalancesMap.asStateFlow()

    init {
        // Automatically seed data if empty
        viewModelScope.launch {
            allGroups.collect { list ->
                if (list.isEmpty()) {
                    seedDatabase()
                } else {
                    calculateGlobalFinancials(list)
                }
            }
        }

        // Listen to active group changes to recalculate active group details
        viewModelScope.launch {
            combine(_activeGroupId, activeMembers, activeExpenses, activeSettlements) { groupId, members, _, settlements ->
                if (groupId != null && members.isNotEmpty()) {
                    val group = repository.getGroupSync(groupId)
                    val currency = group?.baseCurrency ?: "$"
                    recalculateActiveGroupStats(groupId, currency)
                }
            }
            .collect()
        }
    }

    // --- Seeding Data ---
    private suspend fun seedDatabase() {
        // Group 1: Viaje a Bariloche 🏔️
        val g1Id = repository.insertGroup(
            GroupEntity(
                name = "Viaje a Bariloche",
                description = "Gastos de las vacaciones en la nieve",
                type = "VIAJE",
                imageEmoji = "🏔️",
                baseCurrency = "$"
            )
        )
        val m1 = MemberEntity(groupId = g1Id, name = "Fabrizio", isUser = true, avatarEmoji = "🍕")
        val m2 = MemberEntity(groupId = g1Id, name = "Juan", avatarEmoji = "🎸")
        val m3 = MemberEntity(groupId = g1Id, name = "Lucas", avatarEmoji = "🚲")

        val m1Id = repository.insertMember(m1)
        val m2Id = repository.insertMember(m2)
        val m3Id = repository.insertMember(m3)

        // Expense 1: Cena de bienvenida por $60.000 (cents: 6000000), split equal
        val e1 = ExpenseEntity(
            groupId = g1Id,
            description = "Cena de Bienvenida",
            totalAmount = 6000000,
            category = "Comida",
            notes = "Asado completo con bebidas en el Quincho"
        )
        val ep1 = listOf(ExpensePayerEntity(expenseId = 0, memberId = m1Id, amountPaid = 6000000))
        val es1 = listOf(
            ExpenseSplitEntity(expenseId = 0, memberId = m1Id, amountOwed = 2000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = m2Id, amountOwed = 2000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = m3Id, amountOwed = 2000000, splitType = "EQUAL")
        )
        repository.saveExpense(e1, ep1, es1, "Fabrizio")

        // Expense 2: Alojamiento Cabaña por $150.000 (cents: 15000000), split equal paid by Lucas
        val e2 = ExpenseEntity(
            groupId = g1Id,
            description = "Alquiler de Cabaña",
            totalAmount = 15000000,
            category = "Alojamiento",
            notes = "Seña y saldo de la cabaña por 3 noches"
        )
        val ep2 = listOf(ExpensePayerEntity(expenseId = 0, memberId = m3Id, amountPaid = 15000000))
        val es2 = listOf(
            ExpenseSplitEntity(expenseId = 0, memberId = m1Id, amountOwed = 5000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = m2Id, amountOwed = 5000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = m3Id, amountOwed = 5000000, splitType = "EQUAL")
        )
        repository.saveExpense(e2, ep2, es2, "Lucas")

        // Add a confirmed settlement: Juan pays Fabrizio $10,000
        val s1 = SettlementEntity(
            groupId = g1Id,
            payerMemberId = m2Id,
            receiverMemberId = m1Id,
            amount = 1000000,
            paymentMethod = "Transferencia MP",
            notes = "Adelanto cena de anoche",
            status = "CONFIRMED"
        )
        repository.insertSettlement(s1, "Juan")

        // Group 2: Departamento Compartido 🏠 (To demonstrate perfect debt simplification)
        val g2Id = repository.insertGroup(
            GroupEntity(
                name = "Departamento",
                description = "Gastos mensuales del departamento",
                type = "HOGAR",
                imageEmoji = "🏠",
                baseCurrency = "$"
            )
        )
        val mg1 = MemberEntity(groupId = g2Id, name = "Ana", isUser = true, avatarEmoji = "🌸")
        val mg2 = MemberEntity(groupId = g2Id, name = "Bruno", avatarEmoji = "☕")
        val mg3 = MemberEntity(groupId = g2Id, name = "Carla", avatarEmoji = "🎨")

        val mg1Id = repository.insertMember(mg1)
        val mg2Id = repository.insertMember(mg2)
        val mg3Id = repository.insertMember(mg3)

        // Expense A: Carla pays Expensas $60,000, split equally (each owes $20,000)
        val ea = ExpenseEntity(
            groupId = g2Id,
            description = "Expensas Edificio",
            totalAmount = 6000000,
            category = "Alquiler",
            notes = "Expensas ordinarias del mes de Julio"
        )
        val epa = listOf(ExpensePayerEntity(expenseId = 0, memberId = mg3Id, amountPaid = 6000000))
        val esa = listOf(
            ExpenseSplitEntity(expenseId = 0, memberId = mg1Id, amountOwed = 2000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = mg2Id, amountOwed = 2000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = mg3Id, amountOwed = 2000000, splitType = "EQUAL")
        )
        repository.saveExpense(ea, epa, esa, "Carla")

        // Expense B: Bruno pays Internet $30,000, split equally (each owes $10,000)
        val eb = ExpenseEntity(
            groupId = g2Id,
            description = "Internet Fibertel",
            totalAmount = 3000000,
            category = "Servicios",
            notes = "Abono 300 megas"
        )
        val epb = listOf(ExpensePayerEntity(expenseId = 0, memberId = mg2Id, amountPaid = 3000000))
        val esb = listOf(
            ExpenseSplitEntity(expenseId = 0, memberId = mg1Id, amountOwed = 1000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = mg2Id, amountOwed = 1000000, splitType = "EQUAL"),
            ExpenseSplitEntity(expenseId = 0, memberId = mg3Id, amountOwed = 1000000, splitType = "EQUAL")
        )
        repository.saveExpense(eb, epb, esb, "Bruno")
    }

    // --- Recalculating Calculations ---
    private suspend fun recalculateActiveGroupStats(groupId: Long, currency: String) {
        val result = repository.getSimplificationResultSync(groupId, currency)
        val balances = repository.getBalancesSync(groupId, currency)
        _activeSimplification.value = result
        _activeBalances.value = balances
    }

    private suspend fun calculateGlobalFinancials(groups: List<GroupEntity>) {
        var totalToReceive = 0L
        var totalToPay = 0L
        val balanceMap = mutableMapOf<Long, Long>()

        for (g in groups) {
            val members = repository.getMembersSync(g.id)
            val userMember = members.find { it.isUser }
            if (userMember != null) {
                val balances = repository.getBalancesSync(g.id, g.baseCurrency)
                val userBalanceObj = balances.find { it.memberId == userMember.id }
                val net = userBalanceObj?.netBalance ?: 0L
                balanceMap[g.id] = net
                if (net > 0L) {
                    totalToReceive += net
                } else if (net < 0L) {
                    totalToPay += -net
                }
            }
        }
        _globalTotalToReceive.value = totalToReceive
        _globalTotalToPay.value = totalToPay
        _groupBalancesMap.value = balanceMap
    }

    // --- Public Operations / Actions ---

    suspend fun getExpenseById(id: Long): ExpenseEntity? = repository.getExpenseById(id)
    suspend fun getPayersForExpenseSync(expenseId: Long): List<ExpensePayerEntity> = repository.getPayersForExpenseSync(expenseId)
    suspend fun getSplitsForExpenseSync(expenseId: Long): List<ExpenseSplitEntity> = repository.getSplitsForExpenseSync(expenseId)
    suspend fun getSimplificationResultSync(groupId: Long, currency: String): SimplificationResult = repository.getSimplificationResultSync(groupId, currency)
    suspend fun getMembersSync(groupId: Long): List<MemberEntity> = repository.getMembersSync(groupId)

    fun navigateTo(screen: Screen) {
        screenHistory.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            _currentScreen.value = screenHistory.removeAt(screenHistory.size - 1)
        } else {
            _currentScreen.value = Screen.Main
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        _currentScreen.value = Screen.Main
    }

    fun setActiveGroupId(groupId: Long?) {
        _activeGroupId.value = groupId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createGroup(name: String, description: String, type: String, emoji: String, currency: String, simplify: Boolean) {
        viewModelScope.launch {
            val group = GroupEntity(
                name = name,
                description = description,
                type = type,
                imageEmoji = emoji,
                baseCurrency = currency,
                simplifyDebtsEnabled = simplify
            )
            val id = repository.insertGroup(group)
            // Add current user as default member
            val creator = MemberEntity(groupId = id, name = userAlias.value, isUser = true, avatarEmoji = "👤")
            repository.insertMember(creator)
            navigateTo(Screen.GroupDetail(id))
        }
    }

    fun updateGroupDetails(groupId: Long, name: String, description: String, type: String, emoji: String, currency: String, simplify: Boolean) {
        viewModelScope.launch {
            val existing = repository.getGroupSync(groupId) ?: return@launch
            val updated = existing.copy(
                name = name,
                description = description,
                type = type,
                imageEmoji = emoji,
                baseCurrency = currency,
                simplifyDebtsEnabled = simplify
            )
            repository.updateGroup(updated)
            recalculateActiveGroupStats(groupId, currency)
        }
    }

    fun archiveGroup(groupId: Long, archive: Boolean) {
        viewModelScope.launch {
            val existing = repository.getGroupSync(groupId) ?: return@launch
            val updated = existing.copy(archived = archive)
            repository.updateGroup(updated)
            recalculateActiveGroupStats(groupId, existing.baseCurrency)
        }
    }

    fun addGroupMember(groupId: Long, name: String, email: String = "", phone: String = "", emoji: String = "👤") {
        viewModelScope.launch {
            val member = MemberEntity(groupId = groupId, name = name, email = email, phone = phone, avatarEmoji = emoji)
            repository.insertMember(member)
            val group = repository.getGroupSync(groupId)
            if (group != null) recalculateActiveGroupStats(groupId, group.baseCurrency)
        }
    }

    fun deleteGroupMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.deleteMember(member)
            val group = repository.getGroupSync(member.groupId)
            if (group != null) recalculateActiveGroupStats(member.groupId, group.baseCurrency)
        }
    }

    fun saveExpenseWithDetails(
        groupId: Long,
        expenseId: Long,
        description: String,
        totalAmount: Long,
        category: String,
        notes: String,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
        actorName: String
    ) {
        viewModelScope.launch {
            val group = repository.getGroupSync(groupId) ?: return@launch
            val exp = ExpenseEntity(
                id = expenseId,
                groupId = groupId,
                description = description,
                totalAmount = totalAmount,
                currency = group.baseCurrency,
                category = category,
                notes = notes
            )
            repository.saveExpense(exp, payers, splits, actorName)
            recalculateActiveGroupStats(groupId, group.baseCurrency)
            navigateBack()
        }
    }

    fun deleteExpense(expense: ExpenseEntity, actorName: String) {
        viewModelScope.launch {
            repository.deleteExpense(expense, actorName)
            val group = repository.getGroupSync(expense.groupId)
            if (group != null) recalculateActiveGroupStats(expense.groupId, group.baseCurrency)
            navigateBack()
        }
    }

    fun registerSettlement(
        groupId: Long,
        payerId: Long,
        receiverId: Long,
        amount: Long,
        method: String,
        notes: String,
        proofUri: String? = null
    ) {
        viewModelScope.launch {
            val group = repository.getGroupSync(groupId) ?: return@launch
            val settlement = SettlementEntity(
                groupId = groupId,
                payerMemberId = payerId,
                receiverMemberId = receiverId,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                status = "CONFIRMED", // For a self-contained local experience, confirm immediately!
                proofUri = proofUri
            )
            repository.insertSettlement(settlement, userAlias.value)
            recalculateActiveGroupStats(groupId, group.baseCurrency)
            navigateBack()
        }
    }

    fun addComment(expenseId: Long, groupId: Long, content: String) {
        viewModelScope.launch {
            val comment = CommentEntity(
                expenseId = expenseId,
                memberName = userAlias.value,
                content = content
            )
            repository.insertComment(comment, groupId)
        }
    }

    fun confirmSettlementState(settlementId: Long, confirm: Boolean) {
        viewModelScope.launch {
            if (confirm) {
                repository.confirmSettlement(settlementId, userAlias.value)
            } else {
                repository.rejectSettlement(settlementId, userAlias.value)
            }
            val activeId = _activeGroupId.value
            if (activeId != null) {
                val group = repository.getGroupSync(activeId)
                if (group != null) recalculateActiveGroupStats(activeId, group.baseCurrency)
            }
        }
    }

    fun getCommentsForExpense(expenseId: Long): Flow<List<CommentEntity>> =
        repository.getCommentsForExpense(expenseId)
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
