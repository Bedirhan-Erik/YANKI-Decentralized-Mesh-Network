package com.bedir.yanki.data.remote.api

import com.bedir.yanki.Yanki.Message
import com.bedir.yanki.Yanki.MeshPayload
import com.bedir.yanki.Yanki.SyncRequest
import com.bedir.yanki.YankiSyncServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ConnectivityState
import io.grpc.Status
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class YankiNetworkClient @Inject constructor(
    private val channel: ManagedChannel,
    private val stub: YankiSyncServiceGrpc.YankiSyncServiceBlockingStub
) {
    private val timeoutStub get() = stub.withDeadlineAfter(10, TimeUnit.SECONDS)

    /**
     * TRANSIENT_FAILURE veya IDLE durumundaki kanalı hemen yeniden bağlanmaya zorlar.
     * Ağ kurtarıldıktan sonraki ilk çağrı öncesinde çağrılır.
     */
    private fun resetChannelIfNeeded() {
        val state = channel.getState(true) // true = bağlanmayı talep et (IDLE → CONNECTING)
        if (state == ConnectivityState.TRANSIENT_FAILURE) {
            channel.resetConnectBackoff()
            android.util.Log.w("YANKI_NET", "Kanal TRANSIENT_FAILURE durumundaydı, backoff sıfırlandı.")
        }
    }

    private fun handleUnavailable() {
        // UNAVAILABLE alındığında backoff'u sıfırla ki sonraki çağrı hemen denensin
        channel.resetConnectBackoff()
    }

    fun updateUserProfileOnServer(user: com.bedir.yanki.Yanki.User): Boolean {
        resetChannelIfNeeded()
        return try {
            val response = timeoutStub.registerUser(user)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Profil gRPC Hatası (${e.status.code}): ${e.status.description}")
            if (e.status.code == Status.Code.UNAVAILABLE) handleUnavailable()
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Profil senkronizasyon hatası: ${e.message}", e)
            false
        }
    }

    fun sendMessageToServer(message: Message): Boolean {
        resetChannelIfNeeded()
        return try {
            val response = timeoutStub.syncMessages(message)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Mesaj gönderim gRPC hatası: ${e.status.code}")
            if (e.status.code == Status.Code.UNAVAILABLE) handleUnavailable()
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Mesaj gönderim bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun sendEmergencyToServer(signal: com.bedir.yanki.Yanki.EmergencySignal): Boolean {
        resetChannelIfNeeded()
        return try {
            val response = timeoutStub.sendEmergencySignal(signal)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Acil durum gRPC hatası: ${e.status.code}")
            if (e.status.code == Status.Code.UNAVAILABLE) handleUnavailable()
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Acil durum bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun sendBulletinToServer(post: com.bedir.yanki.Yanki.BulletinPost): Boolean {
        resetChannelIfNeeded()
        return try {
            val response = timeoutStub.syncBulletin(post)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Bülten gRPC hatası: ${e.status.code}")
            if (e.status.code == Status.Code.UNAVAILABLE) handleUnavailable()
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Bülten bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun pullNewDataFromServer(userId: String, lastSync: Long): MeshPayload? {
        resetChannelIfNeeded()
        return try {
            val request = SyncRequest.newBuilder()
                .setUserId(userId)
                .setLastSyncTime(lastSync)
                .build()
            timeoutStub.getNewData(request)
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Pull gRPC Hatası (${e.status.code}): ${e.status.description}")
            if (e.status.code == Status.Code.UNAVAILABLE) {
                android.util.Log.e("YANKI_NET", "SUNUCUYA ULAŞILAMIYOR: 1- Sunucu çalışıyor mu? 2- IP/Port doğru mu?")
                handleUnavailable()
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Pull bilinmeyen hata: ${e.message}", e)
            null
        }
    }
}
