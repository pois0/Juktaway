package net.slash_omega.juktaway.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.nephy.penicillin.core.request.action.CursorJsonObjectApiAction
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.CursorLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.UserListAdapter
import net.slash_omega.juktaway.twitter.currentClient

/**
 * Created on 2018/11/18.
 */
internal class UserListMembershipsFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { UserListAdapter(activity!!, R.layout.row_user_list) }
    override val layout = R.layout.list_guruguru
    private var nextCursor: CursorJsonObjectApiAction<CursorLists>? = null
    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val action = runCatching {
                (nextCursor ?: currentClient.lists.memberships(user.id)).await()
            }.getOrNull()

            if (action?.hasNext == true) {
                nextCursor = action.next
                mAutoLoader = true
            }

            action?.result?.run {
                lists.forEach { mAdapter.add(it) }
                mListView.visibility = View.VISIBLE
            }

            mFooter.visibility = View.GONE

            finishLoading()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            registerForContextMenu(mListView)
        }
    }
}