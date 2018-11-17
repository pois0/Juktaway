package net.slashOmega.juktaway.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan

import java.util.ArrayList
import java.util.regex.Pattern

import net.slashOmega.juktaway.model.AccessTokenManager
import twitter4j.Status

object StatusUtil {
    private val URL_PATTERN = Pattern.compile("(http://|https://)[\\w\\.\\-/:#\\?=&;%~\\+]+")
    private val MENTION_PATTERN = Pattern.compile("@[a-zA-Z0-9_]+")
    private val HASHTAG_PATTERN = Pattern.compile("#\\S+")

    /**
     * source(via)からクライアント名を抜き出す
     *
     * @param source [クライアント名](クライアントURL)という文字列
     * @return クライアント名
     */
    fun getClientName(source: String) = source.split("[<>]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().let {
        it[if (it.size > 1) 2 else 0]
    }

    /**
     * 自分宛てのメンションかどうかを判定する
     *
     * @param status ツイート
     * @return true ... 自分宛てのメンション
     */
    fun isMentionForMe(status: Status): Boolean {
        val userId = AccessTokenManager.getUserId()
        if (status.inReplyToUserId == userId) {
            return true
        }
        val mentions = status.userMentionEntities
        for (mention in mentions) {
            if (mention.id == userId) {
                return true
            }
        }
        return false
    }

    /**
     * 短縮URLを表示用URLに置換する
     *
     * @param status ツイート
     * @return 短縮URLを展開したツイート本文
     */
    fun getExpandedText(status: Status): String {
        var text = status.text
        for (url in status.urlEntities) {
            val m = Pattern.compile(url.url).matcher(text)
            text = m.replaceAll(url.expandedURL)
        }

        for (media in status.mediaEntities) {
            val m = Pattern.compile(media.url).matcher(text)
            text = m.replaceAll(media.expandedURL)
        }
        return text
    }

    /**
     * ツイートに含まれる画像のURLをすべて取得する
     *
     * @param status ツイート
     * @return 画像のURL
     */
    fun getImageUrls(status: Status): ArrayList<String> {
        val imageUrls = ArrayList<String>()
        status.urlEntities.map { it.expandedURL }.forEach { url ->
            for (set in ImageUrlTransformer.list) {
                val matcher = set.pattern.matcher(url)
                if (matcher.find()) {
                    imageUrls.add(set.urlGenerator(url, matcher))
                    continue
                }
            }
        }

        if (status.mediaEntities.isNotEmpty()) {
            status.mediaEntities.map { it.mediaURL }.forEach { imageUrls.add(it) }
        }

        return imageUrls
    }

    fun getVideoUrl(status: Status): String {
        for (extendedMediaEntity in status.mediaEntities) {
            for (videoVariant in extendedMediaEntity.videoVariants) {
                if (videoVariant.url.lastIndexOf("mp4") != -1) {
                    return videoVariant.url
                }
            }
        }
        return ""
    }

    fun generateUnderline(str: String): SpannableStringBuilder {
        // URL、メンション、ハッシュタグ が含まれていたら下線を付ける
        val sb = SpannableStringBuilder().apply { append(str) }
        var us: UnderlineSpan

        val urlMatcher = URL_PATTERN.matcher(str)
        while (urlMatcher.find()) {
            us = UnderlineSpan()
            sb.setSpan(us, urlMatcher.start(), urlMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
