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
