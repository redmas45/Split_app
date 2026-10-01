package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerMappingTest {
    @Test fun legacyExpenseWithoutNewFieldsStillReads() {
        val tx = transactionFromMap(
            mapOf("id" to 1L, "type" to "expense", "description" to "Dinner", "payerId" to 2L, "amount" to "1500"),
        )
        assertEquals(Transaction.Expense(1, "Dinner", 2, 150000, splitBetween = null), tx)
    }

    @Test fun newOptionalFieldsAreRead() {
        val tx = transactionFromMap(
            mapOf(
                "id" to 4L, "type" to "expense", "description" to "Taxi", "payerId" to 1L, "amount" to "99.50",
                "splitBetween" to listOf(1L, 3L), "createdAt" to 1_700_000_000_000L, "createdBy" to "uid-1",
            ),
        )
        assertEquals(Transaction.Expense(4, "Taxi", 1, 9950, listOf(1, 3), 1_700_000_000_000L, "uid-1"), tx)
    }

    @Test fun legacyTransferReads() {
        val tx = transactionFromMap(mapOf("id" to 2L, "type" to "transfer", "fromId" to 1L, "toId" to 2L, "amount" to "500"))
        assertEquals(Transaction.Transfer(2, 1, 2, 50000), tx)
    }

    @Test fun malformedTransactionsAreSkipped() {
        assertNull(transactionFromMap(mapOf("id" to 1L, "type" to "refund", "amount" to "5")))
        assertNull(transactionFromMap(mapOf("id" to 1L, "type" to "expense", "payerId" to 1L, "amount" to "Infinity")))
        assertNull(transactionFromMap(mapOf("id" to 1L, "type" to "expense", "payerId" to 1L, "amount" to 500L)))  // number, not text
        assertNull(transactionFromMap(mapOf("id" to 1L, "type" to "expense", "amount" to "5")))                     // no payer
    }

    @Test fun transactionRoundTripsAndStoresAmountAsFixedText() {
        val txs = listOf(
            Transaction.Expense(1, "Dinner", 2, 150000),
            Transaction.Expense(2, "Taxi", 1, 9950, listOf(1, 3), 1_700_000_000_000L, "uid-1"),
            Transaction.Transfer(3, 1, 2, 50000, 1_700_000_000_001L, "uid-2"),
        )
        txs.forEach { assertEquals(it, transactionFromMap(it.toMap())) }
        assertEquals("1500.00", txs[0].toMap()["amount"])
        assertTrue(txs[0].toMap()["amount"] is String)
    }

    @Test fun legacyShapedWriteOmitsNewFields() {
        val m = Transaction.Expense(1, "Dinner", 2, 150000).toMap()
        assertEquals(setOf("id", "type", "description", "payerId", "amount"), m.keys)
    }

    @Test fun memberMapping() {
        assertEquals(Member(1, "A"), memberFromMap(mapOf("id" to 1L, "name" to "A")))
        assertEquals(Member(1, "A", "u1"), memberFromMap(mapOf("id" to 1L, "name" to "A", "uid" to "u1")))
        assertNull(memberFromMap(mapOf("id" to 1L)))
        assertEquals(mapOf("id" to 1, "name" to "A"), Member(1, "A").toMap())
        assertEquals(mapOf("id" to 1, "name" to "A", "uid" to "u1"), Member(1, "A", "u1").toMap())
    }
}
