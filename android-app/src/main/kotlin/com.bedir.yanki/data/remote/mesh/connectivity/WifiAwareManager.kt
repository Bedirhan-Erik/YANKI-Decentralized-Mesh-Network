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
    
    private var isPublishing = false
    private var isSubscribing = false

    companion object {
        private const val YANKI_AWARE_SERVICE_NAME = "YANKI_AWARE_MESH"
        private const val YANKI_PSK_PASSPHRASE = "YANKI_GIZLI_SIFRE_123"
    }

    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var onSocketReady: ((Socket) -> Unit)? = null

    fun setOnDataReceivedListener(listener: (ByteArray) -> Unit) {
        onDataReceived = listener
    }

    fun setOnSocketReadyListener(listener: (Socket) -> Unit) {
        onSocketReady = listener
    }

    // 1. ADIM: Sisteme Bağlan (Attach)
    fun attachToAwareSystem(onAttached: () -> Unit) {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Log.e("YANKI_AWARE", "Bu cihaz Wi-Fi Aware desteklemiyor!")
            return
        }

        val wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as android.net.wifi.aware.WifiAwareManager?
        if (wifiAwareManager == null || !wifiAwareManager.isAvailable) {
            Log.e("YANKI_AWARE", "Wi-Fi Aware şu an kapalı veya kullanılamıyor.")
            return
        }

        if (awareSession != null) {
            Log.d("YANKI_AWARE", "Oturum zaten açık.")
            onAttached()
            return
        }

        wifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                Log.d("YANKI_AWARE", "Wi-Fi Aware oturumu başarıyla açıldı.")
                onAttached()
            }

            override fun onAttachFailed() {
                Log.e("YANKI_AWARE", "Wi-Fi Aware oturumu açılamadı.")
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 2. ADIM: Yayıncı Ol
    fun startPublishing() {
        if (isPublishing) {
            Log.d("YANKI_AWARE", "Yayın zaten aktif.")
            return
        }

        val config = PublishConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                currentDiscoverySession = session
                isPublishing = true
                Log.d("YANKI_AWARE", "Yayıncı: Servis yayını başladı!")
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                Log.d("YANKI_AWARE", "Yayıncı: Mesaj (Port/IP) geldi, ağ kuruluyor...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createNetworkForPublisher(peerHandle)
                }
            }
            
            override fun onSessionTerminated() {
                isPublishing = false
                Log.d("YANKI_AWARE", "Yayıncı oturumu sonlandırıldı.")
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 3. ADIM: Dinleyici Ol
    fun startSubscribing() {
        if (isSubscribing) {
            Log.d("YANKI_AWARE", "Tarama zaten aktif.")
            return
        }

        val config = SubscribeConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                currentDiscoverySession = session
                isSubscribing = true
                Log.d("YANKI_AWARE", "Dinleyici: Tarama başladı...")
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                Log.d("YANKI_AWARE", "Dinleyici: Yayıncı BULUNDU! Bağlantı isteği gönderiliyor...")
                // Karşı tarafa "ben buradayım, ağ kuralım" mesajı gönder
                currentDiscoverySession?.sendMessage(peerHandle, 0, "CONNECT_REQ".toByteArray())
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // IPv6 adresi NAN protokolü içinde otomatik çözülür, 
                    // şimdilik default port ve link-local üzerinden deniyoruz
                    createNetworkForSubscriber(peerHandle, 8888, "fe80::") 
                }
            }

            override fun onSessionTerminated() {
                isSubscribing = false
                Log.d("YANKI_AWARE", "Dinleyici oturumu sonlandırıldı.")
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
                        val serverSocket = ServerSocket(8888)
                        val clientSocket = serverSocket.accept()
                        serverSocket.close() // Sadece bir bağlantı bekliyoruz

                        Log.d("YANKI_AWARE", "Bağlantı kabul edildi.")
                        onSocketReady?.invoke(clientSocket)
                        receiveDataOverSocket(clientSocket) { data ->
                            onDataReceived?.invoke(data)
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
                        // NOT: fe80:: adresi link-local başlangıcıdır. Wi-Fi Aware ağında
                        // yayıncıya bağlanmak için gerçek IPv6 adresi gereklidir.
                        val socket = network.socketFactory.createSocket(publisherIpv6, publisherPort)
                        Log.d("YANKI_AWARE", "Dinleyici: Bağlandı: ${socket.inetAddress}")
                        
                        onSocketReady?.invoke(socket)
                        
                        receiveDataOverSocket(socket) { data ->
                            onDataReceived?.invoke(data)
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
        isPublishing = false
        isSubscribing = false
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
                val inputStream = DataInputStream(socket.getInputStream())

                while (!socket.isClosed) {
                    val payloadSize = inputStream.readInt()
                    if (payloadSize <= 0) break
                    
                    val payload = ByteArray(payloadSize)
                    inputStream.readFully(payload)
                    onDataReceived(payload)
                }
            } catch (e: Exception) {
                Log.e("YANKI_AWARE", "Veri Okuma Hatası veya Bağlantı Koptu: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }.start()
    }
}
