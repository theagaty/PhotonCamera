from pathlib import Path

path = Path("app/src/main/java/com/hinnka/mycamera/viewmodel/GalleryViewModel.kt")
text = path.read_text()
marker = "    private fun batchExportSelectedPhotos(\n        outputMode: BatchImageOutputMode,\n"
if text.count(marker) != 1:
    raise SystemExit(f"single output marker count={text.count(marker)}")
methods = r'''    private suspend fun exportSingleImage(photo: MediaData, preserveFormat: Boolean): Boolean {
        val context = getApplication<Application>()
        val photoId = if (selectedTab == GalleryTab.SYSTEM) photo.relatedPhoto?.id else photo.id
        if (photoId == null) return false
        val metadata = GalleryManager.loadMetadata(context, photoId)
            ?: photo.relatedPhoto?.metadata ?: photo.metadata ?: MediaMetadata()
        if (preserveFormat) {
            val dngFile = GalleryManager.getDngFile(context, photoId)
            if (dngFile.exists() && dngFile.length() > 0L) {
                return GalleryManager.exportDng(context, photoId, dngFile, metadata)
            }
        }
        val originalFile = GalleryManager.getOriginalImageFile(context, photoId)
        val ext = originalFile?.extension?.lowercase(Locale.US).orEmpty()
        val mime = metadata.mimeType?.lowercase(Locale.US).orEmpty()
        val preserveHeic = preserveFormat &&
            (ext == "heic" || ext == "heif" || mime == "image/heic" || mime == "image/heif")
        return GalleryManager.exportPhoto(
            context,
            photoId,
            null,
            contentRepository.photoProcessor,
            metadata,
            0f,
            0f,
            0f,
            photoQuality.firstOrNull() ?: 95,
            preferHeicExport = preserveHeic,
            preferJpeg444Export = false
        )
    }

    fun exportPhotoPreservingFormat(photo: MediaData, onComplete: (Boolean) -> Unit = {}) {
        if (photo.isVideo) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                val success = exportSingleImage(photo, preserveFormat = true)
                if (success) loadPhotos()
                onComplete(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PLog.e(TAG, "Failed to export photo while preserving format", e)
                onComplete(false)
            }
        }
    }

    fun renderPhotoAsJpeg(photo: MediaData, onComplete: (Boolean) -> Unit = {}) {
        if (photo.isVideo) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                val success = exportSingleImage(photo, preserveFormat = false)
                if (success) loadPhotos()
                onComplete(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PLog.e(TAG, "Failed to render photo as JPEG", e)
                onComplete(false)
            }
        }
    }

'''
path.write_text(text.replace(marker, methods + marker, 1))
print("Applied single-photo output ViewModel helpers")
