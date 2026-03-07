package com.syncu.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.syncu.api.HealthConnectManager
import com.syncu.api.ExtendedHealthConnectManager
import com.syncu.api.IntervalsWellnessApiClient
import com.syncu.utils.PreferencesManager
import com.syncu.utils.SecureCredentialsManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

/**
 * Database Helper with manual edit protection, data provenance, and dual caching (HC & Intervals)
 */
class DatabaseHelper(
    private val context: Context,
    private val database: AppDatabase,
    private val healthManager: HealthConnectManager,
    private val extendedHealthManager: ExtendedHealthConnectManager
) {

    private val preferencesManager = PreferencesManager(context)

    /**
     * Force a full refresh of all data for a specific date from APIs
     */
    suspend fun loadDataForDate(date: LocalDate) {
        try {
            purgeOldCache()
            
            // 1. Force refresh Sleep first (needed for custom resting HR calculation)
            val sleep = healthManager.getSleepForDate(date)
            sleep?.let { database.sleepDao().insertSleep(it) }
            
            // 2. Force refresh Wellness (HC) - Pass "true" sleep boundaries
            val sleepWindow = getAsleepWindow(sleep)
            refreshWellnessCache(date, sleepWindow.first, sleepWindow.second)
            
            // 3. Force refresh Intervals
            refreshIntervalsCache(date)
            
        } catch (e: Exception) {
            Log.e("SyncU_Data", "Error force loading data for $date", e)
        }
    }

    /**
     * Extracts the "true" asleep window from a sleep session.
     * Returns Pair(AsleepStartTime, AsleepEndTime).
     * This excludes leading and trailing "Awake" periods.
     */
    private fun getAsleepWindow(sleep: SleepSession?): Pair<Instant?, Instant?> {
        if (sleep == null) return Pair(null, null)
        
        val intervals = sleep.stageIntervals?.split(";") ?: return Pair(sleep.startTime, sleep.endTime)
        // Sleep stages: 2 (Generic), 4 (Light), 5 (Deep), 6 (REM)
        val sleepStageIds = listOf("2", "4", "5", "6")
        
        val sleepIntervals = intervals.mapNotNull { interval ->
            val parts = interval.split(",")
            if (parts.size == 3 && parts[2] in sleepStageIds) {
                try {
                    Triple(Instant.ofEpochMilli(parts[0].toLong()), Instant.ofEpochMilli(parts[1].toLong()), parts[2])
                } catch (e: Exception) { null }
            } else null
        }

        if (sleepIntervals.isEmpty()) return Pair(sleep.startTime, sleep.endTime)

        val start = sleepIntervals.first().first.truncatedTo(ChronoUnit.MINUTES)
        val end = sleepIntervals.last().second.truncatedTo(ChronoUnit.MINUTES)
        
        return Pair(start, end)
    }

    private suspend fun purgeOldCache() {
        try {
            val retentionDays = preferencesManager.cacheRetentionDays
            val cutoffDate = LocalDate.now().minusDays(retentionDays.toLong())
            database.wellnessDao().deleteWellnessBefore(cutoffDate)
            database.intervalsDao().deleteWellnessBefore(cutoffDate)
        } catch (e: Exception) {
            Log.e("SyncU_Data", "Error purging cache", e)
        }
    }

    private suspend fun refreshWellnessCache(date: LocalDate, sleepStart: Instant? = null, sleepEnd: Instant? = null) {
        val wellness = extendedHealthManager.getWellnessSummaryForDay(date, sleepStart, sleepEnd)
        val steps = healthManager.getStepsForDate(date)
        val totalCalories = healthManager.getActiveCaloriesForDate(date)
        val activeMinutes = healthManager.getActiveMinutesForDate(date)

        val record = DailyWellnessRecord(
            date = date,
            steps = steps,
            caloriesBurned = totalCalories,
            activeMinutes = activeMinutes,
            restingHR = wellness?.restingHR,
            maxHR = wellness?.maxHR,
            hrvMs = wellness?.hrvMs,
            weightKg = wellness?.weightKg,
            bodyFatPercentage = wellness?.bodyFatPercentage,
            leanBodyMassKg = wellness?.leanBodyMassKg,
            boneMassKg = wellness?.boneMassKg,
            spo2Percentage = wellness?.spo2Percentage,
            glucoseMmol = wellness?.glucoseMmol,
            systolicBP = wellness?.systolicBP,
            diastolicBP = wellness?.diastolicBP,
            vo2Max = wellness?.vo2Max,
            respiratoryRate = wellness?.respiratoryRate,
            lastUpdated = Instant.now()
        )
        database.wellnessDao().insertWellness(record)
    }

    suspend fun refreshIntervalsCache(date: LocalDate): IntervalsWellnessData? {
        val credentialsManager = SecureCredentialsManager(context)
        val apiKey = credentialsManager.getApiKey()
        val athleteId = credentialsManager.getAthleteId()

        if (apiKey != null && athleteId != null) {
            val intervalsClient = IntervalsWellnessApiClient(apiKey, athleteId)
            val result = intervalsClient.getWellnessForDate(date)
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    // Fetch existing record to preserve lastSyncedAt
                    val existing = database.intervalsDao().getWellnessForDate(date)
                    
                    val record = IntervalsWellnessRecord(
                        date = date,
                        weight = data.weight,
                        restingHR = data.restingHR,
                        hrv = data.hrv,
                        kcalConsumed = data.kcalConsumed,
                        sleepSecs = data.sleepSecs,
                        avgSleepingHR = data.avgSleepingHR,
                        spO2 = data.spO2,
                        systolic = data.systolic,
                        diastolic = data.diastolic,
                        bloodGlucose = data.bloodGlucose,
                        bodyFat = data.bodyFat,
                        leanMass = data.leanMass,
                        boneMass = data.boneMass,
                        vo2max = data.vo2max,
                        steps = data.steps,
                        respiration = data.respiration,
                        carbohydrates = data.carbohydrates,
                        protein = data.protein,
                        fatTotal = data.fatTotal,
                        lastUpdated = Instant.now(),
                        lastSyncedAt = existing?.lastSyncedAt
                    )
                    database.intervalsDao().insertWellness(record)
                }
                return data
            }
        }
        return null
    }

    suspend fun getDailySummary(date: LocalDate): DailySummary {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val isToday = date == LocalDate.now()
        
        // 1. Fetch Permissions first to filter cache
        val permissions = healthManager.getGrantedPermissions()
        fun has(recordType: KClass<out androidx.health.connect.client.records.Record>) = 
            permissions.contains(HealthPermission.getReadPermission(recordType).toString())

        // 2. Smart Sleep cache (Needed for resting HR logic)
        val sleepList = database.sleepDao().getSleepForDateRange(startOfDay.toEpochMilli(), endOfDay.toEpochMilli())
        var sleep = sleepList.firstOrNull()
        if (sleep == null || isToday) {
            val fetchedSleep = healthManager.getSleepForDate(date)
            fetchedSleep?.let {
                database.sleepDao().insertSleep(it)
                sleep = it
            }
        }
        
        // Clear cached sleep if permission revoked
        if (!has(androidx.health.connect.client.records.SleepSessionRecord::class)) {
            sleep = null
        }

        // 3. Smart Wellness (HC) cache
        var wellnessRecord = database.wellnessDao().getWellnessForDate(date)
        val shouldRefreshWellness = wellnessRecord == null || 
                (isToday && ChronoUnit.MINUTES.between(wellnessRecord.lastUpdated, Instant.now()) > 5) ||
                (ChronoUnit.HOURS.between(wellnessRecord.lastUpdated, Instant.now()) > 24)

        if (shouldRefreshWellness) {
            val sleepWindow = getAsleepWindow(sleep)
            refreshWellnessCache(date, sleepWindow.first, sleepWindow.second)
            wellnessRecord = database.wellnessDao().getWellnessForDate(date)
        }

        // IMPORTANT: Clear cached wellness fields if permission revoked
        wellnessRecord = wellnessRecord?.let {
            it.copy(
                steps = if (has(androidx.health.connect.client.records.StepsRecord::class)) it.steps else null,
                caloriesBurned = if (has(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class)) it.caloriesBurned else null,
                restingHR = if (has(androidx.health.connect.client.records.RestingHeartRateRecord::class) || has(androidx.health.connect.client.records.HeartRateRecord::class)) it.restingHR else null,
                maxHR = if (has(androidx.health.connect.client.records.HeartRateRecord::class)) it.maxHR else null,
                hrvMs = if (has(androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord::class)) it.hrvMs else null,
                weightKg = if (has(androidx.health.connect.client.records.WeightRecord::class)) it.weightKg else null,
                bodyFatPercentage = if (has(androidx.health.connect.client.records.BodyFatRecord::class)) it.bodyFatPercentage else null,
                leanBodyMassKg = if (has(androidx.health.connect.client.records.LeanBodyMassRecord::class)) it.leanBodyMassKg else null,
                boneMassKg = if (has(androidx.health.connect.client.records.BoneMassRecord::class)) it.boneMassKg else null,
                spo2Percentage = if (has(androidx.health.connect.client.records.OxygenSaturationRecord::class)) it.spo2Percentage else null,
                glucoseMmol = if (has(androidx.health.connect.client.records.BloodGlucoseRecord::class)) it.glucoseMmol else null,
                systolicBP = if (has(androidx.health.connect.client.records.BloodPressureRecord::class)) it.systolicBP else null,
                diastolicBP = if (has(androidx.health.connect.client.records.BloodPressureRecord::class)) it.diastolicBP else null,
                vo2Max = if (has(androidx.health.connect.client.records.Vo2MaxRecord::class)) it.vo2Max else null,
                respiratoryRate = if (has(androidx.health.connect.client.records.RespiratoryRateRecord::class)) it.respiratoryRate else null
            )
        }

        // 4. Smart Intervals cache
        val intervalsRecord = database.intervalsDao().getWellnessForDate(date)
        val shouldRefreshIntervals = intervalsRecord == null || 
                (isToday && ChronoUnit.MINUTES.between(intervalsRecord.lastUpdated, Instant.now()) > 5) ||
                (ChronoUnit.HOURS.between(intervalsRecord.lastUpdated, Instant.now()) > 24)
        
        val intervalsData = if (shouldRefreshIntervals) {
            refreshIntervalsCache(date)
        } else {
            IntervalsWellnessData(
                id = date.toString(),
                weight = intervalsRecord.weight,
                restingHR = intervalsRecord.restingHR,
                hrv = intervalsRecord.hrv,
                kcalConsumed = intervalsRecord.kcalConsumed,
                sleepSecs = intervalsRecord.sleepSecs,
                avgSleepingHR = intervalsRecord.avgSleepingHR,
                spO2 = intervalsRecord.spO2,
                systolic = intervalsRecord.systolic,
                diastolic = intervalsRecord.diastolic,
                bloodGlucose = intervalsRecord.bloodGlucose,
                bodyFat = intervalsRecord.bodyFat,
                leanMass = intervalsRecord.leanMass,
                boneMass = intervalsRecord.boneMass,
                vo2max = intervalsRecord.vo2max,
                steps = intervalsRecord.steps,
                respiration = intervalsRecord.respiration,
                carbohydrates = intervalsRecord.carbohydrates,
                protein = intervalsRecord.protein,
                fatTotal = intervalsRecord.fatTotal,
                lastUpdated = intervalsRecord.lastUpdated,
                lastSyncedAt = intervalsRecord.lastSyncedAt
            )
        }

        // 5. Build Provenance
        val provenance = DataProvenance(
            stepsSource = buildStepsProvenance(wellnessRecord?.steps),
            caloriesSource = buildCaloriesProvenance(wellnessRecord?.caloriesBurned),
            heartRateSource = buildHeartRateProvenance(wellnessRecord?.maxHR, sleep?.avgHeartRate),
            sleepSource = sleep?.sourceInfo,
            weightSource = buildWeightProvenance(wellnessRecord?.weightKg),
            bodyCompositionSource = buildBodyCompProvenance(wellnessRecord),
            hrvSource = buildHrvProvenance(wellnessRecord?.hrvMs),
            vitalsSource = buildVitalsProvenance(wellnessRecord)
        )

        return DailySummary(
            date = date,
            steps = wellnessRecord?.steps,
            caloriesBurned = wellnessRecord?.caloriesBurned,
            activeMinutes = wellnessRecord?.activeMinutes,
            sleep = sleep,
            restingHR = wellnessRecord?.restingHR,
            maxHR = wellnessRecord?.maxHR,
            hrvMs = wellnessRecord?.hrvMs,
            weightKg = wellnessRecord?.weightKg,
            bodyFatPercentage = wellnessRecord?.bodyFatPercentage,
            leanBodyMassKg = wellnessRecord?.leanBodyMassKg,
            boneMassKg = wellnessRecord?.boneMassKg,
            spo2Percentage = wellnessRecord?.spo2Percentage,
            glucoseMmol = wellnessRecord?.glucoseMmol,
            systolicBP = wellnessRecord?.systolicBP,
            diastolicBP = wellnessRecord?.diastolicBP,
            vo2Max = wellnessRecord?.vo2Max,
            respiratoryRate = wellnessRecord?.respiratoryRate,
            dataProvenance = provenance,
            intervalsWellness = intervalsData,
            grantedPermissions = permissions
        )
    }

    private fun buildStepsProvenance(steps: Int?): String? = steps?.let { "Health Connect: $it steps" }
    private fun buildCaloriesProvenance(calories: Double?): String? = if (calories == null || calories <= 0) null else "Health Connect: ${calories.toInt()} kcal active"
    private fun buildWeightProvenance(weight: Double?): String? = weight?.let { "Health Connect: ${"%.1f".format(it)} kg" }
    private fun buildHrvProvenance(hrv: Double?): String? = hrv?.let { "Health Connect: ${it.toInt()} ms (avg)" }

    private fun buildHeartRateProvenance(maxHR: Int?, sleepHR: Int?): String? {
        if (maxHR == null && sleepHR == null) return null
        val parts = mutableListOf<String>()
        sleepHR?.let { parts.add("Sleep ${it}") }
        maxHR?.let { parts.add("Max ${it}") }
        return "Health Connect: ${parts.joinToString(", ")} bpm"
    }

    private fun buildBodyCompProvenance(record: DailyWellnessRecord?): String? {
        if (record == null) return null
        val parts = mutableListOf<String>()
        record.bodyFatPercentage?.let { parts.add("Fat: ${"%.1f".format(it)}%") }
        record.leanBodyMassKg?.let { parts.add("Lean: ${"%.1f".format(it)}kg") }
        record.boneMassKg?.let { parts.add("Bone: ${"%.1f".format(it)}kg") }
        return if (parts.isNotEmpty()) "Health Connect: ${parts.joinToString(", ")}" else null
    }

    private fun buildVitalsProvenance(record: DailyWellnessRecord?): String? {
        if (record == null) return null
        val parts = mutableListOf<String>()
        record.spo2Percentage?.let { parts.add("SpO2: ${it.toInt()}%") }
        record.glucoseMmol?.let { parts.add("Glucose: ${"%.1f".format(it)}") }
        record.vo2Max?.let { parts.add("VO2 Max: ${"%.1f".format(it)}") }
        record.respiratoryRate?.let { parts.add("Resp: ${it.toInt()}rpm") }
        return if (parts.isNotEmpty()) "Health Connect: ${parts.joinToString(", ")}" else null
    }
}
