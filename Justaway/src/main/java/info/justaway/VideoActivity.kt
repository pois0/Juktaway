package info.justaway

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.Window
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
import info.justaway.util.StatusUtil
import kotlinx.android.synthetic.main.activity_video.*
import twitter4j.Status
import java.lang.ref.WeakReference
import java.util.regex.Pattern

/**
 * Created on 2018/08/22.
 */
class VideoActivity: FragmentActivity() {
    companion object {
        val pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/video/(\\d+)/?.*")!!

        private class ShowStatus(context: VideoActivity): AsyncTask<Long, Void, Status>() {
            val ref = WeakReference(context)

            override fun doInBackground(vararg params: Long?): twitter4j.Status? {
                return params[0]?.let { try {
                    TwitterManager.getTwitter().showStatus(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }

            override fun onPostExecute(result: twitter4j.Status?) {
                result?.run {
                    StatusUtil.getVideoUrl(this)?.takeIf { it.isNotEmpty() }?.let {
                        ref.get()?.run { setVideoURI(it) }
                    }
                }
            }
        }
    }

    var musicWasPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_video)

        if (intent.extras == null) {
            MessageUtil.showToast("Missing Bundle in Intent")
            finish()
            return
        }

        val statusUrl = intent.extras.getString("statusUrl")
        if (statusUrl != null && statusUrl.isNotEmpty()) {
            pattern.matcher(statusUrl)?.let {
                if (it.find())
                    ShowStatus(this).execute(it.group(1).toLong())
                return
            }
        }

        val videoUrl = intent.extras.getString("videoUrl")

        if (videoUrl != null) {
            setVideoURI(videoUrl)
        } else {
            MessageUtil.showToast("Missing videoUrl in Bundle")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onDestroy() {
        if (musicWasPlaying)
            sendBroadcast(Intent("com.android.music.musicservicecommand")
                    .apply { putExtra("command", "play") })
        super.onDestroy()
    }

    fun setVideoURI(url: String) {
        musicWasPlaying = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive

        guruguru.visibility = View.VISIBLE
        player.setOnTouchListener { _, _ ->
            finish()
            false
        }
        player.setOnPreparedListener { guruguru.visibility = View.GONE }
        player.setOnCompletionListener {
            player.seekTo(0)
            player.start()
        }
        player.setVideoURI(Uri.parse(url))
        player.start()
    }
}