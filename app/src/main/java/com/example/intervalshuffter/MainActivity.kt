package com.example.intervalshuffter

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intervalshuffter.service.CaptureService
import com.example.intervalshuffter.ui.screens.CaptureScreen
import com.example.intervalshuffter.ui.screens.SettingsScreen
import com.example.intervalshuffter.ui.theme.IntervalShuffterTheme
import com.example.intervalshuffter.viewmodel.CaptureViewModel

class MainActivity : ComponentActivity() {

    private var captureService: CaptureService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CaptureService.LocalBinder
            captureService = binder.getService()
            serviceBound = true

            captureService?.onCaptureCountUpdated = { count ->
                // Update UI via ViewModel
            }
            captureService?.onElapsedTimeUpdated = { seconds ->
                // Update UI via ViewModel
            }
            captureService?.onCaptureStopped = {
                // Handle capture stopped
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            serviceBound = false
        }
    }

    private val permissionsToRequest = buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private var hasPermissions by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(permissionsToRequest)

        setContent {
            IntervalShuffterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        hasPermissions = hasPermissions,
                        onStartCapture = ::startCaptureService,
                        onPauseCapture = ::pauseCaptureService,
                        onResumeCapture = ::resumeCaptureService,
                        onStopCapture = ::stopCaptureService,
                        onKeepScreenOnChanged = ::updateKeepScreenOn
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to service if it's running
        Intent(this, CaptureService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun startCaptureService(settings: CaptureSettings) {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_START
            putExtra(CaptureService.EXTRA_INTERVAL, settings.intervalSeconds)
            putExtra(CaptureService.EXTRA_STOP_TYPE, settings.stopConditionType.name)
            putExtra(CaptureService.EXTRA_STOP_COUNT, settings.stopCount)
            putExtra(CaptureService.EXTRA_STOP_DURATION, settings.stopDurationMinutes)
            putExtra(CaptureService.EXTRA_CAMERA_TYPE, settings.cameraType.name)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Bind to get callbacks
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun pauseCaptureService() {
        captureService?.pauseCapturing()
    }

    private fun resumeCaptureService() {
        captureService?.resumeCapturing()
    }

    private fun stopCaptureService() {
        captureService?.stopCapturing()
    }

    private fun updateKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun MainScreen(
    hasPermissions: Boolean,
    onStartCapture: (CaptureSettings) -> Unit,
    onPauseCapture: () -> Unit,
    onResumeCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    viewModel: CaptureViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val captureState by viewModel.captureState.collectAsState()

    LaunchedEffect(settings.keepScreenOn, captureState.isCapturing) {
        if (captureState.isCapturing) {
            onKeepScreenOnChanged(settings.keepScreenOn)
        } else {
            onKeepScreenOnChanged(false)
        }
    }

    if (captureState.isCapturing) {
        CaptureScreen(
            settings = settings,
            captureState = captureState,
            onPause = {
                viewModel.pauseCapture()
                onPauseCapture()
            },
            onResume = {
                viewModel.resumeCapture()
                onResumeCapture()
            },
            onStop = {
                viewModel.stopCapture()
                onStopCapture()
            }
        )
    } else {
        SettingsScreen(
            settings = settings,
            onIntervalChange = viewModel::updateIntervalSeconds,
            onStopConditionTypeChange = viewModel::updateStopConditionType,
            onStopCountChange = viewModel::updateStopCount,
            onStopDurationChange = viewModel::updateStopDurationMinutes,
            onKeepScreenOnChange = viewModel::updateKeepScreenOn,
            onCameraTypeChange = viewModel::updateCameraType,
            onStartCapture = {
                viewModel.startCapture()
                onStartCapture(settings)
            },
            hasPermissions = hasPermissions
        )
    }
}
