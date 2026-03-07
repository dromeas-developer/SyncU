package com.syncu.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure Credentials Manager
 * Stores intervals.icu API credentials in encrypted storage
 */
class SecureCredentialsManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "syncu_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_API_KEY = "intervals_api_key"
        private const val KEY_ATHLETE_ID = "intervals_athlete_id"
    }

    /**
     * Save intervals.icu credentials
     * Uses commit() for synchronous save to ensure data is persisted immediately
     */
    fun saveCredentials(apiKey: String, athleteId: String): Boolean {
        return try {
            val success = sharedPreferences.edit().apply {
                putString(KEY_API_KEY, apiKey)
                putString(KEY_ATHLETE_ID, athleteId)
            }.commit()  // Use commit() for synchronous save

            Log.d("CredentialsManager", "Save credentials result: $success")
            Log.d("CredentialsManager", "Verification - API Key present: ${getApiKey() != null}")
            Log.d("CredentialsManager", "Verification - Athlete ID present: ${getAthleteId() != null}")

            success
        } catch (e: Exception) {
            Log.e("CredentialsManager", "Error saving credentials", e)
            false
        }
    }

    /**
     * Get API key
     */
    fun getApiKey(): String? {
        return try {
            sharedPreferences.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            Log.e("CredentialsManager", "Error getting API key", e)
            null
        }
    }

    /**
     * Get Athlete ID
     */
    fun getAthleteId(): String? {
        return try {
            sharedPreferences.getString(KEY_ATHLETE_ID, null)
        } catch (e: Exception) {
            Log.e("CredentialsManager", "Error getting athlete ID", e)
            null
        }
    }

    /**
     * Check if credentials are saved
     */
    fun hasCredentials(): Boolean {
        val apiKey = getApiKey()
        val athleteId = getAthleteId()
        val hasCredentials = !apiKey.isNullOrEmpty() && !athleteId.isNullOrEmpty()

        Log.d("CredentialsManager", "hasCredentials check:")
        Log.d("CredentialsManager", "  - API Key present: ${!apiKey.isNullOrEmpty()}")
        Log.d("CredentialsManager", "  - Athlete ID present: ${!athleteId.isNullOrEmpty()}")
        Log.d("CredentialsManager", "  - Result: $hasCredentials")

        return hasCredentials
    }

    /**
     * Clear all credentials
     */
    fun clearCredentials() {
        sharedPreferences.edit().clear().commit()
        Log.d("CredentialsManager", "Credentials cleared")
    }
}
