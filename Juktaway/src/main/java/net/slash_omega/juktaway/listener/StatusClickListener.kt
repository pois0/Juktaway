package net.slash_omega.juktaway.listener

import androidx.fragment.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.fragment.dialog.StatusMenuFragment

/**
 * Created on 2018/08/27.
 */
open class StatusClickListener(private val mFragmentActivity: FragmentActivity) : AdapterView.OnItemClickListener {

    open fun getAdapter(adapterView: AdapterView<*>): StatusAdapter {
        return adapterView.adapter as StatusAdapter
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        getAdapter(adapterView).getItem(i)?.let {
            StatusMenuFragment.newInstance(it).show(mFragmentActivity.supportFragmentManager, "dialog")
        }
    }
}
