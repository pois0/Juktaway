package net.slashOmega.juktaway.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserListAdapter
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.tryAndTraceGet

/**
 * Created on 2018/11/18.
 */
internal class UserListMembershipsFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { UserListAdapter(activity!!, R.layout.row_user_list) }
    override val layout = R.layout.list_guruguru
    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val job = async(Dispatchers.Default) {
                tryAndTraceGet {
                    TwitterManager.twitter.getUserListMemberships(user.id, cursor).apply {
                        cursor = nextCursor
                    }
                }
            }

            mFooter.visibility = View.GONE
            job.await()?.run {
                forEach { mAdapter.add(it) }
                if (hasNext()) mAutoLoader = true
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