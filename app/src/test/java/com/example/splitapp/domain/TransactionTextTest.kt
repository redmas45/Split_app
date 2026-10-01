package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionTextTest {
    private val raj = Member(1, "Raj", "uid-raj")
    private val sam = Member(2, "Sam")
    private val members = listOf(raj, sam, Member(3, "Nia"))

    private val dinner = Transaction.Expense(1, "Dinner", payerId = 1, amountPaise = 150000)
    private val transfer = Transaction.Transfer(2, fromId = 2, toId = 1, amountPaise = 50000)

    // ---- names ----

    @Test fun memberName_knownAndFormer() {
        assertEquals("Sam", memberName(members, 2))
        assertEquals("Former member", memberName(members, 99))
    }

    // ---- the delete-confirmation wording ----

    @Test fun deleteSummary_expense() =
        assertEquals("Dinner — ₹1,500.00, paid by Raj", deleteSummary(dinner, members))

    @Test fun deleteSummary_transfer() =
        assertEquals("Transfer — Sam ➡️ Raj, ₹500.00", deleteSummary(transfer, members))

    @Test fun deleteSummary_namesFormerMembersAndBlankDescriptions() {
        assertEquals("Expense — ₹0.50, paid by Former member", deleteSummary(Transaction.Expense(3, "  ", 9, 50), members))
        assertEquals("Transfer — Former member ➡️ Raj, ₹500.00", deleteSummary(transfer.copy(fromId = 9), members))
    }

    @Test fun deleteMessage_addsTheConsequenceSentence() =
        assertEquals(
            "Dinner — ₹1,500.00, paid by Raj\n\nThis removes it for everyone in the group and updates all balances. This can't be undone.",
            deleteMessage(dinner, members),
        )

    // ---- ledger row labels ----

    @Test fun addedBy_matchesTheCreatorsUidToAMember() {
        assertEquals("Raj", addedByName(dinner.copy(createdBy = "uid-raj"), members))
        assertNull(addedByName(dinner.copy(createdBy = "someone-else"), members))
        assertNull(addedByName(dinner, members))   // legacy row: nothing invented
    }

    @Test fun splitLabel_onlyWhenNotEveryone() {
        assertEquals("Split between 2 of 3", splitLabel(dinner.copy(splitBetween = listOf(1, 2)), members))
        assertNull(splitLabel(dinner.copy(splitBetween = listOf(3, 1, 2)), members))   // everyone
        assertNull(splitLabel(dinner, members))                                         // legacy: split among all
        assertNull(splitLabel(transfer, members))
    }
}
