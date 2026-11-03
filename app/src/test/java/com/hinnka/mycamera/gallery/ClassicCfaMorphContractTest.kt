package com.hinnka.mycamera.gallery

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level safety contracts for the published-1.28.5 Morph.
 *
 * A JVM test cannot prove that a real phone/decoder accepts the DNG or that the
 * camera returns Bayer pixels; those cases require device testing. These tests
 * prevent accidentally rewriting Classic's sensor master from rendered pixels
 * during upstream forward-ports.
 */
class ClassicCfaMorphContractTest {
    private val managerSource by lazy {
        File("src/main/java/com/hinnka/mycamera/gallery/GalleryManager.kt").readText()
    }
    private val galleryViewModelSource by lazy {
        File("src/main/java/com/hinnka/mycamera/viewmodel/GalleryViewModel.kt").readText()
    }

    @Test
    fun classicCfaMasterComesFromSingleSensorImageAndNotPreviewBuffer() {
        assertTrue(managerSource.contains("DngCaptureDiagnostics.put(\"capture.frameCount\", 1)"))
        assertTrue(managerSource.contains("RawProcessor.saveToDng(\n                        image = image,"))
        assertTrue(managerSource.contains("sourceRawWidth = image.width"))
        assertTrue(managerSource.contains("sourceRawHeight = image.height"))
        assertTrue(managerSource.contains("capture.physicalRawCrop.master\", false"))
        assertTrue(managerSource.contains("CLASSIC_RENDER_DNG_FILE"))
    }

    @Test
    fun fullSensorMasterIsSavedBeforeAnyPreviewPreparationOrFotufilmRendering() {
        val classic = managerSource.substringAfter("suspend fun saveRawPhoto(")
            .substringBefore("suspend fun savePhoto(")
        val master = classic.indexOf("RawProcessor.saveToDng(")
        val masterMetadata = classic.indexOf("saveMetadata(context, photoId, captureMasterMetadata)")
        val previewCrop = classic.indexOf("RawProcessor.resolveCameraRawPhysicalCrop(")
        val previewCopy = classic.indexOf("RawProcessor.copyRawSensorImageToContiguousBuffer(")
        val proxyWrite = classic.indexOf("if (!persistRenderProxyDng())")
        val fotufilmRender = classic.indexOf("processor.processDngBufferForHdrSources(")

        assertTrue("Master sensor DNG must be created", master >= 0)
        assertTrue("RAW provenance must save before preview", master < masterMetadata)
        assertTrue("Preview cropping must not gate master RAW", masterMetadata < previewCrop)
        assertTrue("Sensor master must precede preview buffer", previewCrop < previewCopy)
        assertTrue("Bayer proxy is prepared before rendering", previewCopy < proxyWrite)
        assertTrue("GPU / Fotufilm may only run after RAW+proxy saves", proxyWrite < fotufilmRender)
    }

    @Test
    fun fullSensorMasterBypassesGpuCaptureProfile() {
        val classic = managerSource.substringAfter("suspend fun saveRawPhoto(")
            .substringBefore("suspend fun savePhoto(")
        val master = classic.substringAfter("RawProcessor.saveToDng(")
            .substringBefore("val masterReady =")
        assertTrue(master.contains("image = image,"))
        assertTrue(master.contains("dngProfilePreparationOptions = null"))
        assertTrue(classic.contains("val captureMasterMetadata = metadata.copy("))
        assertTrue(classic.contains("if (!masterReady) {"))
        assertTrue(classic.contains("MorphRevertBaseline.CROPPED_CFA_FALLBACK"))
    }

    @Test
    fun fullSensorMasterAndCroppedRecoveryAreDistinguishable() {
        assertTrue(managerSource.contains("MorphRevertBaseline.FULL_SENSOR_CFA"))
        assertTrue(managerSource.contains("MorphRevertBaseline.CROPPED_CFA_FALLBACK"))
        assertTrue(managerSource.contains("MorphRevertBaseline.MASTER_STATUS_PROPERTY"))
        assertTrue(managerSource.contains("DngCaptureDiagnostics.put(\"capture.masterStatus\", classicMasterStatus)"))
    }

    @Test
    fun modernFotufilmEditorSettingsRemainInTheHistory() {
        assertTrue(managerSource.contains("fotufilmDocument = FotufilmDocumentStorage.decode"))
        assertTrue(galleryViewModelSource.contains("rawFotufilmDocument = editRawFotufilmDocument.value"))
        assertTrue(galleryViewModelSource.contains("editRawFotufilmDocument.value = snapshot.rawFotufilmDocument"))
    }
}
