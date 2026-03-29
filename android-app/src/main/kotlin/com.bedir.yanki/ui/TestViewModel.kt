import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.remote.mesh.BleMeshManager
import com.bedir.yanki.repository.YankiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val repository: YankiRepository,
    private val bleManager: BleMeshManager
) : ViewModel() {

    private val _foundDevices = MutableStateFlow<List<String>>(emptyList())
    val foundDevices = _foundDevices.asStateFlow()

    // Test amaçlı bir kullanıcı oluşturup DB'ye kaydet
    fun createDummyUser() {
        viewModelScope.launch {
            val user = UserEntity(
                user_id = UUID.randomUUID().toString(),
                user_name = "Test Kullanıcısı ${Random().nextInt(100)}",
                public_key = null,
                last_seen = System.currentTimeMillis()
            )
            repository.saveUser(user)
        }
    }

    // BLE Taramasını Başlat
    fun startBleTest() {
        bleManager.startAdvertising("YANKI-NODE-TEST")
        bleManager.startScanning { deviceName ->
            if (!_foundDevices.value.contains(deviceName)) {
                _foundDevices.value += deviceName
            }
        }
    }
}