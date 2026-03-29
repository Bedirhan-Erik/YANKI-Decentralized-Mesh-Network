import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import javax.inject.Inject

class WifiAwareManager @Inject constructor(
    private val context: Context
) {
    private var wifiAwareSession: WifiAwareSession? = null
    private val wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?

    // 1. Wi-Fi Aware Servisine Bağlanma
    fun attach() {
        if (wifiAwareManager?.isAvailable == true) {
            wifiAwareManager.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    wifiAwareSession = session
                    Log.d("YANKI_WIFI", "Wi-Fi Aware servisine başarıyla bağlanıldı.")
                }
                override fun onAttachFailed() {
                    Log.e("YANKI_WIFI", "Wi-Fi Aware bağlanma hatası.")
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    // 2. Servis Yayınlama (Publish) - Mesaj Gönderen Düğüm
    fun publishService() {
        val config = PublishConfig.Builder()
            .setServiceName("YANKI_MESH_SERVICE")
            .build()

        wifiAwareSession?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                Log.d("YANKI_WIFI", "YANKI Servisi yayınlanmaya başladı.")
            }
            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                // Komşu cihazdan veri geldiğinde burası tetiklenir
                Log.d("YANKI_WIFI", "Veri alındı: ${message.size} byte")
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 3. Servis Arama (Subscribe) - Mesaj Arayan/Alan Düğüm
    fun subscribeToService() {
        val config = SubscribeConfig.Builder()
            .setServiceName("YANKI_MESH_SERVICE")
            .build()

        wifiAwareSession?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                Log.d("YANKI_WIFI", "YANKI Servisi aranıyor...")
            }
            override fun onServiceDiscovered(peerHandle: PeerHandle, serviceSpecificInfo: ByteArray, matchFilter: List<ByteArray>) {
                Log.d("YANKI_WIFI", "Bir YANKI düğümü bulundu!")
                // Burada peerHandle kullanarak veri gönderimi başlatılabilir
            }
        }, Handler(Looper.getMainLooper()))
    }
}