package net.slashOmega.juktaway.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.widget.ImageView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import twitter4j.TwitterException
import twitter4j.User

/**
 * Created on 2018/11/01.
 */
object UserIconManager {
    private const val tableName = "userIcon"

    fun dbInit(db: SQLiteDatabase?) {
        db?.createTable(tableName, true,
                "userId" to INTEGER + PRIMARY_KEY + UNIQUE,
                "iconUrl" to TEXT + NOT_NULL,
                "name" to TEXT + NOT_NULL)
    }

    fun displayUserIcon(user: User, view: ImageView) {
        val url = user.run { when (BasicSettings.userIconSize) {
            BasicSettings.UserIconSize.LARGE -> biggerProfileImageURL
            BasicSettings.UserIconSize.NORMAL -> profileImageURL
            BasicSettings.UserIconSize.SMALL -> miniProfileImageURL
            else -> return
        }}
        ImageUtil.displayImage(url, view, BasicSettings.userIconRoundedOn)
    }

    fun getName(userId: Long): String = dbUse {
        select(tableName, "name")
                .whereArgs("(userId = {id})", "id" to userId)
                .parseSingle(StringParser)
    }

    fun displayUserIcon(userId: Long, view: ImageView) {
        val url = dbUse {
                select(tableName, "iconUrl")
                        .whereArgs("(userId) = {id}", "id" to userId)
                        .parseSingle(StringParser)
            }
        ImageUtil.displayRoundedImage(url, view)
    }

    fun addUserIconMap(user: User) {
        dbUse {
            insert(tableName, "userId" to user.id, "iconUrl" to user.biggerProfileImageURL, "name" to user.name)
        }
    }

    fun warmUpUserIconMap() {
        GlobalScope.launch {
            dbUse {
                val data = select(tableName, "userId").parseList(LongParser)
                if (data.isNotEmpty()) try {
                    val users = TwitterManager.getTwitter().lookupUsers(*data.toLongArray())
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