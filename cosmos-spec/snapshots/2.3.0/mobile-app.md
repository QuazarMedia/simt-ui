# Mobile app — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/mobile-app

Based on simt-app. Put the reading in a stable reference frame, the explanation beside it and the command under the thumb. A sense of wonder follows precision.

Inherits [Common foundations](https://simt.space/cosmos/design/common).

## The instrument field

Section: https://simt.space/cosmos/design/mobile-app#anatomy

The screen explains its measurement before asking for an action.

### M01 — Three useful zones [MUST]

Compose a native context header, flexible instrument field and bottom command zone. The header names the tool, target and confidence/connection state. The field prioritizes one reading with units and a real reference frame; it owns the screen sky, and its quasar aperture marks the target. The command zone contains the primary action and essential secondary controls above gesture/navigation insets.

Acceptance: On compact portrait, the reading and command are both reachable. At fontScale 2.0, switch to a scrollable reading/controls composition rather than clipping the dial or shrinking text.

Rule: https://simt.space/cosmos/design/mobile-app#m01

### M02 — Navigation & continuity [MUST]

Use the existing product destinations and native back stack. Stable destinations may use a bottom bar; tool commands do not become destinations. Preserve target and measurement context across sheets and tool switches where appropriate. Use standard predictive back, native IME behavior and one modal dispatcher.

Acceptance: System back, gesture back and explicit Back agree. Close dismisses only its layer. Deep links handle missing permission, invalid target and authentication safely and preserve an understandable return path.

Rule: https://simt.space/cosmos/design/mobile-app#m02


## Fixed frame, living reading

Section: https://simt.space/cosmos/design/mobile-app#instruments

Design instruments that are legible outside a studio.

### M03 — Compass & bearing [MUST]

Keep the physical north/reference semantics consistent. A dial may rotate with heading while the target bearing is labeled; define that convention visibly. Use direct cardinal labels, a distinct current-direction pointer, target marker and numeric delta. Reserve Cyan for actual live/location state and Gold for precision.

Acceptance: Known headings 0/90/180/270 and 359→0 transitions are correct. True/magnetic reference and accuracy are explicit. Arabic changes control order, never the sensor mathematics. No reading is presented as valid during missing/calibrating/stale state.

Rule: https://simt.space/cosmos/design/mobile-app#m03

### M04 — Sky, moon & orrery [MUST]

Use one inspectable field with selected-body details adjacent, labeled rise/set/time zone and actual visibility conditions. Orbital paths encode real relationships and always have a list/text equivalent. Red mode preserves dark adaptation across menus, tooltips and loading. These fields own no decorative star layer: every mark is data.

Acceptance: Data timestamps and unavailable calculation states are explicit. Small marks get expanded hit regions or a chooser. Screen readers can select a body and read its principal facts without exploring pixels.

Rule: https://simt.space/cosmos/design/mobile-app#m04

### M05 — Map, tracks & links [MUST]

Use map-native geometry, visible scale/attribution, trace direction and labeled start/current/end states. Tracking distinguishes running, paused, stopping, saved and failed persistence. A share/link preview makes target precision and audience clear before leaving the device.

Acceptance: Location denial, coarse-only permission, unavailable tiles, offline coverage, sensor interruption and process restoration all have designed paths. Saving success is based on durable persistence, not an animation finishing.

Rule: https://simt.space/cosmos/design/mobile-app#m05


## Native affordances, Cosmos clarity

Section: https://simt.space/cosmos/design/mobile-app#components

Keep learned Android behavior and refine its visual hierarchy.

### M06 — Component profile [MUST]

Map semantic tokens into one shared Compose theme. Take mobile radii from the platform mapping: 8dp controls, 10dp secondary inline, 14dp emphasis, 20dp records and 28dp sheets. Standard touch controls are ≥48dp; body text 16sp; supporting 14sp. Prefer native switches, pickers, date/time input and selection semantics over custom gesture-only controls. Haptics are optional and respect system settings.

Acceptance: All actionable custom drawings expose merged meaningful semantics and explicit actions. Long press and haptics are never the sole means of discovering or confirming a command.

Rule: https://simt.space/cosmos/design/mobile-app#m06

### M07 — Sheets & reach [MUST]

Detail sheets retain the visible instrument reference; entry forms may expand to full-height. Keep sheet title, close affordance and action track explicit. Apply IME, display cutout, system bar and gesture insets using current platform APIs. Avoid placing a destructive action in the primary thumb position.

Acceptance: Keyboard opening never covers the focused field or submit. Landscape and largest text keep all actions reachable. TalkBack focus enters and exits layers predictably.

Rule: https://simt.space/cosmos/design/mobile-app#m07


## Adapt to the available field

Section: https://simt.space/cosmos/design/mobile-app#adaptive

Posture changes should reveal context, not invent another product.

### M08 — Window classes [MUST]

Use available window size, not device model. Below 600dp use one column; 600–839dp uses a rail or tool/content split when content fits; ≥840dp shows instrument plus context/detail. Short landscape prioritizes reading and compact commands side by side. Keep semantic reading order continuous.

Acceptance: Verify 360×800dp, 600×960dp, 840×900dp and 800×360dp plus resizing. No mandatory pane narrower than its readable minimum; fold hinges never cover controls.

Rule: https://simt.space/cosmos/design/mobile-app#m08

### M09 — Fold & posture [MUST]

For a supported tabletop posture, place the instrument above the hinge and controls below; for a book posture place field and details on separate usable panes. Detect posture from the supported window API and provide a single-pane fallback. Do not claim a posture is implemented solely because the layout is drawn here.

Acceptance: Posture changes retain target, selection and safe draft state. Real hinge and emulator checks confirm separation; absent detection falls back to width-based layout.

Rule: https://simt.space/cosmos/design/mobile-app#m09


## Accurate data, humane permissions

Section: https://simt.space/cosmos/design/mobile-app#sensors

Show people how much to trust a reading.

### M10 — Sensor presentation contract [MUST]

Separate acquisition rate, computation and UI sampling. Select rates from tool accuracy needs; render at most once per display frame and throttle values that need not update at that rate. Show source, freshness and confidence. Visual damping must be bounded and must not change stored measurements or claim precision beyond the source.

Acceptance: Test unavailable sensor, interference, calibration, stale samples, lost GPS and restored source. TalkBack announces on request or meaningful coarse changes, not every sample. Pausing the UI does not falsify an ongoing recording indicator.

Rule: https://simt.space/cosmos/design/mobile-app#m10

### M11 — Permissions & privacy [MUST]

Ask for permission at the point of need with the outcome explained. Design granted, approximate/coarse, denied, permanently denied and unavailable states. Background acquisition is limited to explicit user-owned tasks with platform-required indication and a stop control. Store/export precise data only within the product privacy policy.

Acceptance: Permission revocation during use resolves safely. Settings recovery has a clear path back. Analytics and support output contain no coordinates, sensor payloads, private target labels or draft text.

Rule: https://simt.space/cosmos/design/mobile-app#m11


## Native performance contracts

Section: https://simt.space/cosmos/design/mobile-app#performance

A beautiful reading should not compete with the sensor.

### M12 — Frame & startup acceptance [MUST]

On a named lowest-supported physical device, meet the active display deadline: 16.67ms at 60Hz, 11.11ms at 90Hz, 8.33ms at 120Hz. Target <5% janky frames in a repeatable 30-second instrument/map journey. Target cold first-useful frame ≤1.5s and warm ≤500ms for offline-capable tools, measured with Macrobenchmark; network content reports a separate readiness metric.

Acceptance: Record device, OS, refresh rate, thermal state, build and traces. No main-thread I/O or per-frame path/particle allocation. The benchmark includes real input and sensor fixtures; a static screenshot does not prove pacing.

Rule: https://simt.space/cosmos/design/mobile-app#m12

### M13 — Lifecycle & memory [MUST]

Cache static paths/gradients and compose with stable models. Subscribe to sensor presentation only for the active lifecycle; release hidden map/media resources. Reduce cosmetic work first under battery saver/thermal pressure. Persistent recording follows its explicit service contract and does not depend on a visible composable.

Acceptance: Repeated tool/sheet navigation does not grow retained allocations. Background/foreground and process death preserve only the intended durable state. Red/Reduced/Static do not affect saved data or task availability.

Rule: https://simt.space/cosmos/design/mobile-app#m13


## Mobile acceptance

Section: https://simt.space/cosmos/design/mobile-app#release

Verify real instruments on real hands and hardware.

### M14 — Native release matrix [MUST]

Review compass, target detail/link, map, sky/moon/orrery, tracking, toolkit, settings, permissions and share flows. Test light/dark/red where supported, compact/expanded/landscape/fold, Arabic RTL, fontScale 2.0, TalkBack, switch access, motion tiers, low power and sensor/permission failure.

Acceptance: Capture screenshots and traces from supported physical devices, plus adaptive emulators. Compile, instrumented navigation/semantics tests and focused Macrobenchmark evidence are required for implementation acceptance.

Rule: https://simt.space/cosmos/design/mobile-app#m14


