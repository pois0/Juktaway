package net.slash_omega.juktaway.util

import android.content.Context
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.endpoints.directMessages
import jp.nephy.penicillin.endpoints.directmessages.create
import jp.nephy.penicillin.endpoints.directmessages.delete
import jp.nephy.penicillin.endpoints.media.MediaCategory
import jp.nephy.penicillin.endpoints.media.MediaComponent
import jp.nephy.penicillin.endpoints.media.MediaType
import jp.nephy.penicillin.endpoints.statuses
import jp.nephy.penicillin.endpoints.statuses.create
import jp.nephy.penicillin.endpoints.statuses.delete
import jp.nephy.penicillin.endpoints.statuses.retweet
import jp.nephy.penicillin.endpoints.statuses.unretweet
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.endpoints.createWithMedia
import jp.nephy.penicillin.extensions.models.favorite
import jp.nephy.penicillin.extensions.models.unfavorite
import jp.nephy.penicillin.models.DirectMessage
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.MainActivity
import net.slash_omega.juktaway.PostActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.event.action.OpenEditorEvent
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyMessageEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.model.FavRetweetManager
import net.slash_omega.juktaway.settings.PostStockSettings
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.twitter.currentIdentifier
import org.jetbrains.anko.startActivity
import java.io.File

inline val Status.original
    get() = retweetedStatus ?: this

suspend inline fun Status.favorite() = original.runCatching { favorite().await() }
        .onSuccess {
            showToast(R.string.toast_favorite_success)
            FavRetweetManager.setFav(original.id)
            EventBus.getDefault().post(StatusActionEvent())
        }
        .onFailure { e ->
            when {
                e is PenicillinException && e.error?.code == 139 -> {
                    showToast(R.string.toast_favorite_already)
                    FavRetweetManager.setFav(id)
                }
                else -> showToast(R.string.toast_favorite_failure)
            }
        }
        .isSuccess

suspend fun Status.unfavorite() = original.runCatching { unfavorite().await() }
        .onSuccess {
            showToast(R.string.toast_destroy_favorite_success)
            FavRetweetManager.removeFav(original.id)
            EventBus.getDefault().post(StatusActionEvent())
        }
        .onFailure { e ->
            when {
                e is PenicillinException && e.error == TwitterErrorMessage.SorryThatPageDoesNotExist -> {
                    showToast(R.string.toast_destroy_favorite_already)
                    FavRetweetManager.removeFav(original.id)
                }
                else -> showToast(R.string.toast_destroy_favorite_failure)
            }
        }
        .isSuccess

suspend fun Status.retweet() = runCatching { currentClient.statuses.retweet(original.id).await() }
        .onSuccess {
            showToast(R.string.toast_retweet_success)
            FavRetweetManager.setRetweet(original.id)
            EventBus.getDefault().post(StatusActionEvent())
        }
        .onFailure { e ->
            when {
                e is PenicillinException && e.error?.code == 37 -> {
                    showToast(R.string.toast_retweet_already)
                    FavRetweetManager.setRetweet(original.id)
                    EventBus.getDefault().post(StatusActionEvent())
                }
                else -> MessageUtil.showToast(R.string.toast_retweet_already)
            }
        }
        .isSuccess

suspend fun Status.destroyRetweet() = runCatching { currentClient.statuses.unretweet(original.id).await() }
        .onSuccess {
            showToast(R.string.toast_destroy_retweet_success)
            FavRetweetManager.removeRetweet(original.id)
            EventBus.getDefault().post(StatusActionEvent())
        }
        .onFailure { e ->
            when {
                e is PenicillinException && e.error == TwitterErrorMessage.SorryThatPageDoesNotExist -> {
                    showToast(R.string.toast_destroy_retweet_already)
                    FavRetweetManager.removeRetweet(original.id)
                    EventBus.getDefault().post(StatusActionEvent())
                }
                else -> MessageUtil.showToast(R.string.toast_destroy_retweet_failure)
            }
        }
        .isSuccess

fun Status.quote(context: Context) {
    val text = " $urlString"
    if (context is MainActivity) {
        EventBus.getDefault().post(OpenEditorEvent(text, this, null, null))
    } else {
        context.startActivity<PostActivity>("status" to text, "inReplyToStatus" to toJsonString())
    }
}

suspend fun Status.delete() {
    val res = runCatching {
        currentClient.statuses.delete(id).await()
    }.isSuccess
    if (res) {
        MessageUtil.showToast(R.string.toast_destroy_status_success)
        EventBus.getDefault().post(StreamingDestroyStatusEvent(id))
    } else {
        MessageUtil.showToast(R.string.toast_destroy_status_failure)
    }
}

suspend fun Identifier.updateStatus(str: String, inReplyToStatusId: Long? = null, imageList: List<File> = emptyList()) = runCatching {
        asClient {
            (if (imageList.isNotEmpty()) statuses.createWithMedia(str,
                    imageList.map { MediaComponent(
                            file = it,
                            type = it.mediaType(),
                            category = if (it.mediaType() == MediaType.GIF) MediaCategory.TweetGif else MediaCategory.TweetImage
                    ) },
                    "inReplyToStatusId" to inReplyToStatusId
                )
            else statuses.create(status = str, inReplyToStatusId = inReplyToStatusId)).await().result
        }
    }.onSuccess { s ->
        s.entities.hashtags.forEach { PostStockSettings.addHashtag("#${it.text}") }
    }.exceptionOrNull()

suspend fun Identifier.sendDirectMessage(rawMessage: String) = asClient { sendDirectMessage(rawMessage) }

suspend fun ApiClient.sendDirectMessage(rawMessage: String) = runCatching {
    val message = rawMessage.split(" ".toRegex(), 3)
    directMessages.create(message[2], screenName = message[1]).await()
}.exceptionOrNull()

object ActionUtil {
    fun doReply(status: Status, context: Context) {
        val mentions = status.entities.userMentions
        val text = "@" + (if (status.user.id == currentIdentifier.userId && mentions.size == 1) mentions[0].screenName
                else status.user.screenName) + " "
        if (context is MainActivity) {
            EventBus.getDefault().post(OpenEditorEvent(text, status, text.length, null))
        } else {
            context.startActivity<PostActivity>("status" to text, "selection" to text.length, "inReplyToStatus" to status.toJsonString())
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
            context.startActivity<PostActivity>("status" to text, "selection" to text.length)
        }
    }

    suspend fun destroyDirectMessage(id: Long) {
        val dm = runCatching { currentClient.directMessages.delete(id).await().result }.getOrNull()
        if (dm != null) {
            MessageUtil.showToast(R.string.toast_destroy_direct_message_success)
            EventBus.getDefault().post(StreamingDestroyMessageEvent(dm.id))
        } else {
            MessageUtil.showToast(R.string.toast_destroy_direct_message_failure)
        }
    }
}
