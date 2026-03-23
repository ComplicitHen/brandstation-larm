package com.brandstation.larm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat

object BatteryMonitor {

    private const val CHANNEL_BATTERY = "ch_battery"
    private const val NOTIF_BATTERY = 5
    private const val WARN_THRESHOLD = 20
    private const val WARN_INTERVAL_MS = 60 * 60 * 1000L  // 1 timme

    fun checkAndNotify(context: Context) {
        val prefs = Prefs(context)
        if (!prefs.isEnabled) return

        val batteryStatus: Intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return

        val batteryPct = (level * 100 / scale)
        if (batteryPct >= WARN_THRESHOLD) return

        val now = System.currentTimeMillis()
        if (now - prefs.batteryWarnedAt < WARN_INTERVAL_MS) return

        prefs.batteryWarnedAt = now
        sendBatteryNotification(context, batteryPct)
    }

    private fun sendBatteryNotification(context: Context, pct: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_BATTERY,
                "Batterinivå-varning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Varning när batterinivån är låg"
            }
            nm.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_BATTERY)
            .setContentTitle("Låg batterinivå")
            .setContentText("Larmövervakning kan sluta fungera! ($pct%)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_BATTERY, notif)
    }
}
