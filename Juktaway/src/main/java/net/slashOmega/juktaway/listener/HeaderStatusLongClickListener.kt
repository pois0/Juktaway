package net.slashOmega.juktaway.listener

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.HeaderViewListAdapter
import net.slashOmega.juktaway.adapter.TwitterAdapter

/**
 * Created on 2018/08/27.
 */
class HeaderStatusLongClickListener(activity: Activity): StatusLongClickListener(activity) {
    override fun getAdapter(adapterView: AdapterView<*>): TwitterAdapter {
        return (adapterView.adapter as HeaderViewListAdapter).wrappedAdapter as TwitterAdapter
    }

    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        return super.onItemLongClick(adapterView, view, position - 1, id)
    }
}