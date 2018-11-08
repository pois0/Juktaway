package net.slashOmega.juktaway.settings.mute

import kotlinx.coroutines.selects.select
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.db.createTable

/**
 * Created on 2018/11/09.
 */

object WordMute: Mute<String>() {
    private const val tableName = "tableName"
    private const val dbWord = "word"

    init { dbUse {
        createTable(tableName, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbWord to TEXT + NOT_NULL)
    }}

    override fun getIds(t: String): List<Long> = dbUse {
        select(tableName, dbId)
                .whereArgs("$dbWord = {w}", "w" to t)
                .parseList(LongParser)
    }

    override fun getAllItems(): List<String> = dbUse {
        select(tableName, dbWord)
                .parseList(StringParser)
    }

    override fun plusAssign(t: String) {
        addUniqueRecord(tableName, dbWord, dbWord to t)
    }

    override fun minusAssign(t: String) {
        dbUse {
            delete(tableName, "$dbWord = {word}", "word" to t)
        }
    }
}