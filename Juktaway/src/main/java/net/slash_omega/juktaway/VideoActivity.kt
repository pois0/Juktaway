package net.slash_omega.juktaway

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import jp.nephy.penicillin.endpoints.statuses
import jp.nephy.penicillin.endpoints.statuses.show
import jp.nephy.penicillin.extensions.await
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.*
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.tryAndTraceGet
import org.jetbrains.anko.toast
import java.util.regex.Pattern

private val pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/video/(\\d+)/?.*")!!

class VideoActivity: DividedFragmentActivity() {
    private var musicWasPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_video)

        val extra = intent.extras

        if (extra == null) {
            toast("Missing Bundle in Intent")
            finish()
            return
        }

        val statusUrl = extra.getString("statusUrl")
        if (statusUrl.isNullOrEmpty().not()) {
            pattern.matcher(statusUrl)?.let { m ->
                if (m.find()) {
                    launch {
                        // TODO 画質を選べるように
                        val status = tryAndTraceGet {
                            currentClient.statuses.show(m.group(1).toLong()).await()
                        }?.result ?: return@launch
                        status.extendedEntities?.media?.first { it.type == "video" }?.videoInfo?.variants?.get(1)?.url
                                ?.let { setVideoURI(it) }
                    }
                }
                return
            }
        }

        extra.getString("videoUrl")?.let {
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