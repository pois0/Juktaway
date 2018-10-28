package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_around.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.TwitterAdapter
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class AroundFragment: DialogFragment() {
    companion object {
        private class BeforeStatusTask(f: AroundFragment): AsyncTask<Status, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg p: twitter4j.Status?): ResponseList<twitter4j.Status>? = p[0]?.let {
                try {
                    TwitterManager.getTwitter().getUserTimeline(it.user.screenName, Paging().apply {
                        count = 3
                        maxId = it.id - 1
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(statuses: ResponseList<twitter4j.Status>?) { ref.get()?.apply {
                mProgressBarBottom.visibility = View.GONE
                statuses?.let {
                    if (it.size > 0) {
                        it.forEach { s -> mAdapter.add(Row.newStatus(s)) }
                        mAdapter.notifyDataSetChanged()
                        AfterStatusTask(this).execute(it[0])
                    }
                } ?:
                    MessageUtil.showToast(R.string.toast_load_data_failure)
                }
            }
        }

        private class AfterStatusTask(f: AroundFragment): AsyncTask<Status, Void, List<Status>>() {
            private val ref = WeakReference(f)

            override fun doInBackground(vararg p: twitter4j.Status?): List<twitter4j.Status>? = p[0]?.let { s ->
                try {
                    val paging = Paging().apply {
                        count = 200
                        sinceId = s.id - 1
                    }
                    for (i in 1..5) {
                        paging.page = i
                        val statuses = TwitterManager.getTwitter().getUserTimeline(s.user.screenName)
                        for ((j, row) in statuses.withIndex()) {
                            if (row.id == s.id && j > 0) return statuses.subList(Math.max(0, j - 4), j - 1)
                        }
                    }
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: List<twitter4j.Status>?) { ref.get()?.apply {
                mProgressBarBottom.visibility = View.GONE
                result?.let {
                    it.forEachIndexed { i, status -> mAdapter.insert(Row.newStatus(status), i) }
                    mAdapter.notifyDataSetChanged()
                }
            }}
        }
    }

    private lateinit var mProgressBarTop: ProgressBar
    private lateinit var mProgressBarBottom: ProgressBar
    private val mAdapter by lazy { TwitterAdapter(activity, R.layout.row_tweet) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity).apply {
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
            (arguments?.getSerializable("status") as? Status)?.let {
                mAdapter.add(Row.newStatus(it))
                BeforeStatusTask(this@AroundFragment).execute(it)
            }
        }
    }
}