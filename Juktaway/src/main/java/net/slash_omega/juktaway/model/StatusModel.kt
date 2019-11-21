package net.slash_omega.juktaway.model

import androidx.databinding.ObservableBoolean
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.util.displayText
import net.slash_omega.juktaway.util.isMentionForMe
import net.slash_omega.juktaway.util.relativeTime

data class StatusModel(val status: Status) {
    val originalStatus = status.retweetedStatus ?: status
    val isMentionForMe = status.isMentionForMe
    val displayText by lazy { status.displayText }
    val relativeDateTime = status.relativeTime

    val isFavorited = ObservableBoolean(status.favorited)
    val isRetweeted = ObservableBoolean(status.retweeted)

    val favoriteCount = if (status.favoriteCount > 0) status.favoriteCount.toString() else ""
    val retweetCount = if (status.retweetCount > 0) status.retweetCount.toString() else ""

    val quoteUserDisplayName = status.quotedStatus?.user?.name.orEmpty()
    val quoteUserScreenName = status.quotedStatus?.user?.screenName.orEmpty()
    val quoteStatusDisplayText = status.quotedStatus?.displayText ?: ""
}
