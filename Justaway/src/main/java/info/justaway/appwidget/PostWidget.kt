package info.justaway.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import info.justaway.R

class PostWidget: AppWidgetProvider() {
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingIntent = PendingIntent.getActivity(context, 0,
                Intent().apply {
                    putExtra("widget", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }, PendingIntent.FLAG_CANCEL_CURRENT)
        context?.let {
            appWidgetManager?.updateAppWidget(appWidgetIds,
                    RemoteViews(it.packageName, R.layout.widget_main).apply {
                        setOnClickPendingIntent(R.id.icon, pendingIntent)
                    })
        }
    }
}