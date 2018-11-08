package net.slashOmega.juktaway.settings

import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.db.createTable
import twitter4j.User

/**
 * Created on 2018/11/08.
 */


object MuteSettings {
    private const val wordTable = "wordTable"
    private const val dbWord = "word"



    abstract class Mute<T> {
        abstract operator fun plusAssign(t: T)

        abstract operator fun minusAssign(t: T)

        abstract fun getId(t: T): List<Long>

        operator fun contains(t: T) = getId(t).isNotEmpty().not()

        fun add(t: T) { plusAssign(t) }

        fun remove(t: T) { minusAssign(t) }
    }





    private val mutedWords = mutableListOf<String>()



    private fun addUniqueRecord(tableName: String, column: String, value: Any) {
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