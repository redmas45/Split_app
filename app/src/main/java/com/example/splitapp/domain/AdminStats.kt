package com.example.splitapp.domain

data class AdminMetrics(val transactionCount: Int, val spendingPaise: Long)

@Suppress("UNCHECKED_CAST")
private fun rawTransactions(group: Map<String, Any>): List<Map<String, Any>> =
    (group["transactions"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.map { it as Map<String, Any> } ?: emptyList()

/**
 * Dashboard numbers. Every transaction is counted, but "spending" is expenses only: a transfer is money moving between
 * friends, not spending. Amounts are read exactly (paise), and an unreadable amount is counted but adds nothing.
 */
fun adminMetrics(groups: List<Map<String, Any>>): AdminMetrics {
    var count = 0
    var spending = 0L
    for (group in groups) for (tx in rawTransactions(group)) {
        count++
        if (tx["type"] == "expense") (tx["amount"] as? String)?.let(::storageToPaise)?.let { spending += it }
    }
    return AdminMetrics(count, spending)
}

/** One readable line for the admin's ledger dialog: names, not ids. Unreadable rows are shown, not hidden. */
fun adminLedgerLine(rawTx: Map<String, Any>, rawMembers: List<Map<String, Any>>): String {
    val tx = transactionFromMap(rawTx) ?: return "Unreadable entry (id ${rawTx["id"] ?: "?"})"
    return deleteSummary(tx, rawMembers.mapNotNull { memberFromMap(it) })
}
