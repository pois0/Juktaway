package net.slash_omega.juktaway.listener

import androidx.fragment.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import android.widget.HeaderViewListAdapter
import net.slash_omega.juktaway.adapter.StatusAdapter

/**
 * Created on 2018/08/27.
 */
class HeaderStatusClickListener(fActivity: FragmentActivity): StatusClickListener(fActivity) {

    override fun getAdapter(adapterView: AdapterView<*>): StatusAdapter {
        val headerViewListAdapter = adapterView.adapter as HeaderViewListAdapter
        return headerViewListAdapter.wrappedAdapter as StatusAdapter
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        super.onItemClick(adapterView, view, i - 1, l)
    }
}
