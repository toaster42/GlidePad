# GlidePad — Project Invariants

These invariants apply only to `projects/GlidePad` (Android / Jetpack Compose / ChromeOS adaptive windowing).

## 1. Edge-to-Edge Window Size & Float Mode Measurement (`P-04`)

- **Measure un-padded window bounds**: Keep `WindowLayoutPolicy.resolve(...)` / `BoxWithConstraints` measurement on the un-padded root container before applying `WindowInsets.safeDrawing`; apply `.windowInsetsPadding(WindowInsets.safeDrawing)` only on an inner child `Box` wrapping `MainLayoutFrame`. Applying `safeDrawing` to the measuring `BoxWithConstraints` subtracts system bars and IME height from `maxHeight` and falsely forces `LayoutMode.FLOAT` on landscape phones.
- **Gate size updates while IME is visible**: Skip `viewModel.onWindowSize(...)` updates whenever `WindowInsets.ime.getBottom(LocalDensity.current) > 0` so opening the soft keyboard in portrait never triggers `LayoutMode.FLOAT` and unmounts `KeyboardScreen`.
