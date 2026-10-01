package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupOpsTest {
    private val a = Member(1, "A")
    private val b = Member(2, "B")
    private val c = Member(3, "C")

    private fun members(vararg m: Member) = m.map { it.toMap() }
    private fun ids(raw: List<Map<String, Any>>) = raw.map { (it["id"] as Number).toInt() }
    private fun parsedTxs(raw: List<Map<String, Any>>) = raw.mapNotNull { transactionFromMap(it) }
    private fun parsedMembers(raw: List<Map<String, Any>>) = raw.mapNotNull { memberFromMap(it) }

    // A legacy expense: no splitBetween / createdAt / createdBy.
    private val legacyDinner = Transaction.Expense(1, "Dinner", 1, 30000).toMap()
    // A row the app cannot read (garbage amount). Must survive every write untouched.
    private val unreadable: Map<String, Any> =
        mapOf("id" to 5L, "type" to "expense", "payerId" to 1L, "amount" to "Infinity", "description" to "?")

    private val expenseDraft = TransactionDraft.Expense("Taxi", payerId = 2, amountPaise = 9950, splitBetween = listOf(1, 2))
    private val transferDraft = TransactionDraft.Transfer(fromId = 2, toId = 1, amountPaise = 5000)

    // ---- nextId ----

    @Test fun nextId_isOneForEmpty() = assertEquals(1, nextId(emptyList()))
    @Test fun nextId_isMaxPlusOneEvenWithGaps() = assertEquals(8, nextId(listOf(1, 7, 3)))

    // ---- appendTransaction ----

    @Test fun append_assignsNextIdAndStampsCreatorAndTime() {
        val out = appendTransaction(listOf(legacyDinner), expenseDraft, nowMillis = 1_700_000_000_000L, uid = "u1")
        assertEquals(listOf(1, 2), ids(out))
        assertEquals(
            Transaction.Expense(2, "Taxi", 2, 9950, listOf(1, 2), 1_700_000_000_000L, "u1"),
            transactionFromMap(out.last()),
        )
        assertEquals("99.50", out.last()["amount"])
    }

    @Test fun append_transferDraft() {
        val out = appendTransaction(emptyList(), transferDraft, 5L, "u1")
        assertEquals(Transaction.Transfer(1, 2, 1, 5000, 5L, "u1"), transactionFromMap(out.single()))
    }

    @Test fun append_keepsExistingRowsExactlyAsStored() {
        val out = appendTransaction(listOf(legacyDinner, unreadable), expenseDraft, 1L, "u1")
        assertEquals(listOf(legacyDinner, unreadable), out.take(2))
    }

    @Test fun append_idAccountsForUnreadableRows() {
        val out = appendTransaction(listOf(unreadable), expenseDraft, 1L, "u1")
        assertEquals(listOf(5, 6), ids(out))
    }

    @Test fun append_twoWritersThatEachReReadTheLatestStateGetDistinctIds() {
        // What runTransaction guarantees: the second writer re-reads after the first commits.
        val afterFirst = appendTransaction(listOf(legacyDinner), expenseDraft, 1L, "u1")
        val afterSecond = appendTransaction(afterFirst, transferDraft, 2L, "u2")
        assertEquals(listOf(1, 2, 3), ids(afterSecond))
        assertEquals(3, afterSecond.size)
    }

    // ---- removeTransaction ----

    @Test fun remove_dropsOnlyTheMatchingId() {
        val second = Transaction.Expense(2, "Taxi", 2, 9950).toMap()
        assertEquals(listOf(second), removeTransaction(listOf(legacyDinner, second), 1))
    }

    @Test fun remove_leavesUnreadableRowsAndUnknownIdsAlone() {
        val rows = listOf(legacyDinner, unreadable)
        assertEquals(rows, removeTransaction(rows, 99))
        assertEquals(listOf(unreadable), removeTransaction(rows, 1))
    }

    // ---- freezeLegacySplits ----

    @Test fun freeze_writesCurrentMembersOntoLegacyExpensesOnly() {
        val partial = Transaction.Expense(2, "Taxi", 1, 100, listOf(1, 2)).toMap()
        val transfer = Transaction.Transfer(3, 1, 2, 100).toMap()
        val out = freezeLegacySplits(listOf(legacyDinner, partial, transfer, unreadable), listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), (out[0]["splitBetween"] as List<*>).map { (it as Number).toInt() })
        assertEquals(partial, out[1])
        assertEquals(transfer, out[2])
        assertEquals(unreadable["id"], out[3]["id"])
    }

    @Test fun freeze_treatsAnEmptySplitListAsLegacy() {
        val emptySplit = legacyDinner + ("splitBetween" to emptyList<Int>())
        val out = freezeLegacySplits(listOf(emptySplit), listOf(1, 2))
        assertEquals(listOf(1, 2), (out.single()["splitBetween"] as List<*>).map { (it as Number).toInt() })
    }

    // ---- canRemoveMember ----

    private val txs = listOf(
        Transaction.Expense(1, "x", payerId = 1, amountPaise = 100, splitBetween = listOf(1, 2)),
        Transaction.Transfer(2, fromId = 3, toId = 4, amountPaise = 100),
    )

    @Test fun canRemove_falseForPayer() = assertFalse(canRemoveMember(1, txs))
    @Test fun canRemove_falseForSplitParticipant() = assertFalse(canRemoveMember(2, txs))
    @Test fun canRemove_falseForTransferSender() = assertFalse(canRemoveMember(3, txs))
    @Test fun canRemove_falseForTransferReceiver() = assertFalse(canRemoveMember(4, txs))
    @Test fun canRemove_trueWhenUnrelated() = assertTrue(canRemoveMember(9, txs))
    @Test fun canRemove_trueWithNoTransactions() = assertTrue(canRemoveMember(1, emptyList()))

    // ---- removalBlockReason ----

    @Test fun removalBlock_nullWhenMemberIsFreeAndUnlinked() =
        assertNull(removalBlockReason(Member(9, "Zed"), txs))

    @Test fun removalBlock_namesTheMemberWhoHasTransactions() =
        assertEquals("B has transactions in this group and can't be removed.", removalBlockReason(Member(2, "B"), txs))

    @Test fun removalBlock_linkedAccountComesFirst() =
        assertEquals("A is linked to an account and can't be removed.", removalBlockReason(Member(1, "A", "uid-a"), txs))

    // ---- planJoin ----

    private val joinable = listOf(Member(1, "A", "uid-a"), Member(2, "Sam"), Member(3, "C", "uid-c"))

    @Test fun plan_alreadyAMemberIsRecognisedByUidWhateverTheChoice() {
        assertEquals(JoinPlan.AlreadyMember(1), planJoin(joinable, "uid-a", JoinChoice.NewMember("Again")))
        assertEquals(JoinPlan.AlreadyMember(1), planJoin(joinable, "uid-a", JoinChoice.ClaimMember(2)))
    }

    @Test fun plan_claimsAFreeMember() =
        assertEquals(JoinPlan.Claim(2), planJoin(joinable, "uid-new", JoinChoice.ClaimMember(2)))

    @Test fun plan_rejectsClaimingSomeoneElsesMemberOrAMissingOne() {
        assertTrue(planJoin(joinable, "uid-new", JoinChoice.ClaimMember(3)) is JoinPlan.Rejected)
        assertTrue(planJoin(joinable, "uid-new", JoinChoice.ClaimMember(99)) is JoinPlan.Rejected)
    }

    @Test fun plan_addsANewMemberWithTheNextIdAndTrimmedName() =
        assertEquals(JoinPlan.Add(4, "Nia"), planJoin(joinable, "uid-new", JoinChoice.NewMember("  Nia ")))

    @Test fun plan_rejectsABlankName() =
        assertTrue(planJoin(joinable, "uid-new", JoinChoice.NewMember("   ")) is JoinPlan.Rejected)

    // ---- applyJoinPlan / addMember ----

    @Test fun join_newcomerStartsAtZeroAndOldBalancesDoNotChange() {
        val rawMembers = members(a, b, c)
        val rawTxs = listOf(legacyDinner)   // A paid 300, legacy split among A, B, C
        val before = computeBalances(parsedMembers(rawMembers), parsedTxs(rawTxs))

        val plan = planJoin(parsedMembers(rawMembers), "uid-d", JoinChoice.NewMember("D"))
        val after = applyJoinPlan(rawMembers, rawTxs, plan, "uid-d")
        val balances = computeBalances(parsedMembers(after.members), parsedTxs(after.transactions))

        assertEquals(0L, balances[4])
        assertEquals(before, balances.filterKeys { it != 4 })
        assertEquals("uid-d", parsedMembers(after.members).single { it.id == 4 }.uid)
    }

    @Test fun join_aNewExpenseAfterwardsIncludesTheNewcomerWhenListed() {
        val plan = planJoin(parsedMembers(members(a, b)), "uid-c", JoinChoice.NewMember("C"))
        val after = applyJoinPlan(members(a, b), listOf(legacyDinner), plan, "uid-c")
        val withNew = appendTransaction(after.transactions, TransactionDraft.Expense("Tea", 1, 3000, listOf(1, 2, 3)), 1L, "u")
        val balances = computeBalances(parsedMembers(after.members), parsedTxs(withNew))
        assertEquals(-1000L, balances[3])
    }

    @Test fun join_claimSetsOnlyUid() {
        val raw = members(a, Member(2, "Sam"), c)
        val after = applyJoinPlan(raw, listOf(legacyDinner), JoinPlan.Claim(2), "uid-sam")
        assertEquals(Member(2, "Sam", "uid-sam"), parsedMembers(after.members).single { it.id == 2 })
        assertEquals(raw.filter { it["id"] != 2 }, after.members.filter { it["id"] != 2 })
        assertEquals(listOf(legacyDinner), after.transactions)   // claiming does not touch transactions
    }

    @Test fun join_twiceNeverCreatesASecondMember() {
        val rawMembers = members(a, b)
        val first = applyJoinPlan(rawMembers, emptyList(), planJoin(parsedMembers(rawMembers), "uid-d", JoinChoice.NewMember("D")), "uid-d")
        val second = applyJoinPlan(
            first.members, first.transactions,
            planJoin(parsedMembers(first.members), "uid-d", JoinChoice.NewMember("D again")), "uid-d",
        )
        assertEquals(first, second)
        assertEquals(3, second.members.size)
    }

    @Test fun addMember_freezesOldExpensesAndAssignsNextId() {
        val after = addMemberUpdate(members(a, b), listOf(legacyDinner), "  C ")
        assertEquals(listOf(1, 2, 3), ids(after.members))
        assertEquals(Member(3, "C"), parsedMembers(after.members).last())
        assertNull(after.members.last()["uid"])
        assertEquals(listOf(1, 2), (after.transactions.single()["splitBetween"] as List<*>).map { (it as Number).toInt() })
    }
}
