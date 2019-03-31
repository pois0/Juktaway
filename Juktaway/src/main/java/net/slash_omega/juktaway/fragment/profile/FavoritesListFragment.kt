package net.slash_omega.juktaway.fragment.profile

import android.view.View
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.endpoints.favorites
import jp.nephy.penicillin.endpoints.favorites.listByUserId
import jp.nephy.penicillin.extensions.await
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

/**
 * Created on 2018/11/18.
 */
internal class FavoritesListFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.list_guruguru
    private var mMaxId = 0L

    override fun showList() {
        launch {
            val statuses = runCatching {
                currentClient.favorites.run {
                    if (mMaxId > 0) listByUserId(user.id, maxId = mMaxId - 1, count = preferences.api.pageCount)
                    else listByUserId(user.id, count = preferences.api.pageCount)
                }.await()
            }.getOrNull()

            statuses?.takeIf { it.isNotEmpty() }?.run {
                mMaxId = statuses.last().id
                mAdapter.extensionAddAllFromStatuses(statuses)
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }

            finishLoading()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        launch { mAdapter.removeStatus(event.statusId!!) }
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        showList()
    }

    override fun View.init() {
        mListView.onItemClickListener = StatusClickListener(activity!!)
        mListView.onItemLongClickListener = StatusLongClickListener(activity!!)
    }
}