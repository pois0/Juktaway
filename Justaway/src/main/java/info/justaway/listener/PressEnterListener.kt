package info.justaway.listener

import android.view.KeyEvent
import android.view.View

/**
 * Created on 2018/08/24.
 */
class PressEnterListener(private val exec: () -> Boolean): View.OnKeyListener {
    override fun onKey(v: View?, code: Int, e: KeyEvent?): Boolean {
        return if (e?.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) exec()
        else false
    }
}