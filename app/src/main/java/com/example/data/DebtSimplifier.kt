package com.example.data

import kotlin.math.abs
import kotlin.math.min

data class MemberBalance(
    val memberId: Long,
    val name: String,
    val avatarEmoji: String,
    val totalPaid: Long,       // money paid for expenses
    val totalOwed: Long,       // money owed for expenses
    val settledSent: Long,     // money sent in confirmed payments
    val settledReceived: Long, // money received in confirmed payments
    val netBalance: Long       // totalPaid - totalOwed + settledSent - settledReceived
)

data class DebtTransfer(
    val fromMemberId: Long,
    val fromMemberName: String,
    val fromMemberEmoji: String,
    val toMemberId: Long,
    val toMemberName: String,
    val toMemberEmoji: String,
    val amount: Long, // in cents
    val currency: String
)

data class SimplificationResult(
    val originalDebts: List<DebtTransfer>,
    val simplifiedDebts: List<DebtTransfer>,
    val beforeCount: Int,
    val afterCount: Int,
    val avoidedCount: Int
)

object DebtSimplifier {

    /**
     * Calculates the net balances for all members in a group.
     */
    fun calculateBalances(
        members: List<MemberEntity>,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>,
        currency: String
    ): List<MemberBalance> {
        val memberMap = members.associateBy { it.id }

        // Initialize maps
        val paidMap = members.associate { it.id to 0L }.toMutableMap()
        val owedMap = members.associate { it.id to 0L }.toMutableMap()
        val sentMap = members.associate { it.id to 0L }.toMutableMap()
        val receivedMap = members.associate { it.id to 0L }.toMutableMap()

        // 1. Accumulate expense payments
        for (payer in payers) {
            val current = paidMap[payer.memberId] ?: 0L
            paidMap[payer.memberId] = current + payer.amountPaid
        }

        // 2. Accumulate expense splits
        for (split in splits) {
            val current = owedMap[split.memberId] ?: 0L
            owedMap[split.memberId] = current + split.amountOwed
        }

        // 3. Accumulate confirmed settlements
        for (settlement in settlements) {
            if (settlement.status == "CONFIRMED") {
                val sent = sentMap[settlement.payerMemberId] ?: 0L
                sentMap[settlement.payerMemberId] = sent + settlement.amount

                val rec = receivedMap[settlement.receiverMemberId] ?: 0L
                receivedMap[settlement.receiverMemberId] = rec + settlement.amount
            }
        }

        // 4. Construct MemberBalance list
        return members.map { member ->
            val paid = paidMap[member.id] ?: 0L
            val owed = owedMap[member.id] ?: 0L
            val sent = sentMap[member.id] ?: 0L
            val rec = receivedMap[member.id] ?: 0L
            val net = paid - owed + sent - rec

            MemberBalance(
                memberId = member.id,
                name = member.name,
                avatarEmoji = member.avatarEmoji,
                totalPaid = paid,
                totalOwed = owed,
                settledSent = sent,
                settledReceived = rec,
                netBalance = net
            )
        }
    }

    /**
     * Simplifies a set of net balances to the minimum number of transfers.
     */
    fun simplify(
        balances: List<MemberBalance>,
        members: List<MemberEntity>,
        currency: String
    ): List<DebtTransfer> {
        val memberMap = members.associateBy { it.id }
        
        // Step 1 & 2: Separate and order creditors and debtors
        val creditors = balances
            .filter { it.netBalance > 0 }
            .map { Pair(it.memberId, it.netBalance) }
            .sortedByDescending { it.second }
            .toMutableList()

        val debtors = balances
            .filter { it.netBalance < 0 }
            .map { Pair(it.memberId, abs(it.netBalance)) }
            .sortedByDescending { it.second }
            .toMutableList()

        val transfers = mutableListOf<DebtTransfer>()

        // Step 3: Match debtors and creditors
        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]
            val debtor = debtors[dIdx]

            val credId = creditor.first
            val credVal = creditor.second

            val debtId = debtor.first
            val debtVal = debtor.second

            val amount = min(credVal, debtVal)

            val fromMem = memberMap[debtId]
            val toMem = memberMap[credId]

            if (amount > 0 && fromMem != null && toMem != null) {
                transfers.add(
                    DebtTransfer(
                        fromMemberId = debtId,
                        fromMemberName = fromMem.name,
                        fromMemberEmoji = fromMem.avatarEmoji,
                        toMemberId = credId,
                        toMemberName = toMem.name,
                        toMemberEmoji = toMem.avatarEmoji,
                        amount = amount,
                        currency = currency
                    )
                )
            }

            // Update balances
            val newCredVal = credVal - amount
            val newDebtVal = debtVal - amount

            if (newCredVal == 0L) {
                cIdx++
            } else {
                creditors[cIdx] = Pair(credId, newCredVal)
            }

            if (newDebtVal == 0L) {
                dIdx++
            } else {
                debtors[dIdx] = Pair(debtId, newDebtVal)
            }
        }

        return transfers
    }

    /**
     * Calculates both original and simplified debts, returning a complete comparison.
     */
    fun calculateSimplification(
        members: List<MemberEntity>,
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>,
        currency: String
    ): SimplificationResult {
        val memberMap = members.associateBy { it.id }
        val balances = calculateBalances(members, payers, splits, settlements, currency)

        // 1. Calculate simplified debts using group-level net balances
        val simplified = simplify(balances, members, currency)

        // 2. Calculate original debts.
        // We do this by calculating original debts for each individual expense, accumulating them,
        // and then adjusting by subtracting any confirmed settlements between pairs of members.
        
        // Group payers and splits by expenseId
        val payersByExpense = payers.groupBy { it.expenseId }
        val splitsByExpense = splits.groupBy { it.expenseId }
        val allExpenseIds = (payersByExpense.keys + splitsByExpense.keys).distinct()

        val rawDebtsBetweenMembers = mutableMapOf<Pair<Long, Long>, Long>() // (from, to) -> amount

        for (expId in allExpenseIds) {
            val expPayers = payersByExpense[expId] ?: emptyList()
            val expSplits = splitsByExpense[expId] ?: emptyList()

            // Calculate "net balance" for just this expense
            val expBalances = members.map { member ->
                val paid = expPayers.filter { it.memberId == member.id }.sumOf { it.amountPaid }
                val owed = expSplits.filter { it.memberId == member.id }.sumOf { it.amountOwed }
                MemberBalance(
                    memberId = member.id,
                    name = member.name,
                    avatarEmoji = member.avatarEmoji,
                    totalPaid = paid,
                    totalOwed = owed,
                    settledSent = 0,
                    settledReceived = 0,
                    netBalance = paid - owed
                )
            }

            // Simplify this single expense to see who owes whom directly for it
            val expTransfers = simplify(expBalances, members, currency)
            for (t in expTransfers) {
                val pair = Pair(t.fromMemberId, t.toMemberId)
                rawDebtsBetweenMembers[pair] = (rawDebtsBetweenMembers[pair] ?: 0L) + t.amount
            }
        }

        // Also we have settlements. A confirmed settlement from A to B reduces A's debt to B.
        // If B owed A, it could also offset or accumulate. Let's net them out:
        val netDebts = mutableMapOf<Pair<Long, Long>, Long>()
        for ((pair, amount) in rawDebtsBetweenMembers) {
            val oppositePair = Pair(pair.second, pair.first)
            val oppAmount = rawDebtsBetweenMembers[oppositePair] ?: 0L
            if (amount > oppAmount) {
                netDebts[pair] = amount - oppAmount
            } else if (amount < oppAmount) {
                netDebts[oppositePair] = oppAmount - amount
            }
        }

        // Apply settlements: settlement from S to R reduces S's debt to R.
        // If S doesn't owe R, but R owes S, it increases R's debt to S.
        // Let's create a ledger for each pair:
        val settlementPayments = mutableMapOf<Pair<Long, Long>, Long>()
        for (settlement in settlements) {
            if (settlement.status == "CONFIRMED") {
                val pair = Pair(settlement.payerMemberId, settlement.receiverMemberId)
                settlementPayments[pair] = (settlementPayments[pair] ?: 0L) + settlement.amount
            }
        }

        // Net out direct original debts with direct payments
        val original = mutableListOf<DebtTransfer>()
        val processedPairs = mutableSetOf<Pair<Long, Long>>()

        // Combine netDebts and settlementPayments keys
        val allPairs = (netDebts.keys + settlementPayments.keys).distinct()

        for (pair in allPairs) {
            val revPair = Pair(pair.second, pair.first)
            if (processedPairs.contains(pair) || processedPairs.contains(revPair)) continue
            processedPairs.add(pair)

            // A owes B raw
            val debtAB = netDebts[pair] ?: 0L
            // B owes A raw
            val debtBA = netDebts[revPair] ?: 0L

            // Paid from A to B
            val paidAB = settlementPayments[pair] ?: 0L
            // Paid from B to A
            val paidBA = settlementPayments[revPair] ?: 0L

            // Net debt from A to B
            // Net = (debtAB - paidAB) - (debtBA - paidBA)
            val netAB = (debtAB - paidAB) - (debtBA - paidBA)

            val fromMem = memberMap[pair.first]
            val toMem = memberMap[pair.second]

            if (fromMem != null && toMem != null) {
                if (netAB > 0) {
                    original.add(
                        DebtTransfer(
                            fromMemberId = pair.first,
                            fromMemberName = fromMem.name,
                            fromMemberEmoji = fromMem.avatarEmoji,
                            toMemberId = pair.second,
                            toMemberName = toMem.name,
                            toMemberEmoji = toMem.avatarEmoji,
                            amount = netAB,
                            currency = currency
                        )
                    )
                } else if (netAB < 0) {
                    original.add(
                        DebtTransfer(
                            fromMemberId = pair.second,
                            fromMemberName = toMem.name,
                            fromMemberEmoji = toMem.avatarEmoji,
                            toMemberId = pair.first,
                            toMemberName = fromMem.name,
                            toMemberEmoji = fromMem.avatarEmoji,
                            amount = abs(netAB),
                            currency = currency
                        )
                    )
                }
            }
        }

        val beforeCount = original.size
        val afterCount = simplified.size
        val avoidedCount = beforeCount - afterCount

        return SimplificationResult(
            originalDebts = original.sortedByDescending { it.amount },
            simplifiedDebts = simplified.sortedByDescending { it.amount },
            beforeCount = beforeCount,
            afterCount = afterCount,
            avoidedCount = if (avoidedCount < 0) 0 else avoidedCount
        )
    }
}
