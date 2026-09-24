package com.softhome.core.common

/** Lightweight result type for repository operations that can fail gracefully. */
sealed interface SoftResult<out T> {
    data class Success<T>(val value: T) : SoftResult<T>
    data class Failure(val reason: String, val cause: Throwable? = null) : SoftResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value

    companion object {
        inline fun <T> catching(block: () -> T): SoftResult<T> = try {
            Success(block())
        } catch (t: Throwable) {
            Failure(t.message ?: "Unknown error", t)
        }
    }
}

inline fun <T, R> SoftResult<T>.map(transform: (T) -> R): SoftResult<R> = when (this) {
    is SoftResult.Success -> SoftResult.Success(transform(value))
    is SoftResult.Failure -> this
}
