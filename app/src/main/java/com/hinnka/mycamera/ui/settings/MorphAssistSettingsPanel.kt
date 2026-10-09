package com.hinnka.mycamera.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinnka.mycamera.R
import com.hinnka.mycamera.ui.camera.MorphAssistSettings
import com.hinnka.mycamera.ui.camera.MorphLevelPrecision

@Composable
fun MorphAssistSettingsPanel() {
    val context = LocalContext.current
    MorphAssistSettings.initialize(context)

    val rotationLabels = linkedMapOf(
        0 to stringResource(R.string.morph_grid_rotation_0),
        90 to stringResource(R.string.morph_grid_rotation_90),
        180 to stringResource(R.string.morph_grid_rotation_180),
        270 to stringResource(R.string.morph_grid_rotation_270),
    )
    DropdownSettingItem(
        title = stringResource(R.string.morph_grid_rotation),
        description = stringResource(R.string.morph_grid_rotation_description),
        value = rotationLabels.getValue(MorphAssistSettings.gridRotationDegrees),
        options = rotationLabels.values.toList(),
        isLoading = false,
        onExpanded = {},
        onOptionSelected = { label ->
            rotationLabels.entries.firstOrNull { it.value == label }?.key?.let(
                MorphAssistSettings::updateGridRotationDegrees
            )
        }
    )

    HorizontalDivider(
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 8.dp)
    )

    val precisionLabels = linkedMapOf(
        MorphLevelPrecision.STANDARD to stringResource(R.string.morph_level_standard),
        MorphLevelPrecision.FINE to stringResource(R.string.morph_level_fine),
    )
    DropdownSettingItem(
        title = stringResource(R.string.morph_level_precision),
        description = stringResource(R.string.morph_level_precision_description),
        value = precisionLabels.getValue(MorphAssistSettings.levelPrecision),
        options = precisionLabels.values.toList(),
        isLoading = false,
        onExpanded = {},
        onOptionSelected = { label ->
            precisionLabels.entries.firstOrNull { it.value == label }?.key?.let(
                MorphAssistSettings::updateLevelPrecision
            )
        }
    )
}
