# Changelog

## 0.1.0 — 2026-11-17

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
- Added bounded source/history and nested publication audits; final public release still requires current owner-reviewed evidence and separate signing/upload authorization.
