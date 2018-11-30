package net.slashOmega.juktaway.settings.mute

import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.util.StatusUtil
import twitter4j.Status

/**
 * Created on 2018/11/09.
 */
object Mute {
    fun isMute(row: Row): Boolean = row.takeIf { it.isStatus }?.let { isMute(it.status!!) } ?: false

    fun isMute(status: Status): Boolean = run {
        if (status.user in UserMute) return true
        for (m in status.userMentionEntities) {
            if (m.id in UserMute) return true
        }
        val rt = status.retweetedStatus
        val sourceStatus = rt ?: status
        if (rt != null && rt.user in UserMute) return true
        if (StatusUtil.getClientName(sourceStatus.source) in SourceMute) return true
        val text = sourceStatus.text
        for (word in WordMute.getAllItems()) {
            if (text.contains(word)) return true
        }
        return false
    }

    operator fun contains(status: Status) = isMute(status)

    operator fun contains(row: Row) = isMute(row)
}
