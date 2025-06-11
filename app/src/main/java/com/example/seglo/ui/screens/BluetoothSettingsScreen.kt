package com.example.seglo.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seglo.bluetooth.BluetoothManager
import com.example.seglo.ui.components.BluetoothDeviceList
import java.util.*
import kotlin.Result.Companion.success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSettingsScreen(
    bluetoothManager: BluetoothManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val status by bluetoothManager.status.collectAsState()
    val connectionStatus by bluetoothManager.connectionStatus.collectAsState()
    val devices by bluetoothManager.devices.collectAsState()
    val sensorValues by bluetoothManager.sensorValues.collectAsState()

    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isPairing by remember { mutableStateOf(false) }
    var isPaired by remember { mutableStateOf(false) }
    var scanInProgress by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hc05UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            true -> arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    fun ensurePermissions(onGranted: () -> Unit) {
        val required = getRequiredPermissions()
        val missing = required.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onGranted()
        } else {
            permissionLauncher.launch(required)
        }
    }

    LaunchedEffect(Unit) {
        val required = getRequiredPermissions()
        val missing = required.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            permissionsGranted = true
        } else {
            permissionLauncher.launch(required)
        }
    }

    LaunchedEffect(status) {
        if (status.contains("Scan finished", ignoreCase = true)) {
            scanInProgress = false
        }
    }

    LaunchedEffect(connectionStatus) {
        when {
            connectionStatus.contains("Connected", true) -> {
                Toast.makeText(context, connectionStatus, Toast.LENGTH_SHORT).show()
            }
            connectionStatus.contains("Disconnected", true) ||
                    connectionStatus.contains("Failed", true) -> {
                errorMessage = connectionStatus
            }
        }
    }

    if (!permissionsGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Bluetooth permissions are required to use this feature.")
        }
        return
    }

    val uniqueDevices = devices.distinctBy { it.address }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Status: $status", style = MaterialTheme.typography.bodyLarge)

            if (scanInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        ensurePermissions {
                            scanInProgress = true
                            errorMessage = null
                            try {
                                bluetoothManager.startClassicScan()
                            } catch (e: SecurityException) {
                                errorMessage = "Permission denied for Classic scan."
                                scanInProgress = false
                            }
                        }
                    },
                    enabled = !scanInProgress
                ) {
                    Text("Scan Classic")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Button(
                        onClick = {
                            ensurePermissions {
                                scanInProgress = true
                                errorMessage = null
                                try {
                                    bluetoothManager.startBleScan()
                                } catch (e: SecurityException) {
                                    errorMessage = "Permission denied for BLE scan."
                                    scanInProgress = false
                                }
                            }
                        },
                        enabled = !scanInProgress
                    ) {
                        Text("Scan BLE")
                    }
                }
            }

            Text("Devices Found:")
            BluetoothDeviceList(
                devices = uniqueDevices,
                selectedDevice = selectedDevice,
                onDeviceSelected = { selectedDevice = it }
            )

            Text("Connection: $connectionStatus")
        }
    }

    // Dialog for Pair/Connect
    selectedDevice?.let { device ->
        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED

        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = { Text("Device: ${device.name ?: "Unknown"}") },
            text = { Text("Address: ${device.address}") },
            confirmButton = {
                Button(
                    onClick = {
                        ensurePermissions {
                            isPairing = true
                            errorMessage = null
                            if (!isBonded) {
                                try {
                                    val method = device.javaClass.getMethod("createBond")
                                    method.invoke(device)
                                    isPaired = true
                                } catch (e: Exception) {
                                    errorMessage = "Pairing failed: ${e.localizedMessage}"
                                } finally {
                                    isPairing = false
                                    selectedDevice = null
                                }
                            } else {
                                if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                                    Toast.makeText(context, "Connecting via BLE...", Toast.LENGTH_SHORT).show()
                                    bluetoothManager.connectBle(device)
                                    isPairing = false
                                    isPaired = true
                                    bluetoothManager.startReadingSensorValues()
                                } else {
                                    Toast.makeText(context, "Connecting via Classic...", Toast.LENGTH_SHORT).show()
                                    bluetoothManager.connectClassic(device, hc05UUID) { success ->
                                        isPairing = false
                                        isPaired = success
                                        if (!success) {
                                            errorMessage = "Connection failed."
                                        }
                                        selectedDevice = null
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isPairing
                ) {
                    Text(if (isBonded) "Connect" else "Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDevice = null }) {
                    Text("Cancel")
                }
            }
        )
    }

}