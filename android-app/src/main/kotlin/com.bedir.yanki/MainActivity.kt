package com.bedir.yanki

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bedir.yanki.ui.navigation.MainScreen
import com.bedir.yanki.ui.permissions.PermissionHandler
import com.bedir.yanki.ui.theme.YankiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        schedulePeriodicSync()
        
        setContent {
            YankiTheme {
                PermissionHandler { MainScreen()}
            }
        }
    }

    private fun schedulePeriodicSync() {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.bedir.yanki.data.remote.sync.SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_yanki_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
