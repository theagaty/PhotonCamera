from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1))
    print(f"Applied: {label}")


def replace_between(path: Path, start_marker: str, end_marker: str, replacement: str, label: str) -> None:
    text = path.read_text()
    start = text.find(start_marker)
    if start < 0:
        raise SystemExit(f"{label}: start marker not found in {path}")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"{label}: end marker not found in {path}")
    path.write_text(text[:start] + replacement + text[end:])
    print(f"Applied: {label}")


view_model = Path("app/src/main/java/com/hinnka/mycamera/viewmodel/GalleryViewModel.kt")
gallery_screen = Path("app/src/main/java/com/hinnka/mycamera/ui/gallery/GalleryScreen.kt")
strings = Path("app/src/main/res/values/strings.xml")

replace_once(
    view_model,
    """enum class GalleryBatchOperation {
    PASTE_SETTINGS,
    EXPORT
}
""",
    """enum class GalleryBatchOperation {
    PASTE_SETTINGS,
    EXPORT,
    RENDER
}

private enum class BatchImageOutputMode {
    PRESERVE_FORMAT,
    RENDER_JPEG
}
""",
    "Gallery batch export modes",
)

batch_export_start = """    /**
     * 批量导出选中的照片
     */
    fun exportSelectedPhotos(onComplete: (Int) -> Unit = {}) {
"""
batch_export_end = """    private data class ShareRequest(val uri: Uri, val mimeType: String)
"""

batch_export_replacement = """    /**
     * Batch export while preserving each photo's source format.
     * RAW/DNG remains DNG. Raster photos remain raster and preserve HEIC vs JPEG.
     */
    fun exportSelectedPhotos(onComplete: (Int) -> Unit = {}) {
        batchExportSelectedPhotos(BatchImageOutputMode.PRESERVE_FORMAT, onComplete)
    }

    /**
     * Batch render every selected image through Photon's processing pipeline as JPEG.
     */
    fun renderSelectedPhotos(onComplete: (Int) -> Unit = {}) {
        batchExportSelectedPhotos(BatchImageOutputMode.RENDER_JPEG, onComplete)
    }

    private fun batchExportSelectedPhotos(
        outputMode: BatchImageOutputMode,
        onComplete: (Int) -> Unit = {}
    ) {
        if (_isExporting.value || _isPastingSettings.value) return
        val selectedSnapshot = selectedPhotos.toList()
        if (selectedSnapshot.isEmpty()) return
        val toExport = selectedSnapshot.filter { it.isImage }
        if (toExport.isEmpty()) {
            onComplete(0)
            return
        }
        val selectedTabSnapshot = selectedTab
        val operation = if (outputMode == BatchImageOutputMode.PRESERVE_FORMAT) {
            GalleryBatchOperation.EXPORT
        } else {
            GalleryBatchOperation.RENDER
        }

        viewModelScope.launch {
            _isExporting.value = true
            val total = toExport.size
            exportProgress = 0 to total
            _batchOperationProgress.value = GalleryBatchOperationProgress(
                operation = operation,
                completed = 0,
                total = total
            )
            var successCount = 0

            try {
                val context = getApplication<Application>()
                val quality = photoQuality.firstOrNull() ?: 95

                withContext(Dispatchers.IO) {
                    toExport.forEachIndexed { index, photo ->
                        try {
                            val photoId = if (selectedTabSnapshot == GalleryTab.SYSTEM) {
                                photo.relatedPhoto?.id
                            } else {
                                photo.id
                            }
                            if (photoId != null) {
                                val metadata = GalleryManager.loadMetadata(context, photoId)
                                    ?: photo.relatedPhoto?.metadata
                                    ?: photo.metadata
                                    ?: MediaMetadata()
                                val dngFile = GalleryManager.getDngFile(context, photoId)
                                val exported = if (
                                    outputMode == BatchImageOutputMode.PRESERVE_FORMAT &&
                                    dngFile.exists() &&
                                    dngFile.length() > 0L
                                ) {
                                    // Preserve RAW as RAW: no demosaic/render/JPEG conversion.
                                    GalleryManager.exportDng(
                                        context,
                                        photoId,
                                        dngFile,
                                        metadata
                                    )
                                } else {
                                    // Raster export keeps JPEG/HEIC in Preserve mode.
                                    // Render mode always produces JPEG, including from DNG.
                                    val originalFile = GalleryManager.getOriginalImageFile(context, photoId)
                                    val originalExtension = originalFile
                                        ?.extension
                                        ?.lowercase(Locale.US)
                                        .orEmpty()
                                    val originalMimeType = metadata.mimeType
                                        ?.lowercase(Locale.US)
                                        .orEmpty()
                                    val preserveHeic =
                                        outputMode == BatchImageOutputMode.PRESERVE_FORMAT &&
                                            (
                                                originalExtension == "heic" ||
                                                    originalExtension == "heif" ||
                                                    originalMimeType == "image/heic" ||
                                                    originalMimeType == "image/heif"
                                                )

                                    GalleryManager.exportPhoto(
                                        context,
                                        photoId,
                                        null,
                                        contentRepository.photoProcessor,
                                        metadata,
                                        0f,
                                        0f,
                                        0f,
                                        quality,
                                        preferHeicExport = preserveHeic,
                                        preferJpeg444Export = false
                                    )
                                }
                                if (exported) {
                                    successCount += 1
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            PLog.e(
                                TAG,
                                "Failed to ${if (outputMode == BatchImageOutputMode.PRESERVE_FORMAT) "export" else "render"} selected photo ${photo.id}",
                                e
                            )
                        } finally {
                            withContext(Dispatchers.Main) {
                                val completed = index + 1
                                exportProgress = completed to total
                                _batchOperationProgress.value =
                                    GalleryBatchOperationProgress(
                                        operation = operation,
                                        completed = completed,
                                        total = total
                                    )
                            }
                        }
                    }
                }

                exitSelectionMode()
                loadPhotos()
                onComplete(successCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PLog.e(
                    TAG,
                    if (outputMode == BatchImageOutputMode.PRESERVE_FORMAT) {
                        "Failed to batch export photos"
                    } else {
                        "Failed to batch render photos"
                    },
                    e
                )
                onComplete(0)
            } finally {
                _isExporting.value = false
                exportProgress = 0 to 0
                _batchOperationProgress.value = null
            }
        }
    }

"""

replace_between(
    view_model,
    batch_export_start,
    batch_export_end,
    batch_export_replacement,
    "Format-preserving Export and JPEG Render batch paths",
)

replace_once(
    gallery_screen,
    """                            val progressLabel = when (progress.operation) {
                                GalleryBatchOperation.PASTE_SETTINGS ->
                                    R.string.pasting_settings_progress
                                GalleryBatchOperation.EXPORT ->
                                    R.string.exporting_progress
                            }
""",
    """                            val progressLabel = when (progress.operation) {
                                GalleryBatchOperation.PASTE_SETTINGS ->
                                    R.string.pasting_settings_progress
                                GalleryBatchOperation.EXPORT ->
                                    R.string.exporting_progress
                                GalleryBatchOperation.RENDER ->
                                    R.string.rendering_progress
                            }
""",
    "Batch Render progress label",
)

replace_once(
    gallery_screen,
    """                            // 批量导出
                            if (viewModel.selectedTab == GalleryTab.PHOTON) {
                                val canExport = selectedImageCount > 0 &&
                                    !isBatchOperationRunning
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable(enabled = canExport) {
                                        viewModel.exportSelectedPhotos { count ->
                                            if (count > 0) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.export_complete,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Output,
                                        contentDescription =
                                            stringResource(R.string.export),
                                        tint = if (canExport) {
                                            AccentOrange
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.export),
                                        color = if (canExport) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
""",
    """                            // Batch Export: preserve each image's source format.
                            if (viewModel.selectedTab == GalleryTab.PHOTON) {
                                val canExport = selectedImageCount > 0 &&
                                    !isBatchOperationRunning
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable(enabled = canExport) {
                                        viewModel.exportSelectedPhotos { count ->
                                            if (count > 0) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.export_complete,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Output,
                                        contentDescription =
                                            stringResource(R.string.export),
                                        tint = if (canExport) {
                                            AccentOrange
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.export),
                                        color = if (canExport) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        fontSize = 12.sp
                                    )
                                }

                                // Batch Render: process every selected image and output JPEG.
                                val canRender = selectedImageCount > 0 &&
                                    !isBatchOperationRunning
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable(enabled = canRender) {
                                        viewModel.renderSelectedPhotos { count ->
                                            if (count > 0) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.render_complete,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.AutoAwesome,
                                        contentDescription =
                                            stringResource(R.string.render),
                                        tint = if (canRender) {
                                            AccentOrange
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.render),
                                        color = if (canRender) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.38f)
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
""",
    "Batch Export and Render controls",
)

replace_once(
    strings,
    """    <string name="export">Export</string>
    <string name="export_complete">Export Complete</string>
""",
    """    <string name="export">Export</string>
    <string name="export_complete">Export Complete</string>
    <string name="render">Render</string>
    <string name="render_complete">Render Complete</string>
""",
    "Render strings",
)

replace_once(
    strings,
    """    <string name="pasting_settings_progress">Pasting settings · %1$d/%2$d</string>
    <string name="exporting_progress">Exporting · %1$d/%2$d</string>
""",
    """    <string name="pasting_settings_progress">Pasting settings · %1$d/%2$d</string>
    <string name="exporting_progress">Exporting · %1$d/%2$d</string>
    <string name="rendering_progress">Rendering · %1$d/%2$d</string>
""",
    "Render progress string",
)

print("All format-preserving batch export refinements applied successfully.")
