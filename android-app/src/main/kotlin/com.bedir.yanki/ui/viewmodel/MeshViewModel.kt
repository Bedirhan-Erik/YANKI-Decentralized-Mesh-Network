package com.bedir.yanki.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.repository.YankiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.bedir.yanki.ui.radar.NeighborPoint
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MeshViewModel @Inject constructor(
    val repository: YankiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _meshStatus = MutableStateFlow(MeshUiState())
    val meshStatus: StateFlow<MeshUiState> = _meshStatus.asStateFlow()

    private val _userStatus = MutableStateFlow(UserStatus())
    val userStatus: StateFlow<UserStatus> = _userStatus.asStateFlow()

    init {
        observeMeshData()
        observeMessages()
        startStatusTracking()
    }

    private fun startStatusTracking() {
        viewModelScope.launch {
            while (true) {
                val battery = getBatteryLevel()
                val location = getLastLocation()
                _userStatus.update { 
                    it.copy(
                        batteryLevel = battery,
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0
                    )
                }
                delay(10000) // 10 saniyede bir güncelle
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
    }

    private fun getLastLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            bestLocation
        } catch (e: SecurityException) {
            null
        }
    }

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val allMessages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getAllMessagesFlow().collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun getMessagesWithUser(userId: String): Flow<List<MessageEntity>> {
        return repository.getChatHistory(userId) // Repository'den DAO'ya gider
    }

    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.sendMessage(content, receiverId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeMeshData() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { users ->
                try {
                    val calculatedRange = "${users.size * 25}m"

                    // UserEntity listesini RadarView'in beklediği NeighborPoint listesine çevir
                    val neighborPoints = users.map { user ->
                        // Her kullanıcı için sabit ama benzersiz bir konum üret (ID'ye göre)
                        val random = java.util.Random(user.user_id.hashCode().toLong())
                        val xRatio = (random.nextFloat() * 1.6f) - 0.8f // -0.8 ile 0.8 arası
                        val yRatio = (random.nextFloat() * 1.6f) - 0.8f
                        
                        // Son 30 saniye içinde görüldüyse online sayalım
                        val isOnline = System.currentTimeMillis() - user.last_seen < 30000
                        
                        NeighborPoint(xRatio, yRatio, isOnline)
                    }

                    _meshStatus.update { it.copy(
                        neighborCount = users.size,
                        neighbors = users,
                        neighborPoints = neighborPoints,
                        coverageRange = if (users.isEmpty()) "0m" else calculatedRange,
                        isInternetAvailable = checkRealTimeInternet()
                    )}
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkRealTimeInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private val _sosEvent = MutableSharedFlow<String>()
    val sosEvent: SharedFlow<String> = _sosEvent.asSharedFlow()

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
                _sosEvent.emit("Acil durum sinyali mesh ağına yayılıyor!")
            } catch (e: Exception) {
                // Eğer hala hata varsa logcat'e yazdırır, uygulamayı çökertmez
                e.printStackTrace()
                _sosEvent.emit("Sinyal gönderilemedi: ${e.message}")
            }
        }
    }

    fun startMeshService() {
        _meshStatus.update { it.copy(isMeshActive = true) }
        val intent = Intent(context, com.bedir.yanki.services.MeshService::class.java)
        context.startForegroundService(intent)
    }
}

data class MeshUiState(
    val isMeshActive: Boolean = false,
    val neighborCount: Int = 0,
    val neighbors: List<com.bedir.yanki.data.local.entity.UserEntity> = emptyList(),
    val neighborPoints: List<NeighborPoint> = emptyList(),
    val isInternetAvailable: Boolean = false,
    val coverageRange: String = "0m"
)

data class UserStatus(
    val batteryLevel: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
