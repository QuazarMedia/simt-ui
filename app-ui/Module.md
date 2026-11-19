# Module app-ui

Generic, caller-state-owned Compose primitives in `app.simt.ui` for Android API 28+ and Kotlin consumers targeting `iosArm64` or `iosSimulatorArm64`. Said Elimam confirmed the required UI rights and granted Apache-2.0 licensing and public redistribution on 2026-11-17. Version `0.1.0` is approved for a tag-triggered public release.

Use the caller's Material3 theme. Labels, callbacks, state, window policy, reduced motion, visibility and host lifecycle remain application-owned. Resources are private and automatically packaged; callers do not reconstruct Android resource IDs or copy assets. English, French, Spanish and Arabic strings are preserved, with default English fallback for unsupported locales.

## Fields

`ClearableTextField` is a controlled string field. Edits are truncated to `maxLength` (default 1000; null removes the limit); this operates on Kotlin string length, not grapheme clusters or locale-aware input validation. The clear action appears only for nonempty values and emits an empty string, including when `readOnly` is true. Read-only prevents keyboard editing, not the deliberate clear action. `onClick` observes released field presses and uses the current callback after recomposition. Labels, clear description, error state, keyboard options and visual transformation are caller-configurable. Do not treat visual transformation as secure storage or logging protection.

`CityCountryFields` retains caller-owned city/country values. City edits are capped at `CITY_NAME_MAX_LENGTH` (30); country edits are capped at `COUNTRY_CODE_LENGTH` (2), then uppercased. It does not validate ISO codes, persist location data or perform geocoding.

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

## Countdown and platform policies

`CountdownEffect` runs once per composition lifetime: changing configuration alone does not restart it. Use composition identity (for example `key`) to deliberately restart. It shows the positive start value down to one, waiting `tickMs` after each value; completion uses the latest `onFinished`. `onStart` is launched on the platform IO dispatcher as structured child work. It must not directly mutate platform UI and it is cancelled when the countdown leaves composition. Completion does not implicitly remove the composable; the caller controls the next screen or visibility.

`LocalUiMotionEnabled` and `LocalUiHapticsEnabled` default true and are independent. Supply effective app accessibility/lifecycle/visibility policies through `CompositionLocalProvider`; this library does not observe the host's lifecycle or system reduced-motion setting automatically. Disabled motion renders the countdown opaque at unit scale without animation; disabled haptics suppress feedback without suppressing timing/completion.

Android haptics preserve `CLOCK_TICK` for intermediate values and `LONG_PRESS` for the final value. iOS uses light and medium UIKit impact generators. These are real implementations, not no-op portability shims; compile/link and emulator checks do not establish physical feedback quality.

`Modifier.fullScreenTopInsets` and `fullScreenBottomInsets` pad and consume their respective inset sides. Android uses ignoring-visibility status/system-bar unions with cutouts, preserving immersive-overlay behavior. iOS uses safe drawing top and system-bar bottom. Bottom insets deliberately exclude IME. These helpers are not interchangeable with a host's ordinary `safeDrawing` policy or keyboard avoidance.

## Compatibility evidence and limits

The ordinary Android and CMP examples consume filesystem Maven publications without composite substitution or manual resources. Android host/emulator behavior, default R8 packaging, four locales, unsupported-locale fallback and both Native framework links have been exercised. API 28 remains host-only; iOS launch, bundled-resource runtime rendering, native IME/screen-reader behavior and physical haptics remain known validation limits. No pure Swift/SwiftUI API is promised: Kotlin consumers can embed Compose through their existing framework/controller.

Built-in Kotlin ABI checks compare the reviewed Android and Native baselines. Generated documentation, source artifacts and a deterministic `legal-notices` ZIP accompany all four publications. The ZIP contains root `LICENSE` (Apache-2.0 code), `LICENSE-DOCS` (CC-BY-4.0 Cosmos documents), `NOTICE` and `app-ui/source-provenance.json`, with no Cosmos payload or credentials. Original provenance paths/hashes describe upstream bytes, not the current package layout. Public release requires confirmed code/document/spec rights, strict source/publication audits, stable matching tag, PGP signing, and approval of the protected GitHub release environment.
