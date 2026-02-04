package com.github.sters.intervalshuffter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.sters.intervalshuffter.CameraType
import com.github.sters.intervalshuffter.CaptureSettings
import com.github.sters.intervalshuffter.StopConditionType
import com.github.sters.intervalshuffter.ui.components.CameraPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: CaptureSettings,
    onIntervalChange: (Int) -> Unit,
    onStopConditionTypeChange: (StopConditionType) -> Unit,
    onStopCountChange: (Int) -> Unit,
    onStopDurationChange: (Int) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onCameraTypeChange: (CameraType) -> Unit,
    onStartCapture: () -> Unit,
    hasPermissions: Boolean
) {
    val scrollState = rememberScrollState()
    val showLongDurationWarning = settings.stopConditionType == StopConditionType.FOREVER ||
            (settings.stopConditionType == StopConditionType.DURATION && settings.stopDurationMinutes >= 30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Interval Shuffter",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Camera Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (hasPermissions) {
                CameraPreview(
                    cameraType = settings.cameraType,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "カメラの権限が必要です",
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Selection
        Text(
            text = "カメラ選択",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = settings.cameraType == CameraType.BACK,
                onClick = { onCameraTypeChange(CameraType.BACK) },
                label = { Text("リアカメラ") }
            )
            FilterChip(
                selected = settings.cameraType == CameraType.FRONT,
                onClick = { onCameraTypeChange(CameraType.FRONT) },
                label = { Text("フロントカメラ") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interval Setting
        Text(
            text = "撮影間隔: ${settings.intervalSeconds}秒",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = settings.intervalSeconds.toFloat(),
            onValueChange = { onIntervalChange(it.toInt()) },
            valueRange = 1f..60f,
            steps = 58,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stop Condition
        Text(
            text = "終了条件",
            style = MaterialTheme.typography.titleMedium
        )
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.stopConditionType == StopConditionType.FOREVER,
                    onClick = { onStopConditionTypeChange(StopConditionType.FOREVER) }
                )
                Text("無制限")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.stopConditionType == StopConditionType.COUNT,
                    onClick = { onStopConditionTypeChange(StopConditionType.COUNT) }
                )
                Text("指定枚数: ")
                if (settings.stopConditionType == StopConditionType.COUNT) {
                    OutlinedTextField(
                        value = settings.stopCount.toString(),
                        onValueChange = { it.toIntOrNull()?.let(onStopCountChange) },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                    Text(" 枚")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.stopConditionType == StopConditionType.DURATION,
                    onClick = { onStopConditionTypeChange(StopConditionType.DURATION) }
                )
                Text("指定時間: ")
                if (settings.stopConditionType == StopConditionType.DURATION) {
                    OutlinedTextField(
                        value = settings.stopDurationMinutes.toString(),
                        onValueChange = { it.toIntOrNull()?.let(onStopDurationChange) },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                    Text(" 分")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Screen On/Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "撮影中も画面をONにする",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning for long duration
        if (showLongDurationWarning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ 長時間の撮影を行う場合は、充電ケーブルを接続することをおすすめします。",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Button
        Button(
            onClick = onStartCapture,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = hasPermissions
        ) {
            Text(
                text = "撮影開始",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
