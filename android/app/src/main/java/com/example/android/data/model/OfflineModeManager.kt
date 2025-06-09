package com.example.android.data.model


import android.content.Context
import android.content.SharedPreferences

object OfflineModeManager {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_OFFLINE_MODE = "offline_mode_enabled"

    fun isOfflineModeEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }

    fun setOfflineModeEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, enabled).apply()
    }
}