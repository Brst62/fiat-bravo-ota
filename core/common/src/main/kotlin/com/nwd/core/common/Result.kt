package com.nwd.core.common

sealed class NwdResult<out T> {
    data class Ok<T>(val value: T) : NwdResult<T>()
    data class Err(val message: String, val cause: Throwable? = null) : NwdResult<Nothing>()
}

inline fun <T> runCatchingNwd(block: () -> T): NwdResult<T> = try {
    NwdResult.Ok(block())
} catch (t: Throwable) {
    NwdResult.Err(t.message ?: t::class.java.simpleName, t)
}
