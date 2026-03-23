package com.brandstation.larm

import android.content.Context
import java.util.Calendar

enum class AlarmType { REGULAR, TOTAL }

class ScheduleManager(context: Context) {
    private val prefs = Prefs(context)

    /**
     * Returnerar true om nuvarande tid faller inom schemat för vanligt jour.
     */
    fun isOnDuty(): Boolean {
        val cal = Calendar.getInstance()
        val bitIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            Calendar.SATURDAY  -> 5
            Calendar.SUNDAY    -> 6
            else               -> return false
        }

        val isDutyDay = (prefs.dutyDaysMask shr bitIndex) and 1 == 1
        if (!isDutyDay) return false

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return nowMinutes in prefs.dutyStartMinutes until prefs.dutyEndMinutes
    }

    /**
     * Avgör om ett inkommande SMS ska utlösa larm och i så fall vilket typ.
     * - Under schema: vanligt larm ELLER totallarm
     * - Utanför schema: endast totallarm
     * Returnerar null om inget larm ska utlösas.
     */
    fun shouldTrigger(sender: String, message: String): AlarmType? {
        if (!prefs.isEnabled) return null
        if (sender.trim() != prefs.senderNumber.trim()) return null

        val isTotalAlarm = message.contains(prefs.totalAlarmKeyword, ignoreCase = true)

        return when {
            isTotalAlarm  -> AlarmType.TOTAL
            isOnDuty()    -> AlarmType.REGULAR
            else          -> null   // Utanför schema, inget totallarm → ignorera
        }
    }
}
