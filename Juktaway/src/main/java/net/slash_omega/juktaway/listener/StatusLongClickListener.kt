package net.slash_omega.juktaway.listener

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.AdapterView
import jp.nephy.jsonkt.toJsonString
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.fragment.AroundFragment
import net.slash_omega.juktaway.fragment.TalkFragment
import net.slash_omega.juktaway.settings.Preferences.OperationPreferences.LongTapAction.*
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.util.ActionUtil
import net.slash_omega.juktaway.util.generateJsonBundle
import net.slash_omega.juktaway.util.quote
import net.slash_omega.juktaway.util.uri

/**
 * Created on 2018/08/27.
 */
open class StatusLongClickListener(activity: Activity): AdapterView.OnItemLongClickListener {
    val activity = activity as FragmentActivity


    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long)
        = adapterView?.let { getAdapter(it) }?.getItem(position)?.let { status ->
            val source = status.retweetedStatus ?: status
            when (preferences.operation.longTap) {
                TALK ->
                    if (source.inReplyToStatusId != null) {
                        TalkFragment().apply {
                            arguments = source.generateJsonBundle()
                        }.show(activity.supportFragmentManager, "dialog")
                        true
                    } else false
                QUOTE -> {
                    source.quote(activity)
                    true
                }
                SHOW_AROUND -> {
                    AroundFragment().apply {
                        arguments = source.generateJsonBundle()
                    }.show(activity.supportFragmentManager, "dialog")
                    true
                }
                SHARE_URL -> {
                    activity.startActivity(Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, status.uri)
                    })
                    true
                }
                REPLY_ALL -> {
                    ActionUtil.doReplyAll(source, activity)
                    true
                }
                else -> false
            }
        } ?: false

    open fun getAdapter(adapterView: AdapterView<*>): StatusAdapter {
        return adapterView.adapter as StatusAdapter
    }
}