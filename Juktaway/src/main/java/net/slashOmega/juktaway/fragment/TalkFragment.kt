package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.util.LongSparseArray
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AbsListView.LayoutParams
import com.google.common.primitives.Longs
import de.greenrobot.event.EventBus
import kotlinx.android.synthetic.main.list_talk.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.TwitterAdapter
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.HeaderStatusClickListener
import net.slashOmega.juktaway.listener.HeaderStatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Query
import twitter4j.Status
import java.lang.ref.WeakReference
import java.util.*

class TalkFragment: DialogFragment() {
    companion object {
        private class LoadTalk(talkFragment: TalkFragment) : AsyncTask<Long, Void, Status>() {
            val ref = WeakReference(talkFragment)

            override fun doInBackground(vararg p: Long?): twitter4j.Status? = try {
                p[0]?.let { ref.get()?.mTwitter?.showStatus(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            override fun onPostExecute(result: twitter4j.Status?) { ref.get()?.run {
                if (dialog == null) return
                result?.let { status ->
                    if (BasicSettings.talkOrderNewest) {
                        mAdapter.add(Row.newStatus(status))
                    } else {
                        val position = mListView.lastVisiblePosition
                        mAdapter.insert(Row.newStatus(status), 0)
                        mListView.setSelectionFromTop(position + 1, mListView.getChildAt(position)?.top ?: 0)
                        if (mListView.firstVisiblePosition > 0) {
                            mHeaderView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0)
                        }
                    }

                    if (status.inReplyToStatusId > 0) {
                        LoadTalk(this).execute(status.inReplyToStatusId)
                    } else {
                        removeGuruGuru()
                    }
                } ?: removeGuruGuru()
            }}
        }

        private class LoadTalkReply(talkFragment: TalkFragment): AsyncTask<Status, Void, ArrayList<Status>>() {
            val ref = WeakReference(talkFragment)

            override fun doInBackground(vararg p: twitter4j.Status?): ArrayList<twitter4j.Status> = ref.get()?.run {try {
                val source = p[0] ?: return@run null
                val toQuery = Query("to:" + source.user.screenName +" AND filter:replies").apply {
                    count = 200
                    sinceId = source.id
                    resultType = Query.ResultType.recent
                }
                val toResult = mTwitter.search(toQuery)
                val searchStatuses = toResult.tweets
                if (toResult.hasNext()) searchStatuses.addAll(mTwitter.search(toResult.nextQuery()).tweets)
                val fromQuery = Query("from:" + source.user.screenName + " AND filter:replies").apply {
                    count = 200
                    sinceId = source.id
                    resultType = Query.ResultType.recent
                }
                val fromResult = mTwitter.search(fromQuery)
                searchStatuses.addAll(fromResult.tweets)
                val isLoadMap = LongSparseArray<Boolean>().apply { searchStatuses.forEach { put(it.id, true) } }
                val lookupStatuses = ArrayList<twitter4j.Status>()
                val statusIds = ArrayList<Long>()
                searchStatuses.forEach {
                    if (it.inReplyToStatusId > 0 && !isLoadMap.get(it.inReplyToStatusId,  false)) {
                        statusIds.add(it.inReplyToStatusId)
                        isLoadMap.put(it.inReplyToStatusId, true)
                        if (statusIds.size == 100) {
                            lookupStatuses.addAll(mTwitter.lookup(*Longs.toArray(statusIds)))
                            statusIds.clear()
                        }
                    }
                }

                if (statusIds.size > 0) lookupStatuses.addAll(mTwitter.lookup(*Longs.toArray(statusIds)))

                searchStatuses.addAll(lookupStatuses)

                searchStatuses.sortWith(Comparator { a0, a1 -> when {
                        a0.id > a1.id -> 1
                        a0.id == a1.id -> 0
                        else -> -1
                    }})

                val isReplyMap = LongSparseArray<Boolean>().apply { put(source.id, true) }
                ArrayList<twitter4j.Status>().apply {
                    searchStatuses.forEach {
                        if (isReplyMap.get(it.inReplyToStatusId, false)) {
                            add(it)
                            isReplyMap.put(it.id, true)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }} ?: ArrayList()
        }
    }

    private val mTwitter by lazy { TwitterManager.getTwitter() }
    private val mAdapter by lazy { TwitterAdapter(activity, R.layout.row_tweet) }
    private val mListView by lazy { list }
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity).apply {
        window.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        }
        setContentView(R.layout.list_talk)

        (arguments?.getSerializable("status") as? Status)?.let { status ->
            val inReplyToAreaPixels = if (status.inReplyToStatusId > 0) resources.displayMetrics.heightPixels else 0
            val (headerH, footerH) = if (BasicSettings.talkOrderNewest) Pair(100, inReplyToAreaPixels)
                    else Pair(inReplyToAreaPixels, 100)
            mHeaderView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, headerH)
            mFooterView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, footerH)

            mListView.apply {
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
            if (status.inReplyToStatusId > 0) LoadTalk(this@TalkFragment).execute(status.inReplyToStatusId)
            else removeGuruGuru()
            LoadTalkReply(this@TalkFragment).execute(status)
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

    fun onEventMainThread(event: StreamingDestroyStatusEvent) { mAdapter.removeStatus(event.statusId!!) }

    fun removeGuruGuru() { (if (BasicSettings.talkOrderNewest) guruguru_footer else guruguru_header).visibility = View.GONE }
}