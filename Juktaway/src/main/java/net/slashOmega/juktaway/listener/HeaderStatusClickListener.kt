package net.slashOmega.juktaway.listener

import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import android.widget.HeaderViewListAdapter
import net.slashOmega.juktaway.adapter.TwitterAdapter

/**
 * Created on 2018/08/27.
 */
class HeaderStatusClickListener(fActivity: FragmentActivity): StatusClickListener(fActivity) {

    override fun getAdapter(adapterView: AdapterView<*>): TwitterAdapter {
        val headerViewListAdapter = adapterView.adapter as HeaderViewListAdapter
        return headerViewListAdapter.wrappedAdapter as TwitterAdapter
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        super.onItemClick(adapterView, view, i - 1, l)
    }
}