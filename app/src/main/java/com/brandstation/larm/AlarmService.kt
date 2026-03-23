package com.brandstation.larm

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Persistent foreground service som körs hela tiden i bakgrunden.
 * Håller telefonens larmövervakning aktiv och spelar larm vid behov.
 *
 * Android 14+ kräver foregroundServiceType="remoteMessaging|mediaPlayback"
 * vilket är deklarerat i manifestet.
 */
class AlarmService : Service() {

    private lateinit var alarmPlayer: AlarmPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var isAlarmActive = false

    override fun onCreate() {
        super.onCreate()
        alarmPlayer = AlarmPlayer(this)
        createNotificationChannels()
        Log.i(TAG, "Tjänsten skapad")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Starta alltid foreground direkt — Android kräver detta inom 5 sekunder
        startForeground(NOTIF_MONITOR, buildMonitorNotification())

        when (intent?.action) {
            ACTION_TRIGGER -> {
                val typeName = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: return START_STICKY
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                val alarmType = AlarmType.valueOf(typeName)
                triggerAlarm(alarmType, message)
            }
            ACTION_DISMISS -> dismissAlarm()
            ACTION_START_MONITOR -> {
                // Uppdatera statusnotisen
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_MONITOR, buildMonitorNotification())
            }
        }

        return START_STICKY   // Systemet startar om tjänsten om den dödas
    }

    private fun triggerAlarm(alarmType: AlarmType, message: String) {
        if (isAlarmActive) {
            Log.w(TAG, "Larm redan aktivt, ignorerar nytt larm")
            return
        }
        isAlarmActive = true
        Log.i(TAG, "LARM UTLÖST: $alarmType — $message")

        // Håll skärmen tänd under larmet (max 10 minuter)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "BrandstationLarm::AlarmWake"
        ).apply { acquire(10 * 60 * 1000L) }

        alarmPlayer.play(alarmType)
        showAlarmNotification(alarmType, message)

        // Starta AlarmActivity som visas ovanpå låsskärmen
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(EXTRA_MESSAGE, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(alarmIntent)
    }

    fun dismissAlarm() {
        if (!isAlarmActive) return
        isAlarmActive = false
        alarmPlayer.stop()
        wakeLock?.release()
        wakeLock = null
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ALARM)
        // Uppdatera statusnotisen
        nm.notify(NOTIF_MONITOR, buildMonitorNotification())
        Log.i(TAG, "Larm avslutat")
    }

    private fun showAlarmNotification(alarmType: AlarmType, message: String) {
        val title = if (alarmType == AlarmType.TOTAL) "⚠ TOTALLARM" else "🚒 LARM"

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPi = PendingIntent.getService(
            this, 10, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAlarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(EXTRA_MESSAGE, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openAlarmPi = PendingIntent.getActivity(
            this, 11, openAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openAlarmPi, true)
            .addAction(android.R.drawable.ic_delete, "Stäng av larm", dismissPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ALARM, notif)
    }

    private fun buildMonitorNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val scheduleManager = ScheduleManager(this)
        val prefs = Prefs(this)
        val statusText = when {
            !prefs.isEnabled -> "Inaktiverad"
            scheduleManager.isOnDuty() -> "På pass – alla larm aktiva"
            else -> "Utanför schema – endast totallarm"
        }

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("Brandstation Larm")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "Övervakningsstatus",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Visar att larmövervakning är aktiv"
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Larm",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Larmnotifikation från SOS/brandstation"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        alarmPlayer.stop()
        wakeLock?.release()
        Log.w(TAG, "AlarmService förstörd — systemet borde starta om den")
    }

    companion object {
        private const val TAG = "AlarmService"

        const val ACTION_TRIGGER = "com.brandstation.larm.TRIGGER"
        const val ACTION_DISMISS = "com.brandstation.larm.DISMISS"
        const val ACTION_START_MONITOR = "com.brandstation.larm.START_MONITOR"

        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val EXTRA_MESSAGE = "message"

        const val CHANNEL_MONITOR = "ch_monitor"
        const val CHANNEL_ALARM = "ch_alarm"

        const val NOTIF_MONITOR = 1
        const val NOTIF_ALARM = 2
    }
}
