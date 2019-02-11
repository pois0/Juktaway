package net.slash_omega.juktaway.twitter

import de.greenrobot.event.EventBus
import io.ktor.client.engine.android.Android
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.OfficialClient
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.session.config.*
import jp.nephy.penicillin.endpoints.common.TweetMode.Extended
import kotlinx.coroutines.*
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.app
import net.slash_omega.juktaway.event.action.AccountChangeEvent
import net.slash_omega.juktaway.model.FavRetweetManager
import net.slash_omega.juktaway.twitter.Core.consumerParser
import net.slash_omega.juktaway.twitter.Core.consumerTable
import net.slash_omega.juktaway.twitter.Core.identifierParser
import net.slash_omega.juktaway.util.JuktawayDBOpenHelper.Companion.dbUse
import net.slash_omega.juktaway.util.SharedPreference
import org.jetbrains.anko.db.*
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * Created on 2018/12/23.
 */

lateinit var currentClient: ApiClient
    private set

lateinit var currentIdentifier: Identifier
    private set

val identifierList
    get() = runCatching {
        dbUse {
            select(Core.identifierTable, "consumerId", "at", "ats", "userId", "screenName")
                    .parseList(identifierParser)
        }
    }.getOrNull() ?: emptyList()

val consumerList
    get() = runCatching {
        dbUse {
            select(Core.consumerTable, "id", "name", "ck", "cs")
                    .parseList(consumerParser)
        }
    }.getOrNull() ?: emptyList()

val isIdentifierSet
    get() = identifierList.isNotEmpty()

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
            if (consumerList.isNullOrEmpty()) {
                dbUse {
                    val iPhone = OfficialClient.OAuth1a.TwitterForiPhone
                    insert(consumerTable, "name" to "Twitter for iPhone",
                            "ck" to iPhone.consumerKey,
                            "cs" to iPhone.consumerSecret
                    )
                    insert(consumerTable, "name" to "Juktaway",
                            "ck" to app.getString(R.string.juktaway_ck),
                            "cs" to app.getString(R.string.juktaway_cs)
                    )
                }
            }
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

    suspend fun switchToken(id: Identifier) {
        withContext(Dispatchers.Default) {
            println(id.toString())
            currentIdentifier = id
            currentClient = id.toClient()
            lastIdentifierAts = id.ats
            FavRetweetManager.clear()
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

    suspend fun addConsumer(name: String, ck: String, cs: String) = withContext(Dispatchers.Default) {
            dbUse {
                insert(consumerTable, "name" to name, "ck" to ck, "cs" to cs)
            } != -1L
        }

    suspend fun removeIdentifier(identifier: Identifier) {
        withContext(Dispatchers.Default) {
            dbUse {
                delete(identifierTable, "at = {at}", "at" to identifier.at)
            }
        }
    }

    suspend fun removeConsumer(consumer: Consumer) = withContext(Dispatchers.Default) {
        consumer.ck != OfficialClient.OAuth1a.TwitterForAndroid.consumerKey
                && consumer.ck != app.getString(R.string.juktaway_ck)
                && runCatching {
                    dbUse {
                        delete(identifierTable, "consumerId = {consumerId}", "consumerId" to consumer.id)
                        delete(identifierTable, "id = {id}", "id" to consumer.id)
                    }
                }.isSuccess
    }

    suspend fun getConsumer(name: String) = withContext(Dispatchers.Default) {
            dbUse {
                select(consumerTable, "id", "name", "ck", "cs")
                        .whereArgs("name = {name}", "name" to name)
                        .parseSingle(consumerParser)
            }
        }

    suspend fun getConsumer(consumerId: Long) = withContext(Dispatchers.Default) {
        dbUse {
            runCatching {
                select(consumerTable, "id", "name", "ck", "cs")
                        .whereArgs("id = {id}", "id" to consumerId)
                        .parseSingle(consumerParser)
            }.getOrNull()
        }
    }
}

data class Identifier(val consumerId: Long, val at: String, val ats: String, val userId: Long, val screenName: String): Serializable {
    override fun hashCode(): Int = at.hashCode()
    override fun equals(other: Any?): Boolean = other is Identifier && this.at == other.at
    suspend fun toClient(): ApiClient = withContext(Dispatchers.Default) {
        val consumer = dbUse {
            select(consumerTable, "id", "name", "ck", "cs")
                    .whereArgs("id = {id}", "id" to consumerId)
                    .parseSingle(consumerParser)
        }
        PenicillinClient {
            account {
                application(consumer.ck, consumer.cs)
                token(at, ats)
            }

            httpClient(Android)

            api {
                maxRetries = 3
                retryInterval(1, TimeUnit.SECONDS)
                defaultTweetMode(Extended)
            }
        }
    }

    suspend inline fun <T> asClient(block: ApiClient.() -> T) = toClient().use(block)
}

data class Consumer(val id: Long, val name: String, val ck: String, val cs: String) {
    override fun hashCode() = cs.hashCode()
    override fun equals(other: Any?) = (other as? Consumer)?.cs == cs
}