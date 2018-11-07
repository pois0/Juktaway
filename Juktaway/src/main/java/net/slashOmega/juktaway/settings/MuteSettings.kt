package net.slashOmega.juktaway.settings

import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import org.jetbrains.anko.db.createTable

/**
 * Created on 2018/11/08.
 */

object MuteSettings {
    private const val wordTable = "wordTable"
    private const val sourceTable = "sourceTable"
    private const val userTable = "userTable"
    private const val dbId = "id"
    private const val dbWord = "word"
    private const val dbSource = "source"
    private const val dbUser = "user"

    private val mutedWords = mutableListOf<String>()

    init { dbUse {
        createTable(wordTable, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbWord to TEXT + NOT_NULL)
        createTable(sourceTable, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbSource to TEXT + NOT_NULL)
        createTable(userTable, true,
                dbId to INTEGER + PRIMARY_KEY,
                dbUser to INTEGER + NOT_NULL)
    }}

    private fun addUniqueRecord(tableName: String, column: String, value: String, )

    fun addSource(source: String) {
        dbUse {

        }
    }
}