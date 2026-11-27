<!-- simt:begin -->
<!-- simt:generated -->
<!-- simt:sha=7497dcce13068164496a88042e8b0a668a8e9bf1 -->
<!-- simt:sources=rules/stacks/cloudflare-workers.md -->
<!-- Generated content. Do not edit by hand - your changes are overwritten.
     Run `simt sync --yes` to regenerate, or `simt check` to see what is stale. -->

The rules this package selects on its own. Everything the repository shares with its other packages is in the region at the root.

## Stack: cloudflare-workers

Applies to: `cloudflare-workers` — a `wrangler.json`, `wrangler.jsonc` or
`wrangler.toml`, or a `wrangler` dependency.

### Schema and config

- `wrangler.jsonc` is the single source of binding and environment truth. Do not read
  configuration from a second file.
- Bindings are the integration surface. A Worker calls a service binding, not a REST endpoint
  over the network to its own account.
- Secrets come from the runtime binding. Never commit them, never log them, never accept one
  from a query parameter.

### Data

- D1: one new sequential immutable migration per change. Never edit an applied migration.
  Define coexistence order and forward repair; do not promise SQL rollback.
- Prefer D1 constraints and triggers over read-then-write in application code.
- Prepared, parameterised statements only. Never interpolate a value into SQL.
- Durable Objects: one owner per piece of state. Use alarms for scheduling, and RPC for
  internal calls rather than self-fetch.
- Queues are at-least-once: consumers must be idempotent, keyed by an idempotency token that
  is stable across retries.

### Request handling

- Validate at the edge with the schema layer. Authorization is server-authoritative; a
  client-side role, claim, cache entry or URL parameter grants nothing.
- `Request`/`Response` are globals. Do not pass them through as values into domain code.
- Caching defaults to `private, no-store`. Never cache tokens, account data, admin
  decisions or authenticated errors.
- Streaming is the default for large bodies; do not buffer without a stated reason.

### Module state

- No module-scope mutable state. A Worker instance is reused across requests, so a counter or
  cache at module scope leaks between users.

### Observability

- Structured logs through the logger interface, with an allowlist. Never log bodies, tokens,
  UIDs, user text, exact coordinates or full URLs.
- Use `Math.random` never for anything security-relevant; use Web Crypto.

### Types and tests

- `strict` plus `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`,
  `noImplicitOverride`, `noFallthroughCasesInSwitch`, `noImplicitReturns`, `isolatedModules`.
  With `exactOptionalPropertyTypes`, pass optionals through conditional spreads or an explicit
  `prop: value | undefined`; never `prop?: T` into a `prop: T | undefined` parameter.
- Generate Wrangler types from the config; do not hand-write binding types.
- Test with the real Workers runtime pool (`@cloudflare/vitest-pool-workers`) so Workers APIs,
  D1 and Durable Objects behave as they will in production.

<!-- simt:end -->
