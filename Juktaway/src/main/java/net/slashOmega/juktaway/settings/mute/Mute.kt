package net.slashOmega.juktaway.settings.mute

import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import net.slashOmega.juktaway.util.StatusUtil
import org.jetbrains.anko.db.LongParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import twitter4j.Status

/**
 * Created on 2018/11/09.
 */
abstract class Mute<T> {
    companion object {
        const val dbId = "id"

        fun addUniqueRecord(tableName: String, column: String, value: Any) {
            dbUse {
                if (select(tableName, dbId)
                                .whereArgs("$column = {data}", "data" to value)
                                .parseList(LongParser)
                                .isNullOrEmpty()) {
                    insert(tableName, column to value)
                }
            }
        }

        fun isMute(row: Row): Boolean = row.takeIf { it.isStatus }?.let { isMute(it.status!!) } ?: false

        fun isMute(status: Status): Boolean = run {
            if (status.user in UserMute) return true
            for (m in status.userMentionEntities) {
                if (m.id in UserMute) return true
            }
            val rt = status.retweetedStatus
            val sourceStatus = rt ?: status
            if (rt != null && rt.user in UserMute) return true
            if (StatusUtil.getClientName(sourceStatus.source) in SourceMute) return true
            val text = sourceStatus.text
            for (word in WordMute.getAllItems()) {
                if (text.contains(word)) return true
            }
            return false
        }

        operator fun contains(status: Status) = isMute(status)

        operator fun contains(row: Row) = isMute(row)
    }

    abstract operator fun plusAssign(t: T)

    abstract operator fun minusAssign(t: T)

    abstract fun getIds(t: T): List<Long>

    abstract fun getAllItems(): List<T>

    operator fun contains(t: T) = getIds(t).isNullOrEmpty().not()

    fun add(t: T) { plusAssign(t) }

    fun remove(t: T) { minusAssign(t) }
}