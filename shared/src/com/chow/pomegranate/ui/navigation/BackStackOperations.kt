package com.chow.pomegranate.ui.navigation

/**
 * Removes the current destination while keeping the root destination in place.
 *
 * This is shared by screen and dialog back handling so that dismiss gestures cannot leave an
 * empty back stack.
 */
fun <T> MutableList<T>.popDestination(): Boolean {
    if (size <= 1) return false
    removeAt(lastIndex)
    return true
}

/** Replaces the current top-level destination without accumulating tab history. */
fun <T> MutableList<T>.selectTopLevel(destination: T) {
    if (size == 1 && first() == destination) return
    clear()
    add(destination)
}
