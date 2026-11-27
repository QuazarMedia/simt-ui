# Contributing to SIMT UI

## Development setup

Use JDK 21, Android SDK 37, Node 22.20+ and Python 3. Native iOS compilation also requires a supported Xcode installation. Use the checked-in Gradle wrapper and set `ANDROID_HOME` locally; do not commit `local.properties`.

```sh
npm --prefix design-spec ci --ignore-scripts --no-audit --no-fund
npm --prefix design-site ci --ignore-scripts --no-audit --no-fund
./gradlew --no-daemon --max-workers=2 check spotlessCheck
```

The web-only suite is:

```sh
npm --prefix web-ui run check
```

The publication and workflow audit suite is:

```sh
python3 -B -m unittest discover -s scripts -p 'test_*.py'
python3 -B scripts/check_publication.py source --root .
```

`./gradlew check` also runs the Python audits and the web-ui tests. Native and device tests require their documented platform tooling; do not claim unavailable runtime or device evidence.

## Change boundaries

- Keep `app-ui`, `web-ui`, `design-spec` and `design-site` independently understandable.
- Do not add application frameworks, analytics, persistence, requested permissions, private host types or runtime network clients to `app-ui`.
- Keep `web-ui` dependency-free and framework-agnostic. Storage is opt-in.
- Do not edit generated projection, specification export, site `dist` or ABI files as a shortcut. Change the authoritative source and run the documented generator or explicit ABI update task.
- Preserve the frozen Cosmos bytes, license texts, notices and provenance ledgers.
- Add focused behavior tests for executable changes. Documentation/configuration-only changes still need named format or static checks.
- Record user-visible changes under the appropriate `<library> Unreleased` section in [CHANGELOG.md](CHANGELOG.md).

## Generated files and secrets

Never commit credentials, private keys, signing passwords, generated keystores, local properties, environment files, npm tokens or Cloudflare credentials. Review `git status` and the staged diff before committing. Leave unrelated work untouched.

The SIMT agent/hook files are generated integration files. Reinstall or regenerate them through simt-dev rather than introducing machine-specific fallback paths.

## Pull requests

Keep a pull request scoped to one independently valuable change. Include the exact validation commands and their results, known platform limits, and any follow-up work. Do not tag, publish, deploy or change shared repository settings from a contribution; those are maintainer operations.

Report vulnerabilities through [SECURITY.md](SECURITY.md), not a public issue.

## Releases

Release tags use `<library>-vMAJOR.MINOR.PATCH`. The tag version must match `VERSION_NAME` for `app-ui` or `web-ui/package.json` for `web-ui`, and the changelog must contain the matching release section. Only maintainers may create release tags or approve the protected `release` environment.

The publishing workflow is documented in [README.md](README.md). A successful registry publication is irreversible; reruns resume by recognizing existing versions and updating the GitHub Release. Never use an already-published version for different bytes.

## License

Contributions are accepted under Apache-2.0 for code and CC BY 4.0 for Design specifications/documentation, consistent with the repository licenses and notices.
