# SIMT UI

Shared Compose primitives for Android and Kotlin Multiplatform iOS consumers, framework-agnostic web UI primitives, an independently versioned SIMT Design specification with frozen Cosmos provenance, and the standalone SIMT Design site.

## Published artifacts

| Module | Artifact | Current public version | Boundary |
| --- | --- | --- | --- |
| `app-ui` | `app.simt:app-ui` on Maven Central | `0.1.0` | Android API 28+, `iosArm64`, `iosSimulatorArm64` |
| `web-ui` | `@simt-dev/web-ui` on npm | `0.1.0` | Dependency-free ESM and CSS for static sites |
| `design-spec` | Source-controlled specification and frozen provenance | SIMT Design `2.4.0` / Cosmos `2.3.0` | Not a runtime dependency or registry release unit |
| `design-site` | Static publication at `https://design.simt.app` | SIMT Design `2.4.0` | Generated site and Cloudflare Worker configuration; deployed separately |

`app-ui 0.1.0` was published to Maven Central and released on GitHub on 2026-11-18. `@simt-dev/web-ui 0.1.0` was published to npm on 2026-11-27. Changes under `app-ui Unreleased` are not included in the Maven Central `0.1.0` artifact.

Said Elimam confirmed the UI and Design redistribution rights. Code is licensed under Apache-2.0; Design specifications and documentation are licensed under CC BY 4.0. Retain [LICENSE](LICENSE), [LICENSE-DOCS](LICENSE-DOCS) and [NOTICE](NOTICE), including the frozen Cosmos attribution. See [the extraction plan](docs/plans/0001-shared-ui-extraction.md), [app-ui provenance](app-ui/source-provenance.json) and [Design provenance](design-spec/provenance.json).

## Install

Add `mavenCentral()` to the consumer's repositories, then declare the Android or common Kotlin dependency:

```kotlin
implementation("app.simt:app-ui:0.1.0")
```

For web projects using Node 22.20+:

```sh
npm install @simt-dev/web-ui@0.1.0
```

```js
import { initMotion, initTheme, observeReveals, trackSections } from "@simt-dev/web-ui";
import "@simt-dev/web-ui/styles.css";
```

The optional prepaint script is exported from `@simt-dev/web-ui/bootstrap`. Social-style preference persistence is separate at `@simt-dev/web-ui/storage`; import it only in products that allow storage.

## Modules and boundaries

- `app-ui`: caller-state-owned Compose primitives and feature kits in `app.simt.ui`, private Compose resources, and narrow Android/iOS inset, width, dispatcher and haptic implementations. It includes no application framework, service configuration, analytics, persistence, requested permission or runtime network client.
- `design-spec`: inert, byte-pinned Cosmos 2.3.0 upstream sources and exports plus the authored SIMT Design 2.4.0 specification. The runtime library does not depend on it.
- `design-site`: independently generated SIMT Design 2.4.0 static publication. It owns canonical HTML, scoped and union exports, schemas, Markdown, discovery files, static assets and the static-assets-only Worker configuration.
- `web-ui`: reveal/stagger motion, ambient pausing, header/section observers, performance tiering, pointer spotlight/tilt, theme/motion controls, buttons and marquees. It has no framework or runtime package dependency.
- `examples/android-consumer`: an ordinary Android Compose build with no CMP plugin, consuming filesystem-published Maven artifacts by default.
- `examples/multiplatform-consumer`: a separate CMP build using the same root coordinate and linking both Apple frameworks.

No theme, navigation, route, branding, host build convention or application configuration was extracted. API contracts are in [the app-ui module documentation](app-ui/Module.md).

## Shared web UI

`web-ui` is consumed as source ESM/CSS, so Vite and custom static generators use the same implementation without a transpilation step. The runtime uses `data-motion="full|reduced"`, `data-perf="lite"`, `data-reveal`, `data-ambient`, `.is-revealed`, `.is-offscreen` and `.is-scrolled`.

```sh
npm --prefix web-ui run check
./gradlew --no-daemon --max-workers=2 :web-ui:check
```

The Gradle root `check` includes the web-ui tests.

## Build locally

Use JDK 21, Android SDK 37 and the checked-in wrapper. Set `ANDROID_HOME` locally instead of committing `local.properties`. Native compilation/linking requires a supported Xcode installation; select it per command with `DEVELOPER_DIR` rather than changing global settings. Node 22.20+ and Python 3 are required for the specification, static site and audit tools.

```sh
npm --prefix design-spec ci --ignore-scripts --no-audit --no-fund
npm --prefix design-site ci --ignore-scripts --no-audit --no-fund
./gradlew --no-daemon --max-workers=2 check spotlessCheck
./gradlew --no-daemon --max-workers=2 :app-ui:dokkaGeneratePublicationHtml
./gradlew --no-daemon --max-workers=2 :app-ui:publishAllPublicationsToLocalValidationRepository
```

The npm bootstrap is explicit and may download locked dependencies. Later specification and site checks do not install dependencies; the frozen snapshot resolves schemas offline, while the site deterministically regenerates its ignored `dist` tree before verification. Gradle/dependency bootstrap may use the configured public repositories; this is separate from offline library runtime behavior.

Local Maven publications live in ignored `build/maven-repository`. All four publications carry metadata, sources, generated HTML documentation and legal notices; native targets also carry resource ZIPs. Local tasks do not upload unless `publishAggregationToCentralPortal` is explicitly invoked with release credentials. ABI dumps in `app-ui/api` are reviewed source-controlled baselines: `:app-ui:checkKotlinAbi` checks them, while `:app-ui:updateKotlinAbi` is an explicit maintainer action.

## Independent consumption rehearsal

After local publication, run these independent builds with JDK 21 and the Android SDK configured. Instrumentation commands require a running device and should run sequentially when sharing one device.

```sh
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 --refresh-dependencies testDebugUnitTest verifyPackagedLocales
examples/android-consumer/gradlew -p examples/android-consumer --no-daemon --max-workers=2 connectedDebugAndroidTest
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 --refresh-dependencies testAndroidHostTest compileIosMainKotlinMetadata linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64
examples/multiplatform-consumer/gradlew -p examples/multiplatform-consumer --no-daemon --max-workers=2 connectedAndroidDeviceTest
```

Both examples resolve group `app.simt` from `../../build/maven-repository` by default. Override with `-PuiRepository=/absolute/path/to/repository` and `-PuiVersion=<actually-staged-version>`. They do not use `includeBuild`, Maven Local, project substitution, producer sources or manually copied library assets. Keep the host's existing Compose/BOM and plugin policy unless real resolution proves a change necessary.

## Android IDE previews

Open `examples/android-consumer` as an independent project in Android Studio with JDK 21 and SDK 37. Set `uiRepository=/absolute/path/to/extracted/maven` and `uiVersion=0.1.0` in local Gradle properties, or pass equivalent `-P` options. Sync, select the debug variant and open `src/debug/kotlin/app/simt/ui/preview/SharedUiPreviews.kt` in Design/Split. The Compose-enabled Android fixture hosts debug-only previews for the released shared primitives and header tools; preview sources and tooling dependencies are not producer library or release APK inputs.

Samples use synthetic values, a small Material theme, bounded viewports and disabled motion/haptics. Light/dark, French/RTL and large-text variants supplement the component samples. Host composition tests do not establish that the Android IDE Preview panel rendered, nor native/device runtime or haptic correctness.

## Specification checks

```sh
./gradlew --offline --no-daemon --max-workers=2 :design-spec:check :design-spec:privateSnapshotZip
npm --prefix design-spec run check:canonical-schema
```

The first command validates the exact licensed pin and 52 offline tests, then creates a deterministic local ZIP retaining the three upstream license/notice files. The strict second command requires exactly 36 union sections and the correct 17/16 common-plus-platform sets. The 33 reviewed files remain byte-identical to historical source commit `86b36f9e9b13e2426d54207b60beb7323fbcd5d6`; no pinned bytes are weakened or repaired. This snapshot preserves provenance but is not the current publication authority and does not introduce a normative iOS adapter.

## Standalone SIMT Design site

`design-site` publishes SIMT Design `2.4.0` at the root of `https://design.simt.app`, with frozen Cosmos `2.3.0` under `/v/2.3.0`. Its generator owns canonical HTML, scoped and union exports, schemas, Markdown, discovery files, a true 404 page and self-hosted assets. `wrangler.jsonc` configures a static-assets-only Worker with explicit clean-route rewrites, `html_handling: "none"` and no `workers.dev` route.

```sh
npm --prefix design-site run build
npm --prefix design-site run check
./gradlew --no-daemon --max-workers=2 :design-site:check
```

The Gradle task uses already installed locked npm dependencies and never installs them. `dist` is generated and ignored. Deployment remains a separate, explicitly authorized operation; verification does not upload or alter Cloudflare or `simt-space`.

## Release automation

Tags are namespaced per library: `<library>-vMAJOR.MINOR.PATCH`, for example `app-ui-v0.1.0` or `web-ui-v0.2.0`. Release notes must have a matching `<library> <version>` heading in [CHANGELOG.md](CHANGELOG.md). Pushing a tag runs [`.github/workflows/publish.yml`](.github/workflows/publish.yml); unknown or unstable tags are rejected before any publication.

- `app-ui` verifies the tag against `VERSION_NAME`, runs `releaseCheck`, signs all publications in memory and uploads the aggregate to the Maven Central Portal with automatic release.
- `web-ui` verifies the exact package name and version, audits source/history, runs the web-ui tests and publishes through npm trusted publishing with OIDC and provenance. The job uses Node 24 and pinned npm 11.21.3, which satisfies npm's trusted-publishing CLI requirement. No npm token is used.
- The GitHub Release job runs only after the selected registry publication succeeds. Existing registry versions are treated as already published, and an existing GitHub Release is updated rather than recreated, so reruns can finish after a partial release.

Before tagging, maintainers must configure a protected GitHub `release` environment with required reviewers and these Central Portal secrets:

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`
- `SIGNING_KEY` — ASCII-armored private PGP key
- `SIGNING_PASSWORD`

These secrets currently exist only at repository scope. Move them into the protected `release` environment and remove the repository-level copies before tagging. The Central credentials are generated Sonatype user tokens. NMCP currently authenticates with username/password; no Maven Central OIDC path is configured. No credential belongs in the repository.

For `@simt-dev/web-ui`, configure an npm trusted publisher with organization/user `QuazarMedia`, repository `simt-ui`, workflow filename `publish.yml`, environment `release`, and permission for direct `npm publish`. After a successful OIDC publication is verified, disallow traditional npm tokens and revoke obsolete automation tokens. See [npm trusted publishing](https://docs.npmjs.com/trusted-publishers/).

Workflow files cannot enable GitHub protections. Maintainers must also protect `main`, require pull-request reviews and status checks, restrict release-tag creation, enable secret scanning with push protection, Dependabot security updates and private vulnerability reporting, and restrict Actions to trusted, SHA-pinned sources.

## Source, publication and release audits

```sh
python3 -B -m unittest discover -s scripts -p 'test_*.py'
python3 -B scripts/check_publication.py source --root .
python3 -B scripts/check_publication.py publication --repo build/maven-repository --version 0.1.0
./gradlew --no-daemon --max-workers=2 localQuality
./gradlew --no-daemon --max-workers=2 stageIntegration
./gradlew --no-daemon --max-workers=2 releaseCheck
./gradlew --no-daemon --max-workers=2 stageRelease
```

`check` includes the audit regression tests, publishing-workflow invariants and the current/all-ref history scan. `localQuality` adds formatting, generated documentation and fresh filesystem publication auditing. Publication selection uses exact GAVs and snapshot metadata, including compound native resource extensions and the root Kotlin tooling-metadata attachment. Artifact hashes/sizes, SHA-256 sidecars, target redirects, public dependencies, generated HTML, sources, nested archive bounds/paths and permission-free AAR manifests are checked. Stale unselected snapshots and ignored debug frameworks are not release inputs.

The current generated agent hook is portable. One exact historical generated hook blob remains in public Git history and is exempted only from the personal-path category by its SHA-256 digest; credential, private-key, forbidden-file and every other audit category still apply. History was not rewritten.

`stageIntegration` is the pre-integration staging gate: it runs `localQuality`, then creates `build/simt-ui-VERSION-private-integration.zip` from only the exact audited Maven inventory, retained license/notice texts and UI provenance/module/setup documentation. Each Maven publication also carries a deterministic legal-notices ZIP. Extract the archive's `maven/` directory into a new local directory and point consumers' `uiRepository` at it. The manifest records HEAD, the current source-content fingerprint and whether the source tree is dirty; a dirty tree is not release approval. Source/history scans, metadata, checksums and safe exclusive output remain mandatory. Existing ZIPs are never overwritten.

`releaseCheck` and `stageRelease` are the final local app-ui public-release gates. They require stable SemVer, a matching `<library>-vVERSION>` tag at clean HEAD, independently confirmed UI and Design redistribution rights, strict canonical Design validation, complete release documents, ABI baselines, a clean source/history audit and exact fresh publication validation. Neither task uploads or signs anything. `stageRelease` creates a deterministic unsigned ZIP under `build`; the tag workflow separately signs and uploads the Gradle publication aggregate.

The standalone `release` and `stage` subcommands require `--root`, `--repo`, `--version` and `--library`; `stage` additionally requires a new workspace-contained `--output` ZIP path. There is no force flag or environment bypass. The release audit inventories all selected Maven artifacts and sidecars plus license, notice, README, changelog, security, contribution and module documents, plan documentation, ABI baselines, both provenance ledgers and every specification inventory file.

Version `0.1.0` deliberately does not require iOS runtime, physical Android API 28, Android Studio Preview-panel or cloud security evidence. These remain documented validation limits rather than publication gates; the workflow does not claim those checks occurred.

## Validation limits and release obligations

Producer and independent-consumer Android host/emulator tests, R8 packaging, localized resources and both Native framework links have been exercised. API 28 coverage is Robolectric host-only, not physical API 28 evidence. No iOS simulator runtime is installed in the validated environment. Native compile/link success is not iOS UI/resource launch, screen-reader, physical haptic or documented Xcode compatibility evidence; Xcode 27.1 exceeds the inspected Kotlin compatibility table.

Local redacted audits are bounded pattern/content checks, not a guarantee that all secrets or redistribution restrictions are absent. They cover the current tree, all committed history and exact metadata-selected artifacts, including nested sources, resources, AARs, klibs and generated documentation. Public release additionally requires owner-confirmed source/spec rights, a stable matching tag at clean HEAD, strict Design conformance, complete audited artifacts, valid PGP signing and approval of the protected GitHub release environment.

Never commit publishing or signing credentials. Do not retire the old app-ui submodule or alter Wakt as cleanup. A safe production migration still needs an actually available producer artifact and separately validated host changes.

## Security and contributing

Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md); do not open a public issue for an undisclosed vulnerability. Development setup, validation expectations and release ownership are in [CONTRIBUTING.md](CONTRIBUTING.md).
