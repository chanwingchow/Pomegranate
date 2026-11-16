package com.chow.pomegranate.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackStackOperationsTest {
    @Test
    fun popDestinationKeepsRootDestination() {
        val backStack = mutableListOf("root")

        assertFalse(backStack.popDestination())
        assertEquals(listOf("root"), backStack)
    }

    @Test
    fun popDestinationRemovesDialogOrDetailDestination() {
        val backStack = mutableListOf("root", "detail", "dialog")

        assertTrue(backStack.popDestination())
        assertEquals(listOf("root", "detail"), backStack)
    }

    @Test
    fun selectTopLevelClearsPreviousNavigationHistory() {
        val backStack = mutableListOf("timetable", "detail")

        backStack.selectTopLevel("modules")

        assertEquals(listOf("modules"), backStack)
    }
}
