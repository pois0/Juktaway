package net.slash_omega.juktaway

import android.app.IntentService
import android.content.Intent
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import kotlinx.coroutines.*
import net.slash_omega.juktaway.settings.PostStockSettings.addDraft
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.util.updateStatus
import org.jetbrains.anko.toast
import java.io.File
import kotlin.coroutines.CoroutineContext

class PostService: IntentService("PostingTweetService"), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onHandleIntent(intent: Intent) {
        launch(Dispatchers.Default) {
            val text = intent.getStringExtra("text") ?: ""
            val replyStatusId = intent.getLongExtra("replyStatusId", -1L).takeIf { it < 0 }
            val imagePathList = intent.getSerializableExtra("imagePathList") as? List<File> ?: emptyList()
            val identifier = intent.getSerializableExtra("identifier") as Identifier
            val e = runCatching {
                identifier.updateStatus(text, replyStatusId, imagePathList)
            }.run { getOrNull() ?: exceptionOrNull() }
            if (e != null) {
                addDraft(text)
                val message = if (e is PenicillinException && e.error == TwitterErrorMessage.StatusIsADuplicate) {
                    R.string.toast_update_status_already
                } else R.string.toast_update_status_failure

                withContext(Dispatchers.Main) { toast(message) }
            } else {
                withContext(Dispatchers.Main) { toast(R.string.update_status_success) }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean {
        job.cancelChildren()
        return super.stopService(name)
    }
}