package net.slash_omega.juktaway.settings.mute

import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.Status

/**
 * Created on 2018/11/09.
 */

object Mute {
    private val mutedIds = HashMap<Long, Boolean>()
    private var userMute = UserMute.getAllItems().map { it.first }
    private var sourceMute = SourceMute.getAllItems()
    private var wordMute = WordMute.getAllItems()

    internal fun clearMutedIds() {
        mutedIds.clear()
        userMute = UserMute.getAllItems().map { it.first }
        sourceMute = SourceMute.getAllItems()
        wordMute = WordMute.getAllItems()
    }

    fun isMute(status: Status): Boolean = mutedIds[status.id] ?: run {
            val source = status.retweetedStatus ?: status
            status.user.id in userMute ||
                    status.entities.userMentions.map { it.id }.any { it in userMute } ||
                    status.retweetedStatus?.user?.id in userMute ||
                    source.via.name in sourceMute ||
                    wordMute.any { source.text.contains(it) }
        }.also { mutedIds[status.id] = it }

    operator fun contains(status: Status) = isMute(status)
}
