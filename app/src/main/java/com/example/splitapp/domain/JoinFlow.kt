package com.example.splitapp.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

sealed class JoinStep {
    data class AlreadyMember(val memberId: Int) : JoinStep()
    data class ChooseWho(val freeMembers: List<Member>) : JoinStep()
}

sealed class CreateOutcome {
    data object Created : CreateOutcome()
    data object PendingOffline : CreateOutcome()
    data class Failed(val message: String) : CreateOutcome()
}

/** What the join dialog shows next: skip straight in if this account is already a member, else ask who they are. */
fun joinStepFor(members: List<Member>, myUid: String): JoinStep {
    members.firstOrNull { it.uid == myUid }?.let { return JoinStep.AlreadyMember(it.id) }
    return JoinStep.ChooseWho(members.filter { it.uid == null })
}

/** Group codes get pasted: drop surrounding and stray whitespace (spaces, newlines). */
fun normalizeGroupCode(input: String): String = input.filterNot { it.isWhitespace() }

/**
 * Firestore only finishes a write once the server confirms it, so offline [block] never returns. After [timeoutMs]
 * we report [CreateOutcome.PendingOffline]: the write is already queued in the local cache and syncs later.
 */
suspend fun createOrQueue(timeoutMs: Long, block: suspend () -> Result<Unit>): CreateOutcome {
    val result = try {
        withTimeoutOrNull(timeoutMs) { block() } ?: return CreateOutcome.PendingOffline
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
    return result.fold(
        onSuccess = { CreateOutcome.Created },
        onFailure = { CreateOutcome.Failed(it.message ?: "Couldn't create the group. Please try again.") },
    )
}
