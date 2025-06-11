package com.example.seglo.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BluetoothDeviceList(
    devices: List<BluetoothDevice>,
    selectedDevice: BluetoothDevice?,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    LazyColumn {
        items(devices.size) { idx ->
            val device = devices[idx]
            val deviceName = try {
                device.name?.takeIf { it.isNotBlank() } ?: "Unknown"
            } catch (e: SecurityException) { "Unknown" }
            ListItem(
                headlineContent = { Text(deviceName) },
                supportingContent = { Text(device.address) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceSelected(device) }
                    .then(
                        if (selectedDevice?.address == device.address)
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        else Modifier
                    )
            )
        }
    }
}