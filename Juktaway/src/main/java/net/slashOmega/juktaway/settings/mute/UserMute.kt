package net.slashOmega.juktaway.settings.mute

import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.db.createTable
import twitter4j.User

/**
 * Created on 2018/11/09.
 */
object UserMute: Mute<Pair<Long, String>>() {
    private const val tableName = "userTable"
    private const val dbUserId = "userId"
    private const val dbScreenName = "screenName"

    private val parser by lazy {
        rowParser { id: Long, screenName: String -> Pair(id, screenName) }
    }

    init { dbUse {
        createTable(tableName, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbUserId to INTEGER + NOT_NULL,
                dbScreenName to TEXT + NOT_NULL)
    }}

    override fun plusAssign(t: Pair<Long, String>) {
        dbUse {
            if (select(tableName, dbId)
                            .whereArgs("$dbUserId = {data}", "data" to t.first)
                            .parseList(LongParser)
                            .isNullOrEmpty()) {
                insert(tableName, dbUserId to t.first, dbScreenName to t.second)
            }
        }
    }

    operator fun plusAssign(u: User) {
        plusAssign(Pair(u.id, u.screenName))
    }

    override fun minusAssign(t: Pair<Long, String>) {
        minusAssign(t.first)
    }

    operator fun minusAssign(id: Long) {
        dbUse {
            delete(tableName, "$dbUserId = {id}", "id" to id)
        }
    }

    operator fun minusAssign(u: User) {
        minusAssign(u.id)
    }

    override fun getIds(t: Pair<Long, String>): List<Long> = getIds(t.first)

    private fun getIds(id: Long): List<Long> = dbUse {
        select(tableName, dbId)
                .whereArgs("$dbUserId = {id}", "id" to id)
                .parseList(LongParser)
    }

    operator fun contains(u: User) = contains(u.id)

    operator fun contains(id: Long) = dbUse {
        select(tableName, dbUserId)
                .whereArgs("$dbUserId = {id}", "id" to id)
                .parseList(LongParser)
    }.isNullOrEmpty().not()

    override fun getAllItems(): List<Pair<Long, String>> = dbUse {
        select(tableName, dbUserId, dbScreenName)
                .parseList(parser)
    }
}