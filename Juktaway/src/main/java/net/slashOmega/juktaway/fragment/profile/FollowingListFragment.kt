package net.slashOmega.juktaway.fragment.profile

import android.view.View
import jp.nephy.penicillin.core.request.action.CursorJsonObjectApiAction
import jp.nephy.penicillin.endpoints.friends
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.CursorUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.tryAndTraceGet

internal class FollowingListFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { UserAdapter(activity, R.layout.row_user) }
    override val layout = R.layout.list_guruguru
    var cursor: CursorJsonObjectApiAction<CursorUsers>? = null

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val action = cursor ?: currentClient.friends.list(user.screenName)
            val resp = tryAndTraceGet {
                action.await().apply {
                    if (hasNext) cursor = next
                }
            }

            if (resp != null) {
                resp.result.users.takeIf { it.isNotEmpty() }?.forEach { mAdapter.add(it) }
                if (resp.hasNext) mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
            finishLoading()
        }
    }
}