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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class WifiAwareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isAwareActive = MutableStateFlow(false)
    val isAwareActive: StateFlow<Boolean> = _isAwareActive.asStateFlow()

    private var awareSession: WifiAwareSession? = null
    private var currentDiscoverySession: DiscoverySession? = null
    
    private var isPublishing = false
    private var isSubscribing = false

    private val _dataReceivedFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val dataReceivedFlow = _dataReceivedFlow.asSharedFlow()

    private val _socketReadyFlow = MutableSharedFlow<Socket>(extraBufferCapacity = 5)
    val socketReadyFlow = _socketReadyFlow.asSharedFlow()

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

        val wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as android.net.wifi.aware.WifiAwareManager?
        if (wifiAwareManager == null || !wifiAwareManager.isAvailable) {
            Log.e("YANKI_AWARE", "Wi-Fi Aware şu an kapalı veya kullanılamıyor.")
            return
        }

        if (awareSession != null) {
            onAttached()
            return
        }

        wifiAwareManager.attach(object : AttachCallback() {
            override fun onAttached(session: WifiAwareSession) {
                awareSession = session
                _isAwareActive.value = true
                onAttached()
            }

            override fun onAttachFailed() {
                Log.e("YANKI_AWARE", "Wi-Fi Aware oturumu açılamadı.")
                _isAwareActive.value = false
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 2. ADIM: Yayıncı Ol
    fun startPublishing() {
        if (isPublishing) return

        val config = PublishConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                currentDiscoverySession = session
                isPublishing = true
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createNetworkForPublisher(peerHandle)
                }
            }
            
            override fun onSessionTerminated() {
                isPublishing = false
            }
        }, Handler(Looper.getMainLooper()))
    }

    // 3. ADIM: Dinleyici Ol
    fun startSubscribing() {
        if (isSubscribing) return

        val config = SubscribeConfig.Builder()
            .setServiceName(YANKI_AWARE_SERVICE_NAME)
            .build()

        awareSession?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                currentDiscoverySession = session
                isSubscribing = true
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                currentDiscoverySession?.sendMessage(peerHandle, 0, "CONNECT_REQ".toByteArray())
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createNetworkForSubscriber(peerHandle, 8888, "fe80::") 
                }
            }

            override fun onSessionTerminated() {
                isSubscribing = false
            }
        }, Handler(Looper.getMainLooper()))
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
                scope.launch {
                    try {
                        ServerSocket(8888).use { serverSocket ->
                            val clientSocket = serverSocket.accept()
                            _socketReadyFlow.emit(clientSocket)
                            receiveDataOverSocket(clientSocket)
                        }
                    } catch (e: Exception) {
                        Log.e("YANKI_AWARE", "Sunucu Soket Hatası: ${e.message}")
                    }
                }
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
                scope.launch {
                    try {
                        val socket = network.socketFactory.createSocket(publisherIpv6, publisherPort)
                        _socketReadyFlow.emit(socket)
                        receiveDataOverSocket(socket)
                    } catch (e: Exception) {
                        Log.e("YANKI_AWARE", "İstemci Soket Hatası: ${e.message}")
                    }
                }
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
        _isAwareActive.value = false
    }

    fun sendDataOverSocket(socket: Socket, payload: ByteArray) {
        scope.launch {
            try {
                val outputStream = DataOutputStream(socket.getOutputStream())
                outputStream.writeInt(payload.size)
                outputStream.write(payload)
                outputStream.flush()
            } catch (e: Exception) {
                Log.e("YANKI_AWARE", "Veri Gönderme Hatası: ${e.message}")
            }
        }
    }

    private suspend fun receiveDataOverSocket(socket: Socket) {
        try {
            val inputStream = DataInputStream(socket.getInputStream())
            while (!socket.isClosed) {
                val payloadSize = inputStream.readInt()
                if (payloadSize <= 0) break
                
                val payload = ByteArray(payloadSize)
                inputStream.readFully(payload)
                _dataReceivedFlow.emit(payload)
            }
        } catch (e: Exception) {
            Log.e("YANKI_AWARE", "Bağlantı Koptu: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
