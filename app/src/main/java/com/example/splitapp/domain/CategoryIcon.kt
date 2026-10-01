package com.example.splitapp.domain

// Whole-word match (a trailing "s"/"es" is allowed); "word*" is a prefix match. First matching rule wins.
private val CATEGORIES = listOf(
    "🍔" to listOf("food", "dinner", "lunch", "cafe", "drink", "eat", "restaurant", "snack", "tea"),
    "🚗" to listOf("cab", "taxi", "fuel", "car", "bus", "travel*", "flight", "trip", "metro", "train"),
    "🛍️" to listOf("shopping", "dress", "gift", "grocer*", "mart", "buy", "store"),
    "🎟️" to listOf("movie", "ticket", "show", "game", "play", "entertainment"),
    "🏠" to listOf("rent", "bill", "elect*", "wifi", "stay", "hotel", "room"),
)

private fun matches(token: String, word: String) =
    if (word.endsWith("*")) token.startsWith(word.dropLast(1))
    else token == word || token == word + "s" || token == word + "es"

/** Automatic icon from the description's words. */
fun getCategoryIcon(desc: String): String {
    val tokens = desc.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
    return CATEGORIES.firstOrNull { (_, words) -> tokens.any { t -> words.any { matches(t, it) } } }?.first ?: "📄"
}
