# Changelog

## app-ui Unreleased

### Added

- Modal foundation extracted from simt-app `core:ui` — `SimtDialog` (Layer-1 centered glass dialog with `wrapInDialog` window hosting, suppressed window animations, tier-aware entrance, Alert/Glass panels) plus the Layer-2 presets `ConfirmDialog`, `PromptDialog`, `InfoDialog`, `ActionListDialog`, `DateTimePickerDialog` and `RateAppDialog`, and `ContentDialog` / `ModalDefaults`, in `app.simt.ui.components.dialogs` (androidMain).
- `SimtSheetSurface` / `SheetEdge` in `app.simt.ui.components.sheets` (androidMain).
- `BlockingLoaderController` / `BlockingLoaderHandle` / `LocalBlockingLoaderState` with the `withBlockingLoader` / `blockingLoader` progress and background-warning helpers in `app.simt.ui.components.loader` (androidMain).
- `CanopyGlass`, `GradientDivider`, `GradientBackground`, `InformationBanner`, `DialogActionButtons` (stable `modal_button_cancel` / `modal_button_save` test tags) and `DataField` in commonMain component packages.
- `SimtMotion` Cosmos motion spec and `CosmosTier` / `LocalCosmosTier` ambient tier in `app.simt.ui.effects`.
- `simt_ui_`-prefixed strings for the moved components in English, French, Spanish and Arabic.
- Robolectric host smoke tests for the modal foundation (`ModalFoundationTest`: confirm/cancel callback routing, Glass-panel close button, stable action-button tags).
- Permissions kit extracted from simt-app `core:ui` in `app.simt.ui.components.permissions` (androidMain) — `PermissionGate` (unified runtime-permission / hardware-requirements gate), `FeatureRequirement` / `FeatureRequirementCard` with the presentation-level `RequirementStatus` enum, `PermissionPresentation` and `PermissionRequestDialog`, and the `AnimatedDialogBackground` style enum with a caller-supplied background slot.
- `simt_ui_`-prefixed permission strings (requirement status labels, device compatibility, get started, grant permission, blocked hint, open app settings) in English, French, Spanish and Arabic.
- Robolectric host smoke tests for the permissions kit (`PermissionGateTest`: requirements-unmet gating, get-started/grant callbacks, requirement-card fix dialog).
- Toast system extracted from simt-app `core:ui` in `app.simt.ui.components.toasts` (androidMain) — `GlobalToastHost` (click-through full-screen dialog window that stacks toasts above every screen and dialog), `ToastSurface` (canopy-glass banner) and the `ToastController` / `ToastUiState` / `ToastStyle` contract with `LocalToastController`.
- Robolectric host smoke tests for the toast kit (`ToastHostTest`: surface rendering, stacked rendering, auto-dismiss).
- `GlobalBlockingLoaderHost` extracted from simt-app `core:ui` in `app.simt.ui.effects` (androidMain) — the full-screen dialog-window renderer for the `components.loader` contract, with keep-screen-on, interrupt button and background-processing warning banner; `simt_ui_blocking_loader_*` strings in four locales.
- Robolectric host smoke tests for the loader host (`GlobalBlockingLoaderHostTest`: hidden/visible/clear, interrupt forwarding, warning banner dismissal).

### Notes

- The `dialogs`, `sheets`, `loader`, `permissions` and `toasts` packages are androidMain because they depend on Android/JVM-only APIs (`java.time`, `LocalConfiguration`, `decorFitsSystemWindows`, haptics, `DialogWindowProvider` window flags), and the `GlobalBlockingLoaderHost` effect is androidMain for the same dialog-window reason; the extraction batch's consumers are Android-only. `GlobalToastHost` collects its state with `collectAsStateWithLifecycle`; the `lifecycle-runtime-compose` artifact is an implementation-scoped dependency, never part of the public API. Source `@Preview` functions were dropped (no preview-tooling dependency in simt-ui). The permissions kit's animated header background is a caller slot: the library resolves the accent color and renders a flat tinted wash by default; applications with an ambient background (simt-app's Cosmos gate background) supply their own renderer.

- `app.simt.ui.features.update`: the in-app update feature as a self-contained package — `UpdateAvailableDialog` (animated header banner, dynamic `appName` input, cancel/update actions, English/French/Spanish/Arabic strings) plus the `UpdateChecker` / `UpdateCheckResult` contract and `PlayCoreUpdateChecker`, the Google Play in-app updates implementation that owns all play-core wiring.

## app-ui 0.1.0 — 2026-11-17

### Added

- Kotlin Multiplatform `app.simt:app-ui` artifacts for Android API 28+, iOS arm64, and iOS simulator arm64.
- Shared form, list, scroll-edge, tooltip, overlay-header, countdown, and fullscreen-inset primitives under `app.simt.ui`.
- Debug-only Android Studio preview samples covering every shared primitive and header tool.
- Licensed, byte-pinned Cosmos 2.3.0 specification snapshot with strict offline schema and integrity validation.
- ABI, documentation, source/history, publication, private-staging, and owner-gated release checks.

### Changed

- Aligned the producer with AGP 9.5.1 and Gradle 9.9.1 for sibling composite compatibility.
- Adopted the shared library on validated local simt-app and ECHO consumer branches while retaining host-owned behavior.

### Security

- Excluded application configuration, credentials, permissions, network clients, analytics, persistence, and private host data from the library boundary.
- Added bounded source/history and nested publication audits; tagged publication additionally requires confirmed redistribution rights, strict Cosmos validation, protected-environment approval, and PGP signing.
