package net.slashOmega.juktaway.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.widget.ImageView
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.ImageUtil
import org.jetbrains.anko.db.*
import twitter4j.User

/**
 * Created on 2018/11/01.
 */
object UserIconManagerNew {
    data class UserInfo(val userId: Long, val iconUrl: String, val name: String)

    class UserInfoDatabaseOpenHelper(c: Context): ManagedSQLiteOpenHelper(c, "userInfo.db", null, 1) {
        companion object {
            const val tableName = "userIcon"
            val parser by lazy { classParser<UserInfo>() }
            private var instance :UserInfoDatabaseOpenHelper? = null

            fun getInstance() = instance ?: UserInfoDatabaseOpenHelper(JuktawayApplication.app)
        }

        override fun onCreate(db: SQLiteDatabase?) {
            db?.createTable(tableName, true, "userId" to INTEGER + PRIMARY_KEY + UNIQUE, "iconUrl" to TEXT, "name" to TEXT)
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

    fun getName(userId: Long): Long = UserInfoDatabaseOpenHelper.run { getInstance().use {
        select(tableName, "name").whereArgs("(userId = {id})", "id" to userId).parseSingle(LongParser)
    }}
}