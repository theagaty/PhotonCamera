package com.hinnka.mycamera.gallery

import android.graphics.Rect
import com.google.gson.Gson
import com.hinnka.mycamera.lut.BaselineColorCorrectionTarget
import com.hinnka.mycamera.model.ColorRecipeParams
import com.hinnka.mycamera.raw.HncsFilmCurveMode
import com.hinnka.mycamera.raw.HncsRenderIntent
import com.hinnka.mycamera.raw.RawRenderingEngine
import com.hinnka.mycamera.raw.RawToneMappingParameters
import org.json.JSONObject

/**
 * Persistent capture-time editing baseline for Perfect Morph's "Revert to Original".
 *
 * Only edit/development state is serialized. Capture identity, EXIF, source links and exportedUris
 * remain owned by the live MediaMetadata record and are never removed by Revert.
 */
internal object MorphRevertBaseline {
    const val CUSTOM_PROPERTY_KEY = "perfectMorph.revertBaseline.v1"

    private val gson = Gson()

    fun hasBaseline(metadata: MediaMetadata?): Boolean {
        return !metadata?.customProperties?.get(CUSTOM_PROPERTY_KEY).isNullOrBlank()
    }

    fun encode(metadata: MediaMetadata): String {
        return JSONObject().apply {
            putNullable("lutId", metadata.lutId)
            putNullable("tonemapMode", metadata.tonemapMode)
            putNullable("colorRecipeParams", metadata.colorRecipeParams?.toJson())
            putNullable("baselineTarget", metadata.baselineTarget?.name)
            putNullable("baselineLutId", metadata.baselineLutId)
            putNullable("baselineColorRecipeParams", metadata.baselineColorRecipeParams?.toJson())
            putNullable("sharpening", metadata.sharpening)
            putNullable("noiseReduction", metadata.noiseReduction)
            putNullable("chromaNoiseReduction", metadata.chromaNoiseReduction)

            putNullable("rawExposureCompensation", metadata.rawExposureCompensation)
            putNullable("rawAutoExposure", metadata.rawAutoExposure)
            putNullable("rawHighlightsAdjustment", metadata.rawHighlightsAdjustment)
            putNullable("rawShadowsAdjustment", metadata.rawShadowsAdjustment)
            putNullable("rawBlackPointCorrection", metadata.rawBlackPointCorrection)
            putNullable("rawWhitePointCorrection", metadata.rawWhitePointCorrection)
            putNullable("rawLensShadingCorrectionEnabled", metadata.rawLensShadingCorrectionEnabled)
            putNullable("rawDcpId", metadata.rawDcpId)
            putNullable("rawEmbeddedDngProfileId", metadata.rawEmbeddedDngProfileId)
            putNullable("rawHncsProfileId", metadata.rawHncsProfileId)
            put("rawHncsRenderIntent", metadata.rawHncsRenderIntent.assetValue)
            put("rawHncsFilmCurveMode", metadata.rawHncsFilmCurveMode.persistedValue)
            put("rawRenderingEngine", metadata.rawRenderingEngine.name)
            put("rawToneMappingParameters", JSONObject(gson.toJson(metadata.rawToneMappingParameters)))

            putNullable("frameId", metadata.frameId)
            putNullable("computationalAperture", metadata.computationalAperture)
            put("computationalBokehStyle", metadata.computationalBokehStyle)
            putNullable("focusPointX", metadata.focusPointX)
            putNullable("focusPointY", metadata.focusPointY)
            putNullable("postCropRegion", metadata.postCropRegion?.toJson())
            put("postRotationDegrees", metadata.postRotationDegrees)
            put("postStraightenDegrees", metadata.postStraightenDegrees.toDouble())
            put("postMirrorHorizontal", metadata.postMirrorHorizontal)

            putNullable("droMode", metadata.droMode)
            put("manualHdrEffectEnabled", metadata.manualHdrEffectEnabled)
            put("hdrEffectStrength", metadata.hdrEffectStrength.toDouble())
            put("hasAiDenoisedBase", metadata.hasAiDenoisedBase)
            putNullable("aiDenoiseStrength", metadata.aiDenoiseStrength)
            putNullable("rawBlackLevelMode", metadata.rawBlackLevelMode)
            putNullable("rawCustomBlackLevel", metadata.rawCustomBlackLevel)
            putNullable("rawWhiteLevelMode", metadata.rawWhiteLevelMode)
            putNullable("rawCustomWhiteLevel", metadata.rawCustomWhiteLevel)
            putNullable("rawCfaCorrectionMode", metadata.rawCfaCorrectionMode)
            put("applyEffectsToVideo", metadata.applyEffectsToVideo)
            putNullable("spectralFilmStock", metadata.spectralFilmStock)
            putNullable("spectralFilmPrint", metadata.spectralFilmPrint)
            put("spectralFilmCDensityGain", metadata.spectralFilmCDensityGain.toDouble())
            put("spectralFilmMDensityGain", metadata.spectralFilmMDensityGain.toDouble())
            put("spectralFilmYDensityGain", metadata.spectralFilmYDensityGain.toDouble())

            val captureProperties = JSONObject()
            metadata.customProperties
                .filterKeys { it != CUSTOM_PROPERTY_KEY }
                .forEach { (key, value) -> captureProperties.put(key, value) }
            put("customProperties", captureProperties)
        }.toString()
    }

    fun restore(current: MediaMetadata, encoded: String): MediaMetadata? {
        return runCatching {
            val obj = JSONObject(encoded)
            val restoredProperties = mutableMapOf<String, String>()
            obj.optJSONObject("customProperties")?.let { properties ->
                val keys = properties.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    restoredProperties[key] = properties.optString(key)
                }
            }
            restoredProperties[CUSTOM_PROPERTY_KEY] = encoded

            current.copy(
                lutId = obj.optNullableString("lutId"),
                tonemapMode = obj.optNullableString("tonemapMode"),
                colorRecipeParams = obj.optNullableString("colorRecipeParams")
                    ?.let { ColorRecipeParams.fromJson(it) },
                baselineTarget = obj.optNullableString("baselineTarget")?.let {
                    runCatching { BaselineColorCorrectionTarget.valueOf(it) }.getOrNull()
                },
                baselineLutId = obj.optNullableString("baselineLutId"),
                baselineColorRecipeParams = obj.optNullableString("baselineColorRecipeParams")
                    ?.let { ColorRecipeParams.fromJson(it) },
                sharpening = obj.optNullableFloat("sharpening"),
                noiseReduction = obj.optNullableFloat("noiseReduction"),
                chromaNoiseReduction = obj.optNullableFloat("chromaNoiseReduction"),

                rawExposureCompensation = obj.optNullableFloat("rawExposureCompensation"),
                rawAutoExposure = obj.optNullableBoolean("rawAutoExposure"),
                rawHighlightsAdjustment = obj.optNullableFloat("rawHighlightsAdjustment"),
                rawShadowsAdjustment = obj.optNullableFloat("rawShadowsAdjustment"),
                rawBlackPointCorrection = obj.optNullableFloat("rawBlackPointCorrection"),
                rawWhitePointCorrection = obj.optNullableFloat("rawWhitePointCorrection"),
                rawLensShadingCorrectionEnabled =
                    obj.optNullableBoolean("rawLensShadingCorrectionEnabled"),
                rawDcpId = obj.optNullableString("rawDcpId"),
                rawEmbeddedDngProfileId = obj.optNullableString("rawEmbeddedDngProfileId"),
                rawHncsProfileId = obj.optNullableString("rawHncsProfileId"),
                rawHncsRenderIntent = HncsRenderIntent.fromPersistedValue(
                    obj.optString("rawHncsRenderIntent", current.rawHncsRenderIntent.assetValue),
                    current.rawHncsRenderIntent,
                ),
                rawHncsFilmCurveMode = HncsFilmCurveMode.fromPersistedValue(
                    obj.optString(
                        "rawHncsFilmCurveMode",
                        current.rawHncsFilmCurveMode.persistedValue,
                    ),
                    current.rawHncsFilmCurveMode,
                ),
                rawRenderingEngine = RawRenderingEngine.fromPersistedName(
                    obj.optString("rawRenderingEngine", current.rawRenderingEngine.name),
                    current.rawRenderingEngine,
                ),
                rawToneMappingParameters = obj.optJSONObject("rawToneMappingParameters")?.let {
                    gson.fromJson(it.toString(), RawToneMappingParameters::class.java).normalized()
                } ?: current.rawToneMappingParameters,

                frameId = obj.optNullableString("frameId"),
                customProperties = restoredProperties,
                computationalAperture = obj.optNullableFloat("computationalAperture"),
                computationalBokehStyle = obj.optString("computationalBokehStyle", "DEFAULT"),
                focusPointX = obj.optNullableFloat("focusPointX"),
                focusPointY = obj.optNullableFloat("focusPointY"),
                postCropRegion = obj.optJSONObject("postCropRegion")?.toRect(),
                postRotationDegrees = obj.optInt("postRotationDegrees", 0),
                postStraightenDegrees =
                    obj.optDouble("postStraightenDegrees", 0.0).toFloat(),
                postMirrorHorizontal = obj.optBoolean("postMirrorHorizontal", false),

                droMode = obj.optNullableString("droMode"),
                manualHdrEffectEnabled = obj.optBoolean("manualHdrEffectEnabled", false),
                hdrEffectStrength = obj.optDouble("hdrEffectStrength", 1.0).toFloat(),
                hasAiDenoisedBase = obj.optBoolean("hasAiDenoisedBase", false),
                aiDenoiseStrength = obj.optNullableFloat("aiDenoiseStrength"),
                rawBlackLevelMode = obj.optNullableString("rawBlackLevelMode"),
                rawCustomBlackLevel = obj.optNullableFloat("rawCustomBlackLevel"),
                rawWhiteLevelMode = obj.optNullableString("rawWhiteLevelMode"),
                rawCustomWhiteLevel = obj.optNullableFloat("rawCustomWhiteLevel"),
                rawCfaCorrectionMode = obj.optNullableString("rawCfaCorrectionMode"),
                applyEffectsToVideo = obj.optBoolean("applyEffectsToVideo", false),
                spectralFilmStock = obj.optNullableString("spectralFilmStock"),
                spectralFilmPrint = obj.optNullableString("spectralFilmPrint"),
                spectralFilmCDensityGain =
                    obj.optDouble("spectralFilmCDensityGain", 1.0).toFloat(),
                spectralFilmMDensityGain =
                    obj.optDouble("spectralFilmMDensityGain", 1.0).toFloat(),
                spectralFilmYDensityGain =
                    obj.optDouble("spectralFilmYDensityGain", 1.0).toFloat(),
            )
        }.getOrNull()
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (!has(key) || isNull(key)) null else optString(key)
    }

    private fun JSONObject.optNullableFloat(key: String): Float? {
        return if (!has(key) || isNull(key)) null else optDouble(key).toFloat()
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        return if (!has(key) || isNull(key)) null else optBoolean(key)
    }

    private fun Rect.toJson(): JSONObject = JSONObject().apply {
        put("left", left)
        put("top", top)
        put("right", right)
        put("bottom", bottom)
    }

    private fun JSONObject.toRect(): Rect = Rect(
        optInt("left"),
        optInt("top"),
        optInt("right"),
        optInt("bottom"),
    )
}
