package net.slash_omega.juktaway.util

import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.idObj
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.settings.preferences
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


private val DATE_DETAILED_FORMAT = SimpleDateFormat("yyyy/MM'/'dd' 'HH':'mm':'ss'.'SSS", Locale.ENGLISH)

private val DATE_CONCISE_FORMAT = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.ENGLISH)

val Status.createdAtString: String
    get() = if (preferences.display.tweet.shouldDisplayMilliSec) DATE_DETAILED_FORMAT.format(idObj.date)
            else DATE_CONCISE_FORMAT.format(createdAt.date)

val Status.relativeTime: String
    get() {
        val diff = ChronoUnit.SECONDS.between(createdAt.instant, Instant.now())
        return when {
            diff < 1 -> "now"
            diff < 60 -> diff.toString() + "s"
            diff < 3600 -> (diff / 60).toString() + "m"
            diff < 86400 -> (diff / 3600).toString() + "h"
            else -> (diff / 86400).toString() + "d"
        }
    }
