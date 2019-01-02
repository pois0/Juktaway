package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_retweeters.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.twitter.currentClient
import org.jetbrains.anko.support.v4.toast

class RetweetersFragment: DialogFragment() {
    private lateinit var mProgressBar: ProgressBar
    private val mAdapter by lazy { UserAdapter(activity, R.layout.row_user) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity!!).apply {
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setFlags(WindowManager.LayoutParams.FLAGS_CHANGED, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.fragment_retweeters)

        list.adapter = mAdapter
        mProgressBar = guruguru

        arguments?.getLong("statusId", -1L)?.takeIf { it > 0 }?.let { id ->
            GlobalScope.launch(Dispatchers.Main) {
                val statuses = runCatching { currentClient.status.retweets(id).await() }.getOrNull()
                mProgressBar.visibility = View.GONE

                if (statuses == null) {
                    toast(R.string.toast_load_data_failure)
                } else {
                    statuses.forEach { mAdapter.add(it.user) }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}