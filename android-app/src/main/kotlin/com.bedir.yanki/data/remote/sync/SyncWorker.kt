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

            // 1. Henüz buluta gönderilmemiş (is_synced = false) mesajları getir
            val unsyncedMessages = repository.getUnsyncedMessages()

            if (unsyncedMessages.isEmpty()) {
                Log.d("YANKI_SYNC", "Gönderilecek yeni veri bulunamadı.")
                return@withContext Result.success()
            }

            Log.d("YANKI_SYNC", "${unsyncedMessages.size} adet mesaj gönderiliyor...")

            var allSuccess = true

            // 2. Her bir mesajı tek tek paketle ve gönder
            unsyncedMessages.forEach { entity ->
                // Entity -> Protobuf dönüşümü
                val protoMsg = ProtoMapper.toProto(entity)

                // Sunucuya (Node.js/gRPC) gönderim dene
                val isSent = networkClient.sendMessageToServer(protoMsg)

                if (isSent) {
                    // 3. Başarılıysa veritabanında "senkronize edildi" olarak işaretle
                    repository.markMessageAsSynced(entity.msg_id)
                    Log.d("YANKI_SYNC", "Mesaj başarıyla senkronize edildi: ${entity.msg_id}")
                } else {
                    allSuccess = false
                    Log.e("YANKI_SYNC", "Mesaj gönderimi başarısız: ${entity.msg_id}")
                }
            }

            // Eğer bazı mesajlar gitmediyse WorkManager uygun bir zamanda tekrar deneyecek
            if (allSuccess) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("YANKI_SYNC", "Senkronizasyon sırasında kritik hata: ${e.message}")
            // Beklenmedik bir hata (örn: sunucu kapalı) durumunda tekrar dene
            Result.retry()
        }
    }
}