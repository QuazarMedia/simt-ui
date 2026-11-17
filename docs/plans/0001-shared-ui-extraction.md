# SIMT UI: shared Compose library extraction

- Investigation date: 2026-11-17.
- Status: private extraction, independent consumer builds, Cosmos snapshot, ABI/docs and local release tooling implemented; public redistribution and production migration remain rights/runtime/owner-gated.
- Owner: `simt-ui`, with producer-first integration in `simt-app` and `simt-echo`.
- Original investigation scope: this plan and its parent directories only. Later authorized private implementation and verification are recorded in the evidence sections; no remote artifact publication or production migration is claimed.

## 1. Recommendation and decision boundary

**Prefer a common Kotlin Multiplatform `:app-ui` module, not an Android-only component module.** Publish its Android AAR and iOS klibs under one KMP root coordinate, `app.simt:app-ui`. An ordinary Android Compose app can consume its Android variant without becoming multiplatform; ECHO can consume the same coordinate from `commonMain` and render it on Android and iOS.

Start with exactly two production modules:

| Module | Responsibility | Consumption |
|---|---|---|
| `:app-ui` | Theme-respecting Compose primitives; Android and iOS implementations only where needed | Maven Central KMP dependency; local composite substitution during development |
| `:cosmos-spec` | Licensed, versioned Cosmos specification snapshots, machine-readable exports, provenance, and verification | Repository documentation and reproducible ZIP; not an application runtime dependency |

At investigation time this was an architectural go-ahead, **not a verified build-compatibility claim**. The later private extraction and independent published-artifact builds now validate Android resource packaging, common Material APIs and both Native framework links, as recorded below. Native runtime and the remaining producer gates still prevent production host migration.

“Without big impact” means: no conversion of simt-app to KMP; no navigation, DI, domain, persistence, or permission changes; no wholesale `:core:ui` extraction; no forced visual redesign; no mass import rename; no manual copying of library assets into consuming applications. A broad toolchain migration or unresolved publication/resource issue fails this condition.

If that gate fails, report the concrete blocker and obtain a decision before proceeding. The fallback is `:ui-android` plus `:cosmos-spec`, directly usable by simt-app and ECHO's `androidMain` only. Do not claim an Android AAR can be imported from ECHO's `commonMain` or used on iOS; do not maintain duplicate Android and common component implementations by default.

## 2. Inspected source state before extraction

### 2.1 Extraction boundary and real dependency path

`simt-app/app-ui` is already a separate Git submodule, not an app-owned directory awaiting a first split. Its declared upstream is `QuazarMedia/app-ui`; Wakt also declares this upstream. The inspected SIMT checkout is clean at `970b6fe6b3d2081bd1c69523cafefa6aa1d096b3`. There are 26 tracked files and eight Kotlin source files; no tracked tests or fonts were found in this boundary.

The module has no host domain, Hilt, navigation, network, database, or Play Services dependency. However, it is **not a standalone Gradle build**: it relies on the host's plugin classpath and `libs` catalog, uses `com.android.library`, and has no settings, wrapper, or publication configuration of its own. Android compile SDK is 37 and minimum SDK is 28.

Only `simt-app/core/ui/build.gradle.kts` directly declares `api(project(":app-ui"))` in the inspected Gradle sources. Feature modules access its public types transitively through `:core:ui`; therefore the primary migration is one API dependency replacement, not dozens of new feature dependencies. Existing Kotlin imports remain important compile-time consumers. External `app.shared.ui.R` imports were not found outside app-ui in the inspected simt-app Kotlin sources.

Do not confuse this extraction with `:core:ui`: that module also depends on `:core:common`, `:core:domain`, Credential Manager, and Google identity libraries, and has separate GitHub Packages publishing configuration. Its application wrappers and integrations remain app-owned.

Evidence: [submodule declaration](../../../simt-app/.gitmodules#L1), [existing inclusion and GNSS substitution](../../../simt-app/settings.gradle.kts#L36), [app-ui build](../../../simt-app/app-ui/build.gradle.kts#L1), [core UI dependency boundary](../../../simt-app/core/ui/build.gradle.kts#L44), [separate core-ui publication](../../../simt-app/core/ui/build.gradle.kts#L78).

### 2.2 Component-by-component portability

| Existing source | Findings | Proposed treatment |
|---|---|---|
| [WindowedList.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/components/WindowedList.kt#L39) | Already Compose-only; exposes `LazyListState`, `LazyListScope`, keys, content types, and an empty slot | Move implementation to `commonMain`; preserve both overloads, stable-key behavior, scope contract, and reverse-layout semantics |
| [ScrollEdgeEffect.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/effects/ScrollEdgeEffect.kt#L105) | Runtime renderer is Compose-only; Android `Configuration` is used by previews. Offscreen `DstIn`, arrows, layout direction, and infinite animation need platform evidence | Share renderer; move Android previews out; test transparent backgrounds, RTL/reversal, input pass-through, and GPU cost on both platforms |
| [LongPressTooltipEffect.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/effects/LongPressTooltipEffect.kt#L49) | Compose-only, but public signature exposes experimental `TooltipAnchorPosition`; wraps clickable content | Verify the selected Material API on all targets; preserve source API when supported; test nested clicks, long press, keyboard/accessibility dismissal, and layout scopes |
| [ClearableTextField.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/components/forms/ClearableTextField.kt#L44) | Only Android resource lookup is intrinsically platform-specific; state is hoisted. Interaction collector can retain an old `onClick` | Use CMP string resources; preserve existing parameters; fix callback freshness with behavior tests; verify IME, selection, read-only clear, and text-length behavior |
| [CityCountryFields.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/components/forms/CityCountryFields.kt#L21) | Android resource lookup; otherwise Compose and Kotlin. Two-letter truncation is not complete ISO country validation | Share fields/resources and existing normalization contract; leave geocoding, validation, persistence, and personal place data in the app |
| [TopOverlayHeader.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/components/TopOverlayHeader.kt#L74) | Uses Android `LocalConfiguration` orientation, fullscreen insets, Android previews, and experimental tooltip API | Common content/composition; isolate width policy and insets; preserve Android landscape half-width first; give iOS constraint-based behavior; test animated content-slot retention |
| [CountdownEffect.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/effects/CountdownEffect.kt#L53) | Android `LocalView` haptics plus explicit `Dispatchers.IO`; visual rendering and timing are portable | Common rendering/timing; narrow haptic seam with Android/iOS actuals; settle callback context and restart/cancellation contract before publishing |
| [FullscreenInsets.kt](../../../simt-app/app-ui/src/main/java/app/shared/ui/utils/FullscreenInsets.kt#L18) | No Android import, but `statusBarsIgnoringVisibility`, `systemBarsIgnoringVisibility`, and cutout semantics are platform-specific | Preserve public modifier functions through expect/actual implementations; Android retains current hidden-bar behavior, iOS applies safe-area top/bottom and avoids double consumption |

All eight can plausibly share their UI implementation; being free of `android.*` imports alone does not prove a source belongs in `commonMain`. Insets, dispatchers, resources, previews, haptics, and experimental APIs require explicit treatment.

Countdown has one located simt-app call at [MeasureScreen.kt:581](../../../simt-app/feature/measure/src/main/java/app/simt/feature/measure/screen/MeasureScreen.kt#L581), which does not pass `onStart`. Preserve the public callback and its existing `Dispatchers.IO` dispatch semantics where supported by the selected Native coroutine version; verify this explicitly in the spike. Prefer caller-owned I/O context for future APIs, but do not silently change this existing contract during extraction. Any incompatible Native behavior requires a documented alternative and approval after checking other consumers. Cancellation must not invoke `onFinished`, completion occurs once, latest callbacks are observed, and changing countdown configuration either explicitly restarts or follows a documented stable run contract.

### 2.3 Toolchain compatibility: promising, but not yet resolved

| Item | simt-app working tree | simt-echo working tree | Initial library choice |
|---|---|---|---|
| Kotlin catalog | `2.4.21`; a separate `kotlinGradlePlugin` entry is `2.4.20` | `2.4.21` | `2.4.21`; verify effective plugin versions, not just catalog entries |
| Compose | AndroidX BOM `2026.09.00` | CMP `1.12.2` | CMP `1.12.2`; independently verify Material, icons, and resources |
| AGP | `9.5.1` | `9.5.1` | Candidate `9.5.1`, subject to supported-range/build gate |
| Wrapper | Gradle `9.9.1` | Gradle `9.9.1` | Choose from verified KGP/AGP intersection; do not blindly copy the wrappers |
| Android floor | API 28 | API 29 | API 28; compile SDK 37 |
| Bytecode | App convention sets Java 21; app-ui has no explicit target | Shared module sets JVM 11 | Explicit JVM 11 for the library; build JDK chosen separately from bytecode target |
| Apple targets | None | `iosArm64`, `iosSimulatorArm64` | Both existing ECHO targets; no speculative additional platform targets |

JetBrains' documentation maps CMP `1.12.2` to Jetpack Compose `1.12.1` on Android. The locally cached AndroidX BOM `2026.09.00` POM also lists animation, foundation, runtime, and UI `1.12.1`, Material3 `1.4.0`, and extended icons `1.7.8`. This is strong evidence that the base Compose lines already align, **not proof of the consumers' complete resolved graphs**. Material3 versions independently; do not pin every JetBrains artifact to `1.12.2` or mistake a cached artifact for the selected dependency.

The official KGP table retrieved on the investigation date lists Kotlin `2.4.20–2.4.21` with Gradle `7.6.3–9.7.0`, AGP `8.5.2–9.3.1`, and Xcode `26.4`. Both consumers' AGP and wrappers exceed those documented maxima. This does not establish failure, but prevents calling the combination officially supported. Record the actual Xcode version and supported SDK/runtime before Native validation. A supported producer build may still work with newer consumers; test publication and composite use separately rather than downgrading hosts as an extraction side effect.

Use `org.jetbrains.kotlin.multiplatform`, `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`, and `com.android.kotlin.multiplatform.library`. Give the new standalone build its own catalog and versioned plugins; use current `kotlin { android { ... } }` DSL. The Android-KMP plugin has one Android variant, not debug/release build types. Do not transplant `singleVariant("release")`, legacy `androidTarget`, or simt-app's Android-only convention plugin into this KMP module.

Evidence: [app catalog](../../../simt-app/gradle/libs.versions.toml#L13), [app Compose BOM](../../../simt-app/gradle/libs.versions.toml#L48), [app wrapper](../../../simt-app/gradle/wrapper/gradle-wrapper.properties#L3), [app bytecode convention](../../../simt-app/build-logic/src/main/kotlin/app/simt/buildlogic/AndroidConfig.kt#L59), [ECHO catalog](../../../simt-echo/gradle/libs.versions.toml#L8), [ECHO wrapper](../../../simt-echo/gradle/wrapper/gradle-wrapper.properties#L3), [ECHO KMP targets and dependencies](../../../simt-echo/shared/build.gradle.kts#L13). External guidance is in section 10.

## 3. Repository and module structure

The tree below is the implementation target, not a claim that these files currently exist:

```text
simt-ui/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew, gradlew.bat, gradle/wrapper/
  gradle/libs.versions.toml
  LICENSE, NOTICE, README.md, CHANGELOG.md
  .github/workflows/
  app-ui/
    build.gradle.kts
    api/
    src/commonMain/kotlin/app/shared/ui/
      components/       # current header/list files retain their packages initially
        forms/
      effects/          # current names retained; no extraction-only API rename
      utils/            # current fullscreen modifier names retained
      internal/platform/
    src/commonMain/composeResources/
      values/, values-fr/, values-ar/, values-es/
    src/androidMain/kotlin/app/shared/ui/internal/platform/
    src/iosMain/kotlin/app/shared/ui/internal/platform/
    src/commonTest/kotlin/
    src/androidHostTest/kotlin/
    src/androidDeviceTest/kotlin/
    src/iosTest/kotlin/
  cosmos-spec/
    build.gradle.kts
    snapshots/<cosmos-version>/
      manifest.json, specification.json, rules.json, tokens.json
      contrast-pairs.json, motion.json, components.json
      specification.schema.json, rules.schema.json
      specification.md, common.md, mobile-app.md
      web-page.md, web-app.md, watch-app.md
      mobile-app/tokens.json, mobile-app/rules.json
      web-page/tokens.json, web-page/rules.json
      web-app/tokens.json, web-app/rules.json
      watch-app/tokens.json, watch-app/rules.json
    provenance.json
  examples/
    android-consumer/   # independent Android-only build consuming published coordinates
    multiplatform-consumer/ # independent KMP build and iOS controller, same coordinates
  docs/plans/0001-shared-ui-extraction.md
```

`app-ui` depends only on the necessary Compose artifacts, resources, and coroutine core. Use `api` where public signatures expose Compose types and `implementation` for private helpers; verify both compile and runtime dependency metadata. No dependency points back to either host. Samples are not published and must not use host build logic or app configuration.

`cosmos-spec` is a docs/data Gradle project using `base` plus verification/archive tasks, not a fake Android or KMP runtime module. It is not an `api` dependency of app-ui. A library build from a clean checkout must not require a sibling simt-space checkout, live network retrieval of specs, or the website deployment toolchain.

Do not split eight files into separate effects, layouts, forms, tokens, platform, and utility artifacts. Add a Gradle module when there is a real independent dependency/platform/release boundary. Plausible later additions are `:cosmos-compose` for an optional theme/components adapter, `:ui-android-integrations` for Android View/window integrations, or `:ui-wear` for Wear-specific UI. None is scaffolded as an empty promise in the initial extraction.

### 3.1 Component/effect/layout organization and public APIs

Preserve `app.shared.ui` packages and current entrypoint names for the initial migration. Coordinates and repository names do not require package renaming. No forwarding compatibility wrappers or aliases are needed. Semantic groupings below apply to new APIs or a separately approved breaking release, not mandatory import churn:

| Family | Put here | Do not put here |
|---|---|---|
| `components/forms` | Fields and stateless form groups | Repositories, geocoders, persistence, app validation policy |
| `components/feedback` | Visible countdown/progress/empty/error content | Background workflow orchestration |
| `components/overlays` | Header and composable tooltip surfaces | Activities, permission requests, application routing |
| `layouts/lists` | List containers and slot-based arrangements | Domain-specific record screens |
| `layouts/insets` | Layout padding/safe-area contracts | Window flag mutation or immersive-mode ownership |
| `effects/scroll` | Fade masks and scroll overflow affordances | Lazy data loading/paging ownership |
| `effects/motion` | Animation policy and small visual modifiers | Lifecycle/power service implementations |
| `foundation` | Shared spacing/shape/motion types when actual consumers justify them | An unconditional branded theme or global device state |
| `internal/platform` | Minimal actual implementations | Android/iOS types in public common signatures |

A component renders content or offers an interaction; a layout arranges supplied slots; an effect changes drawing/animation/behavior without owning app workflows. Existing `CountdownEffect` and `LongPressTooltipEffect` are historically component-like names, not a reason to rename them during extraction.

Apply official Compose component guidelines:

- Values plus event callbacks; hoist observable/control state. Internal animation and tooltip visibility can remain local. Do not accept ViewModels or `MutableState` as convenient library contracts.
- One root `modifier`, first optional parameter, applied once before internal decoration. Preserve scoped modifier and slot behavior; do not shadow an outer layout scope accidentally.
- Slots for caller-owned content where needed, rather than proliferating title/icon/action configuration models. Keep default parameters source-compatible and document callback context/cancellation.
- Small `FooDefaults` objects for genuine component defaults. Use the caller's `MaterialTheme`; do not silently replace app themes. Do not expose every internal brush/helper as API.
- Mark data immutable/stable only when its invariants justify the promise. Keep generated `Res`, platform handles, renderer internals, and test helpers private/internal.
- Explicit public API types and ABI dumps/checks. Adding a defaulted parameter can still break compiled clients; source compatibility is not binary compatibility. Stabilize APIs before 1.0.
- All meaningful icons/actions need localized labels and semantic roles/state. Decorative arrows must not enter focus or intercept gestures. Assert actual hit targets, font scaling, and modal focus behavior.
- Honor reduced motion and visibility. Infinite scroll-arrow animation must stop or become static when policy requires it; static countdown still preserves timing/functional completion. Keep platform signal collection app-owned and inject only the effective UI policy.

The current header has a hardcoded English close default and 38 dp tool wrappers; Material may expand hit regions, so inspect actual bounds instead of assuming accessibility from visual size. Resolve overlapping targets in dense headers. The text field collector must use current callbacks, and header content-slot removal/replacement needs regression tests before library release.

## 4. Platform and resource design

Prefer existing common Compose APIs. Use expect/actual only for genuine platform behavior: stable immersive insets, haptics where common feedback cannot preserve semantics, and the existing Android orientation policy. Keep those seams internal; preserve the two existing public fullscreen modifier APIs.

Android actuals may use `LocalView`/platform haptic constants and `LocalConfiguration`. iOS actuals must use appropriate safe-area and feedback behavior; a missing physical haptic on a simulator is not a failure, but requires device validation for a claimed device contract. Header sizing should use available layout constraints on iOS, not treat every landscape tablet/window as a phone. Do not mutate system bars, orientation, theme, or locale from the library.

Move the existing six string keys and English/French/Arabic/Spanish translations from Android `res/values*` to `commonMain/composeResources/values*`; use `org.jetbrains.compose.resources.stringResource`. Give `Res` an explicit unique namespace and keep it internal. Translate the header close default rather than preserving English as the only default. Preserve Android caller-supplied labels and normal locale behavior, including any app-specific locale selection.

Enable Android resource processing in the Android-KMP target (`androidResources.enable = true`) as required by CMP's resource integration. Validate generated AAR assets, manifest, resource lookup, transitive dependency packaging, and iOS bundle resources from a published artifact. Ordinary Android consumers must work without applying CMP merely to reconstruct resource files.

ECHO's [EchoFonts.kt:25](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/theme/EchoFonts.kt#L25) records a manual copy of Compose font assets into androidApp because the shared-module copy task was not wired under its current combination. Its shared Gradle Android target does not enable Android resources. This is a concrete packaging risk, not proof that packaged libraries cannot work. Investigate the supported resource configuration in the spike; do not duplicate that workaround in simt-ui or change ECHO's unrelated font packaging in this planning task.

No fonts need to ship for the eight-file extraction. A later optional Cosmos theme may bundle fonts with SIL OFL notices, real weights, Arabic coverage, and size/loading tests. Do not copy ECHO's brand, font assets, global ripple field, or application theme wholesale.

## 5. Cosmos specification module and authority

The canonical Cosmos system currently belongs to `simt-space`, not app-ui or either application. The inspected source revision is `637edcf633b4e7d6407b4f9a715dad359c41fce8`, with declared Cosmos `2.3.0`, updated `2026-10-03`. Record both commit and content checksums: the working tree may evolve without a version bump, so a version label alone is not enough provenance.

Primary authored inputs are `cosmos/design/specification.js`, `tokens.js`, `motion.js`, `components.js`, and the starfield/contrast sources. The website generates Markdown, JSON, schemas, and CSS into `dist`; its stylesheet and site registry are not the specification module. `specification.js` currently imports the site's origin/subcontext descriptor, so moving the source graph is not a simple directory copy.

**Initial authority decision:** retain simt-space as the normative authoring/publishing owner; make `:cosmos-spec` a reproducible, licensed distribution of pinned exports, including all platform parts for future modules. Prefer this to two separately edited specifications. A future move of canonical authoring to simt-ui would require an explicit cross-repo ownership decision, removing site-envelope coupling, and making simt-space consume pinned releases; it is outside the initial app-ui extraction.

Snapshot process:

1. Select a reviewed simt-space commit/release and run its actual `npm run check`/verification commands in an appropriate isolated checkout.
2. Export only the resource allowlist into `cosmos-spec/snapshots/<version>`. Full-site `dist`, landing content, deployment files, analytics, and operational material are excluded.
3. Write provenance: upstream identity, commit, declared version/date, export tool version, file checksums, code/document licenses, and preserved notices. Do not record personal filesystem paths.
4. Verify schemas, stable rule IDs, required platform payloads, and checksum consistency offline. Snapshot updates are explicit reviewable changes; no automatic refresh during a consumer build.
5. Keep specification version separate from UI artifact SemVer. State which release each implementation adopts and any measured deviations; do not label generic extracted primitives fully Cosmos-conformant by default.

Do not use simt-app's Android guide or ECHO's adoption freeze as canonical inputs. They are dated implementation readings, and ECHO's Luminous layer adds product-specific decisions. Preserve Cosmos' Android shape/easing mapping without inventing an officially specified iOS adapter: the current specification lists web, Android/mobile, and watch parts. Propose any iOS-specific normative mapping upstream before claiming conformance; shared mechanical Compose rendering does not itself establish design conformity.

Evidence: [canonical source declaration](../../../simt-space/README.md#L54), [release](../../../simt-space/cosmos/design/specification.js#L241), [export resource registry](../../../simt-space/products/cosmos/design/product.js#L78), [rule/version governance](../../../simt-space/README.md#L305), [Android snapshot authority](../../../simt-app/docs/design/COSMOS_REDESIGN.md#L43), [ECHO freeze provenance](../../../simt-echo/docs/research/cosmos-echo-freeze.md#L3).

### 5.1 Later reusable Cosmos implementation candidates

These are selective follow-up work, not permission to move `:core:ui` or ECHO's UI directory:

- Canopy surfaces exist in [SIMT CanopyGlass.kt](../../../simt-app/core/ui/src/main/java/app/simt/core/ui/components/CanopyGlass.kt#L27) and [ECHO CanopyGlass.kt](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/atmosphere/CanopyGlass.kt#L41), but gradient borders versus line/top-light variants differ. Port the approved spec contract, not whichever copy is closest.
- Clearable fields overlap [ECHO CosmosFields.kt](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/components/CosmosFields.kt#L39) and existing app-ui, but ECHO adds focus gold, theme tokens, and a hardcoded Clear label. Reuse a generic primitive first; keep branded policy layered separately.
- [ECHO CosmosMotion.kt](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/atmosphere/CosmosMotion.kt#L58) has a portable pure motion-tier resolver. Reconcile against the pinned spec and tests; do not import platform signal collectors or product ambient services.
- Background/starfield geometry can share deterministic algorithms and parity fixtures later; keep device sensors, screen visibility, app theme state, and power policy outside the renderer.
- [ECHO CosmosButtons.kt](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/components/CosmosButtons.kt#L44) is coupled to `LocalEchoRipples` and ECHO extras. Separate presentation from the product's emitted ripple field before considering extraction.
- [SIMT AppScreen.kt](../../../simt-app/core/ui/src/main/java/app/simt/core/ui/layout/AppScreen.kt#L38) exposes Android resource IDs and app action/background policy; [ECHO CosmosModals.kt](../../../simt-echo/shared/src/commonMain/kotlin/app/simt/echo/ui/components/CosmosModals.kt#L43) owns a different modal wrapper. Keep both app-facing wrappers and modal ownership checks; do not replace them with a raw shared dialog bypass.

## 6. Distribution and consumer wiring: the GNSS model

Match GNSS's user experience: standalone build, no private repository authentication to consume releases, Maven Central coordinates, a pinned version-catalog alias, and automatic sibling composite substitution for local development. Do not copy GNSS's JVM/Android publication internals into a KMP module.

Proposed consumer declarations:

```toml
[versions]
simtUi = "0.1.0"

[libraries]
simt-app-ui = { module = "app.simt:app-ui", version.ref = "simtUi" }
```

```kotlin
// simt-app/core/ui/build.gradle.kts
api(libs.simt.app.ui)

// simt-echo/shared/build.gradle.kts, commonMain.dependencies
implementation(libs.simt.app.ui)
```

These versions/coordinates are proposals, not currently published dependencies. Retain simt-app's `api` visibility because feature imports rely on it. ECHO should adopt one bounded generic component first; do not migrate its visual language merely because a dependency exists.

In settings, mirror the existing GNSS sibling/worktree lookup, requiring the candidate directory to contain `settings.gradle.kts`, then explicitly substitute `module("app.simt:app-ui")` with `project(":app-ui")` inside `includeBuild`. ECHO gets equivalent standard/worktree lookup appropriate to its actual checkout layout. Prove Gradle's common/Android/iOS variant selection; inspect generated platform coordinates and add targeted substitutions only if the resolved graph actually requires them. Avoid hardcoding guessed KMP publication suffixes.

Release validation uses independent consumers without any composite inclusion and an isolated filesystem Maven repository. This is necessary because a composite can mask missing publication dependencies, root metadata, platform artifacts, or resources. It also avoids stale Maven Local artifacts producing misleading passes.

Publication contract:

- Apply `maven-publish` and use KMP-generated root and target publications. Publish all advertised targets and `.module` metadata, sources, approved POM metadata, and generated API documentation.
- Android AAR, `iosArm64`, and `iosSimulatorArm64` klibs must be available through root metadata. ECHO links them into its existing `Shared` framework; no additional app framework, CocoaPods setup, or SwiftUI rewrite is required for Kotlin consumers.
- Pure Swift/SwiftUI consumption is not promised by a Compose Kotlin library. A native host/controller wrapper would be a separate integration deliverable.
- Do not export a forced Compose BOM or an `enforcedPlatform` to all clients. Use the CMP dependency set and verify downstream selection and declared minimums.
- Model version/tag handling, Central aggregation, and in-memory signing on GNSS. Credentials come only from release environment/properties; ordinary local builds must not require them. Configure SCM/project URLs only after the actual new remote exists.
- Add API/ABI checks, real documentation, local-publication smoke tests, and a validation dependency before release upload. GNSS's current release workflow does not explicitly run its tests in the publish job; do not inherit that gap.
- Publish the spec ZIP as a separately versioned documentation release asset initially. Do not add it to mobile runtime dependencies or pretend it is a KMP binary; Maven ZIP publication can follow a real consumer need.

Evidence: [GNSS standalone settings and Central upload](../../../simt-gnss/settings.gradle.kts#L9), [GNSS shared publication metadata](../../../simt-gnss/build.gradle.kts#L38), [GNSS consumption documentation](../../../simt-gnss/README.md#L62), [SIMT GNSS substitution](../../../simt-app/settings.gradle.kts#L36), [GNSS publish workflow](../../../simt-gnss/.github/workflows/publish.yml#L36).

## 7. Open-source safety and licensing gates

**No critical data was identified by the bounded allowlisted-export review; this is not public security clearance.** Later current/all-ref source and exact publication pattern/content audits are recorded below. Pattern-based checks cannot guarantee the absence of every secret or legal restriction; owner review remains mandatory.

The initial investigation found no project-level code license in app-ui's inspected tracked inventory, nor a clear redistribution license for the Cosmos source/specification in the inspected simt-space files. The 2026-11-17 owner-confirmed Apache-2.0/CC BY 4.0 grants and pinned notices now resolve that license gap; final release approval remains separate. A license declared in simt-app's existing publication POM is not proof that every submodule/document is covered. An npm dependency's license and a font OFL notice are not a code/spec license.

Required before public export:

1. Confirm code, documentation, contributor, copied-source, font, and branding rights. Recommend Apache-2.0 for code to match GNSS, subject to owner approval; choose and record an explicit documentation/spec license. Preserve required copyright/NOTICE/OFL text. Do not infer a license from public accessibility or repository ownership.
2. Use a clean-history, tracked-file allowlist export with approved provenance. Do not recursively copy a working tree, `.git`, build outputs, or a parent repository/history. Importing only the old app-ui history is possible after a complete relevant-ref/blob review, but is not the default.
3. Exclude local properties, environment files, signing/upload keys, service configuration, private API endpoints/tokens, account/device identifiers, locations/routes, logs, measurement footage, submission/roadmap material, personal paths, IDE caches, generated agent state, and private app/tooling configuration. Do not carry app manifests or app release/signing Gradle blocks into the library.
4. Review current export contents and any imported history with a local value-redacting secret scanner and manual review. Scan new commits and the exact source JARs, AAR assets/manifest, klibs, spec ZIP, examples, and documentation, not just filenames. Any confirmed leaked credential needs owner-led rotation; deleting it from this export does not repair the original leak.
5. Use synthetic samples, offline functionality, no analytics or startup network activity, no requested permissions, and no user-data logging. City/country component inputs remain caller-owned and unpersisted.
6. Review dependencies/licenses and lock/verify release inputs where supported. Release permissions and credentials are least-privilege and unavailable to untrusted pull-request code; CI must not print secret properties.
7. Review this planning document too before public release: local source links are an investigation ledger, not a permanent public library contract. Replace private/unavailable references with approved provenance and public documentation without copying their internal contents.

Wakt uses the old app-ui submodule and is not an authorized migration target here. Its local submodule is uninitialized at a different pin; do not remove/rename the old upstream, rewrite history, or assume its compatibility. Preserve its existing pin and schedule a separate migration if requested.

## 8. Implementation slices and acceptance gates

Implementation proceeds in validated, committed chunks. UI-01 is split into build infrastructure (UI-01a), portable component/resource behavior (UI-01b), and independent published-artifact consumers (UI-01c). App integration and public artifact publication remain gated; no source license is assumed.

| id | lane | depends | title | state | evidence |
|---|---|---|---|---|---|
| UI-00 | simt-ui / rights owners | - | Approve export manifest, code/spec licenses, provenance, and public-data boundaries | Rights approved; final release audit pending | Said Elimam confirms code/document rights on 2026-11-17; Apache-2.0 / CC BY 4.0 notices retained; bounded source/artifact review does not replace final owner approval |
| UI-01 | simt-ui | - | Independent KMP/resource/API compatibility spike plus two published-artifact consumer fixtures | Runtime gates pending | All eight primitives compile; independent Android/CMP published builds and both Native frameworks link; iOS launch and physical API 28 remain unverified |
| UI-01a | simt-ui | - | Standalone wrapper/catalog, KMP Android/iOS targets, resource wiring, and local publication | Complete | `ca05f60` — commands below passed |
| UI-01b | simt-ui | UI-01a | Portable primitives/resources and behavioral tests | Complete | All eight primitives and Android/iOS seams compile; 28 host and 13 emulator tests pass including the header transition regression |
| UI-01c | simt-ui | UI-01b | Independent ordinary Android and CMP published-artifact consumers | Local builds complete; runtime gates pending | Android 18 host / 9 emulator tests, CMP 8 host / 8 emulator tests, unsigned R8 release, and both Native frameworks pass; no iOS UI or physical API 28 claim |
| UI-02 | simt-ui | UI-00, UI-01 | Extract eight primitives, resource locales, platform seams, API baseline, and behavior tests | Private implementation complete; public/runtime blocked | Eight primitives/resources/platform seams, 28 host / 13 emulator tests, explicit API and checked Android/both Native ABI baselines pass; iOS runtime, physical API 28 and final release approval remain external gates |
| UI-03 | simt-ui / simt-space | UI-00 | Pin/export Cosmos specs with offline schema/checksum and authority verification | Licensed canonical snapshot complete | Upstream pin 86b36f9; 33 byte-identical allowlisted sources/exports/notices, 52 offline tests, every canonical schema and deterministic licensed ZIP pass; no iOS adapter or release approval inferred |
| UI-04 | simt-ui | UI-02, UI-03 | API docs, owner-gated unsigned local aggregation tooling, artifact scan, and release validation workflow | Licensed private staging complete; actual release/signing blocked | Generated HTML, checked ABI, 111 audit regressions, current/all-ref source and all four licensed publications pass; private integration stage carries licenses/notices; final release still rejects absent runtime/owner/artifact approval and stable version |
| UI-05 | simt-app | UI-04 | Replace core-ui API dependency, add sibling substitution, retire app-ui gitlink/config safely | Blocked; not implemented | Actual producer staging refuses rights/runtime/owner/release gates; existing API dependency, submodule pin and host configuration preserved |
| UI-06 | simt-echo | UI-04 | Add common dependency/substitution and adopt one bounded generic primitive | Blocked; not implemented | Same producer gates plus actual Android/iOS adoption evidence required; bounded candidate confirmed, no host dependency or UI changed |
| UI-07 | simt-ui / owners | UI-05, UI-06 | Final public-source/artifact/license review and first Central release | Blocked; not published | Complete runtime/security evidence, host migrations, stable release/tag and final explicit owner/artifact approval; retain confirmed licenses and strict Cosmos validation |

### UI-01: go/no-go compatibility spike

Use minimal synthetic fixtures and reviewed original code only within authorized rights; do not publicly distribute experimental copied code before UI-00. Confirm declared version availability and effective build/plugin versions. Register/inspect actual Gradle tasks rather than assuming Android-KMP task names from conventional Android libraries.

The spike must exercise a field using CMP resources, the experimental tooltip signature, the fade renderer, and the platform inset/haptic seams. Publish all candidate artifacts to an isolated local Maven repository, then build an ordinary Android Compose consumer and a KMP consumer from that repository with composite substitution absent.

Record: selected Compose/Material/icons/resources versions and reasons; root and target metadata; no duplicate Android Compose classes; Android debug/release build and resources; iOS device and simulator linking; simulator resource/UI launch; actual Android API 28 operation; supported Xcode/runtime; configuration and build times. Prove the single Android producer variant serves both consumer build types.

Go only if no host architectural migration or manual asset copying is needed and experimental APIs compile/link for every advertised target. If narrow version alignment is necessary, document its exact impact and get approval before changing host catalogs/wrappers. Do not hide failures with forced versions, disabled checks, no-op actuals, or an Android-only dependency in commonMain.

### UI-02: behavior and library-quality gates

Original proposed coverage areas: list/scroll, both forms, tooltip/header, countdown, fullscreen insets and resource locale/packaging tests. Implemented runner grouping and executed evidence are recorded below rather than prescribing one file per area. Match test runner/source set to behavior; pure tests cannot prove hit targets, GPU rendering, native keyboard behavior, or bundle packaging.

| Area | Required coverage |
|---|---|
| List/scroll | Empty and populated lists; keys/reorder remembered state; reverse layout; vertical/horizontal; RTL; fade off; no-overflow layer; transparent/gradient backdrop; decorative arrows ignore input/focus |
| Forms | Editing, clear once, truncation contract, country uppercase, read-only clicks, callback replacement, IME/composition, caller labels, all four locales, unsupported locale default |
| Header | Portrait/landscape and bounded windows; long text/font scale; disabled tools; non-overlapping targets; current actions; slot replacement/removal/re-entry during animation; modifier/outer scope behavior |
| Tooltip/countdown | Long press/click competition; keyboard/focus/dismiss; reduced/static animation; deterministic test-clock timing; cancellation; restart policy; callback freshness/context; haptic mapping |
| Platform | Hidden bars/cutout on Android; iOS notch/home indicator; consumed insets; keyboard overlap; foreground/background and visibility; touch and screen-reader checks |
| Publication | Clean Android-only and CMP fixtures; packaged strings and later fonts; root/platform metadata; selected variants; debug/release and Native linking; no private classes/config/assets |

Run focused Android host/device tests and iOS tests through the new wrapper after discovering its real task names with `./gradlew tasks --all`. Use common Compose test APIs where supported, with Android instrumentation and iOS host/UI tests for platform-specific behavior. Document any experimental test APIs. Existing ECHO UI tests provide useful assertions, not portable runner infrastructure to copy wholesale.

### UI-05/UI-06: producer-first migration and rollback

Before source replacement, use the audited private `stageIntegration` artifact, or a separately approved released artifact. Private staging does not require completed host integration and is not public redistribution/runtime approval; final `releaseCheck`/`stageRelease` retain every owner, rights, runtime, integration and strict Cosmos gate. simt-app changes `api(project(":app-ui"))` to the catalog dependency, adds the composite mapping, and removes `include(":app-ui")`, its gitlink entry, `.gitmodules` entry, and the root Spotless exception only after confirming no remaining module/config references. Submodule removal is a deliberate reviewed operation; never delete `.git/modules/app-ui` or discard local submodule work as cleanup.

Keep existing imports and app-level wrappers. Run `:core:ui` tests, the measure countdown path, compass immersive/header/tooltip paths, camera overlays, target forms/lists, and representative help/billing screens; then assemble app debug/release as the cross-system phase gate. Preserve current rendering and modal policy unless an explicit accessibility correction is covered by tests.

For ECHO, add the dependency to `commonMain`, retain the existing application/theme/route/brand structure, and replace only a genuinely equivalent primitive. Link its `Shared` frameworks for both Apple targets and launch the adopted component in Android and iOS hosts. Do not claim host Robolectric tests prove the iOS UI.

Verify composite behavior with the sibling present and published behavior from isolated checkout(s) without it. Avoid moving/deleting the user's sibling directory to simulate absence. Record the exact discovered consumer tasks and tests in the tracker. The implementation repositories' current runners include `./gradlew :core:ui:testDebugUnitTest` and ECHO's `./gradlew :shared:testAndroidHostTest`; these were not executed for this plan.

Rollback uses a normal new change reverting consumer wiring to its previous dependency/pin, not reset/history rewriting. Keep the old submodule upstream/pin available while migration is reviewed, and avoid depending on an unpublished release from a merged consumer change.

## 9. Investigation result and remaining unknowns

Completed: source/build-boundary inspection; eight-file portability assessment; direct/transitive consumer discovery; GNSS publication/composite comparison; Cosmos authority/export assessment; bounded local safety/provenance review; official documentation review; proposed module/API/test/release design; creation of this plan.

At the end of the original investigation, no build/test, source extraction, Git initialization, remote synchronization, or publication had run. Subsequent implementation evidence is recorded below. Independent Native framework linking now passes. iOS UI launch, cloud security scanning, comprehensive history/content secret scanning, legal clearance, production consumer migration, and remote artifact publication remain unexecuted. Existing dirty files in the source repositories were left untouched. Source paths and line links reflect the inspected local working trees, not guaranteed contents of future public revisions.

Remaining release blockers: confirmed code/spec rights and release documents; supported-range Native runtime validation, iOS resource/UI/IME/accessibility and physical API 28/haptic evidence; producer-gated production host migrations; stable version/tag and explicit HEAD-tied owner/security approval; strict upstream Cosmos conformance. Common Material APIs compile, packaged Android resources/R8 and Native frameworks pass, and bounded current/history/artifact audits now run, without implying comprehensive public clearance.

The shared `:app-ui` implementation is locally viable without converting simt-app to KMP or forcing host toolchain/theme changes. Complete the external rights/runtime/release gates before production migration or public distribution; do not fall back to duplicated Android/common implementations merely because those external approvals are pending.

### Implementation evidence: UI-01a (2026-11-17)

- Initial plan committed as `7eddd39` and pushed to the requested empty private GitHub repository. Subsequent implementation commits are local unless separately approved for push.
- Build pins: Kotlin `2.4.21`, CMP `1.12.2`, AGP `9.3.1`, checksum-verified Gradle `9.7.0`; toolchain JDK 21, Android bytecode JVM 11, API 28 floor, SDK 37. Existing application builds/catalogs are unchanged.
- Resolved CMP defaults replaced by explicit catalog coordinates: Material3 `1.9.0` maps to AndroidX `1.4.0`; extended icons `1.7.3` maps to AndroidX `1.7.6` before any consumer BOM alignment. Base Compose maps to AndroidX `1.12.1`.
- Xcode `27.1` is installed but not globally selected; commands use `DEVELOPER_DIR` without changing system settings. This exceeds the documented KGP table's Xcode version, so successful klib compilation is not a claim of officially supported Native linking/runtime.
- Passed through the new wrapper: `tasks --all`, `wrapper`, `spotlessApply`, `spotlessCheck`, `:app-ui:assembleAndroidMain`, `:app-ui:compileKotlinIosArm64`, `:app-ui:compileKotlinIosSimulatorArm64`, `:app-ui:publishAllPublicationsToLocalValidationRepository`, and `:cosmos-spec:check`.
- Locally generated root/Android/two iOS publications have metadata and matching artifact checksums. Output stays under ignored `build/maven-repository`; no Maven Local or remote artifact upload occurred.
- This chunk contains build/configuration only; compilation covers generated resource scaffolding, not extracted components. No behavioral test exists or is claimed yet; the docs-only module has no snapshot to verify. Bounded source-pattern checks passed, not a comprehensive secret audit.
- Root Spotless excludes generated wrapper properties after Gradle detected an implicit producer/formatter task dependency; wrapper generation and format checks now pass separately.

### Implementation evidence: UI-01b first forms slice (2026-11-17)

- Allowlisted `ClearableTextField` and `CityCountryFields` retain their existing packages/signatures and length, uppercase, and read-only clearing behavior. Compose resources replace Android resource IDs; `rememberUpdatedState` keeps the body-click callback current after recomposition.
- Source provenance records the original repository revision and SHA-256 hashes for the two Kotlin files and four resource files. All six hashes match the source; all 24 translated values are preserved with `simt_ui_` names. No source history or application configuration was imported, and redistribution rights remain unconfirmed.
- Eight Robolectric Android host tests pass: explicit/default/unlimited lengths, clear visibility and exactly-once callbacks, read-only keyboard rejection and clearing, callback replacement, city/country constraints, and default resource resolution. Native graphics and separated tap timing avoid an unintended double-tap magnifier gesture in the callback test.
- Three Android 16 emulator tests pass: 48 dp clear touch bounds with a real tap near the expanded left edge followed by editing, packaged default strings with read-only clearing, and city/country input normalization. Touch bounds are checked rather than the smaller visible icon-button container; no production size change was necessary.
- Test-only Espresso `3.7.0` replaces the older transitive input driver that cannot inject input on Android 16. This does not change library runtime or application dependencies. Host and device tests use the Compose JUnit v2 rule; host tests need the JVM export for Robolectric's Android input implementation.
- Passed command: `./gradlew spotlessApply :app-ui:testAndroidHostTest --tests app.shared.ui.components.forms.FormsTest :app-ui:connectedAndroidDeviceTest :app-ui:compileKotlinIosArm64 :app-ui:compileKotlinIosSimulatorArm64 :app-ui:publishAllPublicationsToLocalValidationRepository check --console=plain`, using local Android SDK and per-command Xcode selection.
- Latest root/Android/two iOS publication metadata references artifacts with matching SHA-256 checksums. The AAR and both native resource-publication ZIPs each contain all four locale payloads; the AAR requests no permissions. These are filesystem-only publications under ignored build output.
- The initial native artifacts embedded personal build paths in klib IR strings. Both iOS targets now pass the supported `-Xklib-relative-path-base` compiler option using the repository root; rebuilt latest publications contain no matching personal paths. A bounded credential/private-key/personal-path pattern check inspected 127 nested artifact entries and 12 source/config/provenance/plan files with no findings; this is not comprehensive release security clearance.
- An independent read-only review approved the bounded forms slice with residual runtime/accessibility test breadth limitations; it verified the expanded-edge tap injects actual input without coordinate clamping.
- iOS targets compile as klibs, but no iOS simulator runtime is installed and no framework linking/UI launch is claimed. Android API 28, IME composition, translated runtime selection/unsupported-locale fallback, large text/RTL, independent consumer fixtures, remaining primitives, API baselines, and comprehensive release artifact/security/license review remain separate gates. Root `check` skips Native tests with no test sources; its success is not Native UI evidence.

### Implementation evidence: UI-01b list/scroll slice (2026-11-17)

- Both `WindowedList` overloads and `ScrollEdgeEffect` preserve source signatures, keyed row identity, reverse-layout/RTL mapping, caller layout scopes, and alpha-mask rendering. Android preview code is excluded from common runtime; animation is an explicit implementation dependency.
- `LocalUiMotionEnabled` is caller-owned effective motion/visibility policy. Disabling it cancels infinite arrow motion and renders edge/arrow visibility immediately; default behavior remains animated. Zero-width and inactive fades do not allocate an offscreen mask.
- Five host tests cover empty/populated lists, shared scrolling state, remembered state following stable keys on reorder, reverse layout, and actual taps through decorative arrows at both edges.
- Seven Android emulator render tests measure red content dissolving over a blue parent via `DstIn`, vertical forward/backward edges, horizontal LTR/RTL, reversed lists, disabled/zero fades, and frame-stable arrows with immediate visibility when motion is disabled. This is pixel/input evidence on Android, not iOS rendering evidence.
- Original source hashes are added to the existing restricted provenance ledger and verify against upstream. The full regression run passes 13 host and 10 emulator tests, Android and both iOS compilation, formatting, local publications, metadata artifact hashes, and bounded credential/private-key/personal-path archive checks. An interrupted shared-daemon invocation was diagnosed and the final successful run used `--no-daemon --max-workers=2`; no checks were disabled.

### Implementation evidence: UI-01b overlays/countdown/platform slice (2026-11-17)

- Remaining four original source files are exported with matching provenance hashes. Header/tooltip/countdown/fullscreen modifier signatures and countdown one-shot/background-callback scheduling are preserved; finishing uses the current callback and removal cancels structured work.
- Android actuals preserve landscape header width, ignoring-visibility status/system-bar unions with cutouts, and `CLOCK_TICK`/`LONG_PRESS` feedback. iOS actuals fill the caller's bounded width, use safe top/system bottom insets separately from IME, and real UIKit light/medium impact generators. The IO dispatcher is a narrow internal platform seam because Native's IO accessor is not in common metadata.
- Header close defaults resolve the existing four-locale resource; a French host test verifies actual translated selection. Retained cards replace/remove/reenter correctly through exit; motion-disabled cards release immediately. Disabled labeled tools cannot invoke a wrapper callback; each tool has one click owner, selected semantics, and nonoverlapping 48 dp layout/touch targets. Glyphs remain 20 dp.
- Fourteen additional host tests cover exact countdown/haptic order and timing, current policies/callbacks, no restart, IO thread and cancellation, tooltip tap/long-press/keyboard ownership, header slots/targets/translation/width, hidden bars/cutout unions, IME independence, correct inset sides, and nested consumption. Three additional device tests exercise tool edge taps/disabled long presses and opaque frame-stable reduced-motion countdown rendering.
- Full regression passes 27 host and 13 emulator tests, Android/both iOS compilation, formatting, filesystem publications, matching metadata hashes, and bounded credential/private-key/personal-path archive checks. iOS intermediate metadata reports duplicate lifecycle/navigation/savedstate common-library-name warnings from transitive AndroidX/JetBrains artifacts; independent Native framework linking in UI-01c must investigate these before release compatibility is claimed. No iOS device/runtime test is claimed.

### Implementation evidence: header transition regression (2026-11-17)

- `93e1d95` keeps the card transition host composed while its content is absent. A clock/height regression reproduced skipped expansion on first entry and after a completed exit, then passed with the fix; retained content still releases and reentry cancels pending removal.
- All seven focused header tests and both Native compiles pass. The subsequent full run passes 28 host and 13 emulator tests, complete root formatting, and refreshed filesystem publications.
- The exact latest snapshot metadata selects 21 files across four publications; declared artifact SHA-256 hashes and sizes match. A bounded, value-redacting credential/private-key/personal-path scan inspected 409 source and nested publication entries with no findings. Maven's compound `kotlin_resources.zip` extension is resolved from metadata rather than guessed filenames. This is not comprehensive security or licensing clearance.

### Implementation evidence: UI-01c independent consumers (2026-11-17)

- Separate `examples/android-consumer` and `examples/multiplatform-consumer` builds resolve `app.simt` exclusively from the producer's filesystem Maven repository. No `includeBuild`, project substitution, Maven Local, host build logic, asset copying, or producer-source access is used. Both pin the real host toolchain (AGP `9.5.1`, Kotlin `2.4.21`, Gradle `9.9.1`); the ordinary Android fixture does not apply CMP and uses BOM `2026.09.00`.
- Fresh dependency resolution against the publication containing `93e1d95` passes. Ordinary Android passes 18 NATIVE Robolectric checks across API 28/36 and nine Android 16 emulator checks. CMP passes eight host and eight emulator checks. Four locales, unsupported-locale English fallback, editing/truncation/clear/close, list selection, actual horizontal overflow/scrolling, tooltip click/long-press competition, and both default countdown ticks are exercised.
- Android debug and unsigned minified release assemble with default R8 rules; `verifyPackagedLocales` decodes and asserts all four packaged clear/close translations in both APKs. Debug/test manifests request no permissions. Device commands ran sequentially on the shared emulator.
- `compileIosMainKotlinMetadata`, `linkDebugFrameworkIosArm64`, and `linkDebugFrameworkIosSimulatorArm64` pass. Both generated binaries have static archive headers and export `MainViewController`. Ten duplicate common-library-name warnings occur in intermediate metadata; actual target linking succeeds without forced versions or exclusions. No supported-range, Native runtime, bundle launch, physical haptic, screen-reader, or iOS visual claim follows from this.
- Independent source/wrapper review found no secrets, personal paths, network/logging behavior, permissions, or manual resources. Verified wrapper JAR/distribution checksums match Gradle `9.9.1`. Review exposed an unattached horizontal state and test-only policy coverage; the final fixture attaches a real horizontal scroller and adds default motion/haptic countdown execution with clock assertions. Insets are invoked, not geometrically measured here; producer tests cover Android geometry.
- Reproduction commands from the producer root, after local publication (supply the local Android SDK, JDK 21 and per-command Xcode selection through environment):
  - `examples/android-consumer/gradlew -p examples/android-consumer --refresh-dependencies testDebugUnitTest --tests app.simt.fixture.ConsumerHostTest verifyPackagedLocales connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.simt.fixture.ConsumerDeviceTest --no-daemon --max-workers=2`
  - `examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --refresh-dependencies testAndroidHostTest --tests app.simt.fixture.ConsumerHostTest compileIosMainKotlinMetadata linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64 --no-daemon --max-workers=2`
  - `examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=app.simt.fixture.ConsumerDeviceTest --no-daemon --max-workers=2`
- An installed API 28 physical/emulated device and an installed iOS simulator runtime remain missing. API 28 evidence is host-only; Xcode `27.1` remains above the documented Kotlin compatibility table. Production hosts remain unchanged pending release/rights/runtime gates.

### Implementation evidence: UI-03 private Cosmos snapshot (2026-11-17)

- The frozen `637edcf` source pin contributes exactly seven inert authored JavaScript files and 23 canonical exports to Cosmos `2.3.0`. All 30 copies match their upstream bytes and ledger SHA-256 hashes; the generator script hash also matches. No source graph is executed, site/assets/config/history are excluded, authority remains simt-space, and no normative iOS adapter is invented.
- Pinned Ajv `8.20.0` / ajv-formats `3.0.1` are bootstrapped explicitly with `npm --prefix cosmos-spec ci --ignore-scripts --no-audit --no-fund`. Gradle checks never install dependencies or retrieve schemas. The checked-in lockfile pins dependency integrity; all schema IDs are resolved from the snapshot offline.
- `./gradlew --offline :cosmos-spec:check :cosmos-spec:privateSnapshotZip spotlessCheck --no-daemon --max-workers=2` passes 44 tests: inventory/hash/symlink rejection, rehashed semantic tampering, exact global/scoped rule inheritance/counts, token adapter subsets, recomputed Bezier parity, contrast references/eligibility, component rule/motion links, offline missing-schema rejection, and actual deterministic archive repetition.
- The upstream union rule schema requires 36 sections, but its four scoped payloads contain 16/17. Both `npm --prefix cosmos-spec run check:canonical-schema` and `:cosmos-spec:checkCanonicalSchema` deliberately fail on these exact four errors. Private integrity accepts only the recorded unchanged defects, not arbitrary validation errors; instance-required properties remain enforced. Upstream schemas/bytes are neither repaired nor weakened, and successful private checks are not full conformance.
- The local private ZIP contains 31 sorted files with fixed timestamps/modes. Repeated SHA-256: `1bc4ae351fbbcf5bae939a81cb0d34ccafd43a6b739e98ab36ef83850d826663`. Independent byte/hash/archive review and a bounded value-redacting credential/private-key/personal-path scan found no matches in the ZIP. It stays in ignored build output.
- Frozen provenance records code/document licenses separately as unconfirmed and public redistribution as blocked. A private archive is not publication authorization or a legal clearance; owner review and upstream schema resolution remain release obligations.

### Implementation evidence: UI-02/UI-04 API, documentation and local gates (2026-11-17)

- KGP explicit API compilation adds only explicit visibility and existing constant/default types. Checked Android and combined `iosArm64`/`iosSimulatorArm64` ABI dumps are reviewed baselines; normal checks compare them and never refresh them silently. A read-only API/docs review found no contract or behavior change.
- Dokka `2.3.0` generates 36 real HTML pages with the module's field/list/scroll/tooltip/header/countdown/platform contracts. All local href/src targets resolve; the module and package pages load in the browser. Pointer/visual interaction was unavailable because the browser viewport was hidden, so no visual browser-test claim is made. The deterministic documentation JAR is attached to all four publications; no placeholder documentation or private resource class is exported.
- Full producer checks pass 28 host / 13 Android emulator tests, ABI, both Native compiles, formatting, fresh publications and Cosmos 44 offline checks. Refreshed independent consumers pass Android 18 host / 9 emulator checks, default R8 packaging/locales and CMP eight host / eight emulator checks plus both Native frameworks. Compilation/linking still does not prove iOS launch or physical API 28.
- `python3 -B -m unittest discover -s scripts -p test_check_publication.py` passes 80 fixtures for source/all-ref history, redaction, metadata/target redirects, compound resources and Kotlin tooling attachments, malformed input, archive traversal/symlinks/bounds, Android permissions, exact owner approval, stale evidence, immutable inputs and deterministic unsigned staging. Review repaired a test whose XML limit hid the intended archive limit, a real root tooling-metadata classifier omitted by the initial checker, and history deduplication that could skip a renamed identical manifest. Synthetic approved fixtures do not approve the real repository.
- Actual source/history audit passes with 440 scanned entries before this evidence update. Exact metadata-selected publications pass with 614 source/nested content entries, 29 selected metadata/artifact files and their SHA-256 sidecars, matching checksums/sizes, generated docs/sources/resources, approved dependency families and permission-free AARs. Stale unselected snapshots and ignored debug frameworks are excluded. These are bounded local audits, not a cloud scan or comprehensive release clearance.
- Root `check` includes audit tests and source/history review; `localQuality` adds formatting/docs/fresh publication verification. Actual Gradle `stageRelease` stops at `releaseCheck`, and standalone staging also refuses the current snapshot, unconfirmed provenance/rights, missing HEAD-tied runtime/integration/owner evidence, missing release documents and strict Cosmos failure. No candidate ZIP is created. No force/environment bypass, uploader, signing secrets, CI/permission changes or inferred licenses exist; signing is a separate owner requirement.
- `README.md` documents reproduction commands, independent consumer setup, public contracts, the complete approval/evidence shape, artifact inventory and remaining obligations. Private-development implementation/tooling is locally complete; signing/publication and producer-gated host integration are not performed or claimed.

### Completion assessment and blocked production migrations (2026-11-17)

- Locally feasible private extraction is complete through `194dfe3`: standalone shared Android/iOS UI, all eight primitives and platform seams, four resource locales, independent filesystem-published Android/CMP fixtures, pinned inert Cosmos exports, checked ABI, generated documentation and owner-gated local validation tooling. No Android-only duplicate module or speculative future modules were added.
- Post-commit `localQuality` passes 80 audit tests, 44 Cosmos tests, Android/both Native ABI checks, producer host checks, formatting and fresh four-target publications. Current/all-ref source audit scans 461 entries with no findings; publication audit scans 614 entries and validates 29 selected files plus SHA-256 sidecars. Counts describe this run, not permanent totals or comprehensive security clearance.
- Clean-tree standalone staging returns failure without a candidate ZIP. Actual blockers remain separate code/spec rights and required release documents, five missing HEAD-tied evidence records, owner approval, stable SemVer, and strict Cosmos validation. The strict command reproduces exactly the four unchanged scoped `minItems` failures; neither schemas nor gates are bypassed.
- `xcrun simctl list runtimes` reports no installed iOS runtime; the attached Android emulator is API 36. Existing API 28 checks are host-only. Supported-range iOS UI/resources/insets/IME/accessibility and physical Android floor/haptic evidence cannot be substituted with successful framework linking or Robolectric.
- UI-05 is deliberately unimplemented: simt-app remains clean with `api(project(":app-ui"))`, its settings inclusion, `.gitmodules` entry and `970b6fe` submodule pin intact. No gitlink retirement, app build/visual validation, guessed stable dependency, or composite-only replacement is claimed.
- UI-06 is deliberately unimplemented: ECHO remains clean without an `app.simt:app-ui` dependency. Direct inspection confirms `FilmStrip` at `shared/src/commonMain/kotlin/app/simt/echo/ui/screens/review/ReviewStoryUi.kt:166` and its actual horizontal scroller at lines 175–180; shared scroll overflow is a bounded future adoption candidate, not permission to replace product semantics/theme. ECHO's motion policy remains app-owned. No production Android/iOS adoption or runtime validation is claimed.
- Resume only after the relevant producer rights/runtime/strict-schema/owner gates permit a reviewed stage or release. Validate each host separately with and without sibling substitution, commit each bounded host chunk, then perform final public review and separately approved signing/upload. Local private fixtures do not replace those production obligations.
- The requested remote is configured; only the initial plan was pushed by this implementation session. Later implementation commits remain local unless externally synchronized. No additional push, public visibility change, remote artifact upload, unapproved license, credential, signing or CI permission configuration was performed.

### Authorized private staging follow-up (2026-11-17)

- The owner requests staging before consumer integration, Android IDE previews for every component, and a package rename from `app.shared.ui` to `app.simt.ui`, followed by remaining locally feasible work. These are separate validated committed chunks; no code/spec license or public upload approval is inferred.
- `stageIntegration` now uses producer quality plus current/history safety and exact publication integrity, not the final release approval gate. Its private-only deterministic ZIP contains the Maven snapshot selected by metadata and UI provenance/documentation, with HEAD/source-content fingerprints and checksum inventory. Existing outputs, symlink/traversal, changed source state and changed artifact bytes are rejected. Dirty local development is recorded rather than misrepresented as a clean release.
- Independent and production consumer rehearsals use the extracted `maven/` directory or an explicit local sibling mapping. Final release staging still requires stable clean/tagged HEAD, independently confirmed rights, genuine runtime/security/consumer evidence, strict canonical Cosmos, complete documents and exact owner approval. Private stage success is not permission to publish or a claim of production/iOS validation.
- `stageIntegration` and 89 audit tests pass. The actual private ZIP contains 61 checksummed Maven/document/provenance entries; independent Android host tests/R8/locales and CMP host tests/both Native frameworks pass after fresh resolution from its extracted `maven/` directory, with no mutable producer repository or composite mapping. The final release checker still refuses missing real approval/rights/runtime/integration/stable-version/strict-Cosmos gates.
- An unrelated untracked generated daemon-JVM configuration found at the start of this follow-up is preserved and excluded from commits; its local presence is represented by the private source fingerprint. Host migrations and preview/runtime evidence will be recorded only after actual validation.

### Approved licensing and upstream canonical repair (2026-11-17)

- Said Elimam explicitly confirmed control of the required UI/Cosmos code/document rights and selected Apache-2.0 for code plus CC BY 4.0 for Cosmos specifications/documentation. Complete license texts and scoped notices retain attribution, third-party exclusions and modification requirements; no unrelated site assets or trademark rights are granted. This is license permission, not final release/upload/runtime approval.
- Upstream local commits `940cd5f` (licenses/notices and two focused tests) and `86b36f9` (canonical scope fix and offline validator regressions) are safely fast-forwarded into simt-space main; neither is pushed or deployed. `npm run build && npm run check` on the committed, formatted worktree passes 75 generated files, 18 assets and all 21 tests (13 Cosmos license/schema plus eight SEO).
- The refreshed Cosmos 2.3.0 pin is `86b36f9e9b13e2426d54207b60beb7323fbcd5d6`. Its 23 exports, seven inert JavaScript sources and three upstream license/notice files are verified byte-identical against that worktree. All 33 files have pinned inventory hashes; generator SHA-256 and frozen provenance are deliberately reviewed and updated. Existing release version/date and normative content remain unchanged.
- All canonical schemas now pass. Exclusive schema branches preserve exactly 36 union sections and the correct 17/16 common-plus-platform sets, enforcing each section once and rejecting missing, duplicate, extra, foreign or substituted scopes. The former four-defect exception is removed; integrity requires zero errors and root Cosmos `check` includes the strict gate.
- `npm --prefix cosmos-spec run check` passes 52 offline regressions; `check:canonical-schema` exits zero. The deterministic local archive contains 34 entries including the full licenses/notices and provenance, repeated SHA-256 `53e98f8f433586b2bde34bf97bd00c1570028396468fb93f0aa4dee88ec691ac`. License filename coverage exposed and fixed a cross-language locale-sort mismatch without relaxing sorted/unique inventory checks.
- UI source provenance now records the dated confirmed Apache-2.0 grant while preserving all original source paths/hashes. Every Maven POM declares Apache-2.0 and all four publications attach the same deterministic four-file legal-notices ZIP. Its actual repeat-run SHA-256 is `11dc5484c41b3d2e9fd93e5fcbc31620ffaed880bdf56ed329739308b3482bb2`.
- `spotlessApply localQuality :cosmos-spec:privateSnapshotZip` passes, including 111 audit regressions, 52 Cosmos tests, unchanged Android/both Native ABI checks, existing producer host checks and fresh four-target licensed publications. This run's bounded source/history scan checks 474 entries; publication scan checks 634 entries and selects 66 metadata/artifact/sidecar files. The real licensed private stage contains 72 checksummed entries and explicitly denies final public/runtime approval; archive SHA-256 `67382c2dee269ce8b106cf45f8576ba5ed03d2d6a41bc1ff10cb3ad64d6498b3`. All staged entries are independently rehashed.
- The real final release command still refuses snapshot version, dirty tree, absent release changelog, five HEAD-tied evidence records and final owner/artifact approval. It no longer reports unconfirmed licenses or canonical-schema defects. No release candidate is created, signed or uploaded.
- Android IDE previews, the `app.simt.ui` package rename and production consumer migrations remain separate next chunks. No physical Android API 28, iOS runtime or final release approval is fabricated; the unrelated daemon-JVM configuration remains untouched.

## 10. External guidance consulted

Official documentation was consulted on 2026-11-17. Re-check it when implementing because supported toolchains and Android-KMP APIs change.

- [Compose Multiplatform compatibility and Android artifact mapping](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).
- [Kotlin Multiplatform toolchain compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).
- [Compose compiler/Kotlin alignment](https://kotlinlang.org/docs/multiplatform/compose-compiler.html).
- [Android-KMP library plugin, resources, tests, variants, and publishing](https://developer.android.com/kotlin/multiplatform/plugin).
- [KMP root/target publishing](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html).
- [Multiplatform resource setup/publication](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html) and [resource access/fonts](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html).
- [Compose component API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md) and [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting).
- [Kotlin library/API compatibility](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html) and [klib compatibility policy](https://kotlinlang.org/docs/kotlin-evolution-principles.html#kotlin-klib-binaries).
- [Compose Multiplatform UI testing](https://kotlinlang.org/docs/multiplatform/compose-test.html).
- [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html), [platform constraints](https://docs.gradle.org/current/userguide/platforms.html), and [Android Compose BOM](https://developer.android.com/develop/ui/compose/bom).
