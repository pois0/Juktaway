package net.slashOmega.juktaway.twitter

import de.greenrobot.event.EventBus
import jp.nephy.penicillin.PenicillinClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.event.action.AccountChangeEvent
import net.slashOmega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import net.slashOmega.juktaway.util.SharedPreference
import org.jetbrains.anko.db.*
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * Created on 2018/12/23.
 */

lateinit var currentClient: PenicillinClient
    private set

lateinit var currentIdentifier: Identifier
    private set

val identifierList
    get() = dbUse {
        select(Core.tokensTable, "ck", "cs", "at", "ats", "userId", "screenName")
                .parseList(Core.identifierParser)
    }

val isIdentifierSet
    get() = identifierList.isNotEmpty()

inline fun <T> withClient(block: PenicillinClient.() -> T) = currentClient.use(block)

object Core {
    internal val identifierParser = classParser<Identifier>()
    internal const val tokensTable = "accounts"
    private var lastIdentifierAts by SharedPreference("twittercore", "lastIdentifier", "")

    fun initialize() {
        dbUse {
            createTable(tokensTable, true,
                    "id" to INTEGER + PRIMARY_KEY,
                    "ck" to TEXT + NOT_NULL,
                    "cs" to TEXT + NOT_NULL,
                    "at" to TEXT + NOT_NULL + UNIQUE,
                    "ats" to TEXT + NOT_NULL + UNIQUE,
                    "userId" to INTEGER + NOT_NULL,
                    "screenName" to TEXT + NOT_NULL)
        }

        if (isIdentifierSet) {
            currentIdentifier = dbUse {
                select(tokensTable, "ck", "cs", "at", "ats", "userId", "screenName")
                        .whereArgs("ats = {ats}", "ats" to lastIdentifierAts)
                        .parseSingle(identifierParser)
            }

            currentClient = currentIdentifier.toClient()
        }
    }

    suspend fun switchToken(acc: Identifier) {
        withContext(Dispatchers.Default) {
            if (identifierList.size > 1) currentClient.close()
            currentIdentifier = acc
            currentClient = acc.toClient()
            lastIdentifierAts = currentIdentifier.ats
            EventBus.getDefault().post(AccountChangeEvent())
        }
    }

    suspend fun addToken(set: Identifier, switchClient: Boolean = true): Boolean {
        val res = withContext(Dispatchers.Default) {
            dbUse {
                runCatching {
                    select(tokensTable, "id")
                            .whereArgs("(userId = {userId}) and (cs = {cs})",
                                    "userId" to set.userId,
                                    "cs" to set.cs)
                            .parseSingle(LongParser)
                }.onFailure {
                    insert(tokensTable,
                            "ck" to set.ck,
                            "cs" to set.cs,
                            "at" to set.at,
                            "ats" to set.ats,
                            "userId" to set.userId,
                            "screenName" to set.screenName)
                }.isFailure
            }
        }

        if (switchClient) switchToken(set)
        return res
    }

    suspend fun removeIdentifier(identifier: Identifier) {
        withContext(Dispatchers.Default) {
            dbUse {
                delete(tokensTable, "at = {at}", "at" to identifier.at)
            }
        }
    }
}

data class Identifier(val ck: String, val cs: String, val at: String, val ats: String, val userId: Long, val screenName: String): Serializable {
    override fun hashCode(): Int = at.hashCode()
    override fun equals(other: Any?): Boolean = other is Identifier && this.at == other.at
    fun toClient() = PenicillinClient {
        account {
            application(ck, cs)
            token(at, ats)
        }
        dispatcher { coroutineContext = Dispatchers.Default }

        maxRetries = 3
        retry(1, TimeUnit.SECONDS)
    }

    inline fun <T> asClient(block: PenicillinClient.() -> T) = toClient().use(block)
}