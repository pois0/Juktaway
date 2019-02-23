package net.slash_omega.juktaway.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.slash_omega.juktaway.app
import org.jetbrains.anko.db.*

class JuktawayDBOpenHelper(c: Context): ManagedSQLiteOpenHelper(c, "justaway.db", null, 1) {
    companion object {
        private var instance :JuktawayDBOpenHelper? = null

        private fun getInstance() = instance ?: JuktawayDBOpenHelper(app)

        fun <T> dbUse(f: SQLiteDatabase.() -> T): T {
            return getInstance().use(f)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}