package com.hinnka.mycamera.gallery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-safe contract tests. JSONObject/Rect serialization itself is Android-runtime code and is
 * covered by on-device Revert validation; local unit tests intentionally avoid Android stubs.
 */
class MorphRevertBaselineTest {
    @Test
    fun baselinePresenceUsesDedicatedPersistentProperty() {
        val withBaseline = MediaMetadata(
            customProperties = mapOf(
                MorphRevertBaseline.CUSTOM_PROPERTY_KEY to "{capture-baseline}"
            )
        )
        assertTrue(MorphRevertBaseline.hasBaseline(withBaseline))
    }

    @Test
    fun missingOrBlankBaselineIsNotAvailable() {
        assertFalse(MorphRevertBaseline.hasBaseline(MediaMetadata()))
        assertFalse(
            MorphRevertBaseline.hasBaseline(
                MediaMetadata(
                    customProperties = mapOf(
                        MorphRevertBaseline.CUSTOM_PROPERTY_KEY to ""
                    )
                )
            )
        )
    }
}
