package info.justaway.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    private val DATE_FORMAT = SimpleDateFormat("yyyy/MM'/'dd' 'HH':'mm':'ss", Locale.ENGLISH)

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

    /**
     * 絶対時刻取得
     *
     * @param date 日付
     * @return 絶対時刻
     */
    fun getAbsoluteTime(date: Date): String {
        return DATE_FORMAT.format(date)
    }
}
