package net.slashOmega.juktaway.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import net.slashOmega.juktaway.JuktawayApplication

class FontelloTextView : TextView {

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (isInEditMode) { return }
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (isInEditMode) { return }
        init()
    }

    private fun init() { typeface = JuktawayApplication.font }
}
