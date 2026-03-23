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

    var totalAlarmKeyword: String
        get() = p.getString("totallarm_kw", "TOTALLARM") ?: "TOTALLARM"
        set(v) = p.edit().putString("totallarm_kw", v).apply()

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
}
