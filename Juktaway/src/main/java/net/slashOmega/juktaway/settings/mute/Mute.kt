package net.slashOmega.juktaway.settings.mute

import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.Status
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.util.StatusUtil

/**
 * Created on 2018/11/09.
 */
object Mute {
    fun isMute(row: Row): Boolean = row.takeIf { it.isStatus }?.let { isMute(it.status!!) } ?: false

    fun filterAll(statuses: List<Status>): List<Status> {
        val userMute = UserMute.getAllItems().map { it.first }
        val sourceMute = SourceMute.getAllItems()
        val wordMute = WordMute.getAllItems()
        return statuses.filter { status ->
            val source = status.retweetedStatus ?: status
            status.user.id !in userMute &&
            status.entities.userMentions.map { it.id }.any { userMute.contains(it) }.not() &&
            status.retweetedStatus?.user?.id !in userMute &&
            source.via.name !in sourceMute &&
            wordMute.contains(source.text).not()
        }
    }

    fun isMute(status: Status): Boolean {
        if (status.user in UserMute) return true
        for (m in status.entities.userMentions) {
            if (m.id in UserMute) return true
        }
        val rt = status.retweetedStatus
        val sourceStatus = rt ?: status
        if (rt != null && rt.user in UserMute) return true
        if (StatusUtil.getClientName(sourceStatus.via.name) in SourceMute) return true
        val text = sourceStatus.text
        for (word in WordMute.getAllItems()) {
            if (text.contains(word)) return true
        }
        return false
    }

    operator fun contains(status: Status) = isMute(status)

    operator fun contains(row: Row) = isMute(row)
}
