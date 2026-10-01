package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFormTest {
    private val everyone = setOf(1, 2, 3)

    private fun expense(
        amount: String = "250", description: String = "Dinner", payer: Int? = 1,
        split: Set<Int> = everyone, members: Set<Int> = everyone,
    ) = validateDraft(TxKind.Expense, amount, description, payer, null, split, members)

    private fun transfer(amount: String = "100", from: Int? = 1, to: Int? = 2, members: Set<Int> = everyone) =
        validateDraft(TxKind.Transfer, amount, "", from, to, emptySet(), members)

    private fun errors(r: FormResult) = (r as FormResult.Invalid).errors

    // ---- valid ----

    @Test fun validExpense_buildsTheDraftWithTheSplitSortedById() {
        val r = expense(amount = "1,500", description = "  Dinner ", split = setOf(3, 1))
        assertEquals(FormResult.Valid(TransactionDraft.Expense("Dinner", 1, 150000, listOf(1, 3))), r)
    }

    @Test fun validTransfer_buildsTheDraft() =
        assertEquals(FormResult.Valid(TransactionDraft.Transfer(1, 2, 10000)), transfer())

    // ---- amount ----

    @Test fun badAmounts_showTheAmountError() {
        listOf("", " ", "abc", "0", "-5", "1.234", "1e5", "Infinity", "10000000.01").forEach {
            assertEquals("'$it'", AMOUNT_ERROR, errors(expense(amount = it)).amount)
            assertEquals("'$it'", AMOUNT_ERROR, errors(transfer(amount = it)).amount)
        }
    }

    // ---- expense fields ----

    @Test fun blankDescription_showsTheDescriptionError() {
        assertEquals(DESCRIPTION_ERROR, errors(expense(description = "")).description)
        assertEquals(DESCRIPTION_ERROR, errors(expense(description = "   ")).description)
    }

    @Test fun missingOrUnknownPayer_showsThePeopleError() {
        assertEquals(PAYER_ERROR, errors(expense(payer = null)).people)
        assertEquals(PAYER_ERROR, errors(expense(payer = 9)).people)
    }

    @Test fun emptySplit_showsTheSplitError() {
        assertEquals(SPLIT_ERROR, errors(expense(split = emptySet())).split)
    }

    @Test fun splitOfOnlyFormerMembers_showsTheSplitError() {
        assertEquals(SPLIT_ERROR, errors(expense(split = setOf(9))).split)
    }

    @Test fun splitIgnoresFormerMembers() {
        val r = expense(split = setOf(1, 9)) as FormResult.Valid
        assertEquals(listOf(1), (r.draft as TransactionDraft.Expense).splitBetween)
    }

    // ---- transfer fields ----

    @Test fun transferToTheSamePerson_showsThePeopleError() =
        assertEquals(PEOPLE_ERROR, errors(transfer(from = 2, to = 2)).people)

    @Test fun transferWithAMissingOrUnknownPerson_showsThePeopleError() {
        assertEquals(PEOPLE_ERROR, errors(transfer(from = null)).people)
        assertEquals(PEOPLE_ERROR, errors(transfer(to = null)).people)
        assertEquals(PEOPLE_ERROR, errors(transfer(to = 9)).people)
    }

    @Test fun transferIgnoresDescriptionAndSplit() {
        val r = validateDraft(TxKind.Transfer, "100", "", 1, 2, emptySet(), everyone)
        assertTrue(r is FormResult.Valid)
    }

    // ---- several errors at once ----

    @Test fun everyBadFieldIsReportedTogether() {
        val e = errors(expense(amount = "x", description = "", payer = null, split = emptySet()))
        assertEquals(AMOUNT_ERROR, e.amount)
        assertEquals(DESCRIPTION_ERROR, e.description)
        assertEquals(PAYER_ERROR, e.people)
        assertEquals(SPLIT_ERROR, e.split)
    }

    @Test fun aValidFormHasNoErrorsAndAnInvalidOneReportsAny() {
        assertNull(errors(expense(amount = "x")).description)
        assertTrue(errors(expense(amount = "x")).any)
    }
}
