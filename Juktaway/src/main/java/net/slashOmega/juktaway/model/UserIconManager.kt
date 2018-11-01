package net.slashOmega.juktaway.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.widget.ImageView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ImageUtil
import org.jetbrains.anko.db.*
import twitter4j.TwitterException
import twitter4j.User

/**
 * Created on 2018/11/01.
 */
object UserIconManager {
    class UserInfoDatabaseOpenHelper(c: Context): ManagedSQLiteOpenHelper(c, "justaway.db", null, 1) {
        companion object {
            const val tableName = "userIcon"
            private var instance :UserInfoDatabaseOpenHelper? = null

            fun getInstance() = instance ?: UserInfoDatabaseOpenHelper(JuktawayApplication.app)
        }

        override fun onCreate(db: SQLiteDatabase?) {
            db?.createTable(tableName, true,
                    "userId" to INTEGER + PRIMARY_KEY + UNIQUE,
                    "iconUrl" to TEXT + NOT_NULL,
                    "name" to TEXT + NOT_NULL)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
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

    fun getName(userId: Long): String = UserInfoDatabaseOpenHelper.run { getInstance().use {
        select(tableName, "name")
                .whereArgs("(userId = {id})", "id" to userId)
                .parseSingle(StringParser)
    }}

    fun displayUserIcon(userId: Long, view: ImageView) {
        val url = UserInfoDatabaseOpenHelper.run { getInstance().use {
                select(tableName, "iconUrl")
                        .whereArgs("(userId) = {id}", "id" to userId)
                        .parseSingle(StringParser)
            }}
        ImageUtil.displayRoundedImage(url, view)
    }

    fun addUserIconMap(user: User) {
        UserInfoDatabaseOpenHelper.run { getInstance().use {
            insert(tableName, "userId" to user.id, "iconUrl" to user.biggerProfileImageURL, "name" to user.name)
        }}
    }

    fun warmUpUserIconMap() {
        GlobalScope.launch {
            UserInfoDatabaseOpenHelper.run { getInstance().use {
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
            }}
        }
    }
}