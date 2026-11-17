import { srgbChannels, srgbOklchComponents } from './color.js';

// Canonical values. HTML, CSS, Markdown and machine exports derive from this file.
export const colors = [
  ['void', '#120A14', 'Page and immersive field'],
  ['void-edge', '#1F1126', 'Depth at the edge of the field'],
  ['hull', '#1D1220', 'Standard opaque surface'],
  ['hull-raised', '#2A1830', 'Raised or selected surface'],
  ['hull-highest', '#352040', 'Highest temporary surface'],
  ['ember', '#FF8C42', 'Primary agency: create, continue, commit'],
  ['ember-soft', '#FFB27C', 'Supporting warm emphasis'],
  ['rose', '#FF7E8A', 'Social participation and attention'],
  ['violet', '#C9A0FF', 'Identity, roles and transformation'],
  ['gold', '#FFC766', 'Focus, time, precision and value'],
  ['cyan', '#00E5FF', 'Live, connected and located'],
  ['moss', '#83B692', 'Outdoor and natural categories'],
  ['danger', '#FF6978', 'Destructive action and failure'],
  ['success', '#71D5A4', 'Confirmed completion'],
  ['petrol', '#102C2D', 'Map ground'],
  ['water', '#123D42', 'Map water'],
  ['ink', '#FFF8F2', 'Primary text on Deep Space'],
  ['muted', '#BBAEBB', 'Supporting text'],
  ['faint', '#9A8B9A', 'Secondary metadata; check actual surface'],
  ['dim', '#90839A', 'Inactive ornament; not essential text'],
  ['boundary', '#9A8B9A', 'Essential control outlines on dark surfaces'],
];

export const anchor = Object.fromEntries(colors.map(([name, value]) => [name, value]));
const components = srgbChannels;

// Light is a grammar of eight gradients, each with one job. Stops are
// [anchor, alpha, position]; nothing outside the brand anchors is introduced.
// Geometry follows CSS conventions so one definition drives every platform:
// angles are degrees clockwise from 12 o'clock, positions and radii are
// fractions of the painted box. Compose sweep gradients start at 3 o'clock,
// so native adapters rotate conic geometry by -90 degrees.
export const gradients = [
  {
    name: 'field',
    geometry: { kind: 'radial', at: [0.5, 0], radius: [1.2, 0.9] },
    stops: [
      ['void-edge', 1, 0],
      ['void', 1, 0.72],
    ],
    job: 'Root depth behind every Deep Space screen. Opaque, so text is measured against void-edge and void.',
    profiles: ['deepSpace'],
  },
  {
    name: 'nebula',
    geometry: { kind: 'radial', at: [0.5, 0.5], radius: [0.5, 0.5] },
    stops: [
      ['violet', 0.26, 0],
      ['rose', 0.13, 0.36],
      ['ember', 0.06, 0.64],
      ['ember', 0, 1],
    ],
    job: 'One atmospheric field per screen, behind the focal object or a section entry; never behind reading text.',
    profiles: ['deepSpace'],
  },
  {
    name: 'corona',
    geometry: { kind: 'radial', at: [0.5, 0.5], radius: [0.5, 0.5] },
    stops: [
      ['gold', 0.9, 0],
      ['ember', 0.45, 0.14],
      ['rose', 0.12, 0.42],
      ['rose', 0, 1],
    ],
    job: 'The only glow: the halo of the single focal object, painted in a box four to six times its diameter.',
    profiles: ['deepSpace'],
  },
  {
    name: 'accretion',
    geometry: { kind: 'conic', at: [0.5, 0.5], from: 0 },
    stops: [
      ['ember', 1, 0],
      ['gold', 1, 0.25],
      ['rose', 1, 0.5],
      ['violet', 1, 0.75],
      ['ember', 1, 1],
    ],
    job: 'A 1–2 px ring around the focal object. Never progress, status or a control outline.',
    profiles: ['deepSpace'],
  },
  {
    name: 'horizon',
    geometry: { kind: 'linear', angle: 90 },
    stops: [
      ['ember', 0, 0],
      ['ember', 1, 0.2],
      ['gold', 1, 0.5],
      ['violet', 1, 0.8],
      ['violet', 0, 1],
    ],
    job: 'Light lines of 1–2 px: the top light of a surface, the horizon ribbon, a section signature.',
    profiles: ['deepSpace', 'daylight'],
  },
  {
    name: 'rim',
    geometry: { kind: 'linear', angle: 135 },
    stops: [
      ['ember', 0.55, 0],
      ['ink', 0.1, 0.45],
      ['violet', 0.5, 1],
    ],
    job: 'A 1 px border light for the focal or selected surface, beside its essential boundary rather than replacing it.',
    profiles: ['deepSpace'],
  },
  {
    name: 'action',
    geometry: { kind: 'linear', angle: 180 },
    stops: [
      ['ember-soft', 1, 0],
      ['ember', 1, 0.55],
    ],
    job: 'Fill of the one primary action. The Void label passes on every stop.',
    profiles: ['deepSpace'],
  },
  {
    name: 'spectrum',
    geometry: { kind: 'linear', angle: 90 },
    stops: [
      ['ember', 1, 0],
      ['rose', 1, 0.2],
      ['violet', 1, 0.4],
      ['gold', 1, 0.6],
      ['cyan', 1, 0.8],
      ['moss', 1, 1],
    ],
    job: 'The brand signature line. Category legends use solid labelled swatches instead.',
    profiles: ['deepSpace', 'daylight'],
  },
];

// Two shadows. Depth is dark; the only colored shadow belongs to the primary
// action. Elevation is not light, so both profiles keep both shadows, but the
// values are tuned per profile: a shadow built for a near-black field reads as
// a bruise on a light one, and the ember underglow that sits behind the action
// on void is a glow at all only because void is black. `profiles` holds the
// override for a profile, or null for a profile that does not run the shadow at
// all — red mode and OLED ambient take no colored glow (C27).
export const shadows = [
  {
    name: 'float',
    color: ['void', 0.8],
    offset: [0, 16],
    blur: 40,
    spread: -16,
    job: 'Floating surfaces: popovers, sheets, dialogs and device frames.',
    profiles: { daylight: { color: ['#241A14', 0.16], offset: [0, 12], blur: 32, spread: -14 } },
  },
  {
    name: 'action',
    color: ['ember', 0.5],
    offset: [0, 10],
    blur: 28,
    spread: -12,
    job: 'Underglow of the one primary action at rest. Removed when disabled.',
    profiles: { daylight: { color: ['#D84E00', 0.24] }, astronomy: null, watchAmbient: null },
  },
];

const cssColor = (name, alpha) => {
  // A profile shadow may name a brand anchor or carry a literal hex of its own,
  // because a tinted light-mode shadow is not a lighter Void.
  const hex = anchor[name] || name;
  if (alpha === 1) return hex;
  const [r, g, b] = components(hex).map((v) => Math.round(v * 255));
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};
const pct = (n) => `${Number((n * 100).toFixed(2))}%`;

// A gradient names brand anchors, so a profile that runs one has to resolve
// those anchors too: a horizon drawn in Deep Space Ember on a light surface is
// a garish bar, not a horizon (C27). An anchor that aliases to a role takes the
// profile's value for that role; an anchor with no role keeps its brand value,
// which only ever shows up in a gradient the profile does not run.
function profileAnchor(profile, name) {
  const role = Object.entries(cssRoleAliases).find(([alias]) => alias === name)?.[1];
  return (role && themeProfiles[profile][role]) || anchor[name];
}

export function gradientCSS({ geometry: g, stops }, profile = 'deepSpace') {
  const list = stops
    .map(([name, alpha, position]) => `${cssColor(profileAnchor(profile, name), alpha)} ${pct(position)}`)
    .join(', ');
  if (g.kind === 'radial')
    return `radial-gradient(${pct(g.radius[0])} ${pct(g.radius[1])} at ${pct(g.at[0])} ${pct(g.at[1])}, ${list})`;
  if (g.kind === 'conic') return `conic-gradient(from ${g.from}deg at ${pct(g.at[0])} ${pct(g.at[1])}, ${list})`;
  return `linear-gradient(${g.angle}deg, ${list})`;
}

export function shadowCSS({ color: [name, alpha], offset: [x, y], blur, spread }) {
  return `${x}px ${y}px ${blur}px ${spread}px ${cssColor(name, alpha)}`;
}

// The shadow a profile actually runs: the base definition with that profile's
// override applied, or null when the profile declines it.
export function shadowForProfile(shadow, profile) {
  const override = shadow.profiles?.[profile];
  return override === null ? null : { ...shadow, ...override };
}

function profileShadowCSS(profile) {
  return (name) => {
    const shadow = shadowForProfile(
      shadows.find((s) => s.name === name),
      profile,
    );
    return shadow ? shadowCSS(shadow) : 'none';
  };
}

const dtcgColor = (name, alpha) => parseCssColor(cssColor(name, alpha));
const px = (value) => ({ value, unit: 'px' });

export const tokens = {
  color: Object.fromEntries(
    colors.map(([name, value, description]) => [
      name,
      {
        $type: 'color',
        $value: { colorSpace: 'srgb', components: components(value), alpha: 1 },
        $description: description,
        $extensions: { 'app.simt.cosmos': { hex: value } },
      },
    ]),
  ),
  gradient: Object.fromEntries(
    gradients.map((g) => [
      g.name,
      {
        $type: 'gradient',
        $value: g.stops.map(([name, alpha, position]) => ({ color: dtcgColor(name, alpha), position })),
        $description: g.job,
        $extensions: {
          'app.simt.cosmos': { geometry: g.geometry, anchors: g.stops.map(([name]) => name), profiles: g.profiles },
        },
      },
    ]),
  ),
  shadow: Object.fromEntries(
    shadows.map((s) => [
      s.name,
      {
        $type: 'shadow',
        $value: {
          color: dtcgColor(...s.color),
          offsetX: px(s.offset[0]),
          offsetY: px(s.offset[1]),
          blur: px(s.blur),
          spread: px(s.spread),
        },
        $description: s.job,
      },
    ]),
  ),
  // The description of a scale belongs on the group: repeating one sentence per
  // token published thirteen copies of a fact the reader has already been given, in
  // every one of the five documents that carry this tree.
  space: {
    $description:
      'Shared rhythm: 1 unit is 1 CSS px on web and 1 dp natively, so a client uses these numbers unchanged and never publishes its own table.',
    ...Object.fromEntries(
      [4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128].map((v) => [
        String(v),
        { $type: 'dimension', $value: { value: v, unit: 'px' } },
      ]),
    ),
  },
  radius: {
    $description:
      'Web anchors. Android and Wear map the same roles to native geometry in platformAdapters (C09); the anchors are not copied into a native scale.',
    ...Object.fromEntries(
      [
        ['control', 8],
        ['record', 16],
        ['overlay', 24],
      ].map(([name, value]) => [name, { $type: 'dimension', $value: { value, unit: 'px' } }]),
    ),
  },
  motion: {
    $description:
      'Durations are shared: 150 ms feedback, 300 ms local state, 500 ms context change. Each platform part adds its own easing set in platformAdapters (C30); easing here is the cross-platform standard.',
    feedback: { $type: 'duration', $value: { value: 150, unit: 'ms' } },
    state: { $type: 'duration', $value: { value: 300, unit: 'ms' } },
    context: { $type: 'duration', $value: { value: 500, unit: 'ms' } },
    easing: { $type: 'cubicBezier', $value: [0.2, 0, 0.2, 1] },
  },
  font: {
    display: { $type: 'fontFamily', $value: ['Space Grotesk', 'system-ui', 'sans-serif'] },
    body: { $type: 'fontFamily', $value: ['Source Sans 3', 'system-ui', 'sans-serif'] },
    arabic: { $type: 'fontFamily', $value: ['El Messiri', 'sans-serif'] },
  },
  layer: {
    $description: 'Relative semantic stacking tier; use platform layer primitives for modal isolation.',
    ...Object.fromEntries(
      [
        ['field', 0],
        ['chrome', 10],
        ['context', 20],
        ['scrim', 30],
        ['modal', 40],
        ['feedback', 50],
        ['skip', 100],
      ].map(([name, value]) => [name, { $type: 'number', $value: value }]),
    ),
  },
};

// Per-part geometry and easing mapping (C28, C30). `tokens.space`, `tokens.radius`
// and `tokens.motion` carry the shared rhythm, the web anchors and the shared
// durations; the parts below carry the geometry and the curves each part is built
// to, so a client adopts the published set instead of inventing a dp value or a
// bezier.
//
// This mapping is authored here. It is not read out of a product. A corner scale
// follows from the touch-target size and the density of the part: 8 / 10 / 14 / 20
// / 28 dp on a handheld screen steps from a 48 dp target to a full-bleed overlay in
// five increments where the web needs three at 8 / 16 / 24 px, and a round screen
// has no corner radius to publish at all, so Wear carries the native round and
// capsule shape instead of a number. Curves follow the same reasoning: an arrival
// and a departure have to be opposites, so each part publishes a closed set of
// three or four curves rather than one curve reused everywhere. A product that
// cannot adopt a value yet is out of date with this mapping, and no product is
// ever its source.
// DTCG has exactly two dimension units, px and rem, so a dp radius cannot claim to
// be one. The number is the token and the unit rides in $extensions, which is where
// a consumer's private vocabulary belongs: nothing is faked into a type the format
// does not have, and a client reading the file with a standard tool still gets a
// number it can emit.
const dp = (value) => ({ $type: 'number', $value: value, $extensions: { 'app.simt.cosmos': { unit: 'dp' } } });
const duration = (value) => ({ $type: 'duration', $value: { value, unit: 'ms' } });
const easing = (...values) => ({ $type: 'cubicBezier', $value: values });

// Only the tokens: the group carries its own $description, which is not a radius.
const webRadius = Object.fromEntries(
  Object.entries(tokens.radius)
    .filter(([role]) => !role.startsWith('$'))
    .map(([role, token]) => [role, { $type: 'dimension', $value: { ...token.$value } }]),
);
const webEasing = {
  standard: easing(...tokens.motion.easing.$value),
  emphasis: easing(0.22, 1, 0.36, 1),
  entrance: easing(0.16, 0.84, 0.44, 1),
  exit: easing(0.4, 0, 0.7, 0.2),
};
// Read from the token group rather than typed beside it: the group is what the
// brief reads and what tokens.json publishes, and the curves one line above are
// already derived this way. Typed, the two owners drift -- change the token and
// the agent brief prints one duration while C17, motion.json and every part
// payload publish another, with nothing failing.
const webDuration = Object.fromEntries(
  Object.entries(tokens.motion)
    .filter(([, token]) => token.$type === 'duration')
    .map(([role, token]) => [role, duration(token.$value.value)]),
);
const nativeEasing = {
  standard: easing(...tokens.motion.easing.$value),
  enter: easing(0.05, 0.7, 0.1, 1),
  exit: easing(0.3, 0, 0.8, 0.15),
  transform: easing(0.2, 0, 0, 1),
};
// The shared three durations are the shared three durations: a native part runs
// the same feedback, state and context timings, so C17 reads one table rather than
// one per platform. (A native screen that hands a context change to the platform's
// own transition still has a budget to meet, and the budget is this one.)
const nativeDuration = { ...webDuration };

// What each part actually runs. These four answers are read off the rules rather
// than chosen for a payload's convenience, so the per-part files can be filtered
// without the filtering becoming a second, quieter specification:
//
//   profiles  — C06 offers Daylight "where supported", reserves astronomy red for
//               instruments and gives watch ambient to the OLED wrist, and M14 tests
//               light/dark/red on a handheld. So the wrist runs Deep Space and
//               watch ambient, a handset adds astronomy, and no browser runs red.
//   gradients — the narrowing this part actually makes, which has a floor and a
//               ceiling. The floor is C12's action gradient on the one primary
//               action and C10's rim on the focal surface: both are common rules,
//               every part inherits them, and a payload that filtered them out
//               could not satisfy the rule it inherits from the only document it
//               is told to read. The ceiling is the atmosphere: a part draws the
//               glows its surfaces have room for, and W01 leaves the wrist with
//               one static corona. Every part therefore carries action and rim,
//               and the lists below differ only in what surrounds them.
//   stars     — C11 says watch screens own no stars, and the table gives every
//               watch surface a "None" star layer.
//
// A part that starts using something new adds it here first, the way C27 and C11
// require it to be added to the rule and the table before it reaches a payload.
// checkPartScopeRules() holds the floor: it reads the rules each part inherits
// and fails when one of them names a gradient the payload drops.
export const partScope = {
  webPage: {
    profiles: ['deepSpace', 'daylight'],
    stars: true,
    gradients: ['field', 'nebula', 'corona', 'accretion', 'horizon', 'rim', 'action', 'spectrum'],
  },
  webApp: {
    profiles: ['deepSpace', 'daylight'],
    stars: true,
    gradients: ['field', 'corona', 'horizon', 'rim', 'action'],
  },
  android: {
    profiles: ['deepSpace', 'daylight', 'astronomy'],
    stars: true,
    gradients: ['field', 'nebula', 'corona', 'accretion', 'horizon', 'rim', 'action'],
  },
  watch: { profiles: ['deepSpace', 'watchAmbient'], stars: false, gradients: ['corona', 'rim', 'action'] },
};

// The notation a part authors in (C29). Web publishes the projection because a
// browser can; Android and Wear read sRGB because neither toolkit has an Oklab
// colour space, so a projected value would be a token they cannot emit. The
// declaration is metadata rather than a token, which is why it rides in
// $extensions instead of standing in the colour group as a bare string.
export const partColorSpace = { webPage: 'oklch', webApp: 'oklch', android: 'srgb', watch: 'srgb' };

// The projection, published once as real colour tokens rather than as a string per
// adapter. It was two copies of the same block — one per web part — each projecting
// four profiles, two of which a browser never selects. One group, filtered to the
// profiles a part actually runs, is both smaller and honest about why the others
// are absent.
// A function, not a value: the semantic roles come from themeColorTokens(), which
// reads themeProfiles further down this file, so it cannot run at module init.
export function colourProjection(profiles) {
  const project = (token) => ({
    $type: 'color',
    $value: {
      colorSpace: 'oklch',
      components: srgbOklchComponents(token.$value.components, token.$value.alpha),
      alpha: token.$value.alpha,
    },
    $extensions: { 'app.simt.cosmos': { ...token.$extensions['app.simt.cosmos'] } },
  });
  const roleGroups = themeColorTokens();
  return {
    $description:
      'oklch projection of the anchors and of the semantic roles of the profiles this part runs, for authoring and theming. Derived from the sRGB value beside it, which stays the truth contrast is measured in (C29).',
    anchors: Object.fromEntries(
      colors.map(([name, value]) => [
        name,
        project({ $value: parseCssColor(value), $extensions: { 'app.simt.cosmos': { hex: value } } }),
      ]),
    ),
    roles: Object.fromEntries(
      Object.entries(roleGroups)
        .filter(([profile]) => !profile.startsWith('$') && profiles.includes(profile))
        .map(([profile, roles]) => [
          profile,
          Object.fromEntries(
            Object.entries(roles)
              .filter(([role]) => !role.startsWith('$'))
              .map(([role, token]) => [role, project(token)]),
          ),
        ]),
    ),
  };
}

export const platformAdapters = {
  $description:
    'Radius, duration and easing per platform part, with the notation each part authors in and the geometry a native shape replaces. Spacing needs no mapping: 1 unit is 1 CSS px on web and 1 dp natively (C09).',
  webPage: {
    $description:
      'Long-form marketing and documentation pages. Shares the web radius anchors and the web easing set; a page needs a mapping of its own only if it introduces a role the anchors do not cover.',
    $extensions: { 'app.simt.cosmos': { colorSpace: partColorSpace.webPage } },
    radius: webRadius,
    duration: webDuration,
    easing: webEasing,
  },
  webApp: {
    $description:
      'Signed-in web application. Same web profile today; a divergence is recorded here rather than by editing the shared anchors.',
    $extensions: { 'app.simt.cosmos': { colorSpace: partColorSpace.webApp } },
    radius: webRadius,
    duration: webDuration,
    easing: webEasing,
  },
  android: {
    $description:
      "Compose applications. Radius is the part's own scale — control 8, secondary 10, emphasis 14, record 20, overlay 28 dp — because a handheld screen steps from a 48 dp target to a full-bleed overlay in five increments where a pointer surface needs three. Compose has no Oklab colour space, so this part reads the anchors and the profile roles in sRGB.",
    $extensions: { 'app.simt.cosmos': { colorSpace: partColorSpace.android, shapes: { circle: 'round' } } },
    radius: { control: dp(8), secondary: dp(10), emphasis: dp(14), record: dp(20), overlay: dp(28) },
    duration: nativeDuration,
    easing: nativeEasing,
  },
  watch: {
    $description:
      'Wear OS round screens. There is no corner to measure on a round screen, so this part publishes shapes rather than a radius: control and record are round and an overlay is a capsule. The easing and duration sets are the Android ones by decision — the same scale at a much smaller physical size.',
    $extensions: {
      'app.simt.cosmos': {
        colorSpace: partColorSpace.watch,
        radius: { control: 'round', record: 'round', overlay: 'capsule' },
      },
    },
    duration: nativeDuration,
    easing: nativeEasing,
  },
};

// Palettes already present in SIMT. These are role adapters, not replacements for the brand anchors.
// accentText, faint and line are the three roles that had no mapping and forced
// the pages to reach for a brand anchor as if it were a semantic role. accentText
// exists because one accent cannot be both a fill and small text on a light
// background: Daylight's Ember is 4.6:1 against its own fill with the Void
// label, but only 3.9:1 as text on Daylight, so the text case needs a darker
// step (C05, C06).
export const adapters = {
  daylight: {
    background: '#FFF6EC',
    surface: '#FFF9F2',
    raised: '#F6E7DC',
    highest: '#F3E0F7',
    text: '#241A14',
    muted: '#52443C',
    faint: '#6B5F58',
    line: 'rgb(36 26 20 / 0.14)',
    action: '#D84E00',
    accentText: '#B04300',
    onAction: '#120A14',
    social: '#B83266',
    identity: '#7C4DBD',
    precision: '#8B5E00',
    live: '#006974',
    outdoors: '#356447',
    danger: '#9B2432',
    success: '#276442',
    focus: '#8B5E00',
    boundary: '#52443C',
  },
  astronomy: {
    background: '#0A0000',
    surface: '#1A0000',
    raised: '#2A0000',
    highest: '#330000',
    text: '#E88989',
    muted: '#D47575',
    faint: '#C06A6A',
    line: 'rgb(232 137 137 / 0.16)',
    action: '#8B0000',
    accentText: '#E88989',
    onAction: '#FFB3B3',
    social: '#E88989',
    identity: '#E88989',
    precision: '#E88989',
    live: '#E88989',
    outdoors: '#E88989',
    danger: '#FFB3B3',
    success: '#E88989',
    focus: '#E88989',
    boundary: '#D47575',
  },
  watchAmbient: {
    background: '#000000',
    surface: '#000000',
    text: '#BBAEBB',
    faint: '#9A8B9A',
    line: 'rgb(187 174 187 / 0.16)',
    bearing: '#FFC766',
  },
};

// CSS and JSON expose the same semantic profile names without changing brand anchors.
export const themeProfiles = {
  deepSpace: {
    background: anchor.void,
    surface: anchor.hull,
    raised: anchor['hull-raised'],
    highest: anchor['hull-highest'],
    text: anchor.ink,
    muted: anchor.muted,
    faint: anchor.faint,
    line: 'rgb(255 236 226 / 0.12)',
    action: anchor.ember,
    accentText: anchor['ember-soft'],
    onAction: anchor.void,
    social: anchor.rose,
    identity: anchor.violet,
    precision: anchor.gold,
    live: anchor.cyan,
    outdoors: anchor.moss,
    danger: anchor.danger,
    success: anchor.success,
    focus: anchor.gold,
    boundary: anchor.boundary,
  },
  ...adapters,
};

// The vocabulary the Cosmos stylesheets are written against, mapped onto profile
// roles. The pages predate the role names and say `var(--ink)`, so this is the
// layer that lets one stylesheet serve four profiles: a rule states the role it
// means and the active profile decides the value. Gradients and shadows are not
// in this table because they are per-profile instruments rather than roles —
// see themeCSS, where a profile that does not run one resolves it to `none`.
export const cssRoleAliases = {
  void: 'background',
  hull: 'surface',
  'hull-raised': 'raised',
  'hull-highest': 'highest',
  ink: 'text',
  muted: 'muted',
  faint: 'faint',
  boundary: 'boundary',
  ember: 'action',
  rose: 'social',
  violet: 'identity',
  gold: 'precision',
  cyan: 'live',
  moss: 'outdoors',
  danger: 'danger',
  success: 'success',
  'accent-text': 'accentText',
  'on-action': 'onAction',
  line: 'line',
};

// The published semantic roles, which are what tokens.json documents and what
// an agent implements against. Every role any profile defines, so an absent role
// is cleared explicitly rather than inherited from the parent profile.
const profileRoles = [...new Set(Object.values(themeProfiles).flatMap((profile) => Object.keys(profile)))];

// The one answer to "which CSS variable carries this name".
//
// Three namespaces publish under different conventions, which is why a drawing
// cannot simply write `var(--action)`: the semantic roles are published with the
// cosmos prefix, the layers under `layer-`, and the anchors and the aliases
// unprefixed. `var(--action)` therefore names a variable that does not exist, the
// browser drops the declaration, and an SVG fill falls back to its initial value,
// which is black. That is not a subtle mistake: it rendered every primary action,
// every chip and every focus ring on the gallery as a black rectangle, while the
// colours that happened to be aliases (danger, ember, ink, line, muted) painted
// correctly and hid the fault.
//
// Names are resolved here from the same tables the stylesheet is generated from,
// so the answer cannot drift from what is published, and an unknown name returns
// null so the caller fails while building rather than painting black.
const LAYER_NAMES = new Set(Object.keys(tokens.layer).filter((key) => !key.startsWith('$')));
const ANCHOR_NAMES = new Set(colors.map(([name]) => name));
export const kebab = (role) => role.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`);

/**
 * The published CSS variable for a token name painted into a property, or a build
 * failure.
 *
 * Callers that paint a shape use this rather than cssVariable, because painting a
 * name nothing publishes produces a shape that looks deliberately black rather
 * than one that is obviously missing.
 *
 * The property is required because the same name can be a colour in one namespace
 * and not in another. The layer tokens publish a stacking tier — `scrim` is 30 —
 * so `stroke: var(--layer-scrim)` resolves to `stroke: 30`, the declaration is
 * dropped, and a shape carrying `fill="none"` disappears entirely rather than
 * looking wrong. A shape cannot be checked for that without knowing which
 * property it is being read by, so the caller has to say.
 */
export function paintVariable(name, property) {
  if (!property)
    throw new Error(
      `paintVariable("${name}") was called without the property it paints; a token cannot be checked against the wrong namespace without it.`,
    );
  const published = cssVariable(name);
  if (published === null)
    throw new Error(`Nothing publishes a token named "${name}", so ${property} cannot be painted with it.`);
  if (published.startsWith('--layer-'))
    throw new Error(
      `"${name}" is a layer, not a colour: it publishes ${published}, a stacking tier. Painting it as ${property} produces a declaration the browser drops. Paint a role or an anchor instead, and name the layer in the component that declares it.`,
    );
  return published;
}

/** The published CSS variable for a token name, or null when nothing publishes it. */
// Cached because a drawing asks per paint and a page has hundreds of var()
// references: the answer depends only on the name, and the four namespaces it
// consults do not change within a build. The map holds the misses too, so a
// role that resolves to null is not looked up on every reference either.
const cssVariableCache = new Map();

function cssVariable(name) {
  if (cssVariableCache.has(name)) return cssVariableCache.get(name);
  let resolved = null;
  if (LAYER_NAMES.has(name)) resolved = `--layer-${kebab(name)}`;
  else if (name in cssRoleAliases) resolved = `--${name}`;
  else if (profileRoles.includes(name)) resolved = `${cssRolePrefix}${kebab(name)}`;
  else if (ANCHOR_NAMES.has(name)) resolved = `--${name}`;
  cssVariableCache.set(name, resolved);
  return resolved;
}

// Roles whose value is a hairline rather than an opaque color, plus the color
// scheme each profile declares. A profile with no interactive roles (watch
// ambient) still resolves its text ramp and its line.
const cssColorScheme = { deepSpace: 'dark', daylight: 'light', astronomy: 'dark', watchAmbient: 'dark' };

// The roles a control or a state is drawn from. A profile publishing none of
// them is a display profile: it resolves a text ramp and a line and nothing a
// control could take its colour from. Which profiles those are has to be named
// -- the set of interactive roles is not itself published, so deriving it would
// be a second list to keep in step -- and the two are held together by the gate,
// so a named profile that starts publishing one of these fails, and so does an
// unnamed profile that stops.
const INTERACTIVE_ROLES = ['action', 'accentText', 'focus', 'danger', 'success'];
export const DISPLAY_ONLY_PROFILES = ['watchAmbient'];
const isDisplayOnly = (name) => DISPLAY_ONLY_PROFILES.includes(name);

// Accepts #RRGGBB and both rgb() spellings. Roles carry CSS text so the same
// table drives tokens.css and tokens.json, and a role may legitimately be a
// translucent hairline, which no hex can express.
export function parseCssColor(value) {
  if (value.startsWith('#')) {
    return { components: components(value), alpha: 1, hex: value.toUpperCase() };
  }
  // Accepts both spellings of the function form: the space/slash one a design
  // token uses and the comma form `cssColor` emits, which carries the alpha as
  // a fourth argument rather than after a slash.
  const body = value.replace(/^rgba?\(/, '').replace(')', '');
  const [rgb, slashed] = body.split('/');
  const parts = rgb
    .trim()
    .split(/\s*,\s*|\s+/)
    .filter(Boolean);
  const alpha = slashed === undefined ? (parts[3] ?? 1) : Number(slashed);
  const [r, g, b] = parts.slice(0, 3).map((v) => Number(v) / 255);
  return { components: [r, g, b], alpha: Number(alpha), hex: null };
}

export function themeColorTokens() {
  return Object.fromEntries(
    Object.entries(themeProfiles).map(([name, roles]) => [
      name,
      {
        // The note belongs to the profile it describes. Typed into the template it
        // was published thirteen times, and three of those were about a profile the
        // reader was not reading.
        $description: `${name} semantic colors.${isDisplayOnly(name) ? ' A display profile publishes none of the interactive roles, so a control on this theme takes its colour from the profile it is not drawn in.' : ''}`,
        ...Object.fromEntries(
          Object.entries(roles).map(([role, value]) => {
            const parsed = parseCssColor(value);
            // A translucent role has no hex to publish, so the CSS text stands in and
            // the alpha travels with it rather than being lost.
            const published = parsed.hex ? { hex: parsed.hex } : { css: value };
            return [
              role,
              {
                $type: 'color',
                $value: { colorSpace: 'srgb', components: parsed.components, alpha: parsed.alpha },
                // No per-role prose: the key names the role and the profile's own
                // $description names the profile, so a sentence repeating both would be
                // eighty-four copies of a fact the reader already has.
                $extensions: { 'app.simt.cosmos': published },
              },
            ];
          }),
        ),
      },
    ]),
  );
}

// The two hooks that connect a theme to an implementation: the attribute that
// selects a profile and the prefix its semantic roles are published under.
// Published in tokens.json extensions and documented in the design skill, so
// they are named once here.
export const cssThemeAttribute = 'data-cosmos-theme';
export const cssRolePrefix = '--cosmos-';
// Daylight is the profile the system preference selects, so it is the one that
// also needs a media query. `deepSpace` is the signature and the default when
// the system asks for nothing.
export const lightProfile = 'daylight';
// The profile a reader gets when nothing is stored and the system asks for
// nothing. Named beside the light profile because it is published the same way:
// the manifest and tokens.json both tell a reader which one they get by default,
// and a second literal in a generator is a second answer.
export const defaultProfile = 'deepSpace';

// One block per profile: the stylesheet vocabulary first, then the published
// semantic roles, then the light and shadow instruments. A gradient the profile
// does not run resolves to `none` rather than being left unset, so an element
// painted with it falls back to its own flat role color instead of inheriting
// another profile's light (C27).
function profileBlock(name, selector) {
  const roles = themeProfiles[name];
  const variable = (role) => `${cssRolePrefix}${kebab(role)}`;
  const shadow = profileShadowCSS(name);
  const declarations = [
    ...Object.entries(cssRoleAliases).map(([alias, role]) => `  --${alias}: ${roles[role] || 'initial'};`),
    ...profileRoles.map((role) => `  ${variable(role)}: ${roles[role] || 'initial'};`),
    ...gradients.map((g) => `  --gradient-${g.name}: ${g.profiles.includes(name) ? gradientCSS(g, name) : 'none'};`),
    ...shadows.map((s) => `  --shadow-${s.name}: ${shadow(s.name)};`),
    `  --atmosphere-sky: ${atmosphere[name]};`,
    // The OLED watch specimen is a picture of a black panel rather than an
    // instance of the active profile, so it is the one surface whose background
    // does not follow the page. It is published here, from the profile that owns
    // a black screen, rather than typed into the stylesheet: a hand-copied hex is
    // the one colour in the file that no token owns, and it drifts silently when
    // the Watch Ambient background changes.
    `  --watch-ambient-background: ${themeProfiles.watchAmbient.background};`,
  ];
  return `${selector} {\n  color-scheme: ${cssColorScheme[name]};\n${declarations.join('\n')}\n}`;
}

// Every selector that resolves a profile, in cascade order. Published so
// verification can read a block by name instead of re-deriving the selectors and
// string-splitting the generated file.
// Which profiles draw the star layer. It is not a color and not a gradient — it
// is a generated fixture with its own parity contract — so it is published as a
// display keyword rather than forced into the color vocabulary. The atmosphere
// table in the specification is the source: Daylight's background is flat and
// carries no stars, and red mode and watch ambient have no light at all.
export const atmosphere = { deepSpace: 'block', daylight: 'none', astronomy: 'none', watchAmbient: 'none' };

// Reference plates and illustrations that must show Deep Space whatever the
// visitor has selected.
//
// - `.light-sample` paints all eight gradients, six of which a light profile
//   resolves to `none`, so without the pin six of its eight chips render as
//   empty boxes. The pin is on the chip and not on the section: a chip is a
//   picture of a gradient, and freezing the plate it sits on would freeze the
//   heading, the caption and the surface the reader is reading them on.
// - `.ambient-face` no longer appears here, and that is the point. The OLED watch
//   specimen is a picture *of* a black panel rather than an instance of the active
//   one, so its plate takes the Watch Ambient background while everything drawn on
//   it follows the theme like every other specimen. Pinning the face froze all
//   three of its strings as well, so on a Daylight page `Compass`, `042°` and
//   `Last reading 09:41` stayed on Deep Space values above a plate every other
//   specimen had already recoloured. The plate is its own element now, and it
//   paints from a published profile value rather than a hex typed beside it.
//
// The orbit art is deliberately *not* pinned. It draws the active profile's
// anchors, so it recolours with the page; pinning it held its labels at Deep
// Space muted and measured 1.99:1 on the Daylight background.
//
// This belongs in the token layer rather than the stylesheet because the values
// are generated; a hand-copied copy would drift from what it claims to show.
export const deepSpacePins = ['.light-sample'];

export function themeSelectors() {
  const attribute = (name) => `[${cssThemeAttribute}="${name}"]`;
  return Object.keys(themeProfiles).flatMap((name) => [
    ...(name === lightProfile
      ? // Follow the system only while the visitor has expressed no preference.
        // `:not([data-cosmos-theme])` and `[data-cosmos-theme="daylight"]` cannot
        // both match, so the explicit attribute wins wherever it is set (C06).
        [{ profile: name, media: '(prefers-color-scheme: light)', selector: `:root:not([${cssThemeAttribute}])` }]
      : []),
    {
      profile: name,
      media: null,
      selector:
        name === 'deepSpace'
          ? `:root, ${attribute(name)}${deepSpacePins.map((pin) => `, ${pin}`).join('')}`
          : attribute(name),
    },
  ]);
}

export function themeCSS() {
  return (
    themeSelectors()
      .map(({ profile, media, selector }) => {
        const block = profileBlock(profile, selector);
        return media ? `@media ${media} {\n${block.replace(/^/gm, '  ')}\n}` : block;
      })
      .join('\n\n') + '\n'
  );
}

export const metrics = {
  targets: { web: { value: 44, unit: 'css-px' }, android: { value: 48, unit: 'dp' }, wear: { value: 48, unit: 'dp' } },
  webVitals: { lcpMs: 2500, inpMs: 200, cls: 0.1, percentile: 75 },
  native: { frameDeadline60HzMs: 16.67, frameDeadline90HzMs: 11.11, frameDeadline120HzMs: 8.33, allowedJankPercent: 5 },
};
