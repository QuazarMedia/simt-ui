# Security policy

## Supported versions

| Component | Supported version |
| --- | --- |
| `app.simt:app-ui` | Latest published `0.1.x` release |
| `@simt-dev/web-ui` | Latest published `0.1.x` release |
| `design-site` | Current deployment of `design.simt.app` |

Security fixes are released for the newest published patch line. Older versions may require an upgrade.

## Reporting a vulnerability

Report vulnerabilities privately to **contact@simt.app** with the subject `SIMT UI security report`. GitHub private vulnerability reporting is not enabled yet; maintainers should enable it and update this section when available.

Include:

- affected component and version;
- reproduction steps or a minimal proof of concept;
- expected and actual behavior;
- impact and any known mitigations;
- whether the issue is already public.

Do not include credentials, private keys, signing material, real user data or unpublished exploit details in email subject lines. Do not open a public GitHub issue for an undisclosed vulnerability. Maintainers will acknowledge the report, assess scope and coordinate a fix and disclosure with the reporter.

## Security boundaries

- `app-ui` contains UI primitives and feature kits, not application configuration, analytics, persistence or a runtime network client.
- `web-ui` is dependency-free ESM/CSS. Preference persistence is an explicit opt-in module and must not be imported by privacy-only consumers.
- `design-spec` is inert source data and documentation, not executable runtime code.
- `design-site` is a static-assets-only Cloudflare Worker configuration with no `workers.dev` route.

The release workflow validates namespaced stable tags, package/version identity, source and history safety, release authorization and changelog notes. Maven publications are signed in memory. npm publications use GitHub OIDC trusted publishing, a pinned supported npm CLI and provenance without a long-lived npm token.

## Maintainer controls

Before the next release, maintainers must:

- protect `main` and require reviewed pull requests with passing checks;
- protect the existing `release` environment with required reviewers and restrict release-tag creation;
- move Central Portal and PGP signing secrets from repository scope into that environment, then remove the repository-level copies;
- configure the npm trusted publisher for `QuazarMedia/simt-ui`, workflow `publish.yml`, environment `release`, and direct `npm publish`;
- verify an OIDC publication, then disallow npm tokens and revoke obsolete automation tokens;
- enable secret scanning with push protection, Dependabot security updates and private vulnerability reporting;
- restrict GitHub Actions to trusted sources and keep third-party Actions pinned to full commit SHAs.

Workflow and documentation changes do not enable these repository, environment or registry settings.

## Audit limits

The local audit is a bounded offline pattern/content check. It is not a guarantee that every secret, malicious dependency or redistribution restriction is absent. Publication audits additionally inspect exact Maven artifacts, nested archives, metadata, checksums, public dependencies, sources and generated documentation.

One reviewed historical generated agent-hook blob contains a maintainer home-directory path. It is not a credential, remains visible in public Git history, and is exempted only from the personal-path finding by its exact SHA-256 digest. Credential, private-key, forbidden-file and all other findings remain blocking.
