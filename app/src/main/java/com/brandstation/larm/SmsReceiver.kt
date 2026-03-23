package com.brandstation.larm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Sätt ihop fragmenterade SMS-delar till ett meddelande
        val sender = messages[0].originatingAddress ?: ""
        val body = messages.joinToString("") { it.messageBody }

        Log.d(TAG, "SMS mottaget från: $sender")

        val scheduleManager = ScheduleManager(context)
        val alarmType = scheduleManager.shouldTrigger(sender, body) ?: run {
            Log.d(TAG, "Inget larm ska utlösas (avsändare/schema/typ matchar inte)")
            return
        }

        Log.i(TAG, "Larmar! Typ: $alarmType")

        // Skicka till AlarmService — om tjänsten redan kör som foreground
        // räcker startService(), annars startForegroundService()
        val serviceIntent = Intent(context, AlarmService::class.java)
        serviceIntent.setAction(AlarmService.ACTION_TRIGGER)
        serviceIntent.putExtra(AlarmService.EXTRA_ALARM_TYPE, alarmType.name)
        serviceIntent.putExtra(AlarmService.EXTRA_MESSAGE, body)
        serviceIntent.putExtra(AlarmService.EXTRA_SENDER, sender)
        context.startForegroundService(serviceIntent)
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
