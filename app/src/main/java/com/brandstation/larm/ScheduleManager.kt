package com.brandstation.larm

import android.content.Context
import java.util.Calendar

enum class AlarmType { REGULAR, TOTAL }

class ScheduleManager(context: Context) {
    private val prefs = Prefs(context)

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
     * Avgör om ett inkommande SMS ska utlösa larm.
     *
     * Riktiga SMS från VRR Ledningscentral ser ut så här:
     *   TOTALLARM: innehåller "TOTALLARM"
     *   Vanligt:   innehåller "LARM" men inte "TOTALLARM"
     *
     * SMS-testläge (smsTestMode=true): avsändarkontrollen hoppas över
     * så att man kan smsa till sig själv för att testa hela kedjan.
     */
    fun shouldTrigger(sender: String, message: String): AlarmType? {
        if (!prefs.isEnabled) return null

        val senderMatch = prefs.smsTestMode ||
                sender.trim() == prefs.senderNumber.trim()
        if (!senderMatch) return null

        val isTotalAlarm = message.contains(prefs.totalAlarmKeyword, ignoreCase = true)
        // "LARM" finns i båda typerna — det räcker att meddelandet innehåller det
        val isAnyAlarm = message.contains(prefs.alarmKeyword, ignoreCase = true)

        return when {
            isTotalAlarm && prefs.totalAlarmEnabled -> AlarmType.TOTAL
            isAnyAlarm && isOnDuty()                -> AlarmType.REGULAR
            else                                    -> null
        }
    }
}
