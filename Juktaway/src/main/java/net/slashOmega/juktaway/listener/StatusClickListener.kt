package net.slashOmega.juktaway.listener

import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import net.slashOmega.juktaway.adapter.TwitterAdapter
import net.slashOmega.juktaway.fragment.dialog.StatusMenuFragment

/**
 * Created on 2018/08/27.
 */
open class StatusClickListener(private val mFragmentActivity: FragmentActivity) : AdapterView.OnItemClickListener {

    open fun getAdapter(adapterView: AdapterView<*>): TwitterAdapter {
        return adapterView.adapter as TwitterAdapter
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        getAdapter(adapterView).getItem(i)?.let {
            StatusMenuFragment.newInstance(it).show(mFragmentActivity.supportFragmentManager, "dialog")
        }
    }
}
