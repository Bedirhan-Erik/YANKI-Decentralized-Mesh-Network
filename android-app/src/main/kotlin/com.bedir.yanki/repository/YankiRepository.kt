package com.bedir.yanki.repository

import android.util.Log
import com.bedir.yanki.data.local.dao.UserDao
import com.bedir.yanki.data.local.dao.MessageDao
import com.bedir.yanki.data.local.dao.EmergencySignalDao
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.remote.mesh.connectivity.BleMeshManager
import com.bedir.yanki.data.remote.mesh.connectivity.WifiAwareManager
import com.bedir.yanki.data.mapper.ProtoMapper
import androidx.core.content.edit
import com.bedir.yanki.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YankiRepository @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val emergencySignalDao: EmergencySignalDao,
    private val bleMeshManager: BleMeshManager,
    private val wifiAwareManager: WifiAwareManager,
    private val securityManager: SecurityManager,
    val sharedPreferences: android.content.SharedPreferences,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val STATUS_RECEIVED = 0
        const val STATUS_RELAYED = 1
    }

    val currentUserId: String
        get() = sharedPreferences.getString("user_id", "USER_${UUID.randomUUID().toString().take(8)}") ?: "MY_LOCAL_USER_ID"

    init {
        // user_id yoksa oluştur ve kaydet
        if (!sharedPreferences.contains("user_id")) {
            sharedPreferences.edit { putString("user_id", currentUserId) }
        }
    }

    // ==========================================
    // --- 1. KULLANICI İŞLEMLERİ ---
    // ==========================================
    suspend fun saveUser(user: UserEntity) = userDao.insertOrUpdateUser(user)
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    fun getAllUsersFlow() = userDao.getAllUsersFlow()
    suspend fun getAllUsers() = userDao.getAllUsers()

    // ==========================================
    // --- 2. MESAJ İŞLEMLERİ ---
    // ==========================================
    suspend fun saveMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun getUnsyncedMessages() = messageDao.getUnsyncedMessages()
    suspend fun markMessageAsSynced(msgId: String) = messageDao.markAsSynced(msgId)
    suspend fun markSignalAsSynced(signalId: String) {
        emergencySignalDao.markAsSynced(signalId)
    }
    fun getChatHistory(userId: String) = messageDao.getChatHistory(userId)
    fun getAllMessagesFlow() = messageDao.getAllMessages()


    suspend fun sendMessage(content: String, receiverId: String) {
        try {
            ensureLocalUserExists()
            val messageBytes = content.toByteArray()

            // Mesajı imzala
            var signature: ByteArray? = null
            try {
                val privateKey = getMySecretKey()
                if (privateKey.isNotEmpty()) {
                    signature = securityManager.signData(messageBytes, privateKey)
                }
            } catch (t: Throwable) {
                Log.e("YANKI_REPO", "Mesaj imzalama hatası: ${t.message}")
            }

            val messageEntity = MessageEntity(
                msg_id = UUID.randomUUID().toString(),
                sender_id = currentUserId,
                receiver_id = receiverId,
                content_blob = messageBytes,
                signature = signature ?: byteArrayOf(),
                timestamp = System.currentTimeMillis(),
                status = STATUS_RELAYED,
                is_synced = false,
                ttl = 7
            )

            saveMessage(messageEntity)
            Log.d("YANKI_REPO", "Mesaj yerel veritabanına kaydedildi: ${messageEntity.msg_id}")
            
            // --- YENİ: İnternet Varsa Hemen Buluta Gönder ---
            triggerImmediateCloudSync()

            // Komşu listesini kontrol et ve mesh üzerinden gönderim yapmayı dene
            val neighbors = userDao.getAllUsers()
            val currentTime = System.currentTimeMillis()
            
            neighbors.forEach { neighbor ->
                try {
                    // Son 5 dakika içinde görüldüyse ve MAC adresi varsa
                    if (currentTime - neighbor.last_seen < 300000 && !neighbor.last_mac.isNullOrBlank()) {
                        // Eğer direkt alıcı ise veya genel gossip için
                        if (neighbor.user_id == receiverId || neighbor.user_id != currentUserId) {
                            val payload = ProtoMapper.packageAll(emptyList(), listOf(messageEntity))
                            
                            if (payload.isNotEmpty()) {
                                if (payload.size < 500) {
                                    Log.d("YANKI_MESH", "Mesaj komşuya (${neighbor.user_id}) BLE üzerinden denenecek.")
                                    bleMeshManager.sendPayloadToNeighbor(neighbor.last_mac, payload)
                                } else {
                                    Log.d("YANKI_MESH", "Mesaj BLE için çok büyük (${payload.size} byte), Wi-Fi Aware beklenecek.")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("YANKI_MESH", "Neighbor gönderim hatası (${neighbor.user_id}): ${e.message}")
                }
            }
            
            // Wi-Fi Aware için genel tetikleyici
            triggerGossipProtocol("", "")
        } catch (t: Throwable) {
            Log.e("YANKI_REPO", "sendMessage fatal error: ${t.message}")
            t.printStackTrace()
        }
    }

    private fun getMySecretKey(): ByteArray {
        val keyHex = sharedPreferences.getString("secret_key", null)
        return if (keyHex != null) {
            try {
                java.util.Base64.getDecoder().decode(keyHex)
            } catch (e: Exception) {
                Log.e("YANKI_REPO", "Gizli anahtar decode edilemedi: ${e.message}")
                ByteArray(32)
            }
        } else {
            // Eğer anahtar yoksa (henüz oluşturulmadıysa), yeni bir tane oluşturup kaydedelim
            val keyPair = securityManager.generateUserKeyPair()
            
            // FİX: Doğru sözdizimi ve null safety
            val newKey = keyPair?.secretKey?.asBytes ?: ByteArray(32)
            
            try {
                sharedPreferences.edit { 
                    putString("secret_key", java.util.Base64.getEncoder().encodeToString(newKey)) 
                }
            } catch (e: Exception) {
                Log.e("YANKI_REPO", "Gizli anahtar kaydedilemedi")
            }
            newKey
        }
    }

    suspend fun handleIncomingMeshMessage(incomingMessage: MessageEntity) {
        try {
            val alreadySeen = messageDao.isMessageExists(incomingMessage.msg_id)
            if (alreadySeen) return

            // Mesaj imzasını doğrula
            val sender = userDao.getUserById(incomingMessage.sender_id)
            if (sender != null && sender.public_key.isNotEmpty() && incomingMessage.signature != null) {
                try {
                    val isValid = securityManager.verifySignature(
                        data = incomingMessage.content_blob,
                        signature = incomingMessage.signature,
                        publicKey = sender.public_key
                    )
                    if (!isValid) {
                        Log.w("YANKI_SECURITY", "Geçersiz mesaj imzası tespit edildi! MsgID: ${incomingMessage.msg_id}")
                    }
                } catch (t: Throwable) {
                    Log.e("YANKI_SECURITY", "İmza doğrulama sırasında fatal hata: ${t.message}")
                }
            }

            if (incomingMessage.receiver_id == currentUserId) {
                messageDao.insertMessage(incomingMessage.copy(status = STATUS_RECEIVED))
            } else {
                if (incomingMessage.ttl > 0) {
                    val relayedMessage = incomingMessage.copy(
                        ttl = incomingMessage.ttl - 1,
                        status = STATUS_RELAYED
                    )
                    messageDao.insertMessage(relayedMessage)
                }
            }
        } catch (t: Throwable) {
            Log.e("YANKI_REPO", "handleIncomingMeshMessage fatal error: ${t.message}")
        }
    }

    // ==========================================
    // --- 3. ACİL DURUM (SOS) İŞLEMLERİ ---
    // ==========================================
    suspend fun saveEmergencySignal(signal: EmergencySignalEntity) = emergencySignalDao.insertSignal(signal)
    suspend fun getAllSignals() = emergencySignalDao.getAllSignals()

    suspend fun sendEmergencySignal(type: String, lat: Double, lon: Double, battery: Int) {
        ensureLocalUserExists()
        val signal = EmergencySignalEntity(
            signal_id = UUID.randomUUID().toString(),
            user_id = currentUserId,
            latitude = lat,
            longitude = lon,
            emergency_type = type,
            battery_level = battery,
            timestamp = System.currentTimeMillis(),
            is_synced = false,
            hop_count = 0
        )
        saveEmergencySignal(signal)
        Log.d("YANKI_REPO", "SOS Sinyali yerel veritabanına kaydedildi. Bulut tetikleniyor...")
        
        // Önemli: Direkt buluta çıkmayı dene
        triggerImmediateCloudSync()
        
        // Komşulara mesh üzerinden yay
        val neighbors = userDao.getAllUsers()
        neighbors.forEach { neighbor ->
            if (!neighbor.last_mac.isNullOrBlank()) {
                val payload = ProtoMapper.packageSOSOnly(listOf(signal))
                bleMeshManager.sendPayloadToNeighbor(neighbor.last_mac, payload)
            }
        }
    }

    suspend fun handleEmergencySignal(signal: EmergencySignalEntity) {
        if (emergencySignalDao.isSignalExists(signal.signal_id)) return
        emergencySignalDao.insertSignal(signal)

        if (signal.hop_count < 10) {
            val relayedSignal = signal.copy(hop_count = signal.hop_count + 1)
            Log.d("YANKI_MESH", "SOS Sinyali yönlendiriliyor: ${relayedSignal.signal_id}")
        }
    }

    private suspend fun ensureLocalUserExists() {
        val existing = userDao.getUserById(currentUserId)
        if (existing == null) {
            Log.d("YANKI_REPO", "Yerel kullanıcı bulunamadı, yeni kimlik oluşturuluyor...")
            val keyPair = securityManager.generateUserKeyPair()
            
            // KRİTİK: Eğer kütüphane yüklenemediyse boş bir anahtar ile devam et (Çökme engellenir)
            val publicKeyBytes = keyPair?.publicKey?.asBytes ?: byteArrayOf()
            
            val localUser = UserEntity(
                user_id = currentUserId,
                username = "Ben (Kendi Cihazım)",
                public_key = publicKeyBytes,
                last_seen = System.currentTimeMillis(),
                is_trusted = true
            )
            userDao.insertOrUpdateUser(localUser)
        }
    }

    // ==========================================
    // --- 4. MESH & GOSSIP PROTOKOLÜ ---
    // ==========================================
    fun startListeningForMeshPayloads() {
        // 1. BLE'den gelenleri dinle
        bleMeshManager.setOnDataReceivedListener { data ->
            processIncomingPayload(data)
        }

        // 2. Wi-Fi Aware'den gelenleri dinle
        wifiAwareManager.setOnDataReceivedListener { data ->
            processIncomingPayload(data)
        }

        // 3. Wi-Fi Aware Soket Köprüsü Kurulduğunda Bekleyenleri Gönder
        wifiAwareManager.setOnSocketReadyListener { socket ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pendingMessages = messageDao.getPendingMessages()
                    val pendingSOS = emergencySignalDao.getPendingSignals()
                    
                    if (pendingMessages.isNotEmpty() || pendingSOS.isNotEmpty()) {
                        val fullPayload = ProtoMapper.packageAll(pendingSOS, pendingMessages)
                        wifiAwareManager.sendDataOverSocket(socket, fullPayload)
                        
                        // NOT: Burada markAsSynced ÇAĞIRILMAMALI. 
                        // is_synced sadece bulut (Firebase) onayı geldiğinde true olmalı.
                        // Mesh üzerinden gönderim, bulut senkronizasyonu yerine geçmez.
                        Log.d("YANKI_REPO", "Mesh üzerinden ${pendingMessages.size} mesaj paylaşıldı.")
                    }
                } catch (e: Exception) {
                    Log.e("YANKI_REPO", "Soket üzerinden veri gönderimi başarısız: ${e.message}")
                }
            }
        }
    }

    private fun processIncomingPayload(data: ByteArray) {
        Log.d("YANKI_MESH", "AĞDAN VERİ GELDİ! Boyut: ${data.size} byte")
        val parsed = ProtoMapper.parseIncomingPayload(data)
        if (parsed != null) {
            Log.d("YANKI_MESH", "Paket çözüldü: ${parsed.messages.size} mesaj var.")

            val parsed = ProtoMapper.parseIncomingPayload(data)
            parsed?.let { payload ->
                CoroutineScope(Dispatchers.IO).launch {
                    payload.signals.forEach { signal -> handleEmergencySignal(signal) }
                    payload.messages.forEach { message -> handleIncomingMeshMessage(message) }

                    // Mesh'ten yeni veri geldi, eğer internet varsa hemen buluta gönder
                    if (payload.signals.isNotEmpty() || payload.messages.isNotEmpty()) {
                        triggerImmediateCloudSync()
                    }
                }
            }
        }
    }

    suspend fun handleNeighborFound(neighborId: String, neighborMacAddress: String, rssi: Int) {
        // Kendi kullanıcımızın veritabanında olduğundan emin olalım (Çökme koruması)
        ensureLocalUserExists()

        val currentTime = System.currentTimeMillis()
        val existingUser = userDao.getUserById(neighborId)

        if (existingUser != null) {
            userDao.updateLastSeen(userId = neighborId, timestamp = currentTime, macAddress = neighborMacAddress, rssi = rssi)
        } else {
            val newUser = UserEntity(
                user_id = neighborId,
                username = "Bilinmeyen Komşu",
                public_key = byteArrayOf(),
                last_seen = currentTime,
                last_mac = neighborMacAddress,
                last_rssi = rssi,
                is_trusted = false
            )
            userDao.insertOrUpdateUser(newUser)
        }
        triggerGossipProtocol(neighborId, neighborMacAddress)
    }

    private fun triggerImmediateCloudSync() {
        try {
            // Ağ kısıtlamasını kaldırıyoruz: Yerel Wi-Fi'da (internet olmasa bile) denesin
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.bedir.yanki.data.remote.sync.SyncWorker>()
                .build() 
            
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork("immediate_sync", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
            Log.d("YANKI_SYNC", "Anlık bulut senkronizasyonu tetiklendi (Kısıtlamasız).")
        } catch (e: Exception) {
            Log.e("YANKI_SYNC", "WorkManager tetikleme hatası: ${e.message}")
        }
    }

    private suspend fun triggerGossipProtocol(neighborId: String, neighborMacAddress: String) {
        try {
            val pendingSOS = emergencySignalDao.getPendingSignals()
            val pendingMessages = messageDao.getPendingMessages()

            if (pendingSOS.isEmpty() && pendingMessages.isEmpty()) return

            // Öncelik 1: SOS Sinyalleri (Her zaman BLE ile hızlıca fırlat)
            if (pendingSOS.isNotEmpty() && neighborMacAddress.isNotBlank()) {
                Log.d("YANKI_MESH", "Komşuya SOS paketleri gönderiliyor: $neighborMacAddress")
                val sosPayload = ProtoMapper.packageSOSOnly(pendingSOS)
                bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, sosPayload)
            }

            // Öncelik 2: Mesaj Paketleme ve BLE Kontrolü
            var needsWifiAware = false
            if (pendingMessages.isNotEmpty()) {
                val messagePayload = ProtoMapper.packageAll(emptyList(), pendingMessages)
                
                // Eğer paket BLE sınırları içindeyse (Parçalama desteğiyle 2000 byte'a kadar makul) ve MAC biliniyorsa BLE kullan
                if (messagePayload.size < 2000 && neighborMacAddress.isNotBlank()) {
                    Log.d("YANKI_MESH", "Mesaj paketi BLE üzerinden (parçalı) gönderiliyor: $neighborMacAddress")
                    bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, messagePayload)
                } else {
                    // Paket çok büyükse Wi-Fi Aware şart
                    needsWifiAware = true
                }
            }

            // Öncelik 3: Wi-Fi Aware (Sadece büyük veriler veya toplu gönderim için)
            if (needsWifiAware || pendingMessages.size > 5) {
                Log.d("YANKI_MESH", "Wi-Fi Aware üzerinden yüksek kapasiteli veri trafiği başlatılıyor...")
                wifiAwareManager.attachToAwareSystem {
                    wifiAwareManager.startPublishing()
                    wifiAwareManager.startSubscribing()
                }
            }
        } catch (e: Exception) {
            Log.e("YANKI_MESH", "triggerGossipProtocol hatası: ${e.message}")
        }
    }
}
