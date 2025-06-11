package com.example.seglo.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seglo.bluetooth.BluetoothManager
import com.example.seglo.ui.components.*
import com.example.seglo.ui.theme.LocalCustomColors
import com.example.seglo.ui.theme.SeGloTheme
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.*

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    bluetoothManager: BluetoothManager,
    sensorData: Map<String, Float> = emptyMap()
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLanguage by remember { mutableStateOf("English") }
    var messageText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var showSensorValues by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    val isConnected by bluetoothManager.isConnected.collectAsState()
    val sensorValues by bluetoothManager.sensorValues.collectAsState()
    val inferredWords by bluetoothManager.inferredWords.collectAsState()

    // Initialize Text-to-Speech
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    // Clean up Text-to-Speech
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Start sensor reading if connected and toggle is on
    LaunchedEffect(isConnected, showSensorValues) {
        if (isConnected && showSensorValues) {
            bluetoothManager.startReadingSensorValues()
        }
    }

    // Update messageText when inferredWords changes and is not blank
    LaunchedEffect(inferredWords) {
        if (inferredWords.isNotBlank()) {
            messageText = inferredWords
            translatedText = translateMessageMLKit(inferredWords, selectedLanguage)
            // Speak after translation completes
            speakText(tts, translatedText, selectedLanguage) { isSpeaking = it }
        }
    }

    // Update translatedText and speak after translation completes
    LaunchedEffect(messageText, selectedLanguage) {
        if (messageText.isNotEmpty()) {
            val translation = translateMessageMLKit(messageText, selectedLanguage)
            translatedText = translation
            // Speak after translation completes
            speakText(tts, translation, selectedLanguage) { isSpeaking = it }
        } else {
            translatedText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customColors.softGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WelcomeCard()

        BluetoothStatusCard(
            isConnected = isConnected,
            onConnectClick = {
                if (isConnected) {
                    val permission = android.Manifest.permission.BLUETOOTH_CONNECT
                    if (context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        coroutineScope.launch {
                            bluetoothManager.disconnect()
                            showToast(context, "Disconnecting from HC-05...")
                        }
                    } else {
                        showToast(context, "Bluetooth disconnect permission denied")
                    }
                } else {
                    showToast(context, "Connect from HomeScreen is not implemented")
                }
            }
        )

        LanguageSelector(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { selectedLanguage = it }
        )

        SensorToggle(
            showSensorValues = showSensorValues,
            onToggleChange = {
                showSensorValues = it
                if (it && !isConnected) {
                    showToast(context, "Connect to HC-05 to view real-time sensor data")
                }
            }
        )

        if (showSensorValues) {
            SensorValuesDisplay(
                flexSensorValues = sensorValues.flex,
                gyroSensorValues = sensorValues.gyro
            )
        }

        MessageTextBox(
            message = messageText,
            onMessageChange = {
                messageText = it
            }
        )

        SpeechControls(
            onSpeakClick = {
                if (messageText.isNotEmpty()) {
                    coroutineScope.launch {
                        val spokenText = translateMessageMLKit(messageText, selectedLanguage)
                        speakText(tts, spokenText, selectedLanguage) { isSpeaking = it }
                    }
                } else {
                    showToast(context, "No text to speak")
                }
            },
            onStopSpeakClick = {
                tts?.stop()
                isSpeaking = false
            },
            isSpeaking = isSpeaking
        )

        TranslationBox(
            translatedText = translatedText,
            onCopyClick = {
                if (translatedText.isNotEmpty()) {
                    copyToClipboard(context, translatedText)
                    showToast(context, "Translation copied to clipboard")
                } else {
                    showToast(context, "No translation to copy")
                }
            },
            onShareClick = {
                if (translatedText.isNotEmpty()) {
                    shareText(context, translatedText)
                } else {
                    showToast(context, "No translation to share")
                }
            }
        )
    }
}

private fun speakText(tts: TextToSpeech?, text: String, language: String, onSpeakingChange: (Boolean) -> Unit) {
    tts?.let {
        // Set the language for TTS
        val locale = when (language) {
            "English" -> Locale.ENGLISH
            "Tagalog" -> Locale("fil", "PH")
            "Korean" -> Locale.KOREAN
            "Chinese (Simplified)" -> Locale("zh", "CN")
            "Japanese" -> Locale.JAPAN
            "Spanish" -> Locale("es", "ES")
            "French" -> Locale("fr", "FR")
            "Italian" -> Locale("it", "IT")
            else -> Locale.getDefault()
        }
        val result = it.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Optionally notify user that the language is not supported
            onSpeakingChange(false)
            return
        }
        onSpeakingChange(true)
        it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")

        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onSpeakingChange(true)
            override fun onDone(utteranceId: String?) = onSpeakingChange(false)
            override fun onError(utteranceId: String?) = onSpeakingChange(false)
        })
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Translation", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, text: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Translation"))
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

suspend fun translateMessageMLKit(message: String, targetLanguage: String): String {
    val targetLangCode = when (targetLanguage) {
        "English" -> TranslateLanguage.ENGLISH
        "Tagalog" -> TranslateLanguage.TAGALOG
        "Korean" -> TranslateLanguage.KOREAN
        "Chinese (Simplified)" -> TranslateLanguage.CHINESE
        "Japanese" -> TranslateLanguage.JAPANESE
        "Spanish" -> TranslateLanguage.SPANISH
        "French" -> TranslateLanguage.FRENCH
        "Italian" -> TranslateLanguage.ITALIAN
        else -> TranslateLanguage.ENGLISH
    }
    val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(targetLangCode)
        .build()
    val translator = Translation.getClient(options)
    try {
        // Download model if needed
        suspendCancellableCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) } // fallback: continue even if download fails
        }
        // Translate
        return suspendCancellableCoroutine { cont ->
            translator.translate(message)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(message) } // fallback to original
        }
    } finally {
        translator.close()
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SeGloTheme {
        // Static preview only
    }
}