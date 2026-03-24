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

    /** Vibration-only: spela inget ljud, bara vibrera */
    var vibrationOnly: Boolean
        get() = p.getBoolean("vibration_only", false)
        set(v) = p.edit().putBoolean("vibration_only", v).apply()

    /** Text-to-speech: använd röst istället för larmsignal */
    var useTts: Boolean
        get() = p.getBoolean("use_tts", false)
        set(v) = p.edit().putBoolean("use_tts", v).apply()

    // Feature: Auto-dismiss efter X minuter (0 = aldrig)
    var autoDismissMinutes: Int
        get() = p.getInt("auto_dismiss_min", 10)
        set(v) = p.edit().putInt("auto_dismiss_min", v).apply()

    // Feature: Batterinivå-varning — tidsstämpel för senaste varning
    var batteryWarnedAt: Long
        get() = p.getLong("battery_warned_at", 0L)
        set(v) = p.edit().putLong("battery_warned_at", v).apply()

    // Feature: Ficklampa blinkar vid larm
    var flashlightStrobe: Boolean
        get() = p.getBoolean("flashlight_strobe", false)
        set(v) = p.edit().putBoolean("flashlight_strobe", v).apply()

    // Feature: Larmfri tid (tyst nattetid för vanliga larm)
    var quietHoursEnabled: Boolean
        get() = p.getBoolean("quiet_hours_enabled", false)
        set(v) = p.edit().putBoolean("quiet_hours_enabled", v).apply()

    /** Quiet hours start, minuter från midnatt. Standard 22:00 = 1320 */
    var quietStartMinutes: Int
        get() = p.getInt("quiet_start", 22 * 60)
        set(v) = p.edit().putInt("quiet_start", v).apply()

    /** Quiet hours slut, minuter från midnatt. Standard 06:00 = 360 */
    var quietEndMinutes: Int
        get() = p.getInt("quiet_end", 6 * 60)
        set(v) = p.edit().putInt("quiet_end", v).apply()

    // Geolokalisering
    var geoFilterEnabled: Boolean
        get() = p.getBoolean("geo_filter_enabled", false)
        set(v) = p.edit().putBoolean("geo_filter_enabled", v).apply()

    var stationLat: Double
        get() = p.getString("station_lat", "57.656563")!!.toDoubleOrNull() ?: 57.656563
        set(v) = p.edit().putString("station_lat", v.toString()).apply()

    var stationLng: Double
        get() = p.getString("station_lng", "12.114100")!!.toDoubleOrNull() ?: 12.114100
        set(v) = p.edit().putString("station_lng", v.toString()).apply()

    var geoRadiusKm: Int
        get() = p.getInt("geo_radius_km", 25)
        set(v) = p.edit().putInt("geo_radius_km", v).apply()

    /** Cachat svar: är användaren inom radie? Uppdateras av LocationWorker var 30:e min */
    var withinRadius: Boolean
        get() = p.getBoolean("within_radius", true) // default true = larma om position okänd
        set(v) = p.edit().putBoolean("within_radius", v).apply()

    /** Tidsstämpel för senaste positionskoll */
    var lastLocationCheckMs: Long
        get() = p.getLong("last_location_check_ms", 0L)
        set(v) = p.edit().putLong("last_location_check_ms", v).apply()
}
