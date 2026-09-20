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

## Stage 3 - v1.27.1 capture behavior

- Compare and selectively port the v1.27.1 automatic RAW exposure behavior into Classic CFA only.
- Do not transplant the full v1.27.1 Camera2Controller.
- Preserve manual exposure behavior.

## Stage 4 - Spatial Bayer

- Expose the existing `SPATIAL_BAYER` / `MgcSpatialOutputMode.BAYER` engine path as an advanced RAW option.
- Label it clearly as computational multi-frame Bayer RAW, not Classic single-frame RAW.

## Validation

For RAW outputs, compare CFA layout, samples-per-pixel, black/white levels, default crop/active area, BaselineExposure and Lightroom behavior. Verify DNG export independently from capture processing.

## Unified editor history

Built on the stable Stage 2 RAW foundation.

- Undo and Redo operate on complete editor snapshots rather than individual numeric parameters.
- Continuous adjustments are settled into one history state after interaction stops instead of creating a state for every tiny slider tick.
- A still-pending slider edit is committed immediately when Undo is pressed.
- Reset All restores the editor-entry baseline and is itself undoable.
- A new edit after Undo clears the old Redo branch.
- History is bounded to 100 states per editing session.
- RAW history restoration re-persists the restored RAW development metadata before the preview refreshes.
- LUT, color recipe, frame selection, detail controls, RAW controls, bokeh parameters, crop, straighten, rotation and mirror state participate in history.
- Capture code, Classic CFA routing, HDR+/RAWmax, DNG persistence, Sabre and Spatial processing are untouched.

