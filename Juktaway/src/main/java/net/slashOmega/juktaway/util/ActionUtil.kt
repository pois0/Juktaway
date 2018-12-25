package net.slashOmega.juktaway.util

import android.content.Context
import android.content.Intent

import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.MainActivity
import net.slashOmega.juktaway.PostActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.action.OpenEditorEvent
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.task.DestroyDirectMessageTask
import net.slashOmega.juktaway.task.DestroyStatusTask
import net.slashOmega.juktaway.task.FavoriteTask
import net.slashOmega.juktaway.task.RetweetTask
import net.slashOmega.juktaway.task.UnFavoriteTask
import net.slashOmega.juktaway.task.UnRetweetTask
import net.slashOmega.juktaway.twitter.currentIdentifier
import twitter4j.DirectMessage

object ActionUtil {
    fun doFavorite(statusId: Long) { FavoriteTask(statusId).execute() }

    fun doDestroyFavorite(statusId: Long) { UnFavoriteTask(statusId).execute() }

    fun doDestroyStatus(statusId: Long) { DestroyStatusTask(statusId).execute() }

    fun doRetweet(statusId: Long) { RetweetTask(statusId).execute() }

    fun doDestroyRetweet(status: twitter4j.Status) {
        val retweet = status.retweetedStatus
        if (status.user.id == currentIdentifier.userId && retweet != null) {
            // 自分がRTしたStatus
            UnRetweetTask(retweet.id, status.id).execute()
        } else {
            // 他人のStatusで、それを自分がRTしている

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

            if (statusId != null && statusId == 0L) {
                // 処理中は 0
                MessageUtil.showToast(R.string.toast_destroy_retweet_progress)
            } else {
                if (statusId != null && statusId > 0 && retweetedStatusId > 0) {
                    UnRetweetTask(retweetedStatusId, statusId).execute()
                }
            }
        }
    }

    fun doReply(status: twitter4j.Status, context: Context) {
        val mentions = status.userMentionEntities
        val text = "@" + (if (status.user.id == currentIdentifier.userId && mentions.size == 1) mentions[0].screenName
                else status.user.screenName) + " "
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, text.length, null))
        } else {
            context.startActivity(Intent(context, PostActivity::class.java).apply {
                putExtra("status", text)
                putExtra("selection", text.length)
                putExtra("inReplyToStatus", status)
            })
        }
    }

    fun doReplyAll(status: twitter4j.Status, context: Context) {
        val userId = currentIdentifier.userId
        var text = ""
        var selectionStart = 0
        if (status.user.id != userId) {
            text = "@" + status.user.screenName + " "
            selectionStart = text.length
        }
        for (mention in status.userMentionEntities) {
            if (status.user.id == mention.id || userId == mention.id) continue
            text += ("@" + mention.screenName + " ")
            if (selectionStart == 0) selectionStart = text.length
        }
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, selectionStart, text.length))
        } else {
            context.startActivity(Intent(context, PostActivity::class.java).apply {
                putExtra("status", text)
                putExtra("selection", selectionStart)
                putExtra("selection_stop", text.length)
                putExtra("inReplyToStatus", status)
            })
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

    fun doQuote(status: twitter4j.Status, context: Context) {
        val text = (" https://twitter.com/"
                + status.user.screenName
                + "/status/" + status.id.toString())
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, null, null))
        } else {
            context.startActivity(Intent(context, PostActivity::class.java).apply {
                putExtra("status", text)
                putExtra("inReplyToStatus", status)
            })
        }
    }
}
