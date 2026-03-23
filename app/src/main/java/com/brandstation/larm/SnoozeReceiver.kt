package com.brandstation.larm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Mottar snooze-broadcast och re-triggar AlarmService efter 5 minuter.
 */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("SnoozeReceiver", "Snooze-tid ute, re-triggar larm")
        val alarmType = intent.getStringExtra(AlarmService.EXTRA_ALARM_TYPE) ?: return
        val message = intent.getStringExtra(AlarmService.EXTRA_MESSAGE) ?: ""

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_TRIGGER
            putExtra(AlarmService.EXTRA_ALARM_TYPE, alarmType)
            putExtra(AlarmService.EXTRA_MESSAGE, message)
        }
        context.startForegroundService(serviceIntent)
    }
}
