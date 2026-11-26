import { platformAdapters, tokens, themeProfiles } from './tokens.js';
import { motionMotifs } from './motion.js';

// Component registry.
//
// C12 through C16 already say what an action, a field, a record, a layer and a
// feedback surface must do, and the shared state matrix already says what each
// state looks like. What neither could answer was the question an agent building
// a screen actually starts with: given this design, which of those rules apply to
// the thing in front of me, which theme roles and which corner role does it
// consume, and which part of it is allowed to move.
//
// So a registry entry is an index, not a second copy. It names rule IDs, theme
// roles, token names and motif IDs, and it deliberately carries no prose: a
// treatment restated here would be a second answer that could disagree with the
// rule it claims to summarise, and the whole reason the rules are published as
// rules is that they are the only place a treatment is written. A component that
// needs a treatment nobody has written is missing a rule, and the registry says
// so rather than inventing one.
//
// The state vocabulary is shared. C12 names the eight interaction states a
// control must expose; C16 names the states a surface reaches when its data is
// stale or it is offline. A component lists the states it can actually be in,
// because forcing all eleven onto a sheet that cannot be "selected" would be
// padding the matrix with states the component has no answer for.

/** The theme role names every profile publishes, so an entry cannot name one that does not exist. */
export const THEME_ROLES = Object.keys(themeProfiles.deepSpace);

/**
 * The z-order roles a component may claim, read from the shared layer group. A
 * component that names a theme role for its stacking would be mixing two
 * vocabularies: `scrim` and `chrome` are layers, not colours, and the two
 * resolve from different token groups.
 */
export const COMPONENT_LAYERS = Object.keys(tokens.layer).filter((key) => !key.startsWith('$'));

/**
 * Corner roles a component may claim, read from the mapping rather than typed.
 * The web parts publish control, record and overlay; Android adds secondary and
 * emphasis; a circle is a shape rather than a corner, so it comes from the
 * adapter's shape declaration the same way the spec's geometry table reads it.
 */
export const COMPONENT_RADII = [
  ...new Set([
    ...Object.keys(tokens.radius).filter((key) => !key.startsWith('$')),
    ...Object.keys(platformAdapters.android.radius ?? {}).filter((key) => !key.startsWith('$')),
    ...Object.keys(platformAdapters.android.$extensions?.['app.simt.cosmos']?.shapes ?? {}),
    ...Object.keys(platformAdapters.watch.$extensions?.['app.simt.cosmos']?.radius ?? {}),
  ]),
];

// The shared state vocabulary, each state pointing at the rule that governs it.
// A state with no rule is a state nobody has specified, so the registry refuses
// to invent one: the guard checks that each rule named here exists and mentions
// the state.
export const COMPONENT_STATES = {
  default: { rules: ['C12'] },
  hover: { rules: ['C12'] },
  pressed: { rules: ['C12'] },
  focus: { rules: ['C12', 'C19'] },
  pending: { rules: ['C12'] },
  disabled: { rules: ['C12'] },
  error: { rules: ['C13', 'C16'] },
  confirmed: { rules: ['C12'] },
  selected: { rules: ['C12', 'C14'] },
  stale: { rules: ['C16'] },
  offline: { rules: ['C16'] },
};

const ALL_WEB = ['webPage', 'webApp'];
const WEB_AND_MOBILE = [...ALL_WEB, 'android'];

/**
 * The gallery families, and the rules that own each one.
 *
 * A gallery route per family only works if membership is derived rather than
 * typed, or the same component would be listed twice and the routes would drift
 * apart. Membership is therefore the rule a component cites: the family is the
 * first family whose `rules` the entry actually uses, so a component is filed
 * under the contract that governs it and cannot be filed twice.
 *
 * `order` is the reading order of the gallery index, which is why it is
 * published rather than implied by key order.
 */
export const COMPONENT_FAMILIES = [
  { id: 'actions', label: 'Actions and selection', rules: ['C12', 'W03'], order: 1 },
  { id: 'inputs', label: 'Inputs and validation', rules: ['C13'], order: 2 },
  { id: 'records', label: 'Records and navigation', rules: ['C14'], order: 3 },
  { id: 'layers', label: 'Layers and dialogs', rules: ['C15', 'C26'], order: 4 },
  { id: 'feedback', label: 'Feedback at the source', rules: ['C16'], order: 5 },
  { id: 'instruments', label: 'Instruments and readings', rules: ['C08', 'C18', 'M03', 'M10'], order: 6 },
];

/** The family a component belongs to, or null when no family rule governs it. */
// No allocation. This runs once per component per family it is asked about, and
// the set it used to build was thrown away immediately. The families are still
// consulted in order, so a component citing two families still resolves to the
// first, which is what the slice grouping reads as.
export function familyFor(component) {
  return COMPONENT_FAMILIES.find((family) => family.rules.some((rule) => component.rules.includes(rule))) ?? null;
}

/** The components in one family, in registry order. */
export function componentsForFamily(family) {
  return componentRegistry.filter((component) => familyFor(component)?.id === family);
}

/**
 * The registry. Every entry is a reference: rule IDs, theme roles, corner and
 * duration roles, motif IDs and the native counterpart's name.
 *
 * `parts` is the slice boundary — an agent building one platform reads only that
 * platform's slice — and the guard holds it to the platform of the rules the
 * entry cites, so a component cannot claim a part whose rules it does not use.
 * `native` names the existing counterpart in each product rather than describing
 * it: a port starts from what already exists, and a registry that re-specified
 * the counterpart would be a second implementation.
 */
/**
 * Why a component may not have a state. A closed vocabulary, so a reason is a
 * name a reader can look up rather than a sentence that drifts, and so the gate
 * can refuse a reason nobody defined.
 *
 * Each reason is about one state. That is deliberate: a reason that could
 * explain any state would explain nothing, so `notSelectable` is a claim about
 * selection and never about hover. A component that declares none of these for
 * a state is claiming that state exists.
 */
export const NOT_APPLICABLE = {
  notInteractive: {
    why: 'The component has no hover or press affordance of its own; whatever the pointer is really addressing is the control inside or behind it.',
  },
  notFocusable: {
    why: 'Nothing of it takes focus. What takes focus is the control it hosts or the surface it reports into.',
  },
  notAsync: {
    why: 'The component is already on screen with its final reading, so there is no state between the request and the answer.',
  },
  alwaysAvailable: {
    why: 'There is nothing to disable. It carries no action to withhold, so a disabled variant would be a decoration.',
  },
  noFailureMode: {
    why: 'It cannot fail on its own. Anything that can goes through the banner, the toast or the dialog that owns the failure.',
  },
  confirmedElsewhere: {
    why: 'Confirmation is a separate dialog, so the component itself has no settled confirmed state to draw.',
  },
  notSelectable: {
    why: 'There is no selected and unselected condition. Choosing it is an action or a dialog, not a mode it holds.',
  },
  notFetched: {
    why: 'It shows no remote data, so it cannot be out of date. Whatever is stale about it is stale about a record or a list.',
  },
  notNetworkBound: {
    why: 'It does not depend on a connection, so it cannot be offline. What can be offline is the screen carrying it.',
  },
};

/**
 * The states each component opts out of, by id.
 *
 * A map beside the registry rather than a field on each entry, because the
 * guard has to tell three cases apart: a component that declares every state, a
 * component that opts out of some with a stated reason, and a key that is simply
 * misspelled. An inline field collapses the last two — a typo would read as a
 * deliberate opt-out and the coverage gap would reopen silently, which is exactly
 * what Phase 2 recorded happening. Here a missing key means "opts out of
 * nothing", and the guard holds every entry's states and reasons to the full
 * vocabulary between them.
 */
export const componentApplicability = {
  primaryAction: { selected: 'notSelectable', stale: 'notFetched', offline: 'notNetworkBound' },
  secondaryAction: {
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  destructiveAction: { selected: 'notSelectable', stale: 'notFetched', offline: 'notNetworkBound' },
  clearableField: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
    offline: 'notNetworkBound',
  },
  chipSelection: {
    pending: 'notAsync',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  recordRow: {
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    offline: 'notNetworkBound',
  },
  sheet: {
    hover: 'notInteractive',
    focus: 'notFocusable',
    disabled: 'alwaysAvailable',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
    stale: 'notFetched',
  },
  dialog: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    selected: 'notSelectable',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  banner: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    disabled: 'alwaysAvailable',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
  },
  toast: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    disabled: 'alwaysAvailable',
    selected: 'notSelectable',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  emptyState: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  readingStat: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
  },
  compassDial: {
    hover: 'notInteractive',
    focus: 'notFocusable',
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
  },
  targetMarker: {
    hover: 'notInteractive',
    focus: 'notFocusable',
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    offline: 'notNetworkBound',
  },
  contextSpine: {
    hover: 'notInteractive',
    focus: 'notFocusable',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    stale: 'notFetched',
  },
  horizonRibbon: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    focus: 'notFocusable',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
  },
  watchActionCapsule: {
    hover: 'notInteractive',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
    selected: 'notSelectable',
    stale: 'notFetched',
    offline: 'notNetworkBound',
  },
  commandPalette: {
    hover: 'notInteractive',
    pressed: 'notInteractive',
    disabled: 'alwaysAvailable',
    confirmed: 'confirmedElsewhere',
    stale: 'notFetched',
  },
  bearingScrubber: {
    pending: 'notAsync',
    disabled: 'alwaysAvailable',
    error: 'noFailureMode',
    confirmed: 'confirmedElsewhere',
  },
};

export const componentRegistry = [
  {
    id: 'primaryAction',
    name: 'Primary action',
    parts: WEB_AND_MOBILE,
    rules: ['C12', 'C19', 'C26'],
    anatomy: ['label', 'optionalLeadingIcon', 'progressMark'],
    roles: ['action', 'onAction', 'focus', 'boundary'],
    radius: 'control',
    motion: 'targetAcquired',
    states: ['default', 'hover', 'pressed', 'focus', 'pending', 'disabled', 'error', 'confirmed'],
    a11y: ['C19', 'C12'],
    native: { web: 'Button', android: 'SimtButton', wear: null },
  },
  {
    id: 'secondaryAction',
    name: 'Secondary action',
    parts: WEB_AND_MOBILE,
    rules: ['C12', 'C19'],
    anatomy: ['label', 'optionalLeadingIcon'],
    roles: ['raised', 'text', 'focus', 'boundary'],
    radius: 'control',
    motion: 'targetAcquired',
    states: ['default', 'hover', 'pressed', 'focus', 'pending', 'disabled'],
    a11y: ['C19', 'C12'],
    native: { web: 'Button', android: 'SimtButton', wear: null },
  },
  {
    id: 'destructiveAction',
    name: 'Destructive action',
    parts: WEB_AND_MOBILE,
    rules: ['C12', 'C15', 'C19'],
    anatomy: ['label', 'optionalLeadingIcon', 'consequenceSummary'],
    roles: ['danger', 'text', 'focus', 'boundary'],
    radius: 'control',
    motion: 'targetAcquired',
    states: ['default', 'hover', 'pressed', 'focus', 'pending', 'disabled', 'error', 'confirmed'],
    a11y: ['C19', 'C15'],
    native: { web: 'Button', android: 'SimtButton', wear: null },
  },
  {
    id: 'clearableField',
    name: 'Field with Clear',
    parts: ALL_WEB,
    rules: ['C13', 'C19', 'C20'],
    anatomy: ['label', 'input', 'hint', 'error', 'clearButton', 'counter'],
    roles: ['surface', 'text', 'muted', 'line', 'danger', 'focus', 'boundary'],
    radius: 'control',
    // A field can go stale, and dataStale's job is that change: dim and gain a
    // label, never a pulse. It read `null` while C31's acceptance required every
    // motif to be reachable, so the motif existed with no home.
    motion: 'dataStale',
    states: ['default', 'focus', 'pending', 'disabled', 'error', 'stale'],
    a11y: ['C13', 'C19', 'C20'],
    native: { web: 'Field', android: 'ClearableTextField', wear: null },
  },
  {
    id: 'chipSelection',
    name: 'Chips and selection',
    parts: ALL_WEB,
    rules: ['C12', 'C14', 'C19'],
    anatomy: ['label', 'check', 'leadingMarker'],
    roles: ['raised', 'text', 'muted', 'focus', 'boundary', 'identity'],
    radius: 'control',
    motion: 'listReveal',
    states: ['default', 'hover', 'pressed', 'focus', 'selected', 'disabled'],
    a11y: ['C12', 'C19'],
    native: { web: 'Chip', android: 'SimtFilterChip', wear: null },
  },
  {
    id: 'recordRow',
    name: 'Record row',
    parts: ALL_WEB,
    rules: ['C14', 'C10', 'C19'],
    anatomy: ['identity', 'primaryReading', 'secondaryReading', 'trailingAffordance'],
    roles: ['surface', 'text', 'muted', 'faint', 'line', 'boundary'],
    radius: 'record',
    motion: 'listReveal',
    states: ['default', 'hover', 'pressed', 'focus', 'selected', 'stale'],
    a11y: ['C14', 'C19', 'C08'],
    native: { web: null, android: null, wear: null },
  },
  {
    id: 'sheet',
    name: 'Sheet',
    parts: ['android', 'webApp'],
    rules: ['C15', 'C26', 'M07', 'C19'],
    anatomy: ['grabber', 'title', 'body', 'actions'],
    roles: ['raised', 'highest', 'text', 'muted', 'line'],
    layers: ['scrim', 'modal'],
    radius: 'overlay',
    motion: 'sheetRise',
    states: ['default', 'pressed', 'pending', 'error', 'offline'],
    a11y: ['C15', 'C19', 'M07'],
    native: { web: null, android: 'SimtSheetSurface', wear: null },
  },
  {
    id: 'dialog',
    name: 'Dialog',
    parts: WEB_AND_MOBILE,
    rules: ['C15', 'C19'],
    anatomy: ['title', 'body', 'closeAction', 'actions'],
    roles: ['highest', 'text', 'muted', 'line', 'focus'],
    layers: ['scrim', 'modal'],
    radius: 'overlay',
    motion: 'sheetRise',
    states: ['default', 'pending', 'disabled', 'error', 'confirmed'],
    a11y: ['C15', 'C19'],
    native: { web: 'ConfirmDialog', android: 'SimtDialog', wear: null },
  },
  {
    id: 'banner',
    name: 'Banner',
    parts: WEB_AND_MOBILE,
    rules: ['C16', 'C21', 'C19'],
    anatomy: ['icon', 'message', 'recoveryAction'],
    roles: ['raised', 'text', 'muted', 'line', 'danger', 'live', 'focus', 'boundary'],
    radius: 'record',
    motion: 'dataStale',
    states: ['default', 'pending', 'error', 'stale', 'offline'],
    a11y: ['C16', 'C19', 'C21'],
    native: { web: 'InlineError', android: 'SimtBanner', wear: null },
  },
  {
    id: 'toast',
    name: 'Toast',
    parts: WEB_AND_MOBILE,
    rules: ['C16', 'C19'],
    anatomy: ['message', 'dismissAction'],
    roles: ['highest', 'text', 'muted', 'line', 'success'],
    radius: 'record',
    motion: 'routeChange',
    states: ['default', 'pending', 'confirmed', 'error'],
    a11y: ['C16', 'C19'],
    native: { web: 'ToastProvider', android: 'SimtToast', wear: null },
  },
  {
    id: 'emptyState',
    name: 'Empty state',
    parts: WEB_AND_MOBILE,
    rules: ['C16', 'C01', 'C19'],
    anatomy: ['title', 'explanation', 'primaryAction'],
    roles: ['surface', 'text', 'muted', 'line'],
    radius: 'record',
    motion: null,
    states: ['default'],
    a11y: ['C16', 'C19'],
    native: { web: 'EmptyState', android: 'SimtEmptyState', wear: null },
  },
  {
    id: 'readingStat',
    name: 'Reading and stat',
    parts: WEB_AND_MOBILE,
    rules: ['C08', 'C21', 'C19'],
    anatomy: ['value', 'unit', 'qualifier'],
    roles: ['text', 'muted', 'faint', 'identity'],
    radius: null,
    // A stored reading falling outside its freshness window is dataStale's stated
    // job in as many words, and C18 requires stale data to be separable from the
    // value rather than interpolated into it.
    motion: 'dataStale',
    states: ['default', 'stale', 'offline'],
    a11y: ['C08', 'C19', 'C21'],
    native: { web: null, android: 'SimtReading', wear: 'WearReading' },
  },
  {
    id: 'compassDial',
    name: 'Compass dial',
    parts: ['android', 'watch'],
    rules: ['M03', 'M10', 'W06', 'C18', 'C19'],
    anatomy: ['bearingRing', 'needle', 'lubberLine', 'label'],
    roles: ['surface', 'text', 'muted', 'line', 'identity'],
    radius: 'circle',
    motion: 'spineAdvance',
    states: ['default', 'pressed', 'stale', 'offline'],
    a11y: ['M03', 'M10', 'C19'],
    native: { web: null, android: 'SimtCompassDial', wear: 'CompassScreen' },
  },
  {
    id: 'targetMarker',
    name: 'Target marker',
    parts: ['android', 'watch'],
    rules: ['C18', 'M03', 'M05', 'W05', 'C27'],
    anatomy: ['body', 'label', 'bearingTick'],
    roles: ['identity', 'text', 'line', 'boundary'],
    radius: 'circle',
    motion: 'targetAcquired',
    states: ['default', 'pressed', 'selected', 'stale'],
    a11y: ['C18', 'M05', 'C19'],
    native: { web: null, android: 'SimtTargetMarker', wear: 'CompassScreen' },
  },
  {
    id: 'contextSpine',
    name: 'Context spine',
    parts: ['webPage', 'webApp', 'android'],
    rules: ['A02', 'C26', 'C10'],
    anatomy: ['currentPosition', 'steps', 'trailingAffordance'],
    roles: ['text', 'muted', 'line', 'focus'],
    layers: ['chrome'],
    radius: 'control',
    motion: 'spineAdvance',
    states: ['default', 'pressed', 'selected', 'pending', 'offline'],
    a11y: ['A02', 'C19', 'C26'],
    native: { web: null, android: 'SimtContextSpine', wear: null },
  },
  {
    id: 'horizonRibbon',
    name: 'Horizon ribbon',
    parts: WEB_AND_MOBILE,
    rules: ['A04', 'C27', 'C26'],
    anatomy: ['title', 'progressTrack', 'elapsedLabel'],
    roles: ['text', 'muted', 'line', 'live'],
    layers: ['chrome'],
    radius: 'control',
    motion: 'horizonSweep',
    states: ['default', 'pending', 'stale', 'offline'],
    a11y: ['A04', 'C19'],
    native: { web: null, android: 'SimtHorizonRibbon', wear: null },
  },
  {
    id: 'watchActionCapsule',
    name: 'Watch action capsule',
    parts: ['watch'],
    rules: ['W02', 'W05', 'W03', 'C19'],
    anatomy: ['icon', 'label', 'progressArc'],
    roles: ['surface', 'text', 'muted', 'boundary', 'focus'],
    radius: null,
    motion: 'targetAcquired',
    states: ['default', 'pressed', 'focus', 'pending', 'disabled'],
    a11y: ['W02', 'W03', 'W04', 'C19'],
    native: { web: null, android: null, wear: 'WearActionCapsule' },
  },
  {
    id: 'commandPalette',
    name: 'Command palette',
    parts: ['webApp'],
    rules: ['A14', 'C15', 'C19', 'C26'],
    anatomy: ['trigger', 'searchInput', 'resultList', 'shortcutHint'],
    roles: ['highest', 'surface', 'text', 'muted', 'focus', 'boundary', 'line'],
    layers: ['scrim', 'modal'],
    radius: 'overlay',
    motion: 'apertureMorph',
    states: ['default', 'focus', 'pending', 'error', 'selected', 'offline'],
    a11y: ['A14', 'C15', 'C19', 'C20'],
    native: { web: 'CommandPalette', android: null, wear: null },
  },
  {
    id: 'bearingScrubber',
    name: 'Bearing scrubber',
    parts: ['webPage', 'android', 'watch'],
    rules: ['C08', 'P12', 'M15', 'W13', 'C19', 'C20'],
    anatomy: ['bearingArc', 'targetCorona', 'headingThumb', 'deltaReadout'],
    roles: ['surface', 'text', 'muted', 'precision', 'live', 'focus', 'boundary'],
    radius: 'circle',
    motion: 'instrumentDetent',
    states: ['default', 'hover', 'pressed', 'focus', 'selected', 'stale', 'offline'],
    a11y: ['C08', 'C19', 'C20', 'W04'],
    native: { web: 'BearingDial', android: 'SimtBearingScrubber', wear: 'WearRotaryDial' },
  },
];

/** The parts a component registry is sliced by, read from the adapter keys. */
export const COMPONENT_PARTS = Object.keys(platformAdapters).filter((key) => !key.startsWith('$'));

/**
 * What each part is called, keyed by the adapter key the registry slices on. A
 * part index that showed `webPage` would be publishing an internal key to the
 * reader, so the label travels with the vocabulary that names it. The guard
 * compares this against the parts the site publishes, because a mapping entry
 * with no page is a part an agent cannot read and a page with no entry is a part
 * no component can be sliced for.
 */
export const PART_LABELS = {
  webPage: 'Web page',
  webApp: 'Web app',
  android: 'Mobile app',
  watch: 'Watch app',
};

/**
 * Rule prefixes, in adapter keys so a component's `parts` and the rules it
 * cites are the same vocabulary. A component that appears in two parts cites the
 * rule each of them owns — the watch dial is M03 on a handset and W06 on a
 * wrist — because a part inherits only the common rules and its own, so citing
 * the other platform's rule would name a requirement this slice never inherits.
 */
const RULE_PREFIX = { P: 'webPage', A: 'webApp', M: 'android', W: 'watch', C: 'common' };

// The document each platform is published under, so the adapter key a rule is
// written in and the slug a reader follows are one fact with one owner: a part
// added here appears in the token payloads, the component slices and the rule
// links at once, rather than three tables that agree until one of them is edited
// and a card starts pointing at a 404 rule anchor.
export const PART_SLUGS = {
  webPage: 'web-page',
  webApp: 'web-app',
  android: 'mobile-app',
  watch: 'watch-app',
  common: 'common',
};

export const PART_PREFIXES = Object.fromEntries(
  Object.entries(RULE_PREFIX).map(([prefix, adapter]) => [adapter, prefix]),
);

/** Which platform each rule ID belongs to, read from its prefix rather than a second table. */
export function platformForRule(ruleId) {
  for (const [prefix, part] of Object.entries(RULE_PREFIX)) {
    if (ruleId.startsWith(prefix)) return part;
  }
  return 'unknown';
}

/** The document a rule ID is read from, derived from the platform that owns it. */
export function slugForRule(ruleId) {
  return PART_SLUGS[platformForRule(ruleId)] ?? 'unknown';
}

/**
 * The timing a component may spend, derived from the motif it names rather than
 * typed beside it.
 *
 * C31 says the motif is what a state change spends: "spend only the properties,
 * duration and easing tokens that motif lists". So the duration and easing roles
 * are the motif's, not the component's, and a component that named them itself
 * would be a second copy of motion.json that could disagree with it — the first
 * draft of this registry had three actions publishing `feedback` beside a motif
 * whose own duration is `state`. Naming the motif is enough, and this resolves
 * it for a reader so the published component still answers "which easing".
 *
 * A component with no motif has no timing, because C31 also says a change with no
 * motif does not animate. `null` is that answer, and it is not a missing value:
 * the field is published as a stage that does not animate, which is a different
 * design decision from a stage nobody has described yet.
 */
export function motionRolesFor(component) {
  if (component.motion === null) return null;
  const motif = motionMotifs.find((candidate) => candidate.id === component.motion);
  if (motif === undefined) return null;
  return { motif: motif.id, duration: motif.duration, easing: motif.easing };
}


// When to fetch the component document. Published twice — once as the pointer
// every token document carries, once by the document itself — for the reason the
// contrast and motion documents do it: written once here so the gate can compare
// the two renderings rather than trusting them to stay equal.
export const COMPONENTS_FETCH_WHEN = [
  'building or changing any surface, before choosing a pattern',
  'deciding which rules a component inherits and therefore which it must satisfy',
  'porting a component to a native toolkit and finding the counterpart that exists',
];
