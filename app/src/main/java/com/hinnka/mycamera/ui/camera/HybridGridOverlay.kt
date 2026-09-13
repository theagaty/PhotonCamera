package com.hinnka.mycamera.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform

private const val HYBRID_GOLDEN_SECTION = 0.61803398875f

/**
 * Backport of the newer Photon composition guides with an added user rotation control.
 * The preview itself is never rotated; only guide geometry is transformed.
 */
@Composable
fun HybridGridOverlay(
    aspectRatio: Float,
    style: HybridGridStyle,
    rotationDegrees: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            if (size.width <= 0f || size.height <= 0f ||
                !aspectRatio.isFinite() || aspectRatio <= 0f
            ) {
                return@drawWithCache onDrawBehind { }
            }

            val width = minOf(size.width, size.height * aspectRatio)
            val height = minOf(size.height, size.width / aspectRatio)
            val left = (size.width - width) / 2f
            val top = (size.height - height) / 2f
            val requestedRotation = ((rotationDegrees % 360) + 360) % 360
            // Match the newer Photon's default spiral orientation, then apply user rotation.
            val effectiveRotation = if (style == HybridGridStyle.GOLDEN_SPIRAL) {
                (requestedRotation + 180) % 360
            } else {
                requestedRotation
            }

            val swapped = effectiveRotation == 90 || effectiveRotation == 270
            val path = if (swapped) {
                buildHybridGridPath(style, height, width)
            } else {
                buildHybridGridPath(style, width, height)
            }

            onDrawBehind {
                clipRect(left, top, left + width, top + height) {
                    withTransform({
                        translate(left, top)
                        when (effectiveRotation) {
                            90 -> {
                                translate(left = width)
                                rotate(90f, pivot = Offset.Zero)
                            }
                            180 -> rotate(180f, pivot = Offset(width / 2f, height / 2f))
                            270 -> {
                                translate(top = height)
                                rotate(-90f, pivot = Offset.Zero)
                            }
                        }
                    }) {
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.5f),
                            style = Stroke(3f)
                        )
                    }
                }
            }
        }
    )
}

private fun buildHybridGridPath(
    style: HybridGridStyle,
    width: Float,
    height: Float
): Path = Path().apply {
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }

    when (style) {
        HybridGridStyle.THIRDS,
        HybridGridStyle.GOLDEN_RATIO -> {
            val fraction = if (style == HybridGridStyle.THIRDS) {
                1f / 3f
            } else {
                1f - HYBRID_GOLDEN_SECTION
            }
            for (position in floatArrayOf(fraction, 1f - fraction)) {
                line(width * position, 0f, width * position, height)
                line(0f, height * position, width, height * position)
            }
        }

        HybridGridStyle.DIAGONALS -> {
            val length = minOf(width, height)
            line(0f, 0f, length, length)
            line(width, 0f, width - length, length)
            if (width != height) {
                line(0f, height, length, height - length)
                line(width, height, width - length, height - length)
            }
        }

        HybridGridStyle.GOLDEN_TRIANGLE -> {
            line(0f, 0f, width, height)
            val denominator = width * width + height * height
            val upperProjection = width * width / denominator
            val lowerProjection = height * height / denominator
            line(width, 0f, width * upperProjection, height * upperProjection)
            line(0f, height, width * lowerProjection, height * lowerProjection)
        }

        HybridGridStyle.GOLDEN_SPIRAL -> addHybridGoldenSpiral(width, height)
    }
}

private fun Path.addHybridGoldenSpiral(width: Float, height: Float) {
    var left = 0f
    var top = 0f
    var right = width
    var bottom = height
    val spiral = Path()

    repeat(12) { index ->
        if (minOf(right - left, bottom - top) < 1f) return@repeat
        val tile: Rect
        val center: Offset
        val startAngle: Float
        when (index % 4) {
            0 -> {
                val split = top + (bottom - top) * HYBRID_GOLDEN_SECTION
                tile = Rect(left, top, right, split)
                center = Offset(right, split)
                startAngle = 270f
                moveTo(left, split)
                lineTo(right, split)
                top = split
            }
            1 -> {
                val split = left + (right - left) * HYBRID_GOLDEN_SECTION
                tile = Rect(left, top, split, bottom)
                center = Offset(split, top)
                startAngle = 180f
                moveTo(split, top)
                lineTo(split, bottom)
                left = split
            }
            2 -> {
                val split = bottom - (bottom - top) * HYBRID_GOLDEN_SECTION
                tile = Rect(left, split, right, bottom)
                center = Offset(left, split)
                startAngle = 90f
                moveTo(left, split)
                lineTo(right, split)
                bottom = split
            }
            else -> {
                val split = right - (right - left) * HYBRID_GOLDEN_SECTION
                tile = Rect(split, top, right, bottom)
                center = Offset(split, bottom)
                startAngle = 0f
                moveTo(split, top)
                lineTo(split, bottom)
                right = split
            }
        }
        spiral.arcTo(
            rect = Rect(
                center.x - tile.width,
                center.y - tile.height,
                center.x + tile.width,
                center.y + tile.height
            ),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = -90f,
            forceMoveTo = index == 0
        )
    }
    addPath(spiral)
}
