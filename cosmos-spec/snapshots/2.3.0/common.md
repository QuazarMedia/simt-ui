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
