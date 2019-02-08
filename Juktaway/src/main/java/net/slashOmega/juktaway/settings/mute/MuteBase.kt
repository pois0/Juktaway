package net.slashOmega.juktaway.settings.mute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.settings.Stock
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper
import org.jetbrains.anko.db.LongParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select

/**
 * Created on 2018/11/24.
 */

abstract class MuteBase<T>: Stock<T>() {
    protected val dbId = "id"

    protected fun addUniqueRecord(tableName: String, column: String, value: Any) {
        GlobalScope.launch(Dispatchers.Default) {
            JuktawayDBOpenHelper.dbUse {
                if (select(tableName, dbId)
                                .whereArgs("$column = {data}", "data" to value)
                                .parseList(LongParser)
                                .isNullOrEmpty()) {
                    insert(tableName, column to value)
                }
            }
            Mute.clearMutedIds()
        }
    }
}