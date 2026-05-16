package com.bedir.yanki.data.remote.api

import com.bedir.yanki.Yanki.Message // Proto'dan üretilen mesaj sınıfı
import com.bedir.yanki.Yanki.MeshPayload
import com.bedir.yanki.Yanki.SyncRequest
import com.bedir.yanki.YankiSyncServiceGrpc
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class YankiNetworkClient @Inject constructor(
    private val stub: YankiSyncServiceGrpc.YankiSyncServiceBlockingStub
) {
    private val timeoutStub get() = stub.withDeadlineAfter(10, TimeUnit.SECONDS)

    fun updateUserProfileOnServer(user: com.bedir.yanki.Yanki.User): Boolean {
        return try {
            val response = timeoutStub.registerUser(user)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Profil gRPC Hatası (${e.status.code}): ${e.status.description}")
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Profil senkronizasyon hatası: ${e.message}", e)
            false
        }
    }

    fun sendMessageToServer(message: Message): Boolean {
        return try {
            val response = timeoutStub.syncMessages(message)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Mesaj gönderim gRPC hatası: ${e.status.code}")
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Mesaj gönderim bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun sendEmergencyToServer(signal: com.bedir.yanki.Yanki.EmergencySignal): Boolean {
        return try {
            val response = timeoutStub.sendEmergencySignal(signal)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Acil durum gRPC hatası: ${e.status.code}")
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Acil durum bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun sendBulletinToServer(post: com.bedir.yanki.Yanki.BulletinPost): Boolean {
        return try {
            val response = timeoutStub.syncBulletin(post)
            response.success
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Bülten gRPC hatası: ${e.status.code}")
            false
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Bülten bilinmeyen hata: ${e.message}")
            false
        }
    }

    fun pullNewDataFromServer(userId: String, lastSync: Long): MeshPayload? {
        return try {
            val request = SyncRequest.newBuilder()
                .setUserId(userId)
                .setLastSyncTime(lastSync)
                .build()
            timeoutStub.getNewData(request)
        } catch (e: io.grpc.StatusRuntimeException) {
            android.util.Log.e("YANKI_NET", "Pull gRPC Hatası (${e.status.code}): ${e.status.description}")
            if (e.status.code == io.grpc.Status.Code.UNAVAILABLE) {
                android.util.Log.e("YANKI_NET", "SUNUCUYA ULAŞILAMIYOR: 1- Sunucu çalışıyor mu? 2- IP/Port doğru mu?")
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Pull bilinmeyen hata: ${e.message}", e)
            null
        }
    }
}
