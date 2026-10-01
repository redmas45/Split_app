package com.example.splitapp.domain

/**
 * The transactions worth a notification: rows that were not there the last time we looked ([prevIds]) and were added by
 * someone else. Comparing ids (not row counts) means "one deleted, one added" is still noticed; deletions and our own
 * additions never notify; the very first load ([prevIds] == null) is not news. A row without a creator comes from an
 * older app version and counts as someone else's.
 */
fun newForeignTransactions(prevIds: Set<Int>?, current: List<Transaction>, myUid: String?): List<Transaction> {
    if (prevIds == null) return emptyList()
    return current.filter { it.id !in prevIds && (myUid == null || it.createdBy != myUid) }
}
