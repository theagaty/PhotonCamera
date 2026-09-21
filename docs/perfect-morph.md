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


## Persistent Revert to Original

- Separate from session-only Reset All.
- A capture-time edit/development baseline is frozen after every successful new Photon capture.
- The baseline is stored with the private Photon photo and survives app restarts.
- Revert restores saved LUT/recipe/frame/detail/RAW/bokeh/geometry state from that baseline.
- RAW Revert regenerates Photon's internal preview from the untouched DNG after restoring capture-time RAW metadata.
- AI-denoise/bokeh/detail-HDR derivative files are invalidated as part of Revert.
- Exported system-gallery copies are never deleted, overwritten or modified; exportedUris remain on the live photo record.
- After Revert, the editor session is rebuilt from the restored capture state, so Reset All returns to disabled until new edits are made.
- Legacy photos created before this feature did not have a historical capture baseline; the first state seen by this build is preserved safely as their baseline.

## Spatial Bayer [Advanced]

Implemented after the stable Classic CFA + editor-history/Revert checkpoints.

- Adds a third HDR+/RAWmax merge choice: Spatial Bayer [Advanced].
- Uses the existing MGC SPATIAL_BAYER merge implementation; the merge engine itself is not redesigned.
- Multi-frame alignment/rejection/fusion occurs before persistence, but the persistent DNG remains CFA/Bayer with one sample per pixel.
- Lightroom/ACR or another RAW editor still owns the final demosaic.
- Spatial Bayer DNG output is native 1.00x CFA resolution; RGB-only output scaling is hidden for this mode.
- The RGB FinishRaw conversion/denoise stage is intentionally bypassed for the persistent Bayer DNG.
- Photon still renders an internal JPEG preview from the saved CFA DNG so gallery/editing workflows remain intact.
- Sabre remains RGB/SABRE and Spatial remains RGB/SPATIAL_RGB without routing changes.
- Spatial Bayer supports the same Spatial bracket-exposure planner; Sabre remains bracket-disabled.
