package com.bedir.yanki.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.bedir.yanki.R
import com.bedir.yanki.data.remote.mesh.connectivity.BleMeshManager
import com.bedir.yanki.repository.YankiRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint
class MeshService : Service() {

    @Inject lateinit var bleMeshManager: BleMeshManager
    @Inject lateinit var repository: YankiRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var settingsJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "YANKI_MESH_CHANNEL")
            .setContentTitle("YANKI Aktif")
            .setContentText("Çevrimdışı ağ taraması ve veri aktarımı devam ediyor...")
            .setSmallIcon(android.R.drawable.stat_notify_sync) 
            .setOngoing(true)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(1, notification)
        }

        // Ayarları dinle ve motoru dinamik olarak yönet
        observeSettings()

        return START_STICKY
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            repository.getSettingsFlow().collect { settings ->
                val bleEnabled = settings["pref_ble_mode"] ?: true
                val wifiEnabled = settings["pref_wifi_aware"] ?: true

                // BLE Yönetimi
                if (bleEnabled) {
                    if (!bleMeshManager.isScanning.value && !bleMeshManager.isAdvertising.value) {
                        startMeshEngine()
                    }
                } else {
                    bleMeshManager.stopMesh()
                }

                // Wi-Fi Aware Yönetimi
                if (wifiEnabled) {
                    repository.wifiAwareManager.attachToAwareSystem {
                        repository.wifiAwareManager.startPublishing()
                        repository.wifiAwareManager.startSubscribing()
                    }
                } else {
                    repository.wifiAwareManager.stopAware()
                }
            }
        }
    }

    private fun startMeshEngine() {
        repository.startListeningForMeshPayloads(serviceScope)

        bleMeshManager.startScanning { neighborId, address, rssi ->
            serviceScope.launch {
                repository.handleNeighborFound(neighborId, address, rssi)
            }
        }

        bleMeshManager.startAdvertising(repository.currentUserId)
        bleMeshManager.startGattServer()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "YANKI_MESH_CHANNEL",
            "YANKI Mesh Servisi",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        bleMeshManager.stopMesh()
    }
}