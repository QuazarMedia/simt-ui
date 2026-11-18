import { authority } from '../site/authority.js';
import { colors, gradients, platformAdapters, tokens } from './tokens.js';
import { oklch } from './color.js';
import { durationFor, easingKeysFor, EASING_PURPOSES, motionMotifs } from './motion.js';
import { COMPONENT_STATES, componentApplicability, componentRegistry } from './components.js';
import { starfield } from './starfield.js';

const rule = (id, title, requirement, acceptance, level = 'must') => ({ id, title, level, requirement, acceptance });
const section = (id, title, summary, rules = [], table = null) => ({ id, title, summary, rules, table });
const table = (caption, columns, rows) => ({ caption, columns, rows });

/** The release is published directly at the standalone origin root. */
export const releasePath = '';

// The geometry and easing tables are generated from platformAdapters so the
// published rules and the machine-readable token file cannot disagree (C09, C30).
// A dp radius is a number token with its unit in $extensions, because DTCG has no
// dp dimension, and a native shape is not a token at all — so the two are read from
// where they actually live rather than forced through one accessor.
const cosmos = (token, key) => token.$extensions['app.simt.cosmos'][key];
const geom = (t) =>
  t.$type === 'dimension' ? `${t.$value.value} ${t.$value.unit}` : `${t.$value} ${cosmos(t, 'unit')}`;
const curve = (t) => `cubic-bezier(${t.$value.map((n) => String(n)).join(', ')})`;
const GEOMETRY_ROLES = [
  ['Control — compact control, chip, field', 'control', 'control'],
  ['Secondary — inline affordance', null, 'secondary'],
  ['Emphasis — inline emphasis', null, 'emphasis'],
  ['Record — card or list row', 'record', 'record'],
  ['Overlay — sheet or dialog', 'overlay', 'overlay'],
  ['Circle — instrument or compact icon control', null, 'circle'],
];
const geometryMapping = () => {
  const androidRadius = platformAdapters.android.radius;
  const androidShapes = cosmos(platformAdapters.android, 'shapes');
  const wearRadius = cosmos(platformAdapters.watch, 'radius');
  const wearCell = (role) => wearRadius[role] ?? androidShapes[role] ?? '—';
  return table(
    'Platform geometry mapping',
    ['Role', 'Web page / web app', 'Android', 'Wear'],
    GEOMETRY_ROLES.map(([role, web, native]) => [
      role,
      web ? geom(platformAdapters.webApp.radius[web]) : '—',
      androidRadius[native] ? geom(androidRadius[native]) : (androidShapes[native] ?? '—'),
      wearCell(web ?? native),
    ]),
  );
};
// The rule prose reads the same tables, so a number cannot be typed into a rule and
// left behind when the mapping changes.
const spaceList = () =>
  Object.keys(tokens.space)
    .filter((key) => !key.startsWith('$'))
    .join(', ');
// The rule names each value with the role it belongs to, the way M06 does two
// rules later and the way the mapping table beside it does. A bare list of
// numbers is unreadable on its own: a reader cannot tell which value is the
// control and which is the sheet without opening tokens.json.
const radiusList = (part, column) => {
  const radii = platformAdapters[part].radius ?? {};
  // Only Android publishes a shapes extension; a part without one has no
  // shape-valued roles, which is why the lookup has to tolerate its absence
  // rather than assume the token it was handed carries one.
  const shapes = platformAdapters[part].$extensions?.['app.simt.cosmos']?.shapes ?? {};
  return GEOMETRY_ROLES.map(([role, web, native]) => {
    const key = column === 'web' ? web : native;
    // A part publishes a circle as a shape rather than a radius, so the role has a
    // value that is not a length; the rule names the shape rather than inventing
    // a number for it.
    if (key === null) return null;
    const value = radii[key];
    const label = role.split(' —')[0].toLowerCase();
    if (value !== undefined) return `${geom(value)} ${label}`;
    return shapes[key] === undefined ? null : `${shapes[key]} ${label}`;
  })
    .filter(Boolean)
    .join(' / ');
};
const dpRole = (role) => `${platformAdapters.android.radius[role].$value}dp`;
const DURATION_ROLES = { feedback: 'feedback', state: 'local state', context: 'context change' };
const durationList = () =>
  Object.entries(platformAdapters.webApp.duration)
    .map(([role, t]) => `${t.$value.value}ms ${DURATION_ROLES[role]}`)
    .join(', ');
// C11 is the most number-dense rule in the document and every figure in it is a
// published starfield token, so the rule reads them the way the brief already
// does rather than typing a second copy. Multiplying a share into a percentage
// is done through percent(): 0.015 * 100 is 1.4999999999999998, and a rule that
// prints that is a rule nobody can copy.
const percentValue = (share) => String(Number((share * 100).toFixed(4)));
const starDensity = (kind) =>
  `area / ${starfield.density[kind].areaPerStar} stars clamped ${starfield.density[kind].min}\u2013${starfield.density[kind].max}`;
// The easing purposes and the motif set are read from cosmos/motion.js, so the
// published table, the machine-readable document and the resolver a port reads
// are one owner. A motif resolves its easing through the same table, so a table
// row and a motif can never name different curves (C30, C31).
const EASING_PARTS = [
  ['webApp', 'Web page / web app'],
  ['android', 'Android'],
  ['watch', 'Wear'],
];
const easingMapping = () =>
  table(
    'Platform easing mapping',
    ['Purpose', ...EASING_PARTS.map(([, label]) => label)],
    Object.entries(EASING_PURPOSES).map(([purpose, definition]) => [
      definition.label,
      ...EASING_PARTS.map(([part]) => {
        const keys = easingKeysFor(part, [purpose]);
        return keys.length === 0 ? '—' : keys.map((key) => curve(platformAdapters[part].easing[key])).join('; ');
      }),
    ]),
  );
// The reduced tier is the accessibility-critical column, so it is the one that
// reaches the page. The steps, the static tier and the per-toolkit mapping live
// in motion.json, where an agent building one platform reads them once.
const motifMapping = () =>
  table(
    'Motion motifs',
    ['Motif', 'Job', 'Duration', 'Easing', 'Reduced'],
    motionMotifs.map((motif) => [
      motif.id,
      motif.job,
      durationFor('webPage', motif.duration).$value.value + 'ms ' + DURATION_ROLES[motif.duration],
      // The easing column reads in the same vocabulary as the mapping table
      // directly above it, so a reader comparing the two is not asked to know that
      // one spells a purpose out and the other abbreviates it.
      motif.easing.map((purpose) => EASING_PURPOSES[purpose].label).join(', '),
      motif.tiers.reduced,
    ]),
  );

// The state vocabulary is owned by the component registry, because the registry is
// the index of which states a surface can actually be in: a vocabulary typed here
// would be a second list that could gain a state the registry never exposes, or
// lose one it depends on. The treatments stay here, where every other treatment
// lives — the registry carries references only. `hover` and `pressed` are separate
// states because a pointer and a finger reach them differently, and `stale` and
// `offline` are separate because one is a fact about the data and the other about
// the connection, so their recoveries differ.
export const STATE_TREATMENTS = {
  default: ['Opaque surface, legible label, essential boundary', 'Real enabled semantics'],
  hover: ['Tonal lift; no movement of layout', 'Optional; a pointer that cannot hover simply never shows it'],
  pressed: ['Inset tone; no movement of layout', 'Equivalent to hover for touch and keyboard'],
  focus: ['2px Gold ring + dark separation, 3px offset', 'Visible, unclipped and not obscured by sticky UI'],
  pending: ['Stable label and small progress mark', 'Busy, duplicate guard; cancel if operation allows'],
  disabled: [
    'Muted label, disabled semantics, adjacent reason if useful',
    'No opacity reduction of all ancestor content',
  ],
  error: ['Danger icon, explicit text and retry', 'Preserve safe inputs; error related to source'],
  confirmed: ['Success check and specific confirmation', 'Retain durable result in the surface'],
  selected: [
    'Check, leading marker or tonal boundary; rim light may accompany, never replace it',
    'Selected / pressed / current state as appropriate',
  ],
  stale: [
    'Muted surface and an age label beside the value',
    'The value stays readable and carries its age; never a pulse',
  ],
  offline: [
    'Dimmed live surfaces and an offline notice with what is still available',
    'Cached content stays readable; recovery names the retry',
  ],
};

// The fourth column is the one that makes the matrix complete. Every component
// answers for all eleven states, so a row that listed only treatments would leave
// a reader unable to tell a component that deliberately has no `selected` from
// one nobody has thought about. The reasons are named rather than written here
// because components.json publishes each reason once as prose; a copy of eleven
// sentences in every part's rules payload would be a second answer to the same
// question, and one of them would drift.
const stateMatrix = () =>
  table(
    'Shared state matrix',
    ['State', 'Visible treatment', 'Behavior / announcement', 'Opted out by'],
    Object.entries(COMPONENT_STATES).map(([state, { rules }]) => {
      // The vocabulary and the treatments are two maps, and nothing else forces
      // them to agree: a state added to one and not the other used to fail as a
      // TypeError about an index rather than as the missing treatment it is.
      const treatment = STATE_TREATMENTS[state];
      if (treatment === undefined)
        throw new Error(
          `The state ${state} is in the shared vocabulary but has no treatment. Every state the matrix names must state what it looks like and what it announces.`,
        );
      const reasons = {};
      for (const component of componentRegistry) {
        const reason = componentApplicability[component.id]?.[state];
        if (reason !== undefined) reasons[reason] = (reasons[reason] ?? 0) + 1;
      }
      const optedOut = Object.entries(reasons)
        .map(([reason, count]) => `${count} × ${reason}`)
        .join(', ');
      return [
        state.charAt(0).toUpperCase() + state.slice(1) + ' · ' + rules.join(', '),
        treatment[0],
        treatment[1],
        optedOut === '' ? 'every component declares it' : optedOut,
      ];
    }),
  );

// The component index is generated from the registry so a component added there
// reaches the page without a second edit. It carries references, not treatments:
// the rules it names remain the only place a treatment is written. It is
// deliberately two columns. Corner geometry is already published by the geometry
// table, motion by the motif table, and the parts a component ships on are
// published as per-part slices in components.json — repeating all three here would
// put a second copy of each into every part's rules payload to say nothing a
// reader did not already have one page away.
const componentIndex = () =>
  table(
    'Component index',
    ['Component', 'Rules'],
    componentRegistry.map((component) => [component.name, component.rules.join(', ')]),
  );

// `version` is the only place the release number is written, in this repository
// or any that reads it. `notes` carries what this release changed and `history`
// carries the releases before it, so nothing keys anything by a second copy of
// the version and no gate has to exist to check the two agree.
//
// Each release keeps only its own notes, so a reader of one is never told what a
// different one did: specification.json names the current release and points at
// changelog.md, which publishes the whole history. A version that never shipped
// is not a version at all — the notes for everything since the last release that
// did ship belong to that release, because a release record nobody can reach
// describes a release that did not happen. 2.4.0 and everything after it never
// shipped, so those notes are 2.3.0's and `history` is empty while 2.3.0 is the
// only release there is.
//
// A note may interpolate a `{name}` from the measured document sizes, so a size
// claim in the record is always the size of the document that shipped with it.
// Nothing in a note may type a size by hand: scripts/verify.js refuses one.
export const release = {
  version: '2.3.0',
  updated: '2026-10-03',
  status: 'Normative specification',
  owner: 'SIMT UI product design and platform maintainers',
  base: authority.origin,
  scope: 'Shared foundations plus four platform parts: web page, web app, mobile app and watch app.',
  implementationStatus:
    'The web product and the mobile product are built on this specification. Conformance is recorded rule by rule in a release record; examples use illustrative data.',
  // The notes for the release named above, oldest change first. A list rather
  // than a map keyed by version, because the version is declared once, here: a
  // second copy of it that only has to agree is a value the gate would have to
  // exist to check.
  notes: [
    'Adds C27 and the light tokens: eight gradients and two shadows composed only from the existing anchors.',
      'C11 now specifies one deterministic edge star layer with a published parity fixture, and moves to the new atmosphere section.',
      'The focus aperture is drawn as a quasar: core, corona and optional accretion ring (C03, A03, M01).',
      'The primary action fills with the action gradient (C12, P04). Watch interactive mode may show one static corona on its marker (W01).',
      'Sky and orrery fields own no decorative stars (M04). No anchor color, rule ID or section URL changed.',
      'The theme adapters become the only definition of a surface: every interactive profile publishes the same set of semantic roles and a display profile publishes fewer, and a component reads roles rather than anchors.',
      'Three roles are new. `accentText` carries an accent that is used as text, `faint` is the third text step and `line` is a translucent hairline no hex can express.',
      'Gradients and shadows are now per-profile. A profile that does not run one resolves the token to `none` and the element falls back to its flat role color.',
      'C06 gains the selection rule: follow the system preference until the visitor chooses, and an explicit choice persists.',
      "C27 names the one ambient motion: the focal object's accretion disc revolves about the core every 40-90 s, and the accretion ring no longer rotates.",
      'The ring stopped moving because C27 already said light never carries the only boundary, and a rotating outline is a boundary. What moves now is geometry the visitor was reading as depth.',
      'No anchor color, rule ID or section URL changed.',
      'Six published claims are now enforced rather than stated. The conformance figures are compared between the token block and the three rules that name them, so a target cannot drift from the number a port is measured against; the parity fixture publishes its iteration count, sample count, duration table and per-motif timing, and each is checked against a second source or against a floor a port can rely on; the manifest names every document the product declares, including the two it published but never listed; the rule-ID shape both schemas declare is asserted instead of inferred from the prefix; the physical-placement ban reaches the landing stylesheet, with one named exemption for the marker the script positions in the SVG coordinate system; and the browser tier runs from one command that builds, serves what it built and stops the server, so playback, focus and no-JS rendering no longer depend on someone remembering a second command.',
      'Six facts that had more than one owner now have one, and the gate says which file owns each. The theme profile list is read from the token file by the generator and off the rendered picker by the browser instead of being written out three times, so a fifth profile is one edit; the origin, the logo and the stylesheet are read from the registry rather than restated in the generator, which also drops a field no module read; the anchor map, the sRGB channel decode, the six-decimal fixture rounding and the camel-to-kebab rule each have a single owner, and a second copy is now a gate failure rather than a number that quietly disagrees with its twin; every published byte budget is checked against an assertion, so a ceiling listed in three tables and measured in none cannot pass; and the rule-prefix vocabulary is owned once, so the part documents, the component slices and the rule links are derived from it together. Nothing published changes.',
      'The eight per-part payloads are listed as resources instead of being published unannounced. Each part publishes a tokens document and a rules document, and an agent reading the catalog was told about neither: llms.txt and the ARD catalog enumerate what the product declares, and these were in neither list, so a document that exists was indistinguishable from one that does not. They are now declared, and the generator takes its published resource map from that same declaration rather than restating fourteen names, so a payload cannot be added without being announced. Two more owners went with it: humans.txt describes the accessibility target from the product that claims it rather than naming Cosmos, and the 404 page carries its own styling, because the one page that must render when a path is wrong cannot depend on a product stylesheet resolving. The published Markdown is now rendered once per part and shared, which changed no byte, and the two schemas state their own counts instead of typing numbers that were already wrong.',
      'The anchor reservation is published once. --anchor-reserved was read by two rules and assigned nowhere, so each of them carried its own 144px fallback: every in-page anchor jump on a part page landed about 31px lower than intended against a header that measures 113px, and the number a maintainer read in a comment was a third figure that matched neither. The header now publishes the measurement at :root, the two rules read it with no fallback, and the gate requires it to be declared exactly once and to be a length -- a variable nobody assigns, declared twice, or given a second fallback all fail instead of shipping.'
      ,
      'A stored motion preference is now honoured from the first frame. The tier used to be restored by a deferred module, so a reader who asked for still frames watched the ambient disc revolve for the whole of the load; the inline script in the document head restores it alongside the theme, generated from the same tier list the picker renders, so a tier added to the picker is restored by the same edit. The tier list has one owner and the client reads it off the rendered radios rather than carrying its own copy. The browser chrome is painted from the profile that was chosen: selecting Astronomy or Watch Ambient previously left a light-mode browser showing the cream theme colour above a page painted near-black, because only Daylight was ever given its own colour. Every picker option now publishes that profile colour, and the page chrome colour is read from the token file rather than typed, so a profile whose background is not a paintable hex fails the build instead of shipping.'
      ,
      'The build does the work once. The gate generated every published document a second time in order to compare it with the disk, and the four words of language naming in humans.txt paid for a cold Intl.DisplayNames on each of those passes; both now happen once per process. The per-pin document reads, the CSS variable lookup behind 223 variable references on one page, the family lookup behind every gallery card and the seventeen painters behind each state drawing are resolved once and reused. No published byte changed, and none of this was a measurable cost at the current size -- it is recorded so it is not mistaken for a defect later.'
      ,
      'The generation path is checked where it happens. The static asset tree was copied by the build and verified by nothing, while the log line reported the two compiled files as though they were the whole job; the check now walks the copied tree and fails on a missing or drifted file, and the count it prints is the number of files that actually matter. One escaping owner prints every label: it escapes a quote as well as the three XML-significant characters, and the sitemap escapes through it as XML rather than as HTML, so an apostrophe in a product name is a correct document instead of a broken one. The root redirect is derived from the landing locale rather than typed, so it stops being right by coincidence. The note sizes that appear in release notes name the document they cannot find instead of publishing Infinity or throwing a bare type error, and a part that publishes no payload is a build failure rather than an empty range. One sentence no longer counts the pages it describes; a derived count read worse than no count.'
      ,
      'The gate now inverts the oklch notation this document publishes, not only the component triple behind it. C29 promises every published web oklch resolves back to the sRGB value it came from, and the check that made that promise was reading a DTCG artefact that no reader ever sees while the formatted string in the token registry table went unchecked -- a rounding regression in the notation itself would have shipped. Each published notation is now parsed, resolved and compared with the hex beside it in the same row. The dead code that hid this is gone too: the parse existed for exactly this check and nothing called it. A star colour that nothing publishes is now a build failure rather than a sky that quietly drops its stars, and the accent index in the starfield algorithm is derived from the accent list instead of a literal 3 that only matched the four accents it was written beside.'
      ,
      'The generated tables now speak one language. C09 listed its corner radii as bare numbers while M06, two rules away, named every role, so a reader of C09 could not tell which value was the control and which was the sheet without opening tokens.json; both now read the same mapping the table beside them prints. The motion section published two tables a few lines apart in different vocabularies for the same easing purpose, and the motif table abbreviated them through an identity map that added nothing; both now name the purpose as the mapping does. The OLED watch specimen painted its plate from a hand-typed #000 -- the one colour in the stylesheet that no token owned -- and the token pin beside it had to be widened past that literal to stop three strings freezing with it, so on a light page the reading stayed on dark-profile values above a plate every other specimen had recoloured. The plate is its own element now, painted from a published profile value, and its type follows the theme: Compass, 042 and the last reading each move with the profile. No stylesheet rule may paint with a literal hex again, which is what makes the next one a failure rather than a surprise.'
      ,
      'Every URL in this release moved. The design system is published at /cosmos/design rather than at a top-level path of its own, because /cosmos is where this origin keeps its reference work and the design system is the first sub-context in it rather than the only thing that will ever live there. Nothing about a rule, a token or a component changed; every canonical URL, sitemap entry, agent resource, redirect and header moved with it, and the old address returns the overview rather than a 404 so a link already in the wild still arrives somewhere. The structure is declared once -- the context in the space descriptor, the sub-context beside it, and a product base derived from both -- so publishing the next reference system under /cosmos is a descriptor rather than another root typed in a dozen places.'
      ,
      'Adds C30: easing is published per platform part in tokens.json under platformAdapters, so a part reads its own curves instead of inferring them.',
      'C09 and M06 state the Android shape scale (8 / 10 / 14 / 20 / 28 dp) in place of the three-step 12 / 20 / 28, because a handheld screen steps from a 48 dp target to a full-bleed overlay in five increments where a pointer surface needs three, and the Wear round and capsule shapes become normative rather than implied. Both keep their ID and their acceptance criterion.',
      'The platform mapping is authored in tokens.json as the normative source for every part. A product that diverges from it is out of date with the mapping, and the mapping is never revised to match a product.',
      'Spacing is unchanged: 1 unit is 1 CSS px on web and 1 dp natively, and no platform publishes its own scale.',
      'C28 is merged into C09. It repeated the same mapping from a second rule, and one rule that owns the geometry cannot leave a copy of it behind when the mapping changes. No rule ID changed meaning; C28 is free for the next common rule.',
      'C09, M06 and C17 no longer type a number. The spacing scale, the radius scale and the three durations are read from the token file, so the prose cannot drift from the mapping that an agent reads.',
      'Every platform part now runs the same three durations. The Android and Wear mapping publishes 150ms feedback, 300ms local state and 500ms context change, and C17 reads that table rather than naming one figure per platform.',
      'W01 states that corona is the only gradient a watch screen may use, which is what the atmosphere table already implied and what a part payload needs in order to ship one gradient instead of eight.',
      "Each of the four parts now publishes tokens.json and rules.json at /cosmos/design/<part>/ carrying the common foundations plus that part's own adapter, so an agent building one platform no longer reads the other three.",
      'C22 owns those addresses: a payload carries exactly one adapter, publishes the scope it was filtered to, and stays inside a published byte budget. Everything it shares is byte-identical to the canonical document, and filtering may drop a profile but never rewrite one.',
      'The composited contrast measurements move out of tokens.json into contrast-pairs.json, with the reason to fetch it stated in every file that points at it.',
      'C05 now names that document as the place the measurements live; C22 owns the rule that a sometimes-needed document gets its own address.',
      'The token document is {tokens} with the measurements out of it, and one part payload is {partPayloadMin}–{partPayloadMax} against {union} for the canonical union. Every measured ratio is unchanged.',
      'A part payload is now filtered to what that part runs, and the scope it was filtered to is published in the document: the profiles it selects, the gradients its surfaces use, and whether it draws the star layer. The wrist payload, the narrowest of the four, is {watchTokens} of tokens: a round screen draws one glow and no stars, beside the action and rim fills every part inherits.',
      'The glows a part draws follow the atmosphere table, which is a table of profiles and so cannot say which of them a round screen has room for; the published scope is the owner and the table covers its ambient half. Deep Space draws the whole grammar, Daylight keeps horizon and spectrum, astronomy red and watch ambient draw none, and a watch screen narrows further to one. A floor sits under all four: the action gradient of C12 and the rim of C10 are inherited by every part, so no payload drops them, and no rule can be satisfied from a document that filtered its own token out.',
      'The oklch projection is published once, as real colour tokens, instead of once per web part as a block of notation strings, and only the parts that can emit an oklch value carry it. It projects the profiles a part actually runs, not all four.',
      'The token files conform to the design-token standard: a dp radius is a number with its unit in the extension, a native shape is a shape rather than a fake token type, and no group mixes a bare value with its tokens. A tool that reads these files no longer skips a colour.',
      'Adds C29: colour is authored and verified in sRGB, and every published colour also carries the oklch projection the web parts may author in.',
      'The Deep Space token registry gains an oklch column. Every hex value is unchanged; the projection is derived, so the two notations cannot disagree.',
      'Android and Wear declare sRGB in the platform mapping, because neither toolkit has an Oklab colour space to author in.',
      'Adds C31: a state change is animated by naming the motif whose job is that change. Seven motifs cover the recurring changes — a target acquired, a list revealed, a horizon sweep, the spine advancing, a sheet rising, data going stale, a route change — each publishing its trigger, steps, allowed properties, duration, easing, tier behaviour and CSS, Compose and Wear mapping. A change with no motif does not animate.',
      'The motifs are published in motion.json, outside the token payloads, with a fetchWhen note saying when to read it. The token files carry the address and a payload never embeds the set, so an agent working on one state change does not read seven choreographies to animate a dim.',
      'The same document publishes a curve parity fixture: every published easing curve sampled at a fixed number of points by a fixed number of bisection steps, the way the star field publishes its own. A native port reproduces the numbers instead of trusting its toolkit to match, and the fixture carries both the web and the native departure curve because they genuinely differ.',
      'A section can now carry more than one table, so the motion section publishes the easing mapping and the motif set side by side. The easing purposes are read from the motif module rather than restated, so a table row and a motif cannot name different curves.',
      'Adds the component index: components.json names, per component, its rule IDs, theme roles, layer and corner role, states, motif and the counterpart each product already has, sliced by platform part. It carries references only — a treatment restated is a second answer that can disagree with the rule it summarises.',
      'The shared state matrix grows from eight rows to eleven and is now generated. Hover and pressed are separate because a pointer and a finger are different, and confirmed, stale and offline are named because a surface that reaches a data state has to show its confidence. Generated from one vocabulary, so a state with no written treatment fails verification instead of appearing in a table with nothing beside it.',
      'A component names the motif that animates it and inherits its duration and easing from that motif, so a component repeating the timing was a second copy of motion.json; three actions named a feedback duration beside a motif whose own is state. A component with no motif publishes no timing, which is how a stage that does not animate is stated.',
      "Every component is now shown at /cosmos/design/gallery, one route per family so each stays inside the per-route HTML budget and a reader can link to one family instead of all of them. A card is a rendering, not a second contract: the state matrix is the rule's own wording, the timing and choreography from the named motif, the colours from the published tokens.",
      'A gallery card is static HTML. The state matrix is a table, the motif is four still SVG frames, and the Play control ships disabled, so the page explains itself with scripting off and under every theme. Playback is a courtesy on top: one run at a time, nothing on its own, a run that leaves the screen ends rather than resuming unseen, and Static removes the control.',
      'The duration a card runs is published as a token role rather than a figure, so the client resolves it from the stylesheet and a card cannot state a duration the mapping has moved. Cross-page view transitions between the parts are CSS only and only where the reader has not asked for reduced motion.',
      'The manifest names the galleries, their families and the components each one shows, derived from the registry. The routes were reachable from the navigation and llms.txt but not from the document an agent loads first.',
      "Every page carries the site chrome once. A shared page shell let one caller emit its own navigation beside the shell's, putting a second sticky header on six routes; the shell owns the chrome and a check holds each page to one of each.",
      'A drawing is only as honest as the state it claims to show, and the illustration helpers emitted one style attribute per colour. A browser kept the first and dropped the rest, so 53 gallery and five specimen shapes lost their fill or stroke and a card could not show what it claimed. One helper emits a single merged attribute, and a check refuses a repeated one.',
      'Every component now accounts for all eleven states, naming the ones it has and the ones it has deliberately opted out of. Of 187 state slots, 102 were unaccounted for, so a component with one state passed. A component opting out gives one of nine codes, each published once as prose, because applicability cannot be derived: the components share sixteen distinct state sets.',
      'The shared state matrix gains a fourth column naming how many components opt out of each state and by which reason, so coverage is visible on the page.',
      'A purpose is not a curve: web publishes standard, emphasis, entrance and exit while native adds transform, so a motif naming transform resolved to nothing on a web part and the gate said nothing. Every motif must now resolve on every part it runs on.',
      'Budget failure messages name the budget and quote its live ceiling, instead of typing a figure that stopped being true when a budget was raised.',
      'The gallery pages carry the tier picker and the tier is remembered. A reader comparing tiers across seven pages had lost the choice on every navigation, and the picker lived only on the foundations page, so Reduced and Static were unreachable from a gallery. The gallery index carries no picker: it has nothing to play and arrives carrying the stored tier.',
      'Browser verification covers all thirteen routes and checks the gallery across all three tiers: nothing autoplays, a Static run cannot start even when its button is clicked directly, starting one demo settles the other, and a Reduced run moves no frame and settles into the state a default run does. The tiers are read from the published picker, so a check cannot invent one.',
      'A run is a crossfade in every tier that has one. It was not: the transition sat on the drawing rather than the frame the script marks, the reduced tier pinned every frame opaque, and both reduced-motion blankets reached the frame with no transition allowed. The fade is not scoped to a tier, because scoping it to Reduced left the reduced run the smoother of the two.',
      'A card fits a narrow column at large text: the breadcrumb could not wrap and an anatomy identifier has no space to break in, so a 320-pixel page scrolled sideways at doubled text.',
      'The mark that opens a section label is the brand signature, so it paints spectrum rather than horizon. Horizon is the token for a 1-2 px light line and corona and accretion belong to the single focal object, so a page of eyebrows was a page of light lines standing in for a mark. Spectrum is published in every profile that carries branding, and in the profile that carries none the mark is absent rather than borrowed.',
      'Focusing a control in the header no longer scrolls the page. The document reserves space above each scroll-into-view target so an anchor jump clears the header, and a header control is already inside that reservation, so focusing one scrolled the page up by 490 pixels on Chromium and 236 on WebKit, from the theme select and the brand link alike. A control cancels it with the reservation the document already publishes.',
      'A drawing is repaired at the source: a name resolves to the variable that publishes it, and a name nothing publishes fails the build. Every primary action, chip and focus ring had rendered as a black rectangle, because the semantic roles are published under the cosmos prefix and the layers under layer-, so a hand-written var(--action) named nothing and an SVG fill fell back to black. The colours that happened to be aliases painted correctly, which is what hid it.',
      'A card draws every state it declares and no two of its drawings are the same; it used to draw only the first, so a card listed up to nine treatments and showed one, and thirty-two of its sixty-eight drawings were the resting picture under another caption. Hover, pressed and disabled are the tone of a surface, and confirmation, offline, a pending mark, a danger glyph, a selection check and an age label are facts about the surface rather than the shape inside it, so those are drawn once for every component instead of seventeen times.',
      'The four stills of a motion card are one drawing at four points along the motif, and what differs between them is a share on the progress bar beside it. It used to switch the last frame to pending, inventing a state the motif never claimed to cross, and then to fade the drawing itself, which is what a spend on a motif means when every motif here spends opacity. Fifty-three drawings also carried a gradient and a rect nothing referenced, which is why a card could not afford its states.',
      'A still no longer fades. Every drawing in the gallery carries a label, and in Daylight that label is 4.64:1 on its own action fill, which is under the 4.5 C05 requires for normal text once it is dimmed by a fifth — so a frame has no dim available to it and the run fades the bar instead, which carries no words. A check composites the label and the caption in every profile and refuses an opacity on a frame, on the drawing inside it, or inline.',
      'Recorded deviation: the progress bar under a motion strip paints the horizon gradient at 3px. C27 gives horizon to one- and two-pixel light lines and says light never carries status, and a progress bar is status. It stays, because it reads as a deliberate choice and changing it silently would lose a visual nobody asked to lose.',
      "The release notes live in changelog.md alone, and the index points at them. They had become the larger half of the index, which is why its ceiling had to be raised twice and why a defect fix worth a fifth of a kilobyte was nearly deleted to fit under a limit the notes themselves had created. The index is a map of this document; the notes are the document's history.",
      'A scrim is painted as a scrim. The dialog drawing outlined its backdrop with the scrim token, which is a stacking tier rather than a colour, so the declaration was dropped and the shape was not on the page at all — the one component whose subject is a modal over a scrim was the one drawing with no scrim. It is now the void anchor at half strength, laid down before the modal. A painted token says which property it is read by, so a layer can no longer be painted as a colour, and a check holds every published page to that.',
      'The browser suite now watches every page it opens. It opened three — the width sweep, the parked-focus journey and the no-JS document — and attached its error and failed-response listeners to the first, so a throw in the two journeys most likely to fail passed silently while every assertion after them still held. Every catalog entry is now rewritten to the origin under test whatever that origin is, so a catalog that moves cannot be read from production and reported as a pass, and the URL the browser ends up on is compared with the path the registry declares.',
      "The landing pages are checked, which means the space holds budgets of its own. Four pages an origin publishes at its root — including the only right-to-left document on the site — were generated, linked, budgeted elsewhere and never looked at: every page check filtered on the specification's own path. They now carry the same one H1, unique id, live anchor, readable structured data and one attribute per element, are required to declare their direction from their locale, and are held to a ceiling the space declares. The specification's HTML, CSS and JavaScript budgets belonged to the specification, so adding the landing to them would have failed a gate that had only just started looking; the landing carries three of its own.",
      'A profile description states what its own profile carries. The note that a display profile has no interactive role sat in the template every profile was built from, so it was published thirteen times and three of those were about a profile the reader was not reading.',
      'The gallery stops asking for an accessibility it did not have. A selected chip filled with the identity accent and labelled it in ink, which measures 2.00:1, 2.96:1 and 1.00:1 across the three profiles that publish identity; selection is now a tonal treatment -- an accent ring and a check over the raised fill, with the label on the pair that reads. Gold focus is no longer borrowed to mark selection, so a selected state and a focused one are not the same ring. Heading order is a rule rather than a count, so a page cannot go from one h1 straight to h3; the four frames of a strip are named by position; a drawing is named by the component the registry publishes; the state matrix scrolls inside a focusable, named region like every other scroll container; the footer brand and the part download links carry the published target floor instead of a 29 px line box; and a motif the Static tier will not run is announced by a control that stays in the tab order, marked by aria-disabled rather than removed from it.',
      'Four sentences in this document contradicted another part of it. The surface budget said only the focal surface adds a rim border while the gradient grammar gives the rim to the focal or selected surface and the state matrix permits it, so the budget was amended to the answer the other two already gave. The same sentence required a surface called Canopy that no part of the registry publishes, so the one translucency allowance now names the role it uses and states the legibility floor without inventing a colour. The composited contrast document said eligibility was never stored per pair while every one of its 212 pairs stores it, so the sentence describes what it does. And two figures were typed into prose -- a role count and the version -- so both are now read from the record they belong to, and the checks that hold them are the reason a rule cannot name a surface the registry does not publish or a version the release record does not carry.',
      'C11 no longer types a number. Every star figure it states is read from the starfield module, which is the same source the agent brief reads, so the rule and the brief cannot disagree; and the Ink claim is restated as the cap the algorithm actually keeps -- at most a tenth of the layer in any one accent, so Ink is the majority -- because a 70% floor is not a property the generator can hold: measured, the accepted share is 69.6% on a 320px screen, where placement rejects one star of the layer it asked for. The clearance figure is gone with the token that carried it: the exclusion region is authored per surface in the same normalised coordinates the stars are placed in, and a published dimension nothing applied was a promise with no consumer.',
      'No anchor colour or section URL changed, so every section address stays valid.',
  ],
  // Every release before the one above, oldest first. Empty while 2.3.0 is the
  // only release that shipped; an entry moves here when `version` moves on.
  // Each entry is a version, the date that version shipped, and its notes: the
  // date is required rather than optional because the changelog, the release
  // feed and the sitemap all report it, and a release the origin can name but
  // not date is one a reader cannot order.
  history: [],
};

const percent = (n) => `${Math.round(n * 100)}%`;
const shape = ({ kind, at, radius, angle, from }) =>
  kind === 'radial'
    ? `Radial ${percent(radius[0])} × ${percent(radius[1])} at ${percent(at[0])} ${percent(at[1])}`
    : kind === 'conic'
      ? `Conic from ${from}° at ${percent(at[0])} ${percent(at[1])}`
      : `Linear ${angle}°`;
const stopList = (stops) =>
  stops
    .map(
      ([name, alpha, position]) =>
        `${name}${alpha === 1 ? '' : ` ${percent(alpha)}`}${position === 0 || position === 1 ? '' : ` @${percent(position)}`}`,
    )
    .join(' → ');

export const sources = [
  {
    name: 'WCAG 2.2',
    url: 'https://www.w3.org/TR/WCAG22/',
    use: 'Web accessibility baseline; Cosmos adopts a stronger 44 CSS px target than the 24 CSS px AA minimum.',
  },
  {
    name: 'Core Web Vitals',
    url: 'https://web.dev/articles/vitals',
    use: 'Field LCP, INP and CLS thresholds at the 75th percentile.',
  },
  {
    name: 'Wear accessibility',
    url: 'https://developer.android.com/design/ui/wear/guides/foundations/accessibility',
    use: 'Touch geometry, text and assistive input on round screens.',
  },
  {
    name: 'Wear always-on',
    url: 'https://developer.android.com/training/wearables/always-on',
    use: 'Ambient lifecycle, burn-in protection and battery-aware behavior.',
  },
  {
    name: 'Design Tokens format',
    url: 'https://www.designtokens.org/tr/2025.10/format/',
    use: 'Typed token interchange; exported sRGB components retain exact hex anchors in extensions.',
  },
];

export const provenance = [
  {
    repository: 'simt-landing (superseded by simt-space)',
    paths: ['cosmos-design/index.html', 'styles/cosmos-design.less', 'scripts/cosmos-design.js'],
    observed: 'One large guide; page-owned animated star canvases and mixed native/web examples.',
    direction: 'Replace with a static, linked specification with measurable rules and one optional atmospheric owner.',
  },
  {
    repository: 'simt-social-landing',
    paths: ['src/templates/partials/hero.hbs', 'src/templates/partials/atlas.hbs', 'src/styles/tokens.less'],
    observed: 'Narrative hero, activity atlas, role discovery, privacy and multilingual public content.',
    direction: 'Keep the human story and useful atlas; use editorial horizons and actual product evidence.',
  },
  {
    repository: 'simt-social',
    paths: ['app/styles/tokens-dark.css', 'app/routes/discover.tsx', 'mockups/README.md'],
    observed:
      'Canonical warm palette, persistent social shell, map/list discovery, activity composer and moderation console.',
    direction: 'Preserve route responsibilities; improve context continuity, scanning, recovery and density.',
  },
  {
    repository: 'simt-app',
    paths: [
      'docs/design/COSMOS_REDESIGN.md',
      'core/ui/src/main/java/app/simt/core/ui/theme/Color.kt',
      'core/ui/src/main/java/app/simt/core/ui/theme/Motion.kt',
    ],
    observed: 'Native instrument palette and typography; adaptive maps, dials and tools.',
    direction:
      'Task-first instruments with stable reference frames, deliberate reach zones and system-owned back behavior.',
  },
  {
    repository: 'simt-app/wear',
    paths: [
      'src/main/java/app/simt/wear/screens/HomeScreen.kt',
      'src/main/java/app/simt/wear/screens/CompassScreen.kt',
      'src/main/java/app/simt/wear/theme/WearColors.kt',
    ],
    observed: 'Compass, sky, geofence, plumb bob, targets and settings; cyan has a legacy tool-specific value.',
    direction: 'One-glance tools with shared signal meaning, round geometry and a genuinely quiet ambient state.',
  },
];

export const pages = [
  {
    slug: 'common',
    number: '00',
    label: 'Common foundations',
    title: 'One universe.\nA coherent language.',
    description:
      'The shared Cosmos contract: palette, type, geometry, components, accessibility, motion, data and production acceptance.',
    intro:
      'Warm depth, precise signals and generous quiet. Every platform shares the same meanings; each gives the task the space and input it needs.',
    signature: 'Clarity at the center. Atmosphere at the edge.',
    specimen: 'common',
    sections: [
      section('identity', 'Identity & authority', 'A useful spatial system, with room for imagination.', [
        rule(
          'C01',
          'Task before spectacle',
          'Put the primary object, current state and next action in the first useful viewport. A decorative motif may frame them but cannot displace them. Use literal operational copy even when the visual language is celestial.',
          'In grayscale, a first-time reader can identify the task, state and next step. Removing all ornament leaves a complete interface.',
        ),
        rule(
          'C02',
          'Shared core, four adapters',
          `Apply these common rules and the relevant platform part together. Product safety, privacy and accessibility requirements take precedence. Older prototypes and repository guides are migration evidence where they disagree with v${release.version.split('.')[0]}.`,
          `Each implementation brief names version ${release.version}, platform, applicable rule IDs, owner and any explicit exception with reason, evidence and review date.`,
        ),
        rule(
          'C03',
          'Original composition',
          "Build visual identity from the focus aperture, context spine and horizon ribbon. An aperture frames one meaningful object and is drawn as a quasar: a solid core in the object's channel, a corona halo and, where space allows, an accretion ring. A spine connects current context to the next step; a ribbon orders actual time or distance. Compose with continuous fields and aligned rows before adding cards.",
          'Every motif names the information it carries. One quasar aperture per screen marks the focal object. Cards contain individual records; no nested record cards or identical boxes around every page section.',
        ),
      ]),
      section(
        'palette',
        'Color with a job',
        'The original Deep Space colors are preserved exactly.',
        [
          rule(
            'C04',
            'Stable semantic channels',
            'Use Ember for primary agency, Rose for social attention, Violet for identity, Gold for focus/time/value, Cyan for live/location state and Moss for nature. Danger and Success are separate status channels. Limit competing large accents to one dominant and two supporting channels; categorized data may use the spectrum with labels.',
            'Tokens resolve to the anchors below. A hue never supplies the only meaning. Category Moss cannot imply a successful operation; Rose cannot imply an error.',
          ),
          rule(
            'C05',
            'Contrast is composited',
            'Require 4.5:1 normal text, 3:1 large text and 3:1 essential control boundaries, focus and graphical objects against adjacent colors. Use boundary for necessary input outlines; line is decorative only. Check every gradient stop, translucent surface and hover/disabled distinction.',
            'Measure the final composited pairs in each theme. The measurements are published once at contrast-pairs.json; fetch that document when introducing a colour, pairing a translucent or gradient surface or checking a theme, and do not expect a token payload to carry them. Document any inactive-control exception. Faint is supporting text only on a passing surface; Dim is never needed to complete a task.',
          ),
          rule(
            'C06',
            'Daylight, red and ambient',
            "Offer Daylight Nebula where supported. Dark remains the signature; a persisted explicit choice wins over the system preference, and with no choice the system preference is followed. Astronomy red is user-selected for instruments, uses only red luminance and retains text contrast. Watch ambient may use pure black for OLED efficiency. Theme adapters preserve the brand anchors and alter semantic role mappings, and an accent used as text takes the profile's accent-text role rather than its fill role.",
            'Theme switches retain layout, focus, selection and data. Red never flashes white/cyan on entry, dialogs or loading. Dark controls use dark on-action text; the existing Daylight Ember requires dark text, not assumed white. Test astronomy text on its action fill.',
          ),
          rule(
            'C29',
            'One colour truth, two notations',
            'Author and verify colour in sRGB and publish an oklch projection for the web parts. The anchors, gradients, shadows and semantic roles stay sRGB in the specification, because contrast is defined on sRGB luminance and every published contrast pair is measured there. Web tokens may be written in oklch from the projection in platformAdapters, so one role can step lightness without re-picking a hue. Android and Wear read sRGB: neither toolkit has an Oklab colour space, so a converted value would be a hex with extra steps.',
            'Every published web oklch resolves back to the sRGB value it came from within one 8-bit step, and every contrast claim is measured against the sRGB value. A colour is changed once, in hex, and both notations move together; no theme or component carries a colour absent from the registry.',
          ),
        ],
        table(
          'Deep Space token registry',
          ['Token', 'sRGB', 'oklch (web)', 'Role'],
          colors.map(([name, value, job]) => [name, value, oklch(value), job]),
        ),
      ),
      section(
        'light',
        'Light with a job',
        'Eight gradients and two shadows. Each one is a named instrument, never a texture.',
        [
          rule(
            'C27',
            'Gradient & glow grammar',
            "Use only the published gradient and shadow tokens, each for its stated job: field for root depth, nebula for one atmospheric field, corona and accretion for the single focal object, horizon for 1–2 px light lines, rim for the border light of the focal or selected surface, action for the one primary action and spectrum for the brand signature. A screen has at most one corona, one accretion ring and one nebula. Light carries depth and focus, never status, selection or the only boundary, and text never takes a gradient fill. In Full tier the hero figure runs one ambient motion: the focal object's accretion disc revolves once every 40–90 s about the core, and every other light is static — including the accretion ring itself, because a boundary that moves stops being a boundary. Sky drift (C11) is the one alternative a screen may take instead. Deep Space uses the whole grammar, Daylight keeps horizon and spectrum, and astronomy red and watch ambient use none. A gradient token a profile does not run resolves to no gradient, so the element falls back to its flat role color instead of borrowing another profile's light.",
            'Every gradient and shadow in code resolves to a token name and its published geometry. Text over a gradient passes against every stop as an opaque pair. Removing all light leaves hierarchy, selection and boundaries intact. The revolving disc moves only `rotate` or `transform`, never a length, and is the only animation on the screen. Forced colors and astronomy red replace light with solid system or semantic borders.',
          ),
        ],
        table(
          'Gradient registry',
          ['Token', 'Geometry', 'Stops', 'Job', 'Profiles'],
          gradients.map((g) => [
            `gradient.${g.name}`,
            shape(g.geometry),
            stopList(g.stops),
            g.job,
            g.profiles.join(', '),
          ]),
        ),
      ),
      section(
        'atmosphere',
        'Stars at the edge',
        'One deterministic sky per screen, generated the same way on every platform.',
        [
          rule(
            'C11',
            'One deterministic sky',
            `A screen, hero, panel or dialog background may own one edge star layer from the published starfield algorithm: stars only in the outer ${percentValue(starfield.band)}% band, ${starDensity('screen')} for screens and ${starDensity('dialog')} for dialogs, three magnitudes, at most ${percentValue(starfield.maxAccentShare)}% of the requested layer in any one accent so Ink is always the majority, and at most ${starfield.maxSpikes} diffraction-spiked stars. Seed it from a stable surface ID and a ${starfield.bucket} px/dp size bucket. Exclude known text and controls by authoring the region per surface, in the same normalised coordinates the stars are placed in, and never reseed to avoid them. Records, maps, media, data canvases, sky charts and watch screens own no stars.`,
            `The same surface ID and bucket reproduce the published fixture on every platform. The layer is one cached bitmap, texture or compositor layer, never an element or view per star. Full tier may drift the whole layer once per ${starfield.drift.minPeriodS}–${starfield.drift.maxPeriodS} s by at most ${percentValue(starfield.drift.maxTravel)}% of the shorter edge; Reduced, Static, Save-Data and hidden states freeze it, and low power draws ${percentValue(starfield.lowPowerFactor)}% of the stars. Removing the layer changes no semantics or available task.`,
          ),
        ],
        table(
          'Atmosphere by surface',
          ['Surface', 'Field & nebula', 'Star layer', 'Focal light', 'Lines'],
          [
            [
              'Web page hero',
              'Field and one nebula behind the focal object',
              'Screen density; masked from the narrative column',
              'Corona and accretion on the emblem or selected record',
              'Horizon top light; spectrum signature',
            ],
            ['Web page reading', 'Field only', 'None', 'None', 'Horizon section signature'],
            [
              'Web app shell',
              'Field; no nebula',
              'Dialog density on sign-in, onboarding and empty states; never over a map or list',
              'Corona on the selected marker',
              'Horizon top light on the context pane',
            ],
            [
              'Mobile instrument',
              'Field and one Full-tier nebula',
              'Screen density, cached once per screen',
              'Corona and accretion on the target',
              'Horizon top light on sheets',
            ],
            [
              'Mobile sheet & dialog',
              'Inherits the screen field',
              'Dialog density',
              'None',
              'Horizon top light; rim on the focal sheet',
            ],
            ['Mobile sky & orrery', 'Field only', 'None: every mark is data', 'Corona on the selected body', 'None'],
            ['Watch interactive', 'Solid Void', 'None', 'One static corona on the bearing or target marker', 'None'],
            ['Watch ambient', 'Pure black', 'None', 'None', 'None'],
            ['Daylight', 'Flat background', 'None', 'None', 'Horizon and spectrum'],
            ['Astronomy red', 'Flat red background', 'None', 'None', 'Solid red boundaries only'],
          ],
        ),
      ),
      section(
        'typography',
        'Readable at every scale',
        'A confident display voice, with a quieter reading voice.',
        [
          rule(
            'C07',
            'Families & language',
            'Use Space Grotesk for Latin titles and instrumentation, Source Sans 3 for web reading and form text, native system body type on Android, and El Messiri for Arabic roles. Bundle fonts, declare fallbacks and reserve fallback geometry. Use zero tracking by default; uppercase and spaced labels are Latin-only enhancements.',
            'No third-party font requests. Arabic has no mixed-script heading caused by an unsupported font. Font fallback and final font both retain unclipped text and controls.',
          ),
          rule(
            'C08',
            'Roles & number semantics',
            'Use the roles below and rem/sp for text. Body line-height is 1.5–1.65; Arabic uses at least 1.6 with tested diacritic room. Reading columns are 55–72 characters. Instrument digits use tabular numerals, visible units and stable width; precision follows actual sensor/data accuracy.',
            'Web reflows at 400% on a 1280 CSS px viewport and text zoom at 200%; Android fontScale 2.0; watch largest system text. No essential label is clipped or replaced by an ellipsis-only value.',
          ),
        ],
        table(
          'Type roles; size / line height',
          ['Role', 'Web', 'Mobile', 'Watch'],
          [
            ['Narrative display', '48–88 / 1.05, fluid rem', '40–48sp / 1.1, entry only', 'Not used'],
            ['Primary instrument', '48–72 / 1.1', '48–64sp / 1.1', '32–40sp / 1.1'],
            ['Page title', '28–36 / 1.2', '28–32sp / 1.2', '18–20sp / 1.25'],
            ['Section title', '20–24 / 1.3', '20–24sp / 1.3', '16–18sp / 1.3'],
            ['Body', '16–18 / 1.6', '16sp / 1.5', '14–16sp / 1.4'],
            ['Supporting', '14 / 1.5', '14sp / 1.45', '12–14sp / 1.4'],
            ['Control label', '14–16 / 1.3', '14–16sp / 1.3', '14–16sp / 1.3'],
          ],
        ),
      ),
      section(
        'geometry',
        'Field, spine, aperture',
        'Geometry should organize attention, not create visual noise.',
        [
          rule(
            'C09',
            'Rhythm & mapped shape',
            `Use ${spaceList()} units. One unit is a CSS px or native dp, so the same numbers serve every platform and none of them publishes a spacing scale of its own. Take corner geometry from the platform mapping published in tokens.json rather than from a local scale: ${radiusList('webApp', 'web')} on web, ${radiusList('android', 'native')} on Android, and the native round and capsule shapes on Wear, which publish no dp radius at all. A part that needs another role adds it to the mapping first. Circles denote instruments or compact icon controls, not paragraph containers.`,
            "Spacing and shape map to named tokens, and a component's corner radius matches its role in the mapping for the part it ships in. No screen carries an ad-hoc px or dp corner value, and a native shape is expressed as the native shape rather than approximated with a number. The Wear layout fits its round screen instead of shrinking its targets. Small-screen compression first removes ornament and gutters; it does not shrink the readable type or hit target.",
          ),
          rule(
            'C10',
            'Surface budget',
            'Use opaque Hull for reading and work. A panel, sheet or dialog may carry a horizon top light inset from its corners; only the focal or selected surface adds a rim border, which is the answer C27 gives. One header or contextual overlay may sit over imagery at no less than 88% opacity with an opaque fallback, in the highest surface role and never in a colour of its own. Allow at most one backdrop blur region per visible screen, capped at 12px on web; disable it when measured costly. Borders establish hierarchy before light.',
            'The screen remains legible with blur unsupported or disabled. No live blur behind scrolling long copy, dialog inside a blurred sheet, or full-screen animated haze. Top light never touches a rounded corner at full intensity.',
          ),
          rule(
            'C25',
            'Icons, imagery & diagrams',
            'Use one consistent icon family per platform: Lucide on web or the established native library. Standard marks are 20–24px/dp with a consistent stroke; the target remains 44px/48dp. Custom marks are reserved for concepts without a familiar equivalent. Images show the real object/place/person with a declared crop and reserved aspect ratio; diagrams have text equivalents.',
            'Decorative icons are hidden from assistive technology. Icon-only controls have localized names; unfamiliar commands retain visible labels. Mirror navigational arrows only. No duplicate icon font, raster text, misleading geographic imagery or unsupported scientific precision.',
          ),
          rule(
            'C26',
            'Layering & scroll ownership',
            'Keep field, persistent chrome, contextual popover, modal and urgent feedback in that semantic order using a shared layer registry. Give each pane one scroll owner; the whole page reflows before adding nested scrolling. Bounded overflowing lists expose native scrolling and a visible edge/next-item or explicit navigation affordance. A fade cannot cover essential labels or intercept input.',
            'Popover/dialog/focus are never clipped by a record or overflow ancestor. Touch, wheel, keyboard and rotary reach all content. Scroll position and focus survive return from detail; closing a layer restores the correct owner. Reduced mode uses static scroll cues.',
          ),
        ],
        geometryMapping(),
      ),
      section(
        'components',
        'Component contracts',
        'The same behavior travels; shape and density adapt.',
        [
          rule(
            'C12',
            'Actions & selection',
            'Give a task region one visually dominant Ember action, filled with the action gradient and resting on the action shadow. Destructive actions use Danger and consequence-first confirmation where irreversible. Buttons expose default, hover, pressed, focus, pending, disabled, error and confirmed states. Selection uses shape/check plus aria-pressed, aria-selected or native semantics, while Gold focus is independent.',
            'Click/Enter/Space trigger once. Pending preserves the accessible name and width, exposes busy state and prevents duplicates. Confirm only after success; error restores safe retry without losing input.',
          ),
          rule(
            'C13',
            'Inputs & validation',
            'Keep visible labels; connect hint, requirement, counter and error. Use real input/select/radio/checkbox/switch semantics. A nonempty editable single-line field exposes a 44px web/48dp native Clear button that returns focus to the field. Do not add it to password, read-only or disabled fields. Validate on submit or meaningful exit without announcing every keystroke.',
            'Keyboard autofill, paste, password managers, IME and RTL text work. Invalid fields expose invalid state and a text error; submit focuses the first error or linked error summary. Safe values survive retries.',
          ),
          rule(
            'C14',
            'Records & navigation',
            'Keep records in aligned rows unless media/object identity justifies a card. Do not wrap nested links/buttons in another interactive target. Back follows history; Close dismisses a layer. Selected navigation combines label, boundary and programmatic current state. Navigation persists where the task does.',
            'Tab order matches reading order. Route navigation moves focus to a named destination heading; dialogs return focus to a surviving trigger. Back/forward restore filter, position and context as applicable.',
          ),
          rule(
            'C15',
            'Dialog, sheet & popover',
            'Use one modal layer with a visible title, Close action, safe dismissal policy and actions in reading order. Web uses native dialog or an audited equivalent with focus containment, Escape and background inertness. Mobile uses platform back and inset-aware sheets. Popovers anchor to triggers and reflow before viewport clipping.',
            'Focus cannot enter the background of a modal. Unsaved edits get an explicit discard decision. Last action is reachable with the keyboard open and large text. Tooltips never contain the only critical instruction.',
          ),
          rule(
            'C16',
            'Feedback at the source',
            'Use inline errors for local failures, a banner for screen-wide degraded state, and status/toast for confirmed transient results. Every primary surface designs first load, refresh/stale, empty, denied, offline, recoverable error, terminal unavailable and success. Loading reserves real geometry; shimmer is optional and never indefinite.',
            'Screen readers hear concise changes once; high-rate sensor updates are not live-announced each frame. Error copy says what happened, what remains safe and which action recovers. A toast is never the sole record of a failure.',
          ),
        ],
        [stateMatrix(), componentIndex()],
      ),
      section(
        'motion',
        'Motion earns its place',
        'Use movement to explain a change. Silence is a valid finish.',
        [
          rule(
            'C17',
            'Three effective tiers',
            `Full uses ${durationList()} and the curve published for the part (C30). Reduced removes spatial travel and ambient loops; optional opacity feedback lasts at most 150ms. Static applies final state immediately. Explicit Static wins; reduced-motion, data saving, low power, thermal pressure and visibility can only lower the tier.`,
            'Preference changes apply during the session. Full/Reduced/Static produce identical outcomes. Hidden screens and background tabs stop animation and sensor presentation. Hardware concurrency alone never determines ability or tier.',
          ),
          rule(
            'C18',
            'Progress & moving data',
            'Animate transform/opacity for transitional UI, avoiding width/height/blur animation. Never auto-pan a map during a user gesture. Sensor smoothing cannot conceal stale/low-confidence data; separate visual interpolation from the stored/announced reading. Auto-updating content provides pause or stable inspection when applicable.',
            'No flashing beyond accessibility limits, parallax-dependent meaning, autoplay sound or attention-pulsing CTA. Reduced motion does not disable accurate data. Waiting is stated as text, not inferred from a spinner.',
          ),
          rule(
            'C30',
            'Easing is published per part',
            'Use the easing set published for the part: web uses the standard curve with emphasis, entrance and exit for arriving and departing UI; Android and Wear use the standard curve with enter, exit and transform. Every part runs the same three durations; a part that needs another curve or duration publishes it in the mapping instead of defining one locally.',
            'Every transition resolves to a published curve, arrival and departure use opposite curves from the same set, and the reduced and static tiers still reach the final state without a curve. No component hard-codes a cubic-bezier, and a curve change is a mapping change rather than a silent divergence.',
          ),
          rule(
            'C31',
            'One motif per state change',
            'Animate a state change by naming the published motif whose job is that change, and spend only the properties, duration and easing tokens that motif lists. A change with no motif does not animate. A new choreography is added as a motif with its own job rather than designed once inside a component. The motifs are published in motion.json, and the curve parity fixture in that document is how a native port proves it matches.',
            'Every animated state change names a motif, and every motif in motion.json is reachable from at least one surface. No motif animates a property outside its allowed list, and none loops or repeats to hold attention: stale data dims and gains a label. Reduced removes travel and keeps an opacity change within the feedback duration; Static settles immediately and reaches the same final state as Full. A port reproduces the sampled curve values to the published precision, or the mapping is wrong.',
          ),
        ],
        [easingMapping(), motifMapping()],
      ),
      section(
        'accessibility',
        'Everyone keeps their bearings',
        'Input, language and physical access are part of the design.',
        [
          rule(
            'C19',
            'Access baseline',
            'Target WCAG 2.2 AA on web and equivalent native behavior. Adopt 44×44 CSS px web targets and 48×48dp Android/Wear targets; design the round watch layout to fit rather than shrinking targets. Provide visible focus, names/roles/values, heading structure, error relationships, alternatives to dragging and a skip link.',
            'Complete primary flows with keyboard, touch and a screen reader. Focus is not covered by sticky chrome. Forced colors preserves borders, icons and focus; controls use system colors where needed.',
          ),
          rule(
            'C20',
            'Direction & localization',
            'Use logical spacing and placement. Mirror navigation flow, never geography, north, compass mathematics or media playback. Isolate authored text, handles, coordinates and times. Localize visible copy, relative dates, plural counts, measurement units and accessible names. Persist unit preference independently of language.',
            'Test English, French, Arabic RTL and Spanish with long content and 200% expansion fixtures. Geographic points and bearing values are identical across directions. Critical facts remain readable in the full detail view.',
          ),
          rule(
            'C21',
            'Privacy & honest content',
            'Represent accuracy, confidence, time zone, stale data, approximate location and permission state explicitly. Do not imply live data from decoration. Keep private coordinates, profile fields, draft text and sensitive actions out of URLs, crawled pages, analytics and error messages. Use illustrative labels for invented specimen data.',
            'Privacy decisions are explained at the action. Public previews reveal only authorized data. A denied or blocked profile does not disclose which private condition caused its unavailability.',
          ),
        ],
      ),
      section(
        'implementation',
        'Structured delivery & release',
        'Make the rules usable by people and machines.',
        [
          rule(
            'C22',
            'One source & stable addresses',
            'Maintain this specification in cosmos/. Generate HTML, plain Markdown, the typed token file and the complete specification JSON together. Stable rule IDs and section URLs are public contracts, and each part publishes its own tokens and rules payload carrying the common foundations plus only its own adapter. Return real files with correct MIME types; machine requests must not silently receive the home page.',
            'Generated-output drift fails verification. All routes, anchors, cross-links and downloads resolve in development, preview and production. Manifest includes version, status, scope, assets, platform inheritance and source provenance. A part payload is byte-identical to the canonical documents for everything it shares, carries exactly one adapter, and stays inside its published byte budget, so it cannot drift back into the union. A document needed only sometimes keeps its own address: composited contrast measurements are fetched from contrast-pairs.json instead of being embedded in a payload, and every published document states when to fetch it.',
          ),
          rule(
            'C23',
            'Performance is measured',
            'Use the platform budgets and release matrices. Web field targets are p75 LCP ≤2.5s, INP ≤200ms and CLS ≤0.1 for mobile and desktop cohorts. Lab tests are diagnostic, not a substitute for field results. Record route, build, device, browser/OS, network, locale, motion tier and method with evidence.',
            'A release has a named owner and real measurements. Visual specifications and mockups are never labeled proof that unimplemented products meet their runtime targets.',
          ),
          rule(
            'C24',
            'Conformance & maintenance',
            'Release only after applicable rules pass contrast, input, localization, state and performance checks. Record exceptions with affected rule, product reason, accessible alternative, owner and expiry. A palette or semantic-rule change requires a version update and migration note; minor additions preserve stable IDs.',
            'The release record links screenshots, accessibility findings, performance traces and resolved defects. Core maintainers review shared changes; platform maintainers review adapter changes. Broken required flows block acceptance.',
          ),
        ],
        table(
          'Implementation content model',
          ['Field', 'Required structure', 'Purpose'],
          [
            ['Rule', 'id, title, level, requirement, acceptance', 'Stable conformance mapping'],
            ['Platform', 'slug, description, sections, inheritance', 'A bounded adapter with common dependencies'],
            ['Token', '$type, $value, $description; semantic adapter', 'Typed values; explicit units and color space'],
            [
              'Screen brief',
              'task, privacy class, layout, states, input, data, rules',
              'Enough context to implement without guessing',
            ],
            ['Evidence', 'version, build, route, environment, result, owner', 'Traceable release decision'],
          ],
        ),
      ),
    ],
  },
  {
    slug: 'web-page',
    number: '01',
    label: 'Web page',
    title: 'A horizon\nworth exploring.',
    description:
      'Cosmos public and marketing websites: editorial horizons, a useful atlas, product evidence, conversion, SEO and restrained motion.',
    intro:
      'Based on simt-social-landing. Create an invitation to real places and real people, with an open editorial composition and a glimpse of the product that feels usable.',
    signature: 'An editorial horizon, not a wall of cards.',
    specimen: 'web-page',
    sections: [
      section(
        'composition',
        'The editorial horizon',
        'The first screen feels spacious and explains the product immediately.',
        [
          rule(
            'P01',
            'Two complementary planes',
            'Use a left-aligned narrative column and an asymmetric atlas on wide screens. The horizon is a fine shared baseline linking headline, time and place. On compact screens, show the proposition, one CTA and a cropped but meaningful atlas in normal reading order. The hero uses content height, not forced 100vh. It owns the page sky and its one nebula sits behind the atlas, never behind the headline.',
            'At 390×844, the product purpose and primary CTA appear without waiting for animation. At 320px and large text, the hero grows with content rather than clipping it.',
          ),
          rule(
            'P02',
            'Product evidence over decoration',
            'The atlas shows representative activity records, time labels and approximate places. Label illustrative data. Use contour lines and a few weighted nodes to show relationships; do not mimic a production map with fake live participants. Include one real product screenshot or an accessible product walkthrough below the hero.',
            'Every showcased interaction has an honest response or is a noninteractive figure. No fabricated member counts, testimonials, countdown urgency or unverifiable availability claims.',
          ),
        ],
        table(
          'Public page sequence',
          ['Sequence', 'Composition', 'Decision answered'],
          [
            ['01 Invitation', 'Narrative + atlas + primary CTA', 'What can I do here?'],
            ['02 Proof', 'One real experience with annotated outcome', 'Does the product deliver it?'],
            ['03 Path', 'Discover → inspect → participate as a connected spine', 'How does it work?'],
            ['04 Possibilities', 'Role/category rows with real examples', 'Is this relevant to me?'],
            ['05 Trust', 'Plain privacy and location explanation', 'What am I sharing?'],
            ['06 Questions', 'Native disclosure FAQ', 'What still stops me?'],
            ['07 Next step', 'A calm final CTA and legal/locale links', 'Where do I go next?'],
          ],
        ),
      ),
      section('layout', 'Responsive composition', 'Keep the visual rhythm while changing the arrangement.', [
        rule(
          'P03',
          'Grid & navigation',
          'Use a 1200px content maximum, 12 columns expanded, 6 medium and 4 compact. Gutters are 48px at ≥1200, 32px at 768–1199 and 20px below 768. Narrative blocks alternate open field, ruled rows and a single focused evidence panel. Header is 68px minimum with real anchor links; mobile menus are accessible disclosures.',
          'At 320, 390, 768, 1024 and 1440px no page overflow or collision. Header can grow at large text; anchor headings remain unobscured. At 400% zoom navigation stays reachable.',
        ),
        rule(
          'P04',
          'Type & emphasis',
          'Use display 48–88px expressed in fluid rem, maximum 12–16 words, line-height ≥1.05. Body is 18px at comfortable widths and ≥16px compact. Keep the headline solid Ink; Ember accents one meaningful phrase. Show the primary action in the action gradient; allow a secondary text link with an explicit destination.',
          'Headline wraps naturally in French and Arabic. No critical information is gradient text or microsized metadata. Equal prominence is not given to every link.',
        ),
      ]),
      section('atlas', 'An atlas that tells the truth', 'Give place and time an understandable structure.', [
        rule(
          'P05',
          'Narrative atlas behavior',
          'Use a small inline SVG or pre-rendered image for the public hero, with a nearby text list of representative records. If interactive, use native buttons to select a record and show a labeled detail region; all records work by keyboard/touch. Map controls must have real meaning. Reuse the same selected record in text and visual representations.',
          'The page remains complete with JavaScript disabled and reduced motion. Selection is visible by label and shape. Decorative geography is marked schematic; exact private coordinates never enter the illustration.',
        ),
        rule(
          'P06',
          'Timelines & categories',
          'Use a horizon ribbon to order real dates or steps, with direct labels and connected milestones. Category color follows common meanings; category filters pair a label with count or selected state. Do not add an auto-scrolling marquee to communicate essential content.',
          'All milestone and category content is available as text. Horizontal overflow has an obvious scroll or navigation affordance without fading essential labels.',
        ),
      ]),
      section('conversion', 'Confident, calm conversion', 'Make the destination and commitment clear.', [
        rule(
          'P07',
          'CTA contract',
          'Primary CTA names the next action and its actual destination. Preserve direct hrefs, keyboard activation and expected new-tab behavior. Explain sign-in or install requirements beside the relevant action. Use no looping CTA pulse, hover-only action, modal interruption or surprise auto-navigation.',
          'A CTA works without client JavaScript. Track only allowlisted coarse events; never block navigation on analytics. External destinations and download labels match what opens.',
        ),
        rule(
          'P08',
          'Public content & localization',
          'Use one H1, a meaningful H2 outline, semantic landmarks, purpose-based alt text and visible legal/contact links. Localized pages use correct lang/dir, self-canonical URLs and reciprocal hreflang. Dates and assertions have a source/owner; legal copy is maintained independently of promotional language.',
          'All key text is in the initial HTML, not canvas or image-only type. Arabic reading order and DOM order agree. Each locale has a complete primary journey and no mixed untranslated navigation.',
        ),
      ]),
      section('delivery', 'Fast by construction', 'The atmosphere should be cheaper than the story.', [
        rule(
          'P09',
          'Delivery budgets',
          'Target ≤30KiB gzip initial JavaScript, ≤35KiB gzip CSS, ≤120KiB initial font files and ≤250KiB initial hero media. Ship responsive AVIF/WebP with dimensions; load the actual LCP image eagerly and later media lazily. Load no full map engine, animation framework or video before it is requested.',
          'Check production compression and request waterfall on a cold load. No unused font preload. Measure Core Web Vitals with constrained mobile lab diagnostics and real field cohorts after launch.',
        ),
        rule(
          'P10',
          'Enhancement lifecycle',
          'At most one optional compositor-only ambient hero motion in Full; stop it when out of view or page hidden. Content never starts invisible awaiting an observer. Reduced and data-saving modes show final static compositions. Third-party tags are isolated from critical rendering and governed by the product consent policy.',
          'No animation-frame loop runs while scrolling the reference page. The first paint remains useful if script fails. Layout shift stays within the shared CLS budget.',
        ),
      ]),
      section('release', 'Web page acceptance', 'Verify the narrative and the real path out of it.', [
        rule(
          'P11',
          'Public release matrix',
          'Verify home, FAQ, legal, locale switch and each CTA at compact/medium/expanded widths; English, French and Arabic; keyboard, screen reader, no-JS, reduced motion and forced colors. Run focused Chromium/Firefox/WebKit checks for shared layout and browser APIs. Test real mobile Safari/Android at release.',
          'Evidence includes first viewport and full page, link/status checks, metadata parity, accessibility findings and asset budgets. Do not advertise runtime or accessibility conformance based on screenshots alone.',
        ),
      ]),
    ],
  },
  {
    slug: 'web-app',
    number: '02',
    label: 'Web app',
    title: 'Every signal.\nIn context.',
    description:
      'Cosmos for Simt Social: continuous map/list discovery, context-preserving detail, activity composition, profiles, inbox and operations.',
    intro:
      'Based on simt-social. The map establishes where; an aligned stream explains what; a context spine keeps the next decision close. The shell stays predictable while the experience becomes richer.',
    signature: 'One field, one selected object, one clear next step.',
    specimen: 'web-app',
    sections: [
      section('shell', 'A persistent social field', 'Preserve spatial context without trapping people in a map.', [
        rule(
          'A01',
          'Shell geometry',
          'At ≥1200px use a 216px labeled rail, flexible discovery field and a 360–420px context pane when selected. At 768–1199px use a 72px rail with accessible names and a 320–360px detail overlay. Below 768px use a 56px top context bar and a ≥64px bottom dock plus safe area. Map/list is a labeled mode switch, not a separate feature implementation.',
          'Dock and rail show Discover, Inbox, Saved and Profile; create remains one stable primary command. The shell accommodates large text through vertical growth or a labeled collapsed layout, never label clipping.',
        ),
        rule(
          'A02',
          'Context spine',
          'Place location scope, active time/filter summary and result count on one stable context spine. Selected detail begins with identity, date/time, place precision and participation state; the primary action follows those facts. Keep maps mounted across sheets when geography is part of the task, but release inactive hidden routes.',
          'Closing detail restores the previous selected marker/list row, focus and scroll. Browser back restores context. Filters and result count agree across map and list; manual map exploration never silently turns on follow-location.',
        ),
      ]),
      section('discovery', 'Discover by place, time or people', 'Dense information, with calm priority.', [
        rule(
          'A03',
          'Map & list parity',
          'Use Petrol land and Water, warm low-contrast roads and labeled category markers. Clusters show counts, the selected marker gets the quasar aperture and a label, current location shows confidence radius plus text. Render map objects with native map layers/feature state; expose the same authorized results in a semantic list. Location permission is requested only when needed.',
          'Every meaningful map action has a list/keyboard equivalent. Marker targets expand to 44px; overlaps resolve through clusters or a chooser. Attribution is visible and not covered. Unavailable tiles leave a useful list and retained filters.',
        ),
        rule(
          'A04',
          'Horizon ribbon',
          'Order activity results by actual local start time with visible date groups and explicit time zone when relevant. A compact time ribbon may select a range, paired with an editable date/time control and result summary. Live/stale labels refer to the real data freshness policy, not animation.',
          'DST, past/current/upcoming and locale dates are correct. Manual keyboard/touch date selection is possible. Zero matches explains the active scope and offers reset without discarding unrelated choices.',
        ),
        rule(
          'A05',
          'Activity record',
          'Display title, date/time, coarse place, role/category, participation/capacity and owner identity in stable tracks. One primary target opens details; Save and other commands are separate controls. Image ratio is 3:2 for a media card; text rows remain a valid low-data representation.',
          'Long titles wrap to two lines in summary with the full value in detail. Missing images have a purposeful placeholder. Loading, zero results, denied data, offline cached results and removed activities keep a coherent layout.',
        ),
      ]),
      section('participation', 'Honest participation', 'An action is meaningful only when its result is clear.', [
        rule(
          'A06',
          'Detail & participation states',
          'Design available, pending approval, confirmed, waitlisted, full, cancelled, finished and unavailable states where the backend supports them. Distinguish requesting participation from being accepted. Place consequences, privacy and location precision beside the action. Do not invent a backend capability to satisfy a visual.',
          'The UI maps each supported server state explicitly; unsupported target capabilities are recorded as contract dependencies. Pending mutation is guarded, failure rolls back safely, and confirmed counts come from authoritative data.',
        ),
        rule(
          'A07',
          'Saved, profile & inbox',
          'Reuse record geometry for Saved; profile opens with identity and authorized roles before activity rows. Inbox separates actionable unread events from history, with text and a leading marker rather than glow. Timeline messages preserve drafts on failure and retain read-only/removed/access-revoked states.',
          'Read/unread changes update counts consistently. Public profiles are permission-safe. Owner-only controls are absent for other users; denied routes provide recovery without leaking private conditions.',
        ),
      ]),
      section('composer', 'Compose with continuity', 'Keep the idea intact while people solve the details.', [
        rule(
          'A08',
          'Four-step composer',
          'Use Idea → Moment → Place → Review, an always-readable progress spine and a concise draft summary. Each step validates its owned fields; users can go back without losing safe values. Preserve product capacity/range limits from the real API contract. Show exact/approximate place choice and its audience consequences in Place and Review.',
          'No false autosave claim: label saved only after the supported persistence succeeds. Draft resume, discard, upload progress/failure, permission denial, version conflict and submit failure are designed. Review displays the exact publish audience and location precision.',
        ),
        rule(
          'A09',
          'Forms & destructive changes',
          'Use a 640px reading/form maximum, stable labels and grouped sections separated by rules. Sticky submit belongs to the form and respects keyboard/safe area. Confirm deletion with object name and consequence, without coercive wording. For stale concurrent edits, present an explicit refresh/review choice and preserve the local draft.',
          'Submission errors link to fields; network failure retains input. Double activation cannot create two activities. Disabled controls state the unmet requirement when a person can resolve it.',
        ),
      ]),
      section(
        'operations',
        'A precise operational console',
        'The same palette, with quieter structure and stronger evidence.',
        [
          rule(
            'A10',
            'Moderation & administration',
            'Use semantic tables with direct status labels, stable column alignment, consequence summaries and traceable decisions. Separate queue, subject evidence and decision area; never put each cell in a card. Honor the existing desktop-only moderation policy on compact widths with a useful desktop-required explanation.',
            'Unauthorized tools do not render. Permission revocation fails closed. Conflict, stale evidence, audit failure and terminal state block unsafe actions. Sensitive evidence never appears in public previews, telemetry or exports without authorization.',
          ),
        ],
      ),
      section('performance', 'A responsive working set', 'Responsiveness includes memory, map work and recovery.', [
        rule(
          'A11',
          'Loading & runtime budgets',
          'Target ≤180KiB gzip initial app JavaScript excluding the deferred map engine, ≤50KiB gzip shared CSS and ≤120KiB initial fonts. Render a useful list/shell before map readiness; defer admin/composer/media code. Bound visible records, images and map sources to the task; virtualization must preserve accessibility, focus and findability.',
          'On a named baseline device, repeated filter/detail/back cycles do not cause monotonic retained-memory growth. Map and list retain selection. Long tasks are diagnosed; field Core Web Vitals meet common targets before release acceptance.',
        ),
        rule(
          'A12',
          'Secure rendering & recovery',
          'SSR public content and safe shell data only; authenticate before rendering private payloads. Authenticated responses default to private, no-store unless a reviewed isolation policy permits caching. Offline data is shown only when policy permits, with timestamp and safe recovery. Never trade data isolation for instant-looking transitions.',
          'Multi-account switching cannot reveal previous private data. Rendered errors are localized and safe; technical error IDs may appear only as support metadata. Retry is bounded and cancel/abort is respected.',
        ),
      ]),
      section('release', 'Web app acceptance', 'Ship a complete task slice, including the hard states.', [
        rule(
          'A13',
          'Route matrix',
          'Review sign-in/onboarding, map/list discovery, detail/participation, create/edit, saved, inbox/timeline, profile/settings, account states, share/unavailable and authorized admin routes. Compare 390, 768, 1024 and 1440px, Arabic RTL, large text, forced colors and every motion tier. Exercise keyboard and screen-reader flows with real supported contract fixtures.',
          'Evidence names the direct composition, build and tested routes. A missing denied/offline/conflict state or inaccessible map-only task blocks the affected feature; unrelated routes are not claimed validated.',
        ),
      ]),
    ],
  },
  {
    slug: 'mobile-app',
    number: '03',
    label: 'Mobile app',
    title: 'A precise instrument.\nWithin reach.',
    description:
      'Cosmos for SIMT Android: compass, map, sky, tracking and tool layouts, adaptive postures, native input, sensors and measured performance.',
    intro:
      'Based on simt-app. Put the reading in a stable reference frame, the explanation beside it and the command under the thumb. A sense of wonder follows precision.',
    signature: 'A fixed reference frame and a deliberate command zone.',
    specimen: 'mobile-app',
    sections: [
      section('anatomy', 'The instrument field', 'The screen explains its measurement before asking for an action.', [
        rule(
          'M01',
          'Three useful zones',
          'Compose a native context header, flexible instrument field and bottom command zone. The header names the tool, target and confidence/connection state. The field prioritizes one reading with units and a real reference frame; it owns the screen sky, and its quasar aperture marks the target. The command zone contains the primary action and essential secondary controls above gesture/navigation insets.',
          'On compact portrait, the reading and command are both reachable. At fontScale 2.0, switch to a scrollable reading/controls composition rather than clipping the dial or shrinking text.',
        ),
        rule(
          'M02',
          'Navigation & continuity',
          'Use the existing product destinations and native back stack. Stable destinations may use a bottom bar; tool commands do not become destinations. Preserve target and measurement context across sheets and tool switches where appropriate. Use standard predictive back, native IME behavior and one modal dispatcher.',
          'System back, gesture back and explicit Back agree. Close dismisses only its layer. Deep links handle missing permission, invalid target and authentication safely and preserve an understandable return path.',
        ),
      ]),
      section('instruments', 'Fixed frame, living reading', 'Design instruments that are legible outside a studio.', [
        rule(
          'M03',
          'Compass & bearing',
          'Keep the physical north/reference semantics consistent. A dial may rotate with heading while the target bearing is labeled; define that convention visibly. Use direct cardinal labels, a distinct current-direction pointer, target marker and numeric delta. Reserve Cyan for actual live/location state and Gold for precision.',
          'Known headings 0/90/180/270 and 359→0 transitions are correct. True/magnetic reference and accuracy are explicit. Arabic changes control order, never the sensor mathematics. No reading is presented as valid during missing/calibrating/stale state.',
        ),
        rule(
          'M04',
          'Sky, moon & orrery',
          'Use one inspectable field with selected-body details adjacent, labeled rise/set/time zone and actual visibility conditions. Orbital paths encode real relationships and always have a list/text equivalent. Red mode preserves dark adaptation across menus, tooltips and loading. These fields own no decorative star layer: every mark is data.',
          'Data timestamps and unavailable calculation states are explicit. Small marks get expanded hit regions or a chooser. Screen readers can select a body and read its principal facts without exploring pixels.',
        ),
        rule(
          'M05',
          'Map, tracks & links',
          'Use map-native geometry, visible scale/attribution, trace direction and labeled start/current/end states. Tracking distinguishes running, paused, stopping, saved and failed persistence. A share/link preview makes target precision and audience clear before leaving the device.',
          'Location denial, coarse-only permission, unavailable tiles, offline coverage, sensor interruption and process restoration all have designed paths. Saving success is based on durable persistence, not an animation finishing.',
        ),
      ]),
      section(
        'components',
        'Native affordances, Cosmos clarity',
        'Keep learned Android behavior and refine its visual hierarchy.',
        [
          rule(
            'M06',
            'Component profile',
            `Map semantic tokens into one shared Compose theme. Take mobile radii from the platform mapping: ${dpRole('control')} controls, ${dpRole('secondary')} secondary inline, ${dpRole('emphasis')} emphasis, ${dpRole('record')} records and ${dpRole('overlay')} sheets. Standard touch controls are ≥48dp; body text 16sp; supporting 14sp. Prefer native switches, pickers, date/time input and selection semantics over custom gesture-only controls. Haptics are optional and respect system settings.`,
            'All actionable custom drawings expose merged meaningful semantics and explicit actions. Long press and haptics are never the sole means of discovering or confirming a command.',
          ),
          rule(
            'M07',
            'Sheets & reach',
            'Detail sheets retain the visible instrument reference; entry forms may expand to full-height. Keep sheet title, close affordance and action track explicit. Apply IME, display cutout, system bar and gesture insets using current platform APIs. Avoid placing a destructive action in the primary thumb position.',
            'Keyboard opening never covers the focused field or submit. Landscape and largest text keep all actions reachable. TalkBack focus enters and exits layers predictably.',
          ),
        ],
      ),
      section(
        'adaptive',
        'Adapt to the available field',
        'Posture changes should reveal context, not invent another product.',
        [
          rule(
            'M08',
            'Window classes',
            'Use available window size, not device model. Below 600dp use one column; 600–839dp uses a rail or tool/content split when content fits; ≥840dp shows instrument plus context/detail. Short landscape prioritizes reading and compact commands side by side. Keep semantic reading order continuous.',
            'Verify 360×800dp, 600×960dp, 840×900dp and 800×360dp plus resizing. No mandatory pane narrower than its readable minimum; fold hinges never cover controls.',
          ),
          rule(
            'M09',
            'Fold & posture',
            'For a supported tabletop posture, place the instrument above the hinge and controls below; for a book posture place field and details on separate usable panes. Detect posture from the supported window API and provide a single-pane fallback. Do not claim a posture is implemented solely because the layout is drawn here.',
            'Posture changes retain target, selection and safe draft state. Real hinge and emulator checks confirm separation; absent detection falls back to width-based layout.',
          ),
        ],
      ),
      section('sensors', 'Accurate data, humane permissions', 'Show people how much to trust a reading.', [
        rule(
          'M10',
          'Sensor presentation contract',
          'Separate acquisition rate, computation and UI sampling. Select rates from tool accuracy needs; render at most once per display frame and throttle values that need not update at that rate. Show source, freshness and confidence. Visual damping must be bounded and must not change stored measurements or claim precision beyond the source.',
          'Test unavailable sensor, interference, calibration, stale samples, lost GPS and restored source. TalkBack announces on request or meaningful coarse changes, not every sample. Pausing the UI does not falsify an ongoing recording indicator.',
        ),
        rule(
          'M11',
          'Permissions & privacy',
          'Ask for permission at the point of need with the outcome explained. Design granted, approximate/coarse, denied, permanently denied and unavailable states. Background acquisition is limited to explicit user-owned tasks with platform-required indication and a stop control. Store/export precise data only within the product privacy policy.',
          'Permission revocation during use resolves safely. Settings recovery has a clear path back. Analytics and support output contain no coordinates, sensor payloads, private target labels or draft text.',
        ),
      ]),
      section(
        'performance',
        'Native performance contracts',
        'A beautiful reading should not compete with the sensor.',
        [
          rule(
            'M12',
            'Frame & startup acceptance',
            'On a named lowest-supported physical device, meet the active display deadline: 16.67ms at 60Hz, 11.11ms at 90Hz, 8.33ms at 120Hz. Target <5% janky frames in a repeatable 30-second instrument/map journey. Target cold first-useful frame ≤1.5s and warm ≤500ms for offline-capable tools, measured with Macrobenchmark; network content reports a separate readiness metric.',
            'Record device, OS, refresh rate, thermal state, build and traces. No main-thread I/O or per-frame path/particle allocation. The benchmark includes real input and sensor fixtures; a static screenshot does not prove pacing.',
          ),
          rule(
            'M13',
            'Lifecycle & memory',
            'Cache static paths/gradients and compose with stable models. Subscribe to sensor presentation only for the active lifecycle; release hidden map/media resources. Reduce cosmetic work first under battery saver/thermal pressure. Persistent recording follows its explicit service contract and does not depend on a visible composable.',
            'Repeated tool/sheet navigation does not grow retained allocations. Background/foreground and process death preserve only the intended durable state. Red/Reduced/Static do not affect saved data or task availability.',
          ),
        ],
      ),
      section('release', 'Mobile acceptance', 'Verify real instruments on real hands and hardware.', [
        rule(
          'M14',
          'Native release matrix',
          'Review compass, target detail/link, map, sky/moon/orrery, tracking, toolkit, settings, permissions and share flows. Test light/dark/red where supported, compact/expanded/landscape/fold, Arabic RTL, fontScale 2.0, TalkBack, switch access, motion tiers, low power and sensor/permission failure.',
          'Capture screenshots and traces from supported physical devices, plus adaptive emulators. Compile, instrumented navigation/semantics tests and focused Macrobenchmark evidence are required for implementation acceptance.',
        ),
      ]),
    ],
  },
  {
    slug: 'watch-app',
    number: '04',
    label: 'Watch app',
    title: 'One glance.\nOne useful answer.',
    description:
      'A simplified Cosmos for SIMT Wear OS: round-screen instruments, accessible rotary input, concise tools and battery-aware ambient states.',
    intro:
      'Based on wear inside simt-app. Keep the warmth, remove the spectacle. One reading, one qualifier, one action: enough information to act without asking for sustained attention.',
    signature: 'A quiet dial, not a phone squeezed into a circle.',
    specimen: 'watch-app',
    sections: [
      section('glance', 'The single-glance contract', 'Simplicity is a platform requirement.', [
        rule(
          'W01',
          'One answer per screen',
          'Show one primary value/subject, one short qualifier and one primary action or clear route to it. Use no glass blur, decorative stars, ambient drift, dense map, paragraph card or phone-style bottom dock. Retain Void and the same signal colors in interactive mode; corona is the only ambient gradient a watch screen may use, as one static halo on the bearing or target marker, never under text, and a one-accent screen is valid.',
          'The compass communicates direction and confidence immediately. The sky communicates the selected body and next event. The geofence communicates target distance/state. Additional facts require a deliberate tap/scroll.',
        ),
        rule(
          'W02',
          'Round geometry',
          'Design for 192dp and 228dp round viewports plus the minimum supported device. Place critical content inside a safe central area. At vertical offset y from center, available width is 2√(r²−y²) minus at least 12dp each side; avoid generic rectangular edge padding. Bottom commands use the native edge button/scaffold.',
          'No text, hit region or action is clipped by the circular boundary at the largest supported font. If content does not fit, flow it into a native scaling/transforming scroll list.',
        ),
      ]),
      section('type-input', 'Readable and reachable', 'A small display still deserves full-sized targets.', [
        rule(
          'W03',
          'Type & controls',
          'Use 32–40sp primary readings, 18–20sp titles, 14–16sp body/control labels and 12–14sp secondary qualifiers. Keep units visible beside the reading. Touch targets are ≥48dp, with nonoverlapping geometry and at least 4dp separation where feasible. Use native Wear typography and components mapped to Cosmos roles.',
          'Largest system text preserves values, units and actions through reflow/scroll. A short label may have an accessible expanded name; truncation cannot hide the primary reading, qualifier or destructive consequence.',
        ),
        rule(
          'W04',
          'Rotary & assistive input',
          'Use rotary input for native list scrolling and appropriate value adjustment; expose focus/state and a touch alternative. Keep system swipe-back behavior. Long press is optional, never the sole route. TalkBack receives one coherent instrument description with an explicit refresh/read action.',
          'Touch, rotary and TalkBack complete each essential task. Rotary focus is acquired and released with the active screen. Sensor updates do not flood announcements.',
        ),
      ]),
      section('screens', 'A small set of useful tools', 'Each tool has one recognizable visual structure.', [
        rule(
          'W05',
          'Home & targets',
          'Home is a short native list of Compass, Sky, Geofence, Plumb bob, Targets and Settings where supported. Use an icon, literal name and useful last-state qualifier; category color is secondary. Targets is a bounded readable list with selected state and a detail route. Empty targets offers a clear phone-sync or add path supported by the product.',
          'No orbit carousel or icon-only launch grid. The top item is readable immediately on entry and the last item is reachable by touch/rotary. Synced state is honest about connectivity and freshness.',
        ),
        rule(
          'W06',
          'Compass & plumb bob',
          'Compass uses a simplified dial with no more than four cardinal labels, a clear bearing pointer and one numeric value. Confidence appears as text/icon. Plumb bob uses a stable center/level frame and explicit tilt units. Use no extra precision ticks that compete with the reading.',
          'Known heading/tilt fixtures render correctly, including wrap-around. Calibrating/unavailable/stale states replace the value with honest status; a tap can expose calibration or target selection.',
        ),
        rule(
          'W07',
          'Sky & geofence',
          'Sky shows one selected body with next rise/set event and a concise availability state; the full catalog is a separate list. Geofence shows selected target, distance and inside/outside/unknown state. Cyan denotes actual located/live state, not the entire tool theme. Keep the same canonical Cyan; migrate the legacy watch-specific cyan through theme roles.',
          'Indoor/no-fix/offline/stale and revoked permission states cannot resemble a valid lock. The text names the state, and haptic alerts are supplementary, user-controlled and deduplicated.',
        ),
      ]),
      section('ambient', 'Ambient is a different composition', 'When the wrist rests, the interface rests too.', [
        rule(
          'W08',
          'Always-on state',
          'Provide an explicit ambient layout: pure black background, a sparse muted reading, no filled surfaces/gradient/glow, no seconds, no animation and no touch affordances implying interactivity. Mark the reading time if frozen. Honor low-bit and burn-in requirements and platform-recommended repositioning; ambient is not a dimmed interactive screenshot.',
          'Verify the real ambient lifecycle and supported hardware. UI updates follow platform ambient cadence; no UI frame loop runs. Any background sensing/geofence monitoring is separately justified by the user task and power policy.',
        ),
        rule(
          'W09',
          'Wake & confidence',
          'On return to interactive mode, display the last value as stale until a fresh valid sample arrives. Restore target and navigation state; never flash a bright intermediate surface. A watch face complication/tile exposes only supported concise public-safe facts and opens the corresponding task.',
          'Stale becomes fresh only on new evidence. Interrupted connection and missing phone do not discard the current task. A complication never leaks private target text or exact coordinates without the product authorization.',
        ),
      ]),
      section('performance', 'Battery is part of UX', 'Remove work that cannot help the person.', [
        rule(
          'W10',
          'Runtime & energy acceptance',
          'Use no WebView or phone-scale map engine. Cache static dial geometry; stop presentation subscriptions when hidden and never allocate decorative paths per frame. Target <5% janky frames in a named 30-second interactive journey at the device refresh rate. First useful offline reading should appear ≤1s cold on the baseline watch. Use explicit timeouts for acquisition and phone-sync work.',
          'Record physical watch, OS, refresh rate, build, thermal state and trace. Ambient rendering has zero continuous animation callbacks; retained memory does not grow across repeated tool navigation.',
        ),
        rule(
          'W11',
          'Power validation',
          'Measure a repeatable 30-minute interactive/ambient scenario with fixed brightness, connectivity and sensor policy. Record total drain, incremental cost versus a same-build task-disabled baseline and the approved product battery budget before release. Do not publish an invented universal mAh or percentage promise.',
          'A named owner approves the observed budget on physical hardware. Low power disables ornament first; stopping a user-owned monitoring task requires an explicit visible reason rather than silent disappearance.',
        ),
      ]),
      section('release', 'Watch acceptance', 'Check the smallest device, largest text and resting wrist.', [
        rule(
          'W12',
          'Wear release matrix',
          'Verify home, compass, sky list/detail, geofence, plumb bob, targets and settings in interactive, ambient, low-bit/burn-in where supported, offline and no-phone states. Include 192/228dp, largest text, Arabic, touch, rotary, TalkBack, permission/sensor failure and low battery.',
          'Physical-device screenshots, lifecycle traces, focused instrumentation and power measurements are required for implementation acceptance. Round emulator screenshots alone do not certify glanceability, comfort or battery behavior.',
        ),
      ]),
    ],
  },
];

export const faqs = [
  [
    'How is the specification organized?',
    'One shared foundation and four platform parts: web page, web app, mobile app and watch app. Each platform inherits the common requirements and adds its own composition, input and release criteria.',
  ],
  [
    'Are the colors changing?',
    'No anchor color changed. The Deep Space surfaces and Ember, Rose, Violet, Gold, Cyan and Moss anchors are retained. Version 2.1 adds eight gradients and a deterministic star layer built only from those anchors, and version 2.2 makes the semantic profiles the single definition of a surface: light, astronomy red and watch ambient publish their own role values instead of leaving a component to read an anchor and guess.',
  ],
  [
    'Will the stars look the same on every platform?',
    'Yes, where a platform draws them. The star layer is generated from a surface ID and size bucket by a published algorithm with a parity fixture, so web and Android produce the same sky. Watches, maps, sky charts and data never draw one.',
  ],
  [
    'Does v2 describe already shipped products?',
    'Yes. The web product and the mobile product are built on it, and the platform mapping was authored as the target they were brought to. That makes the mapping the source they conform to rather than a description of them, so a product that diverges from a published value is the one out of date. Conformance is still recorded rule by rule: the release record names the build, the rule IDs it carries and the evidence, and accessibility, performance and device acceptance are measured on each implementation. The examples on these pages use illustrative data.',
  ],
  [
    'How should an agent implement a screen?',
    'Load the manifest, common part and relevant platform part. Map applicable rule IDs to a screen brief, use exported tokens, implement the supported data and state contracts, then attach real release evidence.',
  ],
];
