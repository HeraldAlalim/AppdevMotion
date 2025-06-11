package com.example.seglo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val LocalTextSize = staticCompositionLocalOf { 16f } // Now Float

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SoftGrayDark,
    surface = Color(0xFF23232B),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = DarkGrayDark,
    onSurface = DarkGrayDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = SoftGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkGray,
    onSurface = DarkGray
)

@Composable
fun SeGloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    textSize: Float = 16f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Provide custom colors for both modes
    val customColors = if (darkTheme) {
        CustomColors(
            lightPurple = LightPurpleDark,
            darkPurple = DarkPurpleDark,
            softGray = SoftGrayDark,
            darkGray = DarkGrayDark,
            accentBlue = AccentBlueDark,
            speakButtonColor = SpeakButtonColorDark,
            connectButtonColor = ConnectButtonColorDark,
            lightGrayBackground = LightGrayBackgroundDark,
            independenceBlue = IndependenceBlue,
            lavenderGray = LavenderGray,
            deepPurple = DeepPurpleDark
        )
    } else {
        CustomColors(
            lightPurple = LightPurple,
            darkPurple = DarkPurple,
            softGray = SoftGray,
            darkGray = DarkGray,
            accentBlue = AccentBlue,
            speakButtonColor = SpeakButtonColor,
            connectButtonColor = ConnectButtonColor,
            lightGrayBackground = LightGrayBackground,
            independenceBlue = IndependenceBlue,
            lavenderGray = LavenderGray,
            deepPurple = DeepPurple
        )
    }

    CompositionLocalProvider(
        LocalTextSize provides textSize,
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypographyForTextSize(textSize),
            content = content
        )
    }
}

// Helper to get Typography based on text size (Float)
fun getTypographyForTextSize(size: Float): Typography {
    return when {
        size <= 14f -> Typography.copy(
            headlineLarge = Typography.headlineLarge.copy(fontSize = 24.sp),
            bodyLarge = Typography.bodyLarge.copy(fontSize = 12.sp),
        )
        size >= 20f -> Typography.copy(
            headlineLarge = Typography.headlineLarge.copy(fontSize = 36.sp),
            bodyLarge = Typography.bodyLarge.copy(fontSize = 20.sp),
        )
        else -> Typography
    }
}

// Custom color holder for easy access in components
data class CustomColors(
    val lightPurple: Color,
    val darkPurple: Color,
    val softGray: Color,
    val darkGray: Color,
    val accentBlue: Color,
    val speakButtonColor: Color,
    val connectButtonColor: Color,
    val lightGrayBackground: Color,
    val independenceBlue: Color,
    val lavenderGray: Color,
    val deepPurple: Color
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        lightPurple = LightPurple,
        darkPurple = DarkPurple,
        softGray = SoftGray,
        darkGray = DarkGray,
        accentBlue = AccentBlue,
        speakButtonColor = SpeakButtonColor,
        connectButtonColor = ConnectButtonColor,
        lightGrayBackground = LightGrayBackground,
        independenceBlue = IndependenceBlue,
        lavenderGray = LavenderGray,
        deepPurple = DeepPurple
    )
}