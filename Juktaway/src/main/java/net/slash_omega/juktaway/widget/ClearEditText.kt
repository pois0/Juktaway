package net.slash_omega.juktaway.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.EditText

/**
 * バックキーでフォーカスが外れるEditTextの拡張
 */
class ClearEditText : EditText {

    val string: String
        get() = text?.toString() ?: ""

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            clearFocus()
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
