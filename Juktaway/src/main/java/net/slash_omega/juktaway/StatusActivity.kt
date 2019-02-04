package net.slash_omega.juktaway

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.statuses
import jp.nephy.penicillin.endpoints.statuses.show
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.activity_status.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.model.Row
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.parseWithClient
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

/**
 * Created on 2018/08/29.
 */
class StatusActivity: DividedFragmentActivity() {
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
                    startActivity<ScaleImageActivity>("url" to uri.toString())
                    finish()
                    return
                }
                uri.path!!.contains("video") -> {
                    startActivity<VideoActivity>("statusUrl" to uri.toString())
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
        launch {
            if (statusId > 0) {
                MessageUtil.showProgressDialog(this@StatusActivity, getString(R.string.progress_loading))
                load(statusId)
            } else {
                intent.getStringExtra("status")?.toJsonObject()?.parseWithClient<Status>()?.let {
                    mAdapter.addSuspend(Row.newStatus(it))
                    val inReplyToStatusId = it.inReplyToStatusId
                    if (inReplyToStatusId != null) {
                        MessageUtil.showProgressDialog(this@StatusActivity, getString(R.string.progress_loading))
                        load(inReplyToStatusId)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
            list.visibility = View.VISIBLE
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

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        launch { mAdapter.removeStatus(event.statusId!!) }
    }

    private fun load(idParam: Long) {
        var statusId = idParam.takeIf { it > 0 }
        launch {
            while (statusId != null) {
                val status = runCatching { currentClient.statuses.show(statusId!!).await().result }.getOrNull()
                MessageUtil.dismissProgressDialog()

                if(status == null) {
                    toast(R.string.toast_load_data_failure)
                    return@launch
                }

                mAdapter.addSuspend(Row.newStatus(status))
                mAdapter.notifyDataSetChanged()
                statusId = status.inReplyToStatusId
            }
        }
    }
}