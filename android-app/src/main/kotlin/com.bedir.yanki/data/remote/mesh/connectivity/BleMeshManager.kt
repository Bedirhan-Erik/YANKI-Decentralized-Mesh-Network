package com.bedir.yanki.data.remote.mesh.connectivity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class BleMeshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        val YANKI_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val YANKI_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }

    private val scanner get() = bluetoothAdapter?.bluetoothLeScanner
    private val advertiser get() = bluetoothAdapter?.bluetoothLeAdvertiser
    private var scanCallback: ScanCallback? = null
    private var gattServer: BluetoothGattServer? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null

    // --- Parçalama (Chunking) Yapısı ---
    private var messageIdCounter = 0
    private val reassemblers = ConcurrentHashMap<String, Reassembler>()

    private class Reassembler(val totalChunks: Int) {
        val chunks = mutableMapOf<Int, ByteArray>()
        fun isComplete() = chunks.size == totalChunks
        fun getFullData(): ByteArray {
            val sortedKeys = chunks.keys.sorted()
            var totalSize = 0
            for (k in sortedKeys) totalSize += chunks[k]?.size ?: 0
            val result = ByteArray(totalSize)
            var offset = 0
            for (k in sortedKeys) {
                val chunkData = chunks[k] ?: continue
                System.arraycopy(chunkData, 0, result, offset, chunkData.size)
                offset += chunkData.size
            }
            return result
        }
    }

    fun setOnDataReceivedListener(listener: (ByteArray) -> Unit) {
        onDataReceived = listener
    }

    // Aynı komşuyu sürekli işlememek için throttle listesi (MAC -> Son Görülme Zamanı)
    private val recentlySeenNeighbors = mutableMapOf<String, Long>()
    private val RECENTLY_SEEN_THRESHOLD = 10000L // 10 saniye

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private var isScanning = false
    private var isAdvertising = false
    private var isGattServerRunning = false

    @SuppressLint("MissingPermission")
    fun startAdvertising(myUserId: String) {
        if (!hasBluetoothPermission() || advertiser == null) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik veya cihaz desteklemiyor.")
            return
        }

        if (isAdvertising) {
            Log.d("YANKI_BLE", "Yayın zaten aktif, tekrar başlatılmıyor.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true) // GATT bağlantısı (GattServer) için true olmalı
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(YANKI_SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()

        // User ID'yi ScanResponse (ikincil paket) içine koyarsak ana pakette yer açılır
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(YANKI_SERVICE_UUID), myUserId.toByteArray())
            .build()

        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
            isAdvertising = true
        } catch (e: Exception) {
            Log.e("YANKI_BLE", "Yayın başlatılırken hata oluştu", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(onNeighborFound: (id: String, address: String, rssi: Int) -> Unit) {
        if (!hasBluetoothPermission() || scanner == null) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik veya cihaz desteklemiyor.")
            return
        }

        if (isScanning) {
            Log.d("YANKI_BLE", "Tarama zaten aktif.")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(YANKI_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val address = result.device.address
                    val currentTime = System.currentTimeMillis()

                    // Throttling: Eğer bu cihazı son 10 saniye içinde işlediysek pas geç
                    val lastSeen = recentlySeenNeighbors[address] ?: 0L
                    if (currentTime - lastSeen < RECENTLY_SEEN_THRESHOLD) return

                    val scanRecord = result.scanRecord
                    // Önce ServiceData'ya bak (Eski yöntem)
                    var serviceData = scanRecord?.getServiceData(ParcelUuid(YANKI_SERVICE_UUID))
                    
                    // Eğer orada yoksa ScanResponse içindeki ServiceData'ya bak (Yenilenmiş yöntem)
                    if (serviceData == null || serviceData.isEmpty()) {
                        // Bazı cihazlarda scanRecord.serviceData tüm map'i dönmeyebilir, manuel parse gerekebilir veya alternatif kontrol:
                        serviceData = scanRecord?.serviceData?.get(ParcelUuid(YANKI_SERVICE_UUID))
                    }

                    if (serviceData != null && serviceData.isNotEmpty()) {
                        val neighborId = String(serviceData).trim()

                        if (neighborId.isNotEmpty()) {
                            recentlySeenNeighbors[address] = currentTime
                            Log.d("YANKI_MESH", "Yeni Komşu Bulundu: $neighborId ($address), RSSI: ${result.rssi}")
                            onNeighborFound(neighborId, address, result.rssi)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("YANKI_MESH", "Tarama sonucu işlenirken hata oluştu: ${e.message}")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("YANKI_MESH", "Tarama Hatası: $errorCode")
                isScanning = false
            }
        }

        scanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
    }

    // 3. GATT SERVER: Posta Kutusunu Aç (Veri Almak İçin)
    @SuppressLint("MissingPermission")
    fun startGattServer() {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, GATT Server başlatılamadı.")
            return
        }

        if (isGattServerRunning) {
            Log.d("YANKI_BLE", "GATT Server zaten aktif.")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(YANKI_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            YANKI_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristic)

        gattServer?.addService(service)
        isGattServerRunning = true
        Log.d("YANKI_BLE", "GATT Server (Posta Kutusu) açıldı.")
    }

    // 4. GATT CLIENT: Komşuya Veri Fırlat (Parçalama Desteğiyle)
    @SuppressLint("MissingPermission")
    fun sendPayloadToNeighbor(neighborMacAddress: String, payload: ByteArray) {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, veri gönderilemedi.")
            return
        }

        if (neighborMacAddress.isBlank() || !BluetoothAdapter.checkBluetoothAddress(neighborMacAddress)) {
            Log.e("YANKI_BLE", "Geçersiz MAC adresi: $neighborMacAddress")
            return
        }

        // Chunking hazırlığı: 1024 byte ve üzeri için parçalama şart
        val messageId = (messageIdCounter++ % 256)
        val maxChunkSize = 480 
        val chunks = if (payload.size > 500) {
            val list = mutableListOf<ByteArray>()
            val totalChunks = (payload.size + maxChunkSize - 1) / maxChunkSize
            for (i in 0 until totalChunks) {
                val start = i * maxChunkSize
                val end = min(start + maxChunkSize, payload.size)
                val chunkData = payload.copyOfRange(start, end)
                
                // Header: [Magic(0xEE, 0xFF), MsgId, Total, Index] (5 byte)
                val header = byteArrayOf(0xEE.toByte(), 0xFF.toByte(), messageId.toByte(), totalChunks.toByte(), i.toByte())
                list.add(header + chunkData)
            }
            list
        } else {
            listOf(payload)
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(neighborMacAddress)
            if (device == null) {
                Log.e("YANKI_BLE", "Cihaz bulunamadı: $neighborMacAddress")
                return
            }

            device.connectGatt(context, false, object : BluetoothGattCallback() {
                private var nextChunkIndex = 0

                private fun closeGatt(gatt: BluetoothGatt) {
                    gatt.disconnect()
                    gatt.close()
                    Log.d("YANKI_BLE", "GATT Bağlantısı tamamen kapatıldı.")
                }

                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("YANKI_BLE", "Komşuya bağlandık, MTU talep ediliyor...")
                        gatt.requestMtu(512)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("YANKI_BLE", "Bağlantı kesildi. Status: $status")
                        gatt.close()
                    } else if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e("YANKI_BLE", "GATT Hatası oluştu. Status: $status")
                        closeGatt(gatt)
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gatt.discoverServices()
                    } else {
                        Log.e("YANKI_BLE", "MTU değişimi başarısız.")
                        closeGatt(gatt)
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        sendNextChunk(gatt)
                    } else {
                        Log.e("YANKI_BLE", "Servis keşfi başarısız. Status: $status")
                        closeGatt(gatt)
                    }
                }

                private fun sendNextChunk(gatt: BluetoothGatt) {
                    if (nextChunkIndex >= chunks.size) {
                        Log.d("YANKI_BLE", "Tüm parçalar (${chunks.size}) başarıyla gönderildi.")
                        closeGatt(gatt)
                        return
                    }

                    val service = gatt.getService(YANKI_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(YANKI_CHARACTERISTIC_UUID)
                    if (characteristic == null) {
                        Log.e("YANKI_BLE", "Karakteristik bulunamadı.")
                        closeGatt(gatt)
                        return
                    }

                    val currentChunk = chunks[nextChunkIndex]
                    val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(characteristic, currentChunk, writeType)
                    } else {
                        @Suppress("DEPRECATION")
                        characteristic.value = currentChunk
                        @Suppress("DEPRECATION")
                        characteristic.writeType = writeType
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(characteristic)
                    }
                    Log.d("YANKI_BLE", "Parça gönderiliyor: ${nextChunkIndex + 1}/${chunks.size}")
                }

                override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        nextChunkIndex++
                        sendNextChunk(gatt)
                    } else {
                        Log.e("YANKI_BLE", "Yazma hatası (Parça $nextChunkIndex), status: $status")
                        closeGatt(gatt)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("YANKI_BLE", "GATT bağlantısı sırasında kritik hata", e)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("YANKI_MESH", "BLE Yayını Başarıyla Başlatıldı.")
            isAdvertising = true
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e("YANKI_MESH", "Yayın Hatası: $errorCode")
            isAdvertising = false
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic.uuid == YANKI_CHARACTERISTIC_UUID && value != null) {
                // Parçalı paket kontrolü: Magic Bytes = 0xEE, 0xFF
                if (value.size > 5 && value[0] == 0xEE.toByte() && value[1] == 0xFF.toByte()) {
                    val msgId = value[2].toInt() and 0xFF
                    val total = value[3].toInt() and 0xFF
                    val index = value[4].toInt() and 0xFF
                    val data = value.copyOfRange(5, value.size)

                    val key = "${device.address}_$msgId"
                    val reassembler = reassemblers.getOrPut(key) { Reassembler(total) }
                    reassembler.chunks[index] = data

                    Log.d("YANKI_BLE", "Parça alındı: ${index + 1}/$total (Cihaz: ${device.address})")

                    if (reassembler.isComplete()) {
                        Log.d("YANKI_BLE", "Tüm parçalar birleşti, paket işleniyor...")
                        val fullData = reassembler.getFullData()
                        reassemblers.remove(key)
                        onDataReceived?.invoke(fullData)
                    }
                } else {
                    // Normal paket
                    Log.d("YANKI_BLE", "${device.address} cihazından tekil veri geldi.")
                    onDataReceived?.invoke(value)
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (!hasBluetoothPermission()) return
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopMesh() {
        if (hasBluetoothPermission()) {
            try {
                advertiser?.stopAdvertising(advertiseCallback)
            } catch (e: Exception) {
                Log.e("YANKI_BLE", "Yayın durdurulurken hata", e)
            }
            stopScanning()
            gattServer?.close()
        }
        gattServer = null
        isAdvertising = false
        isGattServerRunning = false
        Log.d("YANKI_MESH", "BLE Mesh Durduruldu.")
    }
}
