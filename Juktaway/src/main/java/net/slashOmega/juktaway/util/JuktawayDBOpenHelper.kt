package net.slashOmega.juktaway.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.model.UserIconManager
import org.jetbrains.anko.db.*

class JuktawayDBOpenHelper(c: Context): ManagedSQLiteOpenHelper(c, "justaway.db", null, 1) {
    companion object {
        private var instance :JuktawayDBOpenHelper? = null

        fun getInstance() = instance ?: JuktawayDBOpenHelper(JuktawayApplication.app)

        fun <T> dbUse(f: SQLiteDatabase.() -> T): T {
            return getInstance().use { f() }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}