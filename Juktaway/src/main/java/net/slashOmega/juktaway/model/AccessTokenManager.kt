package net.slashOmega.juktaway.model

import android.content.Context
import com.google.gson.Gson
import net.slashOmega.juktaway.JuktawayApplication
import twitter4j.auth.AccessToken
import java.util.ArrayList

/**
 * Created on 2018/10/28.
 */
object AccessTokenManager {
    private const val TOKENS = "tokens"
    private const val PREF_NAME = "twitter_access_token"
    private var sAccessToken: AccessToken? = null

    private val sharedPreferences = JuktawayApplication.app
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAccessToken(): AccessToken? = sAccessToken ?: run {
        val json = sharedPreferences.getString(TOKENS, null)

        val accountSettings = Gson().fromJson(json, AccountSettings::class.java)
        if (accountSettings?.accessTokens == null) return@run null
        sAccessToken = accountSettings.accessTokens.get(accountSettings.index)
        sAccessToken
    }

    fun setAccessToken(accessToken: AccessToken) {
        sAccessToken = accessToken

        TwitterManager.twitter.oAuthAccessToken = sAccessToken
        val gson = Gson()
        val accountSettings = sharedPreferences.getString(TOKENS, null)?.let { json ->
            gson.fromJson(json, AccountSettings::class.java).apply {
                var existUser = false
                accessTokens.forEachIndexed { i, token ->
                    if (accessToken.userId == token.userId) {
                        accessTokens[i] = accessToken
                        index = i
                        existUser = true
                    }
                }
                if (!existUser) {
                    index = accessTokens.size
                    accessTokens.add(accessToken)
                }
            }
        }?: AccountSettings().apply {
            accessTokens.add(accessToken)
        }

        sharedPreferences.edit().apply{
            putString(TOKENS, gson.toJson(accountSettings))
        }.apply()
    }

    fun removeAccessToken(removeAccessToken: AccessToken) {
        val json = sharedPreferences.getString(TOKENS, null)
        val gson = Gson()

        val accountSettings = gson.fromJson<AccountSettings>(json, AccountSettings::class.java)

        /**
         * 現在設定されているAccessTokenより先に削除すべきAccessTokenがある場合indexをデクリメントする
         * これをしないと位置がずれる
         */

        accountSettings.apply {
            val currentAccessToken = accessTokens[accountSettings.index]

            for (accessToken in accessTokens) {
                if (accessToken.userId == removeAccessToken.userId) {
                    index--
                    break
                }
                if (accessToken.userId == currentAccessToken.userId) break
            }
            accessTokens.remove(removeAccessToken)
        }

        sharedPreferences.edit().apply {
            putString(TOKENS, gson.toJson(accountSettings))
        }.apply()
    }

    //Done
    fun getUserId() = sAccessToken?.userId ?: -1L

    class AccountSettings {
        internal var index: Int = 0
        internal var accessTokens: ArrayList<AccessToken> = arrayListOf()
    }
}