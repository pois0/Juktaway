package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.util.LongSparseArray
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AbsListView.LayoutParams
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.parameters.SearchResultType
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.list_talk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.HeaderStatusClickListener
import net.slashOmega.juktaway.listener.HeaderStatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient
import java.util.*
import kotlin.collections.ArrayList

class TalkFragment: DialogFragment() {
    private val mAdapter by lazy { StatusAdapter(activity!!) }
    private lateinit var mListView: ListView
    private val mHeaderView by lazy { View(activity) }
    private val mFooterView by lazy { View(activity) }
    private val mOnScrollListener = object: AbsListView.OnScrollListener {
        override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {}
        override fun onScrollStateChanged(p0: AbsListView?, scrollState: Int) {
            when (scrollState) {
                AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> {
                    if (mListView.firstVisiblePosition > 0) {
                        mHeaderView.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 0)
                    }
                    if (mListView.lastVisiblePosition <= mAdapter.count) {
                        mFooterView.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 0)
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity!!).apply {
        window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        }

        setContentView(R.layout.list_talk)

        arguments?.getString("status")?.toJsonObject()?.parse(Status::class)?.let { status ->
            val inReplyToAreaPixels = if (status.inReplyToStatusId != null) resources.displayMetrics.heightPixels else 0
            val (headerH, footerH) = if (BasicSettings.talkOrderNewest) Pair(100, inReplyToAreaPixels)
            else Pair(inReplyToAreaPixels, 100)
            mHeaderView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, headerH)
            mFooterView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, footerH)

            mListView = list.apply {
                addHeaderView(mHeaderView, null, false)
                addFooterView(mFooterView, null, false)
                adapter = mAdapter
                setOnScrollListener(mOnScrollListener)
                activity?.let {
                    onItemClickListener = HeaderStatusClickListener(it)
                    onItemLongClickListener = HeaderStatusLongClickListener(it)
                }
            }

            mAdapter.add(Row.newStatus(status))

            if (!BasicSettings.talkOrderNewest) mListView.setSelectionFromTop(1, 0)
            status.inReplyToStatusId?.let { loadTalk(it) }
            loadTalkReply(status)
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

    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) { GlobalScope.launch(Dispatchers.Main) { mAdapter.removeStatus(event.statusId!!) } }

    private fun Dialog.removeGuruGuru() { (if (BasicSettings.talkOrderNewest) guruguru_footer else guruguru_header).visibility = View.GONE }

    private fun loadTalk(idParam: Long) {
        var statusId = idParam
        GlobalScope.launch(Dispatchers.Main) {
            while (statusId > 0) {
                val status = runCatching { currentClient.statuses.show(statusId).await().result }.getOrNull() ?: break

                if (BasicSettings.talkOrderNewest) {
                    mAdapter.addSuspend(Row.newStatus(status))
                } else {
                    val pos = mListView.lastVisiblePosition
                    mAdapter.insertSuspend(Row.newStatus(status), 0)
                    mListView.setSelectionFromTop(pos + 1, mListView.getChildAt(pos)?.top ?: 0)
                    if (mListView.firstVisiblePosition > 0) {
                        mHeaderView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
                    }
                }

                status.inReplyToStatusId?.let { statusId = it }
            }
            dialog?.removeGuruGuru()
        }
    }

    private fun loadTalkReply(source: Status) {
        GlobalScope.launch(Dispatchers.Main) {
            runCatching {
                val toResult = currentClient.search.search("to:" + source.user.screenName + " AND filter:replies",
                        count = 200,
                        sinceId = source.id,
                        resultType = SearchResultType.Recent).await()
                val searchedStatuses = toResult.result.statuses.toMutableList()
                if (toResult.hasNext) searchedStatuses.addAll(toResult.next.await().result.statuses)

                val fromResult = currentClient.search.search("from:" + source.user.screenName + " AND filter:replies",
                        count = 200,
                        sinceId = source.id,
                        resultType = SearchResultType.Recent).await()
                searchedStatuses.addAll(fromResult.result.statuses)
                val isLoadMap = LongSparseArray<Boolean>().apply { searchedStatuses.forEach { put(it.id, true) } }
                val lookupStatuses = ArrayList<Status>()
                val statusIds = mutableListOf<Long>()
                searchedStatuses.forEach { status ->
                    if (status.inReplyToStatusId != null &&
                            status.inReplyToStatusId?.let { isLoadMap.get(it, false) } == true) {
                        statusIds.add(status.inReplyToStatusId!!)
                        isLoadMap.put(status.inReplyToStatusId!!, true)
                        if (statusIds.size == 200) {
                            lookupStatuses.addAll(currentClient.statuses.lookup(statusIds).await())
                            statusIds.clear()
                        }
                    }
                }

                if (statusIds.size > 0) lookupStatuses.addAll(currentClient.statuses.lookup(statusIds).await())

                searchedStatuses.addAll(lookupStatuses)

                searchedStatuses.sortWith(Comparator { a0, a1 -> when {
                    a0.id > a1.id -> 1
                    a0.id == a1.id -> 0
                    else -> -1
                }})

                val isReplyMap = LongSparseArray<Boolean>().apply { put(source.id, true) }
                mutableListOf<Status>().apply {
                    searchedStatuses.forEach { status ->
                        if (status.inReplyToStatusId?.let { isReplyMap.get(it, false) } == true) {
                            add(status)
                            isReplyMap.put(status.id, true)
                        }
                    }
                }
            }.onSuccess { statuses ->
                if (dialog == null) return@launch
                dialog.run {
                    if (BasicSettings.talkOrderNewest) guruguru_header else guruguru_footer
                }.visibility = View.GONE

                if (BasicSettings.talkOrderNewest) {
                    val lastPos = mListView.lastVisiblePosition

                    val y = mListView.getChildAt(lastPos)?.top ?: 0

                    statuses.forEach { mAdapter.insertSuspend(Row.newStatus(it), 0) }
                    mListView.setSelectionFromTop(lastPos + statuses.size, y)

                    if (mListView.firstVisiblePosition > 0) {
                        mHeaderView.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 0)
                    }
                } else {
                    mAdapter.addAllFromStatuses(statuses)
                }
            }
        }
    }
}