package com.syncu.api

import com.google.gson.Gson
import com.syncu.data.Activity
import com.syncu.data.ActivityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.format.DateTimeFormatter

/**
 * intervals.icu API Client
 * Uploads activities to intervals.icu
 */
class IntervalsApiClient(
    private val apiKey: String,
    private val athleteId: String
) {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Basic $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()
    private val baseUrl = "https://intervals.icu/api/v1/athlete/$athleteId"

    /**
     * Upload an activity to intervals.icu
     */
    suspend fun uploadActivity(activity: Activity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activityJson = mapToIntervalsFormat(activity)
            val json = gson.toJson(activityJson)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/activities")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Result.success(response.body?.string() ?: "")
            } else {
                Result.failure(IOException("Upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload multiple activities in batch
     */
    suspend fun uploadActivities(activities: List<Activity>): Result<Map<String, Boolean>> = 
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, Boolean>()
            
            for (activity in activities) {
                val result = uploadActivity(activity)
                results[activity.id] = result.isSuccess
                
                // Rate limiting
                if (activities.size > 1) {
                    kotlinx.coroutines.delay(500)
                }
            }
            
            Result.success(results)
        }

    /**
     * Map our Activity to intervals.icu format
     */
    private fun mapToIntervalsFormat(activity: Activity): Map<String, Any?> {
        val formatter = DateTimeFormatter.ISO_INSTANT
        
        return mapOf(
            "start_date_local" to activity.startTime.toString(),
            "type" to mapActivityType(activity.type),
            "duration" to activity.durationSeconds,
            "distance" to activity.distanceMeters,
            "calories" to activity.caloriesBurned?.toInt(),
            "avg_hr" to activity.avgHeartRate,
            "max_hr" to activity.maxHeartRate,
            "avg_watts" to activity.avgPower?.toInt(),
            "total_elevation_gain" to activity.elevationGain
        ).filterValues { it != null }
    }

    /**
     * Map our ActivityType to intervals.icu activity type
     */
    private fun mapActivityType(type: ActivityType): String {
        return when (type) {
            ActivityType.RUNNING -> "Run"
            ActivityType.CYCLING -> "Ride"
            ActivityType.WALKING -> "Walk"
            ActivityType.SWIMMING -> "Swim"
            ActivityType.HIKING -> "Hike"
            ActivityType.YOGA -> "Yoga"
            ActivityType.STRENGTH_TRAINING -> "WeightTraining"
            ActivityType.OTHER -> "Workout"
        }
    }

    /**
     * Delete an activity from intervals.icu
     */
    suspend fun deleteActivity(activityId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/activities/$activityId")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("Delete failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
