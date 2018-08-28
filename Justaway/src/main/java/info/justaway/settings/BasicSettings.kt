package info.justaway.settings

import android.content.Context
import android.content.SharedPreferences

import info.justaway.JustawayApplication
import info.justaway.NotificationService

object BasicSettings {

    private const val PREF_NAME_SETTINGS = "settings"
    var fontSize: Int = 0
        private set
    var longTapAction: String? = null
        private set
    var themeName: String? = null
        private set
    var displayAccountName: DisplayAccountName? = null
        private set
    var userIconRoundedOn: Boolean = false
        private set
    var displayThumbnailOn: Boolean = false
        private set
    var fastScrollOn: Boolean = false
        private set
    var talkOrderNewest: Boolean = false
        private set
    var userIconSize: String? = null
        private set
    var pageCount: Int = 0
        private set

    private const val STREAMING_MODE = "streamingMode"
    private var mStreamingMode: Boolean = false

    private const val QUICK_MODE = "quickMode"

    private val sharedPreferences: SharedPreferences
        get() = JustawayApplication.app
                .getSharedPreferences(PREF_NAME_SETTINGS, Context.MODE_PRIVATE)

    val quickMode: Boolean
        get() = sharedPreferences.getBoolean(QUICK_MODE, false)

    private val notificationOn: Boolean
        get() = sharedPreferences.getBoolean("notification_on", true)

    var streamingMode: Boolean
        get() = mStreamingMode
        set(streamingMode) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(STREAMING_MODE, streamingMode)
            editor.apply()
            mStreamingMode = streamingMode
        }

    val keepScreenOn: Boolean
        get() = sharedPreferences.getBoolean("keep_screen_on", true)

    enum class DisplayAccountName(val string: String) {
        SCREEN_NAME("SCREEN_NAME"),
        DISPLAY_NAME("DISPLAY_NAME"),
        NONE("NONE")
    }

    fun setQuickMod(quickMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(QUICK_MODE, quickMode)
        editor.apply()
    }

    fun init() {
        val preferences = sharedPreferences
        fontSize = Integer.parseInt(preferences.getString("font_size", "12"))
        longTapAction = preferences.getString("long_tap", "nothing")
        themeName = preferences.getString("themeName", "black")
        userIconRoundedOn = preferences.getBoolean("user_icon_rounded_on", true)
        userIconSize = preferences.getString("user_icon_size", "bigger")
        displayThumbnailOn = preferences.getBoolean("display_thumbnail_on", true)
        pageCount = Integer.parseInt(preferences.getString("page_count", "200"))
        mStreamingMode = sharedPreferences.getBoolean(STREAMING_MODE, true)
        fastScrollOn = preferences.getBoolean("fast_scroll_on", true)
        talkOrderNewest = preferences.getBoolean("talk_order_newest", false)
        displayAccountName = DisplayAccountName.valueOf(preferences.getString("display_account_name", "screen_name")!!.toUpperCase())
    }

    fun resetNotification() {
        if (notificationOn) {
            NotificationService.start()
        } else {
            NotificationService.stop()
        }
    }
}
