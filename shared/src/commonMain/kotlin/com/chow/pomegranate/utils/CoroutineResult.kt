package com.chow.pomegranate.utils

import kotlinx.coroutines.CancellationException

/**
 * Runs a suspending operation while preserving structured-concurrency cancellation.
 *
 * Kotlin's [runCatching] catches every [Throwable], including [CancellationException].
 * UI callers that turn a failed [Result] into an error state should use this helper so a
 * cancelled request does not incorrectly render an error.
 */
internal suspend inline fun <T> runSuspendCatching(
    block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
