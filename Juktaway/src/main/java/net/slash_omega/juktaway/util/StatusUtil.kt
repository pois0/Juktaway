package net.slash_omega.juktaway.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentIdentifier
import java.util.*
import java.util.regex.Pattern

val Status.videoUrl: String
    get() {
        extendedEntities?.run {
            for (entity in media) {
                return entity.videoInfo?.variants?.filter { it.bitrate != null }
                        ?.sortedBy { it.bitrate!! }
                        ?.let {
                            it.runCatching { get(preferences.display.videoQuality.rank) }
                                    .getOrDefault(it.firstOrNull())
                        }?.url ?: continue
            }
        }
        return ""
    }

val Status.imageUrls: List<String>
    get() {
        val imageUrls = ArrayList<String>()
        entities.urls.map { it.expandedUrl }.forEach { url ->
            for (set in ImageUrlTransformer.list) {
                val matcher = set.pattern.matcher(url)
                if (matcher.find()) {
                    imageUrls.add(set.urlGenerator(url, matcher))
                    continue
                }
            }
        }

        entities.media.takeNotEmpty()?.map { it.mediaUrl }?.forEach { imageUrls.add(it) }

        return imageUrls
    }

val Status.expandedText: String
    get () = text
            .let { entities.urls.fold(it) { acc, url -> acc.replace(url.url, url.expandedUrl) } }
            .let { entities.media.fold(it) { acc, media -> acc.replace(media.url, media.expandedUrl) } }

fun TextView.setTextFromStatus(status: Status) {
    movementMethod = LinkMovementMethod.getInstance()
    val str = status.text
    val sb = SpannableStringBuilder().apply { append(str) }

    val urlMatcher = StatusUtil.URL_PATTERN.matcher(str)
    while (urlMatcher.find()) {
        sb.setSpan(URLSpan(urlMatcher.group()), urlMatcher.start(), urlMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    status.entities.urls.forEach { urlEntity ->
        sb.setSpan(URLSpan(urlEntity.expandedUrl))
    }


    val mentionMatcher = StatusUtil.MENTION_PATTERN.matcher(str)
    while (mentionMatcher.find()) {
        us = UnderlineSpan()
        sb.setSpan(us, mentionMatcher.start(), mentionMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    val hashtagMatcher = StatusUtil.HASHTAG_PATTERN.matcher(str)
    while (hashtagMatcher.find()) {
        us = UnderlineSpan()
        sb.setSpan(us, hashtagMatcher.start(), hashtagMatcher.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    text = sb
}

object StatusUtil {
    private val URL_PATTERN = Pattern.compile("(http://|https://)[\\w.\\-/:#?=&;%~+]+")
    private val MENTION_PATTERN = Pattern.compile("@[a-zA-Z0-9_]+")
    @Suppress("SpellCheckingInspection")
    private val HASHTAG_PATTERN = Pattern.compile("#\\S+")

    /**
     * 自分宛てのメンションかどうかを判定する
     *
     * @param status ツイート
     * @return true ... 自分宛てのメンション
     */
    fun isMentionForMe(status: Status): Boolean = currentIdentifier.userId.let { userId ->
        status.inReplyToUserId == userId || status.entities.userMentions.any { it.id == userId }
    }

    /**
     * 短縮URLを表示用URLに置換する
     *
     * @param status ツイート
     * @return 短縮URLを展開したツイート本文
     */
    fun getExpandedText(status: Status): String
            = status.text
                    .let { status.entities.urls.fold(it) { acc, url -> acc.replace(url.url, url.expandedUrl) } }
                    .let { status.entities.media.fold(it) { acc, media -> acc.replace(media.url, media.expandedUrl) } }

    /**
     * ツイートに含まれる画像のURLをすべて取得する
     *
     * @param status ツイート
     * @return 画像のURL
     */
    fun getImageUrls(status: Status): List<String> {
        val imageUrls = status.extendedEntities?.media.takeNotEmpty()?.map { it.mediaUrl }?.toMutableList() ?: mutableListOf()
        status.entities.urls.map { it.expandedUrl }.forEach { url ->
            for (set in ImageUrlTransformer.list) {
                val matcher = set.pattern.matcher(url)
                if (matcher.find()) {
                    imageUrls.add(set.urlGenerator(url, matcher))
                    break
                }
            }
        }

        return imageUrls
    }

    fun generateUnderline(str: String): SpannableStringBuilder {
        // URL、メンション、ハッシュタグ が含まれていたら下線を付ける
        val sb = SpannableStringBuilder().apply { append(str) }
        var us: UnderlineSpan

        val urlMatcher = URL_PATTERN.matcher(str)
        while (urlMatcher.find()) {
            sb.setSpan(URLSpan(urlMatcher.group()), urlMatcher.start(), urlMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }


        val mentionMatcher = MENTION_PATTERN.matcher(str)
        while (mentionMatcher.find()) {
            us = UnderlineSpan()
            sb.setSpan(us, mentionMatcher.start(), mentionMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val hashtagMatcher = HASHTAG_PATTERN.matcher(str)
        while (hashtagMatcher.find()) {
            us = UnderlineSpan()
            sb.setSpan(us, hashtagMatcher.start(), hashtagMatcher.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        return sb
    }
}
