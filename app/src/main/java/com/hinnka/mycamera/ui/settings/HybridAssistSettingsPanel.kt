package com.hinnka.mycamera.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinnka.mycamera.ui.camera.HybridAssistSettings
import com.hinnka.mycamera.ui.camera.HybridLevelPrecision

/** Allow the Settings UI to follow the phone while keeping the camera itself portrait-locked. */
@Composable
fun HybridSettingsOrientationEffect() {
    val context = LocalContext.current
    val activity = remember(context) { context.findHybridActivity() }

    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            onDispose {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}

/** Extra controls layered onto 1.27.2.2 without touching its RAW/export preference model. */
@Composable
fun HybridAssistSettingsPanel() {
    val context = LocalContext.current
    HybridAssistSettings.initialize(context)

    Column(modifier = Modifier.fillMaxWidth()) {
        HybridAssistValueRow(
            title = "Grid style",
            value = HybridAssistSettings.gridStyle.label,
            actionLabel = "Next",
            onAction = {
                HybridAssistSettings.setGridStyle(HybridAssistSettings.gridStyle.next())
            }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        HybridAssistDualActionRow(
            title = "Grid rotation",
            value = "${HybridAssistSettings.gridRotationDegrees}°",
            leftLabel = "↶ 90°",
            rightLabel = "90° ↷",
            onLeft = { HybridAssistSettings.rotateGridBy(-90) },
            onRight = { HybridAssistSettings.rotateGridBy(90) }
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        HybridAssistDualActionRow(
            title = "Horizontal level precision",
            value = HybridAssistSettings.levelPrecision.label,
            leftLabel = "Standard",
            rightLabel = "Fine",
            onLeft = {
                HybridAssistSettings.setLevelPrecision(HybridLevelPrecision.STANDARD)
            },
            onRight = {
                HybridAssistSettings.setLevelPrecision(HybridLevelPrecision.FINE)
            }
        )

        Text(
            text = if (HybridAssistSettings.levelPrecision == HybridLevelPrecision.FINE) {
                "Fine: green only inside a very tight ~0° lock window. A 1° error is not green."
            } else {
                "Standard: preserves the original 1.27 leveling tolerance."
            },
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vertical / top-down level",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Show the two-axis bubble when the phone faces up or down.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = HybridAssistSettings.verticalLevelEnabled,
                onCheckedChange = HybridAssistSettings::setVerticalLevelEnabled
            )
        }
    }
}

@Composable
private fun HybridAssistValueRow(
    title: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp)
            Text(text = value, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun HybridAssistDualActionRow(
    title: String,
    value: String,
    leftLabel: String,
    rightLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 15.sp)
                Text(text = value, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Row {
                TextButton(onClick = onLeft) { Text(leftLabel) }
                TextButton(onClick = onRight) { Text(rightLabel) }
            }
        }
    }
}

private tailrec fun Context.findHybridActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHybridActivity()
    else -> null
}
