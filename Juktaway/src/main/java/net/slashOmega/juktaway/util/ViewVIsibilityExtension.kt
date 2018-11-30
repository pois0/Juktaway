package net.slashOmega.juktaway.util

import android.view.View
import java.lang.IllegalArgumentException

/**
 * Created on 2018/10/18.
 */

enum class Visibility(val value: Int) { VISIBLE(0), INVISIBLE(1), GONE(2) }

var View.visibilityE: Visibility
        get() = Visibility.values().find { it.value == visibility }?: throw IllegalArgumentException()
        set(v) { visibility = v.value }