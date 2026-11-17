# Web app — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/web-app

Based on simt-social. The map establishes where; an aligned stream explains what; a context spine keeps the next decision close. The shell stays predictable while the experience becomes richer.

Inherits [Common foundations](https://simt.space/cosmos/design/common).

## A persistent social field

Section: https://simt.space/cosmos/design/web-app#shell

Preserve spatial context without trapping people in a map.

### A01 — Shell geometry [MUST]

At ≥1200px use a 216px labeled rail, flexible discovery field and a 360–420px context pane when selected. At 768–1199px use a 72px rail with accessible names and a 320–360px detail overlay. Below 768px use a 56px top context bar and a ≥64px bottom dock plus safe area. Map/list is a labeled mode switch, not a separate feature implementation.

Acceptance: Dock and rail show Discover, Inbox, Saved and Profile; create remains one stable primary command. The shell accommodates large text through vertical growth or a labeled collapsed layout, never label clipping.

Rule: https://simt.space/cosmos/design/web-app#a01

### A02 — Context spine [MUST]

Place location scope, active time/filter summary and result count on one stable context spine. Selected detail begins with identity, date/time, place precision and participation state; the primary action follows those facts. Keep maps mounted across sheets when geography is part of the task, but release inactive hidden routes.

Acceptance: Closing detail restores the previous selected marker/list row, focus and scroll. Browser back restores context. Filters and result count agree across map and list; manual map exploration never silently turns on follow-location.

Rule: https://simt.space/cosmos/design/web-app#a02


## Discover by place, time or people

Section: https://simt.space/cosmos/design/web-app#discovery

Dense information, with calm priority.

### A03 — Map & list parity [MUST]

Use Petrol land and Water, warm low-contrast roads and labeled category markers. Clusters show counts, the selected marker gets the quasar aperture and a label, current location shows confidence radius plus text. Render map objects with native map layers/feature state; expose the same authorized results in a semantic list. Location permission is requested only when needed.

Acceptance: Every meaningful map action has a list/keyboard equivalent. Marker targets expand to 44px; overlaps resolve through clusters or a chooser. Attribution is visible and not covered. Unavailable tiles leave a useful list and retained filters.

Rule: https://simt.space/cosmos/design/web-app#a03

### A04 — Horizon ribbon [MUST]

Order activity results by actual local start time with visible date groups and explicit time zone when relevant. A compact time ribbon may select a range, paired with an editable date/time control and result summary. Live/stale labels refer to the real data freshness policy, not animation.

Acceptance: DST, past/current/upcoming and locale dates are correct. Manual keyboard/touch date selection is possible. Zero matches explains the active scope and offers reset without discarding unrelated choices.

Rule: https://simt.space/cosmos/design/web-app#a04

### A05 — Activity record [MUST]

Display title, date/time, coarse place, role/category, participation/capacity and owner identity in stable tracks. One primary target opens details; Save and other commands are separate controls. Image ratio is 3:2 for a media card; text rows remain a valid low-data representation.

Acceptance: Long titles wrap to two lines in summary with the full value in detail. Missing images have a purposeful placeholder. Loading, zero results, denied data, offline cached results and removed activities keep a coherent layout.

Rule: https://simt.space/cosmos/design/web-app#a05


## Honest participation

Section: https://simt.space/cosmos/design/web-app#participation

An action is meaningful only when its result is clear.

### A06 — Detail & participation states [MUST]

Design available, pending approval, confirmed, waitlisted, full, cancelled, finished and unavailable states where the backend supports them. Distinguish requesting participation from being accepted. Place consequences, privacy and location precision beside the action. Do not invent a backend capability to satisfy a visual.

Acceptance: The UI maps each supported server state explicitly; unsupported target capabilities are recorded as contract dependencies. Pending mutation is guarded, failure rolls back safely, and confirmed counts come from authoritative data.

Rule: https://simt.space/cosmos/design/web-app#a06

### A07 — Saved, profile & inbox [MUST]

Reuse record geometry for Saved; profile opens with identity and authorized roles before activity rows. Inbox separates actionable unread events from history, with text and a leading marker rather than glow. Timeline messages preserve drafts on failure and retain read-only/removed/access-revoked states.

Acceptance: Read/unread changes update counts consistently. Public profiles are permission-safe. Owner-only controls are absent for other users; denied routes provide recovery without leaking private conditions.

Rule: https://simt.space/cosmos/design/web-app#a07


## Compose with continuity

Section: https://simt.space/cosmos/design/web-app#composer

Keep the idea intact while people solve the details.

### A08 — Four-step composer [MUST]

Use Idea → Moment → Place → Review, an always-readable progress spine and a concise draft summary. Each step validates its owned fields; users can go back without losing safe values. Preserve product capacity/range limits from the real API contract. Show exact/approximate place choice and its audience consequences in Place and Review.

Acceptance: No false autosave claim: label saved only after the supported persistence succeeds. Draft resume, discard, upload progress/failure, permission denial, version conflict and submit failure are designed. Review displays the exact publish audience and location precision.

Rule: https://simt.space/cosmos/design/web-app#a08

### A09 — Forms & destructive changes [MUST]

Use a 640px reading/form maximum, stable labels and grouped sections separated by rules. Sticky submit belongs to the form and respects keyboard/safe area. Confirm deletion with object name and consequence, without coercive wording. For stale concurrent edits, present an explicit refresh/review choice and preserve the local draft.

Acceptance: Submission errors link to fields; network failure retains input. Double activation cannot create two activities. Disabled controls state the unmet requirement when a person can resolve it.

Rule: https://simt.space/cosmos/design/web-app#a09


## A precise operational console

Section: https://simt.space/cosmos/design/web-app#operations

The same palette, with quieter structure and stronger evidence.

### A10 — Moderation & administration [MUST]

Use semantic tables with direct status labels, stable column alignment, consequence summaries and traceable decisions. Separate queue, subject evidence and decision area; never put each cell in a card. Honor the existing desktop-only moderation policy on compact widths with a useful desktop-required explanation.

Acceptance: Unauthorized tools do not render. Permission revocation fails closed. Conflict, stale evidence, audit failure and terminal state block unsafe actions. Sensitive evidence never appears in public previews, telemetry or exports without authorization.

Rule: https://simt.space/cosmos/design/web-app#a10


## A responsive working set

Section: https://simt.space/cosmos/design/web-app#performance

Responsiveness includes memory, map work and recovery.

### A11 — Loading & runtime budgets [MUST]

Target ≤180KiB gzip initial app JavaScript excluding the deferred map engine, ≤50KiB gzip shared CSS and ≤120KiB initial fonts. Render a useful list/shell before map readiness; defer admin/composer/media code. Bound visible records, images and map sources to the task; virtualization must preserve accessibility, focus and findability.

Acceptance: On a named baseline device, repeated filter/detail/back cycles do not cause monotonic retained-memory growth. Map and list retain selection. Long tasks are diagnosed; field Core Web Vitals meet common targets before release acceptance.

Rule: https://simt.space/cosmos/design/web-app#a11

### A12 — Secure rendering & recovery [MUST]

SSR public content and safe shell data only; authenticate before rendering private payloads. Authenticated responses default to private, no-store unless a reviewed isolation policy permits caching. Offline data is shown only when policy permits, with timestamp and safe recovery. Never trade data isolation for instant-looking transitions.

Acceptance: Multi-account switching cannot reveal previous private data. Rendered errors are localized and safe; technical error IDs may appear only as support metadata. Retry is bounded and cancel/abort is respected.

Rule: https://simt.space/cosmos/design/web-app#a12


## Web app acceptance

Section: https://simt.space/cosmos/design/web-app#release

Ship a complete task slice, including the hard states.

### A13 — Route matrix [MUST]

Review sign-in/onboarding, map/list discovery, detail/participation, create/edit, saved, inbox/timeline, profile/settings, account states, share/unavailable and authorized admin routes. Compare 390, 768, 1024 and 1440px, Arabic RTL, large text, forced colors and every motion tier. Exercise keyboard and screen-reader flows with real supported contract fixtures.

Acceptance: Evidence names the direct composition, build and tested routes. A missing denied/offline/conflict state or inaccessible map-only task blocks the affected feature; unrelated routes are not claimed validated.

Rule: https://simt.space/cosmos/design/web-app#a13


