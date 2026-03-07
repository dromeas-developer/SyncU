package com.syncu.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferences Manager
 * Stores app preferences like auto-sync settings, cache retention, and HR methods
 */
class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "syncu_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_CACHE_RETENTION_DAYS = "cache_retention_days"
        private const val KEY_USE_CUSTOM_RESTING_HR = "use_custom_resting_hr"
        private const val KEY_SYNC_TIME_1 = "sync_time_1" // Format: HH:mm
        private const val KEY_SYNC_TIME_2 = "sync_time_2" // Format: HH:mm
        private const val KEY_SYNC_TIME_2_ENABLED = "sync_time_2_enabled"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        
        private const val DEFAULT_CACHE_RETENTION_DAYS = 14
        private const val DEFAULT_SYNC_TIME_1 = "00:30"
        private const val DEFAULT_SYNC_TIME_2 = "09:00"
    }

    var autoSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()
        }

    var cacheRetentionDays: Int
        get() = sharedPreferences.getInt(KEY_CACHE_RETENTION_DAYS, DEFAULT_CACHE_RETENTION_DAYS)
        set(value) {
            sharedPreferences.edit().putInt(KEY_CACHE_RETENTION_DAYS, value).apply()
        }

    var useCustomRestingHR: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_CUSTOM_RESTING_HR, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_USE_CUSTOM_RESTING_HR, value).apply()
        }

    var syncTime1: String
        get() = sharedPreferences.getString(KEY_SYNC_TIME_1, DEFAULT_SYNC_TIME_1) ?: DEFAULT_SYNC_TIME_1
        set(value) {
            sharedPreferences.edit().putString(KEY_SYNC_TIME_1, value).apply()
        }

    var syncTime2: String
        get() = sharedPreferences.getString(KEY_SYNC_TIME_2, DEFAULT_SYNC_TIME_2) ?: DEFAULT_SYNC_TIME_2
        set(value) {
            sharedPreferences.edit().putString(KEY_SYNC_TIME_2, value).apply()
        }

    var syncTime2Enabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SYNC_TIME_2_ENABLED, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SYNC_TIME_2_ENABLED, value).apply()
        }

    var lastSyncTimestamp: Long
        get() = sharedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) {
            sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()
        }
}
