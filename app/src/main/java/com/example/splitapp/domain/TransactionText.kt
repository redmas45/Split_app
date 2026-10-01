package com.example.splitapp.domain

const val FORMER_MEMBER = "Former member"

fun memberName(members: List<Member>, id: Int): String = members.firstOrNull { it.id == id }?.name ?: FORMER_MEMBER

/** One line naming the transaction, e.g. "Dinner — ₹1,500.00, paid by Raj". Shown in the delete confirmation. */
fun deleteSummary(tx: Transaction, members: List<Member>): String = when (tx) {
    is Transaction.Expense ->
        "${tx.description.trim().ifEmpty { "Expense" }} — ${formatRupees(tx.amountPaise)}, paid by ${memberName(members, tx.payerId)}"
    is Transaction.Transfer ->
        "Transfer — ${memberName(members, tx.fromId)} ➡️ ${memberName(members, tx.toId)}, ${formatRupees(tx.amountPaise)}"
}

fun deleteMessage(tx: Transaction, members: List<Member>): String =
    deleteSummary(tx, members) + "\n\nThis removes it for everyone in the group and updates all balances. This can't be undone."

/** The member who added it, matched by account uid. Null for legacy rows (nothing is invented). */
fun addedByName(tx: Transaction, members: List<Member>): String? =
    tx.createdBy?.let { uid -> members.firstOrNull { it.uid == uid }?.name }

/** "Split between 2 of 3" when an expense was not split among everyone; null otherwise (including legacy rows). */
fun splitLabel(tx: Transaction, members: List<Member>): String? {
    val split = (tx as? Transaction.Expense)?.splitBetween?.toSet() ?: return null
    if (split == members.map { it.id }.toSet()) return null
    return "Split between ${split.size} of ${members.size}"
}
