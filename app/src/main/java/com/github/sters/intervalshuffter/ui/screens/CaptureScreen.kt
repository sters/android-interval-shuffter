package com.github.sters.intervalshuffter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sters.intervalshuffter.CaptureSettings
import com.github.sters.intervalshuffter.CaptureState
import com.github.sters.intervalshuffter.StopConditionType

@Composable
fun CaptureScreen(
    settings: CaptureSettings,
    captureState: CaptureState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Background (no camera preview during capture to avoid conflict with CaptureService)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        )

        // Overlay UI
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status info at top
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    // Status
                    Text(
                        text = if (captureState.isPaused) "一時停止中" else "撮影中",
                        color = if (captureState.isPaused) Color.Yellow else Color.Green,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Captured count
                    Text(
                        text = "撮影枚数: ${captureState.capturedCount}枚",
                        color = Color.White,
                        fontSize = 16.sp,
                    )

                    // Progress info based on stop condition
                    when (settings.stopConditionType) {
                        StopConditionType.COUNT -> {
                            Text(
                                text = "目標: ${settings.stopCount}枚",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                            )
                            LinearProgressIndicator(
                                progress = captureState.capturedCount.toFloat() / settings.stopCount,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                            )
                        }
                        StopConditionType.DURATION -> {
                            val elapsedMinutes = captureState.elapsedSeconds / 60
                            val elapsedSecs = captureState.elapsedSeconds % 60
                            Text(
                                text = "経過時間: ${elapsedMinutes}分${elapsedSecs}秒 / ${settings.stopDurationMinutes}分",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                            )
                            LinearProgressIndicator(
                                progress = captureState.elapsedSeconds.toFloat() / (settings.stopDurationMinutes * 60),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                            )
                        }
                        StopConditionType.FOREVER -> {
                            val elapsedMinutes = captureState.elapsedSeconds / 60
                            val elapsedSecs = captureState.elapsedSeconds % 60
                            Text(
                                text = "経過時間: ${elapsedMinutes}分${elapsedSecs}秒",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                            )
                        }
                    }

                    // Interval info
                    Text(
                        text = "撮影間隔: ${settings.intervalSeconds}秒",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                }
            }

            // Control buttons at bottom
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f),
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (captureState.isPaused) {
                        // Resume button
                        Button(
                            onClick = onResume,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("再開")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stop button
                        Button(
                            onClick = onStop,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("終了")
                        }
                    } else {
                        // Pause button
                        Button(
                            onClick = onPause,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("中断")
                        }
                    }
                }
            }
        }
    }
}
