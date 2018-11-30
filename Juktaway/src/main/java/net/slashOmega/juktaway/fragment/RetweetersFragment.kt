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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class RetweetersFragment: DialogFragment() {
    private lateinit var mProgressBar: ProgressBar
    private val mAdapter by lazy { UserAdapter(activity, R.layout.row_user) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity).apply {
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setFlags(WindowManager.LayoutParams.FLAGS_CHANGED, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.fragment_retweeters)

        list.adapter = mAdapter
        mProgressBar = guruguru

        arguments?.getLong("statusId")?.takeIf { it > 0 }?.let {
            GlobalScope.launch(Dispatchers.Main) {
                val retweetJob = async(Dispatchers.Default) {
                    try {
                        TwitterManager.twitter.getRetweets(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                mProgressBar.visibility = View.GONE

                retweetJob.await()?.let { statuses ->
                    statuses.forEach { mAdapter.add(it.user) }
                    mAdapter.notifyDataSetChanged()
                } ?: MessageUtil.showToast(R.string.toast_load_data_failure)
            }
        }
    }
}