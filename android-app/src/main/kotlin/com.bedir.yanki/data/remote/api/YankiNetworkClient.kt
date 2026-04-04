package com.bedir.yanki.data.remote.api

import com.bedir.yanki.Yanki.Message // Proto'dan üretilen mesaj sınıfı
import com.bedir.yanki.YankiSyncServiceGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YankiNetworkClient @Inject constructor(
    private val stub: YankiSyncServiceGrpc.YankiSyncServiceBlockingStub
) {
    fun sendMessageToServer(message: Message): Boolean {
        return try {
            val response = stub.syncMessages(message) // yncMessagess proto'daki isim
            response.success // Sunucudan gelen başarı durumu
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}