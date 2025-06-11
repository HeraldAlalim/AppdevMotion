package com.example.seglo

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import com.example.seglo.bluetooth.BluetoothManager
import com.example.seglo.data.SettingsDataStore
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BluetoothViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val bluetoothManager = BluetoothManager(application, settingsDataStore)

    val connectionStatus: StateFlow<String> = bluetoothManager.connectionStatus
    val status: StateFlow<String> = bluetoothManager.status
    val devices: StateFlow<List<BluetoothDevice>> = bluetoothManager.devices
    val isConnected: StateFlow<Boolean> = bluetoothManager.isConnected
    val sensorValues = bluetoothManager.sensorValues

    // Classic Bluetooth scanning (add implementation in BluetoothManager if missing)
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startClassicScan() {
        bluetoothManager.startClassicScan()
    }

    // BLE scanning
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startBleScan() {
        bluetoothManager.startBleScan()
    }

    // Load paired devices
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadPairedDevices() {
        bluetoothManager.loadPairedDevices()
    }

    // Connect Classic Bluetooth
    fun connectClassic(device: BluetoothDevice, uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), onConnected: (Boolean) -> Unit) {
        bluetoothManager.connectClassic(device, uuid, onConnected)
    }

    // Connect BLE device (with UART service)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectBle(device: BluetoothDevice) {
        bluetoothManager.connectBle(device)
    }

    // Disconnect (add this function to BluetoothManager)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun disconnect() {
        bluetoothManager.disconnect()
    }

    // Send data over classic Bluetooth
    fun sendClassic(data: ByteArray) {
        bluetoothManager.sendClassic(data)
    }

    // Send data over BLE UART characteristic
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendBle(data: ByteArray) {
        bluetoothManager.sendBle(data)
    }

    // Receive classic data callback
    fun receiveClassic(onData: (ByteArray) -> Unit) {
        bluetoothManager.receiveClassic(onData)
    }

    // Get a BluetoothDevice by MAC address (add this function in BluetoothManager)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getDeviceByAddress(address: String): BluetoothDevice? {
        return bluetoothManager.getDeviceByAddress(address)
    }

    // Restore last connected device, trying classic connection by default
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun restoreLastConnection(lastAddress: String?, onConnected: (Boolean) -> Unit) {
        if (lastAddress != null) {
            val device = getDeviceByAddress(lastAddress)
            if (device != null) {
                connectClassic(device, onConnected = onConnected)
            } else {
                onConnected(false)
            }
        } else {
            onConnected(false)
        }
    }
}