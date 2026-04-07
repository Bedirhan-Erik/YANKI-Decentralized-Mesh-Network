package com.bedir.yanki.data.remote.api

import com.bedir.yanki.Yanki.Message // Proto'dan üretilen mesaj sınıfı
import com.bedir.yanki.Yanki.MeshPayload
import com.bedir.yanki.Yanki.SyncRequest
import com.bedir.yanki.YankiSyncServiceGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YankiNetworkClient @Inject constructor(
    private val stub: YankiSyncServiceGrpc.YankiSyncServiceBlockingStub
) {
    fun sendMessageToServer(message: Message): Boolean {
        return try {
            val response = stub.syncMessages(message)
            response.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sendEmergencyToServer(signal: com.bedir.yanki.Yanki.EmergencySignal): Boolean {
        return try {
            val response = stub.sendEmergencySignal(signal)
            response.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun pullNewDataFromServer(userId: String, lastSync: Long): MeshPayload? {
        return try {
            val request = SyncRequest.newBuilder()
                .setUserId(userId)
                .setLastSyncTime(lastSync)
                .build()
            stub.getNewData(request)
        } catch (e: Exception) {
            android.util.Log.e("YANKI_NET", "Pull hatası: ${e.message}")
            null
        }
    }
}
