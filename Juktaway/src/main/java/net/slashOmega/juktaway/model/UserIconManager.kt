package net.slashOmega.juktaway.model

import android.widget.ImageView
import jp.nephy.penicillin.models.CommonUser
import jp.nephy.penicillin.models.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import twitter4j.TwitterException

/**
 * Created on 2018/11/01.
 */
object UserIconManager {
    private const val tableName = "userIcon"

    init { dbUse {
        createTable(tableName, true,
                "userId" to INTEGER + PRIMARY_KEY,
                "iconUrl" to TEXT + NOT_NULL,
                "name" to TEXT + NOT_NULL)
    }}

    fun ImageView.displayUserIcon(user: CommonUser) {displayUserIcon(user, this)}

    fun ImageView.displayUserIcon(userId: Long) { displayUserIcon(userId, this)}

    fun displayUserIcon(user: CommonUser, view: ImageView) {
        val url = user.profileImageUrlWithVariantSize(  when (BasicSettings.userIconSize) {
            BasicSettings.UserIconSize.LARGE -> CommonUser.ProfileImageSize.Bigger
            BasicSettings.UserIconSize.NORMAL -> CommonUser.ProfileImageSize.Normal
            BasicSettings.UserIconSize.SMALL -> CommonUser.ProfileImageSize.Mini
            else -> return
        } )
        ImageUtil.displayImage(url, view, BasicSettings.userIconRoundedOn)
    }

    fun displayUserIcon(userId: Long, view: ImageView) {
        val url = dbUse {
            select(tableName, "iconUrl")
                    .whereArgs("(userId) = {id}", "id" to userId)
                    .parseSingle(StringParser)
        }
        ImageUtil.displayRoundedImage(url, view)
    }

    fun getName(userId: Long): String = dbUse {
        select(tableName, "name")
                .whereArgs("(userId = {id})", "id" to userId)
                .parseSingle(StringParser)
    }

    fun addUserIconMap(user: CommonUser) {
        dbUse {
            insert(tableName, "userId" to user.id, "iconUrl" to user.profileImageUrl, "name" to user.name)
        }
    }

    fun warmUpUserIconMap() {
        if (AccessTokenManager.getAccessToken() != null) {
            GlobalScope.launch {
                dbUse {
                    val data = select(tableName, "userId").parseList(LongParser)
                    if (data.isNotEmpty()) try {
                        val users = TwitterManager.twitter.lookupUsers(*data.toLongArray())
                        for (u in users) {
                            update(tableName, "iconUrl" to u.biggerProfileImageURL, "name" to u.name)
                                    .whereArgs("(userId = {userId})", "userId" to u.id)
                                    .exec()
                        }
                    } catch (e: TwitterException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}