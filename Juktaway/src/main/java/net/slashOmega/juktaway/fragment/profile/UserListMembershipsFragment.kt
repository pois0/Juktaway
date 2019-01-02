package net.slashOmega.juktaway.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.nephy.penicillin.core.PenicillinCursorJsonObjectAction
import jp.nephy.penicillin.models.CursorLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserListAdapter
import net.slashOmega.juktaway.twitter.currentClient

/**
 * Created on 2018/11/18.
 */
internal class UserListMembershipsFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { UserListAdapter(activity!!, R.layout.row_user_list) }
    override val layout = R.layout.list_guruguru
    var nextCursor: PenicillinCursorJsonObjectAction<CursorLists>? = null
    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val action = runCatching {
                (nextCursor ?: currentClient.list.memberships(user.id)).await()
            }.getOrNull()
            nextCursor = action?.next()

            mFooter.visibility = View.GONE
            action?.result?.run {
                lists.forEach { mAdapter.add(it) }
                // TODO
                if (nextCursor != 0L) mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }

            finishLoading()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            registerForContextMenu(mListView)
        }
    }
}