package net.slashOmega.juktaway.settings

import android.content.Context
import android.content.SharedPreferences
import net.slashOmega.juktaway.JuktawayApplication

object BasicSettings {

    private const val PREF_NAME_SETTINGS = "settings"
    var fontSize: Int = 0
        private set
    lateinit var longTapAction: String
        private set
    lateinit var themeName: String
        private set
    lateinit var displayAccountName: DisplayAccountName
        private set
    var userIconRoundedOn: Boolean = false
        private set
    var displayThumbnailOn: Boolean = false
        private set
    var fastScrollOn: Boolean = false
        private set
    var talkOrderNewest: Boolean = false
        private set
    lateinit var userIconSize: UserIconSize
        private set
    var pageCount: Int = 0
        private set

    private const val STREAMING_MODE = "streamingMode"
    private var mStreamingMode: Boolean = false

    private const val QUICK_MODE = "quickMode"

    private val sharedPreferences: SharedPreferences
        get() = JuktawayApplication.app
                .getSharedPreferences(PREF_NAME_SETTINGS, Context.MODE_PRIVATE)

    val quickMode: Boolean
        get() = sharedPreferences.getBoolean(QUICK_MODE, false)

    private val notificationOn: Boolean
        get() = sharedPreferences.getBoolean("notification_on", true)

    var streamingMode: Boolean
        get() = mStreamingMode
        set(streamingMode) {
            sharedPreferences.edit().run {
                putBoolean(STREAMING_MODE, streamingMode)
                apply()
            }
            mStreamingMode = streamingMode
        }

    val keepScreenOn: Boolean
        get() = sharedPreferences.getBoolean("keep_screen_on", true)

    enum class DisplayAccountName(val string: String) {
        SCREEN_NAME("SCREEN_NAME"),
        DISPLAY_NAME("DISPLAY_NAME"),
        NONE("NONE")
    }

    enum class UserIconSize(val value: String) {
        LARGE("large"), NORMAL("normal"), SMALL("small"), NONE("none");
        companion object {
            fun fromString(str: String) = UserIconSize.values().find { it.value == str } ?: throw IllegalArgumentException(str)
        }
    }


    fun setQuickMod(quickMode: Boolean) {
        sharedPreferences.edit().run {
            putBoolean(QUICK_MODE, quickMode)
            apply()
        }
    }

    fun init() {
        val preferences = sharedPreferences
        fontSize = preferences.getString("font_size", "12")?.toInt() ?: 13
        longTapAction = preferences.getString("long_tap", "nothing") ?: "nothing"
        themeName = preferences.getString("themeName", "black") ?: "black"
        userIconRoundedOn = preferences.getBoolean("user_icon_rounded_on", true)
        userIconSize = UserIconSize.fromString(preferences.getString("user_icon_size", "large") ?: "large")
        displayThumbnailOn = preferences.getBoolean("display_thumbnail_on", true)
        pageCount = preferences.getString("page_count", "200")?.toInt() ?: 0
        mStreamingMode = sharedPreferences.getBoolean(STREAMING_MODE, false)
        fastScrollOn = preferences.getBoolean("fast_scroll_on", true)
        talkOrderNewest = preferences.getBoolean("talk_order_newest", false)
        displayAccountName = DisplayAccountName.valueOf(preferences.getString("display_account_name", "screen_name")!!.toUpperCase())
    }
}
