package com.brandstation.larm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

/**
 * Hemskärmswidget som visar aktuell jourtjänststatus.
 * Feature 8: uppdateras var 30:e minut via AppWidgetProviderInfo.
 */
class StatusWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val prefs = Prefs(context)
            val scheduleManager = ScheduleManager(context)

            val (statusText, color) = when {
                !prefs.isEnabled -> Pair("Inaktiv", Color.GRAY)
                scheduleManager.isOnDuty() -> Pair("På pass", Color.parseColor("#388E3C"))
                else -> Pair("Ej på pass", Color.parseColor("#F57C00"))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_status)
            views.setTextViewText(R.id.widgetStatusText, statusText)
            views.setInt(R.id.widgetColorDot, "setBackgroundColor", color)

            // Klick på widget öppnar MainActivity
            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetStatusText, pendingIntent)
            views.setOnClickPendingIntent(R.id.widgetColorDot, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
