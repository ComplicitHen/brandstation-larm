package com.brandstation.larm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
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
    private var flashlight: FlashlightStrobe? = null

    // Feature 3: Watchdog — dynamisk registrering av TIME_TICK
    private val watchdogReceiver = WatchdogReceiver()
    private var watchdogRegistered = false

    // Feature 6: Repeat-larm om ej kvitterat
    private var repeatAlarmType: AlarmType = AlarmType.REGULAR
    private var repeatMessage: String = ""

    override fun onCreate() {
        super.onCreate()
        alarmPlayer = AlarmPlayer(this)
        createNotificationChannels()

        // Feature 3: Registrera watchdog dynamiskt (ACTION_TIME_TICK kräver dynamisk registrering)
        registerReceiver(watchdogReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        watchdogRegistered = true
        Log.i(TAG, "Tjänsten skapad")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Starta alltid foreground direkt — Android kräver detta inom 5 sekunder
        startForeground(NOTIF_MONITOR, buildMonitorNotification())

        when (intent?.action) {
            ACTION_TRIGGER -> {
                val typeName = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: return START_STICKY
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""
                val alarmType = AlarmType.valueOf(typeName)
                triggerAlarm(alarmType, message, sender)
            }
            ACTION_DISMISS -> dismissAlarm()
            ACTION_AUTO_DISMISS -> {
                Log.i(TAG, "Auto-dismiss utlöst")
                dismissAlarm()
            }
            ACTION_START_MONITOR -> {
                // Uppdatera statusnotisen
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_MONITOR, buildMonitorNotification())
            }
            // Feature 6: Upprepa larm om ej kvitterat
            ACTION_REPEAT -> {
                if (isAlarmActive) {
                    Log.i(TAG, "Upprepar larm (ej kvitterat)")
                    alarmPlayer.play(repeatAlarmType)
                    scheduleRepeat(repeatAlarmType, repeatMessage)
                }
            }
        }

        return START_STICKY   // Systemet startar om tjänsten om den dödas
    }

    private fun triggerAlarm(alarmType: AlarmType, message: String, sender: String = "") {
        if (isAlarmActive) {
            Log.w(TAG, "Larm redan aktivt, ignorerar nytt larm")
            return
        }
        isAlarmActive = true
        repeatAlarmType = alarmType
        repeatMessage = message
        Log.i(TAG, "LARM UTLÖST: $alarmType — $message")

        // Spara i larmloggen
        val testMode = Prefs(this).smsTestMode
        AlarmLog.add(this, AlarmEntry(
            timestamp = System.currentTimeMillis(),
            alarmType = alarmType,
            sender = sender,
            message = message,
            wasTestMode = testMode,
        ))

        // Håll skärmen tänd under larmet (max 10 minuter)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "BrandstationLarm::AlarmWake"
        ).apply { acquire(10 * 60 * 1000L) }

        alarmPlayer.play(alarmType)
        showAlarmNotification(alarmType, message)

        // Feature 6: Schemalägg upprepning om 3 minuter
        scheduleRepeat(alarmType, message)

        // Feature: Ficklampa strobe
        val prefs = Prefs(this)
        if (prefs.flashlightStrobe) {
            flashlight = FlashlightStrobe(this).also { it.start() }
        }

        // Feature: Auto-dismiss
        val autoDismissMin = prefs.autoDismissMinutes
        if (autoDismissMin > 0) {
            scheduleAutoDismiss(autoDismissMin)
        }

        // Starta AlarmActivity som visas ovanpå låsskärmen
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(EXTRA_MESSAGE, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(alarmIntent)
    }

    // Feature 6: Schemalägg upprepning av larm om 3 minuter
    private fun scheduleRepeat(alarmType: AlarmType, message: String) {
        val repeatIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_REPEAT
            putExtra(EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(EXTRA_MESSAGE, message)
        }
        val pi = PendingIntent.getService(
            this, REQ_REPEAT, repeatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 3 * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    // Feature 6: Avbryt schemalagd upprepning
    private fun cancelRepeat() {
        val repeatIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_REPEAT
        }
        val pi = PendingIntent.getService(
            this, REQ_REPEAT, repeatIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(it)
        }
    }

    fun dismissAlarm() {
        if (!isAlarmActive) return
        isAlarmActive = false
        alarmPlayer.stop()
        wakeLock?.release()
        wakeLock = null
        cancelRepeat()
        cancelAutoDismiss()
        flashlight?.stop()
        flashlight = null
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ALARM)
        // Uppdatera statusnotisen
        nm.notify(NOTIF_MONITOR, buildMonitorNotification())
        Log.i(TAG, "Larm avslutat")
    }

    // Feature: Schemalägg auto-dismiss
    private fun scheduleAutoDismiss(minutes: Int) {
        val autoDismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_AUTO_DISMISS
        }
        val pi = PendingIntent.getService(
            this, REQ_AUTO_DISMISS, autoDismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    // Feature: Avbryt schemalagd auto-dismiss
    private fun cancelAutoDismiss() {
        val autoDismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_AUTO_DISMISS
        }
        val pi = PendingIntent.getService(
            this, REQ_AUTO_DISMISS, autoDismissIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(it)
        }
    }

    private fun showAlarmNotification(alarmType: AlarmType, message: String) {
        val title = if (alarmType == AlarmType.TOTAL) "⚠ TOTALLARM" else "🚒 LARM"

        val dismissIntent = Intent(this, AlarmService::class.java)
        dismissIntent.setAction(ACTION_DISMISS)
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

        // Feature 5: LED-blinkning på larmkanalen
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Larm",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Larmnotifikation från SOS/brandstation"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

        // Watchdog-kanal skapas i WatchdogReceiver vid behov
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        alarmPlayer.stop()
        wakeLock?.release()
        if (watchdogRegistered) {
            unregisterReceiver(watchdogReceiver)
            watchdogRegistered = false
        }
        Log.w(TAG, "AlarmService förstörd — systemet borde starta om den")
    }

    companion object {
        private const val TAG = "AlarmService"

        const val ACTION_TRIGGER = "com.brandstation.larm.TRIGGER"
        const val ACTION_DISMISS = "com.brandstation.larm.DISMISS"
        const val ACTION_START_MONITOR = "com.brandstation.larm.START_MONITOR"
        // Feature 6
        const val ACTION_REPEAT = "com.brandstation.larm.REPEAT"
        // Feature: Auto-dismiss
        const val ACTION_AUTO_DISMISS = "com.brandstation.larm.AUTO_DISMISS"

        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SENDER = "sender"

        const val CHANNEL_MONITOR = "ch_monitor"
        const val CHANNEL_ALARM = "ch_alarm"

        const val NOTIF_MONITOR = 1
        const val NOTIF_ALARM = 2

        private const val REQ_REPEAT = 300
        private const val REQ_AUTO_DISMISS = 400
    }
}
