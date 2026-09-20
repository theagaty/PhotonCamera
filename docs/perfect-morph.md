# Photon 1.28.2.1 Perfect Morph

Base: official Photon `1.28.2.1` at `0bf02ba5b4482690880337e489dde1f86e9efebf`.

The working Photon 1.27.2.2 custom branch is intentionally left untouched.

## Design rules

- Keep the newest v1.28.2.1 engine and compatibility work as the chassis.
- Port older behavior only when there is a concrete workflow or RAW-flexibility reason.
- Do not collapse Classic CFA, Spatial Bayer, and LinearRaw RGB into one ambiguous mode.
- Build and verify each stage before starting the next RAW-stage change.
- Keep current Sabre and Spatial RGB behavior stock unless a later test proves a defect.

## Stage 1 - photographer workflow

- Grid rotation on top of the official grid styles.
- Standard/Fine horizontal level precision while keeping the official vertical bubble behavior.
- Real portrait/landscape workspace rotation in Settings, Gallery, photo detail and Editor.
- Continuous v1.27-style editing precision instead of integer-only recipe stepping.
- Export preserves the stored source format.
- Render applies Photon processing and forces JPEG.
- Batch selection keeps both Export and Render.
- Opened photos keep the same Export and Render semantics.

## Stage 2 - Classic CFA

- Decouple RAW from RAWmax again.
- Restore a selectable single-frame Classic CFA route.
- Reuse the current v1.28.2.1 single-frame RAW save path rather than replacing the latest RAW writer wholesale.
- Keep current Pro RAW behavior available independently.

## Stage 3 - v1.27.1 Classic CFA behavior

Implemented on top of the Stage 2 Classic/HDR+ separation:

- Classic CFA now persists the complete single-frame RAW_SENSOR Bayer payload before rendering.
- No software physical RAW crop is applied to Classic CFA sensor data.
- DNG DefaultCrop is retained as non-destructive framing metadata, matching the 1.27.1 philosophy.
- Classic CFA uses the maintained spatial/viewfinder solver derived from Photon 1.27.1.
- The Classic capture profile deliberately bypasses Photon HDR scene estimation, HDRNet, portrait-priority weighting and capture PGTM.
- The exposure result is a scalar viewfinder-matching BaselineExposure rather than the newer Photon HDR preparation chain.
- Camera2 manual/automatic sensor exposure controls remain current and untouched; the ordinary single-frame request path was already materially similar to 1.27.1.
- The newest renderer is allowed to create Photon's internal JPEG/preview only after the Classic DNG has been persisted.
- HDR+/RAWmax remains on the current v1.28.2.1 multi-frame path and is unaffected.

## Stage 4 - Spatial Bayer

- Expose the existing `SPATIAL_BAYER` / `MgcSpatialOutputMode.BAYER` engine path as an advanced RAW option.
- Label it clearly as computational multi-frame Bayer RAW, not Classic single-frame RAW.

## Validation

For RAW outputs, compare CFA layout, samples-per-pixel, black/white levels, default crop/active area, BaselineExposure and Lightroom behavior. Verify DNG export independently from capture processing.
