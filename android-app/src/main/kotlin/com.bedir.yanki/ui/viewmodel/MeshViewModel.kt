package com.bedir.yanki.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.repository.YankiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MeshViewModel @Inject constructor(
    val repository: YankiRepository
) : ViewModel() {

    private val _meshStatus = MutableStateFlow(MeshUiState())
    val meshStatus: StateFlow<MeshUiState> = _meshStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())

    init {
        observeMeshData()
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            while (true) {
                // Not ideal, but repository doesn't have Flow yet.
                // Assuming we want messages for all or specific? 
                // Repository has getChatHistory(userId)
                delay(2000)
            }
        }
    }

    fun getMessagesWithUser(userId: String): Flow<List<MessageEntity>> {
        return repository.getChatHistory(userId) // Repository'den DAO'ya gider
    }

    // Mesaj gönderme fonksiyonu
    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val messageEntity = MessageEntity(
                msg_id = UUID.randomUUID().toString(),
                sender_id = "BENIM_ID", // TODO: Kendi ID'ni SharedPreferences'tan almalısın
                receiver_id = receiverId,
                content_blob = content.toByteArray(),
                timestamp = System.currentTimeMillis(),
                status = 1, // Gönderiliyor
                is_synced = false
            )
            repository.saveMessage(messageEntity)
            // Kaydedildiği an, aşağıdaki Flow bunu otomatik fark edip ekrana basacak!
        }
    }

    private fun observeMeshData() {
        viewModelScope.launch {
            while (true) {
                try {
                    val users = repository.getAllUsers()
                    val calculatedRange = "${users.size * 25}m"

                    _meshStatus.update { it.copy(
                        neighborCount = users.size,
                        neighbors = users,
                        coverageRange = if (users.isEmpty()) "0m" else calculatedRange,
                        isInternetAvailable = checkRealTimeInternet()
                    )}
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    private fun checkRealTimeInternet(): Boolean {
        return false
    }

    fun sendEmergencySOS(type: String, lat: Double, lon: Double, battery: Int) {
        // Dispatchers.IO eklendi! Artık Android kızmayacak.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newSignal = EmergencySignalEntity(
                    signal_id = UUID.randomUUID().toString(),
                    user_id = repository.currentUserId,
                    latitude = lat,
                    longitude = lon,
                    emergency_type = type,
                    battery_level = battery,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveEmergencySignal(newSignal)
            } catch (e: Exception) {
                // Eğer hala hata varsa logcat'e yazdırır, uygulamayı çökertmez
                e.printStackTrace()
            }
        }
    }

    fun startMeshService() {
        _meshStatus.update { it.copy(isMeshActive = true) }
    }
}

data class MeshUiState(
    val isMeshActive: Boolean = false,
    val neighborCount: Int = 0,
    val neighbors: List<com.bedir.yanki.data.local.entity.UserEntity> = emptyList(),
    val isInternetAvailable: Boolean = false,
    val coverageRange: String = "0m"
)
