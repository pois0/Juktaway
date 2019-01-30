package net.slash_omega.juktaway.settings.mute

import net.slash_omega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*

/**
 * Created on 2018/11/09.
 */

object SourceMute: MuteBase<String>() {
    private const val dbSource = "source"
    private const val tableName = "sourceTable"

    init { dbUse {
        createTable(tableName, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbSource to TEXT + NOT_NULL)
    }}

    override fun minusAssign(t: String) {
        dbUse {
            delete(tableName, "$dbSource = {source}", "source" to t)
        }
    }

    override fun plusAssign(t: String) {
        addUniqueRecord(tableName, dbSource, t)
        Mute.clearMutedIds()
    }

    override fun getIds(t: String): List<Long> = dbUse {
        select(tableName, dbId)
                .whereArgs("$dbSource = {s}", "s" to t)
                .parseList(LongParser)
    }

    override fun getAllItems(): List<String> = dbUse {
        select(tableName, dbSource)
                .parseList(StringParser)
    }
}