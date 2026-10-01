package com.example.splitapp.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinFlowTest {

    // ---- joinStepFor ----

    private val sam = Member(2, "Sam")
    private val members = listOf(Member(1, "Raj", "uid-raj"), sam, Member(3, "Nia"), Member(4, "Dev", "uid-dev"))

    @Test fun alreadyAMember_isRecognisedByUid_andSkipsTheChoice() =
        assertEquals(JoinStep.AlreadyMember(1), joinStepFor(members, "uid-raj"))

    @Test fun newcomer_isOfferedOnlyTheMembersNobodyHasClaimed_inGroupOrder() =
        assertEquals(JoinStep.ChooseWho(listOf(sam, Member(3, "Nia"))), joinStepFor(members, "uid-new"))

    @Test fun whenEveryMemberIsLinked_theChoiceListIsEmpty_soOnlyNewMemberRemains() =
        assertEquals(JoinStep.ChooseWho(emptyList()), joinStepFor(listOf(Member(1, "Raj", "a"), Member(2, "Sam", "b")), "uid-new"))

    @Test fun anEmptyGroup_hasNobodyToClaim() =
        assertEquals(JoinStep.ChooseWho(emptyList()), joinStepFor(emptyList(), "uid-new"))

    // ---- normalizeGroupCode ----

    @Test fun code_isTrimmed() = assertEquals("5xJ1abc", normalizeGroupCode("  5xJ1abc  "))

    @Test fun code_losesPastedWhitespaceAndNewlines() = assertEquals("5xJ1abc", normalizeGroupCode("5xJ1\n abc\t"))

    @Test fun code_blankStaysEmpty() = assertEquals("", normalizeGroupCode(" \n "))

    // ---- createOrQueue ----

    @Test fun finishing_isCreated() = runTest {
        assertEquals(CreateOutcome.Created, createOrQueue(15_000) { Result.success(Unit) })
    }

    @Test fun aRealFailure_isFailedWithItsMessage() = runTest {
        val out = createOrQueue(15_000) { Result.failure(IllegalStateException("permission denied")) }
        assertEquals(CreateOutcome.Failed("permission denied"), out)
    }

    @Test fun aFailureWithoutAMessage_getsAPlainFallback() = runTest {
        val out = createOrQueue(15_000) { Result.failure(RuntimeException()) } as CreateOutcome.Failed
        assertTrue(out.message.isNotBlank())
    }

    @Test fun aThrownException_isFailedToo() = runTest {
        val out = createOrQueue(15_000) { throw IllegalStateException("boom") }
        assertEquals(CreateOutcome.Failed("boom"), out)
    }

    @Test fun neverFinishing_isPendingOfflineOnlyAfterTheTimeout() = runTest {
        val pending = async { createOrQueue(15_000) { awaitCancellation() } }
        advanceTimeBy(14_999)
        assertFalse("must keep waiting until the timeout", pending.isCompleted)
        advanceTimeBy(2)
        assertEquals(CreateOutcome.PendingOffline, pending.await())
    }

    @Test fun finishingJustBeforeTheTimeout_isStillCreated() = runTest {
        val out = async { createOrQueue(15_000) { delay(14_000); Result.success(Unit) } }
        advanceTimeBy(14_001)
        assertEquals(CreateOutcome.Created, out.await())
    }
}
