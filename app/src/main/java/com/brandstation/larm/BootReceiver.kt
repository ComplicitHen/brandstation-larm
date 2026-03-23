package com.brandstation.larm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.i("BootReceiver", "Startar övervakingstjänsten efter: $action")

        val serviceIntent = Intent(context, AlarmService::class.java)
        serviceIntent.action = AlarmService.ACTION_START_MONITOR
        context.startForegroundService(serviceIntent)
    }
}
