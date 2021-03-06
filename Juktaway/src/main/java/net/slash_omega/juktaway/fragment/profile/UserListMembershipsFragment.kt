package net.slash_omega.juktaway.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.nephy.penicillin.core.request.action.CursorJsonObjectApiAction
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.lists.membershipsByUserId
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.cursor.CursorLists
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
        launch {
            val action = nextCursor ?: currentClient.lists.membershipsByUserId(user.id)
            action.runCatching { await() }.onSuccess { response ->
                if (response.hasNext) {
                    nextCursor = response.next
                    mAutoLoader = true
                }

                mAdapter.addAll(response.result.lists)
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
