package com.brandstation.larm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AlarmEntry(
    val timestamp: Long,
    val alarmType: AlarmType,
    val sender: String,
    val message: String,
    val wasTestMode: Boolean,
)

object AlarmLog {

    private const val MAX_ENTRIES = 50
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun add(context: Context, entry: AlarmEntry) {
        val prefs = Prefs(context)
        val arr = runCatching { JSONArray(prefs.alarmLogJson) }.getOrDefault(JSONArray())

        val obj = JSONObject().apply {
            put("ts", entry.timestamp)
            put("type", entry.alarmType.name)
            put("sender", entry.sender)
            put("message", entry.message)
            put("test", entry.wasTestMode)
        }

        // Lägg till sist, ta bort äldst om nödvändigt
        arr.put(obj)
        val trimmed = JSONArray()
        val start = maxOf(0, arr.length() - MAX_ENTRIES)
        for (i in start until arr.length()) trimmed.put(arr.get(i))

        prefs.alarmLogJson = trimmed.toString()
    }

    fun getAll(context: Context): List<AlarmEntry> {
        val prefs = Prefs(context)
        val arr = runCatching { JSONArray(prefs.alarmLogJson) }.getOrDefault(JSONArray())
        val result = mutableListOf<AlarmEntry>()
        for (i in arr.length() - 1 downTo 0) {
            val obj = arr.getJSONObject(i)
            result.add(
                AlarmEntry(
                    timestamp = obj.getLong("ts"),
                    alarmType = AlarmType.valueOf(obj.getString("type")),
                    sender = obj.optString("sender", ""),
                    message = obj.getString("message"),
                    wasTestMode = obj.optBoolean("test", false),
                )
            )
        }
        return result
    }

    fun formatTime(ts: Long): String = dateFormat.format(Date(ts))

    fun clear(context: Context) {
        Prefs(context).alarmLogJson = "[]"
    }
}
