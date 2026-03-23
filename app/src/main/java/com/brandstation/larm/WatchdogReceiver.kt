package com.brandstation.larm

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Dynamisk BroadcastReceiver för ACTION_TIME_TICK (varje minut).
 * Registreras i AlarmService.onCreate — kan ej registreras statiskt i manifest.
 * Kontrollerar att AlarmService körs och skickar notis om den inte gör det.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_TICK) return

        val prefs = Prefs(context)
        if (!prefs.isEnabled) return

        if (!isAlarmServiceRunning(context)) {
            Log.w("WatchdogReceiver", "AlarmService körs inte — skickar varning")
            sendWatchdogNotification(context)
            // Försök starta om tjänsten
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_MONITOR
            }
            context.startForegroundService(serviceIntent)
        }
    }

    @Suppress("DEPRECATION")
    private fun isAlarmServiceRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(50).any {
            it.service.className == AlarmService::class.java.name
        }
    }

    private fun sendWatchdogNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_WATCHDOG,
                "Övervakningsvarning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Varning när larmövervakning är inaktiv"
            }
            nm.createNotificationChannel(channel)
        }

        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_WATCHDOG)
            .setContentTitle("Larmövervakning inaktiv")
            .setContentText("Tryck för att starta om larmövervakningen")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_WATCHDOG, notif)
    }

    companion object {
        const val CHANNEL_WATCHDOG = "ch_watchdog"
        const val NOTIF_WATCHDOG = 3
    }
}
