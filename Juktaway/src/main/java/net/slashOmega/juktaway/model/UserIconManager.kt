//package net.slashOmega.juktaway.model
//
//import android.content.Context
//import android.view.View
//import android.widget.ImageView
//import com.google.gson.Gson
//import net.slashOmega.juktaway.JuktawayApplication
//import net.slashOmega.juktaway.settings.BasicSettings
//import net.slashOmega.juktaway.util.ImageUtil
//import twitter4j.User
//import java.util.HashMap
//
///**
// * Created on 2018/10/18.
// */
//object UserIconManager {
//    private const val PREF_NAME_USER_ICON_MAP = "user_icon_map"
//    private const val PREF_KEY_USER_ICON_MAP = "data/v2"
//    private var sUserIconMap = HashMap<Long, String>()
//
//    private const val PREF_KEY_USER_NAME_MAP = "data/name"
//    private var sUserNameMap = HashMap<Long, String>()
//
//    private val sharedPreferences by lazy {
//        JuktawayApplication.app.getSharedPreferences(PREF_NAME_USER_ICON_MAP, Context.MODE_PRIVATE)
//    }
//
//    fun displayUserIcon(user: User, view: ImageView) {
//        when (BasicSettings.userIconSize) {
//            "bigger" -> user.biggerProfileImageURL
//            "normal" -> user.profileImageURL
//            "mini" -> user.miniProfileImageURL
//            else -> {
//                view.visibility = View.GONE
//                null
//            }
//        }?.let { url -> ImageUtil.displayImage(url, view, BasicSettings.userIconRoundedOn) }
//    }
//
//    fun getName(userId: Long) = sUserNameMap[userId]?: ""
//
//    fun displayUserIcon(userId: Long, view: ImageView) {
//        sUserIconMap[userId]?.let { ImageUtil.displayImage(it, view, true) }
//                ?: run { view.setImageDrawable(null) }
//    }
//
//    fun warmUpUserIconMap() {
//        val accessTokens = AccessTokenManager.getAccessTokens()
//        if (accessTokens == null || accessTokens.isEmpty()) return
//
//        val gson = Gson()
//        // it will die
//        sharedPreferences.getString(PREF_KEY_USER_ICON_MAP, null)?.let {
//            sUserIconMap = gson.fromJson(it, sUserIconMap.javaClass)
//        }
//
//        sharedPreferences.getString(PREF_KEY_USER_NAME_MAP)?.let {
//
//        }
//    }
//}