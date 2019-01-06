package net.slashOmega.juktaway.fragment.profile

import android.view.View
import de.greenrobot.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

/**
 * Created on 2018/11/18.
 */
internal class FavoritesListFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.list_guruguru
    private var mMaxId = 0L

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                currentClient.favorites.run {
                    if (mMaxId > 0) list(userId = user.id, maxId = mMaxId - 1, count = BasicSettings.pageCount)
                    else list(userId = user.id, count = BasicSettings.pageCount)
                }.await()
            }.getOrNull()

            statuses?.takeIf { it.isNotEmpty() }?.run {
                mMaxId = statuses.last().id
                mAdapter.extensionAddAllFromStatusesSuspend(statuses)
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

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        GlobalScope.launch(Dispatchers.Main) { mAdapter.removeStatus(event.statusId!!) }
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