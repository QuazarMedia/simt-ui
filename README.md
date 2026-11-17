# SIMT UI

Shared Compose primitives for Android and Kotlin Multiplatform iOS consumers, plus an independently versioned Cosmos specification snapshot.

**Private-development status: not approved for public redistribution or release.** Copied code and specification licenses are unconfirmed. No published `app.simt:app-ui` release is promised. See [the extraction plan](docs/plans/0001-shared-ui-extraction.md) and the separate provenance ledgers in [app-ui](app-ui/source-provenance.json) and [cosmos-spec](cosmos-spec/provenance.json).

## Modules and boundaries

- `app-ui`: eight generic UI primitives in `app.shared.ui`, private Compose resources, and narrow Android/iOS inset, width, dispatcher and haptic implementations. Targets: Android API 28+, `iosArm64`, `iosSimulatorArm64`. No application framework, service configuration, analytics, persistence, requested permission or runtime network client.
- `cosmos-spec`: inert, byte-pinned Cosmos 2.3.0 sources and exports from simt-space. No dependency from the runtime library. Private integrity is not canonical schema conformance: four upstream scoped-schema failures are preserved.
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

An Android consumer declares `implementation("app.simt:app-ui:<actually-staged-version>")`; a KMP consumer puts the same dependency in `commonMain.dependencies`. Keep a host's existing Compose/BOM and plugin policy unless real resolution proves a change necessary. The examples reproduce Android host pins without upgrading either production application. A production catalog must not point to an imaginary release; published/composite host migrations remain gated and unapplied.

## Private specification checks

```sh
./gradlew --offline --no-daemon --max-workers=2 :cosmos-spec:check :cosmos-spec:privateSnapshotZip
npm --prefix cosmos-spec run check:canonical-schema
```

The first command validates the exact private pin and 44 offline tests, and creates the deterministic private ZIP. The strict second command currently exits nonzero: the global rule schema requires 36 sections, but four scoped exports contain 16/17. Those bytes and schema are not weakened or silently repaired. Cosmos remains authoritative upstream; this snapshot does not introduce a normative iOS adapter.

## Source, publication and release audits

```sh
python3 -B -m unittest discover -s scripts -p test_check_publication.py
python3 -B scripts/check_publication.py source --root .
python3 -B scripts/check_publication.py publication --repo build/maven-repository --version 0.1.0-SNAPSHOT
./gradlew --no-daemon --max-workers=2 localQuality
./gradlew --no-daemon --max-workers=2 releaseCheck
./gradlew --no-daemon --max-workers=2 stageRelease
```

`check` includes the audit regression tests and current/all-ref history scan. `localQuality` adds formatting, generated documentation and fresh filesystem publication auditing. Publication selection uses exact GAVs and snapshot metadata, including compound native resource extensions and the root Kotlin tooling-metadata attachment. Artifact hashes/sizes, SHA-256 sidecars, target redirects, public dependencies, generated HTML, sources, nested archive bounds/paths and permission-free AAR manifests are checked. Stale unselected snapshots and ignored debug frameworks are not release inputs.

`releaseCheck` and `stageRelease` currently **must fail**: the version is a snapshot, rights/runtime/integration approval is missing and strict Cosmos conformance fails. Neither task uploads or signs anything. If every gate is legitimately satisfied in a later owner-reviewed revision, `stageRelease` can create only a new deterministic unsigned local ZIP under `build`, with its exact checksum inventory. It refuses existing destinations and rechecks inputs and authorization before writing. Synthetic positive staging tests are not an actual approved release rehearsal.

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
