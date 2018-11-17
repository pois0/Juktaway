package net.slashOmega.juktaway.util

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.ScaleImageActivity
import net.slashOmega.juktaway.VideoActivity
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Status

/**
 * Created on 2018/10/25.
 */
object ImageUtil {
    private val sRoundedDisplayImageOptions by lazy {
        DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .resetViewBeforeLoading(true)
                .displayer(FadeInBitmapDisplayer(5))
                .build()
    }

    fun init() {
        val defaultOptions = DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .resetViewBeforeLoading(true)
                .build()

        ImageLoader.getInstance().init(
                ImageLoaderConfiguration.Builder(JuktawayApplication.app)
                        .defaultDisplayImageOptions(defaultOptions)
                        .build())
    }

    fun displayImage(url: String, view: ImageView, isRounded: Boolean) {
        if (isRounded) {
            displayImage(url, view)
        } else {
            displayRoundedImage(url, view)
        }
    }

    fun displayImage(url: String, view: ImageView) {
        val tag = view.tag as? String
        if (tag != null && tag == url) return
        view.tag = url
        ImageLoader.getInstance().displayImage(url, view)
    }

    fun displayRoundedImage(url: String, view: ImageView) {
        val tag = view.tag as? String
        if (tag != null && tag == url) return
        view.tag = url
        if (BasicSettings.userIconRoundedOn) {
            ImageLoader.getInstance().displayImage(url, view, sRoundedDisplayImageOptions)
        } else {
            ImageLoader.getInstance().displayImage(url, view)
        }
    }

    fun displayThumbnailImages(context: Context, group: ViewGroup, wrapperGroup: ViewGroup, play: TextView, status: Status) {
        val videoUrl = StatusUtil.getVideoUrl(status)
        StatusUtil.getImageUrls(status).takeIf { it.isNotEmpty() }?.let {imageUrls ->
            group.removeAllViews()
            imageUrls.forEachIndexed { i, url ->
                val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                group.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 240).apply {
                    if (i > 0) setMargins(0, 20, 0, 0)
                })
                displayRoundedImage(url, image)

                if (videoUrl.isEmpty()) {
                    image.setOnClickListener {
                        context.startActivity(Intent(it.context, ScaleImageActivity::class.java).apply {
                            putExtra("status", status)
                            putExtra("index", i)
                        })
                    }
                } else {
                    image.setOnClickListener {
                        context.startActivity(Intent(it.context, VideoActivity::class.java). apply {
                            putExtra("videoUrl", videoUrl)
                        })
                    }
                }
            }
            group.visibility = View.VISIBLE
            wrapperGroup.visibility = View.VISIBLE
        } ?: run {
            group.visibility = View.GONE
            wrapperGroup.visibility = View.GONE
        }
        play.visibility = if (videoUrl.isEmpty()) View.GONE else View.VISIBLE
    }
}