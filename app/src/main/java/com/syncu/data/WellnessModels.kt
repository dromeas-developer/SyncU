package com.syncu.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate

/**
 * Cached daily wellness data from Health Connect
 */
@Entity(tableName = "daily_wellness_records")
data class DailyWellnessRecord(
    @PrimaryKey
    val date: LocalDate,
    val steps: Int? = null,
    val caloriesBurned: Double? = null,
    val activeMinutes: Int? = null,
    val restingHR: Int? = null,
    val maxHR: Int? = null,
    val hrvMs: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val leanBodyMassKg: Double? = null,
    val boneMassKg: Double? = null,
    val spo2Percentage: Double? = null,
    val glucoseMmol: Double? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val vo2Max: Double? = null,
    val respiratoryRate: Double? = null,
    val lastUpdated: Instant = Instant.now()
)

/**
 * Cached daily wellness data from Intervals.icu
 */
@Entity(tableName = "intervals_wellness_records")
data class IntervalsWellnessRecord(
    @PrimaryKey
    val date: LocalDate,
    val weight: Double? = null,
    val restingHR: Int? = null,
    val hrv: Double? = null,
    val kcalConsumed: Int? = null,
    val sleepSecs: Int? = null,
    val avgSleepingHR: Double? = null,
    val spO2: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val bloodGlucose: Double? = null,
    val bodyFat: Double? = null,
    val leanMass: Double? = null,
    val boneMass: Double? = null,
    val vo2max: Double? = null,
    val steps: Int? = null,
    val respiration: Double? = null,
    val carbohydrates: Double? = null,
    val protein: Double? = null,
    val fatTotal: Double? = null,
    val lastUpdated: Instant = Instant.now(),
    val lastSyncedAt: Instant? = null // When we actually pushed data TO intervals
)

/**
 * Heart Rate Variability (HRV) data
 */
@Entity(tableName = "hrv_records")
data class HRVRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val hrvMs: Double,  // HRV in milliseconds (RMSSD)
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Weight measurements
 */
@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey
    val id: String,
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPercentage: Double? = null,
    val muscleMassKg: Double? = null,
    val boneMassKg: Double? = null,
    val bodyWaterPercentage: Double? = null,
    val visceralFat: Int? = null,
    val bmr: Int? = null,  // Basal Metabolic Rate
    val bmi: Double? = null,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Resting Heart Rate
 */
@Entity(tableName = "resting_hr_records")
data class RestingHRRecord(
    @PrimaryKey
    val id: String,
    val date: LocalDate,
    val restingHR: Int,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Blood Pressure
 */
@Entity(tableName = "blood_pressure_records")
data class BloodPressureRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Blood Glucose
 */
@Entity(tableName = "blood_glucose_records")
data class BloodGlucoseRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val glucoseMmol: Double,  // mmol/L
    val mealType: MealType? = null,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

enum class MealType {
    FASTING,
    BEFORE_MEAL,
    AFTER_MEAL,
    GENERAL
}

/**
 * Hydration / Water intake
 */
@Entity(tableName = "hydration_records")
data class HydrationRecord(
    @PrimaryKey
    val id: String,
    val date: LocalDate,
    val volumeMl: Double,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Nutrition / Macros
 */
@Entity(tableName = "nutrition_records")
data class NutritionRecord(
    @PrimaryKey
    val id: String,
    val date: LocalDate,
    val caloriesKcal: Int? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Oxygen Saturation (SpO2)
 */
@Entity(tableName = "oxygen_saturation_records")
data class OxygenSaturationRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val spo2Percentage: Double,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Body Temperature
 */
@Entity(tableName = "body_temperature_records")
data class BodyTemperatureRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val temperatureCelsius: Double,
    val measurementLocation: TemperatureLocation? = null,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

enum class TemperatureLocation {
    ORAL,
    ARMPIT,
    FOREHEAD,
    RECTAL,
    TEMPORAL_ARTERY,
    EAR,
    WRIST,
    GENERAL
}

/**
 * Respiratory Rate
 */
@Entity(tableName = "respiratory_rate_records")
data class RespiratoryRateRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Instant,
    val breathsPerMinute: Double,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

/**
 * Menstruation (for female athletes)
 */
@Entity(tableName = "menstruation_records")
data class MenstruationRecord(
    @PrimaryKey
    val id: String,
    val date: LocalDate,
    val flow: MenstrualFlow,
    val source: String = "Health Connect",
    val synced: Boolean = false
)

enum class MenstrualFlow {
    LIGHT,
    MEDIUM,
    HEAVY,
    SPOTTING
}

/**
 * Comprehensive daily wellness summary
 */
data class DailyWellnessSummary(
    val date: LocalDate,
    val steps: Int? = null,
    val caloriesBurned: Double? = null,
    val activeMinutes: Int? = null,
    val sleepMinutes: Long? = null,
    val deepSleepMinutes: Long? = null,
    val remSleepMinutes: Long? = null,
    val lightSleepMinutes: Long? = null,
    val restingHR: Int? = null,
    val maxHR: Int? = null,
    val hrvMs: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val boneMassKg: Double? = null,
    val leanBodyMassKg: Double? = null,
    val hipCircumferenceCm: Double? = null,
    val waistCircumferenceCm: Double? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val spo2: Double? = null,
    val vo2Max: Double? = null, // Replaced temperature with vo2Max
    val respiratoryRate: Double? = null,
    val caloriesConsumed: Int? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val hydrationMl: Double? = null,
    val avgGlucoseMmol: Double? = null,
    val activities: List<Activity> = emptyList()
)

/**
 * intervals.icu wellness data format
 * Matching the provided schema + common body comp fields
 */
data class IntervalsWellnessData(
    val id: String,  // YYYY-MM-DD
    val weight: Double? = null,
    val restingHR: Int? = null,
    val hrv: Double? = null,
    val hrvSDNN: Double? = null,
    
    @SerializedName("kcalConsumed")
    val kcalConsumed: Int? = null,
    
    val sleepSecs: Int? = null,
    val sleepScore: Double? = null,
    val sleepQuality: Int? = null,
    val avgSleepingHR: Double? = null,
    val soreness: Int? = null,
    val fatigue: Int? = null,
    val stress: Int? = null,
    val mood: Int? = null,
    val motivation: Int? = null,
    val injury: Int? = null,
    val spO2: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val hydration: Int? = null,
    val hydrationVolume: Double? = null,
    val readiness: Double? = null,
    val baevskySI: Double? = null,
    val bloodGlucose: Double? = null,
    val lactate: Double? = null,
    val bodyFat: Double? = null,
    val leanMass: Double? = null,
    val boneMass: Double? = null,
    val muscleMass: Double? = null,
    val abdomen: Double? = null,
    val vo2max: Double? = null,
    val comments: String? = null,
    val steps: Int? = null,
    
    @SerializedName("respiration")
    val respiration: Double? = null,
    
    val carbohydrates: Double? = null,
    val protein: Double? = null,
    val fatTotal: Double? = null,
    val locked: Boolean? = null,
    
    // Metadata
    var lastUpdated: Instant? = null,
    var lastSyncedAt: Instant? = null
)

/**
 * Wellness Summary for a Day
 */
data class WellnessSummaryDay(
    val date: LocalDate,
    val hrvMs: Double? = null,
    val restingHR: Int? = null,
    val maxHR: Int? = null,
    val weightKg: Double? = null,
    val bodyFatPercentage: Double? = null,
    val spo2Percentage: Double? = null,
    val glucoseMmol: Double? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val vo2Max: Double? = null, // Replaced bodyTemp with vo2Max
    val respiratoryRate: Double? = null,
    val boneMassKg: Double? = null,
    val hipCircumferenceCm: Double? = null,
    val waistCircumferenceCm: Double? = null,
    val leanBodyMassKg: Double? = null
)
