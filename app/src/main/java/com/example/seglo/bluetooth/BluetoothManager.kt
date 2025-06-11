package com.example.seglo.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.example.seglo.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter.LeScanCallback

data class SensorValues(
    val flex: Map<String, Float> = emptyMap(),
    val gyro: Map<String, Float> = emptyMap()
)

class BluetoothManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore? = null
) {
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _sensorValues = MutableStateFlow(SensorValues())
    val sensorValues: StateFlow<SensorValues> = _sensorValues

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _inferredWords = MutableStateFlow("")
    val inferredWords: StateFlow<String> = _inferredWords

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var bleCharacteristic: BluetoothGattCharacteristic? = null
    private var classicSocket: BluetoothSocket? = null

    private val serviceUUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val cccDescriptorUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var classicScanCallback: BluetoothAdapter.LeScanCallback? = null

    private fun isBluetoothSupported() = bluetoothAdapter != null

    private fun isBleSupported() =
        true &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startClassicScan(onDeviceFound: ((BluetoothDevice) -> Unit)? = null) {
        if (!isBluetoothSupported()) {
            _status.value = "Bluetooth not supported"
            return
        }

        val adapter = bluetoothAdapter ?: run {
            _status.value = "Bluetooth adapter not available"
            return
        }

        val foundDevices = mutableListOf<BluetoothDevice>()
        _status.value = "Scanning (Classic)..."

        // Clear old callback if any
        classicScanCallback?.let { adapter.stopLeScan(it) }

        classicScanCallback = LeScanCallback { device, _, _ ->
            if (foundDevices.none { it.address == device.address }) {
                foundDevices.add(device)
                _devices.value = foundDevices
                onDeviceFound?.invoke(device)
            }
        }

        adapter.startLeScan(classicScanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            adapter.stopLeScan(classicScanCallback)
            _status.value = "Classic scan finished"
        }, 10000)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun disconnect() {
        // Disconnect Classic socket
        try {
            classicSocket?.close()
            classicSocket = null
        } catch (e: Exception) {
            // ignore or log error
        }

        // Disconnect BLE GATT
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        bleCharacteristic = null

        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getDeviceByAddress(address: String): BluetoothDevice? {
        return bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadPairedDevices() {
        if (!isBluetoothSupported()) {
            _status.value = "Bluetooth not supported"
            return
        }
        _devices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        _status.value = "Loaded paired devices"
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startBleScan(onDeviceFound: ((BluetoothDevice) -> Unit)? = null) {
        if (!isBleSupported()) {
            _status.value = "BLE not supported"
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val foundDevices = mutableListOf<BluetoothDevice>()
        _status.value = "Scanning (BLE)..."

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (foundDevices.none { it.address == device.address }) {
                    foundDevices.add(device)
                    _devices.value = foundDevices
                    onDeviceFound?.invoke(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _status.value = "BLE Scan failed: $errorCode"
            }
        }

        scanner.startScan(callback)
        Handler(Looper.getMainLooper()).postDelayed({
            scanner.stopScan(callback)
            _status.value = "BLE Scan finished"
        }, 10000)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectBle(device: BluetoothDevice) {
        if (!isBleSupported()) {
            _connectionStatus.value = "BLE not supported"
            return
        }

        val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)

        Toast.makeText(context, "Remote Device: ${remoteDevice?.name ?: remoteDevice?.address}", Toast.LENGTH_SHORT).show()

        _connectionStatus.value = "Connecting (BLE)..."

        bluetoothGatt = remoteDevice?.connectGatt(context, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionStatus.value = "Connected (BLE)"
                    _isConnected.value = true
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionStatus.value = "Disconnected (BLE)"
                    _isConnected.value = false
                    bleCharacteristic = null
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(serviceUUID)
                    val characteristic = service?.getCharacteristic(characteristicUUID)

                    if (characteristic != null) {
                        bleCharacteristic = characteristic
                        _connectionStatus.value = "Enabling notifications..."
                        enableNotifications(gatt, characteristic)
                    } else {
                        _connectionStatus.value = "Characteristic not found"
                    }
                } else {
                    _connectionStatus.value = "Service discovery failed"
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                if (descriptor.uuid == cccDescriptorUUID) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        _connectionStatus.value = "BLE UART Ready"
                    } else {
                        _connectionStatus.value = "Failed to enable notifications"
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val value = characteristic.value ?: return
                val text = value.toString(Charsets.UTF_8)

                if (text.trim().startsWith("F1")) {
                    parseSensorLine(text)?.let {
                        _sensorValues.value = it
                    }
                } else {
                    _inferredWords.value = text
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _status.value = "Data sent (BLE)"
                } else {
                    _status.value = "Data send failed (BLE): $status"
                }
            }
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val success = gatt.setCharacteristicNotification(characteristic, true)
        if (!success) {
            _connectionStatus.value = "Failed to set notifications"
            return
        }
        val descriptor = characteristic.getDescriptor(cccDescriptorUUID)
        if (descriptor == null) {
            _connectionStatus.value = "CCCD Descriptor not found"
            return
        }
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val writeSuccess = gatt.writeDescriptor(descriptor)
        if (!writeSuccess) {
            _connectionStatus.value = "Failed to write descriptor"
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendBle(data: ByteArray) {
        val characteristic = bleCharacteristic ?: run {
            _status.value = "BLE characteristic not ready"
            return
        }
        characteristic.value = data
        val success = bluetoothGatt?.writeCharacteristic(characteristic) == true
        if (success) {
            _status.value = "Sending data (BLE)..."
        } else {
            _status.value = "Failed to send data (BLE)"
        }
    }

    fun sendClassic(data: ByteArray) {
        try {
            classicSocket?.outputStream?.write(data)
            _status.value = "Data sent (Classic)"
        } catch (e: Exception) {
            _status.value = "Send failed: ${e.message}"
        }
    }

    fun receiveClassic(onData: (ByteArray) -> Unit) {
        Thread {
            try {
                val input = classicSocket?.inputStream ?: return@Thread
                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes > 0) {
                        onData(buffer.copyOf(bytes))
                    }
                }
            } catch (e: Exception) {
                _status.value = "Receive failed: ${e.message}"
            }
        }.start()
    }

    fun connectClassic(device: BluetoothDevice, uuid: UUID, onConnected: (Boolean) -> Unit) {
        if (!isBluetoothSupported()) {
            _connectionStatus.value = "Bluetooth not supported"
            onConnected(false)
            return
        }

        _connectionStatus.value = "Connecting (Classic)..."

        Thread {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    _isConnected.value = false
                    _connectionStatus.value = "Permission denied"
                    onConnected(false)
                    return@Thread
                }

                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                classicSocket = socket

                _isConnected.value = true
                _connectionStatus.value = "Connected (Classic)"

                settingsDataStore?.let { ds ->
                    CoroutineScope(Dispatchers.IO).launch {
                        ds.updateLastConnectedDevice(device.address)
                    }
                }

                onConnected(true)
            } catch (e: SecurityException) {
                _isConnected.value = false
                _connectionStatus.value = "Permission denied: ${e.message}"
                onConnected(false)
            } catch (e: Exception) {
                _isConnected.value = false
                _connectionStatus.value = "Connection failed: ${e.message}"
                onConnected(false)
            }
        }.start()
    }

    fun startReadingSensorValues() {
        Thread {
            try {
                val input = classicSocket?.inputStream ?: return@Thread
                val reader = input.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    parseSensorLine(line)?.let {
                        _sensorValues.value = it
                    }
                }
            } catch (e: Exception) {
                _status.value = "Sensor read failed: ${e.message}"
            }
        }.start()
    }

    private fun parseSensorLine(line: String): SensorValues? {
        val flex = mutableMapOf<String, Float>()
        val gyro = mutableMapOf<String, Float>()
        val regex = Regex("""([A-Z][0-9]?):\s*([-+]?[0-9]*\.?[0-9]+)""")

        regex.findAll(line).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].toFloatOrNull() ?: return@forEach
            when (key) {
                "F1", "F2", "F3", "F4", "F5" -> flex[key] = value
                "X", "Y", "Z" -> gyro[key] = value
            }
        }

        return if (flex.isNotEmpty() || gyro.isNotEmpty()) {
            SensorValues(flex, gyro)
        } else null
    }
}