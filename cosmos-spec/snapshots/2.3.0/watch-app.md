# Watch app — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/watch-app

Based on wear inside simt-app. Keep the warmth, remove the spectacle. One reading, one qualifier, one action: enough information to act without asking for sustained attention.

Inherits [Common foundations](https://simt.space/cosmos/design/common).

## The single-glance contract

Section: https://simt.space/cosmos/design/watch-app#glance

Simplicity is a platform requirement.

### W01 — One answer per screen [MUST]

Show one primary value/subject, one short qualifier and one primary action or clear route to it. Use no glass blur, decorative stars, ambient drift, dense map, paragraph card or phone-style bottom dock. Retain Void and the same signal colors in interactive mode; corona is the only ambient gradient a watch screen may use, as one static halo on the bearing or target marker, never under text, and a one-accent screen is valid.

Acceptance: The compass communicates direction and confidence immediately. The sky communicates the selected body and next event. The geofence communicates target distance/state. Additional facts require a deliberate tap/scroll.

Rule: https://simt.space/cosmos/design/watch-app#w01

### W02 — Round geometry [MUST]

Design for 192dp and 228dp round viewports plus the minimum supported device. Place critical content inside a safe central area. At vertical offset y from center, available width is 2√(r²−y²) minus at least 12dp each side; avoid generic rectangular edge padding. Bottom commands use the native edge button/scaffold.

Acceptance: No text, hit region or action is clipped by the circular boundary at the largest supported font. If content does not fit, flow it into a native scaling/transforming scroll list.

Rule: https://simt.space/cosmos/design/watch-app#w02


## Readable and reachable

Section: https://simt.space/cosmos/design/watch-app#type-input

A small display still deserves full-sized targets.

### W03 — Type & controls [MUST]

Use 32–40sp primary readings, 18–20sp titles, 14–16sp body/control labels and 12–14sp secondary qualifiers. Keep units visible beside the reading. Touch targets are ≥48dp, with nonoverlapping geometry and at least 4dp separation where feasible. Use native Wear typography and components mapped to Cosmos roles.

Acceptance: Largest system text preserves values, units and actions through reflow/scroll. A short label may have an accessible expanded name; truncation cannot hide the primary reading, qualifier or destructive consequence.

Rule: https://simt.space/cosmos/design/watch-app#w03

### W04 — Rotary & assistive input [MUST]

Use rotary input for native list scrolling and appropriate value adjustment; expose focus/state and a touch alternative. Keep system swipe-back behavior. Long press is optional, never the sole route. TalkBack receives one coherent instrument description with an explicit refresh/read action.

Acceptance: Touch, rotary and TalkBack complete each essential task. Rotary focus is acquired and released with the active screen. Sensor updates do not flood announcements.

Rule: https://simt.space/cosmos/design/watch-app#w04


## A small set of useful tools

Section: https://simt.space/cosmos/design/watch-app#screens

Each tool has one recognizable visual structure.

### W05 — Home & targets [MUST]

Home is a short native list of Compass, Sky, Geofence, Plumb bob, Targets and Settings where supported. Use an icon, literal name and useful last-state qualifier; category color is secondary. Targets is a bounded readable list with selected state and a detail route. Empty targets offers a clear phone-sync or add path supported by the product.

Acceptance: No orbit carousel or icon-only launch grid. The top item is readable immediately on entry and the last item is reachable by touch/rotary. Synced state is honest about connectivity and freshness.

Rule: https://simt.space/cosmos/design/watch-app#w05

### W06 — Compass & plumb bob [MUST]

Compass uses a simplified dial with no more than four cardinal labels, a clear bearing pointer and one numeric value. Confidence appears as text/icon. Plumb bob uses a stable center/level frame and explicit tilt units. Use no extra precision ticks that compete with the reading.

Acceptance: Known heading/tilt fixtures render correctly, including wrap-around. Calibrating/unavailable/stale states replace the value with honest status; a tap can expose calibration or target selection.

Rule: https://simt.space/cosmos/design/watch-app#w06

### W07 — Sky & geofence [MUST]

Sky shows one selected body with next rise/set event and a concise availability state; the full catalog is a separate list. Geofence shows selected target, distance and inside/outside/unknown state. Cyan denotes actual located/live state, not the entire tool theme. Keep the same canonical Cyan; migrate the legacy watch-specific cyan through theme roles.

Acceptance: Indoor/no-fix/offline/stale and revoked permission states cannot resemble a valid lock. The text names the state, and haptic alerts are supplementary, user-controlled and deduplicated.

Rule: https://simt.space/cosmos/design/watch-app#w07


## Ambient is a different composition

Section: https://simt.space/cosmos/design/watch-app#ambient

When the wrist rests, the interface rests too.

### W08 — Always-on state [MUST]

Provide an explicit ambient layout: pure black background, a sparse muted reading, no filled surfaces/gradient/glow, no seconds, no animation and no touch affordances implying interactivity. Mark the reading time if frozen. Honor low-bit and burn-in requirements and platform-recommended repositioning; ambient is not a dimmed interactive screenshot.

Acceptance: Verify the real ambient lifecycle and supported hardware. UI updates follow platform ambient cadence; no UI frame loop runs. Any background sensing/geofence monitoring is separately justified by the user task and power policy.

Rule: https://simt.space/cosmos/design/watch-app#w08

### W09 — Wake & confidence [MUST]

On return to interactive mode, display the last value as stale until a fresh valid sample arrives. Restore target and navigation state; never flash a bright intermediate surface. A watch face complication/tile exposes only supported concise public-safe facts and opens the corresponding task.

Acceptance: Stale becomes fresh only on new evidence. Interrupted connection and missing phone do not discard the current task. A complication never leaks private target text or exact coordinates without the product authorization.

Rule: https://simt.space/cosmos/design/watch-app#w09


## Battery is part of UX

Section: https://simt.space/cosmos/design/watch-app#performance

Remove work that cannot help the person.

### W10 — Runtime & energy acceptance [MUST]

Use no WebView or phone-scale map engine. Cache static dial geometry; stop presentation subscriptions when hidden and never allocate decorative paths per frame. Target <5% janky frames in a named 30-second interactive journey at the device refresh rate. First useful offline reading should appear ≤1s cold on the baseline watch. Use explicit timeouts for acquisition and phone-sync work.

Acceptance: Record physical watch, OS, refresh rate, build, thermal state and trace. Ambient rendering has zero continuous animation callbacks; retained memory does not grow across repeated tool navigation.

Rule: https://simt.space/cosmos/design/watch-app#w10

### W11 — Power validation [MUST]

Measure a repeatable 30-minute interactive/ambient scenario with fixed brightness, connectivity and sensor policy. Record total drain, incremental cost versus a same-build task-disabled baseline and the approved product battery budget before release. Do not publish an invented universal mAh or percentage promise.

Acceptance: A named owner approves the observed budget on physical hardware. Low power disables ornament first; stopping a user-owned monitoring task requires an explicit visible reason rather than silent disappearance.

Rule: https://simt.space/cosmos/design/watch-app#w11


## Watch acceptance

Section: https://simt.space/cosmos/design/watch-app#release

Check the smallest device, largest text and resting wrist.

### W12 — Wear release matrix [MUST]

Verify home, compass, sky list/detail, geofence, plumb bob, targets and settings in interactive, ambient, low-bit/burn-in where supported, offline and no-phone states. Include 192/228dp, largest text, Arabic, touch, rotary, TalkBack, permission/sensor failure and low battery.

Acceptance: Physical-device screenshots, lifecycle traces, focused instrumentation and power measurements are required for implementation acceptance. Round emulator screenshots alone do not certify glanceability, comfort or battery behavior.

Rule: https://simt.space/cosmos/design/watch-app#w12


