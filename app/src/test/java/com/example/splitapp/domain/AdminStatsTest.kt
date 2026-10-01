package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminStatsTest {
    private fun expense(id: Int, amount: String, payer: Int = 1): Map<String, Any> =
        mapOf("id" to id, "type" to "expense", "description" to "Dinner", "payerId" to payer, "amount" to amount)

    private fun transfer(id: Int, amount: String): Map<String, Any> =
        mapOf("id" to id, "type" to "transfer", "fromId" to 2, "toId" to 1, "amount" to amount)

    private fun group(vararg txs: Map<String, Any>): Map<String, Any> = mapOf("id" to "g", "transactions" to txs.toList())

    // ---- adminMetrics ----

    @Test fun noGroups_isAllZero() = assertEquals(AdminMetrics(0, 0), adminMetrics(emptyList()))

    @Test fun aGroupWithoutTransactions_countsAsNothing() =
        assertEquals(AdminMetrics(0, 0), adminMetrics(listOf(mapOf("id" to "g"))))

    @Test fun volumeCountsExpensesOnly_transfersAreMoneyMovingBetweenFriends() {
        val m = adminMetrics(listOf(group(expense(1, "1500"), transfer(2, "500"))))
        assertEquals(2, m.transactionCount)          // every transaction is counted
        assertEquals(150000L, m.spendingPaise)       // but only the expense is spending
    }

    @Test fun legacyAmountFormatsAreReadExactly_noFloatingPoint() {
        val m = adminMetrics(listOf(group(expense(1, "99.5"), expense(2, "0100"), expense(3, "0.10"), expense(4, "0.20"))))
        assertEquals(9950L + 10000L + 10L + 20L, m.spendingPaise)
    }

    @Test fun sumsAcrossGroups() {
        val m = adminMetrics(listOf(group(expense(1, "100")), group(expense(1, "250.50"), transfer(2, "9"))))
        assertEquals(3, m.transactionCount)
        assertEquals(10000L + 25050L, m.spendingPaise)
    }

    @Test fun anUnreadableAmount_isStillCounted_butNotAddedToSpending() {
        val m = adminMetrics(listOf(group(expense(1, "Infinity"), expense(2, "abc"), expense(3, "50"))))
        assertEquals(3, m.transactionCount)
        assertEquals(5000L, m.spendingPaise)
    }

    // ---- adminLedgerLine ----

    private val members = listOf(mapOf("id" to 1, "name" to "Raj"), mapOf("id" to 2, "name" to "Sam"))

    @Test fun expenseLine_namesThePayer_notAnId() =
        assertEquals("Dinner — ₹1,500.00, paid by Raj", adminLedgerLine(expense(1, "1500"), members))

    @Test fun transferLine_namesBothPeople() =
        assertEquals("Transfer — Sam ➡️ Raj, ₹500.00", adminLedgerLine(transfer(2, "500"), members))

    @Test fun aRemovedMember_isFormerMember() =
        assertEquals("Dinner — ₹10.00, paid by Former member", adminLedgerLine(expense(1, "10", payer = 9), members))

    @Test fun anUnreadableRow_isShownAsSuch_notHidden() =
        assertEquals("Unreadable entry (id 7)", adminLedgerLine(expense(7, "Infinity"), members))
}
