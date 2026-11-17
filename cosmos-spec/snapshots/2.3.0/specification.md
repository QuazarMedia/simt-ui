# Cosmos Design System 2.3.0

Shared foundations plus four platform parts: web page, web app, mobile app and watch app.

The web product and the mobile product are built on this specification. Conformance is recorded rule by rule in a release record; examples use illustrative data.

## What changed in 2.3.0

The 79 changes in this release are listed in [changelog.md](https://simt.space/cosmos/design/changelog.md), which carries every published release newest first.

# Common foundations — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/common

Warm depth, precise signals and generous quiet. Every platform shares the same meanings; each gives the task the space and input it needs.

Inherited by all four platform parts.

## Identity & authority

Section: https://simt.space/cosmos/design/common#identity

A useful spatial system, with room for imagination.

### C01 — Task before spectacle [MUST]

Put the primary object, current state and next action in the first useful viewport. A decorative motif may frame them but cannot displace them. Use literal operational copy even when the visual language is celestial.

Acceptance: In grayscale, a first-time reader can identify the task, state and next step. Removing all ornament leaves a complete interface.

Rule: https://simt.space/cosmos/design/common#c01

### C02 — Shared core, four adapters [MUST]

Apply these common rules and the relevant platform part together. Product safety, privacy and accessibility requirements take precedence. Older prototypes and repository guides are migration evidence where they disagree with v2.

Acceptance: Each implementation brief names version 2.3.0, platform, applicable rule IDs, owner and any explicit exception with reason, evidence and review date.

Rule: https://simt.space/cosmos/design/common#c02

### C03 — Original composition [MUST]

Build visual identity from the focus aperture, context spine and horizon ribbon. An aperture frames one meaningful object and is drawn as a quasar: a solid core in the object's channel, a corona halo and, where space allows, an accretion ring. A spine connects current context to the next step; a ribbon orders actual time or distance. Compose with continuous fields and aligned rows before adding cards.

Acceptance: Every motif names the information it carries. One quasar aperture per screen marks the focal object. Cards contain individual records; no nested record cards or identical boxes around every page section.

Rule: https://simt.space/cosmos/design/common#c03


## Color with a job

Section: https://simt.space/cosmos/design/common#palette

The original Deep Space colors are preserved exactly.

### C04 — Stable semantic channels [MUST]

Use Ember for primary agency, Rose for social attention, Violet for identity, Gold for focus/time/value, Cyan for live/location state and Moss for nature. Danger and Success are separate status channels. Limit competing large accents to one dominant and two supporting channels; categorized data may use the spectrum with labels.

Acceptance: Tokens resolve to the anchors below. A hue never supplies the only meaning. Category Moss cannot imply a successful operation; Rose cannot imply an error.

Rule: https://simt.space/cosmos/design/common#c04

### C05 — Contrast is composited [MUST]

Require 4.5:1 normal text, 3:1 large text and 3:1 essential control boundaries, focus and graphical objects against adjacent colors. Use boundary for necessary input outlines; line is decorative only. Check every gradient stop, translucent surface and hover/disabled distinction.

Acceptance: Measure the final composited pairs in each theme. The measurements are published once at contrast-pairs.json; fetch that document when introducing a colour, pairing a translucent or gradient surface or checking a theme, and do not expect a token payload to carry them. Document any inactive-control exception. Faint is supporting text only on a passing surface; Dim is never needed to complete a task.

Rule: https://simt.space/cosmos/design/common#c05

### C06 — Daylight, red and ambient [MUST]

Offer Daylight Nebula where supported. Dark remains the signature; a persisted explicit choice wins over the system preference, and with no choice the system preference is followed. Astronomy red is user-selected for instruments, uses only red luminance and retains text contrast. Watch ambient may use pure black for OLED efficiency. Theme adapters preserve the brand anchors and alter semantic role mappings, and an accent used as text takes the profile's accent-text role rather than its fill role.

Acceptance: Theme switches retain layout, focus, selection and data. Red never flashes white/cyan on entry, dialogs or loading. Dark controls use dark on-action text; the existing Daylight Ember requires dark text, not assumed white. Test astronomy text on its action fill.

Rule: https://simt.space/cosmos/design/common#c06

### C29 — One colour truth, two notations [MUST]

Author and verify colour in sRGB and publish an oklch projection for the web parts. The anchors, gradients, shadows and semantic roles stay sRGB in the specification, because contrast is defined on sRGB luminance and every published contrast pair is measured there. Web tokens may be written in oklch from the projection in platformAdapters, so one role can step lightness without re-picking a hue. Android and Wear read sRGB: neither toolkit has an Oklab colour space, so a converted value would be a hex with extra steps.

Acceptance: Every published web oklch resolves back to the sRGB value it came from within one 8-bit step, and every contrast claim is measured against the sRGB value. A colour is changed once, in hex, and both notations move together; no theme or component carries a colour absent from the registry.

Rule: https://simt.space/cosmos/design/common#c29

### Deep Space token registry

- Token: void; sRGB: #120A14; oklch (web): oklch(16.10% 0.0242 319.45deg); Role: Page and immersive field
- Token: void-edge; sRGB: #1F1126; oklch (web): oklch(20.77% 0.0447 313.68deg); Role: Depth at the edge of the field
- Token: hull; sRGB: #1D1220; oklch (web): oklch(20.34% 0.0317 318.9deg); Role: Standard opaque surface
- Token: hull-raised; sRGB: #2A1830; oklch (web): oklch(24.45% 0.0506 317.34deg); Role: Raised or selected surface
- Token: hull-highest; sRGB: #352040; oklch (web): oklch(28.55% 0.0626 313.19deg); Role: Highest temporary surface
- Token: ember; sRGB: #FF8C42; oklch (web): oklch(75.36% 0.1637 50.4deg); Role: Primary agency: create, continue, commit
- Token: ember-soft; sRGB: #FFB27C; oklch (web): oklch(82.54% 0.1139 55.48deg); Role: Supporting warm emphasis
- Token: rose; sRGB: #FF7E8A; oklch (web): oklch(74.38% 0.1570 15.89deg); Role: Social participation and attention
- Token: violet; sRGB: #C9A0FF; oklch (web): oklch(77.84% 0.1380 303.2deg); Role: Identity, roles and transformation
- Token: gold; sRGB: #FFC766; oklch (web): oklch(86.11% 0.1307 79.28deg); Role: Focus, time, precision and value
- Token: cyan; sRGB: #00E5FF; oklch (web): oklch(84.42% 0.1457 209.29deg); Role: Live, connected and located
- Token: moss; sRGB: #83B692; oklch (web): oklch(73.03% 0.0750 153.51deg); Role: Outdoor and natural categories
- Token: danger; sRGB: #FF6978; oklch (web): oklch(71.17% 0.1827 17.02deg); Role: Destructive action and failure
- Token: success; sRGB: #71D5A4; oklch (web): oklch(79.88% 0.1186 160.71deg); Role: Confirmed completion
- Token: petrol; sRGB: #102C2D; oklch (web): oklch(27.21% 0.0338 198.5deg); Role: Map ground
- Token: water; sRGB: #123D42; oklch (web): oklch(33.30% 0.0470 205.97deg); Role: Map water
- Token: ink; sRGB: #FFF8F2; oklch (web): oklch(98.29% 0.0110 63.36deg); Role: Primary text on Deep Space
- Token: muted; sRGB: #BBAEBB; oklch (web): oklch(76.57% 0.0233 325.8deg); Role: Supporting text
- Token: faint; sRGB: #9A8B9A; oklch (web): oklch(65.48% 0.0280 325.89deg); Role: Secondary metadata; check actual surface
- Token: dim; sRGB: #90839A; oklch (web): oklch(62.93% 0.0371 310.51deg); Role: Inactive ornament; not essential text
- Token: boundary; sRGB: #9A8B9A; oklch (web): oklch(65.48% 0.0280 325.89deg); Role: Essential control outlines on dark surfaces

## Light with a job

Section: https://simt.space/cosmos/design/common#light

Eight gradients and two shadows. Each one is a named instrument, never a texture.

### C27 — Gradient & glow grammar [MUST]

Use only the published gradient and shadow tokens, each for its stated job: field for root depth, nebula for one atmospheric field, corona and accretion for the single focal object, horizon for 1–2 px light lines, rim for the border light of the focal or selected surface, action for the one primary action and spectrum for the brand signature. A screen has at most one corona, one accretion ring and one nebula. Light carries depth and focus, never status, selection or the only boundary, and text never takes a gradient fill. In Full tier the hero figure runs one ambient motion: the focal object's accretion disc revolves once every 40–90 s about the core, and every other light is static — including the accretion ring itself, because a boundary that moves stops being a boundary. Sky drift (C11) is the one alternative a screen may take instead. Deep Space uses the whole grammar, Daylight keeps horizon and spectrum, and astronomy red and watch ambient use none. A gradient token a profile does not run resolves to no gradient, so the element falls back to its flat role color instead of borrowing another profile's light.

Acceptance: Every gradient and shadow in code resolves to a token name and its published geometry. Text over a gradient passes against every stop as an opaque pair. Removing all light leaves hierarchy, selection and boundaries intact. The revolving disc moves only `rotate` or `transform`, never a length, and is the only animation on the screen. Forced colors and astronomy red replace light with solid system or semantic borders.

Rule: https://simt.space/cosmos/design/common#c27

### Gradient registry

- Token: gradient.field; Geometry: Radial 120% × 90% at 50% 0%; Stops: void-edge → void @72%; Job: Root depth behind every Deep Space screen. Opaque, so text is measured against void-edge and void.; Profiles: deepSpace
- Token: gradient.nebula; Geometry: Radial 50% × 50% at 50% 50%; Stops: violet 26% → rose 13% @36% → ember 6% @64% → ember 0%; Job: One atmospheric field per screen, behind the focal object or a section entry; never behind reading text.; Profiles: deepSpace
- Token: gradient.corona; Geometry: Radial 50% × 50% at 50% 50%; Stops: gold 90% → ember 45% @14% → rose 12% @42% → rose 0%; Job: The only glow: the halo of the single focal object, painted in a box four to six times its diameter.; Profiles: deepSpace
- Token: gradient.accretion; Geometry: Conic from 0° at 50% 50%; Stops: ember → gold @25% → rose @50% → violet @75% → ember; Job: A 1–2 px ring around the focal object. Never progress, status or a control outline.; Profiles: deepSpace
- Token: gradient.horizon; Geometry: Linear 90°; Stops: ember 0% → ember @20% → gold @50% → violet @80% → violet 0%; Job: Light lines of 1–2 px: the top light of a surface, the horizon ribbon, a section signature.; Profiles: deepSpace, daylight
- Token: gradient.rim; Geometry: Linear 135°; Stops: ember 55% → ink 10% @45% → violet 50%; Job: A 1 px border light for the focal or selected surface, beside its essential boundary rather than replacing it.; Profiles: deepSpace
- Token: gradient.action; Geometry: Linear 180°; Stops: ember-soft → ember @55%; Job: Fill of the one primary action. The Void label passes on every stop.; Profiles: deepSpace
- Token: gradient.spectrum; Geometry: Linear 90°; Stops: ember → rose @20% → violet @40% → gold @60% → cyan @80% → moss; Job: The brand signature line. Category legends use solid labelled swatches instead.; Profiles: deepSpace, daylight

## Stars at the edge

Section: https://simt.space/cosmos/design/common#atmosphere

One deterministic sky per screen, generated the same way on every platform.

### C11 — One deterministic sky [MUST]

A screen, hero, panel or dialog background may own one edge star layer from the published starfield algorithm: stars only in the outer 20% band, area / 7500 stars clamped 24–160 for screens and area / 10000 stars clamped 12–72 for dialogs, three magnitudes, at most 10% of the requested layer in any one accent so Ink is always the majority, and at most 3 diffraction-spiked stars. Seed it from a stable surface ID and a 160 px/dp size bucket. Exclude known text and controls by authoring the region per surface, in the same normalised coordinates the stars are placed in, and never reseed to avoid them. Records, maps, media, data canvases, sky charts and watch screens own no stars.

Acceptance: The same surface ID and bucket reproduce the published fixture on every platform. The layer is one cached bitmap, texture or compositor layer, never an element or view per star. Full tier may drift the whole layer once per 90–160 s by at most 1.5% of the shorter edge; Reduced, Static, Save-Data and hidden states freeze it, and low power draws 40% of the stars. Removing the layer changes no semantics or available task.

Rule: https://simt.space/cosmos/design/common#c11

### Atmosphere by surface

- Surface: Web page hero; Field & nebula: Field and one nebula behind the focal object; Star layer: Screen density; masked from the narrative column; Focal light: Corona and accretion on the emblem or selected record; Lines: Horizon top light; spectrum signature
- Surface: Web page reading; Field & nebula: Field only; Star layer: None; Focal light: None; Lines: Horizon section signature
- Surface: Web app shell; Field & nebula: Field; no nebula; Star layer: Dialog density on sign-in, onboarding and empty states; never over a map or list; Focal light: Corona on the selected marker; Lines: Horizon top light on the context pane
- Surface: Mobile instrument; Field & nebula: Field and one Full-tier nebula; Star layer: Screen density, cached once per screen; Focal light: Corona and accretion on the target; Lines: Horizon top light on sheets
- Surface: Mobile sheet & dialog; Field & nebula: Inherits the screen field; Star layer: Dialog density; Focal light: None; Lines: Horizon top light; rim on the focal sheet
- Surface: Mobile sky & orrery; Field & nebula: Field only; Star layer: None: every mark is data; Focal light: Corona on the selected body; Lines: None
- Surface: Watch interactive; Field & nebula: Solid Void; Star layer: None; Focal light: One static corona on the bearing or target marker; Lines: None
- Surface: Watch ambient; Field & nebula: Pure black; Star layer: None; Focal light: None; Lines: None
- Surface: Daylight; Field & nebula: Flat background; Star layer: None; Focal light: None; Lines: Horizon and spectrum
- Surface: Astronomy red; Field & nebula: Flat red background; Star layer: None; Focal light: None; Lines: Solid red boundaries only

## Readable at every scale

Section: https://simt.space/cosmos/design/common#typography

A confident display voice, with a quieter reading voice.

### C07 — Families & language [MUST]

Use Space Grotesk for Latin titles and instrumentation, Source Sans 3 for web reading and form text, native system body type on Android, and El Messiri for Arabic roles. Bundle fonts, declare fallbacks and reserve fallback geometry. Use zero tracking by default; uppercase and spaced labels are Latin-only enhancements.

Acceptance: No third-party font requests. Arabic has no mixed-script heading caused by an unsupported font. Font fallback and final font both retain unclipped text and controls.

Rule: https://simt.space/cosmos/design/common#c07

### C08 — Roles & number semantics [MUST]

Use the roles below and rem/sp for text. Body line-height is 1.5–1.65; Arabic uses at least 1.6 with tested diacritic room. Reading columns are 55–72 characters. Instrument digits use tabular numerals, visible units and stable width; precision follows actual sensor/data accuracy.

Acceptance: Web reflows at 400% on a 1280 CSS px viewport and text zoom at 200%; Android fontScale 2.0; watch largest system text. No essential label is clipped or replaced by an ellipsis-only value.

Rule: https://simt.space/cosmos/design/common#c08

### Type roles; size / line height

- Role: Narrative display; Web: 48–88 / 1.05, fluid rem; Mobile: 40–48sp / 1.1, entry only; Watch: Not used
- Role: Primary instrument; Web: 48–72 / 1.1; Mobile: 48–64sp / 1.1; Watch: 32–40sp / 1.1
- Role: Page title; Web: 28–36 / 1.2; Mobile: 28–32sp / 1.2; Watch: 18–20sp / 1.25
- Role: Section title; Web: 20–24 / 1.3; Mobile: 20–24sp / 1.3; Watch: 16–18sp / 1.3
- Role: Body; Web: 16–18 / 1.6; Mobile: 16sp / 1.5; Watch: 14–16sp / 1.4
- Role: Supporting; Web: 14 / 1.5; Mobile: 14sp / 1.45; Watch: 12–14sp / 1.4
- Role: Control label; Web: 14–16 / 1.3; Mobile: 14–16sp / 1.3; Watch: 14–16sp / 1.3

## Field, spine, aperture

Section: https://simt.space/cosmos/design/common#geometry

Geometry should organize attention, not create visual noise.

### C09 — Rhythm & mapped shape [MUST]

Use 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128 units. One unit is a CSS px or native dp, so the same numbers serve every platform and none of them publishes a spacing scale of its own. Take corner geometry from the platform mapping published in tokens.json rather than from a local scale: 8 px control / 16 px record / 24 px overlay on web, 8 dp control / 10 dp secondary / 14 dp emphasis / 20 dp record / 28 dp overlay / round circle on Android, and the native round and capsule shapes on Wear, which publish no dp radius at all. A part that needs another role adds it to the mapping first. Circles denote instruments or compact icon controls, not paragraph containers.

Acceptance: Spacing and shape map to named tokens, and a component's corner radius matches its role in the mapping for the part it ships in. No screen carries an ad-hoc px or dp corner value, and a native shape is expressed as the native shape rather than approximated with a number. The Wear layout fits its round screen instead of shrinking its targets. Small-screen compression first removes ornament and gutters; it does not shrink the readable type or hit target.

Rule: https://simt.space/cosmos/design/common#c09

### C10 — Surface budget [MUST]

Use opaque Hull for reading and work. A panel, sheet or dialog may carry a horizon top light inset from its corners; only the focal or selected surface adds a rim border, which is the answer C27 gives. One header or contextual overlay may sit over imagery at no less than 88% opacity with an opaque fallback, in the highest surface role and never in a colour of its own. Allow at most one backdrop blur region per visible screen, capped at 12px on web; disable it when measured costly. Borders establish hierarchy before light.

Acceptance: The screen remains legible with blur unsupported or disabled. No live blur behind scrolling long copy, dialog inside a blurred sheet, or full-screen animated haze. Top light never touches a rounded corner at full intensity.

Rule: https://simt.space/cosmos/design/common#c10

### C25 — Icons, imagery & diagrams [MUST]

Use one consistent icon family per platform: Lucide on web or the established native library. Standard marks are 20–24px/dp with a consistent stroke; the target remains 44px/48dp. Custom marks are reserved for concepts without a familiar equivalent. Images show the real object/place/person with a declared crop and reserved aspect ratio; diagrams have text equivalents.

Acceptance: Decorative icons are hidden from assistive technology. Icon-only controls have localized names; unfamiliar commands retain visible labels. Mirror navigational arrows only. No duplicate icon font, raster text, misleading geographic imagery or unsupported scientific precision.

Rule: https://simt.space/cosmos/design/common#c25

### C26 — Layering & scroll ownership [MUST]

Keep field, persistent chrome, contextual popover, modal and urgent feedback in that semantic order using a shared layer registry. Give each pane one scroll owner; the whole page reflows before adding nested scrolling. Bounded overflowing lists expose native scrolling and a visible edge/next-item or explicit navigation affordance. A fade cannot cover essential labels or intercept input.

Acceptance: Popover/dialog/focus are never clipped by a record or overflow ancestor. Touch, wheel, keyboard and rotary reach all content. Scroll position and focus survive return from detail; closing a layer restores the correct owner. Reduced mode uses static scroll cues.

Rule: https://simt.space/cosmos/design/common#c26

### Platform geometry mapping

- Role: Control — compact control, chip, field; Web page / web app: 8 px; Android: 8 dp; Wear: round
- Role: Secondary — inline affordance; Web page / web app: —; Android: 10 dp; Wear: —
- Role: Emphasis — inline emphasis; Web page / web app: —; Android: 14 dp; Wear: —
- Role: Record — card or list row; Web page / web app: 16 px; Android: 20 dp; Wear: round
- Role: Overlay — sheet or dialog; Web page / web app: 24 px; Android: 28 dp; Wear: capsule
- Role: Circle — instrument or compact icon control; Web page / web app: —; Android: round; Wear: round

## Component contracts

Section: https://simt.space/cosmos/design/common#components

The same behavior travels; shape and density adapt.

### C12 — Actions & selection [MUST]

Give a task region one visually dominant Ember action, filled with the action gradient and resting on the action shadow. Destructive actions use Danger and consequence-first confirmation where irreversible. Buttons expose default, hover, pressed, focus, pending, disabled, error and confirmed states. Selection uses shape/check plus aria-pressed, aria-selected or native semantics, while Gold focus is independent.

Acceptance: Click/Enter/Space trigger once. Pending preserves the accessible name and width, exposes busy state and prevents duplicates. Confirm only after success; error restores safe retry without losing input.

Rule: https://simt.space/cosmos/design/common#c12

### C13 — Inputs & validation [MUST]

Keep visible labels; connect hint, requirement, counter and error. Use real input/select/radio/checkbox/switch semantics. A nonempty editable single-line field exposes a 44px web/48dp native Clear button that returns focus to the field. Do not add it to password, read-only or disabled fields. Validate on submit or meaningful exit without announcing every keystroke.

Acceptance: Keyboard autofill, paste, password managers, IME and RTL text work. Invalid fields expose invalid state and a text error; submit focuses the first error or linked error summary. Safe values survive retries.

Rule: https://simt.space/cosmos/design/common#c13

### C14 — Records & navigation [MUST]

Keep records in aligned rows unless media/object identity justifies a card. Do not wrap nested links/buttons in another interactive target. Back follows history; Close dismisses a layer. Selected navigation combines label, boundary and programmatic current state. Navigation persists where the task does.

Acceptance: Tab order matches reading order. Route navigation moves focus to a named destination heading; dialogs return focus to a surviving trigger. Back/forward restore filter, position and context as applicable.

Rule: https://simt.space/cosmos/design/common#c14

### C15 — Dialog, sheet & popover [MUST]

Use one modal layer with a visible title, Close action, safe dismissal policy and actions in reading order. Web uses native dialog or an audited equivalent with focus containment, Escape and background inertness. Mobile uses platform back and inset-aware sheets. Popovers anchor to triggers and reflow before viewport clipping.

Acceptance: Focus cannot enter the background of a modal. Unsaved edits get an explicit discard decision. Last action is reachable with the keyboard open and large text. Tooltips never contain the only critical instruction.

Rule: https://simt.space/cosmos/design/common#c15

### C16 — Feedback at the source [MUST]

Use inline errors for local failures, a banner for screen-wide degraded state, and status/toast for confirmed transient results. Every primary surface designs first load, refresh/stale, empty, denied, offline, recoverable error, terminal unavailable and success. Loading reserves real geometry; shimmer is optional and never indefinite.

Acceptance: Screen readers hear concise changes once; high-rate sensor updates are not live-announced each frame. Error copy says what happened, what remains safe and which action recovers. A toast is never the sole record of a failure.

Rule: https://simt.space/cosmos/design/common#c16

### Shared state matrix

- State: Default · C12; Visible treatment: Opaque surface, legible label, essential boundary; Behavior / announcement: Real enabled semantics; Opted out by: every component declares it
- State: Hover · C12; Visible treatment: Tonal lift; no movement of layout; Behavior / announcement: Optional; a pointer that cannot hover simply never shows it; Opted out by: 12 × notInteractive
- State: Pressed · C12; Visible treatment: Inset tone; no movement of layout; Behavior / announcement: Equivalent to hover for touch and keyboard; Opted out by: 7 × notInteractive
- State: Focus · C12, C19; Visible treatment: 2px Gold ring + dark separation, 3px offset; Behavior / announcement: Visible, unclipped and not obscured by sticky UI; Opted out by: 10 × notFocusable
- State: Pending · C12; Visible treatment: Stable label and small progress mark; Behavior / announcement: Busy, duplicate guard; cancel if operation allows; Opted out by: 6 × notAsync
- State: Disabled · C12; Visible treatment: Muted label, disabled semantics, adjacent reason if useful; Behavior / announcement: No opacity reduction of all ancestor content; Opted out by: 10 × alwaysAvailable
- State: Error · C13, C16; Visible treatment: Danger icon, explicit text and retry; Behavior / announcement: Preserve safe inputs; error related to source; Opted out by: 10 × noFailureMode
- State: Confirmed · C12; Visible treatment: Success check and specific confirmation; Behavior / announcement: Retain durable result in the surface; Opted out by: 13 × confirmedElsewhere
- State: Selected · C12, C14; Visible treatment: Check, leading marker or tonal boundary; rim light may accompany, never replace it; Behavior / announcement: Selected / pressed / current state as appropriate; Opted out by: 13 × notSelectable
- State: Stale · C16; Visible treatment: Muted surface and an age label beside the value; Behavior / announcement: The value stays readable and carries its age; never a pulse; Opted out by: 10 × notFetched
- State: Offline · C16; Visible treatment: Dimmed live surfaces and an offline notice with what is still available; Behavior / announcement: Cached content stays readable; recovery names the retry; Opted out by: 11 × notNetworkBound
### Component index

- Component: Primary action; Rules: C12, C19, C26
- Component: Secondary action; Rules: C12, C19
- Component: Destructive action; Rules: C12, C15, C19
- Component: Field with Clear; Rules: C13, C19, C20
- Component: Chips and selection; Rules: C12, C14, C19
- Component: Record row; Rules: C14, C10, C19
- Component: Sheet; Rules: C15, C26, M07, C19
- Component: Dialog; Rules: C15, C19
- Component: Banner; Rules: C16, C21, C19
- Component: Toast; Rules: C16, C19
- Component: Empty state; Rules: C16, C01, C19
- Component: Reading and stat; Rules: C08, C21, C19
- Component: Compass dial; Rules: M03, M10, W06, C18, C19
- Component: Target marker; Rules: C18, M03, M05, W05, C27
- Component: Context spine; Rules: A02, C26, C10
- Component: Horizon ribbon; Rules: A04, C27, C26
- Component: Watch action capsule; Rules: W02, W05, W03, C19

## Motion earns its place

Section: https://simt.space/cosmos/design/common#motion

Use movement to explain a change. Silence is a valid finish.

### C17 — Three effective tiers [MUST]

Full uses 150ms feedback, 300ms local state, 500ms context change and the curve published for the part (C30). Reduced removes spatial travel and ambient loops; optional opacity feedback lasts at most 150ms. Static applies final state immediately. Explicit Static wins; reduced-motion, data saving, low power, thermal pressure and visibility can only lower the tier.

Acceptance: Preference changes apply during the session. Full/Reduced/Static produce identical outcomes. Hidden screens and background tabs stop animation and sensor presentation. Hardware concurrency alone never determines ability or tier.

Rule: https://simt.space/cosmos/design/common#c17

### C18 — Progress & moving data [MUST]

Animate transform/opacity for transitional UI, avoiding width/height/blur animation. Never auto-pan a map during a user gesture. Sensor smoothing cannot conceal stale/low-confidence data; separate visual interpolation from the stored/announced reading. Auto-updating content provides pause or stable inspection when applicable.

Acceptance: No flashing beyond accessibility limits, parallax-dependent meaning, autoplay sound or attention-pulsing CTA. Reduced motion does not disable accurate data. Waiting is stated as text, not inferred from a spinner.

Rule: https://simt.space/cosmos/design/common#c18

### C30 — Easing is published per part [MUST]

Use the easing set published for the part: web uses the standard curve with emphasis, entrance and exit for arriving and departing UI; Android and Wear use the standard curve with enter, exit and transform. Every part runs the same three durations; a part that needs another curve or duration publishes it in the mapping instead of defining one locally.

Acceptance: Every transition resolves to a published curve, arrival and departure use opposite curves from the same set, and the reduced and static tiers still reach the final state without a curve. No component hard-codes a cubic-bezier, and a curve change is a mapping change rather than a silent divergence.

Rule: https://simt.space/cosmos/design/common#c30

### C31 — One motif per state change [MUST]

Animate a state change by naming the published motif whose job is that change, and spend only the properties, duration and easing tokens that motif lists. A change with no motif does not animate. A new choreography is added as a motif with its own job rather than designed once inside a component. The motifs are published in motion.json, and the curve parity fixture in that document is how a native port proves it matches.

Acceptance: Every animated state change names a motif, and every motif in motion.json is reachable from at least one surface. No motif animates a property outside its allowed list, and none loops or repeats to hold attention: stale data dims and gains a label. Reduced removes travel and keeps an opacity change within the feedback duration; Static settles immediately and reaches the same final state as Full. A port reproduces the sampled curve values to the published precision, or the mapping is wrong.

Rule: https://simt.space/cosmos/design/common#c31

### Platform easing mapping

- Purpose: Standard — in-place transform and state; Web page / web app: cubic-bezier(0.2, 0, 0.2, 1); Android: cubic-bezier(0.2, 0, 0.2, 1); Wear: cubic-bezier(0.2, 0, 0.2, 1)
- Purpose: Arrival — emphasis and entrance; Web page / web app: cubic-bezier(0.22, 1, 0.36, 1); cubic-bezier(0.16, 0.84, 0.44, 1); Android: cubic-bezier(0.05, 0.7, 0.1, 1); Wear: cubic-bezier(0.05, 0.7, 0.1, 1)
- Purpose: Departure — exit; Web page / web app: cubic-bezier(0.4, 0, 0.7, 0.2); Android: cubic-bezier(0.3, 0, 0.8, 0.15); Wear: cubic-bezier(0.3, 0, 0.8, 0.15)
- Purpose: Transform only; Web page / web app: —; Android: cubic-bezier(0.2, 0, 0, 1); Wear: cubic-bezier(0.2, 0, 0, 1)
### Motion motifs

- Motif: targetAcquired; Job: Tell the person that the thing they were tracking is now the thing they have.; Duration: 300ms local state; Easing: Standard — in-place transform and state, Arrival — emphasis and entrance; Reduced: Opacity only, over the feedback duration; the ring appears already at its final angle.
- Motif: listReveal; Job: Show that a set arrived, in an order a person can follow down.; Duration: 150ms feedback; Easing: Arrival — emphasis and entrance; Reduced: Opacity only, staggered, over the feedback duration.
- Motif: horizonSweep; Job: Carry time or progress across a wide surface without a spinner.; Duration: 500ms context change; Easing: Standard — in-place transform and state; Reduced: The band is placed at its final position and its opacity resolves over the feedback duration; no travel.
- Motif: spineAdvance; Job: Move context forward one step, so the person always knows which step they are on.; Duration: 300ms local state; Easing: Standard — in-place transform and state; Reduced: The connector is placed at its final length and the two steps cross-resolve in opacity over the feedback duration.
- Motif: sheetRise; Job: Show which context a layer came from, and leave it dismissible.; Duration: 300ms local state; Easing: Arrival — emphasis and entrance, Departure — exit; Reduced: The scrim resolves over the feedback duration and the surface is presented at its resting offset.
- Motif: dataStale; Job: Say that a reading is no longer fresh, without asking for attention.; Duration: 150ms feedback; Easing: Standard — in-place transform and state; Reduced: The dim resolves over the feedback duration, opacity only, once.
- Motif: routeChange; Job: Carry the reader from one context to the next without losing the frame.; Duration: 500ms context change; Easing: Arrival — emphasis and entrance, Departure — exit; Reduced: Both resolve in opacity over the feedback duration, with no travel.

## Everyone keeps their bearings

Section: https://simt.space/cosmos/design/common#accessibility

Input, language and physical access are part of the design.

### C19 — Access baseline [MUST]

Target WCAG 2.2 AA on web and equivalent native behavior. Adopt 44×44 CSS px web targets and 48×48dp Android/Wear targets; design the round watch layout to fit rather than shrinking targets. Provide visible focus, names/roles/values, heading structure, error relationships, alternatives to dragging and a skip link.

Acceptance: Complete primary flows with keyboard, touch and a screen reader. Focus is not covered by sticky chrome. Forced colors preserves borders, icons and focus; controls use system colors where needed.

Rule: https://simt.space/cosmos/design/common#c19

### C20 — Direction & localization [MUST]

Use logical spacing and placement. Mirror navigation flow, never geography, north, compass mathematics or media playback. Isolate authored text, handles, coordinates and times. Localize visible copy, relative dates, plural counts, measurement units and accessible names. Persist unit preference independently of language.

Acceptance: Test English, French, Arabic RTL and Spanish with long content and 200% expansion fixtures. Geographic points and bearing values are identical across directions. Critical facts remain readable in the full detail view.

Rule: https://simt.space/cosmos/design/common#c20

### C21 — Privacy & honest content [MUST]

Represent accuracy, confidence, time zone, stale data, approximate location and permission state explicitly. Do not imply live data from decoration. Keep private coordinates, profile fields, draft text and sensitive actions out of URLs, crawled pages, analytics and error messages. Use illustrative labels for invented specimen data.

Acceptance: Privacy decisions are explained at the action. Public previews reveal only authorized data. A denied or blocked profile does not disclose which private condition caused its unavailability.

Rule: https://simt.space/cosmos/design/common#c21


## Structured delivery & release

Section: https://simt.space/cosmos/design/common#implementation

Make the rules usable by people and machines.

### C22 — One source & stable addresses [MUST]

Maintain this specification in cosmos/. Generate HTML, plain Markdown, the typed token file and the complete specification JSON together. Stable rule IDs and section URLs are public contracts, and each part publishes its own tokens and rules payload carrying the common foundations plus only its own adapter. Return real files with correct MIME types; machine requests must not silently receive the home page.

Acceptance: Generated-output drift fails verification. All routes, anchors, cross-links and downloads resolve in development, preview and production. Manifest includes version, status, scope, assets, platform inheritance and source provenance. A part payload is byte-identical to the canonical documents for everything it shares, carries exactly one adapter, and stays inside its published byte budget, so it cannot drift back into the union. A document needed only sometimes keeps its own address: composited contrast measurements are fetched from contrast-pairs.json instead of being embedded in a payload, and every published document states when to fetch it.

Rule: https://simt.space/cosmos/design/common#c22

### C23 — Performance is measured [MUST]

Use the platform budgets and release matrices. Web field targets are p75 LCP ≤2.5s, INP ≤200ms and CLS ≤0.1 for mobile and desktop cohorts. Lab tests are diagnostic, not a substitute for field results. Record route, build, device, browser/OS, network, locale, motion tier and method with evidence.

Acceptance: A release has a named owner and real measurements. Visual specifications and mockups are never labeled proof that unimplemented products meet their runtime targets.

Rule: https://simt.space/cosmos/design/common#c23

### C24 — Conformance & maintenance [MUST]

Release only after applicable rules pass contrast, input, localization, state and performance checks. Record exceptions with affected rule, product reason, accessible alternative, owner and expiry. A palette or semantic-rule change requires a version update and migration note; minor additions preserve stable IDs.

Acceptance: The release record links screenshots, accessibility findings, performance traces and resolved defects. Core maintainers review shared changes; platform maintainers review adapter changes. Broken required flows block acceptance.

Rule: https://simt.space/cosmos/design/common#c24

### Implementation content model

- Field: Rule; Required structure: id, title, level, requirement, acceptance; Purpose: Stable conformance mapping
- Field: Platform; Required structure: slug, description, sections, inheritance; Purpose: A bounded adapter with common dependencies
- Field: Token; Required structure: $type, $value, $description; semantic adapter; Purpose: Typed values; explicit units and color space
- Field: Screen brief; Required structure: task, privacy class, layout, states, input, data, rules; Purpose: Enough context to implement without guessing
- Field: Evidence; Required structure: version, build, route, environment, result, owner; Purpose: Traceable release decision


---

# Web page — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/web-page

Based on simt-social-landing. Create an invitation to real places and real people, with an open editorial composition and a glimpse of the product that feels usable.

Inherits [Common foundations](https://simt.space/cosmos/design/common).

## The editorial horizon

Section: https://simt.space/cosmos/design/web-page#composition

The first screen feels spacious and explains the product immediately.

### P01 — Two complementary planes [MUST]

Use a left-aligned narrative column and an asymmetric atlas on wide screens. The horizon is a fine shared baseline linking headline, time and place. On compact screens, show the proposition, one CTA and a cropped but meaningful atlas in normal reading order. The hero uses content height, not forced 100vh. It owns the page sky and its one nebula sits behind the atlas, never behind the headline.

Acceptance: At 390×844, the product purpose and primary CTA appear without waiting for animation. At 320px and large text, the hero grows with content rather than clipping it.

Rule: https://simt.space/cosmos/design/web-page#p01

### P02 — Product evidence over decoration [MUST]

The atlas shows representative activity records, time labels and approximate places. Label illustrative data. Use contour lines and a few weighted nodes to show relationships; do not mimic a production map with fake live participants. Include one real product screenshot or an accessible product walkthrough below the hero.

Acceptance: Every showcased interaction has an honest response or is a noninteractive figure. No fabricated member counts, testimonials, countdown urgency or unverifiable availability claims.

Rule: https://simt.space/cosmos/design/web-page#p02

### Public page sequence

- Sequence: 01 Invitation; Composition: Narrative + atlas + primary CTA; Decision answered: What can I do here?
- Sequence: 02 Proof; Composition: One real experience with annotated outcome; Decision answered: Does the product deliver it?
- Sequence: 03 Path; Composition: Discover → inspect → participate as a connected spine; Decision answered: How does it work?
- Sequence: 04 Possibilities; Composition: Role/category rows with real examples; Decision answered: Is this relevant to me?
- Sequence: 05 Trust; Composition: Plain privacy and location explanation; Decision answered: What am I sharing?
- Sequence: 06 Questions; Composition: Native disclosure FAQ; Decision answered: What still stops me?
- Sequence: 07 Next step; Composition: A calm final CTA and legal/locale links; Decision answered: Where do I go next?

## Responsive composition

Section: https://simt.space/cosmos/design/web-page#layout

Keep the visual rhythm while changing the arrangement.

### P03 — Grid & navigation [MUST]

Use a 1200px content maximum, 12 columns expanded, 6 medium and 4 compact. Gutters are 48px at ≥1200, 32px at 768–1199 and 20px below 768. Narrative blocks alternate open field, ruled rows and a single focused evidence panel. Header is 68px minimum with real anchor links; mobile menus are accessible disclosures.

Acceptance: At 320, 390, 768, 1024 and 1440px no page overflow or collision. Header can grow at large text; anchor headings remain unobscured. At 400% zoom navigation stays reachable.

Rule: https://simt.space/cosmos/design/web-page#p03

### P04 — Type & emphasis [MUST]

Use display 48–88px expressed in fluid rem, maximum 12–16 words, line-height ≥1.05. Body is 18px at comfortable widths and ≥16px compact. Keep the headline solid Ink; Ember accents one meaningful phrase. Show the primary action in the action gradient; allow a secondary text link with an explicit destination.

Acceptance: Headline wraps naturally in French and Arabic. No critical information is gradient text or microsized metadata. Equal prominence is not given to every link.

Rule: https://simt.space/cosmos/design/web-page#p04


## An atlas that tells the truth

Section: https://simt.space/cosmos/design/web-page#atlas

Give place and time an understandable structure.

### P05 — Narrative atlas behavior [MUST]

Use a small inline SVG or pre-rendered image for the public hero, with a nearby text list of representative records. If interactive, use native buttons to select a record and show a labeled detail region; all records work by keyboard/touch. Map controls must have real meaning. Reuse the same selected record in text and visual representations.

Acceptance: The page remains complete with JavaScript disabled and reduced motion. Selection is visible by label and shape. Decorative geography is marked schematic; exact private coordinates never enter the illustration.

Rule: https://simt.space/cosmos/design/web-page#p05

### P06 — Timelines & categories [MUST]

Use a horizon ribbon to order real dates or steps, with direct labels and connected milestones. Category color follows common meanings; category filters pair a label with count or selected state. Do not add an auto-scrolling marquee to communicate essential content.

Acceptance: All milestone and category content is available as text. Horizontal overflow has an obvious scroll or navigation affordance without fading essential labels.

Rule: https://simt.space/cosmos/design/web-page#p06


## Confident, calm conversion

Section: https://simt.space/cosmos/design/web-page#conversion

Make the destination and commitment clear.

### P07 — CTA contract [MUST]

Primary CTA names the next action and its actual destination. Preserve direct hrefs, keyboard activation and expected new-tab behavior. Explain sign-in or install requirements beside the relevant action. Use no looping CTA pulse, hover-only action, modal interruption or surprise auto-navigation.

Acceptance: A CTA works without client JavaScript. Track only allowlisted coarse events; never block navigation on analytics. External destinations and download labels match what opens.

Rule: https://simt.space/cosmos/design/web-page#p07

### P08 — Public content & localization [MUST]

Use one H1, a meaningful H2 outline, semantic landmarks, purpose-based alt text and visible legal/contact links. Localized pages use correct lang/dir, self-canonical URLs and reciprocal hreflang. Dates and assertions have a source/owner; legal copy is maintained independently of promotional language.

Acceptance: All key text is in the initial HTML, not canvas or image-only type. Arabic reading order and DOM order agree. Each locale has a complete primary journey and no mixed untranslated navigation.

Rule: https://simt.space/cosmos/design/web-page#p08


## Fast by construction

Section: https://simt.space/cosmos/design/web-page#delivery

The atmosphere should be cheaper than the story.

### P09 — Delivery budgets [MUST]

Target ≤30KiB gzip initial JavaScript, ≤35KiB gzip CSS, ≤120KiB initial font files and ≤250KiB initial hero media. Ship responsive AVIF/WebP with dimensions; load the actual LCP image eagerly and later media lazily. Load no full map engine, animation framework or video before it is requested.

Acceptance: Check production compression and request waterfall on a cold load. No unused font preload. Measure Core Web Vitals with constrained mobile lab diagnostics and real field cohorts after launch.

Rule: https://simt.space/cosmos/design/web-page#p09

### P10 — Enhancement lifecycle [MUST]

At most one optional compositor-only ambient hero motion in Full; stop it when out of view or page hidden. Content never starts invisible awaiting an observer. Reduced and data-saving modes show final static compositions. Third-party tags are isolated from critical rendering and governed by the product consent policy.

Acceptance: No animation-frame loop runs while scrolling the reference page. The first paint remains useful if script fails. Layout shift stays within the shared CLS budget.

Rule: https://simt.space/cosmos/design/web-page#p10


## Web page acceptance

Section: https://simt.space/cosmos/design/web-page#release

Verify the narrative and the real path out of it.

### P11 — Public release matrix [MUST]

Verify home, FAQ, legal, locale switch and each CTA at compact/medium/expanded widths; English, French and Arabic; keyboard, screen reader, no-JS, reduced motion and forced colors. Run focused Chromium/Firefox/WebKit checks for shared layout and browser APIs. Test real mobile Safari/Android at release.

Acceptance: Evidence includes first viewport and full page, link/status checks, metadata parity, accessibility findings and asset budgets. Do not advertise runtime or accessibility conformance based on screenshots alone.

Rule: https://simt.space/cosmos/design/web-page#p11



---

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



---

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



---

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



## Starfield reference

The algorithm behind C11. Reproduce the `starfield` fixture in tokens.json (surface `cosmos.reference`, 390 × 844) before shipping a port.

1. Bucket: bw = ceil(width / bucket) * bucket and bh likewise, in CSS px or dp.
2. Seed: FNV-1a 32-bit over the UTF-8 bytes of "<surfaceId>@<bw>x<bh>"; random numbers come from mulberry32(seed) as uint32 / 2^32.
3. Count: round-half-up(bw * bh / areaPerStar) clamped to [min, max] for the surface kind.
4. Per star, in this order: one draw selects the magnitude, one the radius, one the alpha, one the color; then up to `attempts` (u, v) pairs.
5. Color: draw < inkShare is ink; otherwise accents[min(accents.length - 1, floor((draw - inkShare) / ((1 - inkShare) / accents.length)))], demoted to ink when that accent already holds floor(count * maxAccentShare) stars or all accents hold floor(count * (1 - inkShare)).
6. Position: reject (u, v) when both lie strictly inside (band, 1 - band), or when the squared bucket-pixel distance to an accepted star is below (2 * max(r, rOther))^2. A star with no accepted attempt is skipped.
7. Spikes: the first `maxSpikes` accepted bright stars carry a four-point diffraction cross 5 * r long.
8. Render at x = u * width, y = v * height. Exclusion zones filter accepted stars afterwards and never reseed.

## Theme adapters

### daylight

- background: #FFF6EC
- surface: #FFF9F2
- raised: #F6E7DC
- highest: #F3E0F7
- text: #241A14
- muted: #52443C
- faint: #6B5F58
- line: rgb(36 26 20 / 0.14)
- action: #D84E00
- accentText: #B04300
- onAction: #120A14
- social: #B83266
- identity: #7C4DBD
- precision: #8B5E00
- live: #006974
- outdoors: #356447
- danger: #9B2432
- success: #276442
- focus: #8B5E00
- boundary: #52443C

### astronomy

- background: #0A0000
- surface: #1A0000
- raised: #2A0000
- highest: #330000
- text: #E88989
- muted: #D47575
- faint: #C06A6A
- line: rgb(232 137 137 / 0.16)
- action: #8B0000
- accentText: #E88989
- onAction: #FFB3B3
- social: #E88989
- identity: #E88989
- precision: #E88989
- live: #E88989
- outdoors: #E88989
- danger: #FFB3B3
- success: #E88989
- focus: #E88989
- boundary: #D47575

### watchAmbient

- background: #000000
- surface: #000000
- text: #BBAEBB
- faint: #9A8B9A
- line: rgb(187 174 187 / 0.16)
- bearing: #FFC766

## Reference sources

- [WCAG 2.2](https://www.w3.org/TR/WCAG22/): Web accessibility baseline; Cosmos adopts a stronger 44 CSS px target than the 24 CSS px AA minimum.
- [Core Web Vitals](https://web.dev/articles/vitals): Field LCP, INP and CLS thresholds at the 75th percentile.
- [Wear accessibility](https://developer.android.com/design/ui/wear/guides/foundations/accessibility): Touch geometry, text and assistive input on round screens.
- [Wear always-on](https://developer.android.com/training/wearables/always-on): Ambient lifecycle, burn-in protection and battery-aware behavior.
- [Design Tokens format](https://www.designtokens.org/tr/2025.10/format/): Typed token interchange; exported sRGB components retain exact hex anchors in extensions.

## Repository provenance

- simt-landing (superseded by simt-space): cosmos-design/index.html, styles/cosmos-design.less, scripts/cosmos-design.js. Observed: One large guide; page-owned animated star canvases and mixed native/web examples. Target: Replace with a static, linked specification with measurable rules and one optional atmospheric owner.
- simt-social-landing: src/templates/partials/hero.hbs, src/templates/partials/atlas.hbs, src/styles/tokens.less. Observed: Narrative hero, activity atlas, role discovery, privacy and multilingual public content. Target: Keep the human story and useful atlas; use editorial horizons and actual product evidence.
- simt-social: app/styles/tokens-dark.css, app/routes/discover.tsx, mockups/README.md. Observed: Canonical warm palette, persistent social shell, map/list discovery, activity composer and moderation console. Target: Preserve route responsibilities; improve context continuity, scanning, recovery and density.
- simt-app: docs/design/COSMOS_REDESIGN.md, core/ui/src/main/java/app/simt/core/ui/theme/Color.kt, core/ui/src/main/java/app/simt/core/ui/theme/Motion.kt. Observed: Native instrument palette and typography; adaptive maps, dials and tools. Target: Task-first instruments with stable reference frames, deliberate reach zones and system-owned back behavior.
- simt-app/wear: src/main/java/app/simt/wear/screens/HomeScreen.kt, src/main/java/app/simt/wear/screens/CompassScreen.kt, src/main/java/app/simt/wear/theme/WearColors.kt. Observed: Compass, sky, geofence, plumb bob, targets and settings; cyan has a legacy tool-specific value. Target: One-glance tools with shared signal meaning, round geometry and a genuinely quiet ambient state.
