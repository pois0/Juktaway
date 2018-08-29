package info.justaway.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import info.justaway.JustawayApplication

object KeyboardUtil {

    private val inputMethodManager: InputMethodManager
        get() = JustawayApplication.app
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @JvmOverloads
    fun showKeyboard(view: View, delay: Int = 200) {
        view.postDelayed({
            /**
             * 表示されてないEditViewを表示と同時にキーボード出したい場合
             * フォーカスが当たってないとキーボードは出てこないのリスナーを使う
             * 元々設定されているリスナーを引っ張りだし、キーボード出したら戻しておく（行儀良い）
             */
            /**
             * 表示されてないEditViewを表示と同時にキーボード出したい場合
             * フォーカスが当たってないとキーボードは出てこないのリスナーを使う
             * 元々設定されているリスナーを引っ張りだし、キーボード出したら戻しておく（行儀良い）
             */
            val listener = view.onFocusChangeListener
            view.onFocusChangeListener = View.OnFocusChangeListener { v, has_focus ->
                if (!has_focus) {
                    return@OnFocusChangeListener
                }
                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED)
                v.onFocusChangeListener = listener
            }
            view.clearFocus()
            view.requestFocus()
        }, delay.toLong())
    }

    fun hideKeyboard(view: View) {
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
