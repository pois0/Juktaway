package net.slashOmega.juktaway

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.RemoteInput
import com.nostra13.universalimageloader.core.ImageLoader
import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.event.model.NotificationEvent
import net.slashOmega.juktaway.model.AccessTokenManager

/**
 * Created on 2018/08/29.
 */
class NotificationService: Service() {
    companion object {
        const val EXTRA_VOICE_REPLY = "extra_voice_reply"
        var mStarted: Boolean = false

        fun start() {
            if (mStarted) return
            val app = JustawayApplication.app
            app.startService(Intent().apply { setClass(app, NotificationService::class.java) })
            mStarted = true
        }

        fun stop() {
            if (!mStarted) {
                return
            }
            val app = JustawayApplication.app
            app.stopService(Intent().apply { setClass(app, NotificationService::class.java) })
            mStarted = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    fun onEvent(event: NotificationEvent) {
        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val application = JustawayApplication.app

        val userId = AccessTokenManager.getUserId()

        val row = event.row
        val status = row.status
        val retweet = status?.retweetedStatus

        val url: String
        val title: String
        var text: String
        val ticker: String
        val smallIcon: Int
        val id: Long
        if (row.isDirectMessage && row.message!!.sender.id != userId) {
            if (!preferences.getBoolean("notification_message_on", true)) {
                return
            }
            url = row.message!!.sender.biggerProfileImageURL
            title = row.message!!.sender.screenName
            text = row.message!!.text
            ticker = text
            smallIcon = R.drawable.ic_notification_mail
            id = row.message!!.id
        } else if (status != null && row.isFavorite) {
            if (!preferences.getBoolean("notification_favorite_on", true)) {
                return
            }
            url = row.source!!.biggerProfileImageURL
            title = row.source!!.screenName
            text = getString(R.string.notification_favorite) + status.text
            ticker = title + getString(R.string.notification_favorite_ticker) + status.text
            smallIcon = R.drawable.ic_notification_star
            id = status.id
        } else if (status != null && status.inReplyToUserId == userId) {
            if (!preferences.getBoolean("notification_reply_on", true)) {
                return
            }
            url = status.user.biggerProfileImageURL
            title = status.user.screenName
            text = status.text
            ticker = text
            smallIcon = R.drawable.ic_notification_at
            id = status.id
        } else if (retweet != null && retweet.user.id == userId) {
            if (!preferences.getBoolean("notification_retweet_on", true)) {
                return
            }
            url = status.user.biggerProfileImageURL
            title = status.user.screenName
            text = getString(R.string.notification_retweet) + status.text
            ticker = title + getString(R.string.notification_retweet_ticker) + status.text
            smallIcon = R.drawable.ic_notification_rt
            id = status.id
        } else {
            return
        }

        val resources = application.resources
        val width = resources.getDimension(android.R.dimen.notification_large_icon_width).toInt() / 3 * 2
        val height = resources.getDimension(android.R.dimen.notification_large_icon_height).toInt() / 3 * 2

        var icon = ImageLoader.getInstance().loadImageSync(url)
        icon = Bitmap.createScaledBitmap(icon, width, height, true)

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(mainPendingIntent)
                .setSmallIcon(smallIcon)
                .setLargeIcon(icon)
                .setTicker(ticker)
                .setAutoCancel(true)
                .setGroup(getString(R.string.app_name))
                .setGroupSummary(true)
                .setWhen(System.currentTimeMillis())

        val vibrate = preferences.getBoolean("notification_vibrate_on", true)
        val sound = preferences.getBoolean("notification_sound_on", true)
        if (vibrate && sound) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND)
        } else if (vibrate) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE)
        } else if (sound) {
            builder.setDefaults(Notification.DEFAULT_SOUND)
        }

        if (status != null && status.inReplyToUserId == userId) {
            val statusIntent = Intent(this, StatusActivity::class.java)
            statusIntent.putExtra("status", status)
            statusIntent.putExtra("notification", true)

            val statusAction = NotificationCompat.Action(R.drawable.ic_notification_twitter,
                    getString(R.string.menu_open),
                    PendingIntent.getActivity(this, 1, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            builder.addAction(statusAction)

            val replyIntent = Intent(this, PostActivity::class.java)
            replyIntent.putExtra("inReplyToStatus", status)
            replyIntent.putExtra("notification", true)
            val mentions = status.userMentionEntities
            text = "@" + if (status.user.id == userId && mentions.size == 1) mentions[0].screenName else status.user.screenName+ " "
            replyIntent.putExtra("status", text)
            replyIntent.putExtra("selection", text.length)
            replyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
                    .setLabel(getResources().getString(R.string.context_menu_reply))
                    .build()

            val wearReplyAction = NotificationCompat.Action.Builder(R.drawable.ic_notification_at,
                    getString(R.string.context_menu_reply),
                    PendingIntent.getActivity(this, 1, replyIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                    .addRemoteInput(remoteInput)
                    .build()
            builder.addAction(wearReplyAction)

            val favoriteIntent = Intent(this, FavoriteActivity::class.java)
            favoriteIntent.putExtra("statusId", status.id)
            favoriteIntent.putExtra("notification", true)

            val wearFavoriteAction = NotificationCompat.Action.Builder(R.drawable.ic_notification_star,
                    getString(R.string.context_menu_create_favorite),
                    PendingIntent.getActivity(this, 1, favoriteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .build()
            builder.addAction(wearFavoriteAction)

            builder.extend(NotificationCompat.WearableExtender().addAction(wearReplyAction).addAction(wearFavoriteAction).addAction(statusAction))
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((if (id > 0) id else 1).toInt(), builder.build())
    }
}