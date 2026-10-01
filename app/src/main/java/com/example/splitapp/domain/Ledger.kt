package com.example.splitapp.domain

data class Member(val id: Int, val name: String, val uid: String? = null)

sealed class Transaction {
    abstract val id: Int
    abstract val amountPaise: Long
    abstract val createdAt: Long?
    abstract val createdBy: String?

    data class Expense(
        override val id: Int,
        val description: String,
        val payerId: Int,
        override val amountPaise: Long,
        val splitBetween: List<Int>? = null,
        override val createdAt: Long? = null,
        override val createdBy: String? = null,
    ) : Transaction()

    data class Transfer(
        override val id: Int,
        val fromId: Int,
        val toId: Int,
        override val amountPaise: Long,
        override val createdAt: Long? = null,
        override val createdBy: String? = null,
    ) : Transaction()
}

data class Payment(val fromId: Int, val toId: Int, val paise: Long)

const val MAX_AMOUNT_PAISE = 10_000_000_00L // ₹1 crore

// All money is Long paise. Nothing here uses Double or String.format (locale-dependent).

private val TYPED_AMOUNT = Regex("""\d+\.?\d{0,2}|\.\d{1,2}""")
private val STORED_AMOUNT = Regex("""\d+(\.\d+)?""")

/** User-typed amount: "1,500", "99.5", " 20 " -> paise. Null if not a positive amount of at most 2 decimals and ≤ ₹1 crore. */
fun parseAmountToPaise(input: String): Long? {
    val s = input.trim().replace(",", "")
    if (!TYPED_AMOUNT.matches(s)) return null
    return decimalToPaise(s)?.takeIf { it in 1..MAX_AMOUNT_PAISE }
}

/** Fixed "1500.00" form written to Firestore (the field stays a String for old app versions). */
fun paiseToStorage(p: Long): String {
    require(p >= 0) { "negative amount" }
    return "${p / 100}.${(p % 100).toString().padStart(2, '0')}"
}

/** Reads legacy stored text ("1500", "99.5", "0100", "12.345"); extra decimals round half up. Null if unreadable. */
fun storageToPaise(s: String): Long? {
    val t = s.trim()
    if (!STORED_AMOUNT.matches(t)) return oldAppAmountToPaise(t)
    val frac = t.substringAfter('.', "").padEnd(3, '0')
    val base = decimalToPaise(t.substringBefore('.') + "." + frac.take(2)) ?: return null
    return if (frac[2] >= '5') base + 1 else base
}

// The old app saved the raw typed text whenever toDoubleOrNull() accepted it and it was > 0, so stored amounts can be
// "100.", ".5", "+50", "1e3" or "50f". Read those exactly (BigDecimal, no Double) so old rows never vanish from the ledger.
private fun oldAppAmountToPaise(t: String): Long? {
    val value = t.trimEnd('f', 'F', 'd', 'D').toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value >= java.math.BigDecimal("1e12")) return null
    return value.setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}

fun formatRupees(p: Long): String {
    val abs = if (p < 0) -p else p
    val whole = (abs / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    return (if (p < 0) "-" else "") + "₹$whole." + (abs % 100).toString().padStart(2, '0')
}

// "12.5" / ".5" / "12." -> paise; expects digits and at most one '.', rejects anything that could overflow.
private fun decimalToPaise(s: String): Long? {
    val whole = s.substringBefore('.')
    if (whole.length > 12) return null
    val frac = s.substringAfter('.', "").padEnd(2, '0')
    return (whole.ifEmpty { "0" }).toLong() * 100 + frac.toLong()
}

/** Positive = is owed, negative = owes. Keeps ids of removed members so the sum is always 0. */
fun computeBalances(members: List<Member>, txs: List<Transaction>): Map<Int, Long> {
    val balances = LinkedHashMap<Int, Long>()
    members.forEach { balances[it.id] = 0L }
    fun add(id: Int, delta: Long) { balances[id] = (balances[id] ?: 0L) + delta }

    for (tx in txs) when (tx) {
        is Transaction.Expense -> {
            // splitBetween == null/empty is a legacy record: split among today's members.
            val parts = (tx.splitBetween?.takeIf { it.isNotEmpty() } ?: members.map { it.id }).distinct().sorted()
            if (parts.isEmpty()) continue
            val share = tx.amountPaise / parts.size
            val extra = (tx.amountPaise % parts.size).toInt()
            add(tx.payerId, tx.amountPaise)
            parts.forEachIndexed { i, id -> add(id, -(share + if (i < extra) 1 else 0)) }
        }
        is Transaction.Transfer -> {
            add(tx.fromId, tx.amountPaise)
            add(tx.toId, -tx.amountPaise)
        }
    }
    return balances
}

/** Transfers are money moving between friends, not spending. */
fun totalExpensePaise(txs: List<Transaction>): Long = txs.filterIsInstance<Transaction.Expense>().sumOf { it.amountPaise }

/** Greedy: largest debtor pays largest creditor. A small number of payments, not always the true minimum. */
fun settle(balances: Map<Int, Long>): List<Payment> {
    val order = compareByDescending<Pair<Int, Long>> { it.second }.thenBy { it.first }
    val debtors = balances.filter { it.value < 0 }.map { it.key to -it.value }.sortedWith(order).toMutableList()
    val creditors = balances.filter { it.value > 0 }.map { it.key to it.value }.sortedWith(order).toMutableList()
    val payments = mutableListOf<Payment>()
    var d = 0
    var c = 0
    while (d < debtors.size && c < creditors.size) {
        val pay = minOf(debtors[d].second, creditors[c].second)
        payments += Payment(debtors[d].first, creditors[c].first, pay)
        debtors[d] = debtors[d].first to debtors[d].second - pay
        creditors[c] = creditors[c].first to creditors[c].second - pay
        if (debtors[d].second == 0L) d++
        if (creditors[c].second == 0L) c++
    }
    return payments
}
