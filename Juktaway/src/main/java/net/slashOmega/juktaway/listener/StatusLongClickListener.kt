package net.slashOmega.juktaway.listener

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import jp.nephy.jsonkt.toJsonString
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.fragment.AroundFragment
import net.slashOmega.juktaway.fragment.TalkFragment
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ActionUtil
import net.slashOmega.juktaway.util.uri

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
                    if (source.inReplyToStatusId != null) {
                        TalkFragment().apply {
                            arguments = Bundle().apply { putString("status", source.toJsonString()) }
                        }.show(activity.supportFragmentManager, "dialog")
                        true
                    } else false
                "quote" -> {
                    ActionUtil.doQuote(source, activity)
                    true
                }
                "show_around" -> {
                    AroundFragment().apply {
                        arguments = Bundle().apply { putString("status", source.toJsonString()) }
                    }.show(activity.supportFragmentManager, "dialog")
                    true
                }
                "share_url" -> {
                    activity.startActivity(Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, status.uri)
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

    open fun getAdapter(adapterView: AdapterView<*>): StatusAdapter {
        return adapterView.adapter as StatusAdapter
    }
}