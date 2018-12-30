package net.slashOmega.juktaway.model

import android.widget.ImageView
import jp.nephy.penicillin.models.CommonUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.twitter.isIdentifierSet
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import net.slashOmega.juktaway.util.tryAndTraceGet
import org.jetbrains.anko.db.*

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

    fun ImageView.displayUserIcon(user: CommonUser) { displayUserIcon(user, this) }

    fun ImageView.displayUserIcon(userId: Long) {
        val url = dbUse {
        select(tableName, "iconUrl")
                .whereArgs("(userId) = {id}", "id" to userId)
                .parseSingle(StringParser)
        }
        ImageUtil.displayRoundedImage(url, this)
    }

    fun displayUserIcon(user: CommonUser, view: ImageView) {
        val url = user.profileImageUrlWithVariantSize(  when (BasicSettings.userIconSize) {
            BasicSettings.UserIconSize.LARGE -> CommonUser.ProfileImageSize.Bigger
            BasicSettings.UserIconSize.NORMAL -> CommonUser.ProfileImageSize.Normal
            BasicSettings.UserIconSize.SMALL -> CommonUser.ProfileImageSize.Mini
            else -> return
        } )
        ImageUtil.displayImage(url, view, BasicSettings.userIconRoundedOn)
    }

    suspend fun getName(userId: Long): String = withContext(Dispatchers.Default) {
        dbUse {
            select(tableName, "name")
                    .whereArgs("(userId = {id})", "id" to userId)
                    .parseSingle(StringParser)
        }
    }

    suspend fun addUserIconMap(user: CommonUser) {
        withContext(Dispatchers.Default) {
            dbUse {
                insert(tableName, "userId" to user.id, "iconUrl" to user.profileImageUrl, "name" to user.name)
            }
        }
    }

    fun warmUpUserIconMap() {
        if (!isIdentifierSet) return
        GlobalScope.launch {
            val data = dbUse { select(tableName, "userId").parseList(LongParser) }
            if (data.isNullOrEmpty()) return@launch
            tryAndTraceGet {
                val users = currentClient.user.lookupByIds(data).await()
                dbUse {
                    users.forEach { u ->
                        update(tableName,
                                "iconUrl" to u.profileImageUrlHttpsWithVariantSize(CommonUser.ProfileImageSize.Bigger),
                                "name" to u.name)
                                .whereArgs("(userId = {userId})", "userId" to u.id)
                                .exec()
                    }
                }
            }
        }
    }
}