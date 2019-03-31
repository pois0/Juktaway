package net.slash_omega.juktaway.settings

import android.content.Context
import android.content.SharedPreferences
import net.slash_omega.juktaway.app

val preferences: Preferences
    get() = sealedPreferences

private lateinit var sealedPreferences: Preferences

data class Preferences(
        val display: DisplayPreferences,
        val operation: OperationPreferences,
        val api: ApiPreferences
) {
    data class DisplayPreferences(
            val general: DisplayGeneralPreferences,
            val main: DisplayMainPreferences,
            val tweet: DisplayTweetPreferences,
            val pictureQuality: PictureQuality,
            val videoQuality: VideoQuality
    ) {
        data class DisplayGeneralPreferences(
                val fontSize: Int,
                val theme: String,
                val isFastScrollEnabled: Boolean
        )

        data class DisplayMainPreferences(
                val userName: UserName,
                var isQuickPostVisible: Boolean
        ) {
            enum class UserName {
                SCREEN_NAME, DISPLAY_NAME, NONE;

                companion object {
                    fun fromString(str: String) = when (str) {
                        "screen_name" -> SCREEN_NAME
                        "display_name" -> DISPLAY_NAME
                        else -> NONE
                    }
                }
            }
        }

        data class DisplayTweetPreferences(
                val isFavoriteButtonHeartShaped: Boolean,
                val shouldDisplayMilliSec: Boolean,
                val shouldShowAuthorIcon: Boolean,
                val isAuthorIconRounded: Boolean,
                val shouldShowThumbnail: Boolean,
                val isTalkSortedByNewest: Boolean
        )

        enum class PictureQuality(val size: String) {
            HIGH("large"), MEDIUM("medium"), LOW("small"), LOWEST("thumb");

            companion object {
                fun fromString(str: String) = when (str) {
                    "high" -> HIGH
                    "medium" -> MEDIUM
                    "low" -> LOW
                    else -> LOWEST
                }
            }
        }

        enum class VideoQuality(val rank: Int) {
            HIGH(2), MEDIUM(1), LOW(0);

            companion object {
                fun formString(str: String) = when (str) {
                    "high" -> HIGH
                    "medium" -> MEDIUM
                    else -> LOW
                }
            }
        }
    }

    data class OperationPreferences(val longTap: LongTapAction) {
        enum class LongTapAction {
            QUOTE, TALK, SHOW_AROUND, SHARE_URL, REPLY_ALL, NOTHING;

            companion object {
                fun fromString(str: String) = when (str) {
                    "quote" -> QUOTE
                    "talk" -> TALK
                    "show_around" -> SHOW_AROUND
                    "share_url" -> SHARE_URL
                    "reply_all" -> REPLY_ALL
                    else -> NOTHING
                }
            }
        }
    }

    data class ApiPreferences(val pageCount: Int)
}

object BasicSettings {
    private val firstTheme by lazy { sharedPreferences.getString("display_general_theme", "black")!! }

    private const val PREF_NAME_SETTINGS = "settings"

    private val sharedPreferences: SharedPreferences
        get() = app.getSharedPreferences(PREF_NAME_SETTINGS, Context.MODE_PRIVATE)

    fun init() {
        sealedPreferences = sharedPreferences.let { pref ->
            Preferences(
                Preferences.DisplayPreferences(
                    Preferences.DisplayPreferences.DisplayGeneralPreferences(
                        pref.getString("display_general_font_size", "12")!!.toInt(),
                        firstTheme,
                        pref.getBoolean("display_general_fast_scroll", true)
                    ),
                    Preferences.DisplayPreferences.DisplayMainPreferences(
                        Preferences.DisplayPreferences.DisplayMainPreferences.UserName.fromString(
                                pref.getString("display_main_user_name", "")!!
                        ),
                        pref.getBoolean("display_main_quick_post", true)
                    ),
                    Preferences.DisplayPreferences.DisplayTweetPreferences(
                        pref.getBoolean("display_tweet_favorite_button", false),
                        pref.getBoolean("display_tweet_millisec", true),
                        pref.getBoolean("display_tweet_show_icon", true),
                        pref.getBoolean("display_tweet_icon_shape", false),
                        pref.getBoolean("layout_tweet_thumbnail", false),
                        pref.getBoolean("layout_tweet_talk_order", false)
                    ),
                    Preferences.DisplayPreferences.PictureQuality.fromString(
                            pref.getString("display_picture_quality", "")!!
                    ),
                    Preferences.DisplayPreferences.VideoQuality.formString(
                            pref.getString("layout_video_quality", "")!!
                    )
                ),
                Preferences.OperationPreferences(
                    Preferences.OperationPreferences.LongTapAction.fromString(
                            pref.getString("operation_long_tap", "")!!
                    )
                ),
                Preferences.ApiPreferences(
                    pref.getString("api_page_count", "0")!!.toInt()
                )
            )
        }
    }
}
