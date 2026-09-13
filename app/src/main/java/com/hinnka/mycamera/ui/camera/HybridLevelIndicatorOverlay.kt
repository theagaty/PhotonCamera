package com.hinnka.mycamera.ui.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hinnka.mycamera.utils.PLog
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private const val HYBRID_LEVEL_TAG = "HybridLevelIndicator"
private const val HYBRID_STANDARD_THRESHOLD_DEGREES = 3.0f
private const val HYBRID_FINE_ENTER_THRESHOLD_DEGREES = 0.20f
private const val HYBRID_FINE_EXIT_THRESHOLD_DEGREES = 0.35f
private const val HYBRID_BUBBLE_THRESHOLD_DEGREES = 1.0f
private const val HYBRID_BUBBLE_ENTER_DEGREES = 30f
private const val HYBRID_BUBBLE_EXIT_DEGREES = 40f

enum class HybridLevelMode { HORIZON, BUBBLE }

private data class HybridLevelReading(
    val mode: HybridLevelMode,
    val angleDegrees: Float,
    val horizonDeviationDegrees: Float,
    val flatTiltDegrees: Float,
    val bubbleX: Float,
    val bubbleY: Float
)

/**
 * Horizontal Standard/Fine level plus the newer two-axis top-down bubble.
 * Fine mode intentionally has a tiny green lock window and hysteresis so sensor noise
 * does not make the aligned state flicker.
 */
@Composable
fun HybridLevelIndicatorOverlay(
    aspectRatio: Float,
    precision: HybridLevelPrecision,
    verticalLevelEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var reading by remember { mutableStateOf<HybridLevelReading?>(null) }
    var fineLocked by remember { mutableStateOf(false) }
    var previousMode by remember { mutableStateOf(HybridLevelMode.HORIZON) }

    val current = reading
    val isLevel = current?.let { value ->
        when (value.mode) {
            HybridLevelMode.BUBBLE -> value.flatTiltDegrees < HYBRID_BUBBLE_THRESHOLD_DEGREES
            HybridLevelMode.HORIZON -> when (precision) {
                HybridLevelPrecision.STANDARD ->
                    value.horizonDeviationDegrees < HYBRID_STANDARD_THRESHOLD_DEGREES
                HybridLevelPrecision.FINE -> fineLocked
            }
        }
    } == true

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 160),
        label = "hybridLevelRotation"
    )
    val animatedBubbleOffset by animateOffsetAsState(
        targetValue = current?.let { Offset(it.bubbleX, it.bubbleY) } ?: Offset.Zero,
        animationSpec = tween(durationMillis = 130),
        label = "hybridBubbleOffset"
    )
    val lineColor by animateColorAsState(
        targetValue = if (isLevel) Color(0xFF00FF00) else Color.White.copy(alpha = 0.82f),
        animationSpec = tween(durationMillis = 120),
        label = "hybridLevelColor"
    )

    DisposableEffect(context, precision, verticalLevelEnabled) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        var invalidLogged = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values
                val next = values?.let {
                    calculateHybridLevelReading(
                        values = it,
                        previousMode = previousMode,
                        verticalLevelEnabled = verticalLevelEnabled
                    )
                }
                if (next == null) {
                    if (!invalidLogged) {
                        invalidLogged = true
                        PLog.w(HYBRID_LEVEL_TAG, "Ignored invalid gravity sensor reading")
                    }
                    reading = null
                    fineLocked = false
                    return
                }

                previousMode = next.mode
                reading = next
                if (next.mode == HybridLevelMode.HORIZON) {
                    targetRotation = next.angleDegrees
                    if (precision == HybridLevelPrecision.FINE) {
                        fineLocked = if (fineLocked) {
                            next.horizonDeviationDegrees <= HYBRID_FINE_EXIT_THRESHOLD_DEGREES
                        } else {
                            next.horizonDeviationDegrees <= HYBRID_FINE_ENTER_THRESHOLD_DEGREES
                        }
                    } else {
                        fineLocked = false
                    }
                } else {
                    fineLocked = false
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        } ?: PLog.w(HYBRID_LEVEL_TAG, "Gravity sensor unavailable")

        onDispose { sensorManager.unregisterListener(listener) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val value = reading ?: return@Canvas
        val targetRatio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
        val drawWidth = minOf(size.width, size.height * targetRatio)
        val drawHeight = minOf(size.height, size.width / targetRatio)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)

        if (value.mode == HybridLevelMode.BUBBLE) {
            val travelRadius = minOf(48.dp.toPx(), minOf(drawWidth, drawHeight) * 0.2f)
            drawCircle(
                color = lineColor,
                radius = 7.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = center + animatedBubbleOffset * travelRadius
            )
            return@Canvas
        }

        val lineLength = drawWidth * 0.4f
        val strokeWidth = if (precision == HybridLevelPrecision.FINE) 4.5f else 6f

        withTransform({
            rotate(degrees = animatedRotation, pivot = center)
        }) {
            drawLine(
                color = lineColor,
                start = Offset(centerX - lineLength / 2f, centerY),
                end = Offset(centerX + lineLength / 2f, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            if (isLevel) {
                drawCircle(
                    color = lineColor,
                    radius = if (precision == HybridLevelPrecision.FINE) 6f else 8f,
                    center = center
                )
            }
        }

        if (!isLevel) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 4f,
                center = center
            )
            val gap = lineLength / 2f + 15f
            val markerLen = 15f
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX - gap - markerLen, centerY),
                end = Offset(centerX - gap, centerY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX + gap, centerY),
                end = Offset(centerX + gap + markerLen, centerY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun calculateHybridLevelReading(
    values: FloatArray,
    previousMode: HybridLevelMode,
    verticalLevelEnabled: Boolean
): HybridLevelReading? {
    if (values.size < 3 || (0..2).any { !values[it].isFinite() }) return null
    val x = values[0].toDouble()
    val y = values[1].toDouble()
    val z = values[2].toDouble()
    val planarGravity = hypot(x, y)
    if (hypot(planarGravity, z) < 0.001) return null

    val flatTiltDegrees = Math.toDegrees(atan2(planarGravity, abs(z))).toFloat()
    val mode = if (!verticalLevelEnabled) {
        HybridLevelMode.HORIZON
    } else {
        when (previousMode) {
            HybridLevelMode.HORIZON ->
                if (flatTiltDegrees <= HYBRID_BUBBLE_ENTER_DEGREES) HybridLevelMode.BUBBLE
                else HybridLevelMode.HORIZON
            HybridLevelMode.BUBBLE ->
                if (flatTiltDegrees >= HYBRID_BUBBLE_EXIT_DEGREES) HybridLevelMode.HORIZON
                else HybridLevelMode.BUBBLE
        }
    }

    val angleDegrees = Math.toDegrees(atan2(x, y)).toFloat()
    val deviation = abs(angleDegrees % 90f)
    val horizonDeviation = minOf(deviation, 90f - deviation)
    val bubbleDistance = (flatTiltDegrees / HYBRID_BUBBLE_EXIT_DEGREES).coerceAtMost(1f)

    return HybridLevelReading(
        mode = mode,
        angleDegrees = angleDegrees,
        horizonDeviationDegrees = horizonDeviation,
        flatTiltDegrees = flatTiltDegrees,
        bubbleX = if (planarGravity > 0.0) (x / planarGravity * bubbleDistance).toFloat() else 0f,
        bubbleY = if (planarGravity > 0.0) (-y / planarGravity * bubbleDistance).toFloat() else 0f
    )
}
