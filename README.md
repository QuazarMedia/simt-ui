# SIMT UI

Shared Compose primitives for Android and Kotlin Multiplatform iOS consumers, plus an independently versioned Cosmos specification snapshot.

Version `0.1.0` is approved for a tag-triggered public release. Said Elimam has confirmed the rights and licensed UI/Cosmos code under Apache-2.0 and Cosmos specifications/documentation under CC BY 4.0; retain [LICENSE](LICENSE), [LICENSE-DOCS](LICENSE-DOCS) and [NOTICE](NOTICE). See [the extraction plan](docs/plans/0001-shared-ui-extraction.md) and the separate provenance ledgers in [app-ui](app-ui/source-provenance.json) and [cosmos-spec](cosmos-spec/provenance.json).

## Modules and boundaries

- `app-ui`: eight generic UI primitives in `app.simt.ui`, private Compose resources, and narrow Android/iOS inset, width, dispatcher and haptic implementations. Targets: Android API 28+, `iosArm64`, `iosSimulatorArm64`. No application framework, service configuration, analytics, persistence, requested permission or runtime network client.
- `cosmos-spec`: inert, byte-pinned Cosmos 2.3.0 sources and exports from simt-space. No dependency from the runtime library. The pin includes upstream licensing and the canonical union/platform scope fix; all declared schemas must pass.
- `examples/android-consumer`: separate ordinary Android Compose build, with no CMP plugin, using published filesystem Maven artifacts.
- `examples/multiplatform-consumer`: separate CMP build using the same published root coordinate and linking both Apple frameworks.

No theme, navigation, route, branding, host build convention or application configuration was extracted. Future modules can be added independently; no empty placeholder module is necessary. API contracts are in [the module documentation](app-ui/Module.md).

## Build locally

Use JDK 21, Android SDK 37 and the checked-in wrapper. Set `ANDROID_HOME` to the local SDK instead of committing `local.properties`. Native compilation/linking requires a supported Xcode installation; select it per command through `DEVELOPER_DIR`, not a global settings change. Node 22.20+ and Python 3 are required for the specification and audit tools.

```sh
npm --prefix cosmos-spec ci --ignore-scripts --no-audit --no-fund
./gradlew --no-daemon --max-workers=2 check spotlessCheck
./gradlew --no-daemon --max-workers=2 :app-ui:dokkaGeneratePublicationHtml
./gradlew --no-daemon --max-workers=2 :app-ui:publishAllPublicationsToLocalValidationRepository
```

The npm bootstrap is explicit and may download locked dependencies. Subsequent specification checks resolve schemas offline and never install dependencies or run the upstream generator. Gradle/dependency bootstrap may use the configured public repositories; this is separate from offline library runtime behavior.

Local Maven publications live in ignored `build/maven-repository`. All four publications carry metadata, sources and generated HTML documentation; native targets also carry resource ZIPs. Local tasks do not upload unless `publishAggregationToCentralPortal` is explicitly invoked with release credentials. ABI dumps in `app-ui/api` are reviewed source-controlled baselines: `:app-ui:checkKotlinAbi` checks them, while `:app-ui:updateKotlinAbi` is an explicit maintainer action, never an automatic repair during verification.

## Independent consumption rehearsal

After local publication, run these independent builds with JDK 21 and the Android SDK configured. Instrumentation commands require a running device and should run sequentially when sharing one device.

```sh
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 --refresh-dependencies testDebugUnitTest verifyPackagedLocales
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 connectedDebugAndroidTest
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 --refresh-dependencies testAndroidHostTest compileIosMainKotlinMetadata linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 connectedAndroidDeviceTest
```

Both examples exclusively resolve group `app.simt` from `../../build/maven-repository` by default. Override with `-PuiRepository=/absolute/path/to/repository` and `-PuiVersion=<actually-staged-version>`. They do not use `includeBuild`, Maven Local, project substitution, producer sources or manually copied library assets.

An Android consumer declares `implementation("app.simt:app-ui:<available-version>")`; a KMP consumer puts the same dependency in `commonMain.dependencies`. Keep a host's existing Compose/BOM and plugin policy unless real resolution proves a change necessary. The examples reproduce Android host pins without upgrading either production application. Version `0.1.0` is a locally audited stable candidate, not a remotely published dependency.

The compatible private producer stage remains `simt-ui-0.1.0-SNAPSHOT-43c285c-integration.zip`, SHA-256 `9583a2bc51a6889edca328facd7a73f6930ddaf2e45edffd87633c29e9270b6c`, with all 72 manifest entries independently rehashed. The reconciled simt-app adoption is locally landed on main at `2fe9aa5cb`, after prerequisite `3d41e6964`; ECHO is locally landed on main at `5443527`. Both staged-artifact and sibling-composite validation pass. No consumer or release commit is pushed by this work without separate authorization.

## Android IDE previews

Open `examples/android-consumer` as an **independent project** in Android Studio with JDK 21 and SDK 37. Set `uiRepository=/absolute/path/to/extracted/maven` and `uiVersion=0.1.0` in your local Gradle properties (or pass the equivalent `-P` options), using an actually staged artifact repository; do not substitute producer sources. Sync, select the debug variant, and open `src/debug/kotlin/app/simt/ui/preview/SharedUiPreviews.kt` in Design/Split. The Compose-enabled Android fixture hosts previews for all eight primitives and header tools; preview sources and tooling dependencies are debug-only, not producer library or release APK inputs.

Samples use synthetic values, a small Material theme, bounded viewports and disabled motion/haptics. Light/dark, French/RTL and large-text variants supplement the component samples. The tooltip's static image shows only its labeled anchor: use interactive preview and long press to reveal the popup. The countdown intentionally stays at 1. Insets use `showSystemUi=true`, but layoutlib may report zero insets; this is not proof of hidden-system-bar behavior. Host composition tests do not establish that the Android IDE Preview panel rendered, nor native/device runtime or haptic correctness.

## Private specification checks

```sh
./gradlew --offline --no-daemon --max-workers=2 :cosmos-spec:check :cosmos-spec:privateSnapshotZip
npm --prefix cosmos-spec run check:canonical-schema
```

The first command validates the exact licensed pin and 52 offline tests, and creates a deterministic local ZIP retaining the three upstream license/notice files. The strict second command passes: the committed upstream schema requires exactly 36 union sections and the correct 17/16 common-plus-platform sets, rejecting missing, duplicate, extra and foreign sections. All 33 reviewed files are byte-identical to upstream commit `86b36f9e9b13e2426d54207b60beb7323fbcd5d6`; no local canonical bytes are weakened or repaired. Cosmos remains authoritative upstream; this snapshot does not introduce a normative iOS adapter.

## Release automation

Pushing a stable `vMAJOR.MINOR.PATCH` tag runs `.github/workflows/publish.yml` on macOS. The workflow requires the tag version to match `VERSION_NAME`, installs the locked Cosmos tooling, passes `releaseCheck`, signs every publication in memory, uploads the aggregate to the Maven Central Portal with automatic release, and creates the matching GitHub Release only after Central accepts the deployment.

Configure a protected GitHub `release` environment with `CENTRAL_PORTAL_USERNAME`, `CENTRAL_PORTAL_PASSWORD`, `SIGNING_KEY`, and `SIGNING_PASSWORD`. The Central credentials are the generated Sonatype user token; the signing key is an ASCII-armored private PGP key. No credential belongs in the repository.

Protect the `release` environment with a required reviewer. Approving that environment is the final publication decision; the workflow still refuses unstable or mismatched tags, dirty source, unconfirmed redistribution rights, canonical Cosmos defects, incomplete release documents, unsafe source/history, or malformed publication artifacts.

## Source, publication and release audits

```sh
python3 -B -m unittest discover -s scripts -p test_check_publication.py
python3 -B scripts/check_publication.py source --root .
python3 -B scripts/check_publication.py publication --repo build/maven-repository --version 0.1.0
./gradlew --no-daemon --max-workers=2 localQuality
./gradlew --no-daemon --max-workers=2 stageIntegration
./gradlew --no-daemon --max-workers=2 releaseCheck
./gradlew --no-daemon --max-workers=2 stageRelease
```

`check` includes the audit regression tests and current/all-ref history scan. `localQuality` adds formatting, generated documentation and fresh filesystem publication auditing. Publication selection uses exact GAVs and snapshot metadata, including compound native resource extensions and the root Kotlin tooling-metadata attachment. Artifact hashes/sizes, SHA-256 sidecars, target redirects, public dependencies, generated HTML, sources, nested archive bounds/paths and permission-free AAR manifests are checked. Stale unselected snapshots and ignored debug frameworks are not release inputs.

`stageIntegration` is the pre-integration private gate: it runs `localQuality`, then creates `build/simt-ui-VERSION-private-integration.zip` from only the exact audited Maven inventory, retained license/notice texts and UI provenance/module/setup documentation. Each Maven publication also carries a deterministic legal-notices ZIP. Extract the archive's `maven/` directory into a new local directory and point consumers' `uiRepository` at it. No sibling producer or mutable publication directory is needed for this rehearsal. The manifest records HEAD, the current source-content fingerprint and whether the source tree is dirty; dirty private development is not release approval. Source/history scans, metadata, checksums and safe exclusive output remain mandatory. The source fingerprint and artifact bytes are rechecked before writing. The manifest explicitly denies public redistribution and runtime approval; Cosmos is not bundled; its independent schema validation does not establish runtime or release approval. Existing ZIPs are never overwritten: reuse the staged archive, or choose a new output path with the standalone `stage-integration --root . --repo build/maven-repository --version VERSION --output build/NEW.zip` command.

`releaseCheck` and `stageRelease` are the final local public-release gates. They require stable SemVer, matching `vVERSION` at clean HEAD, independently confirmed UI/Cosmos redistribution rights, strict canonical Cosmos validation, complete release documents, ABI baselines, a clean source/history audit, and exact fresh publication validation. Neither task uploads or signs anything. `stageRelease` creates a deterministic unsigned ZIP under `build`; the tag workflow separately signs and uploads the Gradle publication aggregate.

The standalone `release` and `stage` subcommands require `--root`, `--repo`, and `--version`; `stage` additionally requires a new workspace-contained `--output` ZIP path. There is no force flag or environment bypass. The release audit inventories all selected Maven artifacts and sidecars plus license/NOTICE/README/changelog/module docs, plan documentation, ABI baselines, both provenance ledgers and every Cosmos inventory file.

Version `0.1.0` deliberately does not require iOS runtime, physical Android API 28, Android Studio Preview-panel, or cloud security evidence. These remain documented validation limits rather than publication gates. This policy was explicitly selected for the first release; the workflow does not claim those checks occurred.

## Validation limits and release obligations

Producer and independent-consumer Android host/emulator tests, R8 packaging, localized resources and both Native framework links have been exercised. API 28 coverage is Robolectric host-only, not physical API 28 evidence. No iOS simulator runtime is installed in the validated environment. Native compile/link success is not iOS UI/resource launch, screen-reader, physical haptic or documented Xcode compatibility evidence; Xcode 27.1 exceeds the inspected Kotlin compatibility table.

Local redacted audits are bounded pattern/content checks, not a guarantee that all secrets or redistribution restrictions are absent. They cover the current tree, all committed history and exact metadata-selected artifacts, including nested sources, resources, AARs, klibs and generated documentation. Public release additionally requires owner-confirmed source/spec rights, a stable matching tag at clean HEAD, strict Cosmos conformance, complete audited artifacts, valid PGP signing and approval of the protected GitHub release environment.

Never commit publishing or signing credentials. Do not retire the old app-ui submodule or alter Wakt as cleanup. A safe production migration still needs an actually available producer artifact and separately validated host changes.
