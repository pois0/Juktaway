package net.slash_omega.juktaway.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

/**
 * Created on 2019/02/03.
 */

val Context.scope: CoroutineScope
    get() = (this as? CoroutineScope) ?: GlobalScope