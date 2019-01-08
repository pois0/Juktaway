package net.slashOmega.juktaway.twitter

import de.greenrobot.event.EventBus
import jp.nephy.penicillin.PenicillinClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.event.action.AccountChangeEvent
import net.slashOmega.juktaway.twitter.Core.consumerParser
import net.slashOmega.juktaway.twitter.Core.identifierParser
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
        select(Core.identifierTable, "consumerId", "at", "ats", "userId", "screenName")
                .parseList(identifierParser)
    }

val consumerList
    get() = dbUse {
        select(Core.consumerTable, "id", "name", "ck", "cs")
                .parseList(consumerParser)
    }

val isIdentifierSet
    get() = identifierList.isNotEmpty()

inline fun <T> withClient(block: PenicillinClient.() -> T) = currentClient.use(block)

object Core {
    internal val identifierParser = classParser<Identifier>()
    internal val consumerParser = classParser<Consumer>()
    internal const val identifierTable = "identifierTable"
    internal const val consumerTable = "consumerTable"
    private var lastIdentifierAts by SharedPreference("twittercore", "lastIdentifier", "")

    fun initialize() {
        dbUse {
            createTable(identifierTable, true,
                    "id" to INTEGER + PRIMARY_KEY,
                    "consumerId" to INTEGER + NOT_NULL,
                    "at" to TEXT + NOT_NULL + UNIQUE,
                    "ats" to TEXT + NOT_NULL + UNIQUE,
                    "userId" to INTEGER + NOT_NULL,
                    "screenName" to TEXT + NOT_NULL)
            createTable(consumerTable, true,
                    "id" to INTEGER + PRIMARY_KEY,
                    "name" to TEXT + NOT_NULL + UNIQUE,
                    "ck" to TEXT + NOT_NULL + UNIQUE,
                    "cs" to TEXT + NOT_NULL + UNIQUE)
        }

        if (isIdentifierSet) {
            GlobalScope.launch(Dispatchers.Default) {
                currentIdentifier = dbUse {
                    select(identifierTable, "consumerId", "at", "ats", "userId", "screenName")
                            .whereArgs("ats = {ats}", "ats" to lastIdentifierAts)
                            .parseSingle(identifierParser)
                }

                currentClient = currentIdentifier.toClient()
            }
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
                    select(identifierTable, "id")
                            .whereArgs("(userId = {userId}) and (consumerId = {consumerId})",
                                    "userId" to set.userId,
                                    "consumerId" to set.consumerId)
                            .parseSingle(LongParser)
                }.onFailure {
                    insert(identifierTable,
                            "consumerId" to set.consumerId,
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

    suspend fun addConsumer(consumer: Consumer) = withContext(Dispatchers.Default) {
            dbUse {
                runCatching {
                    select(consumerTable, "id")
                            .whereArgs("(cs = {cs}) or (name = {name})",
                                    "cs" to consumer.cs,
                                    "name" to consumer.name)
                            .parseSingle(LongParser)
                }.onFailure {
                    insert(consumerTable,
                            "name" to consumer.name,
                            "ck" to consumer.ck,
                            "cs" to consumer.cs)
                }.isFailure
            }
        }

    suspend fun removeIdentifier(identifier: Identifier) {
        withContext(Dispatchers.Default) {
            dbUse {
                delete(identifierTable, "at = {at}", "at" to identifier.at)
            }
        }
    }

    suspend fun removeConsumer(consumer: Consumer) {
        withContext(Dispatchers.Default) {
            dbUse {
                delete(identifierTable, "consumerId = {consumerId}", "consumerId" to consumer.id)
                delete(identifierTable, "id = {id}", "id" to consumer.id)
            }
        }
    }

    suspend fun getConsumer(name: String) = withContext(Dispatchers.Default) {
            dbUse {
                select(consumerTable, "id", "name", "ck", "cs")
                        .whereArgs("name = {name}", "name" to name)
                        .parseSingle(consumerParser)
            }
        }
}

data class Identifier(val consumerId: String, val at: String, val ats: String, val userId: Long, val screenName: String): Serializable {
    override fun hashCode(): Int = at.hashCode()
    override fun equals(other: Any?): Boolean = other is Identifier && this.at == other.at
    suspend fun toClient(): PenicillinClient = withContext(Dispatchers.Default) {
        val consumer = dbUse {
            select(consumerId, "id", "name", "ck", "cs")
                    .whereArgs("id = {id}", "id" to consumerId)
                    .parseSingle(consumerParser)
        }
        PenicillinClient {
            account {
                application(consumer.ck, consumer.cs)
                token(at, ats)
            }
            dispatcher { coroutineContext = Dispatchers.Default }

            maxRetries = 3
            retry(1, TimeUnit.SECONDS)
        }
    }

    suspend inline fun <T> asClient(block: PenicillinClient.() -> T) = toClient().use(block)
}

data class Consumer(val id: Long, val name: String, val ck: String, val cs: String) {
    override fun hashCode() = cs.hashCode()
    override fun equals(other: Any?) = (other as? Consumer)?.cs == cs
}