package info.justaway.listener

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import info.justaway.adapter.TwitterAdapter
import info.justaway.fragment.AroundFragment
import info.justaway.fragment.TalkFragment
import info.justaway.settings.BasicSettings
import info.justaway.util.ActionUtil

/**
 * Created on 2018/08/27.
 */
open class StatusLongClickListener(activity: Activity): AdapterView.OnItemLongClickListener {
    val activity = activity as FragmentActivity

    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        return adapterView?.let { getAdapter(it) }?.getItem(position)?.takeUnless{it.isDirectMessage}?.status?.let { status ->
            val source = status.retweetedStatus ?: status
            when (BasicSettings.longTapAction) {
                "talk" ->
                    if (source.inReplyToStatusId > 0) {
                        TalkFragment().apply {
                            arguments = Bundle().apply { putSerializable("status", source) }
                        }.show(activity.supportFragmentManager, "dialog")
                        true
                    } else false
                "quote" -> {
                    ActionUtil.doQuote(source, activity)
                    true
                }
                "show_around" -> {
                    AroundFragment().apply {
                        arguments = Bundle().apply { putSerializable("status", source) }
                    }.show(activity.supportFragmentManager, "dialog")
                    true
                }
                "share_url" -> {
                    activity.startActivity(Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://twitter.com/" + status.user.screenName
                                + "/status/" + status.id.toString())
                    })
                    true
                }
                "reply_all" -> {
                    ActionUtil.doReplyAll(source, activity)
                    true
                }
                else -> false
            }
        } ?: false
    }

    open fun getAdapter(adapterView: AdapterView<*>): TwitterAdapter {
        return adapterView.adapter as TwitterAdapter
    }
}