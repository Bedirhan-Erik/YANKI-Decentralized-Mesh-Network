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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleMeshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        val YANKI_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val YANKI_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }

    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private var scanCallback: ScanCallback? = null
    private var gattServer: BluetoothGattServer? = null

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 1. ADVERTISING: "Ben Buradayım" Yayını Yap
    @SuppressLint("MissingPermission")
    fun startAdvertising(myUserId: String) {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, yayın başlatılamadı.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true) // GATT bağlantısı (GattServer) için true olmalı
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(YANKI_SERVICE_UUID))
            .addServiceData(ParcelUuid(YANKI_SERVICE_UUID), myUserId.toByteArray())
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    // 2. SCANNING: Etraftaki Komşuları Tara
    @SuppressLint("MissingPermission")
    fun startScanning(onNeighborFound: (id: String, address: String) -> Unit) {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, tarama başlatılamadı.")
            return
        }

        stopScanning()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(YANKI_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val neighborId = result.scanRecord?.getServiceData(ParcelUuid(YANKI_SERVICE_UUID))
                    ?.let { String(it) }

                if (neighborId != null) {
                    Log.d("YANKI_MESH", "Yeni Komşu Bulundu: $neighborId (${result.device.address})")
                    onNeighborFound(neighborId, result.device.address)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("YANKI_MESH", "Tarama Hatası: $errorCode")
            }
        }

        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    // 3. GATT SERVER: Posta Kutusunu Aç (Veri Almak İçin)
    @SuppressLint("MissingPermission")
    fun startGattServer() {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, GATT Server başlatılamadı.")
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
        Log.d("YANKI_BLE", "GATT Server (Posta Kutusu) açıldı.")
    }

    // 4. GATT CLIENT: Komşuya Veri Fırlat
    @SuppressLint("MissingPermission")
    fun sendPayloadToNeighbor(neighborMacAddress: String, payload: ByteArray) {
        if (!hasBluetoothPermission()) {
            Log.e("YANKI_MESH", "Bluetooth izinleri eksik, veri gönderilemedi.")
            return
        }

        val device = bluetoothAdapter?.getRemoteDevice(neighborMacAddress)

        device?.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("YANKI_BLE", "Komşuya bağlandık, servisler aranıyor...")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(YANKI_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(YANKI_CHARACTERISTIC_UUID)

                    if (characteristic != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            characteristic.value = payload
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(characteristic)
                        }
                        Log.d("YANKI_BLE", "Veri yazma komutu gönderildi.")
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("YANKI_BLE", "Veri başarıyla iletildi. Bağlantı kesiliyor.")
                    gatt.disconnect()
                }
            }
        })
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("YANKI_MESH", "BLE Yayını Başarıyla Başlatıldı.")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e("YANKI_MESH", "Yayın Hatası: $errorCode")
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
                Log.d("YANKI_BLE", "${device.address} cihazından veri geldi!")
                // TODO: Gelen veriyi işle
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
        }
    }

    @SuppressLint("MissingPermission")
    fun stopMesh() {
        if (hasBluetoothPermission()) {
            advertiser?.stopAdvertising(advertiseCallback)
            stopScanning()
            gattServer?.close()
        }
        gattServer = null
        Log.d("YANKI_MESH", "BLE Mesh Durduruldu.")
    }
}
