package com.example.seglo.ui.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seglo.data.repository.PredictionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class PredictionState(
    val isLoading: Boolean = false,
    val prediction: String? = null,
    val confidence: Double? = null,
    val error: String? = null
)

class PredictionViewModel : ViewModel() {
    private val repository = PredictionRepository()
    private val _state = MutableStateFlow(PredictionState())
    val state: StateFlow<PredictionState> = _state.asStateFlow()

    fun predictSign(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Convert bitmap to base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                // Make prediction
                repository.predictSign(base64Image).fold(
                    onSuccess = { response ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            prediction = response.prediction,
                            confidence = response.confidence
                        )
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = error.message ?: "Unknown error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun resetState() {
        _state.value = PredictionState()
    }
} 