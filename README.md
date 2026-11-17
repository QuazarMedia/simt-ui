# SIMT UI

Shared Compose primitives for Android and Kotlin Multiplatform iOS consumers, plus an independently versioned Cosmos specification snapshot.

**Private-development status: not approved for public release.** Said Elimam has confirmed the rights and licensed UI/Cosmos code under Apache-2.0 and Cosmos specifications/documentation under CC BY 4.0; retain [LICENSE](LICENSE), [LICENSE-DOCS](LICENSE-DOCS) and [NOTICE](NOTICE). License permission is separate from approval to publish, sign or upload a release. No published `app.simt:app-ui` release is promised. See [the extraction plan](docs/plans/0001-shared-ui-extraction.md) and the separate provenance ledgers in [app-ui](app-ui/source-provenance.json) and [cosmos-spec](cosmos-spec/provenance.json).

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

Local Maven publications live in ignored `build/maven-repository`. All four publications carry metadata, sources and generated HTML documentation; native targets also carry resource ZIPs. No Maven Local or remote uploader is configured. ABI dumps in `app-ui/api` are reviewed source-controlled baselines: `:app-ui:checkKotlinAbi` checks them, while `:app-ui:updateKotlinAbi` is an explicit maintainer action, never an automatic repair during verification.

## Independent consumption rehearsal

After local publication, run these independent builds with JDK 21 and the Android SDK configured. Instrumentation commands require a running device and should run sequentially when sharing one device.

```sh
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 --refresh-dependencies testDebugUnitTest verifyPackagedLocales
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 connectedDebugAndroidTest
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 --refresh-dependencies testAndroidHostTest compileIosMainKotlinMetadata linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 connectedAndroidDeviceTest
```

Both examples exclusively resolve group `app.simt` from `../../build/maven-repository` by default. Override with `-PuiRepository=/absolute/path/to/repository` and `-PuiVersion=<actually-staged-version>`. They do not use `includeBuild`, Maven Local, project substitution, producer sources or manually copied library assets.

An Android consumer declares `implementation("app.simt:app-ui:<actually-staged-version>")`; a KMP consumer puts the same dependency in `commonMain.dependencies`. Keep a host's existing Compose/BOM and plugin policy unless real resolution proves a change necessary. The examples reproduce Android host pins without upgrading either production application. Production-consumer adoption is committed on isolated local branches, using `0.1.0-SNAPSHOT` only through an explicit private Maven repository or sibling composite substitution; it is not merged to either main branch or evidence of a public release.

The compatible producer stage is `simt-ui-0.1.0-SNAPSHOT-43c285c-integration.zip`, SHA-256 `9583a2bc51a6889edca328facd7a73f6930ddaf2e45edffd87633c29e9270b6c`. Its 72 manifest entries were independently rehashed. The simt-app adoption is commit `fb020912f` on `slice/ui05-shared-ui`, after prerequisite `f5b53e053`; the ECHO adoption is commit `5443527` on `slice/ui06-shared-ui`. Both staged-artifact and sibling-composite validation pass. These local commits remain subject to safe main-branch reconciliation and fresh authorization before any push.

## Android IDE previews

Open `examples/android-consumer` as an **independent project** in Android Studio with JDK 21 and SDK 37. Set `uiRepository=/absolute/path/to/extracted/maven` and `uiVersion=0.1.0-SNAPSHOT` in your local Gradle properties (or pass the equivalent `-P` options), using an actually staged artifact repository; do not substitute producer sources. Sync, select the debug variant, and open `src/debug/kotlin/app/simt/ui/preview/SharedUiPreviews.kt` in Design/Split. The Compose-enabled Android fixture hosts previews for all eight primitives and header tools; preview sources and tooling dependencies are debug-only, not producer library or release APK inputs.

Samples use synthetic values, a small Material theme, bounded viewports and disabled motion/haptics. Light/dark, French/RTL and large-text variants supplement the component samples. The tooltip's static image shows only its labeled anchor: use interactive preview and long press to reveal the popup. The countdown intentionally stays at 1. Insets use `showSystemUi=true`, but layoutlib may report zero insets; this is not proof of hidden-system-bar behavior. Host composition tests do not establish that the Android IDE Preview panel rendered, nor native/device runtime or haptic correctness.

## Private specification checks

```sh
./gradlew --offline --no-daemon --max-workers=2 :cosmos-spec:check :cosmos-spec:privateSnapshotZip
npm --prefix cosmos-spec run check:canonical-schema
```

The first command validates the exact licensed pin and 52 offline tests, and creates a deterministic local ZIP retaining the three upstream license/notice files. The strict second command passes: the committed upstream schema requires exactly 36 union sections and the correct 17/16 common-plus-platform sets, rejecting missing, duplicate, extra and foreign sections. All 33 reviewed files are byte-identical to upstream commit `86b36f9e9b13e2426d54207b60beb7323fbcd5d6`; no local canonical bytes are weakened or repaired. Cosmos remains authoritative upstream; this snapshot does not introduce a normative iOS adapter.

## Source, publication and release audits

```sh
python3 -B -m unittest discover -s scripts -p test_check_publication.py
python3 -B scripts/check_publication.py source --root .
python3 -B scripts/check_publication.py publication --repo build/maven-repository --version 0.1.0-SNAPSHOT
./gradlew --no-daemon --max-workers=2 localQuality
./gradlew --no-daemon --max-workers=2 stageIntegration
./gradlew --no-daemon --max-workers=2 releaseCheck
./gradlew --no-daemon --max-workers=2 stageRelease
```

`check` includes the audit regression tests and current/all-ref history scan. `localQuality` adds formatting, generated documentation and fresh filesystem publication auditing. Publication selection uses exact GAVs and snapshot metadata, including compound native resource extensions and the root Kotlin tooling-metadata attachment. Artifact hashes/sizes, SHA-256 sidecars, target redirects, public dependencies, generated HTML, sources, nested archive bounds/paths and permission-free AAR manifests are checked. Stale unselected snapshots and ignored debug frameworks are not release inputs.

`stageIntegration` is the pre-integration private gate: it runs `localQuality`, then creates `build/simt-ui-VERSION-private-integration.zip` from only the exact audited Maven inventory, retained license/notice texts and UI provenance/module/setup documentation. Each Maven publication also carries a deterministic legal-notices ZIP. Extract the archive's `maven/` directory into a new local directory and point consumers' `uiRepository` at it. No sibling producer or mutable publication directory is needed for this rehearsal. The manifest records HEAD, the current source-content fingerprint and whether the source tree is dirty; dirty private development is not release approval. Source/history scans, metadata, checksums and safe exclusive output remain mandatory. The source fingerprint and artifact bytes are rechecked before writing. The manifest explicitly denies public redistribution and runtime approval; Cosmos is not bundled; its independent schema validation does not establish runtime or release approval. Existing ZIPs are never overwritten: reuse the staged archive, or choose a new output path with the standalone `stage-integration --root . --repo build/maven-repository --version VERSION --output build/NEW.zip` command.

`releaseCheck` and `stageRelease` remain the separate final public-release gates and currently **must fail**: the version is a snapshot, and final HEAD-tied owner/artifact approval, runtime and integration evidence remain missing. The code/document license grants and strict Cosmos schemas are now confirmed. Neither task uploads or signs anything. Only after every gate is legitimately satisfied can `stageRelease` create a new deterministic unsigned owner-reviewed ZIP under `build`. Private integration staging cannot supply or override final approval, licenses, stable tag, runtime evidence, complete consumer integration, strict Cosmos or exact artifact review.

The standalone `release` and `stage` subcommands require `--root`, `--repo`, `--version`, an existing `--approval` owner-review JSON, and (for stage) a new workspace-contained `--output` ZIP path. Gradle takes the existing approval path through `-PreleaseApproval=<path>`; there is no force flag or environment approval bypass. A release version must be stable `x.y.z`, matching tag `vVERSION` at a clean HEAD. This tool never creates tags or owner approval.

Owner approval schema version 1 contains:

- `schemaVersion: 1`, exact `head` and stable `version`.
- `ownerReview`: nonempty `reviewer`, and independently true `approved`, `sourceCodeRights`, `specCodeRights`, `specDocumentRights`, `unsignedLocalStage`, `signingSeparateOwnerRequirement`.
- `provenance`: SHA-256 values `sourceSha256` and `cosmosSha256` for the two current provenance ledgers. Source provenance must independently say `licenseStatus: confirmed` and `publicRedistribution: approved`. Cosmos must independently confirm code/document license statuses, public-redistribution scope/approval and no known schema defects. Changing a boolean does not establish rights or repair the frozen canonical pin.
- `evidence`: references named `iosRuntime`, `androidFloorHaptics`, `consumerIntegration`, `securityReview`, `producerQuality`. Each is an existing workspace-relative JSON `path` and exact `sha256`. Each evidence document must name the same `head`, `result: passed`, and true `checks` keys: respectively rendering/insets/haptics/resources; floor/haptics/rendering/resources; integration; security/privacy/accessibility; producer/abi/docs. iOS evidence also needs `platform: ios` and an actual nonempty `runtime`; Android evidence needs `platform: android`, `physicalDevice: true`, `apiLevel: 28`; integration must independently confirm `consumers.simt-app`, `consumers.simt-echo-android`, `consumers.simt-echo-ios`.
- `artifacts`: sorted exact `{path, size, sha256}` entries matching a fresh audit, not merely a version name. The inventory includes `maven/` publications and sidecars, plus `source/` license/NOTICE/README/changelog/module docs, plan documentation, ABI baselines, both provenance ledgers and every Cosmos inventory file. Missing release documents block staging; no unapproved license or fabricated changelog is supplied here.

Owner-reviewed evidence must describe tests actually performed. No approval/evidence file asserting missing device, rights or integration checks is generated by this repository.

## Validation limits and release obligations

Producer and independent-consumer Android host/emulator tests, R8 packaging, localized resources and both Native framework links have been exercised. API 28 coverage is Robolectric host-only, not physical API 28 evidence. No iOS simulator runtime is installed in the validated environment. Native compile/link success is not iOS UI/resource launch, screen-reader, physical haptic or documented Xcode compatibility evidence; Xcode 27.1 exceeds the inspected Kotlin compatibility table.

Local redacted audits are bounded pattern/content checks, not a guarantee that all secrets or redistribution restrictions are absent. They must cover the current tree, all committed history and exact metadata-selected artifacts, including nested sources, resources, AARs, klibs and generated documentation. Public release additionally requires owner-confirmed source/spec rights, a stable version/tag and clean tree, approved runtime/integration evidence, strict Cosmos conformance, complete audited artifacts and explicit approval tied to the release commit.

Do not add an uploader, signing credentials or CI permission changes as a way around missing gates. Do not retire the old app-ui submodule or alter Wakt as cleanup. A safe production migration needs an actually available producer artifact and separate validated host changes. This repository contains no approval file that falsely asserts those external obligations are satisfied.
