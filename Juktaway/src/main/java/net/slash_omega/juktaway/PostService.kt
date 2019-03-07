package net.slash_omega.juktaway

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import kotlinx.coroutines.*
import net.slash_omega.juktaway.settings.PostStockSettings.addDraft
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.util.showToast
import net.slash_omega.juktaway.util.updateStatus
import org.jetbrains.anko.toast
import java.io.File
import kotlin.coroutines.CoroutineContext


private const val channelId = "juktaway_posting_notification"

class PostService: Service(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val manager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        manager.apply {
            if (getNotificationChannel(channelId) == null) {
                createNotificationChannel(NotificationChannel(channelId, "Juktaway",
                        NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Juktaway PostService"
                })
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        launch(Dispatchers.Default) {
            val text = intent.getStringExtra("text") ?: ""
            val replyStatusId = intent.getLongExtra("replyStatusId", -1L).takeIf { it < 0 }
            val imagePathList = intent.getSerializableExtra("imagePathList") as? List<File> ?: emptyList()
            val identifier = intent.getSerializableExtra("identifier") as Identifier
            val e = runCatching {
                identifier.updateStatus(text, replyStatusId, imagePathList)
            }.run { getOrNull() ?: exceptionOrNull() }

            val message = if (e != null) {
                addDraft(text)
                if (e is PenicillinException && e.error == TwitterErrorMessage.StatusIsADuplicate) {
                    R.string.toast_update_status_already
                } else R.string.toast_update_status_failure
            } else {
                R.string.update_status_success
            }

            stopForeground(Service.STOP_FOREGROUND_REMOVE)

            println("toasted")
            manager.notify(1, NotificationCompat.Builder(this@PostService, channelId)
                    .setContentTitle("Juktaway Posting Service")
                    .setContentText(getString(message))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
            )
            println("notify")
        }

        startForeground(1, NotificationCompat.Builder(this, channelId)
                .setContentTitle("Juktaway Posting Service")
                .setContentText("Posting a tweet...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(0, 0, true)
                .build()
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job.cancelChildren()
        super.onDestroy()
    }
}