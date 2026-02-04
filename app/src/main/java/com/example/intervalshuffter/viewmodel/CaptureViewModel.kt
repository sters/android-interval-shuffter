package com.example.intervalshuffter.viewmodel

import androidx.lifecycle.ViewModel
import com.example.intervalshuffter.CameraType
import com.example.intervalshuffter.CaptureSettings
import com.example.intervalshuffter.CaptureState
import com.example.intervalshuffter.StopConditionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CaptureViewModel : ViewModel() {

    private val _settings = MutableStateFlow(CaptureSettings())
    val settings: StateFlow<CaptureSettings> = _settings.asStateFlow()

    private val _captureState = MutableStateFlow(CaptureState())
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    fun updateIntervalSeconds(seconds: Int) {
        _settings.update { it.copy(intervalSeconds = seconds.coerceIn(1, 60)) }
    }

    fun updateStopConditionType(type: StopConditionType) {
        _settings.update { it.copy(stopConditionType = type) }
    }

    fun updateStopCount(count: Int) {
        _settings.update { it.copy(stopCount = count.coerceAtLeast(1)) }
    }

    fun updateStopDurationMinutes(minutes: Int) {
        _settings.update { it.copy(stopDurationMinutes = minutes.coerceAtLeast(1)) }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        _settings.update { it.copy(keepScreenOn = keepOn) }
    }

    fun updateCameraType(type: CameraType) {
        _settings.update { it.copy(cameraType = type) }
    }

    fun startCapture() {
        _captureState.update {
            CaptureState(
                isCapturing = true,
                isPaused = false,
                capturedCount = 0,
                elapsedSeconds = 0
            )
        }
    }

    fun pauseCapture() {
        _captureState.update { it.copy(isPaused = true) }
    }

    fun resumeCapture() {
        _captureState.update { it.copy(isPaused = false) }
    }

    fun stopCapture() {
        _captureState.update { CaptureState() }
    }

    fun incrementCapturedCount() {
        _captureState.update { it.copy(capturedCount = it.capturedCount + 1) }
    }

    fun updateElapsedSeconds(seconds: Long) {
        _captureState.update { it.copy(elapsedSeconds = seconds) }
    }

    fun shouldStopCapture(): Boolean {
        val state = _captureState.value
        val settings = _settings.value

        return when (settings.stopConditionType) {
            StopConditionType.COUNT -> state.capturedCount >= settings.stopCount
            StopConditionType.DURATION -> state.elapsedSeconds >= settings.stopDurationMinutes * 60
            StopConditionType.FOREVER -> false
        }
    }
}
