package net.slashOmega.juktaway

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Window
import android.view.WindowManager
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.activity_status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.AlertDialogEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil
import org.jetbrains.anko.intentFor

/**
 * Created on 2018/08/29.
 */
class StatusActivity: FragmentActivity() {
    private lateinit var mAdapter: StatusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        if (intent.getBooleanExtra("notification", false)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
        val statusId = if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            if (uri == null || uri.path == null) return
            when {
                uri.path!!.contains("photo") -> {
                    startActivity(Intent(this, ScaleImageActivity::class.java).apply {
                        putExtra("url", uri.toString())
                    })
                    finish()
                    return
                }
                uri.path!!.contains("video") -> {
                    startActivity(intentFor<VideoActivity>("statusUrl" to uri.toString()))
                    finish()
                    return
                }
                else -> java.lang.Long.parseLong(uri.lastPathSegment!!)
            }
        } else intent.getLongExtra("id", -1L)

        setContentView(R.layout.activity_status)

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(list)

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = StatusAdapter(this)
        with (list) {
            adapter = mAdapter
            onItemClickListener = StatusClickListener(this@StatusActivity)
            onItemLongClickListener = StatusLongClickListener(this@StatusActivity)
        }
        if (statusId > 0) {
            MessageUtil.showProgressDialog(this, getString(R.string.progress_loading))
            load(statusId)
        } else {
            intent.getStringExtra("status")?.toJsonObject()?.parse<Status>()?.let {
                mAdapter.add(Row.newStatus(it))
                val inReplyToStatusId = it.inReplyToStatusId
                if (inReplyToStatusId != null) {
                    MessageUtil.showProgressDialog(this, getString(R.string.progress_loading))
                    load(inReplyToStatusId)
                }
            }
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

    fun onEventMainThread(event: AlertDialogEvent) {
        event.dialogFragment.show(supportFragmentManager, "dialog")
    }

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        GlobalScope.launch(Dispatchers.Main) { mAdapter.removeStatus(event.statusId!!) }
    }

    private fun load(idParam: Long) {
        var statusId = idParam.takeIf { it > 0 }
        GlobalScope.launch(Dispatchers.Main) {
            while (statusId != null) {
                val status = runCatching { currentClient.statuses.show(statusId!!).await().result }.getOrNull()
                MessageUtil.dismissProgressDialog()

                if(status == null) {
                    MessageUtil.showToast(R.string.toast_load_data_failure)
                    return@launch
                }

                mAdapter.addSuspend(Row.newStatus(status))
                mAdapter.notifyDataSetChanged()
                statusId = status.inReplyToStatusId
            }
        }
    }
}