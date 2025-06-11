package com.example.seglo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.seglo.bluetooth.BluetoothManager
import com.example.seglo.data.SettingsDataStore
import com.example.seglo.ui.theme.LocalCustomColors
import com.example.seglo.ui.theme.SeGloTheme
import com.example.seglo.ui.components.SettingsCard
import com.example.seglo.ui.components.SettingsItem
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    bluetoothManager: BluetoothManager,
    settingsDataStore: SettingsDataStore,
    bluetoothConnectionState: Boolean,
    navController: NavController,
    onBluetoothSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current

    // Collect your app settings from DataStore
    val settings by settingsDataStore.settingsFlow.collectAsState(initial = null)

    // Permissions state
    var permissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionsGranted = perms.values.all { it }
        if (permissionsGranted) {
            bluetoothManager.loadPairedDevices()
        }
    }

    // Determine required permissions based on Android version
    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> arrayOf(
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

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        val required = getRequiredPermissions()
        val missing = required.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            permissionsGranted = true
            bluetoothManager.loadPairedDevices()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    // State flows from BluetoothManager: connection and devices
    val isConnected by bluetoothManager.isConnected.collectAsState()
    val pairedDevices by bluetoothManager.devices.collectAsState()

    // Wait for settings before rendering full UI
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Show a message if permissions are missing
    if (!permissionsGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bluetooth permissions are required to view paired devices.")
        }
        return
    }

    // Local mutable states initialized from stored settings
    var vibrationEnabled by remember(settings) { mutableStateOf(settings!!.haptic) }
    var selectedTheme by remember(settings) { mutableStateOf(settings!!.theme) }
    var textSize by remember(settings) { mutableStateOf(settings!!.textSize) }
    var voicePitch by remember(settings) { mutableStateOf(settings!!.voicePitch) }
    var voiceSpeed by remember(settings) { mutableStateOf(settings!!.voiceSpeed) }
    var selectedVoice by remember(settings) { mutableStateOf(settings!!.voice) }

    val availableVoices = listOf("English - US", "English - UK", "Tagalog", "Korean")

    // Persist changes to DataStore
    LaunchedEffect(selectedTheme) {
        if (selectedTheme != settings!!.theme) {
            settingsDataStore.updateTheme(selectedTheme)
        }
    }
    LaunchedEffect(textSize) {
        if (textSize != settings!!.textSize) {
            settingsDataStore.updateTextSize(textSize)
        }
    }
    LaunchedEffect(vibrationEnabled) {
        if (vibrationEnabled != settings!!.haptic) {
            settingsDataStore.updateHaptic(vibrationEnabled)
        }
    }
    LaunchedEffect(voicePitch) {
        if (voicePitch != settings!!.voicePitch) {
            settingsDataStore.updateVoicePitch(voicePitch)
        }
    }
    LaunchedEffect(voiceSpeed) {
        if (voiceSpeed != settings!!.voiceSpeed) {
            settingsDataStore.updateVoiceSpeed(voiceSpeed)
        }
    }
    LaunchedEffect(selectedVoice) {
        if (selectedVoice != settings!!.voice) {
            settingsDataStore.updateVoice(selectedVoice)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customColors.softGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = customColors.darkGray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsCard(title = "Bluetooth & Hardware") {
            SettingsItem(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Settings",
                subtitle = if (isConnected) "Connected" else "Not connected",
                onClick = onBluetoothSettingsClick,
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = customColors.deepPurple
                    )
                }
            )

            Divider(color = customColors.darkGray.copy(alpha = 0.1f))

            Column(modifier = Modifier.padding(start = 44.dp, top = 8.dp)) {
                Text(
                    text = "Paired Devices:",
                    fontSize = 13.sp,
                    color = customColors.darkGray,
                    fontWeight = FontWeight.Medium
                )
                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No paired devices found.",
                        fontSize = 12.sp,
                        color = customColors.darkGray.copy(alpha = 0.6f)
                    )
                } else {
                    pairedDevices.forEach { device ->
                        Text(
                            text = device.name ?: "Unknown Device",
                            fontSize = 12.sp,
                            color = customColors.darkGray
                        )
                    }
                }
            }
        }

        SettingsCard(title = "App Settings") {
            SettingsItem(
                icon = Icons.Default.Vibration,
                title = "Vibration",
                subtitle = "Haptic feedback for interactions",
                trailing = {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }
            )

            Divider(color = customColors.darkGray.copy(alpha = 0.1f))

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Text Size",
                        fontSize = 14.sp,
                        color = customColors.darkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${textSize.toInt()}sp",
                        fontSize = 14.sp,
                        color = customColors.darkGray
                    )
                }
                Slider(
                    value = textSize,
                    onValueChange = { textSize = it },
                    valueRange = 12f..24f,
                    steps = 6
                )
            }

            Divider(color = customColors.darkGray.copy(alpha = 0.1f))

            var themeExpanded by remember { mutableStateOf(false) }
            val themes = listOf("Light", "Dark", "System")
            Column {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = "Current: $selectedTheme",
                    onClick = { themeExpanded = !themeExpanded },
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = customColors.deepPurple
                        )
                    }
                )
                if (themeExpanded) {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = theme
                                    themeExpanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = {
                                    selectedTheme = theme
                                    themeExpanded = false
                                }
                            )
                            Text(
                                text = theme,
                                fontSize = 14.sp,
                                color = customColors.darkGray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        SettingsCard(title = "Voice Output") {
            var voiceExpanded by remember { mutableStateOf(false) }
            SettingsItem(
                icon = Icons.Default.RecordVoiceOver,
                title = "Voice",
                subtitle = selectedVoice,
                onClick = { voiceExpanded = !voiceExpanded },
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = customColors.deepPurple
                    )
                }
            )
            if (voiceExpanded) {
                availableVoices.forEach { voice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVoice = voice; voiceExpanded = false }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVoice == voice,
                            onClick = { selectedVoice = voice; voiceExpanded = false }
                        )
                        Text(
                            text = voice,
                            fontSize = 14.sp,
                            color = customColors.darkGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Pitch",
                        fontSize = 14.sp,
                        color = customColors.darkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format("%.1f", voicePitch),
                        fontSize = 14.sp,
                        color = customColors.darkGray
                    )
                }
                Slider(
                    value = voicePitch,
                    onValueChange = { voicePitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Speed",
                        fontSize = 14.sp,
                        color = customColors.darkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format("%.1f", voiceSpeed),
                        fontSize = 14.sp,
                        color = customColors.darkGray
                    )
                }
                Slider(
                    value = voiceSpeed,
                    onValueChange = { voiceSpeed = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }
        }

        SettingsCard(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = "SeGlo v1.0.0",
                onClick = {}
            )

            Divider(color = customColors.darkGray.copy(alpha = 0.1f))

            SettingsItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                subtitle = "Get help using SeGlo",
                onClick = {
                    showToast(context, "Help & Support coming soon")
                },
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = customColors.deepPurple
                    )
                }
            )

            Divider(color = customColors.darkGray.copy(alpha = 0.1f))

            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "Read our privacy policy",
                onClick = {
                    showToast(context, "Privacy Policy coming soon")
                },
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = customColors.deepPurple
                    )
                }
            )
        }
    }
}

private fun showToast(context: android.content.Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val navController = rememberNavController()
    // Provide a dummy BluetoothManager for preview
    val bluetoothManager = remember { BluetoothManager(context, settingsDataStore) }
    SeGloTheme {
        SettingsScreen(
            bluetoothManager = bluetoothManager,
            settingsDataStore = settingsDataStore,
            navController = navController,
            bluetoothConnectionState = false,
            onBluetoothSettingsClick = { navController.navigate("bluetooth_settings") }
        )
    }
}