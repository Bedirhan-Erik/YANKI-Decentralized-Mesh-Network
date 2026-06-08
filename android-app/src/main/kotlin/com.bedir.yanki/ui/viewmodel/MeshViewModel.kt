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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.bedir.yanki.ui.radar.NeighborPoint
import java.util.UUID
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MeshViewModel @Inject constructor(
    val repository: YankiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _meshStatus = MutableStateFlow(MeshUiState())
    val meshStatus: StateFlow<MeshUiState> = _meshStatus.asStateFlow()

    private val _userStatus = MutableStateFlow(UserStatus())
    val userStatus: StateFlow<UserStatus> = _userStatus.asStateFlow()

    val isUserRegistered: StateFlow<Boolean> = repository.isUserRegisteredFlow

    val currentUser: StateFlow<com.bedir.yanki.data.local.entity.UserEntity?> = 
        isUserRegistered.flatMapLatest { registered ->
            if (registered) {
                repository.getUserFlow(repository.currentUserId)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<Map<String, Boolean>> = repository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updateSetting(key: String, value: Boolean) {
        repository.setPreference(key, value)
    }

    init {
        observeMeshData()
        observeMessages()
        observeBulletins()
        observeMeshStatus()
        startStatusTracking()
    }

    private fun observeMeshStatus() {
        combine(
            repository.bleMeshManager.isScanning,
            repository.bleMeshManager.isAdvertising,
            repository.wifiAwareManager.isAwareActive
        ) { scanning, advertising, awareActive ->
            Triple(scanning, advertising, awareActive)
        }.onEach { (scanning, advertising, awareActive) ->
            _meshStatus.update { it.copy(
                isMeshActive = scanning || advertising || awareActive,
                isWifiAwareActive = awareActive
            ) }
        }.launchIn(viewModelScope)
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
                delay(10000) // 10 saniyede bir batarya/konum güncellemesi yeterli
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

    private val _bulletins = MutableStateFlow<List<com.bedir.yanki.data.local.entity.BulletinEntity>>(emptyList())
    val allBulletins: StateFlow<List<com.bedir.yanki.data.local.entity.BulletinEntity>> = _bulletins.asStateFlow()

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getAllMessagesFlow().collect { messages ->
                _messages.value = messages
            }
        }
    }

    private fun observeBulletins() {
        viewModelScope.launch {
            repository.getAllBulletinsFlow().collect { bulletins ->
                _bulletins.value = bulletins
            }
        }
    }

    fun postBulletin(content: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val location = getLastLocation()
            repository.postToBulletin(
                content = content,
                type = type,
                lat = location?.latitude ?: 0.0,
                lon = location?.longitude ?: 0.0
            )
        }
    }

    fun getMessagesWithUser(userId: String): Flow<List<MessageEntity>> {
        return repository.getChatHistory(userId)
    }

    fun getLastMessageWithUser(userId: String) = repository.getLastMessageWithUser(userId)

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
            repository.getAllUsersFlow().collect { allUsers ->
                try {
                    // Kendi ID'mizi listeden çıkar (Radar'da kendimizi komşu olarak görmeyelim)
                    val myId = repository.currentUserId
                    val users = allUsers.filter { it.user_id != myId }

                    val calculatedRange = "${users.size * 25}m"

                    // UserEntity listesini RadarView'in beklediği NeighborPoint listesine çevir (Canlı RSSI/Mesafe)
                    val neighborPoints = users.map { user ->
                        // RSSI verisini mesafeye (yarıçapa) dönüştür: -40 (yakın) -> -100 (uzak)
                        val rssi = user.last_rssi.toFloat()
                        val normalizedRadius = ((rssi + 30) / -70f).coerceIn(0.15f, 0.9f)
                        
                        // User ID'ye göre sabit ama her kullanıcı için farklı bir açı (Radyan)
                        val angle = (abs(user.user_id.hashCode()) % 360) * (PI / 180.0)
                        
                        val xRatio = (normalizedRadius * cos(angle)).toFloat()
                        val yRatio = (normalizedRadius * sin(angle)).toFloat()
                        
                        // Son 60 saniye içinde görüldüyse online sayalım
                        val isOnline = System.currentTimeMillis() - user.last_seen < 60000
                        
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository'deki merkezi SOS fonksiyonunu kullanıyoruz
                repository.sendEmergencySignal(type, lat, lon, battery)
                _sosEvent.emit("Acil durum sinyali mesh ağına yayılıyor!")
            } catch (e: Exception) {
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

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            // Stop service on logout
            context.stopService(Intent(context, com.bedir.yanki.services.MeshService::class.java))
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            // Stop service on reset
            context.stopService(Intent(context, com.bedir.yanki.services.MeshService::class.java))
            onComplete()
        }
    }
}

data class MeshUiState(
    val isMeshActive: Boolean = false,
    val isWifiAwareActive: Boolean = false,
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
