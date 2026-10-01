package com.example.splitapp

/**
 * Leaves exactly [key] on the stack, so nothing (a group, the admin panel) stays underneath for Back to return to.
 * The new key goes in first and the rest is dropped after, so the stack is never empty (NavDisplay needs one entry).
 */
fun <T> MutableList<T>.resetTo(key: T) {
    add(key)
    subList(0, size - 1).clear()
}

/** Ignores a double tap: does nothing when [key] is already the top screen. */
fun <T> MutableList<T>.pushIfNotTop(key: T) {
    if (lastOrNull() != key) add(key)
}
