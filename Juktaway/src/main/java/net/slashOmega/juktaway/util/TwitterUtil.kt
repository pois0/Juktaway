package net.slashOmega.juktaway.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.os.BatteryManager
import android.os.Build

import java.util.regex.Matcher
import java.util.regex.Pattern

object TwitterUtil {

    private val URL_PATTERN = Pattern.compile("(http://|https://)[\\w\\.\\-/:#\\?=&;%~\\+]+")
    private const val BATTERY_ROW_LEVEL = 14

    /**
     * ツイートの文字数を数えます
     *
     * @param str ツイート文字列
     * @return 文字数
     */
    fun count(str: String): Int {
        var length = str.codePointCount(0, str.length)
        val max = if (str.indexOf("D ") == 0) 10000 else 140

        // 短縮URLを考慮
        val matcher = URL_PATTERN.matcher(str)
        while (matcher.find()) {
            length = length - matcher.group().length + 23
        }

        return max - length
    }

    /**
     * 突然の死ジェネレーター
     *
     * @param text 対象のテキスト
     * @param selectStart 選択開始位置
     * @param selectEnd 選択終了位置
     * @return 突然の死
     */
    fun convertSuddenly(text: String, selectStart: Int, selectEnd: Int): String {
        // 突然の死対象のテキストを取得
        val targetText: String = (if (selectStart != selectEnd) text.substring(selectStart, selectEnd)  else text)  + "\n"

        val paint = Paint()
        var maxTextWidth = 0f
        // 対象のテキストの最大文字列幅を取得
        val lines = targetText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (paint.measureText(line) > maxTextWidth) {
                maxTextWidth = paint.measureText(line)
            }
        }

        // 上と下を作る
        var top = ""
        var under = ""
        for ( i in 0..(maxTextWidth / 12).toInt()) {
            top += "人"
        }
        for (i in 0..(maxTextWidth / 13).toInt()) {
            under += "^Y"
        }

        var suddenly = ""

        lines.map {
            var line = it
            val spaceWidth = maxTextWidth - paint.measureText(it)
            if (spaceWidth >= 12) {
                val spaceNumber = spaceWidth.toInt() / 12
                for (x in 0..spaceNumber) {
                    line += "　"
                }
                if (spaceWidth % 12 >= 6) {
                    line += "　"
                }
            }
            suddenly = "$suddenly＞ $line ＜\n"
            line
        }

        return if (selectStart != selectEnd) {
            text.substring(0, selectStart) + "＿" + top + "＿\n" + suddenly + "￣" + under + "￣" + text.substring(selectEnd)
        } else {
            "＿" + top + "＿\n" + suddenly + "￣" + under + "￣"
        }
    }

    /**
     * バッテリー情報の文字列を返す
     *
     * @param context コンテキスト
     * @return バッテリー情報
     */
    fun getBatteryStatus(context: Context): String? {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null

        val level = batteryIntent.getIntExtra("level", 0)

        return Build.MODEL + " のバッテリー残量：" + level + when (batteryIntent.getIntExtra("status", 0)) {
            BatteryManager.BATTERY_STATUS_FULL -> "% (0゜・◡・♥​​)"
            BatteryManager.BATTERY_STATUS_CHARGING -> "% 充電なう(・◡・♥​​)"
            else -> if (level <= BATTERY_ROW_LEVEL) "% (◞‸◟)"  else "% (・◡・♥​​)"
        }
    }

    fun Int.omitCount() = when {
        this >= 1000000 -> "${this / 1000000}M"
        this >= 1000 -> "${this / 1000}k"
        else -> this.toString()
    }
}
