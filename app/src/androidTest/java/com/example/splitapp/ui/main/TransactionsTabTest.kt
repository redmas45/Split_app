package com.example.splitapp.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.splitapp.domain.Member
import com.example.splitapp.domain.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Every transaction delete must go through a confirmation dialog (UX-GROUP-1). */
class TransactionsTabTest {

    @get:Rule val rule = createComposeRule()

    private val members = listOf(Member(1, "Raj"), Member(2, "Sam"))
    private val transactions = listOf(
        Transaction.Expense(1, "Dinner", payerId = 1, amountPaise = 150000),
        Transaction.Transfer(2, fromId = 2, toId = 1, amountPaise = 50000),
    )
    private val deleted = mutableListOf<Int>()

    @Before fun setUp() {
        rule.setContent {
            MaterialTheme {
                TransactionsTab(members, transactions, onAdd = { true }, onDelete = { deleted += it })
            }
        }
    }

    @Test fun tappingDelete_opensAConfirmationThatNamesTheRow_andDeletesNothing() {
        rule.onNodeWithContentDescription("Delete transaction: Dinner").performClick()

        rule.onNodeWithText("Delete this transaction?").assertIsDisplayed()
        rule.onNodeWithText("Dinner — ₹1,500.00, paid by Raj", substring = true).assertIsDisplayed()
        assertTrue("nothing may be deleted before confirming", deleted.isEmpty())
    }

    @Test fun cancel_leavesBothRowsAndDeletesNothing() {
        rule.onNodeWithContentDescription("Delete transaction: Dinner").performClick()
        rule.onNodeWithText("Cancel").performClick()

        rule.onNodeWithText("Delete this transaction?").assertDoesNotExist()
        rule.onNodeWithText("Dinner").assertIsDisplayed()
        rule.onNodeWithText("Transfer").assertIsDisplayed()
        assertTrue(deleted.isEmpty())
    }

    @Test fun confirming_callsOnDeleteExactlyOnce_withTheRowsId() {
        rule.onNodeWithContentDescription("Delete transaction: Dinner").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()

        assertEquals(listOf(1), deleted)
    }

    @Test fun deletingTheTransferRow_confirmsWithTheTransferWording() {
        rule.onNodeWithContentDescription("Delete transaction: Transfer").performClick()

        rule.onNodeWithText("Transfer — Sam ➡️ Raj, ₹500.00", substring = true).assertIsDisplayed()
    }
}
