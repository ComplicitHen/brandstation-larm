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
     * Returnerar nästa passstart som "Måndag 07:00", eller null om varje dag är pass.
     * Feature 4: visas i MainActivity under statusraden.
     */
    fun nextDutyStart(): String? {
        val mask = prefs.dutyDaysMask
        // Om alla dagar är pass — ingen "nästa" att visa
        if (mask and 0b1111111 == 0b1111111) return null

        val startMinutes = prefs.dutyStartMinutes
        val cal = Calendar.getInstance()
        val nowDayBit = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            Calendar.SATURDAY  -> 5
            Calendar.SUNDAY    -> 6
            else               -> 0
        }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // Sök nästa passdag inom 7 dagar framåt
        for (offset in 0..6) {
            val dayBit = (nowDayBit + offset) % 7
            val isDutyDay = (mask shr dayBit) and 1 == 1
            if (!isDutyDay) continue
            // Samma dag: passet måste börja i framtiden
            if (offset == 0 && nowMinutes >= startMinutes) continue

            val dayName = when (dayBit) {
                0 -> "Måndag"
                1 -> "Tisdag"
                2 -> "Onsdag"
                3 -> "Torsdag"
                4 -> "Fredag"
                5 -> "Lördag"
                6 -> "Söndag"
                else -> "?"
            }
            val hh = startMinutes / 60
            val mm = startMinutes % 60
            return "$dayName %02d:%02d".format(hh, mm)
        }
        return null
    }

    /**
     * Kontrollerar om aktuell tid är inom quiet hours (hanterar övernattsspann, t.ex. 22:00–06:00).
     */
    fun isQuietHours(): Boolean {
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = prefs.quietStartMinutes
        val end = prefs.quietEndMinutes
        return if (start < end) {
            // Samma dag, t.ex. 10:00–14:00
            nowMinutes in start until end
        } else {
            // Övernatt, t.ex. 22:00–06:00
            nowMinutes >= start || nowMinutes < end
        }
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
            prefs.senderNumber.split(",").map { it.trim() }.any { it == sender.trim() }
        if (!senderMatch) return null

        val isTotalAlarm = message.contains(prefs.totalAlarmKeyword, ignoreCase = true)
        // "LARM" finns i båda typerna — det räcker att meddelandet innehåller det
        val isAnyAlarm = message.contains(prefs.alarmKeyword, ignoreCase = true)

        return when {
            isTotalAlarm && prefs.totalAlarmEnabled -> AlarmType.TOTAL
            isAnyAlarm && isOnDuty() && !(prefs.quietHoursEnabled && isQuietHours()) -> AlarmType.REGULAR
            else -> null
        }
    }
}
