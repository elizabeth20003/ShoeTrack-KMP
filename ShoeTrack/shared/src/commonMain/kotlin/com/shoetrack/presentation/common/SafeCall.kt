package com.shoetrack.presentation.common

import kotlinx.coroutines.CancellationException

/** Like [runCatching], but lets coroutine cancellation propagate instead of swallowing it. */
internal inline fun <T> runCatchingExceptCancellation(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Result.failure(failure)
    }
