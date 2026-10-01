package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerTest {
    private val a = Member(1, "A")
    private val b = Member(2, "B")
    private val c = Member(3, "C")
    private val abc = listOf(a, b, c)

    private fun expense(id: Int, payer: Int, paise: Long, split: List<Int>? = null) =
        Transaction.Expense(id, "x", payer, paise, split)

    private fun transfer(id: Int, from: Int, to: Int, paise: Long) = Transaction.Transfer(id, from, to, paise)

    private fun assertSumsToZero(balances: Map<Int, Long>) = assertEquals(0L, balances.values.sum())

    // ---- computeBalances ----

    @Test fun balances_expenseSplitEvenlyAmongAll() {
        val r = computeBalances(abc, listOf(expense(1, payer = 1, paise = 30000, split = listOf(1, 2, 3))))
        assertEquals(mapOf(1 to 20000L, 2 to -10000L, 3 to -10000L), r)
    }

    @Test fun balances_remainderPaiseGoesToLowestIdsAndTotalStaysExact() {
        val r = computeBalances(abc, listOf(expense(1, payer = 1, paise = 10000, split = listOf(1, 2, 3))))
        // shares 3334 / 3333 / 3333; A also paid 10000
        assertEquals(mapOf(1 to 6666L, 2 to -3333L, 3 to -3333L), r)
        assertSumsToZero(r)
    }

    @Test fun balances_remainderOrderIgnoresWhoPaid() {
        val r = computeBalances(abc, listOf(expense(1, payer = 3, paise = 10000, split = listOf(3, 1, 2))))
        assertEquals(mapOf(1 to -3334L, 2 to -3333L, 3 to 6667L), r)
    }

    @Test fun balances_partialSplitLeavesOthersUntouched() {
        val r = computeBalances(abc, listOf(expense(1, payer = 1, paise = 30000, split = listOf(1, 2))))
        assertEquals(mapOf(1 to 15000L, 2 to -15000L, 3 to 0L), r)
    }

    @Test fun balances_transferSettlesDebt() {
        val r = computeBalances(
            abc,
            listOf(expense(1, 1, 30000, listOf(1, 2, 3)), transfer(2, from = 2, to = 1, paise = 10000)),
        )
        assertEquals(mapOf(1 to 10000L, 2 to 0L, 3 to -10000L), r)
    }

    @Test fun balances_legacyExpenseSplitsAmongAllCurrentMembers() {
        val r = computeBalances(abc, listOf(expense(1, payer = 2, paise = 30000, split = null)))
        assertEquals(mapOf(1 to -10000L, 2 to 20000L, 3 to -10000L), r)
    }

    @Test fun balances_emptySplitListIsTreatedAsLegacy() {
        val r = computeBalances(abc, listOf(expense(1, payer = 2, paise = 30000, split = emptyList())))
        assertEquals(mapOf(1 to -10000L, 2 to 20000L, 3 to -10000L), r)
    }

    @Test fun balances_removedMembersAreKeptSoTotalsBalance() {
        val r = computeBalances(listOf(a, b), listOf(expense(1, payer = 9, paise = 30000, split = listOf(1, 2, 9))))
        assertEquals(mapOf(1 to -10000L, 2 to -10000L, 9 to 20000L), r)
        assertSumsToZero(r)
    }

    @Test fun balances_expenseWithNobodyToSplitWithIsSkipped() {
        val r = computeBalances(emptyList(), listOf(expense(1, payer = 1, paise = 5000)))
        assertTrue(r.isEmpty())
    }

    @Test fun balances_newMemberStartsAtZero() {
        val r = computeBalances(abc + Member(4, "D"), listOf(expense(1, 1, 30000, listOf(1, 2, 3))))
        assertEquals(0L, r[4])
    }

    @Test fun balances_alwaysSumToZeroForMixedFixture() {
        val txs = listOf(
            expense(1, 1, 10001, listOf(1, 2, 3)),
            expense(2, 2, 99999, null),
            expense(3, 3, 7, listOf(2, 3)),
            transfer(4, 1, 3, 1234),
            transfer(5, 2, 9, 55),
            expense(6, 9, 1, listOf(1, 2, 3, 9)),
        )
        assertSumsToZero(computeBalances(abc, txs))
    }

    @Test fun total_countsExpensesOnly() {
        val txs = listOf(expense(1, 1, 30000), expense(2, 2, 500), transfer(3, 1, 2, 99999))
        assertEquals(30500L, totalExpensePaise(txs))
    }

    // ---- settle ----

    private fun applyPayments(balances: Map<Int, Long>, payments: List<Payment>): Map<Int, Long> {
        val m = balances.toMutableMap()
        payments.forEach {
            m[it.fromId] = m.getValue(it.fromId) + it.paise
            m[it.toId] = m.getValue(it.toId) - it.paise
        }
        return m
    }

    @Test fun settle_clearsEveryBalance() {
        val balances = mapOf(1 to 7000L, 2 to -2000L, 3 to -5000L, 4 to 3000L, 5 to -3000L)
        val payments = settle(balances)
        assertTrue(applyPayments(balances, payments).values.all { it == 0L })
    }

    @Test fun settle_usesAtMostNonZeroMinusOnePayments() {
        val balances = mapOf(1 to 7000L, 2 to -2000L, 3 to -5000L, 4 to 3000L, 5 to -3000L, 6 to 0L)
        assertTrue(settle(balances).size <= 4)
    }

    @Test fun settle_isEmptyWhenAlreadySettled() {
        assertTrue(settle(mapOf(1 to 0L, 2 to 0L)).isEmpty())
        assertTrue(settle(emptyMap()).isEmpty())
    }

    @Test fun settle_matchesLargestWithLargestAndBreaksTiesById() {
        val payments = settle(mapOf(1 to 5000L, 2 to 5000L, 3 to -5000L, 4 to -5000L))
        assertEquals(listOf(Payment(3, 1, 5000), Payment(4, 2, 5000)), payments)
    }

    @Test fun settle_endToEndFromTransactions() {
        val balances = computeBalances(abc, listOf(expense(1, 1, 30000, listOf(1, 2, 3))))
        assertEquals(listOf(Payment(2, 1, 10000), Payment(3, 1, 10000)), settle(balances))
    }
}
