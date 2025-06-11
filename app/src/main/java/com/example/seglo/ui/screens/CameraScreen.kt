package com.example.seglo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import com.example.seglo.ui.theme.*
import com.example.seglo.ui.components.*

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    var isCameraActive by remember { mutableStateOf(false) }
    var detectedSign by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Combined") }

    val handleCameraToggle = {
        isCameraActive = !isCameraActive
        if (!isCameraActive) {
            detectedSign = ""
            confidence = 0f
            isProcessing = false
        }
    }

    val handleDetectSign = {
        if (isCameraActive) {
            isProcessing = true
            // Simulate processing delay
            GlobalScope.launch {
                delay(2000)
                detectedSign = listOf(
                    "Hello",
                    "Thank You",
                    "Please",
                    "Good Morning",
                    "How are you?"
                ).random()
                confidence = Random.nextFloat() * (0.95f - 0.7f) + 0.7f
                isProcessing = false
            }
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
        // Header
        Text(
            text = "Sign Language Detection",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.darkGray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Camera Preview Section
        CameraPreviewCard(
            isCameraActive = isCameraActive,
            isProcessing = isProcessing,
            onCameraToggle = handleCameraToggle
        )

        // Camera Controls
        CameraControlButtons(
            isCameraActive = isCameraActive,
            isProcessing = isProcessing,
            onCameraToggle = handleCameraToggle,
            onDetectSign = handleDetectSign
        )

        // Processing Mode Selection
        ProcessingModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it }
        )

        // Detection Results
        DetectionResultsCard(
            detectedSign = detectedSign,
            confidence = confidence,
            isProcessing = isProcessing,
            isCameraActive = isCameraActive
        )
    }
}