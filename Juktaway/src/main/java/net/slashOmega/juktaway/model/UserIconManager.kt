package net.slashOmega.juktaway.model

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import com.google.gson.Gson
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.settings.BasicSettings.UserIconSize.*
import twitter4j.ResponseList
import twitter4j.TwitterException
import twitter4j.User
import java.util.HashMap

/**
 * Created on 2018/10/18.
 */
object UserIconManager {
    class LookUpUsersTask: AsyncTask<Long, Void, ResponseList<User>>() {
        override fun doInBackground(vararg params: Long?): ResponseList<User>? {
            val ge = mutableListOf<Long>()
            for (e in params) { e?.let { ge.add(it) } ?: return null }

            return try {
                TwitterManager.getTwitter().lookupUsers(*ge.toLongArray())
            } catch (e: TwitterException) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: ResponseList<User>?) {
            if (result == null) return
            sUserIconMap.clear()
            sUserNameMap.clear()
            for (user in result) {
                sUserIconMap[user.id] = user.biggerProfileImageURL
                sUserNameMap[user.id] = user.name
            }
            val gson = Gson()
            sharedPreferences.edit().apply {
                clear()
                putString(PREF_KEY_USER_ICON_MAP, gson.toJson(sUserIconMap))
                putString(PREF_KEY_USER_NAME_MAP, gson.toJson(sUserNameMap))
            }.apply()
        }
    }

    private const val PREF_NAME_USER_ICON_MAP = "user_icon_map"
    private const val PREF_KEY_USER_ICON_MAP = "data/v2"
    private var sUserIconMap = HashMap<Long, String>()

    private const val PREF_KEY_USER_NAME_MAP = "data/name"
    private var sUserNameMap = HashMap<Long, String>()

    private val sharedPreferences by lazy {
        JuktawayApplication.app.getSharedPreferences(PREF_NAME_USER_ICON_MAP, Context.MODE_PRIVATE)
    }

    fun displayUserIcon(user: User, view: ImageView) {
        val url = user.run { when (BasicSettings.userIconSize) {
            LARGE -> biggerProfileImageURL
            NORMAL -> profileImageURL
            SMALL -> miniProfileImageURL
            else -> return
        }}
        ImageUtil.displayImage(url, view, BasicSettings.userIconRoundedOn)
    }

    fun getName(userId: Long) = sUserNameMap[userId] ?: ""

    fun displayUserIcon(userId: Long, view: ImageView) {
        sUserIconMap[userId]?.let { ImageUtil.displayRoundedImage(it, view) }
                ?: view.setImageDrawable(null)
    }

    fun addUserIconMap(user: User) {
        val gson = Gson()
        sharedPreferences.getString(PREF_KEY_USER_ICON_MAP, null)?.let {
            sUserIconMap = gson.fromJson(it, sUserIconMap.javaClass)
        }
        sharedPreferences.getString(PREF_KEY_USER_NAME_MAP, null)?.let {
            sUserNameMap = gson.fromJson(it, sUserNameMap.javaClass)
        }
        sUserIconMap[user.id] = user.biggerProfileImageURL
        sUserNameMap[user.id] = user.name
        sharedPreferences.edit().apply {
            clear()
            putString(PREF_KEY_USER_ICON_MAP, gson.toJson(sUserIconMap))
            putString(PREF_KEY_USER_NAME_MAP, gson.toJson(sUserNameMap))
        }.apply()
    }

    fun warmUpUserIconMap() {
        val accessTokens = AccessTokenManager.getAccessTokens()
        if (accessTokens.isEmpty()) return
        val gson = Gson()
        sharedPreferences.getString(PREF_KEY_USER_ICON_MAP, null)?.let {
            Log.d("nyo", it)
            sUserIconMap = gson.fromJson(it, sUserIconMap.javaClass)
        }
        sharedPreferences.getString(PREF_KEY_USER_NAME_MAP, null)?.let {
            sUserNameMap = gson.fromJson(it, sUserNameMap.javaClass)
        }
        LookUpUsersTask().execute(*(accessTokens.map { it.userId }).toTypedArray())
    }
}