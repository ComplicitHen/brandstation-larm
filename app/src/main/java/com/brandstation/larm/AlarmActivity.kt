package com.brandstation.larm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivityAlarmBinding

/**
 * Visas ovanpå låsskärmen när ett larm utlöses.
 * Konfigureras i manifestet med showOnLockScreen + turnScreenOn.
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var alarmType: AlarmType = AlarmType.REGULAR
    private var message: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 8.1 och äldre: sätt flags direkt på fönstret
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } else {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmType = AlarmType.valueOf(intent.getStringExtra(AlarmService.EXTRA_ALARM_TYPE) ?: "REGULAR")
        message = intent.getStringExtra(AlarmService.EXTRA_MESSAGE) ?: ""

        if (alarmType == AlarmType.TOTAL) {
            binding.alarmTitle.text = "TOTALLARM"
            binding.alarmTitle.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.alarmCard.setCardBackgroundColor(getColor(android.R.color.holo_red_dark))
        } else {
            binding.alarmTitle.text = "LARM"
            binding.alarmTitle.setTextColor(getColor(android.R.color.holo_orange_dark))
            binding.alarmCard.setCardBackgroundColor(getColor(android.R.color.holo_orange_dark))
        }

        binding.alarmMessage.text = message

        binding.dismissButton.setOnClickListener {
            dismissAlarm()
        }

        // Feature 2: Snooze 5 minuter
        binding.snoozeButton.setOnClickListener {
            scheduleSnooze()
            dismissAlarm()
        }
    }

    // Feature 1: Volymknapp stänger av larm
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            dismissAlarm()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dismissAlarm() {
        val intent = Intent(this, AlarmService::class.java)
        intent.setAction(AlarmService.ACTION_DISMISS)
        startService(intent)
        finish()
    }

    // Feature 2: Schemalägg snooze via AlarmManager efter 5 minuter
    private fun scheduleSnooze() {
        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra(AlarmService.EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(AlarmService.EXTRA_MESSAGE, message)
        }
        val pi = PendingIntent.getBroadcast(
            this, 200, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Blockera tillbaka-knappen — larmet måste stängas av aktivt
    }
}
