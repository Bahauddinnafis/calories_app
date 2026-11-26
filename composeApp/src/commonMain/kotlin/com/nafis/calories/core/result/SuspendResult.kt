package com.nafis.calories.core.result

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching], but lets coroutine cancellation propagate. */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }