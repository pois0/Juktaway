package net.slash_omega.juktaway.util

import android.os.Bundle
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.models.Status

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

fun <T> Collection<T>?.takeNotEmpty() = this.takeUnless { it.isNullOrEmpty() }

fun Status.generateJsonBundle(capacity: Int = 1) = Bundle(capacity).apply {
    putString("status", toJsonString())
}
