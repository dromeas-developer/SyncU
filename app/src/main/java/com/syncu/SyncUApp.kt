package com.syncu

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * SyncU Application Class
 */
class SyncUApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        )
    }
}
