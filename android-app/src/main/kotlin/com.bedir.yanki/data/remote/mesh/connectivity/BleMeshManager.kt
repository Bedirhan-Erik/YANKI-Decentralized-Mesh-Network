import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import javax.inject.Inject
import java.util.*

class BleMeshManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    // YANKI projesine özel benzersiz bir Servis UUID'si (Cihazlar birbirini bu ID ile tanıyacak)
    private val SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    // 1. ADVERTISING: "Ben buradayım" yayını başlat
    fun startAdvertising(userName: String) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER) // Pil dostu mod
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    // 2. SCANNING: Etraftaki YANKI cihazlarını tara
    fun startScanning(onDeviceFound: (String) -> Unit) {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bluetoothLeScanner?.startScan(listOf(filter), settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val deviceName = result.device.name ?: "Bilinmeyen Cihaz"
                Log.d("YANKI_MESH", "Cihaz bulundu: $deviceName")
                onDeviceFound(deviceName)
            }
        })
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("YANKI_MESH", "BLE Yayını Başarıyla Başlatıldı.")
        }
    }
}