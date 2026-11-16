package com.chow.pomegranate.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CoroutineResultTest {
    @Test
    fun runSuspendCatchingReturnsOrdinaryFailures() = runTest {
        val result = runSuspendCatching { error("network failure") }

        assertTrue(result.isFailure)
        assertEquals("network failure", result.exceptionOrNull()?.message)
    }

    @Test
    fun runSuspendCatchingRethrowsCancellation() = runTest {
        assertFailsWith<CancellationException> {
            runSuspendCatching { throw CancellationException("screen left") }
        }
    }
}
