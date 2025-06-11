package com.example.seglo

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.seglo.data.SettingsDataStore
import com.example.seglo.ui.components.BottomNavigationBar
import com.example.seglo.ui.screens.BluetoothSettingsScreen
import com.example.seglo.ui.screens.CameraScreen
import com.example.seglo.ui.screens.HomeScreen
import com.example.seglo.ui.screens.LoginScreen
import com.example.seglo.ui.screens.SettingsScreen
import com.example.seglo.ui.screens.SignLanguageScreen
import com.example.seglo.ui.theme.SeGloTheme

class BluetoothViewModelFactory(
    private val application: android.app.Application,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothViewModel(application, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var settingsDataStore: SettingsDataStore

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsDataStore = SettingsDataStore(applicationContext)
        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val context = LocalContext.current

            // Initialize ViewModel with factory
            val bluetoothViewModel: BluetoothViewModel = viewModel(
                factory = BluetoothViewModelFactory(application, settingsDataStore)
            )

            // Collect settings flow from DataStore
            val settingsFlow = settingsDataStore.settingsFlow.collectAsState(initial = null)
            val settings = settingsFlow.value

            // Register permission launcher outside LaunchedEffect
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    if (granted) {
                        // Permission granted: restore last connection
                        settings?.lastConnectedDevice?.let { lastAddress ->
                            bluetoothViewModel.restoreLastConnection(lastAddress) { /* handle result */ }
                        }
                    } else {
                        // Permission denied: optionally show message or disable Bluetooth features
                        Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )

            LaunchedEffect(settings?.lastConnectedDevice) {
                settings?.lastConnectedDevice?.let { lastAddress ->
                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        bluetoothViewModel.restoreLastConnection(lastAddress) { /* handle result if needed */ }
                    } else {
                        // Request permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                }
            }

            if (settings != null) {
                var isLoggedIn by remember { mutableStateOf(false) }

                SeGloTheme(
                    darkTheme = when (settings.theme) {
                        "Dark" -> true
                        "Light" -> false
                        else -> isSystemInDarkTheme()
                    },
                    textSize = settings.textSize
                ) {
                    if (!isLoggedIn) {
                        LoginScreen(
                            onLoginClick = { username, password ->
                                if (username == "admin" && password == "password") {
                                    isLoggedIn = true
                                }
                                handleLogin(context, username, password)
                            },
                            onSignUpClick = { handleSignUp(context) }
                        )
                    } else {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (currentRoute in listOf("home", "camera", "settings")) {
                                    BottomNavigationBar(
                                        selectedRoute = currentRoute,
                                        onItemSelected = { route ->
                                            if (route != currentRoute) {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "home"
                            ) {
                                composable("home") {
                                    HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        bluetoothManager = bluetoothViewModel.bluetoothManager
                                    )
                                }
                                composable("camera") {
                                    SignLanguageScreen(modifier = Modifier.padding(innerPadding))
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        bluetoothManager = bluetoothViewModel.bluetoothManager,
                                        bluetoothConnectionState = bluetoothViewModel.status.collectAsState().value.contains(
                                            "Connected",
                                            ignoreCase = true
                                        ),
                                        settingsDataStore = settingsDataStore,
                                        navController = navController,
                                        onBluetoothSettingsClick = { navController.navigate("bluetooth_settings") }
                                    )
                                }
                                composable("bluetooth_settings") {
                                    BluetoothSettingsScreen(
                                        bluetoothManager = bluetoothViewModel.bluetoothManager,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleLogin(context: android.content.Context, username: String, password: String) {
        Toast.makeText(
            context,
            "Login attempted with username: $username",
            Toast.LENGTH_SHORT
        ).show()

        if (username == "admin" && password == "password") {
            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignUp(context: android.content.Context) {
        Toast.makeText(context, "Navigate to Sign Up", Toast.LENGTH_SHORT).show()
    }
}