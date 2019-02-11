package net.slash_omega.juktaway.model

import android.support.v4.util.LongSparseArray
import jp.nephy.penicillin.models.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.slash_omega.juktaway.util.original

/**
 * どのツイートをふぁぼ又はRTしているかを管理する
 */

internal fun Status.isFavorited() = FavRetweetManager.isFavorited(this)

internal fun Status.isRetweeted() = FavRetweetManager.isRetweeted(this)

object FavRetweetManager {
    private val favMap = LongSparseArray<Boolean>()
    private val rtMap = LongSparseArray<Boolean>()

    suspend fun clear() = withContext(Dispatchers.Default) {
        favMap.clear()
        rtMap.clear()
    }

    fun setFav(id: Long) { favMap.put(id, true) }

    fun removeFav(id: Long) { favMap.put(id, false); println("aaaaaaaaa $id") }

    fun isFavorited(status: Status) = status.original.run { favMap.get(id) ?: favorited }

    fun setRetweet(id: Long) { rtMap.put(id, true) }

    fun removeRetweet(id: Long) { rtMap.put(id, false) }

    fun isRetweeted(status: Status) = status.original.run { rtMap.get(id) ?: retweeted }
}
