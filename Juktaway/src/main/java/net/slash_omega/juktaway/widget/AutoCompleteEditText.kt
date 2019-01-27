package net.slash_omega.juktaway.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.AutoCompleteTextView
import kotlin.math.max


class AutoCompleteEditText : AutoCompleteTextView {

    private var myThreshold: Int = 0

    val string: String
        get() {
            val editable = text ?: return ""
            return editable.toString()
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setThreshold(threshold: Int) {
        myThreshold = max(threshold, 0)
    }

    override fun enoughToFilter(): Boolean {
        return text.length >= myThreshold
    }

    override fun getThreshold(): Int {
        return myThreshold
    }
}