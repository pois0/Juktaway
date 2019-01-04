package net.slashOmega.juktaway.fragment.profile

import android.view.View
import jp.nephy.penicillin.core.PenicillinCursorJsonObjectAction
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
    var cursor: PenicillinCursorJsonObjectAction<CursorUsers>? = null

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val action = cursor ?: currentClient.friend.list(user.screenName)
            val resp = tryAndTraceGet {
                action.await().apply {
                    cursor = next()
                }
            }

            mFooter.visibility = View.GONE
            if (resp != null) {
                resp.result.users.takeIf { it.isNotEmpty() }?.forEach { mAdapter.add(it) }
                // TODO hasNext
                if (resp.result.nextCursor != 0L) mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
            finishLoading()
        }
    }
}