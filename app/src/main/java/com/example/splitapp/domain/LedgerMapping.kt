package com.example.splitapp.domain

// Firestore documents <-> domain models. Plain Maps, so no Firebase imports here.
// Old documents lack uid / splitBetween / createdAt / createdBy; those read as null and are omitted when null on write.
// "amount" is always a String in the document (old app versions read it with `as? String`).

fun memberFromMap(map: Map<String, Any?>): Member? {
    val id = (map["id"] as? Number)?.toInt() ?: return null
    val name = map["name"] as? String ?: return null
    return Member(id, name, map["uid"] as? String)
}

fun Member.toMap(): Map<String, Any> =
    buildMap { put("id", id); put("name", name); uid?.let { put("uid", it) } }

fun transactionFromMap(map: Map<String, Any?>): Transaction? {
    val id = (map["id"] as? Number)?.toInt() ?: return null
    val paise = (map["amount"] as? String)?.let(::storageToPaise) ?: return null
    val createdAt = (map["createdAt"] as? Number)?.toLong()
    val createdBy = map["createdBy"] as? String
    return when (map["type"]) {
        "expense" -> Transaction.Expense(
            id = id,
            description = map["description"] as? String ?: "",
            payerId = (map["payerId"] as? Number)?.toInt() ?: return null,
            amountPaise = paise,
            splitBetween = (map["splitBetween"] as? List<*>)?.filterIsInstance<Number>()?.map { it.toInt() },
            createdAt = createdAt,
            createdBy = createdBy,
        )
        "transfer" -> Transaction.Transfer(
            id = id,
            fromId = (map["fromId"] as? Number)?.toInt() ?: return null,
            toId = (map["toId"] as? Number)?.toInt() ?: return null,
            amountPaise = paise,
            createdAt = createdAt,
            createdBy = createdBy,
        )
        else -> null
    }
}

fun Transaction.toMap(): Map<String, Any> = buildMap {
    put("id", id)
    when (val tx = this@toMap) {
        is Transaction.Expense -> {
            put("type", "expense"); put("description", tx.description); put("payerId", tx.payerId)
            tx.splitBetween?.let { put("splitBetween", it) }
        }
        is Transaction.Transfer -> {
            put("type", "transfer"); put("fromId", tx.fromId); put("toId", tx.toId)
        }
    }
    put("amount", paiseToStorage(amountPaise))
    createdAt?.let { put("createdAt", it) }
    createdBy?.let { put("createdBy", it) }
}
