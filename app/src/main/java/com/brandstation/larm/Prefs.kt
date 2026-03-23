package com.brandstation.larm

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("larm_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = p.getBoolean("enabled", true)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    var senderNumber: String
        get() = p.getString("sender", "3315") ?: "3315"
        set(v) = p.edit().putString("sender", v).apply()

    /** Nyckelord i SMS som identifierar TOTALLARM. Standard matchar VRR: "TOTALLARM" */
    var totalAlarmKeyword: String
        get() = p.getString("totallarm_kw", "TOTALLARM") ?: "TOTALLARM"
        set(v) = p.edit().putString("totallarm_kw", v).apply()

    /** Nyckelord i SMS som identifierar vanligt larm. Standard matchar VRR: "LARM" */
    var alarmKeyword: String
        get() = p.getString("alarm_kw", "LARM") ?: "LARM"
        set(v) = p.edit().putString("alarm_kw", v).apply()

    /**
     * Passdagar som bitmask: bit 0 = måndag, bit 6 = söndag.
     * Standard = måndag–fredag (0b0011111 = 31).
     */
    var dutyDaysMask: Int
        get() = p.getInt("duty_days", 0b0011111)
        set(v) = p.edit().putInt("duty_days", v).apply()

    /** Passstart, minuter från midnatt. Standard = 07:00 = 420 */
    var dutyStartMinutes: Int
        get() = p.getInt("duty_start", 7 * 60)
        set(v) = p.edit().putInt("duty_start", v).apply()

    /** Passslut, minuter från midnatt. Standard = 22:00 = 1320 */
    var dutyEndMinutes: Int
        get() = p.getInt("duty_end", 22 * 60)
        set(v) = p.edit().putInt("duty_end", v).apply()

    /** Om totallarm ska trigga larm. Standard = true. */
    var totalAlarmEnabled: Boolean
        get() = p.getBoolean("totallarm_enabled", true)
        set(v) = p.edit().putBoolean("totallarm_enabled", v).apply()

    /**
     * SMS-testläge: när PÅ accepteras SMS från ALLA avsändare (inte bara senderNumber).
     * Används för att testa hela SMS-kedjan genom att smsa till sig själv.
     */
    var smsTestMode: Boolean
        get() = p.getBoolean("sms_test_mode", false)
        set(v) = p.edit().putBoolean("sms_test_mode", v).apply()

    /** URI till eget valt ljud, null = använd inbyggd signal */
    var customSoundUri: String?
        get() = p.getString("custom_sound_uri", null)
        set(v) = if (v == null) p.edit().remove("custom_sound_uri").apply()
                 else p.edit().putString("custom_sound_uri", v).apply()

    /** Larmlogg: JSON-lista med de senaste larmen (max 50 poster) */
    var alarmLogJson: String
        get() = p.getString("alarm_log", "[]") ?: "[]"
        set(v) = p.edit().putString("alarm_log", v).apply()
}
