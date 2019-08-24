package net.slash_omega.juktaway.util

import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.idObj
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.settings.preferences
import java.text.SimpleDateFormat
import java.util.*


private val DATE_DETAILED_FORMAT = SimpleDateFormat("yyyy/MM'/'dd' 'HH':'mm':'ss'.'SSS", Locale.ENGLISH)

private val DATE_CONCISE_FORMAT = SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.ENGLISH)

val Status.createdAtString: String
    get() = if (preferences.display.tweet.shouldDisplayMilliSec) DATE_DETAILED_FORMAT.format(idObj.date)
            else DATE_CONCISE_FORMAT.format(createdAt.date)

object TimeUtil {
    /**
     * 相対時刻取得
     *
     * @param date 日付
     * @return 相対時刻
     */
    fun getRelativeTime(date: Date): String {
        val diff = ((Date().time - date.time) / 1000).toInt()
        return when {
            diff < 1 -> "now"
            diff < 60 -> diff.toString() + "s"
            diff < 3600 -> (diff / 60).toString() + "m"
            diff < 86400 -> (diff / 3600).toString() + "h"
            else -> (diff / 86400).toString() + "d"
        }
    }
}
