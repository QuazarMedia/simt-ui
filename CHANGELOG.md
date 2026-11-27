# Changelog

## Repository Unreleased

### Added

- Root [security policy](SECURITY.md) and [contribution guide](CONTRIBUTING.md), linked from the README with private reporting, release ownership and repository-hardening requirements.
- Static publishing-workflow tests for immutable Action pins, exact package/version guards, pre-publication changelog validation, OIDC-only npm publication, step-scoped Maven credentials, retry behavior and npm Dependabot coverage.
- npm Dependabot entries for `web-ui`, `design-spec` and `design-site`.

### Changed

- Extended the namespaced tag workflow to publish both `app.simt:app-ui` and `@simt-dev/web-ui`, with exact registry/version checks and retry-safe GitHub Release creation or updates.
- Corrected public installation instructions, artifact names, module boundaries and actual `0.1.0` publication status in the README and app-ui module documentation.
- Regenerated the SIMT agent hook without a machine-specific fallback path.

### Security

- Pinned every publishing Action to a full commit SHA, kept workflow permissions least-privilege, removed the broad release-secret preflight and scoped Central credentials/signing material to the Maven publish step.
- Added npm trusted publishing through GitHub OIDC with provenance, a pinned supported npm CLI and no long-lived npm token; documented the required npm-side publisher configuration and token retirement.
- Kept source/history auditing fail-closed while narrowly exempting one reviewed historical generated hook blob from only the personal-path category by its exact SHA-256 digest. No credential, private-key or forbidden-file category is exempt.
- Documented the remaining release blockers: protect the existing `release` environment, move repository-scoped Central/signing secrets into it, and enable the missing GitHub repository security controls.

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
- Onboarding spotlight extracted from simt-app `core:ui` in `app.simt.ui.features.onboarding` (androidMain) — `OnboardingOverlay` highlights a window-coordinate region under a punched scrim with an animated arrow, title and description; the app's "show tips" setting no longer leaks in and is passed as the `enabled` parameter instead.
- Robolectric host smoke tests for the onboarding spotlight (`OnboardingOverlayTest`: text-placement rule ported from simt-app, `enabled` gate, dismiss callback).
- Notifications kit extracted from simt-app `core:ui` in `app.simt.ui.features.notifications` (androidMain) — the `NotificationService` contract, `SystemNotificationService` (platform `NotificationManager` / `NotificationCompat` implementation, Hilt-free constructor), the declarative `NotificationSpec` and the `NotificationActionBuilder` service/broadcast action factories. `androidx.core:core-ktx` is now an `api`-scoped androidMain dependency because `NotificationCompat.Action` is part of the public surface.
- Robolectric host smoke tests for the notifications kit (`NotificationKitTest`: channel creation attributes, spec-to-notification mapping, post/cancel, both action builders).
- Google sign-in extracted from simt-app `core:ui` in `app.simt.ui.features.auth` (androidMain) — the `GoogleSignInLauncher` Credential Manager controller plus the `rememberGoogleSignInLauncher` composable. The OAuth web client id is now an explicit `webClientId` parameter (the application resolves the google-services-injected `default_web_client_id` itself), and the two sign-in error strings moved into the `simt_ui_`-prefixed resources in English, French, Spanish and Arabic.
- `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth` and `com.google.android.libraries.identity.googleid:googleid` as `implementation`-scoped androidMain dependencies, kept out of the public ABI surface.
- Robolectric host smoke tests for the sign-in launcher (`GoogleSignInLauncherTest`: placeholder/blank client-id gate, failed attempt error path with state reset, in-flight no-op).
- QR feature extracted from simt-app `core:ui` in `app.simt.ui.features.qr` (androidMain) — `QrCodeFullscreenOverlay` (the full-screen QR presentation with optional download/share actions) and `QRScannerOverlay` (the animated scan mask with corner brackets, scan line and prompt). The `qr_scanner_prompt` string moved into the `simt_ui_`-prefixed resources in English, French, Spanish and Arabic.
- `io.github.alexzhirkevich:qrose` 1.3.1 as an `implementation`-scoped androidMain dependency; the painter API is Compose-only, so the public surface stays free of qrose types.
- Host tests for the QR width rule (`QrCodeFullscreenOverlayTest`, ported from simt-app).
- Basic form controls extracted from simt-app `core:ui` in commonMain — `DebouncedTextField` (trailing-edge debounce with the latest callback), `RadioOptionLine` (caller-owned selection with animated content) and `SettingLine` (switch or permission action, with a 48 dp minimum permission-button target).
- Robolectric host behavior tests for the basic form controls (`BasicFormsTest`: debounce timing and callback replacement, radio row/button selection, permission-to-switch transition, touch-target size and disabled state).
- Choice and select controls extracted from simt-app `core:ui` in commonMain — `AppMultiChoice` / `AppSingleChoice` with remembered selection state, wrapping/scrolling layouts, icons and tooltips, plus `AppSelectField` with a height-capped dropdown and localized selected description.
- Robolectric host behavior tests for the choice controls (`ChoiceControlsTest`: minimum and optional selection, disabled controls, long-press suppression, and select-field choice propagation).
- Simple form controls extracted from simt-app `core:ui` — `DoubleLabelSwitch` in commonMain with caller-owned state and active supporting text, plus Android-only `TimePickerField` with the shared dialog, 24-hour selection, save/cancel and clear behavior.
- Robolectric host behavior tests for the simple form controls (`BasicFormsTest`: switch toggling and disabled state, active supporting text, time-picker open/save/cancel and clear propagation).
- Icon and color form controls extracted from simt-app `core:ui` — generic `IconPickerChip`, `IconPickerPalette` and `IconColorNameRow<T>` in commonMain, with caller-supplied glyphs, colors and dark-theme policy.
- Android-only `TLEField` with clipboard paste/clear, blur sanitization, checksum validation, epoch freshness and a resource-independent `TleValidationError`; `simt_ui_tle_*` and `simt_ui_cd_paste` strings ship in English, French, Spanish and Arabic.
- Robolectric host behavior tests for palette auto-scroll, icon/color/name selection and name capping, plus TLE sanitization, validation errors, freshness boundaries, invalid epochs and clipboard actions.
- Reusable `InfoRow`, `MetricBadge` and `CustomToolbar` layout primitives in `app.simt.ui.components.layout` (commonMain), with caller-owned content, theme, modifiers and callbacks.
- `AccordionGroup` in `app.simt.ui.components.layout` (commonMain), with caller-owned expansion, labels, optional subtitle/count, tri-state and long-press actions, 48 dp action targets and reduced-motion-aware transitions.
- `EditableItemCard` and `EditableItemViewAction` in `app.simt.ui.components.layout` (commonMain), with caller-owned icons, accessibility labels, warning content, title/view/edit/delete actions, replacement edit content and 48 dp disabled-safe action targets.
- `AppScreen` in `app.simt.ui.components.layout` (commonMain), a string-based full-screen layout with caller-owned background, header, action and content slots, horizontal/bottom safe-drawing insets, optional scrolling and shared dialog-action defaults.
- Robolectric host behavior tests for layout rows, metric labels, callback replacement, horizontally scrolling toolbar actions, accordion disclosure, tri-state isolation, long press, touch targets, motion policy, editable-item action isolation, disabled semantics and edit save/cancel forwarding, plus AppScreen composition, single root-modifier application, action routing, disabled save and fixed/scrollable content (`LayoutPrimitivesTest`, `AccordionGroupTest`, `EditableItemCardTest`, `AppScreenTest`).
- `VerticalTwinControl`, `MapControlPanel`, `MapControlButton` and `MapControlLabel` in `app.simt.ui.components.controls` (commonMain), with caller-owned icons, labels, tints, enabled state, callbacks and additional content.
- Robolectric host behavior tests for independent twin actions, disabled map actions and tooltip suppression, panel slot ordering, 48 dp touch targets and large-font zoom labels (`VerticalTwinControlTest`, `MapControlPanelTest`, `LongPressTooltipEffectTest`).

### Changed

- `LongPressTooltipEffect` accepts caller-owned `enabled` state and an optional semantic `role`; disabled anchors suppress click, long-click and tooltip input.

### Notes

- The `dialogs`, `sheets`, `loader`, `permissions` and `toasts` packages are androidMain because they depend on Android/JVM-only APIs (`java.time`, `LocalConfiguration`, `decorFitsSystemWindows`, haptics, `DialogWindowProvider` window flags), the `GlobalBlockingLoaderHost` effect is androidMain for the same dialog-window reason, `features.onboarding` is androidMain because its scrim cutout composites through the native canvas layer (`saveLayer` / `BlendMode.Clear`), `features.notifications` is androidMain because it is pure Android platform work (`NotificationCompat`, channels, vibrator), `features.auth` is androidMain because it builds on Android Credential Manager and Google ID, and `features.qr` is androidMain because its full-screen presentation builds on the androidMain modal foundation (`ModalDefaults.FullScreenDialogProperties`) and Android's `LocalConfiguration`; the extraction batch's consumers are Android-only. `GlobalToastHost` collects its state with `collectAsStateWithLifecycle`; the `lifecycle-runtime-compose` artifact is an implementation-scoped dependency, never part of the public API. Source `@Preview` functions were dropped (no preview-tooling dependency in simt-ui). The permissions kit's animated header background is a caller slot: the library resolves the accent color and renders a flat tinted wash by default; applications with an ambient background (simt-app's Cosmos gate background) supply their own renderer.

- `app.simt.ui.features.update`: the in-app update feature as a self-contained package — `UpdateAvailableDialog` (animated header banner, dynamic `appName` input, cancel/update actions, English/French/Spanish/Arabic strings) plus the `UpdateChecker` / `UpdateCheckResult` contract and `PlayCoreUpdateChecker`, the Google Play in-app updates implementation that owns all play-core wiring.
- `app.simt.ui.features.onboarding`: the onboarding spotlight as a self-contained feature — `OnboardingOverlay` takes the highlighted region, copy and callbacks; the `enabled` flag replaces the app's settings-local gate so no host types leak into the library.
- `app.simt.ui.features.notifications`: the notifications kit as a self-contained feature — applications keep the DI binding (`SystemNotificationService` is a plain-constructor singleton) and the channel/icon constants; the library ships the contract, the platform implementation and the action factories.
- `app.simt.ui.features.auth`: the Google sign-in feature as a self-contained package — `rememberGoogleSignInLauncher(webClientId, onIdToken, onError, onStart)` runs the full Credential Manager flow (saved-account request with web sign-in fallback, ID-token extraction) and reports ready-to-display error messages; the application supplies the `default_web_client_id` resolved from its own build-time resources.

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

### Notes

- Maven Central publication and the GitHub Release completed on 2026-11-18. Later additions are tracked under `app-ui Unreleased` and are not part of the published `0.1.0` bytes.

## web-ui 0.1.0 — 2026-11-27

### Added

- Published `@simt-dev/web-ui` to npm as dependency-free ESM and CSS for static sites.
- Reveal/stagger motion, ambient pausing, header/section observers, performance tiering, pointer spotlight/tilt, theme/motion controls, buttons and marquees.
- Optional prepaint bootstrap and a separate opt-in preference-storage module.
- Node-based behavior tests and package exports for runtime helpers, styles, bootstrap, storage and package metadata.

### Security

- Kept runtime code free of third-party package dependencies and made persistence an explicit opt-in boundary for privacy-only consumers.
- Published the package under Apache-2.0 with public repository metadata.
