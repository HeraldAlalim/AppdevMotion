package com.example.seglo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seglo.ui.theme.*

@Composable
fun CameraPreviewCard(
    isCameraActive: Boolean,
    isProcessing: Boolean,
    onCameraToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(customColors.deepPurple, customColors.independenceBlue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCameraActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera Active",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Active",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "MediaPipe + TensorFlow Processing",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    // Processing indicator
                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            color = customColors.deepPurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing...",
                            color = customColors.deepPurple,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "Camera Inactive",
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Preview",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap to start camera",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CameraControlButtons(
    isCameraActive: Boolean,
    isProcessing: Boolean,
    onCameraToggle: () -> Unit,
    onDetectSign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCameraToggle,
            modifier = Modifier.weight(1f).height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCameraActive) Color(0xFFE53E3E) else Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isCameraActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isCameraActive) "Stop Camera" else "Start Camera",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCameraActive) "Stop Camera" else "Start Camera",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onDetectSign,
            modifier = Modifier.weight(1f).height(50.dp),
            enabled = isCameraActive && !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = customColors.deepPurple,
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Detect Sign",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detect Sign",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProcessingModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = customColors.softGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Processing Mode",
                    tint = customColors.independenceBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Processing Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = customColors.darkGray,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = customColors.darkGray.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    val modes = listOf(
                        Triple("Camera Only", "(MediaPipe + TensorFlow)", Icons.Default.Videocam),
                        Triple("Glove Only", "(Flex + Gyro Sensors)", Icons.Default.PanTool),
                        Triple("Combined", "(All Sensors + Vision)", Icons.Default.Merge)
                    )

                    modes.forEach { (mode, description, icon) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedMode == mode) customColors.lightPurple.copy(alpha = 0.3f) else Color.Transparent
                            ),
                            border = if (selectedMode == mode)
                                BorderStroke(2.dp, customColors.deepPurple) else null,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMode == mode,
                                    onClick = { onModeSelected(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = customColors.deepPurple,
                                        unselectedColor = customColors.darkGray.copy(alpha = 0.6f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode,
                                    tint = if (selectedMode == mode) customColors.deepPurple else customColors.darkGray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = mode,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedMode == mode) customColors.deepPurple else customColors.darkGray
                                    )
                                    Text(
                                        text = description,
                                        fontSize = 12.sp,
                                        color = customColors.darkGray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionResultsCard(
    detectedSign: String,
    confidence: Float,
    isProcessing: Boolean,
    isCameraActive: Boolean,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = customColors.softGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Detection Results",
                    tint = customColors.independenceBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detection Results",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = customColors.darkGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (detectedSign.isNotEmpty() && !isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (confidence > 0.7f)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color(0xFFFF9800).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Detected Sign:",
                                    fontSize = 12.sp,
                                    color = customColors.darkGray.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = detectedSign,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (confidence > 0.7f) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Confidence:",
                                    fontSize = 12.sp,
                                    color = customColors.darkGray.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(confidence * 100).toInt()}%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (confidence > 0.7f) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = confidence,
                            modifier = Modifier.fillMaxWidth(),
                            color = if (confidence > 0.7f) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isProcessing -> "Processing sign detection..."
                            isCameraActive -> "Ready for sign detection"
                            else -> "Start camera to begin detection"
                        },
                        fontSize = 14.sp,
                        color = customColors.darkGray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontStyle = if (isProcessing) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                }
            }
        }
    }
}