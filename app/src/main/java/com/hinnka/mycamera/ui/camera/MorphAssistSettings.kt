package com.hinnka.mycamera.ui.camera

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MorphLevelPrecision {
    STANDARD,
    FINE,
}

/**
 * Small, isolated preference store for the custom "perfect morph" UI refinements.
 *
 * These settings intentionally live outside the core capture preference graph so they
 * cannot change RAW capture semantics.
 */
object MorphAssistSettings {
    private const val PREFS = "photon_perfect_morph_assist"
    private const val KEY_GRID_ROTATION = "grid_rotation_degrees"
    private const val KEY_LEVEL_PRECISION = "level_precision"

    private var initialized = false
    private var context: Context? = null

    var gridRotationDegrees by mutableIntStateOf(0)
        private set

    var levelPrecision by mutableStateOf(MorphLevelPrecision.STANDARD)
        private set

    fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        this.context = appContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        gridRotationDegrees = normalizeGridRotation(prefs.getInt(KEY_GRID_ROTATION, 0))
        levelPrecision = runCatching {
            MorphLevelPrecision.valueOf(
                prefs.getString(KEY_LEVEL_PRECISION, MorphLevelPrecision.STANDARD.name)
                    ?: MorphLevelPrecision.STANDARD.name
            )
        }.getOrDefault(MorphLevelPrecision.STANDARD)
        initialized = true
    }

    fun updateGridRotationDegrees(degrees: Int) {
        val normalized = normalizeGridRotation(degrees)
        gridRotationDegrees = normalized
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putInt(KEY_GRID_ROTATION, normalized)
            ?.apply()
    }

    fun updateLevelPrecision(precision: MorphLevelPrecision) {
        levelPrecision = precision
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_LEVEL_PRECISION, precision.name)
            ?.apply()
    }

    private fun normalizeGridRotation(degrees: Int): Int {
        val normalized = ((degrees % 360) + 360) % 360
        return when {
            normalized < 45 -> 0
            normalized < 135 -> 90
            normalized < 225 -> 180
            normalized < 315 -> 270
            else -> 0
        }
    }
}
