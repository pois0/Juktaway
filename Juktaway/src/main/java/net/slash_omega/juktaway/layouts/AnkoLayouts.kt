package net.slash_omega.juktaway.layouts

import android.view.ViewManager
import net.slash_omega.juktaway.widget.FontelloTextView
import org.jetbrains.anko.custom.ankoView

/**
 * Created on 2018/11/14.
 */

fun ViewManager.fontelloTextView() = fontelloTextView {}

inline fun ViewManager.fontelloTextView(init: FontelloTextView.() -> Unit): FontelloTextView
    = ankoView({ FontelloTextView(it) }, theme = 0, init = init)