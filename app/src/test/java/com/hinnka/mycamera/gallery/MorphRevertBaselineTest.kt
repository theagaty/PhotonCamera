package com.hinnka.mycamera.gallery

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphRevertBaselineTest {
    @Test
    fun restoreReturnsCaptureTimeEditStateButPreservesLiveExportBookkeeping() {
        val captured = MediaMetadata(
            lutId = "capture-lut",
            sharpening = 0.2f,
            noiseReduction = 0.15f,
            rawExposureCompensation = 0.35f,
            frameId = "capture-frame",
            postCropRegion = Rect(10, 20, 900, 700),
            postRotationDegrees = 90,
            postStraightenDegrees = 2.5f,
            postMirrorHorizontal = true,
            exportedUris = listOf("content://capture-export"),
            iso = 100,
            customProperties = mapOf(
                "captureTag" to "preserve-me",
                "rawCaptureProperty" to "original",
            ),
        )
        val encoded = MorphRevertBaseline.encode(captured)

        val edited = captured.copy(
            lutId = "edited-lut",
            sharpening = 0.9f,
            noiseReduction = 0.8f,
            rawExposureCompensation = 2.0f,
            frameId = "edited-frame",
            postCropRegion = Rect(100, 100, 500, 500),
            postRotationDegrees = 270,
            postStraightenDegrees = -12f,
            postMirrorHorizontal = false,
            // These are live/library fields and must not be rolled back by Revert.
            exportedUris = listOf(
                "content://capture-export",
                "content://later-export",
            ),
            iso = 200,
            customProperties = mapOf(
                MorphRevertBaseline.CUSTOM_PROPERTY_KEY to encoded,
                "captureTag" to "changed-later",
                "editOnlyProperty" to "discard-me",
            ),
        )

        val restored = MorphRevertBaseline.restore(edited, encoded)
        assertNotNull(restored)
        restored!!

        assertEquals("capture-lut", restored.lutId)
        assertEquals(0.2f, restored.sharpening)
        assertEquals(0.15f, restored.noiseReduction)
        assertEquals(0.35f, restored.rawExposureCompensation)
        assertEquals("capture-frame", restored.frameId)
        assertEquals(Rect(10, 20, 900, 700), restored.postCropRegion)
        assertEquals(90, restored.postRotationDegrees)
        assertEquals(2.5f, restored.postStraightenDegrees)
        assertEquals(true, restored.postMirrorHorizontal)

        assertEquals(
            listOf("content://capture-export", "content://later-export"),
            restored.exportedUris,
        )
        assertEquals(200, restored.iso)
        assertEquals("preserve-me", restored.customProperties["captureTag"])
        assertEquals("original", restored.customProperties["rawCaptureProperty"])
        assertEquals(null, restored.customProperties["editOnlyProperty"])
        assertEquals(
            encoded,
            restored.customProperties[MorphRevertBaseline.CUSTOM_PROPERTY_KEY],
        )
    }

    @Test
    fun encodedBaselineIsRecognizedAfterBeingStoredWithPhoto() {
        val metadata = MediaMetadata(lutId = "capture-lut")
        val encoded = MorphRevertBaseline.encode(metadata)
        val withBaseline = metadata.copy(
            customProperties = metadata.customProperties + (
                MorphRevertBaseline.CUSTOM_PROPERTY_KEY to encoded
            )
        )

        assertTrue(MorphRevertBaseline.hasBaseline(withBaseline))
    }
}
