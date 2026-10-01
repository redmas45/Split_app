package com.example.splitapp.domain

sealed class TransactionDraft {
    data class Expense(val description: String, val payerId: Int, val amountPaise: Long, val splitBetween: List<Int>? = null) : TransactionDraft()
    data class Transfer(val fromId: Int, val toId: Int, val amountPaise: Long) : TransactionDraft()
}

sealed class JoinChoice {
    data class ClaimMember(val memberId: Int) : JoinChoice()
    data class NewMember(val name: String) : JoinChoice()
}

sealed class JoinPlan {
    data class AlreadyMember(val memberId: Int) : JoinPlan()
    data class Claim(val memberId: Int) : JoinPlan()
    data class Add(val memberId: Int, val name: String) : JoinPlan()
    data class Rejected(val message: String) : JoinPlan()
}

data class GroupDocUpdate(val members: List<Map<String, Any>>, val transactions: List<Map<String, Any>>)

// Pure decisions for group edits. They work on the RAW Firestore maps (not the parsed model) so rows the app
// cannot read, and fields it does not know about, are written back exactly as stored.
// FirebaseService runs each of these inside db.runTransaction, so they always see the latest document.

private fun rawId(m: Map<String, Any>): Int? = (m["id"] as? Number)?.toInt()

fun nextId(ids: Collection<Int>): Int = (ids.maxOrNull() ?: 0) + 1

fun appendTransaction(raw: List<Map<String, Any>>, draft: TransactionDraft, nowMillis: Long, uid: String): List<Map<String, Any>> {
    val id = nextId(raw.mapNotNull(::rawId))
    val tx = when (draft) {
        is TransactionDraft.Expense ->
            Transaction.Expense(id, draft.description, draft.payerId, draft.amountPaise, draft.splitBetween, nowMillis, uid)
        is TransactionDraft.Transfer ->
            Transaction.Transfer(id, draft.fromId, draft.toId, draft.amountPaise, nowMillis, uid)
    }
    return raw + tx.toMap()
}

// Removes every row with this id (old documents may hold duplicate ids; they cannot be told apart).
fun removeTransaction(raw: List<Map<String, Any>>, txId: Int): List<Map<String, Any>> = raw.filter { rawId(it) != txId }

/** Pins legacy expenses (no splitBetween) to the members who exist now, so a later newcomer never inherits them. */
fun freezeLegacySplits(rawTxs: List<Map<String, Any>>, memberIds: List<Int>): List<Map<String, Any>> =
    rawTxs.map { tx ->
        val legacy = tx["type"] == "expense" && (tx["splitBetween"] as? List<*>).isNullOrEmpty()
        if (legacy) tx + ("splitBetween" to memberIds) else tx
    }

fun canRemoveMember(memberId: Int, txs: List<Transaction>): Boolean = txs.none {
    when (it) {
        is Transaction.Expense -> it.payerId == memberId || (it.splitBetween?.contains(memberId) == true)
        is Transaction.Transfer -> it.fromId == memberId || it.toId == memberId
    }
}

/** Why this member can't be removed right now (a message safe to show), or null when removal is allowed. */
fun removalBlockReason(member: Member, txs: List<Transaction>): String? = when {
    member.uid != null -> "${member.name} is linked to an account and can't be removed."
    !canRemoveMember(member.id, txs) -> "${member.name} has transactions in this group and can't be removed."
    else -> null
}

fun planJoin(members: List<Member>, myUid: String, choice: JoinChoice): JoinPlan {
    members.firstOrNull { it.uid == myUid }?.let { return JoinPlan.AlreadyMember(it.id) }
    return when (choice) {
        is JoinChoice.ClaimMember -> {
            val target = members.firstOrNull { it.id == choice.memberId }
            when {
                target == null -> JoinPlan.Rejected("That member no longer exists in this group.")
                target.uid != null -> JoinPlan.Rejected("${target.name} is already linked to another account.")
                else -> JoinPlan.Claim(target.id)
            }
        }
        is JoinChoice.NewMember ->
            if (choice.name.isBlank()) JoinPlan.Rejected("Enter a name to join with.")
            else JoinPlan.Add(nextId(members.map { it.id }), choice.name.trim())
    }
}

fun applyJoinPlan(rawMembers: List<Map<String, Any>>, rawTxs: List<Map<String, Any>>, plan: JoinPlan, myUid: String): GroupDocUpdate =
    when (plan) {
        is JoinPlan.AlreadyMember, is JoinPlan.Rejected -> GroupDocUpdate(rawMembers, rawTxs)
        is JoinPlan.Claim -> GroupDocUpdate(
            rawMembers.map { if (rawId(it) == plan.memberId) it + ("uid" to myUid) else it },
            rawTxs,
        )
        is JoinPlan.Add -> withNewMember(rawMembers, rawTxs, plan.memberId, plan.name, myUid)
    }

/** A member added by hand (no account linked yet). */
fun addMemberUpdate(rawMembers: List<Map<String, Any>>, rawTxs: List<Map<String, Any>>, name: String): GroupDocUpdate =
    withNewMember(rawMembers, rawTxs, nextId(rawMembers.mapNotNull(::rawId)), name.trim(), uid = null)

private fun withNewMember(
    rawMembers: List<Map<String, Any>>, rawTxs: List<Map<String, Any>>, id: Int, name: String, uid: String?,
): GroupDocUpdate = GroupDocUpdate(
    members = rawMembers + Member(id, name, uid).toMap(),
    transactions = freezeLegacySplits(rawTxs, rawMembers.mapNotNull(::rawId)),
)
