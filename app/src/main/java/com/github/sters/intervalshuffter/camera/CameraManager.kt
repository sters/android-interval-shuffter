package com.github.sters.intervalshuffter.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.github.sters.intervalshuffter.CameraType
import java.util.concurrent.Executor

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var currentCameraType: CameraType = CameraType.BACK

    private val executor: Executor = ContextCompat.getMainExecutor(context)

    fun initialize(
        cameraType: CameraType,
        previewView: PreviewView? = null,
        onInitialized: () -> Unit = {},
    ) {
        currentCameraType = cameraType

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera(previewView, onInitialized)
        }, executor)
    }

    private fun bindCamera(
        previewView: PreviewView?,
        onInitialized: () -> Unit,
    ) {
        val provider = cameraProvider ?: return

        val cameraSelector =
            when (currentCameraType) {
                CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            }

        imageCapture =
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

        try {
            provider.unbindAll()

            if (previewView != null) {
                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture,
                )
            }

            onInitialized()
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Camera binding failed", ex)
        } catch (ex: IllegalArgumentException) {
            Log.e(TAG, "Camera binding failed", ex)
        }
    }

    fun takePhoto(
        outputFileOptions: ImageCapture.OutputFileOptions,
        onSuccess: (ImageCapture.OutputFileResults) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        val capture =
            imageCapture ?: run {
                onError(ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED, "Camera not initialized", null))
                return
            }

        capture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSuccess(outputFileResults)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            },
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}
