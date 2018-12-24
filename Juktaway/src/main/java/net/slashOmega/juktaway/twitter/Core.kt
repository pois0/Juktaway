package net.slashOmega.juktaway.twitter

import de.greenrobot.event.EventBus
import jp.nephy.penicillin.PenicillinClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.event.action.AccountChangeEvent
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import org.jetbrains.anko.db.*
import java.lang.Exception
import java.util.concurrent.TimeUnit

/**
 * Created on 2018/12/23.
 */

var currentClient: PenicillinClient? = null
    private set

inline fun <T> withClient(block: PenicillinClient.() -> T) = currentClient?.use(block)

object Core {
    data class TokensSet(val cs: String, val ck: String, val at: String, val ats: String, val userId: Long, val screenName: String)
    private val accessSetParser = classParser<TokensSet>()
    private const val tokensTable = "accounts"

    init {
        dbUse {
            createTable(tokensTable, true,
                    "id" to INTEGER + PRIMARY_KEY,
                    "cs" to TEXT + NOT_NULL,
                    "ck" to TEXT + NOT_NULL,
                    "at" to TEXT + NOT_NULL,
                    "ats" to TEXT + NOT_NULL,
                    "userId" to INTEGER + NOT_NULL,
                    "screenName" to TEXT + NOT_NULL)
        }
    }

    suspend fun switchToken(id: Long) {
        val acc = try {
            dbUse {
                select(tokensTable, "cs", "ck", "at", "ats", "userId", "screenName")
                        .whereArgs("id = {data}", "data" to id)
                        .parseList(accessSetParser)
            }[0]
        } catch (e: Exception) {
            return
        }
        currentClient = PenicillinClient {
            account {
                application(acc.cs, acc.ck)
                token(acc.at, acc.ats)
            }

            maxRetries = 3
            retry(1, TimeUnit.SECONDS)
        }
        EventBus.getDefault().post(AccountChangeEvent())
    }

    suspend fun addToken(set: TokensSet, switchClient: Boolean = true) {
        val id = withContext(Dispatchers.Default) {
            dbUse {
                runCatching {
                    select(tokensTable, "id")
                            .whereArgs("(userId = {userId}) and (cs = {cs})",
                                    "userId" to set.userId,
                                    "cs" to set.cs)
                            .parseSingle(LongParser)
                }.getOrNull() ?: run {
                    insert(tokensTable,
                            "cs" to set.cs,
                            "ck" to set.ck,
                            "at" to set.at,
                            "ats" to set.ats,
                            "userId" to set.userId,
                            "screenName" to set.screenName)

                    select(tokensTable, "id")
                            .whereArgs("(userId = {userId}) and (cs = {cs})",
                                    "userId" to set.userId,
                                    "cs" to set.cs)
                            .parseSingle(LongParser)
                }
            }
        }

        if (switchClient) switchToken(id)
    }

    suspend fun removeToken(id: Long) {
        dbUse {
            delete(tokensTable, "id = {id}", "id" to id)
        }
    }
}