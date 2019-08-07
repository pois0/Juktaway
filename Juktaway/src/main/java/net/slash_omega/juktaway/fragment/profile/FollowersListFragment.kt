package net.slash_omega.juktaway.fragment.profile

import android.view.View
import jp.nephy.penicillin.core.request.action.CursorJsonObjectApiAction
import jp.nephy.penicillin.endpoints.followers
import jp.nephy.penicillin.endpoints.followers.listUsersByScreenName
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.cursor.CursorUsers
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.UserAdapter
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.tryAndTraceGet

internal class FollowersListFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { UserAdapter(activity!!, R.layout.row_user) }
    override val layout = R.layout.list_guruguru
    private var cursor: CursorJsonObjectApiAction<CursorUsers>? = null

    override fun showList() {
        launch {
            val action = cursor ?: currentClient.followers.listUsersByScreenName(user.screenName)

            action.runCatching { await() }.onSuccess { response ->
                mAdapter.addAll(response.result.users)

                if (response.hasNext) {
                    cursor = response.next
                    mAutoLoader = true
                }

                mListView.visibility = View.VISIBLE
            }

            finishLoading()
        }
    }
}
