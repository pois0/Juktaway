package net.slash_omega.juktaway.util

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.widget.TextView
import jp.nephy.penicillin.extensions.models.firstIndex
import jp.nephy.penicillin.extensions.models.lastIndex
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.entities.MediaEntity
import jp.nephy.penicillin.models.entities.StatusEntity
import jp.nephy.penicillin.models.entities.URLEntity
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentIdentifier
import java.util.*


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

        entities.media.takeNotEmpty()?.map { it.mediaUrl }?.let{ imageUrls.addAll(it) }

        return imageUrls
    }

fun TextView.setTextFromStatus(status: Status, context: Context) {
    val str = status.text
    var sb = SpannableStringBuilder().apply { append(str) }

    status.entities.let { e -> e.userMentions + e.media + e.hashtags + e.urls }
            .sortedBy { it.firstIndex }
            .fold(0) { gap, entity ->
                val start = entity.firstIndex + gap
                val end = entity.lastIndex + gap
                when (entity) {
                    is StatusEntity.UserMentionEntity -> {
                        sb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        gap
                    }
                    is StatusEntity.HashtagEntity -> {
                        sb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        gap
                    }
                    is MediaEntity -> {
                        sb = sb.replace(start, end, entity.expandedUrl)
                        sb.setSpan(UnderlineSpan(), start, start + entity.expandedUrl.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        gap + entity.expandedUrl.length - entity.url.length
                    }
                    is URLEntity -> {
                        sb = sb.replace(start, end, entity.expandedUrl)
                        sb.setSpan(UnderlineSpan(), start, start + entity.expandedUrl.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        gap + entity.expandedUrl.length - entity.url.length
                    }
                    else -> gap
                }
            }

    text = sb
}

val Status.isMentionForMe: Boolean
    get() = currentIdentifier.userId.let { userId ->
        inReplyToUserId == userId || entities.userMentions.any { it.id == userId }
    }

object StatusUtil {
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
}
