# Photon 1.28.5 Perfect Morph Forward Port

Base: official Photon `1.28.5` at `0c6ca12e35782168130ee5d4a83d05287449948d`.

The verified 1.28.2.1 Morph checkpoints remain frozen as the behavioral reference.

The working Photon 1.27.2.2 custom branch is intentionally left untouched.

## Design rules

- Keep the newest v1.28.5 engine and compatibility work as the chassis.
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
- Reuse the current v1.28.5 single-frame RAW save path rather than replacing the latest RAW writer wholesale.
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

## Final regression guardrails

The production Morph branch now validates the finished architecture before every APK build:

- Classic CFA remains independent from HDR+/RAWmax selection.
- Sabre maps only to RGB/SABRE.
- Spatial RGB maps only to RGB/SPATIAL_RGB.
- Spatial Bayer maps only to BAYER/SPATIAL_BAYER and resolves output scale to native 1.00x.
- Bracket exposure remains enabled for both Spatial modes and disabled for Sabre.
- Persistent Revert keeps its dedicated baseline marker under unit test, while CI source invariants forbid the Revert serializer from assigning exportedUris; full restore behavior is additionally validated on-device because JSONObject/Rect are Android runtime classes.
- Post-edit rotation/straighten/crop geometry regression tests run with the Morph build.
- Multi-frame output-scale regression tests run with the Morph build.
- Stage 1 UI/workflow markers, editor history, Revert and Spatial Bayer source invariants are checked before Gradle compilation.

## Final Classic CFA experiment

This stage is intentionally layered on top of the verified final-regression checkpoint.

- The exported/master `original.dng` keeps the complete RAW_SENSOR Bayer payload and does not use the current software physical crop.
- Its capture profile uses the 1.27.1-derived legacy viewfinder/spatial matcher for scalar BaselineExposure.
- Photon HDR scene estimation, HDRNet capture preparation, capture PGTM and portrait-priority weighting are excluded from the master DNG preparation.
- DNG DefaultCrop remains non-destructive framing metadata and does not remove master sensor pixels.
- Photon no longer reopens the experimental master to create its own capture-day gallery JPEG.
- A private `classic_render.dng` is produced from the proven current v1.28 physical-crop path and is used only for Photon rendering, RAW edit refresh, HDR preparation and Revert.
- Original-format/DNG export always uses `original.dng`, never the render proxy.
- If full-sensor master writing fails, the proxy is promoted to `original.dng` so a capture is never lost.
- Spatial Bayer, Spatial RGB and Sabre routes are untouched.


## 1.28.3 forward-port policy

- Preserve upstream Canon rendering engine and Picture Styles.
- Preserve upstream MGC RAISR super-resolution for supported RGB RAWmax paths.
- Preserve upstream RAW digital-zoom resolution changes.
- Preserve upstream Photo-mode Ultra HDR, custom accent colors, capture-setting retention, frame v2/liquid-glass work, and video orientation fixes.
- Reapply Morph as isolated modules instead of replacing overlapping 1.28.3 files wholesale.
- Spatial Bayer remains native CFA at 1.00x; RAISR remains an RGB-path feature.
- Frozen 1.28.2.1 final-regression and final-Classic-CFA branches remain fallback references.


## 1.28.5 forward-port policy

- Preserve upstream Fuji rendering/profile support and GXUP0008 film-simulation assets.
- Preserve upstream Leica M9 rendering and selectable Leica color-matrix behavior.
- Preserve upstream HDR+/ML-AE long/short bracket EV controls and maximum HDR-ratio tuning.
- Preserve upstream Spatial/MGC alignment improvements, Bento region admission, and RAW calibration changes.
- Preserve upstream frame gainmap/editorial-frame work, gallery selection behavior, and AppModalBottomSheet UI.
- Reapply Perfect Morph semantically instead of replacing overlapping 1.28.5 files wholesale.
- Classic CFA remains single-frame Bayer; Spatial Bayer remains computational CFA at native 1.00x.
- The verified 1.28.3 Perfect Morph branch remains frozen as the behavioral fallback reference.


## 1.28.5 release migration notes

- Base is the **published 1.28.5 release commit** `0c6ca12e35782168130ee5d4a83d05287449948d`, rather than unreleased changes added to `main` afterwards.
- Preserve the new Fotufilm rendering engine, assets, presets, in-app RAW preview and DCP preview. Upstream removed the older standalone Fuji simulation; do not reintroduce its deleted engine.
- Capture-time persistent Revert and in-session Undo/Redo/Reset must include `fotufilmDocument` along with other photo development metadata.
- Preserve the official 1.28.5 UI auto-rotation setting by retaining `OrientationObserver.isUiLandscape` and `OrientationObserver.uiRotationDegrees` in the gallery. Keep full-sensor capture orientation separate from UI rotation.
- Preserve independent touch focus/metering, Open Gate manual resolution choice, interactive LUT swipes, and selective backups.
- Preserve the RAW editor's new Fotufilm processing input on the Classic CFA rendering path. Do not overwrite the 1.28.5 `GalleryManager`/RAW processor wholesale.
- Maintain the Morph's single-frame Classic CFA DNG, separate RAWmax toggle, Spatial Bayer native CFA route, and explicit Export/Render actions.
- Do not claim that a CI build proves device-specific DNG color-matrix fidelity. Issue bjzhou/PhotonCamera#85 reports exported magenta DNG on a Realme GT 7 Pro in 1.28.5; verify exported CFA images in an external raw decoder and inspect metadata calibration before considering this port device-validated.


## True RAW acceptance requirements (1.28.5)

A Classic CFA exposure must originate from a **single Camera2 RAW_SENSOR frame**.
The external `original.dng` normally writes the untouched image's full Bayer mosaic
using `RawProcessor.saveToDng(image = image)`, retaining all source pixels.
Black level, white level, CFA phase, as-shot calibration and color matrices are DNG
metadata; preview development (Fotufilm/DCP/LUT/sharpening/denoise/tone mapping)
must never replace the master sensor mosaic.

`classic_render.dng` is the physically cropped *internal* RAW proxy that keeps
the current Photon renderer compatible; it must not be confused with the master.
A failed master write promotes the single-frame cropped CFA proxy as a recovery
file (never rendered RGB), explicitly tagged in photo custom properties as:

- `perfectMorph.capture.classicCfaMasterStatus=FULL_SENSOR_CFA`
- `perfectMorph.capture.classicCfaMasterStatus=CROPPED_CFA_FALLBACK`

This status is immutable across Revert to Original. A cropped fallback remains CFA
RAW but is **not** a full-sensor master; report it as such. The full-size master
write and DNG calibration must be device-validated, not inferred from a green CI
build.

### Acceptance checklist

1. Turn RAW on and RAWmax off. Capture RAW_SENSOR with one frame; export Original
   and verify a CFA/Bayer DNG with dimensions matching the sensor's RAW capture.
2. Confirm the master color calibration in Darktable/Lightroom/another independent
   RAW decoder. Compare 1.28.4 and 1.28.5 under the same light to detect a magenta
   or incorrect-white-balance regression (upstream issue #85).
3. Toggle Fotufilm, DCP/render engine, LUT, sharpening, denoising and exposure
   development. Verify the JPEG/render changes but exporting Original returns the
   same Bayer mosaic. Check Undo, Redo, Reset and Revert, including Fotufilm.
4. Separately test RAWmax Spatial Bayer (native 1.0x CFA), Spatial RGB and SABRE;
   they are computational modes, **not** the single-frame Classic option.
5. Rotate the device with auto-rotation both enabled and disabled. Verify that
   UI orientation options do not change the sensor master or EXIF orientation.
6. If the RAW full-sensor master fails, confirm the fallback is explicitly tagged
   `CROPPED_CFA_FALLBACK`; do not report that capture as full-sensor.
7. Run `photon-1285-perfect-morph.yml` or the manual dispatcher
   `photon-1285-morph-validation.yml` on the fork's main branch.
   The dispatcher checks out the 1.28.5 Morph candidate, not main.
   A green Android compilation **cannot** replace these real-device tests.
