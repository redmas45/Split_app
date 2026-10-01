package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewTransactionsTest {
    private fun tx(id: Int, by: String? = "other") = Transaction.Expense(id, "x", 1, 100, createdBy = by)

    @Test fun firstLoad_neverNotifies_theExistingRowsAreNotNews() =
        assertTrue(newForeignTransactions(prevIds = null, current = listOf(tx(1), tx(2)), myUid = "me").isEmpty())

    @Test fun aNewTransactionFromSomeoneElse_isReported() =
        assertEquals(listOf(tx(3)), newForeignTransactions(setOf(1, 2), listOf(tx(1), tx(2), tx(3)), "me"))

    @Test fun myOwnNewTransaction_isNotReported() =
        assertTrue(newForeignTransactions(setOf(1), listOf(tx(1), tx(2, by = "me")), "me").isEmpty())

    @Test fun aDeletion_neverNotifies() =
        assertTrue(newForeignTransactions(setOf(1, 2, 3), listOf(tx(1), tx(3)), "me").isEmpty())

    @Test fun oneDeletedAndOneAdded_stillReportsTheAddition_whichCountingRowsMissed() =
        assertEquals(listOf(tx(4)), newForeignTransactions(setOf(1, 2, 3), listOf(tx(1), tx(2), tx(4)), "me"))

    @Test fun severalNew_keepTheirOrder_andOnlyForeignOnesCount() =
        assertEquals(
            listOf(tx(5), tx(7)),
            newForeignTransactions(setOf(1), listOf(tx(1), tx(5), tx(6, by = "me"), tx(7)), "me"),
        )

    @Test fun aRowWithNoCreator_comesFromAnOldAppVersion_andCountsAsSomeoneElses() =
        assertEquals(listOf(tx(2, by = null)), newForeignTransactions(setOf(1), listOf(tx(1), tx(2, by = null)), "me"))

    @Test fun whenNobodyIsSignedIn_everyNewRowIsForeign() =
        assertEquals(listOf(tx(2)), newForeignTransactions(setOf(1), listOf(tx(1), tx(2)), myUid = null))

    @Test fun nothingChanged_nothingReported() =
        assertTrue(newForeignTransactions(setOf(1, 2), listOf(tx(1), tx(2)), "me").isEmpty())
}
