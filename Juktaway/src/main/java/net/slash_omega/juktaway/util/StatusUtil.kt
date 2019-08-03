package net.slash_omega.juktaway.util

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.widget.TextView
import jp.nephy.penicillin.extensions.models.firstIndex
import jp.nephy.penicillin.extensions.models.size
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.UrlEntityModel
import jp.nephy.penicillin.models.entities.StatusEntity
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentIdentifier


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
        val imageUrls = extendedEntities?.media.takeNotEmpty()?.map { it.mediaUrl }?.toMutableList() ?: mutableListOf()
        entities.urls.map { it.expandedUrl }.forEach { url ->
            for (set in ImageUrlTransformer.list) {
                val matcher = set.pattern.matcher(url)
                if (matcher.find()) {
                    imageUrls.add(set.urlGenerator(url, matcher))
                }
            }
        }

        return imageUrls
    }

fun TextView.setTextFromStatus(status: Status, context: Context) {
    val str = status.text
    val sb = SpannableStringBuilder()

    status.entities.run { userMentions + hashtags + urls + media }
            .sortedBy { it.firstIndex }
            .fold(0) { acc, entity ->
                when (entity) {
                    is StatusEntity.UserMentionEntity -> {
                        val userMention = "@" + entity.screenName
                        val start = str.indexOf(userMention, acc, true).takeUnless { it < 0 } ?: return@fold acc
                        sb.append(str.substring(acc, start))
                        sb.append(userMention, UnderlineSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        start + entity.size
                    }
                    is StatusEntity.HashtagEntity -> {
                        val hashtag = "#" + entity.text
                        val start = str.indexOf(hashtag, acc, true).takeUnless { it < 0 } ?: return@fold acc
                        sb.append(str.substring(acc, start))
                        sb.append(hashtag, UnderlineSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        start + entity.size
                    }
                    else -> {
                        entity as UrlEntityModel
                        val start = str.indexOf(entity.url, acc, true).takeUnless { it < 0 } ?: return@fold acc
                        sb.append(str.substring(acc, start))
                        sb.append(entity.expandedUrl, UnderlineSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        start + entity.url.length
                    }
                }
            }.let {
                sb.append(str.substring(it))
            }

    text = sb
}

val Status.isMentionForMe: Boolean
    get() = currentIdentifier.userId.let { userId ->
        inReplyToUserId == userId || entities.userMentions.any { it.id == userId }
    }
