package com.hinnka.mycamera.ui.camera

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class HybridGridStyle(val label: String) {
    THIRDS("Rule of Thirds"),
    DIAGONALS("Diagonals"),
    GOLDEN_RATIO("Golden Ratio"),
    GOLDEN_TRIANGLE("Golden Triangle"),
    GOLDEN_SPIRAL("Golden Spiral");

    fun next(): HybridGridStyle {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromPersistedName(name: String?): HybridGridStyle =
            entries.firstOrNull { it.name == name } ?: THIRDS
    }
}

enum class HybridLevelPrecision(val label: String) {
    STANDARD("Standard"),
    FINE("Fine")
}

/**
 * Small, self-contained preference layer for the custom 1.27.2.2 hybrid build.
 * Keeping these refinements outside the main UserPreferencesRepository makes future
 * upstream backports easier and avoids touching the trusted RAW/export architecture.
 */
object HybridAssistSettings {
    private const val PREFS_NAME = "photon_hybrid_assist"
    private const val KEY_GRID_STYLE = "grid_style"
    private const val KEY_GRID_ROTATION = "grid_rotation"
    private const val KEY_LEVEL_PRECISION = "level_precision"
    private const val KEY_VERTICAL_LEVEL = "vertical_level"

    private var appContext: Context? = null

    var gridStyle by mutableStateOf(HybridGridStyle.THIRDS)
        private set

    var gridRotationDegrees by mutableStateOf(0)
        private set

    var levelPrecision by mutableStateOf(HybridLevelPrecision.STANDARD)
        private set

    var verticalLevelEnabled by mutableStateOf(true)
        private set

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        gridStyle = HybridGridStyle.fromPersistedName(prefs.getString(KEY_GRID_STYLE, null))
        gridRotationDegrees = normalizeRotation(prefs.getInt(KEY_GRID_ROTATION, 0))
        levelPrecision = runCatching {
            HybridLevelPrecision.valueOf(
                prefs.getString(KEY_LEVEL_PRECISION, HybridLevelPrecision.STANDARD.name)
                    ?: HybridLevelPrecision.STANDARD.name
            )
        }.getOrDefault(HybridLevelPrecision.STANDARD)
        verticalLevelEnabled = prefs.getBoolean(KEY_VERTICAL_LEVEL, true)
    }

    fun setGridStyle(style: HybridGridStyle) {
        gridStyle = style
        edit { putString(KEY_GRID_STYLE, style.name) }
    }

    fun rotateGridBy(deltaDegrees: Int) {
        gridRotationDegrees = normalizeRotation(gridRotationDegrees + deltaDegrees)
        edit { putInt(KEY_GRID_ROTATION, gridRotationDegrees) }
    }

    fun setGridRotation(degrees: Int) {
        gridRotationDegrees = normalizeRotation(degrees)
        edit { putInt(KEY_GRID_ROTATION, gridRotationDegrees) }
    }

    fun setLevelPrecision(precision: HybridLevelPrecision) {
        levelPrecision = precision
        edit { putString(KEY_LEVEL_PRECISION, precision.name) }
    }

    fun setVerticalLevelEnabled(enabled: Boolean) {
        verticalLevelEnabled = enabled
        edit { putBoolean(KEY_VERTICAL_LEVEL, enabled) }
    }

    private fun normalizeRotation(degrees: Int): Int {
        val normalized = ((degrees % 360) + 360) % 360
        return when {
            normalized < 45 -> 0
            normalized < 135 -> 90
            normalized < 225 -> 180
            normalized < 315 -> 270
            else -> 0
        }
    }

    private inline fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        val context = appContext ?: return
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.block()
        editor.apply()
    }
}
