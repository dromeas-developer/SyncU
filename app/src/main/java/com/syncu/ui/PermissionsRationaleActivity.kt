package com.syncu.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.syncu.R

/**
 * Permissions Rationale Activity
 * Shows explanation for Health Connect permissions when user clicks privacy policy
 */
class PermissionsRationaleActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_rationale)
        
        val tvRationale = findViewById<TextView>(R.id.tvRationale)
        tvRationale.text = buildString {
            append("Health Connect Permissions\n\n")
            append("SyncU requires access to your health data to sync with intervals.icu:\n\n")
            append("• Steps - Track your daily activity\n")
            append("• Distance - Monitor running and cycling distance\n")
            append("• Exercise Sessions - Record workouts\n")
            append("• Heart Rate - Track cardiovascular data\n")
            append("• Sleep - Monitor sleep quality\n")
            append("• Calories - Track energy expenditure\n")
            append("• Weight - Monitor body metrics\n")
            append("• HRV - Heart rate variability for recovery\n\n")
            append("Your privacy is our priority:\n")
            append("• Data is stored locally on your device\n")
            append("• Only synced to intervals.icu (your choice)\n")
            append("• No third-party analytics or tracking\n")
            append("• You control what data is synced\n")
        }
    }
}
