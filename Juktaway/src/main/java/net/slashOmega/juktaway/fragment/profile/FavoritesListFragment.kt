package net.slashOmega.juktaway.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.greenrobot.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.tryAndTraceGet
import twitter4j.Paging

/**
 * Created on 2018/11/18.
 */
internal class FavoritesListFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.list_guruguru
    private var mMaxId = 0L

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val job = async(Dispatchers.Default) {
                tryAndTraceGet {
                    TwitterManager.getTwitter().getFavorites(user.id, Paging().apply {
                        if (mMaxId > 0) {
                            maxId = mMaxId - 1
                            count = BasicSettings.pageCount
                        }
                    })
                }
            }

            mFooter.visibility = View.GONE
            job.await()?.takeIf { it.isNotEmpty() }?.run {
                forEach {
                    if (mMaxId == 0L || mMaxId > it.id) mMaxId = it.id
                    mAdapter.add(Row.newStatus(it))
                }
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            mListView.onItemClickListener = StatusClickListener(activity!!)
            mListView.onItemLongClickListener = StatusLongClickListener(activity!!)
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
        mAdapter.removeStatus(event.statusId!!)
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        showList()
    }
}