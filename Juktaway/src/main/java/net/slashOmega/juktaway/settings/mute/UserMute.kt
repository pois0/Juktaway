package net.slashOmega.juktaway.settings.mute

import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.db.createTable
import twitter4j.User

/**
 * Created on 2018/11/09.
 */
object UserMute: Mute<Long>() {
    private const val tableName = "tableName"
    private const val dbUser = "user"

    init { dbUse {
        createTable(tableName, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbUser to INTEGER + NOT_NULL)
    }}

    override fun plusAssign(t: Long) {
        addUniqueRecord(tableName, dbUser, t)
    }

    operator fun plusAssign(u: User) {
        plusAssign(u.id)
    }

    override fun minusAssign(t: Long) {
        dbUse {
            delete(tableName, "$dbUser = {id}", "id" to t)
        }
    }

    operator fun minusAssign(u: User) {
        minusAssign(u.id)
    }

    override fun getIds(t: Long): List<Long> = dbUse {
        select(tableName, dbId)
                .whereArgs("$dbUser = {id}", "id" to t)
                .parseList(LongParser)
    }

    operator fun contains(u: User) = contains(u.id)
}