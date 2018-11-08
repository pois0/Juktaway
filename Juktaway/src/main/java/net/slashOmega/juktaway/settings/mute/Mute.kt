package net.slashOmega.juktaway.settings.mute

import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.LongParser
import org.jetbrains.anko.db.RowParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select

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
    }

    abstract operator fun plusAssign(t: T)

    abstract operator fun minusAssign(t: T)

    abstract fun getIds(t: T): List<Long>

    abstract fun getAllItems(): List<T>

    operator fun contains(t: T) = getIds(t).isNotEmpty().not()

    fun add(t: T) { plusAssign(t) }

    fun remove(t: T) { minusAssign(t) }
}