package com.syncu.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.syncu.R
import com.syncu.api.HealthConnectManager
import com.syncu.api.ExtendedHealthConnectManager
import com.syncu.api.IntervalsWellnessApiClient
import com.syncu.data.AppDatabase
import com.syncu.data.DatabaseHelper
import com.syncu.data.DailySummary
import com.syncu.utils.SecureCredentialsManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Day Fragment with Side-by-Side Health Connect and Intervals data
 * Supports smart caching and manual refresh
 */
class DayFragment : Fragment() {

    private var date: LocalDate = LocalDate.now()
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var sleepChartView: SleepChartView
    private var currentSummary: DailySummary? = null

    // Cards
    private lateinit var cardActivity: View
    private lateinit var cardVitals: View
    private lateinit var cardHeart: View
    private lateinit var cardBody: View
    private lateinit var cardSleep: View

    // Rows
    private lateinit var rowSteps: View
    private lateinit var rowCalories: View
    private lateinit var rowSystolic: View
    private lateinit var rowDiastolic: View
    private lateinit var rowSpO2: View
    private lateinit var rowBloodGlucose: View
    private lateinit var rowVO2Max: View
    private lateinit var rowRespiratoryRate: View
    private lateinit var rowRestingHR: View
    private lateinit var rowSleepHR: View
    private lateinit var rowHRV: View
    private lateinit var rowWeight: View
    private lateinit var rowBodyFat: View
    private lateinit var rowLeanBodyMass: View
    private lateinit var rowBoneMass: View

    // Text Views
    private lateinit var tvSteps: TextView
    private lateinit var tvStepsIntervals: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvCaloriesIntervals: TextView
    private lateinit var tvSystolic: TextView
    private lateinit var tvSystolicIntervals: TextView
    private lateinit var tvDiastolic: TextView
    private lateinit var tvDiastolicIntervals: TextView
    private lateinit var tvSpO2: TextView
    private lateinit var tvSpO2Intervals: TextView
    private lateinit var tvBloodGlucose: TextView
    private lateinit var tvBloodGlucoseIntervals: TextView
    private lateinit var tvVO2Max: TextView
    private lateinit var tvVO2MaxIntervals: TextView
    private lateinit var tvRespiratoryRate: TextView
    private lateinit var tvRespiratoryRateIntervals: TextView
    private lateinit var tvRestingHR: TextView
    private lateinit var tvRestingHRIntervals: TextView
    private lateinit var tvSleepHR: TextView
    private lateinit var tvSleepHRIntervals: TextView
    private lateinit var tvHRV: TextView
    private lateinit var tvHRVIntervals: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvWeightIntervals: TextView
    private lateinit var tvBodyFat: TextView
    private lateinit var tvBodyFatIntervals: TextView
    private lateinit var tvLeanBodyMass: TextView
    private lateinit var tvLeanBodyMassIntervals: TextView
    private lateinit var tvBoneMass: TextView
    private lateinit var tvBoneMassIntervals: TextView

    // Sleep Text Views
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepDurationIntervals: TextView
    private lateinit var tvSleepAsleep: TextView
    private lateinit var tvSleepAsleepIntervals: TextView
    private lateinit var tvSleepAwakeDetail: TextView
    private lateinit var tvSleepAwakePercent: TextView
    private lateinit var tvSleepREMDetail: TextView
    private lateinit var tvSleepREMPercent: TextView
    private lateinit var tvSleepLightDetail: TextView
    private lateinit var tvSleepLightPercent: TextView
    private lateinit var tvSleepDeepDetail: TextView
    private lateinit var tvSleepDeepPercent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val dateString = it.getString(ARG_DATE)
            dateString?.let { str ->
                try {
                    date = LocalDate.parse(str)
                } catch (e: Exception) {
                    Log.e("DayFragment", "Error parsing date: $str", e)
                }
            }
        }

        parentFragmentManager.setFragmentResultListener("refresh_data", this) { _, _ ->
            loadDayData(forceRefresh = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_day, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initializeViews(view)
            setupExpandCollapse(view)
            setupSwipeRefresh()
            loadDayData(forceRefresh = false) // Use cache on initial swipe
        } catch (e: Exception) {
            Log.e("DayFragment", "Error in onViewCreated", e)
        }
    }

    private fun initializeViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Cards
        cardActivity = view.findViewById(R.id.cardActivity)
        cardVitals = view.findViewById(R.id.cardVitals)
        cardHeart = view.findViewById(R.id.cardHeart)
        cardBody = view.findViewById(R.id.cardBody)
        cardSleep = view.findViewById(R.id.cardSleep)

        // Activity Card
        tvSteps = view.findViewById(R.id.tvSteps)
        tvStepsIntervals = view.findViewById(R.id.tvStepsIntervals)
        tvCalories = view.findViewById(R.id.tvCalories)
        tvCaloriesIntervals = view.findViewById(R.id.tvCaloriesIntervals)
        rowSteps = tvSteps.parent as View
        rowCalories = tvCalories.parent as View

        // Vitals Card
        tvSystolic = view.findViewById(R.id.tvSystolic)
        tvSystolicIntervals = view.findViewById(R.id.tvSystolicIntervals)
        tvDiastolic = view.findViewById(R.id.tvDiastolic)
        tvDiastolicIntervals = view.findViewById(R.id.tvDiastolicIntervals)
        tvSpO2 = view.findViewById(R.id.tvSpO2)
        tvSpO2Intervals = view.findViewById(R.id.tvSpO2Intervals)
        tvBloodGlucose = view.findViewById(R.id.tvBloodGlucose)
        tvBloodGlucoseIntervals = view.findViewById(R.id.tvBloodGlucoseIntervals)
        tvVO2Max = view.findViewById(R.id.tvVO2Max)
        tvVO2MaxIntervals = view.findViewById(R.id.tvVO2MaxIntervals)
        tvRespiratoryRate = view.findViewById(R.id.tvRespiratoryRate)
        tvRespiratoryRateIntervals = view.findViewById(R.id.tvRespiratoryRateIntervals)
        rowSystolic = tvSystolic.parent as View
        rowDiastolic = tvDiastolic.parent as View
        rowSpO2 = tvSpO2.parent as View
        rowBloodGlucose = tvBloodGlucose.parent as View
        rowVO2Max = tvVO2Max.parent as View
        rowRespiratoryRate = tvRespiratoryRate.parent as View

        // Heart Card
        tvRestingHR = view.findViewById(R.id.tvRestingHR)
        tvRestingHRIntervals = view.findViewById(R.id.tvRestingHRIntervals)
        tvSleepHR = view.findViewById(R.id.tvSleepHR)
        tvSleepHRIntervals = view.findViewById(R.id.tvSleepHRIntervals)
        tvHRV = view.findViewById(R.id.tvHRV)
        tvHRVIntervals = view.findViewById(R.id.tvHRVIntervals)
        rowRestingHR = tvRestingHR.parent as View
        rowSleepHR = tvSleepHR.parent as View
        rowHRV = tvHRV.parent as View

        // Body Card
        tvWeight = view.findViewById(R.id.tvWeight)
        tvWeightIntervals = view.findViewById(R.id.tvWeightIntervals)
        tvBodyFat = view.findViewById(R.id.tvBodyFat)
        tvBodyFatIntervals = view.findViewById(R.id.tvBodyFatIntervals)
        tvLeanBodyMass = view.findViewById(R.id.tvLeanBodyMass)
        tvLeanBodyMassIntervals = view.findViewById(R.id.tvLeanBodyMassIntervals)
        tvBoneMass = view.findViewById(R.id.tvBoneMass)
        tvBoneMassIntervals = view.findViewById(R.id.tvBoneMassIntervals)
        rowWeight = tvWeight.parent as View
        rowBodyFat = tvBodyFat.parent as View
        rowLeanBodyMass = tvLeanBodyMass.parent as View
        rowBoneMass = tvBoneMass.parent as View

        // Sleep Card
        sleepChartView = view.findViewById(R.id.sleepChartView)
        tvSleepDuration = view.findViewById(R.id.tvSleepDuration)
        tvSleepDurationIntervals = view.findViewById(R.id.tvSleepDurationIntervals)
        tvSleepAsleep = view.findViewById(R.id.tvSleepAsleep)
        tvSleepAsleepIntervals = view.findViewById(R.id.tvSleepAsleepIntervals)
        
        // Detailed Sleep Views
        tvSleepAwakeDetail = view.findViewById(R.id.tvSleepAwakeDetail)
        tvSleepAwakePercent = view.findViewById(R.id.tvSleepAwakePercent)
        tvSleepREMDetail = view.findViewById(R.id.tvSleepREMDetail)
        tvSleepREMPercent = view.findViewById(R.id.tvSleepREMPercent)
        tvSleepLightDetail = view.findViewById(R.id.tvSleepLightDetail)
        tvSleepLightPercent = view.findViewById(R.id.tvSleepLightPercent)
        tvSleepDeepDetail = view.findViewById(R.id.tvSleepDeepDetail)
        tvSleepDeepPercent = view.findViewById(R.id.tvSleepDeepPercent)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener { loadDayData(forceRefresh = true) }
    }

    private fun setupExpandCollapse(view: View) {
        view.findViewById<View>(R.id.headerActivity).setOnClickListener { toggleCard(view.findViewById(R.id.contentActivity), view.findViewById(R.id.iconActivity)) }
        view.findViewById<View>(R.id.headerVitals).setOnClickListener { toggleCard(view.findViewById(R.id.contentVitals), view.findViewById(R.id.iconVitals)) }
        view.findViewById<View>(R.id.headerHeart).setOnClickListener { toggleCard(view.findViewById(R.id.contentHeart), view.findViewById(R.id.iconHeart)) }
        view.findViewById<View>(R.id.headerBody).setOnClickListener { toggleCard(view.findViewById(R.id.contentBody), view.findViewById(R.id.iconBody)) }
        view.findViewById<View>(R.id.headerSleep).setOnClickListener { toggleCard(view.findViewById(R.id.contentSleep), view.findViewById(R.id.iconSleep)) }
    }

    fun syncDataToIntervals() {
        val summary = currentSummary ?: return
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val credentialsManager = SecureCredentialsManager(requireContext())
                val apiKey = credentialsManager.getApiKey()
                val athleteId = credentialsManager.getAthleteId()
                if (apiKey != null && athleteId != null) {
                    val client = IntervalsWellnessApiClient(apiKey, athleteId)
                    val result = client.uploadWellnessData(summary)
                    if (result.isSuccess) {
                        Toast.makeText(requireContext(), "Synced!", Toast.LENGTH_SHORT).show()
                        
                        // Update local DB lastSyncedAt timestamp manually
                        val database = AppDatabase.getDatabase(requireContext(), charArrayOf())
                        database.intervalsDao().updateLastSyncedAt(date, Instant.now())
                        
                        loadDayData(forceRefresh = true)
                    }
                }
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("DayFragment", "Sync error", e)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleCard(content: View, icon: ImageView) {
        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            icon.setImageResource(android.R.drawable.arrow_down_float)
        } else {
            content.visibility = View.VISIBLE
            icon.setImageResource(android.R.drawable.arrow_up_float)
        }
    }

    fun refreshData() {
        loadDayData(forceRefresh = false)
    }

    /**
     * Called by MainActivity to ensure the sync button enabled state is correct
     */
    fun refreshSyncButtonStatus() {
        if (!isAdded || isDetached) return
        
        val summary = currentSummary ?: return
        val intervals = summary.intervalsWellness

        // 1. Identify what HC actually has. We only sync non-zero/non-null data.
        val hcSleepMinutes = (summary.sleep?.asleepDurationMinutes ?: summary.sleep?.durationMinutes ?: 0L).toInt()
        
        fun isMeaningful(n: Number?): Boolean {
            val d = n?.toDouble() ?: 0.0
            return d > 0.05 // Avoid tiny noise
        }

        val hcHasData = isMeaningful(summary.steps) || 
                        isMeaningful(summary.restingHR) || 
                        isMeaningful(summary.hrvMs) || 
                        isMeaningful(summary.weightKg) || 
                        hcSleepMinutes > 0 ||
                        isMeaningful(summary.systolicBP) ||
                        isMeaningful(summary.caloriesBurned) ||
                        isMeaningful(summary.spo2Percentage) ||
                        isMeaningful(summary.glucoseMmol) ||
                        isMeaningful(summary.vo2Max) ||
                        isMeaningful(summary.respiratoryRate)

        if (!hcHasData) {
            (activity as? MainActivity)?.setSyncButtonEnabled(false)
            return
        }

        if (intervals == null) {
            (activity as? MainActivity)?.setSyncButtonEnabled(true)
            return
        }

        // 2. Compare only what HC is providing.
        
        fun hasDiff(hc: Number?, int: Number?, isInt: Boolean = true): Boolean {
            if (!isMeaningful(hc)) return false // HC has nothing to provide
            
            val hcVal = hc!!.toDouble()
            val intVal = int?.toDouble() ?: 0.0
            
            return if (isInt) {
                hcVal.toInt() != intVal.toInt()
            } else {
                abs(hcVal - intVal) > 0.05
            }
        }

        val diffs = mutableMapOf<String, Boolean>()
        diffs["steps"] = hasDiff(summary.steps, intervals.steps)
        diffs["kcal"] = hasDiff(summary.caloriesBurned, intervals.kcalConsumed)
        diffs["restHR"] = hasDiff(summary.restingHR, intervals.restingHR)
        diffs["hrv"] = hasDiff(summary.hrvMs, intervals.hrv, false)
        diffs["weight"] = hasDiff(summary.weightKg, intervals.weight, false)
        diffs["fat"] = hasDiff(summary.bodyFatPercentage, intervals.bodyFat, false)
        diffs["systolic"] = hasDiff(summary.systolicBP, intervals.systolic)
        diffs["diastolic"] = hasDiff(summary.diastolicBP, intervals.diastolic)
        diffs["spo2"] = hasDiff(summary.spo2Percentage, intervals.spO2)
        diffs["glucose"] = hasDiff(summary.glucoseMmol, intervals.bloodGlucose, false)
        diffs["vo2"] = hasDiff(summary.vo2Max, intervals.vo2max, false)
        diffs["resp"] = hasDiff(summary.respiratoryRate, intervals.respiration, false)
        diffs["sleepHR"] = hasDiff(summary.sleep?.avgHeartRate, intervals.avgSleepingHR)
        
        val intSleepMinutes = (intervals.sleepSecs ?: 0) / 60
        diffs["sleep"] = hcSleepMinutes > 0 && hcSleepMinutes != intSleepMinutes

        val anyDiff = diffs.values.any { it }
        
        (activity as? MainActivity)?.setSyncButtonEnabled(anyDiff)
    }

    private fun loadDayData(forceRefresh: Boolean) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                swipeRefresh.isRefreshing = false
                val database = AppDatabase.getDatabase(requireContext(), charArrayOf())
                val dbHelper = DatabaseHelper(requireContext(), database, HealthConnectManager(requireContext()), ExtendedHealthConnectManager(requireContext()))
                
                if (forceRefresh) {
                    dbHelper.loadDataForDate(date)
                }
                
                val summary = dbHelper.getDailySummary(date)
                currentSummary = summary
                displaySummary(summary)
                
                // Update activity's sync timestamp text based on lastSyncedAt
                updateActivitySyncTimestamp()
                refreshSyncButtonStatus()
                
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("DayFragment", "Error loading data", e)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateActivitySyncTimestamp() {
        // Only update if this fragment is currently visible to the user
        if (!isAdded || isDetached) return
        
        val summary = currentSummary ?: return
        
        // Use lastSyncedAt (pushed data) instead of lastUpdated (fetched data)
        val lastSynced = summary.intervalsWellness?.lastSyncedAt
        
        if (lastSynced != null) {
            val dateTime = lastSynced.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
            (activity as? MainActivity)?.setLastSyncText("Last sync: ${dateTime.format(formatter)}")
        } else {
            (activity as? MainActivity)?.setLastSyncText("Last sync: Never")
        }
    }

    private fun formatMinutes(minutes: Long?): String {
        if (minutes == null) return "--"
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    private fun displaySummary(summary: DailySummary) {
        val intervals = summary.intervalsWellness
        val granted = summary.grantedPermissions

        // Helper to check permission string
        fun hasPerm(recordType: kotlin.reflect.KClass<out Record>): Boolean {
            val permStr = HealthPermission.getReadPermission(recordType)
            return granted.contains(permStr)
        }

        // Helper to check if a value is meaningful
        fun isVal(v: Any?): Boolean {
            return v != null && v != 0 && v != 0.0
        }

        // Activity Card
        rowSteps.visibility = if (hasPerm(StepsRecord::class) || isVal(intervals?.steps)) View.VISIBLE else View.GONE
        rowCalories.visibility = if (hasPerm(ActiveCaloriesBurnedRecord::class) || isVal(intervals?.kcalConsumed)) View.VISIBLE else View.GONE
        cardActivity.visibility = if (rowSteps.visibility == View.VISIBLE || rowCalories.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        
        tvSteps.text = if (hasPerm(StepsRecord::class)) (summary.steps?.toString() ?: "--") else "n.a."
        tvStepsIntervals.text = intervals?.steps?.toString() ?: "--"
        tvCalories.text = if (hasPerm(ActiveCaloriesBurnedRecord::class)) (summary.caloriesBurned?.roundToInt()?.toString() ?: "--") else "n.a."
        tvCaloriesIntervals.text = intervals?.kcalConsumed?.toString() ?: "--"

        // Vitals Card
        rowSystolic.visibility = if (hasPerm(BloodPressureRecord::class) || isVal(intervals?.systolic)) View.VISIBLE else View.GONE
        rowDiastolic.visibility = if (hasPerm(BloodPressureRecord::class) || isVal(intervals?.diastolic)) View.VISIBLE else View.GONE
        rowSpO2.visibility = if (hasPerm(OxygenSaturationRecord::class) || isVal(intervals?.spO2)) View.VISIBLE else View.GONE
        rowBloodGlucose.visibility = if (hasPerm(BloodGlucoseRecord::class) || isVal(intervals?.bloodGlucose)) View.VISIBLE else View.GONE
        rowVO2Max.visibility = if (hasPerm(Vo2MaxRecord::class) || isVal(intervals?.vo2max)) View.VISIBLE else View.GONE
        rowRespiratoryRate.visibility = if (hasPerm(RespiratoryRateRecord::class) || isVal(intervals?.respiration)) View.VISIBLE else View.GONE

        val vitalsVisible = rowSystolic.visibility == View.VISIBLE || rowDiastolic.visibility == View.VISIBLE ||
                          rowSpO2.visibility == View.VISIBLE || rowBloodGlucose.visibility == View.VISIBLE ||
                          rowVO2Max.visibility == View.VISIBLE || rowRespiratoryRate.visibility == View.VISIBLE
        cardVitals.visibility = if (vitalsVisible) View.VISIBLE else View.GONE

        tvSystolic.text = if (hasPerm(BloodPressureRecord::class)) (summary.systolicBP?.toString() ?: "--") else "n.a."
        tvSystolicIntervals.text = intervals?.systolic?.toString() ?: "--"
        tvDiastolic.text = if (hasPerm(BloodPressureRecord::class)) (summary.diastolicBP?.toString() ?: "--") else "n.a."
        tvDiastolicIntervals.text = intervals?.diastolic?.toString() ?: "--"
        tvSpO2.text = if (hasPerm(OxygenSaturationRecord::class)) (summary.spo2Percentage?.roundToInt()?.toString() ?: "--") else "n.a."
        tvSpO2Intervals.text = intervals?.spO2?.roundToInt()?.toString() ?: "--"
        tvBloodGlucose.text = if (hasPerm(BloodGlucoseRecord::class)) (summary.glucoseMmol?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvBloodGlucoseIntervals.text = intervals?.bloodGlucose?.let { "%.1f".format(it) } ?: "--"
        tvVO2Max.text = if (hasPerm(Vo2MaxRecord::class)) (summary.vo2Max?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvVO2MaxIntervals.text = intervals?.vo2max?.let { "%.1f".format(it) } ?: "--"
        tvRespiratoryRate.text = if (hasPerm(RespiratoryRateRecord::class)) (summary.respiratoryRate?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvRespiratoryRateIntervals.text = intervals?.respiration?.let { "%.1f".format(it) } ?: "--"

        // Heart Card
        rowRestingHR.visibility = if (hasPerm(RestingHeartRateRecord::class) || isVal(intervals?.restingHR)) View.VISIBLE else View.GONE
        rowSleepHR.visibility = if (hasPerm(HeartRateRecord::class) || isVal(intervals?.avgSleepingHR)) View.VISIBLE else View.GONE
        rowHRV.visibility = if (hasPerm(HeartRateVariabilityRmssdRecord::class) || isVal(intervals?.hrv)) View.VISIBLE else View.GONE
        cardHeart.visibility = if (rowRestingHR.visibility == View.VISIBLE || rowSleepHR.visibility == View.VISIBLE || rowHRV.visibility == View.VISIBLE) View.VISIBLE else View.GONE

        tvRestingHR.text = if (hasPerm(RestingHeartRateRecord::class)) (summary.restingHR?.toString() ?: "--") else "n.a."
        tvRestingHRIntervals.text = intervals?.restingHR?.toString() ?: "--"
        tvSleepHR.text = if (hasPerm(HeartRateRecord::class)) (summary.sleep?.avgHeartRate?.toString() ?: "--") else "n.a."
        tvSleepHRIntervals.text = intervals?.avgSleepingHR?.toInt()?.toString() ?: "--"
        tvHRV.text = if (hasPerm(HeartRateVariabilityRmssdRecord::class)) (summary.hrvMs?.roundToInt()?.toString() ?: "--") else "n.a."
        tvHRVIntervals.text = intervals?.hrv?.roundToInt()?.toString() ?: "--"

        // Body Card
        rowWeight.visibility = if (hasPerm(WeightRecord::class) || isVal(intervals?.weight)) View.VISIBLE else View.GONE
        rowBodyFat.visibility = if (hasPerm(BodyFatRecord::class) || isVal(intervals?.bodyFat)) View.VISIBLE else View.GONE
        rowLeanBodyMass.visibility = if (hasPerm(LeanBodyMassRecord::class) || isVal(intervals?.leanMass)) View.VISIBLE else View.GONE
        rowBoneMass.visibility = if (hasPerm(BoneMassRecord::class) || isVal(intervals?.boneMass)) View.VISIBLE else View.GONE
        cardBody.visibility = if (rowWeight.visibility == View.VISIBLE || rowBodyFat.visibility == View.VISIBLE || 
                                 rowLeanBodyMass.visibility == View.VISIBLE || rowBoneMass.visibility == View.VISIBLE) View.VISIBLE else View.GONE

        tvWeight.text = if (hasPerm(WeightRecord::class)) (summary.weightKg?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvWeightIntervals.text = intervals?.weight?.let { "%.1f".format(it) } ?: "--"
        tvBodyFat.text = if (hasPerm(BodyFatRecord::class)) (summary.bodyFatPercentage?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvBodyFatIntervals.text = intervals?.bodyFat?.let { "%.1f".format(it) } ?: "--"
        tvLeanBodyMass.text = if (hasPerm(LeanBodyMassRecord::class)) (summary.leanBodyMassKg?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvLeanBodyMassIntervals.text = intervals?.leanMass?.let { "%.1f".format(it) } ?: "--"
        tvBoneMass.text = if (hasPerm(BoneMassRecord::class)) (summary.boneMassKg?.let { "%.1f".format(it) } ?: "--") else "n.a."
        tvBoneMassIntervals.text = intervals?.boneMass?.let { "%.1f".format(it) } ?: "--"

        // Sleep Card
        val sleepHasData = summary.sleep != null || (intervals?.sleepSecs ?: 0) > 0
        cardSleep.visibility = if (hasPerm(SleepSessionRecord::class) || sleepHasData) View.VISIBLE else View.GONE
        
        summary.sleep?.let { sleep ->
            tvSleepDuration.text = formatMinutes(sleep.durationMinutes)
            tvSleepDurationIntervals.text = "n.a."
            
            tvSleepAsleep.text = formatMinutes(sleep.asleepDurationMinutes)
            tvSleepAsleepIntervals.text = intervals?.sleepSecs?.let { formatMinutes((it / 60).toLong()) } ?: "--"
            
            val total = sleep.durationMinutes.toDouble()
            if (total > 0) {
                tvSleepAwakeDetail.text = formatMinutes(sleep.awakeSleepMinutes)
                tvSleepAwakePercent.text = "${((sleep.awakeSleepMinutes ?: 0) * 100 / total).roundToInt()}%"
                
                tvSleepREMDetail.text = formatMinutes(sleep.remSleepMinutes)
                tvSleepREMPercent.text = "${((sleep.remSleepMinutes ?: 0) * 100 / total).roundToInt()}%"
                
                tvSleepLightDetail.text = formatMinutes(sleep.lightSleepMinutes)
                tvSleepLightPercent.text = "${((sleep.lightSleepMinutes ?: 0) * 100 / total).roundToInt()}%"
                
                tvSleepDeepDetail.text = formatMinutes(sleep.deepSleepMinutes)
                tvSleepDeepPercent.text = "${((sleep.deepSleepMinutes ?: 0) * 100 / total).roundToInt()}%"
                
                sleepChartView.setData(sleep.stageIntervals, sleep.startTime.toEpochMilli(), sleep.endTime.toEpochMilli())
            }
        }
    }

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: LocalDate): DayFragment {
            val fragment = DayFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date.toString())
            fragment.arguments = args
            return fragment
        }
    }
}
