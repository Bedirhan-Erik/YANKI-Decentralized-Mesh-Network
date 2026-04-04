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
import com.bedir.yanki.security.SecurityManager
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
    private val securityManager: SecurityManager // SecurityManager eklendi
) {
    companion object {
        const val STATUS_RECEIVED = 0
        const val STATUS_RELAYED = 1
    }

    val currentUserId: String = "MY_LOCAL_USER_ID"

    // ==========================================
    // --- 1. KULLANICI İŞLEMLERİ ---
    // ==========================================
    suspend fun saveUser(user: UserEntity) = userDao.insertOrUpdateUser(user)
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    suspend fun getAllUsers() = userDao.getAllUsers()

    // ==========================================
    // --- 2. MESAJ İŞLEMLERİ ---
    // ==========================================
    suspend fun saveMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun getUnsyncedMessages() = messageDao.getUnsyncedMessages()
    suspend fun markMessageAsSynced(msgId: String) = messageDao.markAsSynced(msgId)
    fun getChatHistory(userId: String) = messageDao.getChatHistory(userId)

    suspend fun sendMessage(content: String, receiverId: String) {
        val myPrivateKey = getMySecretKey()
        val messageBytes = content.toByteArray()

        // Mesajı imzala
        val signature = securityManager.signData(messageBytes, myPrivateKey)

        val messageEntity = MessageEntity(
            msg_id = UUID.randomUUID().toString(),
            sender_id = currentUserId,
            receiver_id = receiverId,
            content_blob = messageBytes,
            signature = signature,
            timestamp = System.currentTimeMillis(),
            status = STATUS_RELAYED,
            is_synced = false,
            ttl = 7
        )

        saveMessage(messageEntity)
        // TODO: Mesh yayını için Gossip tetiklenebilir
    }

    private fun getMySecretKey(): ByteArray {
        // TODO: SharedPreferences veya EncryptedSharedPreferences üzerinden çekilecek
        return ByteArray(32) // Geçici 0-byte array
    }

    suspend fun handleIncomingMeshMessage(incomingMessage: MessageEntity) {
        val alreadySeen = messageDao.isMessageExists(incomingMessage.msg_id)
        if (alreadySeen) return

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
    }

    // ==========================================
    // --- 3. ACİL DURUM (SOS) İŞLEMLERİ ---
    // ==========================================
    suspend fun saveEmergencySignal(signal: EmergencySignalEntity) = emergencySignalDao.insertSignal(signal)
    suspend fun getAllSignals() = emergencySignalDao.getAllSignals()

    suspend fun handleEmergencySignal(signal: EmergencySignalEntity) {
        if (emergencySignalDao.isSignalExists(signal.signal_id)) return
        emergencySignalDao.insertSignal(signal)

        if (signal.hop_count < 10) {
            val relayedSignal = signal.copy(hop_count = signal.hop_count + 1)
            Log.d("YANKI_MESH", "SOS Sinyali yönlendiriliyor: ${relayedSignal.signal_id}")
        }
    }

    // ==========================================
    // --- 4. MESH & GOSSIP PROTOKOLÜ ---
    // ==========================================
    suspend fun handleNeighborFound(neighborId: String, neighborMacAddress: String) {
        val currentTime = System.currentTimeMillis()
        val existingUser = userDao.getUserById(neighborId)

        if (existingUser != null) {
            userDao.updateLastSeen(userId = neighborId, timestamp = currentTime)
        } else {
            val newUser = UserEntity(
                user_id = neighborId,
                username = "Bilinmeyen Komşu",
                public_key = byteArrayOf(),
                last_seen = currentTime,
                is_trusted = false
            )
            userDao.insertOrUpdateUser(newUser)
        }
        triggerGossipProtocol(neighborId, neighborMacAddress)
    }

    private suspend fun triggerGossipProtocol(neighborId: String, neighborMacAddress: String) {
        val pendingSOS = emergencySignalDao.getPendingSignals()
        val pendingMessages = messageDao.getPendingMessages()

        if (pendingSOS.isEmpty() && pendingMessages.isEmpty()) return

        if (pendingMessages.isNotEmpty()) {
            val payload = ProtoMapper.packageAll(pendingSOS, pendingMessages)
            wifiAwareManager.attachToAwareSystem {
                wifiAwareManager.startPublishing()
            }
        } else if (pendingSOS.isNotEmpty()) {
            val payload = ProtoMapper.packageSOSOnly(pendingSOS)
            bleMeshManager.sendPayloadToNeighbor(neighborMacAddress, payload)
        }
    }
}
