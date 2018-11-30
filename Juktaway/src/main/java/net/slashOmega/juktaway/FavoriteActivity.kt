package net.slashOmega.juktaway

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import net.slashOmega.juktaway.task.FavoriteTask

/**
 * Created on 2018/08/24.
 */

class FavoriteActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("notification", false))
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        with (intent.getLongExtra("statusId", -1L)) {
            if (this > 0) FavoriteTask(this).execute()
        }
        finish()
    }
}