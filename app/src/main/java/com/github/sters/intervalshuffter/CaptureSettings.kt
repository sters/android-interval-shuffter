package com.github.sters.intervalshuffter

enum class StopConditionType {
    COUNT,      // 指定枚数
    DURATION,   // 指定時間
    FOREVER     // 無制限
}

enum class CameraType {
    FRONT,
    BACK
}

data class CaptureSettings(
    val intervalSeconds: Int = 5,
    val stopConditionType: StopConditionType = StopConditionType.FOREVER,
    val stopCount: Int = 100,
    val stopDurationMinutes: Int = 10,
    val keepScreenOn: Boolean = true,
    val cameraType: CameraType = CameraType.BACK
)

data class CaptureState(
    val isCapturing: Boolean = false,
    val isPaused: Boolean = false,
    val capturedCount: Int = 0,
    val elapsedSeconds: Long = 0
)
