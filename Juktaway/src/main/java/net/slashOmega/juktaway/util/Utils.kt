package net.slashOmega.juktaway.util

/**
 * Created on 2018/11/13.
 */

inline fun <T, R> T.tryAndTrace (block: T.() -> R): Boolean = runCatching { block() }.run {
    exceptionOrNull()?.printStackTrace()
    isSuccess
}

inline fun <R> tryAndTrace (block: () -> R): Boolean = runCatching { block() }.run {
    exceptionOrNull()?.printStackTrace()
    isSuccess
}

inline fun String?.nullToEmpty() = this ?: ""