from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1))
    print(f"Applied: {label}")


camera_screen = Path("app/src/main/java/com/hinnka/mycamera/ui/camera/CameraScreen.kt")
settings_screen = Path("app/src/main/java/com/hinnka/mycamera/ui/settings/SettingsScreen.kt")

replace_once(
    camera_screen,
    """    val context = LocalContext.current
    val scope = rememberCoroutineScope()
""",
    """    val context = LocalContext.current
    HybridAssistSettings.initialize(context)
    val hybridGridStyle = HybridAssistSettings.gridStyle
    val hybridGridRotation = HybridAssistSettings.gridRotationDegrees
    val hybridLevelPrecision = HybridAssistSettings.levelPrecision
    val hybridVerticalLevelEnabled = HybridAssistSettings.verticalLevelEnabled
    val scope = rememberCoroutineScope()
""",
    "CameraScreen hybrid assist state",
)

replace_once(
    camera_screen,
    """                        // 网格线覆盖
                        if (state.showGrid) {
                            GridOverlay(
                                aspectRatio = previewAspectRatio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 水平仪覆盖
                        if (showLevelIndicator) {
                            LevelIndicatorOverlay(
                                aspectRatio = previewAspectRatio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
""",
    """                        // 网格线覆盖：新网格类型 + 独立 90° 旋转控制
                        if (state.showGrid) {
                            HybridGridOverlay(
                                aspectRatio = previewAspectRatio,
                                style = hybridGridStyle,
                                rotationDegrees = hybridGridRotation,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 水平仪覆盖：Standard/Fine + 可独立关闭的垂直气泡水平仪
                        if (showLevelIndicator) {
                            HybridLevelIndicatorOverlay(
                                aspectRatio = previewAspectRatio,
                                precision = hybridLevelPrecision,
                                verticalLevelEnabled = hybridVerticalLevelEnabled,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
""",
    "CameraScreen custom grid and level overlays",
)

replace_once(
    settings_screen,
    """    val context = androidx.compose.ui.platform.LocalContext.current
    val depthModelState by remember(context.applicationContext) {
""",
    """    val context = androidx.compose.ui.platform.LocalContext.current
    HybridSettingsOrientationEffect()
    val depthModelState by remember(context.applicationContext) {
""",
    "Settings screen sensor orientation",
)

replace_once(
    settings_screen,
    """                        SwitchSettingItem(
                            title = stringResource(R.string.settings_level_indicator),
                            description = stringResource(R.string.settings_level_description),
                            checked = showLevelIndicator,
                            onCheckedChange = { viewModel.setShowLevelIndicator(it) }
                        )

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        SwitchSettingItem(
                            title = stringResource(R.string.settings_focus_peaking),
""",
    """                        SwitchSettingItem(
                            title = stringResource(R.string.settings_level_indicator),
                            description = stringResource(R.string.settings_level_description),
                            checked = showLevelIndicator,
                            onCheckedChange = { viewModel.setShowLevelIndicator(it) }
                        )

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        HybridAssistSettingsPanel()

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        SwitchSettingItem(
                            title = stringResource(R.string.settings_focus_peaking),
""",
    "Assist settings refinement panel",
)

print("All Photon 1.27.2.2 refinement injections applied successfully.")
