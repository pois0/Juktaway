package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_retweeters.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class RetweetersFragment: DialogFragment() {
    companion object {
        private class RetweetsTask(f: RetweetersFragment): AsyncTask<Long, Void, ResponseList<Status>>() {
            private val ref = WeakReference(f)

            override fun doInBackground(vararg p: Long?): ResponseList<twitter4j.Status>? = p[0]?.let {
                try {
                    TwitterManager.getTwitter().getRetweets(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: ResponseList<twitter4j.Status>?) { ref.get()?.run {
                mProgressBar.visibility = View.GONE
                result?.let { statuses ->
                    statuses.forEach { mAdapter.add(it.user) }
                    mAdapter.notifyDataSetChanged()
                } ?: MessageUtil.showToast(R.string.toast_load_data_failure)
            }}
        }
    }

    private lateinit var mProgressBar: ProgressBar
    private val mAdapter by lazy { UserAdapter(activity, R.layout.row_user) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity).apply {
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setFlags(WindowManager.LayoutParams.FLAGS_CHANGED, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.fragment_retweeters)

        list.adapter = mAdapter
        mProgressBar = guruguru

        arguments?.getLong("statusId")?.takeIf { it > 0 }?.let { RetweetsTask(this@RetweetersFragment).execute(it) }
    }
}