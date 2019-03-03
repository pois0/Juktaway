package net.slash_omega.juktaway.util

/**
 * Created on 2018/11/13.
 */

inline fun <T, R> T.tryAndTraceGet (block: T.() -> R): R? = runCatching { block() }.run {
    exceptionOrNull()?.printStackTrace()
    getOrNull()
}

fun String?.nullToBlank() = this ?: ""

fun CharSequence?.toString() = this?.toString() ?: ""

fun CharSequence?.takeNotEmpty() = this.takeUnless { it.isNullOrEmpty() }

fun String?.takeNotEmpty() = this.takeUnless { it.isNullOrEmpty() }

fun <T> Array<out T>?.takeNotEmpty() = this.takeUnless { it.isNullOrEmpty() }

fun <T> Collection<T>?.takeNotEmpty() = this.takeUnless { it.isNullOrEmpty() }
