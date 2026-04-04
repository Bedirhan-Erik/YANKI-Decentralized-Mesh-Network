package com.bedir.yanki.data.remote.mesh.connectivity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.ServerSocket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission") // İzinleri UI tarafında hallettiğimizi varsayıyoruz
class WifiAwareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiAwareSystemManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as android.net.wifi.aware.WifiAwareManager?
    private var awareSession: WifiAwareSession? = null
    private var currentDiscoverySession: DiscoverySession? = null

    companion object {
        private const val YANKI_AWARE_SERVICE_NAME = "YANKI_AWARE_MESH"
        private const val YANKI_PSK_PASSPHRASE = "YANKI_GIZLI_SIFRE_123"
    }

    // 1. ADIM: Sisteme Bağlan (Attach)
    fun attachToAwareSystem(onAttached: () -> Unit) {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Log.e("YANKI_AWARE", "Bu cihaz Wi-Fi Aware desteklemiyor!")
            return
        }

        if (wifiAwareSystemManager?.isAvailable == true) {
            wifiAwareSystemManager.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    awareSession = session
                    Log.d("YANKI_AWARE", "Wi-Fi Aware oturumu başarıyla açıldı.")
                    onAttached()
                }

                override fun onAttachFailed() {
                    Log.e("YANKI_AWARE", "Wi-Fi Aware oturumu açılamadı.")
                }
            }, Handler(Looper.getMainLooper()))
        } else {
            Log.e("YANKI_AWARE", "Wi-Fi Aware şu an kapalı veya kullanılamıyor.")
        }
    }

    // 2. ADIM: Yayıncı Ol
    fun startPublishing() {
        val config = PublishConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                currentDiscoverySession = session
                Log.d("YANKI_AWARE", "Yayıncı: Servis yayını başladı!")
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                Log.d("YANKI_AWARE", "Yayıncı: Mesaj geldi: ${message.size} byte")
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 3. ADIM: Dinleyici Ol
    fun startSubscribing() {
        val config = SubscribeConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                currentDiscoverySession = session
                Log.d("YANKI_AWARE", "Dinleyici: Tarama başladı...")
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                Log.d("YANKI_AWARE", "Dinleyici: Yayıncı BULUNDU!")
                // Otomatik olarak ağ kurmaya çalışabiliriz
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Port ve IP bilgisi discovery mesajı ile gelmeli, şimdilik placeholder
                    // createNetworkForSubscriber(peerHandle, 8888, "fe80::...") 
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // ==========================================
    // --- 4. AĞ KÖPRÜSÜNÜ (SOCKET) KURMA ---
    // ==========================================

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createNetworkForPublisher(peerHandle: PeerHandle) {
        val session = currentDiscoverySession ?: return
        
        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(session, peerHandle)
            .setPskPassphrase(YANKI_PSK_PASSPHRASE)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("YANKI_AWARE", "Yayıncı: Sanal Ağ Kuruldu!")

                Thread {
                    try {
                        ServerSocket(0).use { serverSocket ->
                            Log.d("YANKI_AWARE", "Yayıncı: Port ${serverSocket.localPort} üzerinde dinleniyor...")
                            val clientSocket = serverSocket.accept()
                            Log.d("YANKI_AWARE", "Yayıncı: Bağlantı kabul edildi: ${clientSocket.inetAddress}")
                            // TODO: Veri Transferi
                        }
                    } catch (e: Exception) {
                        Log.e("YANKI_AWARE", "Sunucu Soket Hatası: ${e.message}")
                    }
                }.start()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createNetworkForSubscriber(peerHandle: PeerHandle, publisherPort: Int, publisherIpv6: String) {
        val session = currentDiscoverySession ?: return

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(session, peerHandle)
            .setPskPassphrase(YANKI_PSK_PASSPHRASE)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("YANKI_AWARE", "Dinleyici: Sanal Ağ Kuruldu!")

                Thread {
                    try {
                        network.socketFactory.createSocket(publisherIpv6, publisherPort).use { socket ->
                            Log.d("YANKI_AWARE", "Dinleyici: Bağlandı: ${socket.inetAddress}")
                            // TODO: Veri Transferi
                        }
                    } catch (e: Exception) {
                        Log.e("YANKI_AWARE", "İstemci Soket Hatası: ${e.message}")
                    }
                }.start()
            }
        })
    }

    fun stopAware() {
        currentDiscoverySession?.close()
        currentDiscoverySession = null
        awareSession?.close()
        awareSession = null
        Log.d("YANKI_AWARE", "Wi-Fi Aware kapatıldı.")
    }
    // ==========================================
    // --- 5. VERİ TRANSFERİ (AKIKIŞ) KONTROLÜ ---
    // ==========================================

    // Soket üzerinden karşıya devasa paketleri fırlatma
    fun sendDataOverSocket(socket: Socket, payload: ByteArray) {
        Thread {
            try {
                // Veriyi yollamak için bir "çıkış borusu" (OutputStream) oluşturuyoruz
                val outputStream = DataOutputStream(socket.getOutputStream())

                Log.d("YANKI_AWARE", "Gönderilecek veri boyutu: ${payload.size} byte")

                // 1. Karşı tarafa önce paketin ne kadar büyük olduğunu söylüyoruz
                outputStream.writeInt(payload.size)

                // 2. Ardından verinin kendisini (Protobuf baytlarını) basıyoruz
                outputStream.write(payload)
                outputStream.flush() // Boruda kalanları zorla it

                Log.d("YANKI_AWARE", "Veri başarıyla karşıya fırlatıldı!")

            } catch (e: Exception) {
                Log.e("YANKI_AWARE", "Veri Gönderme Hatası: ${e.message}")
            }
        }.start()
    }

    // Karşıdan gelen paketi karşılama
    fun receiveDataOverSocket(socket: Socket, onDataReceived: (ByteArray) -> Unit) {
        Thread {
            try {
                // Veriyi almak için bir "giriş borusu" (InputStream) oluşturuyoruz
                val inputStream = DataInputStream(socket.getInputStream())

                while (!socket.isClosed) {
                    // 1. Önce karşı tarafın yollayacağı verinin boyutunu okumayı bekle
                    val payloadSize = inputStream.readInt()
                    Log.d("YANKI_AWARE", "Karşıdan $payloadSize byte boyutunda bir paket geliyor...")

                    // 2. Gelecek veri büyüklüğünde boş bir havuz (ByteArray) oluştur
                    val payload = ByteArray(payloadSize)

                    // 3. Havuzu gelen veriyle tamamen doldur
                    inputStream.readFully(payload)
                    Log.d("YANKI_AWARE", "Veri başarıyla alındı ve tam paketlendi!")

                    // Gelen veriyi (ByteArray) çözümlemesi için Repository'ye veya Mapper'a pasla
                    onDataReceived(payload)
                }
            } catch (e: Exception) {
                Log.e("YANKI_AWARE", "Veri Okuma Hatası veya Bağlantı Koptu: ${e.message}")
            }
        }.start()
    }
}
