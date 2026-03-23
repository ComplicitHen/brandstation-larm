package com.brandstation.larm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivityAlarmBinding

/**
 * Visas ovanpå låsskärmen när ett larm utlöses.
 * Konfigureras i manifestet med showOnLockScreen + turnScreenOn.
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding

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

        val alarmType = AlarmType.valueOf(intent.getStringExtra(AlarmService.EXTRA_ALARM_TYPE) ?: "REGULAR")
        val message = intent.getStringExtra(AlarmService.EXTRA_MESSAGE) ?: ""

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
            val intent = Intent(this, AlarmService::class.java)
            intent.setAction(AlarmService.ACTION_DISMISS)
            startService(intent)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Blockera tillbaka-knappen — larmet måste stängas av aktivt
    }
}
