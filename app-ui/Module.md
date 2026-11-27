# Module app-ui

Generic, caller-state-owned Compose primitives in `app.simt.ui` for Android API 28+ and Kotlin consumers targeting `iosArm64` or `iosSimulatorArm64`. Said Elimam confirmed the required UI rights and granted Apache-2.0 licensing and public redistribution on 2026-11-17. Version `0.1.0` is approved for a tag-triggered public release.

Use the caller's Material3 theme. Labels, callbacks, state, window policy, reduced motion, visibility and host lifecycle remain application-owned. Resources are private and automatically packaged; callers do not reconstruct Android resource IDs or copy assets. English, French, Spanish and Arabic strings are preserved, with default English fallback for unsupported locales.

## Fields

`ClearableTextField` is a controlled string field. Edits are truncated to `maxLength` (default 1000; null removes the limit); this operates on Kotlin string length, not grapheme clusters or locale-aware input validation. The clear action appears only for nonempty values and emits an empty string, including when `readOnly` is true. Read-only prevents keyboard editing, not the deliberate clear action. `onClick` observes released field presses and uses the current callback after recomposition. Labels, clear description, error state, keyboard options and visual transformation are caller-configurable. Do not treat visual transformation as secure storage or logging protection.

`CityCountryFields` retains caller-owned city/country values. City edits are capped at `CITY_NAME_MAX_LENGTH` (30); country edits are capped at `COUNTRY_CODE_LENGTH` (2), then uppercased. It does not validate ISO codes, persist location data or perform geocoding.

`DebouncedTextField` wraps `ClearableTextField` with a caller-visible draft and trailing-edge debounce (300 ms by default). Only edits and clear actions publish after a quiet period; a replacement callback is used without restarting the pending delay. `RadioOptionLine` keeps selection caller-owned, makes both its label row and radio button select exactly once, and reveals its content only while selected. `SettingLine` renders a caller-owned switch, or an enabled permission action when permission is missing; its permission button has a 48 dp minimum touch target. Labels and supporting copy remain caller-owned.

`AppMultiChoice` and `AppSingleChoice` expose remembered working selection state with minimum-selection and required-selection rules. Both use caller-owned options and Material3 chip semantics and support wrapping or horizontal scrolling. They report the current selection after every enabled choice tap, including a tap rejected by the minimum or required rule. `AppMultiChoice` also supports option tooltips and long-press callbacks without firing a tap. `AppSelectField` is a caller-controlled read-only anchor with a height-capped scrolling dropdown; selected rows carry a localized content description. Option payloads and the external source of truth remain caller-owned.

`DoubleLabelSwitch` keeps its two-state value caller-owned and animates between the corresponding labels and supporting text; disabled switches do not publish changes. `TimePickerField` is an Android-only read-only `HH:mm` field backed by the shared `ContentDialog` and Material3 24-hour picker. Saving publishes a zero-padded time, cancelling publishes nothing, and the clear action publishes an empty string.

`IconPickerChip` and `IconPickerPalette` render caller-supplied glyphs with caller-owned expansion and selection. The palette scrolls an already-selected option into view when it opens and leaves later user scrolling untouched. `IconColorNameRow<T>` combines those primitives with a color palette and capped name field; icon payloads, colors, glyph rendering, optional option descriptions and the effective dark-theme flag stay caller-owned. `TLEField` is Android-only because it owns clipboard access and `java.time` validation. It sanitizes on blur or paste, publishes `TleValidation` with a resource-independent `TleValidationError`, and reports epoch freshness as fresh, stale, old or unknown.

## Lists and scrolling

`WindowedList` has a `LazyListScope` overload with a caller-supplied `LazyListState`, reverse layout and scroll policy, and a typed overload with mandatory stable keys, content types and an optional centered empty placeholder. Keys must follow item identity through reorder; choosing positional keys can lose remembered row state. Give lists bounded layout space rather than nesting unconstrained vertical scroll containers.

`ScrollEdgeEffect` decorates the supplied real `ScrollableState`; it does not make its child scrollable. Attach the same state to the actual list or scroll modifier. Its content has no BoxScope receiver, preserving surrounding caller layout scopes. Fades use an offscreen alpha mask against transparent content. Zero/inactive fades avoid the mask. Horizontal edges honor layout direction; use `reverseDirection` to match reversed content. Arrows are decorative, have no pointer handlers and do not become focus targets.

`ScrollEdgeEffectDefaults` exposes fade distances, arrow shift/duration, and a theme-derived tint. `LocalUiMotionEnabled = false` renders fades and visibility immediately and removes infinite arrow motion; it does not disable scrolling or independently control haptics.

## Tooltip and header

`LongPressTooltipEffect` owns one combined click/long-click handler. A tap invokes the click callback; a long press displays the tooltip instead. Do not add a competing click handler to the child for the same action. The outer modifier positions the tooltip host; `contentModifier` customizes the anchor. `fillMaxHeight` defaults true, so pass false for an anchor inside a horizontally scrolling row or another unbounded-height context. The public tooltip-position type comes from Material3 and requires the matching Compose dependencies.

`TopOverlayHeader` hosts title/subtitle/icon, caller-owned tools and an optional content card. The default close description is localized. `TopOverlayToolToggle` keeps enabled/selected state caller-owned; its callback receives the current `isOn` value, so the caller decides how to update state. Disabled tools do not activate a second wrapper click path. Tools and close use nonoverlapping 48 dp targets with 20 dp glyphs. Content cards enter/exit over 300 ms when motion is enabled, retain their last content during exit, and allow replacement/reentry; with motion disabled, visibility/removal is immediate.

The header fills its bounded parent, except Android landscape retains the original half-width policy. iOS uses the full bounded width, not physical screen size. Top insets are applied by default; pass `applyTopInsets = false` when the host already owns them to avoid duplicate padding. The header is not a global safe-area or keyboard policy.

## Update feature

`app.simt.ui.features.update` bundles the in-app update prompt as a self-contained feature: the UI (`UpdateAvailableDialog`) and the update-check contract (`UpdateChecker` / `UpdateCheckResult`) live together so hosts wire one piece to the other.

`UpdateAvailableDialog` renders an animated gradient header banner with a pulsing update icon, the app name and current version, and a cancel/update action row. `appName` is interpolated into the title and description, so a single dialog adapts to whichever application launches it. The banner honors `LocalUiMotionEnabled`: with motion disabled it renders a static gradient with the centred icon instead of the particle and ring animation. Dismissal is caller-owned — `onDismiss` fires for the cancel button, the back press and outside clicks alike, and the dialog does not remove itself. Colours come from the caller's Material3 theme.

`UpdateChecker` is the platform-neutral check contract: `checkForUpdate()` returns `UpdateCheckResult.Available` / `Unavailable` and never throws. `PlayCoreUpdateChecker` (androidMain) implements it with Google Play in-app updates and owns all play-core wiring, so host apps never touch play-core types directly. Show the dialog when the check returns `Available`, and call `startUpdateFlow` from its update action.

## Onboarding feature

`app.simt.ui.features.onboarding` (androidMain) is the onboarding spotlight: `OnboardingOverlay` dims the screen with a scrim, punches a circular or rounded-rect cutout around a highlighted region given in window coordinates, and shows an animated arrow plus an uppercase title and a description. Tapping anywhere invokes `onDismiss`; the overlay never removes itself.

The scrim cutout composites through the native canvas layer (`saveLayer` + `BlendMode.Clear`), which is why the feature lives in `androidMain`. The `enabled` flag (default true) gates rendering: applications with a "show tips" preference pass it in, so the library depends on no host settings types. Copy (title, description) arrives as plain strings — callers resolve their own resources. The text block sits above the highlight when the highlight is in the bottom half of the overlay and below it otherwise.

## Notifications feature

`app.simt.ui.features.notifications` (androidMain) is the system-notification kit: the `NotificationService` contract (channel creation, spec-to-`Notification` building, post/cancel, the runtime POST_NOTIFICATIONS check, one-shot vibration), `SystemNotificationService` — the platform `NotificationManager` / `NotificationCompat` implementation with a Hilt-free constructor, so the application binds it as a singleton in its own DI graph — and the declarative `NotificationSpec` whose defaults mirror `NotificationCompat.Builder`.

`NotificationActionBuilder` supplies the two reusable action factories: `bulkServiceAction` (tap delivers an intent back into a foreground service via `PendingIntent.getService`) and `broadcastAction` (one-shot actions delivered to a `BroadcastReceiver`), both immutable `PendingIntent`s with caller-chosen request codes. Applications keep their channel ids, icon resources and intent-action constants; the library ships no app-specific notification content.

## Auth feature

`app.simt.ui.features.auth` (androidMain) is the Google sign-in flow: `rememberGoogleSignInLauncher(webClientId, onIdToken, onError, onStart = {})` returns a `GoogleSignInLauncher` whose `launch()` runs the whole Credential Manager dance — a saved-account request (`GetGoogleIdOption`) with fallback to Google's full web sign-in (`GetSignInWithGoogleOption`) — and hands the resulting ID token to `onIdToken`, or a ready-to-display message to `onError`. `isSigningIn` tracks the in-flight attempt for button/progress UI, and `launch()` is a no-op while an attempt is running.

The `webClientId` parameter is the decoupling point: the application resolves the OAuth web client id itself — typically the `default_web_client_id` string the google-services plugin injects at build time — and passes it in, so the library reads no host resources. A blank or `placeholder` client id is rejected synchronously with a configuration error. The credential and googleid artifacts are `implementation`-scoped, so hosts never see `CredentialManager` types.

## QR feature

`app.simt.ui.features.qr` (androidMain, for the androidMain modal foundation and Android `LocalConfiguration`) is the QR render + scan surface: `QrCodeFullscreenOverlay(qrContent, label, contentDescription, onDismiss, downloadLabel, onDownload, shareLabel, onShare)` is the full-screen presentation (dark backdrop, white card, scale/fade entrance, optional download/share buttons) and `QRScannerOverlay(isScanned)` is the animated camera-scan mask — dark cutout, pulsing corner brackets, sweeping scan line, prompt label — drawn over whatever camera preview the host provides. The scan overlay is a pure drawing layer: it takes no camera handle, so the application composes it above its own scanner surface.

Both surfaces share `rememberSimtQrCodePainter`, the SIMT QR shape (rounded frame, circular balls, rounded dark pixels) backed by qrose, which is `implementation`-scoped — the public API exposes only Compose `Painter` and `String` types. `QRScannerOverlay`'s prompt text comes from the library's `simt_ui_qr_scanner_prompt` resource.

## Countdown and platform policies

`CountdownEffect` runs once per composition lifetime: changing configuration alone does not restart it. Use composition identity (for example `key`) to deliberately restart. It shows the positive start value down to one, waiting `tickMs` after each value; completion uses the latest `onFinished`. `onStart` is launched on the platform IO dispatcher as structured child work. It must not directly mutate platform UI and it is cancelled when the countdown leaves composition. Completion does not implicitly remove the composable; the caller controls the next screen or visibility.

`LocalUiMotionEnabled` and `LocalUiHapticsEnabled` default true and are independent. Supply effective app accessibility/lifecycle/visibility policies through `CompositionLocalProvider`; this library does not observe the host's lifecycle or system reduced-motion setting automatically. Disabled motion renders the countdown opaque at unit scale without animation; disabled haptics suppress feedback without suppressing timing/completion.

Android haptics preserve `CLOCK_TICK` for intermediate values and `LONG_PRESS` for the final value. iOS uses light and medium UIKit impact generators. These are real implementations, not no-op portability shims; compile/link and emulator checks do not establish physical feedback quality.

`Modifier.fullScreenTopInsets` and `fullScreenBottomInsets` pad and consume their respective inset sides. Android uses ignoring-visibility status/system-bar unions with cutouts, preserving immersive-overlay behavior. iOS uses safe drawing top and system-bar bottom. Bottom insets deliberately exclude IME. These helpers are not interchangeable with a host's ordinary `safeDrawing` policy or keyboard avoidance.

## Modals and shared components

The modal foundation extracted from simt-app `core:ui`: the canonical centered glass dialog, its preset dialogs, the full-screen sheet surface, the blocking loader, and the motion/tier tokens they drive.

### Dialogs (`app.simt.ui.components.dialogs`, androidMain)

`SimtDialog` is the Layer-1 canonical centered glass dialog: it owns the modal mechanics once (optional platform `Dialog` window hosting via `wrapInDialog`, suppressed window animations, tier-aware scale+fade entrance) and renders an `Alert` panel (icon/title/text/action slots) or a `Glass` panel (free-form content plus an optional top-end close button). `onClose` renders only for the Glass variant, which does not display a `title`. `ContentDialog` and `ModalDefaults` expose the edge-to-edge `DialogProperties` for callers hosting their own dialogs.

Layer-2 presets built on it: `ConfirmDialog` (confirm/cancel callbacks, destructive styling, height-capped scrollable content), `PromptDialog`, `InfoDialog`, `ActionListDialog`, `DateTimePickerDialog`, and `RateAppDialog`. Presets keep callbacks and state caller-owned; dismissal never removes the dialog by itself.

### Sheets (`app.simt.ui.components.sheets`, androidMain)

`SimtSheetSurface` is the full-screen sheet with `TopOverlayHeader`, a `GradientBackground`, an optional blocking loader bound to `isBlocking`, and a `DialogActionButtons` row. Save/cancel labels are plain `String` values; callers resolve their own resources.

### Blocking loader (`app.simt.ui.components.loader`, androidMain)

`BlockingLoaderController`, `BlockingLoaderHandle` and `LocalBlockingLoaderState` carry the blocking-operation contract. `withBlockingLoader` runs `block` with a handle, rethrows `CancellationException` and always closes the handle. `blockingLoader` adds the progress tick loop (500 ms cadence bounded by `timeoutMs`), an abort check, and background-processing warnings: a generic warning after `BLOCKING_LOADER_WARNING_THRESHOLD_MS` (10 s) and the long-running guidance after `BACKGROUND_PROCESSING_WARNING_THRESHOLD_MS` (20 s).

### Surfaces, banners, forms, buttons (commonMain)

`CanopyGlass`, `GradientBackground` and `GradientDivider` are the glass/gradient surfaces; `InformationBanner` is a closable informational row; `DialogActionButtons` is the labeled action row with stable `modal_button_cancel` / `modal_button_save` test tags; `DataField` wraps a labeled input with error text. `DebouncedTextField`, `RadioOptionLine` and `SettingLine` complete the extracted basic form-control kit.

`OverlayIconButton`, `QuickActionButton`, `LockButton`, `FavoriteToggleButton`, `VisibilityToggleButton` and `BetaChip` retain caller-owned state, callbacks and Material3 styling. Toggle and lock descriptions are localized; favorite and visibility controls combine tap and long-press tooltip behavior without changing the caller's value directly.

`IconLabelButton` and `PremiumIconLabelButton` render uppercase captions with caller-provided glyphs, colors and callbacks; disabled controls do not publish actions. Both honor compact sizing, including the premium red-mode fallback. `IconLabelButtonRow` uses a centered row while its caller-supplied `IconLabelAction` list fits and switches to a lazy horizontal scroller with edge effects on overflow. It publishes caller-owned selection semantics, keeps measurement-only items out of accessibility and positioning callbacks, defaults null tooltip text to the label, and suppresses the tooltip for explicit blank text. A tooltip long press never invokes the action. `IconLabelActionsToolbar` adds caller-owned prefix/suffix slots, theme-derived edge/divider colors, a caller-selected shape and configurable overflow behavior; red-mode presentation remains an explicit caller choice.

`RoundMultiButton` expands caller-provided icon actions around a main button and owns only transient disclosure state. Disabling the control or removing every item collapses it; activating an item collapses before invoking its callback. Caller-supplied descriptions label the main and item actions. Expansion follows layout direction and `LocalUiMotionEnabled`; reduced motion adds or removes actions immediately without transition frames.

### Motion and tier (`app.simt.ui.effects`)

`SimtMotion` is the central Cosmos motion spec (FAST 150 ms / MEDIUM 300 ms, enter/exit/transform easings, `enterSpec`/`exitSpec`). `CosmosTier` (`FULL` / `REDUCED` / `STATIC`) with `LocalCosmosTier` (default `FULL`) lets shared UI degrade ambient animation on lower-performance devices; app roots should provide the tier from their device performance level.

`app.simt.ui.transitions` provides the shared screen fade/scale transitions and `Modifier.staggeredEntrance`. The entrance runs once per composition, keeps its delay and offset caller-controlled, and renders immediately at the final position when `LocalUiMotionEnabled` is false or the cosmos tier is `STATIC`.

`CosmosBackground` provides the shared warm deep-space sky with caller-owned theme colors, optional accent, heading parallax and four density/nebula variants. With UI motion enabled, full tier adds ambient drift and twinkle while reduced tier retains parallax without infinite motion. Disabled UI motion or `frozen` stops ambient animation while FULL/REDUCED tiers retain heading parallax; the STATIC tier stops both ambient motion and parallax.

`CosmosSpecks` provides a deterministic ambient star field. `CosmosWaveField`, `CosmosSuccessBurst` and `CosmosErrorBurst` share the same caller-owned motion policy: full tier adds infinite twinkle/glow, reduced tier retains one-shot entrances, and disabled motion or the static tier renders the final visual immediately. The active wave field retains its functional progress indicator when cosmetic motion is disabled.

`GlobalBlockingLoaderHost` (androidMain) renders the `components.loader` contract above everything — including open dialogs — in its own full-screen `Dialog` window: dimmed click-absorbing backdrop, indeterminate or linear progress, optional message chip, an optional interrupt button wired to `controller.requestInterrupt()`, and an optional background-processing warning `InformationBanner`. While visible it keeps the screen on (view flag + window flag, both restored on dispose). The `findActivity` context walk is module-internal; the generic warning copy ships as `simt_ui_blocking_loader_background_processing_warning` in the four locales.

### Permissions (`app.simt.ui.components.permissions`, androidMain)

`PermissionGate` is the single gate for permission- and hardware-gated features: an animated header (icon medallion over a caller-supplied background), a title with optional experimental tag, a device-compatibility row of `FeatureRequirement` items, and a body that adapts to the situation — requirements unmet (primary action hidden, `requirementsNotMetText` shown), blocked ("Don't ask again" → open-app-settings action), or ready (optional closable `InformationBanner` explanation plus the primary action, which fires a virtual-key haptic before invoking `onGetStarted`).

`FeatureRequirement` is the presentation-level model (`label` / `status` / `description` / `fixActionLabel` / `onFix`); its `RequirementStatus` enum is a mirror of the application's availability model, and applications map their own statuses onto it at the call site. `FeatureRequirementCard` renders the status chip; a card with a description is tappable and opens an info dialog that surfaces a "Fix it" action when `onFix` is provided and the requirement is unmet.

`PermissionPresentation` bundles the icon, tint and copy resources for one runtime permission; the application owns the mapping from its permission model onto this class. `PermissionRequestDialog` wraps `PermissionGate` with the presentation's copy and grant/cancel button labels.

The animated header background is a caller slot (`headerBackground` on the gate and dialog, `background` on `AnimatedDialogBackground`): the library resolves the accent from `AnimatedDialogBackgroundStyle` (`NetworkGrid` → secondary color, all other styles → primary) and renders a flat tinted wash by default; applications with an ambient background (simt-app's Cosmos gate background) supply their own renderer.

### Toasts (`app.simt.ui.components.toasts`, androidMain)

`ToastController` is the application-wide contract for one-shot banners: a `StateFlow` stack of `ToastUiState` (monotonic `id`, `message`, `style`, optional custom icon) plus `showSuccess` / `showError` / `showInfo` / `dismiss` / `clear`. Applications own the implementation (a singleton injected into view-models) and provide it through `LocalToastController`.

`GlobalToastHost` renders the stack above every screen — including open dialogs — inside its own click-through full-screen `Dialog` window (`FLAG_NOT_FOCUSABLE` + `FLAG_NOT_TOUCHABLE`, platform window animations disabled). Each entry animates in (slide + fade), holds for 3 s, then animates out (shrink + fade) before calling back into `controller.dismiss`, so the stack reflows smoothly. `ToastSurface` is the bare canopy-glass banner visual; the fixed accent colors (success green, error red, info blue) and the 3 s duration are module-internal.

### Placement

The `dialogs`, `sheets`, `loader`, `permissions` and `toasts` packages live in `androidMain` because they use Android/JVM-only APIs (`java.time` pickers, `LocalConfiguration`, `DialogProperties.decorFitsSystemWindows`, haptics, `DialogWindowProvider` window flags), the `GlobalBlockingLoaderHost` effect is `androidMain` for the same dialog-window reason, `features.onboarding` is `androidMain` for its native-canvas scrim compositing, `features.notifications` is `androidMain` because it is pure Android platform work (`NotificationCompat`, channels, vibrator), `features.auth` is `androidMain` because it builds on Android Credential Manager and Google ID, and `features.qr` is `androidMain` because its full-screen presentation builds on the androidMain modal foundation and Android `LocalConfiguration`; `components.forms.TLEField` is `androidMain` because it uses Android clipboard access and `java.time`; the remaining components are in `commonMain`. The consumers of this extraction batch are Android-only (simt-app). `@Preview` functions from the source files were not carried over — simt-ui has no preview-tooling dependency.

## Compatibility evidence and limits

The ordinary Android and CMP examples consume filesystem Maven publications without composite substitution or manual resources. Android host/emulator behavior, default R8 packaging, four locales, unsupported-locale fallback and both Native framework links have been exercised. API 28 remains host-only; iOS launch, bundled-resource runtime rendering, native IME/screen-reader behavior and physical haptics remain known validation limits. No pure Swift/SwiftUI API is promised: Kotlin consumers can embed Compose through their existing framework/controller.

Built-in Kotlin ABI checks compare the reviewed Android and Native baselines. Generated documentation, source artifacts and a deterministic `legal-notices` ZIP accompany all four publications. The ZIP contains root `LICENSE` (Apache-2.0 code), `LICENSE-DOCS` (CC-BY-4.0 Cosmos documents), `NOTICE` and `app-ui/source-provenance.json`, with no Cosmos payload or credentials. Original provenance paths/hashes describe upstream bytes, not the current package layout. Public release requires confirmed code/document/spec rights, strict source/publication audits, stable matching tag, PGP signing, and approval of the protected GitHub release environment.
