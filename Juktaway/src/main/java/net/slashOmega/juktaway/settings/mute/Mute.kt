package net.slashOmega.juktaway.settings.mute

import android.support.v4.util.LongSparseArray
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.util.StatusUtil
import twitter4j.Status

/**
 * Created on 2018/11/09.
 */
object Mute {
    private val mutedIds = LongSparseArray<Boolean>()
    private var userMute = UserMute.getAllItems().map { it.first }
    private var sourceMute = SourceMute.getAllItems()
    private var wordMute = WordMute.getAllItems()

    internal fun clearMutedIds() {
        mutedIds.clear()
        userMute = UserMute.getAllItems().map { it.first }
        sourceMute = SourceMute.getAllItems()
        wordMute = WordMute.getAllItems()
    }

    fun isMute(row: Row): Boolean = row.takeIf { it.isStatus }?.status?.let { isMute(it) } ?: false

    fun isMute(status: Status): Boolean = mutedIds[status.id] ?: run {
        val source = status.retweetedStatus ?: status
        status.user.id in userMute ||
                status.userMentionEntities.map { it.id }.any { it in userMute } ||
                status.retweetedStatus?.user?.id in userMute ||
                source.source in sourceMute ||
                wordMute.any { source.text.contains(it) }
    }.also { mutedIds.put(status.id, it) }

    operator fun contains(status: Status) = isMute(status)

    operator fun contains(row: Row) = isMute(row)
}
