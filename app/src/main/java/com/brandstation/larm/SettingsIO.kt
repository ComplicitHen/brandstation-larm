package com.brandstation.larm

import android.content.Context
import org.json.JSONObject

/**
 * Exporterar och importerar alla appinställningar som JSON.
 * Feature 7: används från SettingsActivity.
 */
object SettingsIO {

    fun export(context: Context): String {
        val prefs = Prefs(context)
        return JSONObject().apply {
            put("sender", prefs.senderNumber)
            put("totalAlarmKeyword", prefs.totalAlarmKeyword)
            put("alarmKeyword", prefs.alarmKeyword)
            put("dutyDaysMask", prefs.dutyDaysMask)
            put("dutyStartMinutes", prefs.dutyStartMinutes)
            put("dutyEndMinutes", prefs.dutyEndMinutes)
            put("totalAlarmEnabled", prefs.totalAlarmEnabled)
            put("smsTestMode", prefs.smsTestMode)
            put("customSoundUri", prefs.customSoundUri ?: JSONObject.NULL)
        }.toString(2)
    }

    fun import(context: Context, json: String) {
        val prefs = Prefs(context)
        val obj = JSONObject(json)
        if (obj.has("sender")) prefs.senderNumber = obj.getString("sender")
        if (obj.has("totalAlarmKeyword")) prefs.totalAlarmKeyword = obj.getString("totalAlarmKeyword")
        if (obj.has("alarmKeyword")) prefs.alarmKeyword = obj.getString("alarmKeyword")
        if (obj.has("dutyDaysMask")) prefs.dutyDaysMask = obj.getInt("dutyDaysMask")
        if (obj.has("dutyStartMinutes")) prefs.dutyStartMinutes = obj.getInt("dutyStartMinutes")
        if (obj.has("dutyEndMinutes")) prefs.dutyEndMinutes = obj.getInt("dutyEndMinutes")
        if (obj.has("totalAlarmEnabled")) prefs.totalAlarmEnabled = obj.getBoolean("totalAlarmEnabled")
        if (obj.has("smsTestMode")) prefs.smsTestMode = obj.getBoolean("smsTestMode")
        if (obj.has("customSoundUri") && !obj.isNull("customSoundUri")) {
            prefs.customSoundUri = obj.getString("customSoundUri")
        } else if (obj.has("customSoundUri")) {
            prefs.customSoundUri = null
        }
    }
}
