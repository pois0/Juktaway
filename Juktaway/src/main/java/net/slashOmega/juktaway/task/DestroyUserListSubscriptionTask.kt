package net.slashOmega.juktaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.model.DestroyUserListEvent
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserListCache
import net.slashOmega.juktaway.util.MessageUtil
import twitter4j.UserList

class DestroyUserListSubscriptionTask(private var mUserList: UserList) : AsyncTask<Void, Void, Boolean>() {

    override fun doInBackground(vararg params: Void): Boolean? {
        return try {
            TwitterManager.twitter.destroyUserListSubscription(mUserList.id)
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
