package net.slashOmega.juktaway

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.util.ActionUtil

/**
 * Created on 2018/08/24.
 */

class FavoriteActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("notification", false))
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        intent.getLongExtra("statusId", -1L).takeIf { it > 0 }?.let {
            GlobalScope.launch { ActionUtil.doFavorite(it) }
        }
        finish()
    }
}