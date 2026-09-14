from pathlib import Path


def one(path, old, new, label):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: count={count}")
    path.write_text(text.replace(old, new, 1))
    print(f"Applied: {label}")


def between(path, start, end, replacement, label):
    text = path.read_text()
    a = text.find(start)
    b = text.find(end, a + 1) if a >= 0 else -1
    if a < 0 or b < 0:
        raise SystemExit(f"{label}: markers missing")
    path.write_text(text[:a] + replacement + text[b:])
    print(f"Applied: {label}")


detail = Path("app/src/main/java/com/hinnka/mycamera/ui/gallery/GalleryDetailScreen.kt")
strings = Path("app/src/main/res/values/strings.xml")

one(detail, "    var isExportingDng by remember { mutableStateOf(false) }\n", "", "remove old DNG spinner state")
one(
    detail,
    """    val isCurrentRawPhoto = currentPhoto?.let {
        it.isImage && (viewModel.selectedTab == GalleryTab.PHOTON || it.relatedPhoto != null) && viewModel.isRaw(it.id)
    } == true
""",
    "",
    "remove old dedicated-DNG state",
)

start = "            if (currentPhoto.isImage && (viewModel.selectedTab == GalleryTab.PHOTON || currentPhoto.relatedPhoto != null)) {\n"
end = "\n\n            if (currentPhoto.isVideo) {"
replacement = r'''            if (currentPhoto.isImage && (viewModel.selectedTab == GalleryTab.PHOTON || currentPhoto.relatedPhoto != null)) {
                add(
                    GalleryMoreAction(
                        icon = AppIcons.Output,
                        text = context.getString(R.string.export),
                        isLoading = isSaving,
                        enabled = !isSaving && !isCopyingSettings && !isPastingSettings,
                        onClick = {
                            showMoreSheet = false
                            isSaving = true
                            viewModel.exportPhotoPreservingFormat(currentPhoto) { success ->
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    if (success) R.string.export_success else R.string.export_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
                add(
                    GalleryMoreAction(
                        icon = AppIcons.AutoAwesome,
                        text = context.getString(R.string.render),
                        isLoading = isSaving,
                        enabled = !isSaving && !isCopyingSettings && !isPastingSettings,
                        onClick = {
                            showMoreSheet = false
                            showExportDialog = true
                        }
                    )
                )
            }'''
between(detail, start, end, replacement, "opened-photo Export and Render actions")

old_dng = r'''            if (isCurrentRawPhoto) {
                add(
                    GalleryMoreAction(
                        iconText = context.getString(R.string.dng_format),
                        text = context.getString(R.string.dng_format),
                        isLoading = isExportingDng,
                        enabled = !isSaving &&
                            !isExportingDng &&
                            !isCopyingSettings &&
                            !isPastingSettings,
                        onClick = {
                            showMoreSheet = false
                            isExportingDng = true
                            viewModel.exportDng(currentPhoto) { success ->
                                isExportingDng = false
                                Toast.makeText(
                                    context,
                                    if (success) R.string.export_dng_success else R.string.export_dng_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
            }
'''
one(detail, old_dng, "", "remove separate DNG action")

start_dialog = "    // 导出确认对话框\n    if (showExportDialog) {\n"
end_dialog = "\n\n    // 视频导出确认对话框"
new_dialog = r'''    // Render confirmation: process the image and create a JPEG.
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.render)) },
            text = { Text(stringResource(R.string.render_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        currentPhoto?.let {
                            isSaving = true
                            viewModel.renderPhotoAsJpeg(it) { success ->
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    if (success) R.string.render_success else R.string.render_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.render), color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = Color(0xFF2D2D2D),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }'''
between(detail, start_dialog, end_dialog, new_dialog, "repurpose old Export dialog as Render")

one(
    strings,
    "    <string name=\"render\">Render</string>\n    <string name=\"render_complete\">Render Complete</string>\n",
    "    <string name=\"render\">Render</string>\n"
    "    <string name=\"render_complete\">Render Complete</string>\n"
    "    <string name=\"render_confirm\">Render this photo with its current edits applied as JPEG?</string>\n"
    "    <string name=\"render_success\">Photo rendered successfully</string>\n"
    "    <string name=\"render_failed\">Failed to render photo</string>\n",
    "single-photo Render strings",
)

print("Applied opened-photo Export/Render UI")
