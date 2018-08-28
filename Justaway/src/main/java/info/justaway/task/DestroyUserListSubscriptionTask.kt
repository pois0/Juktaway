package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.model.DestroyUserListEvent
import info.justaway.model.TwitterManager
import info.justaway.model.UserListCache
import info.justaway.util.MessageUtil
import twitter4j.UserList

class DestroyUserListSubscriptionTask(private var mUserList: UserList) : AsyncTask<Void, Void, Boolean>() {

    override fun doInBackground(vararg params: Void): Boolean? {
        return try {
            TwitterManager.getTwitter().destroyUserListSubscription(mUserList.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    override fun onPostExecute(success: Boolean?) {
        if (success!!) {
            MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_success)
            EventBus.getDefault().post(DestroyUserListEvent(mUserList.id))
            UserListCache.userLists!!.remove(mUserList)
        } else {
            MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_failure)
        }
    }
}
