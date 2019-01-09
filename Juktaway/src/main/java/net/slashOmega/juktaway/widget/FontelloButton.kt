package net.slashOmega.juktaway.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.font

class FontelloButton : Button {

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (isInEditMode) { return }
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (isInEditMode) { return }
        init()
    }

    private fun init() { typeface = font }
}
