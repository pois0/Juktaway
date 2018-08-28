package info.justaway.model

import android.support.v4.util.LongSparseArray

/**
 * どのツイートをふぁぼ又はRTしているかを管理する
 */
object FavRetweetManager {
    private val mIsFavMap = LongSparseArray<Boolean>()
    private val mRtIdMap = LongSparseArray<Long>()

    fun setFav(id: Long?) { mIsFavMap.put(id!!, true) }

    fun removeFav(id: Long?) { mIsFavMap.remove(id!!) }

    fun isFav(status: twitter4j.Status): Boolean
            = mIsFavMap.get(status.id, false)
            || status.retweetedStatus?.let { mIsFavMap.get(it.id, false) } ?: false

    fun setRtId(sourceId: Long?, retweetId: Long?) {
        if (retweetId != null) {
            mRtIdMap.put(sourceId!!, retweetId)
        } else {
            mRtIdMap.remove(sourceId!!)
        }
    }

    fun getRtId(status: twitter4j.Status): Long?
            = mRtIdMap.get(status.id) ?: status.retweetedStatus?.let { mRtIdMap.get(it.id) }

    fun getRtId(id: Long): Long? = mRtIdMap.get(id)
}
