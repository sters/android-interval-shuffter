package com.github.sters.intervalshuffter.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.github.sters.intervalshuffter.CameraType

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraType: CameraType,
    onPreviewViewReady: ((PreviewView) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView =
        remember {
            PreviewView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(cameraType) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview =
                Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector =
                when (cameraType) {
                    CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                    CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                )
                onPreviewViewReady?.invoke(previewView)
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )
}
