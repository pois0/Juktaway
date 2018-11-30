package net.slashOmega.juktaway.layouts

import android.view.ViewManager
import net.slashOmega.juktaway.widget.FontelloTextView
import org.jetbrains.anko.custom.ankoView

/**
 * Created on 2018/11/14.
 */

inline fun ViewManager.fontelloTextView() = fontelloTextView {}

inline fun ViewManager.fontelloTextView(init: FontelloTextView.() -> Unit): FontelloTextView
    = ankoView({ FontelloTextView(it) }, theme = 0, init = init)