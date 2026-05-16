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
            
            val myProfile = repository.getUser(repository.currentUserId)
            myProfile?.let { profile ->
                Log.d("YANKI_SYNC", "Profil sunucuya push ediliyor: ${profile.username}")
                val isProfileSent = networkClient.updateUserProfileOnServer(ProtoMapper.toProto(profile))
                if (isProfileSent) {
                    Log.d("YANKI_SYNC", "Profil sunucuya başarıyla push edildi.")
                } else {
                    Log.w("YANKI_SYNC", "Profil push edilemedi (Sunucu hatası veya kapalı).")
                }
            }

            // --- 1. PULL: Sunucudan yeni verileri çek ---
            val lastSync = repository.sharedPreferences.getLong("last_cloud_sync", 0L)
            val pullResponse = networkClient.pullNewDataFromServer(repository.currentUserId, lastSync)
            
            var pullSuccess = false
            pullResponse?.let { payload ->
                pullSuccess = true
                Log.d("YANKI_SYNC", "Sunucudan veri çekildi: ${payload.messagesCount} mesaj, ${payload.signalsCount} SOS")
                
                // Klasik for döngüsü suspend fonksiyonlar için daha güvenlidir
                for (msg in payload.messagesList) {
                    try { repository.saveMessage(ProtoMapper.fromProto(msg)) } catch (e: Exception) { Log.e("YANKI_SYNC", "Mesaj kaydedilemedi", e) }
                }
                for (sig in payload.signalsList) {
                    try { repository.saveEmergencySignal(ProtoMapper.fromProtoSOS(sig)) } catch (e: Exception) { Log.e("YANKI_SYNC", "SOS kaydedilemedi", e) }
                }
                for (bul in payload.bulletinsList) {
                    try { repository.saveBulletin(ProtoMapper.fromProto(bul)) } catch (e: Exception) { Log.e("YANKI_SYNC", "İlan kaydedilemedi", e) }
                }
                for (user in payload.usersList) {
                    try { repository.saveUser(ProtoMapper.fromProto(user)) } catch (e: Exception) { Log.e("YANKI_SYNC", "Kullanıcı kaydedilemedi", e) }
                }

                repository.sharedPreferences.edit().putLong("last_cloud_sync", System.currentTimeMillis()).apply()
            }

            // --- 2. PUSH: Yerel verileri gönder ---
            val unsyncedMessages = repository.getUnsyncedMessages()
            val unsyncedSignals = repository.getUnsyncedSignals()
            val unsyncedBulletins = repository.getUnsyncedBulletins()

            if (!pullSuccess && unsyncedMessages.isEmpty() && unsyncedSignals.isEmpty() && unsyncedBulletins.isEmpty()) {
                return@withContext Result.success() // İş bitti
            }

            var pushSuccess = true
            
            // Mesaj Push
            for (msg in unsyncedMessages) {
                if (networkClient.sendMessageToServer(ProtoMapper.toProto(msg))) {
                    repository.markMessageAsSynced(msg.msg_id)
                } else {
                    pushSuccess = false
                }
            }
            // SOS Push
            for (sig in unsyncedSignals) {
                if (networkClient.sendEmergencyToServer(ProtoMapper.toProtoSOS(sig))) {
                    repository.markSignalAsSynced(sig.signal_id)
                } else {
                    pushSuccess = false
                }
            }
            // İlan Push
            for (bul in unsyncedBulletins) {
                if (networkClient.sendBulletinToServer(ProtoMapper.toProto(bul))) {
                    repository.markBulletinAsSynced(bul.post_id)
                } else {
                    pushSuccess = false
                }
            }

            if (pullSuccess && pushSuccess) Result.success() else Result.retry()

        } catch (e: Throwable) {
            // CancellationException'ı WorkManager'ın kendi yönetimine bırakmalıyız
            if (e is kotlinx.coroutines.CancellationException) {
                Log.w("YANKI_SYNC", "Senkronizasyon işi WorkManager/Sistem tarafından durduruldu.")
                throw e
            }
            // Diğer tüm hataları (NoSuchMethodError vb.) burada yakalayıp detaylı loglayalım
            Log.e("YANKI_SYNC", "Senkronizasyon sırasında KRİTİK HATA oluştu!", e)
            Result.retry()
        }
    }
}