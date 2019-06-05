package net.slash_omega.juktaway.fragment.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.slash_omega.juktaway.adapter.ArrayAdapterBase
import net.slash_omega.juktaway.app

/**
 * Created on 2019/01/20.
 */
class MenuAdapter(c: Context, i: Int): ArrayAdapterBase<Menu>(c, i) {
    private val mMenuList = mutableListOf<Menu>()

    override fun add(menu: Menu?) { menu?.let {
        super.add(menu)
        mMenuList.add(menu)
    }}

    fun add(resId: Int, callback: () -> Unit) {
        add(Menu(resId, callback))
    }

    fun add(label: String, callback: () -> Unit) {
        add(Menu(label, callback))
    }

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ -> (this as TextView).text = mMenuList[pos].label }
}

class Menu(val label: String, val callback: Runnable) {
    constructor(resId: Int, callback: Runnable): this(app.getString(resId), callback)

    constructor(label: String, callback: () -> Unit): this(label, Runnable(callback))

    constructor(resId: Int, callback: () -> Unit): this(app.getString(resId), Runnable(callback))
}