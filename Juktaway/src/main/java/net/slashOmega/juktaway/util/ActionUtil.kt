package net.slashOmega.juktaway.util

import android.content.Context
import android.content.Intent
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.models.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.MainActivity
import net.slashOmega.juktaway.PostActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.action.OpenEditorEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.task.DestroyDirectMessageTask
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.twitter.currentIdentifier
import org.jetbrains.anko.startActivity
import twitter4j.DirectMessage


suspend fun Status.favorite() = ActionUtil.doFavorite(id)

suspend fun Status.unfavorite() = ActionUtil.doDestroyFavorite(id)

object ActionUtil {
    suspend fun updateStatus() {
        val e = runCatching {
            currentClient.status.update()
        }
    }

    suspend fun doFavorite(statusId: Long) {
        withContext(Dispatchers.Default) {
            FavRetweetManager.setFav(statusId)
            EventBus.getDefault().post(StatusActionEvent())
        }

        val e = runCatching { currentClient.favorite.create(statusId).await() }.exceptionOrNull() as? PenicillinException
        when {
            e == null -> MessageUtil.showToast(R.string.toast_destroy_favorite_success)
            e.error?.code == 139 -> MessageUtil.showToast(R.string.toast_favorite_already)
            else -> {
                FavRetweetManager.removeFav(statusId)
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_favorite_failure)
            }
        }
    }

    suspend fun doDestroyFavorite(statusId: Long) {
        withContext(Dispatchers.Default) {
            FavRetweetManager.removeFav(statusId)
            EventBus.getDefault().post(StatusActionEvent())
        }

        val e = runCatching { currentClient.favorite.destroy(statusId).await() }.exceptionOrNull() as? PenicillinException
        when {
            e == null -> MessageUtil.showToast(R.string.toast_destroy_favorite_success)
            e.error == TwitterErrorMessage.SorryThatPageDoesNotExist -> MessageUtil.showToast(R.string.toast_destroy_favorite_already)
            else -> {
                FavRetweetManager.setFav(statusId)
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_destroy_favorite_failure)
            }
        }
    }

    suspend fun doDestroyStatus(statusId: Long) {
        val res = runCatching {
            currentClient.status.delete(statusId).await()
        }.isSuccess
        if (res) {
            MessageUtil.showToast(R.string.toast_destroy_status_success)
            EventBus.getDefault().post(StreamingDestroyStatusEvent(statusId))
        } else {
            MessageUtil.showToast(R.string.toast_destroy_status_failure)
        }
    }

    suspend fun doRetweet(statusId: Long) {
        FavRetweetManager.setRtId(statusId, 0.toLong())
        EventBus.getDefault().post(StatusActionEvent())

        runCatching {
            currentClient.status.retweet(statusId).await().result
        }.onSuccess {
            FavRetweetManager.setRtId(it.retweetedStatus!!.id, it.id)
            MessageUtil.showToast(R.string.toast_retweet_success)
        }.onFailure {
            FavRetweetManager.setRtId(statusId, null)
            val e = it as PenicillinException
            if (e.error?.code == 37) MessageUtil.showToast(R.string.toast_retweet_already)
            else {
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_retweet_failure)
            }
        }
    }

    suspend fun doDestroyRetweet(status: Status) {
        val retweet = status.retweetedStatus
        val (rtId, statusId) = if (status.user.id == currentIdentifier.userId && retweet != null) {
            retweet.id to status.id
        } else {
            // 被リツイート
            var retweetedStatusId: Long = -1L

            // リツイート
            var statusId = FavRetweetManager.getRtId(status.id)
            if (statusId != null && statusId > 0) {
                // そのStatusそのものをRTしている
                retweetedStatusId = status.id
            } else if (retweet != null) {
                statusId = FavRetweetManager.getRtId(retweet.id)
                if (statusId != null && statusId > 0) {
                    // そのStatusがRTした元StatusをRTしている
                    retweetedStatusId = retweet.id
                }
            }

            if (statusId == null) return

            if (statusId == 0L) {
                // 処理中は 0
                MessageUtil.showToast(R.string.toast_destroy_retweet_progress)
                return
            } else if (statusId > 0 && retweetedStatusId > 0) {
                retweetedStatusId to statusId
            } else return
        }

        FavRetweetManager.setRtId(rtId, null)
        EventBus.getDefault().post(StatusActionEvent())

        runCatching {
            currentClient.status.delete(statusId).await()
        }.onSuccess {
            MessageUtil.showToast(R.string.toast_destroy_retweet_success)
            EventBus.getDefault().post(StreamingDestroyStatusEvent(statusId))
        }.onFailure {
            if (it !is PenicillinException) return
            if (it.error == TwitterErrorMessage.SorryThatPageDoesNotExist) {
                MessageUtil.showToast(R.string.toast_destroy_retweet_already)
            } else {
                FavRetweetManager.setRtId(rtId, statusId)
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_destroy_retweet_failure)
            }
        }
    }

    fun doReply(status: Status, context: Context) {
        val mentions = status.entities.userMentions
        val text = "@" + (if (status.user.id == currentIdentifier.userId && mentions.size == 1) mentions[0].screenName
                else status.user.screenName) + " "
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, text.length, null))
        } else {
            context.startActivity(Intent(context, PostActivity::class.java).apply {
                putExtra("status", text)
                putExtra("selection", text.length)
                putExtra("inReplyToStatus", status.toJsonString())
            })
        }
    }

    fun doReplyAll(status: Status, context: Context) {
        val userId = currentIdentifier.userId
        var text = ""
        var selectionStart = 0
        if (status.user.id != userId) {
            text = "@" + status.user.screenName + " "
            selectionStart = text.length
        }
        for (mention in status.entities.userMentions) {
            if (status.user.id == mention.id || userId == mention.id) continue
            text += ("@" + mention.screenName + " ")
            if (selectionStart == 0) selectionStart = text.length
        }
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, selectionStart, text.length))
        } else {
            context.startActivity<PostActivity>("status" to text,
                    "selection" to selectionStart,
                    "selection_stop" to text.length,
                    "inReplyToStatus" to status.toJsonString())
        }
    }

    fun doReplyDirectMessage(directMessage: DirectMessage, context: Context) {
        val text = "D " + (if (currentIdentifier.userId == directMessage.sender.id) directMessage.recipient.screenName
                else directMessage.sender.screenName) + " "
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, null, text.length, null))
        } else {
            context.startActivity(Intent(context, PostActivity::class.java).apply {
                putExtra("status", text)
                putExtra("selection", text.length)
            })
        }
    }

    fun doDestroyDirectMessage(id: Long) { DestroyDirectMessageTask().execute(id) }

    fun doQuote(status: Status, context: Context) {
        val text = " https://twitter.com/${status.user.screenName}/status/${status.id}"
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, null, null))
        } else {
            context.startActivity<PostActivity>("status" to text, "inReplyToStatus" to status.toJsonString())
        }
    }
}
