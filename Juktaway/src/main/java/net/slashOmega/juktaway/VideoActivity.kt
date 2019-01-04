package net.slashOmega.juktaway

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.tryAndTraceGet
import java.util.regex.Pattern

/**
 * Created on 2018/08/22.
 */
class VideoActivity: FragmentActivity() {
    companion object {
        val pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/video/(\\d+)/?.*")!!
    }

    private var musicWasPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_video)

        if (intent.extras == null) {
            MessageUtil.showToast("Missing Bundle in Intent")
            finish()
            return
        }

        val statusUrl = intent?.extras?.getString("statusUrl")
        if (statusUrl.isNullOrEmpty().not()) {
            pattern.matcher(statusUrl)?.let { m ->
                if (m.find()) {
                    GlobalScope.launch(Dispatchers.Main) {
                        // TODO 画質を選べるように
                        val status = tryAndTraceGet {
                            currentClient.status.show(m.group(1).toLong()).await()
                        }?.result ?: return@launch
                        status.extendedEntities?.media?.first { it.type == "video" }?.videoInfo?.variants?.get(1)?.url
                                ?.let { setVideoURI(it) }
                    }
                }
                return
            }
        }

        intent?.extras?.getString("videoUrl")?.let {
            setVideoURI(it)
        } ?: run {
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

    private fun setVideoURI(url: String) {
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