package com.syncu

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import android.util.Log

/**
 * SyncU Application Class
 */
class SyncUApp : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        Log.i("SyncUApp", "Application created")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
