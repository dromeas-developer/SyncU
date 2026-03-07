package com.syncu.api

import android.util.Log
import com.google.gson.Gson
import com.syncu.data.DailySummary
import com.syncu.data.IntervalsWellnessData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * intervals.icu Wellness API Client
 * Handles wellness data exchange with intervals.icu
 */
class IntervalsWellnessApiClient(
    apiKey: String,
    athleteId: String
) {
    private val cleanApiKey = apiKey.trim()
    private val cleanAthleteId = athleteId.trim()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("IntervalsApi", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val credentials = Credentials.basic("API_KEY", cleanApiKey)
            val request = chain.request().newBuilder()
                .addHeader("Authorization", credentials)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "SyncU/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()
    private val baseUrl = "https://intervals.icu/api/v1/athlete/$cleanAthleteId"

    /**
     * Get wellness data for a specific date
     */
    suspend fun getWellnessForDate(date: LocalDate): Result<IntervalsWellnessData?> = withContext(Dispatchers.IO) {
        try {
            val dateStr = date.format(DateTimeFormatter.ISO_DATE)
            val url = "$baseUrl/wellness/$dateStr"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string() ?: "{}"
                val data = gson.fromJson(json, IntervalsWellnessData::class.java)
                Result.success(data)
            } else if (response.code == 404) {
                Result.success(null)
            } else {
                Log.e("IntervalsApi", "Error: ${response.code} ${response.message}")
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("IntervalsApi", "Network error", e)
            Result.failure(e)
        }
    }

    /**
     * Upload or update wellness data to intervals.icu for a specific date
     * Uses PUT /api/v1/athlete/{id}/wellness/{date}
     */
    suspend fun uploadWellnessData(summary: DailySummary): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dateStr = summary.date.format(DateTimeFormatter.ISO_DATE)
            val wellnessData = summary.toIntervalsWellness()
            val json = gson.toJson(wellnessData)
            
            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            // Using PUT for updates to a specific date record
            val request = Request.Builder()
                .url("$baseUrl/wellness/$dateStr")
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.i("IntervalsApi", "Upload successful for $dateStr")
                Result.success(responseBody)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e("IntervalsApi", "Upload failed (${response.code}): $errorBody")
                Result.failure(IOException("Upload failed: ${response.code} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("IntervalsApi", "Upload exception", e)
            Result.failure(e)
        }
    }

    /**
     * Get wellness data for a date range
     */
    suspend fun getWellnessData(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<IntervalsWellnessData>> = withContext(Dispatchers.IO) {
        try {
            val formatter = DateTimeFormatter.ISO_DATE
            val url = "$baseUrl/wellness?oldest=${startDate.format(formatter)}&newest=${endDate.format(formatter)}"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string() ?: "[]"
                val data = gson.fromJson(json, Array<IntervalsWellnessData>::class.java).toList()
                Result.success(data)
            } else {
                Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("IntervalsApi", "Range fetch error", e)
            Result.failure(e)
        }
    }
}

/**
 * Extension function to convert DailySummary to Intervals.icu format
 * Sanitizes data by replacing 0 with null for fields where 0 is invalid/unlikely
 */
fun DailySummary.toIntervalsWellness(): IntervalsWellnessData {
    val formatter = DateTimeFormatter.ISO_DATE
    // Use asleepDurationMinutes if available, otherwise fallback to durationMinutes
    val effectiveSleepMinutes = this.sleep?.asleepDurationMinutes ?: this.sleep?.durationMinutes
    
    // Helper to treat 0 as null for integer fields (Interals.icu 422 error prevention)
    fun Int?.nullIfZero(): Int? = if (this == 0) null else this
    // Helper to treat 0 as null for double fields
    fun Double?.nullIfZero(): Double? = if (this == null || this == 0.0) null else this

    return IntervalsWellnessData(
        id = this.date.format(formatter),
        restingHR = this.restingHR.nullIfZero(),
        hrv = this.hrvMs.nullIfZero(),
        weight = this.weightKg.nullIfZero(),
        bodyFat = this.bodyFatPercentage.nullIfZero(),
        leanMass = this.leanBodyMassKg.nullIfZero(),
        boneMass = this.boneMassKg.nullIfZero(),
        vo2max = this.vo2Max.nullIfZero(),
        sleepSecs = effectiveSleepMinutes?.toInt()?.times(60).nullIfZero(),
        avgSleepingHR = this.sleep?.avgHeartRate?.nullIfZero()?.toDouble(),
        spO2 = this.spo2Percentage.nullIfZero(),
        systolic = this.systolicBP.nullIfZero(),
        diastolic = this.diastolicBP.nullIfZero(),
        bloodGlucose = this.glucoseMmol.nullIfZero(),
        respiration = this.respiratoryRate.nullIfZero(),
        kcalConsumed = this.caloriesBurned?.toInt().nullIfZero(),
        steps = this.steps.nullIfZero(),
        carbohydrates = this.carbsGrams.nullIfZero(),
        protein = this.proteinGrams.nullIfZero(),
        fatTotal = this.fatGrams.nullIfZero()
    )
}
