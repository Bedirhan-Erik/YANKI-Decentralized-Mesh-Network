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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "YANKI_MESH_CHANNEL")
            .setContentTitle("YANKI Aktif")
            .setContentText("Çevrimdışı ağ taraması ve veri aktarımı devam ediyor...")
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Geçici ikon
            .setOngoing(true)
            .build()

        // Servisi "Foreground" olarak başlat (Android 14+ için tip belirtmek gerekebilir)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(1, notification)
        }

        // MESH MOTORUNU ÇALIŞTIR
        startMeshEngine()

        return START_STICKY // Sistem servisi kapatırsa otomatik yeniden başlatır
    }

    private fun startMeshEngine() {
        // 0. Payload dinlemeyi başlat
        repository.startListeningForMeshPayloads()

        // 1. BLE Taramasını Başlat
        bleMeshManager.startScanning { neighborId, address ->
            serviceScope.launch {
                // Komşuyu bulduğunda Repository'deki trafik polisini tetikle
                repository.handleNeighborFound(neighborId, address)
            }
        }

        // 2. BLE Yayınını Başlat (Repository'den gelen gerçek ID'yi kullan)
        bleMeshManager.startAdvertising(repository.currentUserId)

        // 3. GATT Server'ı başlat (Posta kutusunu aç)
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