package net.slash_omega.juktaway.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.fragment_around.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.parseWithClient
import kotlin.math.max

class AroundFragment: DialogFragment() {
    private lateinit var mProgressBarTop: ProgressBar
    private lateinit var mProgressBarBottom: ProgressBar
    private val mAdapter by lazy { StatusAdapter(activity!!) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity!!).apply {
        window?.requestFeature(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.fragment_around)

        mProgressBarTop = findViewById<View>(R.id.guruguru_top) as ProgressBar
        mProgressBarBottom = findViewById<View>(R.id.guruguru_bottom) as ProgressBar

        list.apply {
            registerForContextMenu(this)
            adapter = mAdapter
            activity?.let { a ->
                onItemClickListener = StatusClickListener(a)
                onItemLongClickListener = StatusLongClickListener(a)
            }
            arguments?.getString("status")?.toJsonObject()?.parseWithClient<Status>()?.let { origin ->
                GlobalScope.launch(Dispatchers.Main) {
                    mAdapter.addSuspend(origin)
                    val beforeList = runCatching {
                        currentClient.timeline.userTimelineByUserId(origin.user.id, count = 3, maxId = origin.id - 1).await()
                    }.getOrNull() ?: run {
                        MessageUtil.showToast(R.string.toast_load_data_failure)
                        return@launch
                    }
                    mProgressBarBottom.visibility = View.GONE

                    if (beforeList.isEmpty()) return@launch
                    mAdapter.addAllSuspend(beforeList)
                    mAdapter.notifyDataSetChanged()
                    val afterList = runCatching {
                        var lastId = beforeList[0].id - 1
                        for(i in 0 until 5) {
                            val statuses = currentClient.timeline.userTimelineByUserId(origin.user.id, count = 200, maxId = lastId).await()
                            for ((j, row) in statuses.withIndex()) {
                                if (row.id == origin.id && j > 0) return@runCatching statuses.subList(max(0, j - 4), j - 1)
                            }
                            lastId = statuses.last().id - 1
                        }
                        listOf<Status>()
                    }.getOrNull() ?: run {
                        MessageUtil.showToast(R.string.toast_load_data_failure)
                        return@launch
                    }

                    mProgressBarTop.visibility = View.GONE

                    if (afterList.isEmpty()) return@launch

                    afterList.forEachIndexed { i, status ->
                        mAdapter.insert(status, i)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}
