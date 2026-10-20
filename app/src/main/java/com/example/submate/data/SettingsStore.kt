package com.example.submate.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(val notifications: Boolean = true, val hour: Int = 9, val calendarSync: Boolean = false, val darkMode: String = "system", val purchased: Boolean = false, val calendarMessage: String = "")

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("submate_settings", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private fun read() = AppSettings(prefs.getBoolean("notifications", true), prefs.getInt("hour", 9), prefs.getBoolean("calendar", false), prefs.getString("theme", "system") ?: "system", prefs.getBoolean("purchased", false), prefs.getString("calendar_message", "") ?: "")
    fun update(change: (AppSettings) -> AppSettings) {
        val value = change(mutable.value)
        prefs.edit().putBoolean("notifications", value.notifications).putInt("hour", value.hour).putBoolean("calendar", value.calendarSync).putString("theme", value.darkMode).putBoolean("purchased", value.purchased).putString("calendar_message", value.calendarMessage).apply()
        mutable.value = value
    }
}
