package com.github.sters.intervalshuffter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.camera.core.ImageCaptureException
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.github.sters.intervalshuffter.CameraType
import com.github.sters.intervalshuffter.MainActivity
import com.github.sters.intervalshuffter.R
import com.github.sters.intervalshuffter.StopConditionType
import com.github.sters.intervalshuffter.camera.CameraManager
import com.github.sters.intervalshuffter.storage.ImageSaver

class CaptureService : Service(), LifecycleOwner {

    private val binder = LocalBinder()
    private lateinit var lifecycleRegistry: LifecycleRegistry

    private var cameraManager: CameraManager? = null
    private var imageSaver: ImageSaver? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val handler = Handler(Looper.getMainLooper())
    private var captureRunnable: Runnable? = null
    private var timerRunnable: Runnable? = null

    private var intervalSeconds: Int = 5
    private var stopConditionType: StopConditionType = StopConditionType.FOREVER
    private var stopCount: Int = 100
    private var stopDurationMinutes: Int = 10
    private var cameraType: CameraType = CameraType.BACK

    private var capturedCount = 0
    private var elapsedSeconds: Long = 0
    private var isPaused = false
    private var isRunning = false

    var onCaptureCountUpdated: ((Int) -> Unit)? = null
    var onElapsedTimeUpdated: ((Long) -> Unit)? = null
    var onCaptureStopped: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): CaptureService = this@CaptureService
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        createNotificationChannel()
        imageSaver = ImageSaver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intervalSeconds = intent.getIntExtra(EXTRA_INTERVAL, 5)
                stopConditionType = StopConditionType.valueOf(
                    intent.getStringExtra(EXTRA_STOP_TYPE) ?: StopConditionType.FOREVER.name
                )
                stopCount = intent.getIntExtra(EXTRA_STOP_COUNT, 100)
                stopDurationMinutes = intent.getIntExtra(EXTRA_STOP_DURATION, 10)
                cameraType = CameraType.valueOf(
                    intent.getStringExtra(EXTRA_CAMERA_TYPE) ?: CameraType.BACK.name
                )

                startForegroundService()
                startCapturing()
            }
            ACTION_PAUSE -> pauseCapturing()
            ACTION_RESUME -> resumeCapturing()
            ACTION_STOP -> stopCapturing()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        stopCapturing()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "一時停止中" else "撮影中: ${capturedCount}枚"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Interval Shuffter")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundService() {
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IntervalShuffter::CaptureLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 hours max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun startCapturing() {
        if (isRunning) return

        isRunning = true
        isPaused = false
        capturedCount = 0
        elapsedSeconds = 0
        imageSaver?.resetCounter()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        cameraManager = CameraManager(this, this)
        cameraManager?.initialize(cameraType) {
            scheduleNextCapture()
            startTimer()
        }
    }

    private fun scheduleNextCapture() {
        captureRunnable?.let { handler.removeCallbacks(it) }

        captureRunnable = Runnable {
            if (!isPaused && isRunning) {
                takePhoto()
            }
            if (isRunning && !shouldStop()) {
                scheduleNextCapture()
            }
        }

        handler.postDelayed(captureRunnable!!, intervalSeconds * 1000L)
    }

    private fun startTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }

        timerRunnable = Runnable {
            if (!isPaused && isRunning) {
                elapsedSeconds++
                onElapsedTimeUpdated?.invoke(elapsedSeconds)
                updateNotification()

                if (shouldStop()) {
                    stopCapturing()
                    return@Runnable
                }
            }
            if (isRunning) {
                handler.postDelayed(timerRunnable!!, 1000L)
            }
        }

        handler.postDelayed(timerRunnable!!, 1000L)
    }

    private fun takePhoto() {
        val saver = imageSaver ?: return
        val camera = cameraManager ?: return

        val outputOptions = saver.createOutputFileOptions()

        camera.takePhoto(
            outputOptions,
            onSuccess = { result ->
                capturedCount++
                onCaptureCountUpdated?.invoke(capturedCount)
                updateNotification()
                Log.d(TAG, "Photo saved: ${result.savedUri}")

                if (shouldStop()) {
                    stopCapturing()
                }
            },
            onError = { exception ->
                Log.e(TAG, "Photo capture failed", exception)
            }
        )
    }

    private fun shouldStop(): Boolean {
        return when (stopConditionType) {
            StopConditionType.COUNT -> capturedCount >= stopCount
            StopConditionType.DURATION -> elapsedSeconds >= stopDurationMinutes * 60
            StopConditionType.FOREVER -> false
        }
    }

    fun pauseCapturing() {
        isPaused = true
        updateNotification()
    }

    fun resumeCapturing() {
        if (!isPaused) return
        isPaused = false
        updateNotification()
    }

    fun stopCapturing() {
        isRunning = false
        isPaused = false

        captureRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable?.let { handler.removeCallbacks(it) }

        cameraManager?.shutdown()
        cameraManager = null

        releaseWakeLock()

        onCaptureStopped?.invoke()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    fun getCapturedCount(): Int = capturedCount
    fun getElapsedSeconds(): Long = elapsedSeconds
    fun isPaused(): Boolean = isPaused
    fun isRunning(): Boolean = isRunning

    companion object {
        private const val TAG = "CaptureService"
        private const val CHANNEL_ID = "capture_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.github.sters.intervalshuffter.START"
        const val ACTION_PAUSE = "com.github.sters.intervalshuffter.PAUSE"
        const val ACTION_RESUME = "com.github.sters.intervalshuffter.RESUME"
        const val ACTION_STOP = "com.github.sters.intervalshuffter.STOP"

        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_STOP_TYPE = "stop_type"
        const val EXTRA_STOP_COUNT = "stop_count"
        const val EXTRA_STOP_DURATION = "stop_duration"
        const val EXTRA_CAMERA_TYPE = "camera_type"
    }
}
