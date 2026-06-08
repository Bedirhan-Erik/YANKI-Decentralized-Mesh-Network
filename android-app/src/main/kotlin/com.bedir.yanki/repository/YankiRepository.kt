package com.bedir.yanki.repository

import android.util.Log
import com.bedir.yanki.data.local.dao.UserDao
import com.bedir.yanki.data.local.dao.MessageDao
import com.bedir.yanki.data.local.dao.EmergencySignalDao
import com.bedir.yanki.data.local.dao.BulletinDao
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.local.entity.BulletinEntity
import com.bedir.yanki.data.remote.mesh.connectivity.BleMeshManager
import com.bedir.yanki.data.remote.mesh.connectivity.WifiAwareManager
import com.bedir.yanki.data.mapper.ProtoMapper
import com.bedir.yanki.util.NotificationHelper
import androidx.core.content.edit
import com.bedir.yanki.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.bedir.yanki.data.local.YankiDatabase
import androidx.room.withTransaction

@Singleton
class YankiRepository @Inject constructor(
    private val database: YankiDatabase,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val emergencySignalDao: EmergencySignalDao,
    private val bulletinDao: BulletinDao,
    val bleMeshManager: BleMeshManager,
    val wifiAwareManager: WifiAwareManager,
    private val securityManager: SecurityManager,
    private val notificationHelper: NotificationHelper,
    val sharedPreferences: android.content.SharedPreferences,
    @ApplicationContext private val context: Context
) {
    // ... (mevcut kodlar)

    // ==========================================
    // --- İLAN TAHTASI (BULLETIN) İŞLEMLERİ ---
    // ==========================================
    fun getAllBulletinsFlow() = bulletinDao.getAllPostsFlow()
    suspend fun getUnsyncedBulletins() = bulletinDao.getUnsyncedPosts()
    suspend fun markBulletinAsSynced(postId: String) = bulletinDao.markAsSynced(postId)
    suspend fun saveBulletin(post: BulletinEntity) = bulletinDao.insertPost(post)

    suspend fun postToBulletin(content: String, type: String, lat: Double, lon: Double) {
        val user = userDao.getUserById(currentUserId)
        val post = BulletinEntity(
            post_id = UUID.randomUUID().toString(),
            sender_id = currentUserId,
            sender_name = user?.full_name ?: user?.username ?: "Anonim",
            content = content,
            timestamp = System.currentTimeMillis(),
            type = type,
            latitude = lat,
            longitude = lon,
            ttl = 10 // 10 zıplama (hop) boyunca yayılabilir
        )
        
        bulletinDao.insertPost(post)
        Log.d("YANKI_BULLETIN", "Yeni ilan yerel tahtaya eklendi. Yayılıyor...")
        
        // Gossip protokolünü tetikle (Hemen duyur)
        triggerGossipProtocol("", "")
    }

    private suspend fun handleIncomingBulletin(post: BulletinEntity) {
        if (bulletinDao.isPostExists(post.post_id)) return
        
        if (post.ttl > 0) {
            val relayedPost = post.copy(ttl = post.ttl - 1)
            bulletinDao.insertPost(relayedPost)
            Log.d("YANKI_BULLETIN", "Yeni ilan ağdan alındı ve kaydedildi: ${post.content}")
            
            // TODO: Burada bildirim gönderilecek
            notificationHelper.showBulletinNotification(relayedPost)
        }
    }
    companion object {
        const val STATUS_RECEIVED = 0
        const val STATUS_RELAYED = 1
    }

    val currentUserId: String
        get() = sharedPreferences.getString("user_id", null) ?: "GUEST"

    private val _isUserRegistered = MutableStateFlow(sharedPreferences.contains("user_id"))
    val isUserRegisteredFlow: StateFlow<Boolean> = _isUserRegistered.asStateFlow()

    val isUserRegistered: Boolean
        get() = sharedPreferences.contains("user_id")

    private var isMeshListening = false

    // ==========================================
    // --- 1. KULLANICI İŞLEMLERİ ---
    // ==========================================
    suspend fun saveUser(user: UserEntity) {
        userDao.insertOrUpdateUser(user)
        // Eğer kaydedilen kullanıcı "biz" isek, SharedPreferences'a ID'yi yazalım
        if (user.is_trusted && sharedPreferences.getString("user_id", null) == null) {
            sharedPreferences.edit { putString("user_id", user.user_id) }
            _isUserRegistered.value = true
        }
    }
    
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    fun getUserFlow(userId: String) = userDao.getUserFlow(userId)
    fun getAllUsersFlow() = userDao.getAllUsersFlow()
    suspend fun getAllUsers() = userDao.getAllUsers()

    suspend fun updateUserHealth(
        bloodType: String? = null,
        allergies: String? = null,
        medications: String? = null,
        emergencyContact: String? = null
    ) {
        val userId = currentUserId
        val user = userDao.getUserById(userId) ?: return
        
        val updatedUser = user.copy(
            blood_type = bloodType ?: user.blood_type,
            allergies = allergies ?: user.allergies,
            medications = medications ?: user.medications,
            emergency_contact = emergencyContact ?: user.emergency_contact
        )
        
        userDao.insertOrUpdateUser(updatedUser)
        
        // ÖNEMLİ: Profil güncellendiğinde ağa duyur!
        broadcastIdentityUpdate(updatedUser)
    }

    private suspend fun broadcastIdentityUpdate(user: UserEntity) {
        val identityPayload = ProtoMapper.packageAll(
            signals = emptyList(),
            messages = emptyList(),
            users = listOf(user)
        )
        
        // Aktif komşulara BLE üzerinden gönder
        val neighbors = userDao.getAllUsers()
        neighbors.forEach { neighbor ->
            if (!neighbor.last_mac.isNullOrBlank() && System.currentTimeMillis() - neighbor.last_seen < 60000) {
                bleMeshManager.sendPayloadToNeighbor(neighbor.last_mac!!, identityPayload)
            }
        }
        
        // Wi-Fi Aware üzerinden duyur
        wifiAwareManager.attachToAwareSystem {
            wifiAwareManager.startPublishing()
        }
    }

    suspend fun ensureUserKeys() {
        getMySecretKey()
    }

    suspend fun logout() {
        sharedPreferences.edit { 
            remove("user_id")
        }
        _isUserRegistered.value = false
    }

    suspend fun clearAllData() {
        userDao.deleteAllUsers()
        messageDao.deleteAllMessages()
        emergencySignalDao.deleteAllSignals()
        sharedPreferences.edit { clear() }
        _isUserRegistered.value = false
    }

    suspend fun cleanupOldBulletins() {
        // 48 saatten eski ilan ve SOS verilerini temizle
        val threshold = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
        bulletinDao.deleteOldPosts(threshold)
        emergencySignalDao.deleteOldSignals(threshold)
        Log.d("YANKI_CLEANUP", "48 saatten eski veriler yerel veritabanından temizlendi.")
    }

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

            val myProfile = userDao.getUserById(currentUserId)
            val messageEntity = MessageEntity(
                msg_id = UUID.randomUUID().toString(),
                sender_id = currentUserId,
                sender_name = myProfile?.full_name ?: myProfile?.username ?: "Anonim",
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
            val newSecret = keyPair?.secretKey?.asBytes ?: ByteArray(32)
            val newPublic = keyPair?.publicKey?.asBytes ?: ByteArray(32)
            
            try {
                sharedPreferences.edit { 
                    putString("secret_key", java.util.Base64.getEncoder().encodeToString(newSecret))
                    putString("public_key", java.util.Base64.getEncoder().encodeToString(newPublic))
                }
            } catch (e: Exception) {
                Log.e("YANKI_REPO", "Anahtarlar kaydedilemedi")
            }
            newSecret
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

            // receiver_id eski 4-char prefix olabilir, tam UUID ile prefix karşılaştırması yap
            val isForMe = incomingMessage.receiver_id == currentUserId ||
                (incomingMessage.receiver_id.length < 36 && currentUserId.startsWith(incomingMessage.receiver_id))
            if (isForMe) {
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

            // Gönderenin adı mesajda varsa ve veritabanında "Bilinmeyen Komşu" olarak kayıtlıysa güncelle
            if (!incomingMessage.sender_name.isNullOrBlank()) {
                val existingSender = userDao.getUserById(incomingMessage.sender_id)
                if (existingSender != null &&
                    (existingSender.username == "Bilinmeyen Komşu" || existingSender.full_name == null)
                ) {
                    userDao.insertOrUpdateUser(
                        existingSender.copy(
                            username = incomingMessage.sender_name,
                            full_name = incomingMessage.sender_name
                        )
                    )
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
        val myProfile = userDao.getUserById(currentUserId)
        
        val signal = EmergencySignalEntity(
            signal_id = UUID.randomUUID().toString(),
            user_id = currentUserId,
            user_name = myProfile?.full_name ?: myProfile?.username ?: "Anonim",
            latitude = lat,
            longitude = lon,
            emergency_type = type,
            battery_level = battery,
            timestamp = System.currentTimeMillis(),
            is_synced = false,
            hop_count = 0,
            blood_type = myProfile?.blood_type,
            allergies = myProfile?.allergies,
            medications = myProfile?.medications,
            emergency_contact = myProfile?.emergency_contact
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
                bleMeshManager.sendPayloadToNeighbor(neighbor.last_mac!!, payload)
            }
        }
    }

    suspend fun handleEmergencySignal(signal: EmergencySignalEntity) {
        if (emergencySignalDao.isSignalExists(signal.signal_id)) return
        emergencySignalDao.insertSignal(signal)
        
        // SOS bildirimi göster
        notificationHelper.showSOSNotification(signal)

        // Sinyali buluta hemen göndermeyi dene
        triggerImmediateCloudSync()

        // KRİTİK: Diğer komşulara fırlat (YAYILIM)
        if (signal.hop_count < 10) {
            val relayedSignal = signal.copy(hop_count = signal.hop_count + 1)
            val neighbors = userDao.getAllUsers()
            neighbors.forEach { neighbor ->
                if (!neighbor.last_mac.isNullOrBlank()) {
                    val payload = ProtoMapper.packageSOSOnly(listOf(relayedSignal))
                    bleMeshManager.sendPayloadToNeighbor(neighbor.last_mac!!, payload)
                }
            }
        }
    }

    // ==========================================
    // --- 4. MESH & GOSSIP PROTOKOLÜ ---
    // ==========================================
    fun startListeningForMeshPayloads(scope: CoroutineScope) {
        if (isMeshListening) return
        isMeshListening = true

        // 1. BLE'den gelenleri dinle
        scope.launch {
            bleMeshManager.dataReceivedFlow.collect { (senderMac, data) ->
                processIncomingPayload(data, scope, senderMac)
            }
        }

        // 2. Wi-Fi Aware'den gelen verileri Flow üzerinden dinle
        scope.launch {
            wifiAwareManager.dataReceivedFlow.collect { data ->
                processIncomingPayload(data, scope)
            }
        }

        // 3. Wi-Fi Aware Soket Köprüsü Kurulduğunda Bekleyenleri Gönder
        scope.launch {
            wifiAwareManager.socketReadyFlow.collect { socket ->
                try {
                    val pendingMessages = messageDao.getPendingMessages()
                    val pendingSOS = emergencySignalDao.getPendingSignals()
                    val pendingBulletins = bulletinDao.getUnsyncedPosts()
                    
                    if (pendingMessages.isNotEmpty() || pendingSOS.isNotEmpty() || pendingBulletins.isNotEmpty()) {
                        val fullPayload = ProtoMapper.packageAll(
                            signals = pendingSOS,
                            messages = pendingMessages,
                            bulletins = pendingBulletins
                        )
                        wifiAwareManager.sendDataOverSocket(socket, fullPayload)
                        Log.d("YANKI_REPO", "Mesh üzerinden (Flow/Socket) veri paylaşıldı.")
                    }
                } catch (e: Exception) {
                    Log.e("YANKI_REPO", "Soket üzerinden veri gönderimi başarısız: ${e.message}")
                }
            }
        }
    }

    private fun processIncomingPayload(data: ByteArray, scope: CoroutineScope, senderMac: String = "") {
        Log.d("YANKI_MESH", "AĞDAN VERİ GELDİ! Boyut: ${data.size} byte")
        val parsedPayload = ProtoMapper.parseIncomingPayload(data)
        if (parsedPayload != null) {
            Log.d("YANKI_MESH", "Paket çözüldü: ${parsedPayload.messages.size} mesaj, ${parsedPayload.users.size} kullanıcı var.")
            scope.launch(Dispatchers.IO) {
                try {
                    database.withTransaction {
                        // Önce kullanıcı bilgilerini (Public Key) güncelle/kaydet ki mesaj imzaları doğrulanabilsin
                        parsedPayload.users.forEach { user ->
                            if (user.user_id != currentUserId) {
                                var existing = userDao.getUserById(user.user_id)

                                // Eğer tam ID ile bulunamazsa MAC adresiyle stub kaydı ara ve güncelle
                                if (existing == null && senderMac.isNotBlank()) {
                                    val stub = userDao.getUserByMac(senderMac)
                                    if (stub != null && stub.user_id != user.user_id) {
                                        // Stub kaydını sil, gerçek ID ile yenisini ekleyeceğiz
                                        userDao.deleteUserById(stub.user_id)
                                        existing = stub.copy(user_id = user.user_id)
                                        Log.d("YANKI_MESH", "Stub '${stub.user_id}' → gerçek ID '${user.user_id}' ile güncellendi")
                                    }
                                }

                                if (existing == null) {
                                    userDao.insertOrUpdateUser(user.copy(
                                        last_mac = if (senderMac.isNotBlank()) senderMac else user.last_mac
                                    ))
                                } else {
                                    userDao.insertOrUpdateUser(user.copy(
                                        last_mac = existing.last_mac ?: senderMac,
                                        last_rssi = existing.last_rssi,
                                        blood_type = existing.blood_type ?: user.blood_type,
                                        allergies = existing.allergies ?: user.allergies,
                                        medications = existing.medications ?: user.medications,
                                        emergency_contact = existing.emergency_contact ?: user.emergency_contact
                                    ))
                                }
                            }
                        }

                        parsedPayload.signals.forEach { signal -> handleEmergencySignal(signal) }
                        parsedPayload.messages.forEach { message -> handleIncomingMeshMessage(message) }
                        parsedPayload.bulletins.forEach { bulletin -> handleIncomingBulletin(bulletin) }
                    }

                    // Mesh'ten yeni veri geldi, eğer internet varsa hemen buluta gönder
                    if (parsedPayload.signals.isNotEmpty() || parsedPayload.messages.isNotEmpty() || parsedPayload.bulletins.isNotEmpty()) {
                        triggerImmediateCloudSync()
                    }
                } catch (e: Exception) {
                    Log.e("YANKI_MESH", "Payload işlenirken hata oluştu: ${e.message}")
                }
            }
        }
    }

    suspend fun handleNeighborFound(neighborId: String, neighborMacAddress: String, rssi: Int) {
        val currentTime = System.currentTimeMillis()
        val existingUser = userDao.getUserById(neighborId)

        if (existingUser != null) {
            userDao.updateLastSeen(userId = neighborId, timestamp = currentTime, macAddress = neighborMacAddress, rssi = rssi)
        } else {
            val newUser = UserEntity(
                user_id = neighborId,
                username = "Bilinmeyen Komşu",
                full_name = null,
                public_key = byteArrayOf(),
                last_seen = currentTime,
                last_mac = neighborMacAddress,
                last_rssi = rssi,
                is_trusted = false
            )
            userDao.insertOrUpdateUser(newUser)
        }
        
        // Kimlik duyurusu (Identity Announcement) yap: Kendi profilimizi yeni bulduğumuz komşuya gönder
        val myProfile = userDao.getUserById(currentUserId)
        if (myProfile != null && !neighborMacAddress.isNullOrBlank()) {
            val identityPayload = ProtoMapper.packageAll(
                signals = emptyList(),
                messages = emptyList(),
                users = listOf(myProfile)
            )
            Log.d("YANKI_MESH", "Kimlik duyurusu komşuya ($neighborId) gönderiliyor.")
            bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, identityPayload)
        }

        triggerGossipProtocol(neighborId, neighborMacAddress)
    }

    private fun triggerImmediateCloudSync() {
        try {
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
            val pendingBulletins = bulletinDao.getUnsyncedPosts()

            if (pendingSOS.isEmpty() && pendingMessages.isEmpty() && pendingBulletins.isEmpty()) return

            // Öncelik 1: SOS Sinyalleri (Her zaman BLE ile hızlıca fırlat)
            if (pendingSOS.isNotEmpty() && neighborMacAddress.isNotBlank()) {
                Log.d("YANKI_MESH", "Komşuya SOS paketleri gönderiliyor: $neighborMacAddress")
                val sosPayload = ProtoMapper.packageSOSOnly(pendingSOS)
                bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, sosPayload)
            }

            // Öncelik 2: Mesaj ve Duyuru Paketleme
            var needsWifiAware = false
            if (pendingMessages.isNotEmpty() || pendingBulletins.isNotEmpty()) {
                val messagePayload = ProtoMapper.packageAll(emptyList(), pendingMessages, emptyList(), pendingBulletins)
                
                if (messagePayload.size < 2000 && neighborMacAddress.isNotBlank()) {
                    Log.d("YANKI_MESH", "Veri paketi BLE üzerinden gönderiliyor: $neighborMacAddress")
                    bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, messagePayload)
                    
                    // Not: Burada hemen markAsSynced yapmıyoruz ki diğer komşulara da yayılabilsin.
                    // Bunun yerine Cloud Sync (SyncWorker) başarılı olduğunda işaretlenecek.
                } else {
                    needsWifiAware = true
                }
            }

            // Öncelik 3: Wi-Fi Aware
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

    // ==========================================
    // --- 5. AYARLAR & TERCİHLER ---
    // ==========================================
    fun setPreference(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    fun getPreference(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getSettingsFlow(): Flow<Map<String, Boolean>> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key != null) {
                launch {
                    send(getAllSettings())
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        send(getAllSettings())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun getAllSettings(): Map<String, Boolean> {
        return mapOf(
            "pref_wifi_aware" to sharedPreferences.getBoolean("pref_wifi_aware", true),
            "pref_ble_mode" to sharedPreferences.getBoolean("pref_ble_mode", true),
            "pref_trusted_only" to sharedPreferences.getBoolean("pref_trusted_only", false),
            "pref_sos_notifications" to sharedPreferences.getBoolean("pref_sos_notifications", true),
            "pref_discovery_notifications" to sharedPreferences.getBoolean("pref_discovery_notifications", false),
            "pref_aes_gcm" to sharedPreferences.getBoolean("pref_aes_gcm", true)
        )
    }
}
