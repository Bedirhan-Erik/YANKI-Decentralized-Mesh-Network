package com.bedir.yanki.data.remote.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bedir.yanki.data.mapper.ProtoMapper
import com.bedir.yanki.data.remote.api.YankiNetworkClient
import com.bedir.yanki.repository.YankiRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.bedir.yanki.data.local.entity.MessageEntity

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: YankiRepository,
    private val networkClient: YankiNetworkClient // gRPC üzerinden sunucuya fırlatan istemci
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("YANKI_SYNC", "Senkronizasyon işlemi başlatıldı...")

            // --- 1. PULL: Sunucudan yeni verileri çek (Çift yönlü senkronizasyon) ---
            val lastSync = repository.sharedPreferences.getLong("last_cloud_sync", 0L)
            val pullResponse = networkClient.pullNewDataFromServer(repository.currentUserId, lastSync)
            
            pullResponse?.let { payload ->
                Log.d("YANKI_SYNC", "Sunucudan veri çekildi: ${payload.messagesCount} mesaj, ${payload.signalsCount} SOS")
                
                // Mesajları kaydet
                payload.messagesList.forEach { protoMsg ->
                    val entity = ProtoMapper.fromProto(protoMsg)
                    repository.saveMessage(entity)
                    
                    // Eğer bu mesaj bizim değilse ve mesh üzerinden yayılması gerekiyorsa
                    // status = STATUS_RELAYED (1) olarak işaretleyip mesh'e bırakabiliriz
                }

                // SOS sinyallerini kaydet
                payload.signalsList.forEach { protoSos ->
                    val entity = ProtoMapper.fromProtoSOS(protoSos)
                    repository.saveEmergencySignal(entity)
                }

                // Senkronizasyon zamanını güncelle
                repository.sharedPreferences.edit().putLong("last_cloud_sync", System.currentTimeMillis()).apply()
            }

            // --- 2. PUSH: Henüz buluta gönderilmemiş verileri gönder ---
            val unsyncedMessages = repository.getUnsyncedMessages()
            val unsyncedSignals = repository.getAllSignals().filter { !it.is_synced }

            if (unsyncedMessages.isEmpty() && unsyncedSignals.isEmpty()) {
                Log.d("YANKI_SYNC", "Gönderilecek yeni veri bulunamadı.")
                return@withContext Result.success()
            }

            var allSuccess = true

            // 2. Mesajları Gönder
            unsyncedMessages.forEach { entity ->
                val isSent = networkClient.sendMessageToServer(ProtoMapper.toProto(entity))
                if (isSent) {
                    repository.markMessageAsSynced(entity.msg_id)
                } else {
                    allSuccess = false
                }
            }

            // 3. SOS Sinyallerini Gönder
            unsyncedSignals.forEach { entity ->
                val isSent = networkClient.sendEmergencyToServer(ProtoMapper.toProtoSOS(entity))
                if (isSent) {
                    // SOS sinyalleri için de senkronize edildi işareti (EmergencySignalEntity'e is_synced alanı eklendiği varsayılıyor)
                    repository.markSignalAsSynced(entity.signal_id)
                } else {
                    allSuccess = false
                }
            }

            if (allSuccess) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("YANKI_SYNC", "Senkronizasyon sırasında kritik hata: ${e.message}")
            // Beklenmedik bir hata (örn: sunucu kapalı) durumunda tekrar dene
            Result.retry()
        }
    }
}