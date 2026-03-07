package com.syncu.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.syncu.api.HealthConnectManager
import com.syncu.api.IntervalsWellnessApiClient
import com.syncu.data.AppDatabase
import com.syncu.data.DatabaseHelper
import com.syncu.utils.PreferencesManager
import com.syncu.utils.SecureCredentialsManager
import com.syncu.api.ExtendedHealthConnectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.time.Instant
import kotlinx.coroutines.CancellationException

/**
 * Background sync worker
 * Syncs wellness data to intervals.icu at scheduled times
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefManager = PreferencesManager(applicationContext)
        if (!prefManager.autoSyncEnabled) return@withContext Result.success()

        try {
            Log.i("SyncWorker", "Background sync task started")
            
            // 1. Schedule NEXT run. 
            // We use APPEND_OR_REPLACE so that this running instance isn't cancelled,
            // but the next occurrence is queued up to run after this one completes.
            scheduleNext(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)

            val credentialsManager = SecureCredentialsManager(applicationContext)
            val apiKey = credentialsManager.getApiKey()
            val athleteId = credentialsManager.getAthleteId()

            if (apiKey == null || athleteId == null) {
                Log.w("SyncWorker", "Credentials missing")
                return@withContext Result.failure()
            }

            val healthManager = HealthConnectManager(applicationContext)
            val extendedHealthManager = ExtendedHealthConnectManager(applicationContext)
            
            if (!healthManager.isAvailable() || !healthManager.hasAllPermissions()) {
                Log.w("SyncWorker", "Health Connect permissions missing or SDK unavailable")
                return@withContext Result.failure()
            }

            val database = AppDatabase.getDatabase(applicationContext, charArrayOf())
            val dbHelper = DatabaseHelper(
                applicationContext,
                database,
                healthManager,
                extendedHealthManager
            )

            val intervalsClient = IntervalsWellnessApiClient(apiKey, athleteId)

            // Sync Yesterday and Today
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val daysToSync = listOf(yesterday, today)
            
            for (date in daysToSync) {
                Log.i("SyncWorker", "Processing sync for $date")
                // Load latest data from Health Connect and existing Intervals data
                dbHelper.loadDataForDate(date)
                val summary = dbHelper.getDailySummary(date)
                
                // Upload to Intervals.icu
                val result = intervalsClient.uploadWellnessData(summary)

                if (result.isSuccess) {
                    Log.i("SyncWorker", "Successfully synced $date to Intervals.icu")
                    
                    // Mark as synced in local DB with current timestamp
                    database.intervalsDao().updateLastSyncedAt(date, Instant.now())
                    
                    // IMPORTANT: Refresh Intervals cache AFTER successful upload 
                    // so the local database reflects the changes just sent to the server.
                    dbHelper.refreshIntervalsCache(date)
                } else {
                    Log.e("SyncWorker", "Failed to sync $date: ${result.exceptionOrNull()?.message}")
                }
            }

            Log.i("SyncWorker", "Background sync task completed")
            Result.success()
        } catch (e: CancellationException) {
            Log.i("SyncWorker", "Sync worker cancelled")
            throw e
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync execution error", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "syncu_scheduled_sync"

        /**
         * Calculates the delay to the next scheduled sync time and enqueues the work.
         * 
         * @param policy The policy to use if work already exists. 
         *               Defaults to KEEP to avoid unnecessary rescheduling.
         */
        fun scheduleNext(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val prefManager = PreferencesManager(context)
            if (!prefManager.autoSyncEnabled) {
                cancelSync(context)
                return
            }

            val now = LocalDateTime.now()
            
            // Calculate next occurrence of Time 1
            val time1 = parseTime(prefManager.syncTime1) ?: LocalTime.of(8, 0)
            var nextOccurrence1 = now.with(time1).withSecond(0).withNano(0)
            if (!nextOccurrence1.isAfter(now)) {
                nextOccurrence1 = nextOccurrence1.plusDays(1)
            }

            var nextRunAt = nextOccurrence1

            // If Time 2 is enabled, check if it's sooner
            if (prefManager.syncTime2Enabled) {
                val time2 = parseTime(prefManager.syncTime2) ?: LocalTime.of(1, 0)
                var nextOccurrence2 = now.with(time2).withSecond(0).withNano(0)
                if (!nextOccurrence2.isAfter(now)) {
                    nextOccurrence2 = nextOccurrence2.plusDays(1)
                }
                
                if (nextOccurrence2.isBefore(nextRunAt)) {
                    nextRunAt = nextOccurrence2
                }
            }

            // Safety: If the next run is within 5 seconds, it's likely we are just finishing or 
            // right at the edge of the window. Push it to the next cycle to prevent tight loops.
            val delayMillis = Duration.between(now, nextRunAt).toMillis()
            if (delayMillis < 5000) {
                Log.i("SyncWorker", "Next window too close (${delayMillis}ms). Pushing to next occurrence.")
                nextRunAt = nextRunAt.plusDays(1)
            }

            // Re-calculate delay in seconds for WorkManager. 
            // getSeconds() is API 26 compatible (minSdk).
            val finalDelaySeconds = Duration.between(now, nextRunAt).seconds
            Log.i("SyncWorker", "Next sync scheduled for $now (in ${finalDelaySeconds/60} minutes) with policy $policy")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(finalDelaySeconds, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                policy,
                syncRequest
            )
        }

        private fun parseTime(timeStr: String): LocalTime? {
            return try {
                val parts = timeStr.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) { null }
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
