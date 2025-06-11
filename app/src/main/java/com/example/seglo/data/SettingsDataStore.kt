package com.example.seglo.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

object SettingsKeys {
    val THEME = stringPreferencesKey("theme")
    val TEXT_SIZE = floatPreferencesKey("text_size")
    val HAPTIC = booleanPreferencesKey("haptic")
    val VOICE = stringPreferencesKey("voice")
    val VOICE_PITCH = floatPreferencesKey("voice_pitch")
    val VOICE_SPEED = floatPreferencesKey("voice_speed")
    val LAST_CONNECTED_DEVICE = stringPreferencesKey("last_connected_device")
}

class SettingsDataStore(private val context: Context) {
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            theme = prefs[SettingsKeys.THEME] ?: "System",
            textSize = prefs[SettingsKeys.TEXT_SIZE] ?: 16f,
            haptic = prefs[SettingsKeys.HAPTIC] != false,
            voice = prefs[SettingsKeys.VOICE] ?: "English - US",
            voicePitch = prefs[SettingsKeys.VOICE_PITCH] ?: 1.0f,
            voiceSpeed = prefs[SettingsKeys.VOICE_SPEED] ?: 1.0f,
            lastConnectedDevice = prefs[SettingsKeys.LAST_CONNECTED_DEVICE]
        )
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[SettingsKeys.THEME] = theme }
    }
    suspend fun updateTextSize(size: Float) {
        context.dataStore.edit { it[SettingsKeys.TEXT_SIZE] = size }
    }
    suspend fun updateHaptic(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.HAPTIC] = enabled }
    }
    suspend fun updateVoice(voice: String) {
        context.dataStore.edit { it[SettingsKeys.VOICE] = voice }
    }
    suspend fun updateVoicePitch(pitch: Float) {
        context.dataStore.edit { it[SettingsKeys.VOICE_PITCH] = pitch }
    }
    suspend fun updateVoiceSpeed(speed: Float) {
        context.dataStore.edit { it[SettingsKeys.VOICE_SPEED] = speed }
    }
    suspend fun updateLastConnectedDevice(address: String?) {
        context.dataStore.edit { prefs ->
            if (address != null) prefs[SettingsKeys.LAST_CONNECTED_DEVICE] = address
            else prefs.remove(SettingsKeys.LAST_CONNECTED_DEVICE)
        }
    }
}

data class Settings(
    val theme: String,
    val textSize: Float,
    val haptic: Boolean,
    val voice: String,
    val voicePitch: Float,
    val voiceSpeed: Float,
    val lastConnectedDevice: String?
)