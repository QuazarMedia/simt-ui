import { platformAdapters, tokens } from './tokens.js';
import { six } from './starfield.js';

// Motion motifs.
//
// C18 says what may move and C17 says how long a tier may take, but between
// them a component was still free to invent the transition itself: pick a
// duration, pick a curve, decide how many steps the change takes. That is the
// part of motion a reader cannot predict, and it is where a system stops being
// one system — two products both conform to Design and animate the same state
// change differently.
//
// So a motif is a named choreography with exactly one job. It states what
// triggered it, the ordered steps, the properties it is allowed to touch, the
// duration role and easing purposes it resolves to, what it does in each of the
// three tiers, and how it is built on each platform. A state change that is not
// in this list is not animated; it is applied. There is no fourth tier and no
// per-component curve, because a curve that is not published cannot be checked
// and a duration that is not a role cannot be read off the mapping.
//
// Every motif settles to the same resting state in all three tiers. That is
// C17's "Full/Reduced/Static produce identical outcomes" made checkable: the
// tiers differ in travel and time, never in outcome.

// The properties a transition may touch. C18 already requires transform and
// opacity for transitional UI and rules out width, height and blur; a motif
// may use a subset of this list and nothing else, so "animate it nicely" has no
// answer to give.
export const MOTION_PROPERTIES = ['transform', 'opacity'];

// Duration roles, read from the shared token group rather than typed here, so a
// role that is renamed or added in tokens.motion reaches the motifs with it.
export const MOTION_DURATIONS = Object.keys(tokens.motion).filter((key) => tokens.motion[key].$type === 'duration');

// Easing purposes and the part keys each one resolves to. The purposes are
// named by what the transition is for rather than by a curve, because the same
// arrival is emphasis or entrance on web and enter natively. specification.js
// renders its easing table from this, so the table and the resolution a motif
// performs cannot be two answers.
export const EASING_PURPOSES = {
  standard: { label: 'Standard — in-place transform and state', keys: { web: ['standard'], native: ['standard'] } },
  arrival: { label: 'Arrival — emphasis and entrance', keys: { web: ['emphasis', 'entrance'], native: ['enter'] } },
  departure: { label: 'Departure — exit', keys: { web: ['exit'], native: ['exit'] } },
  transform: { label: 'Transform only', keys: { web: [], native: ['transform'] } },
};

/** The part groups that share a native easing set, as published in platformAdapters. */
const NATIVE_PARTS = ['android', 'watch'];

/**
 * The parts a motion port has to cover, read from the mapping rather than typed.
 * platformAdapters carries a $description beside its four parts, and the wrist
 * part publishes no radius group at all, so nothing here can assume a shape of
 * adapter that a part may legitimately leave out.
 */
export const MOTION_PARTS = Object.keys(platformAdapters).filter((key) => !key.startsWith('$'));

/** Resolve a motif's easing purposes to the curve keys a part actually publishes. */
export function easingKeysFor(part, purposes) {
  const native = NATIVE_PARTS.includes(part);
  return purposes.flatMap((purpose) => EASING_PURPOSES[purpose].keys[native ? 'native' : 'web']);
}

/**
 * The duration token a motif's role resolves to on a part, or null when the part
 * publishes none. An absent role answers null rather than undefined so a caller
 * asking "does this part publish it?" gets one answer either way; a plain
 * property lookup answers undefined, which every `!== null` test would accept.
 */
export function durationFor(part, role) {
  return platformAdapters[part].duration[role] ?? null;
}

/** The curve a purpose resolves to on a part, or null when the part publishes none. */
export function easingFor(part, purpose) {
  const key = easingKeysFor(part, [purpose])[0];
  return key === undefined ? null : platformAdapters[part].easing[key];
}

export const motionMotifs = [
  {
    id: 'targetAcquired',
    job: 'Tell the person that the thing they were tracking is now the thing they have.',
    trigger: 'A selection, fix or search resolves on a target the person was already watching.',
    steps: [
      'The glow scales past its resting size and settles back while its opacity resolves.',
      'The ring resolves to its final angle with the glow.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'state',
    easing: ['standard', 'arrival'],
    tiers: {
      full: 'Glow scale and ring angle both travel over the state duration.',
      reduced: 'Opacity only, over the feedback duration; the ring appears already at its final angle.',
      static: 'Glow and ring settle at once.',
    },
    platforms: {
      css: 'One keyframe set on a wrapper: transform for scale and angle, opacity for the glow.',
      compose:
        'A single graphicsLayer animating scale and rotation; the glow is a separate layer so the two can differ in duration.',
      wear: 'The same two layers. Round geometry carries the ring; the arc length is the measured one, not a web constant.',
    },
    settlesTo: 'The ring at its final angle and the glow at its resting opacity.',
  },
  {
    id: 'listReveal',
    job: 'Show that a set arrived, in an order a person can follow down.',
    trigger: 'A list is populated for the first time, filtered, or reordered.',
    steps: [
      'Items enter along an arc, each offset from the one above it.',
      'Each item resolves to opacity as it reaches its row.',
      'The last item resolves; no item animates after that.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'feedback',
    easing: ['arrival'],
    tiers: {
      full: 'Items travel the arc, staggered by a fraction of the feedback duration each, within one feedback budget in total.',
      reduced: 'Opacity only, staggered, over the feedback duration.',
      static: 'The list is present at once, in its final order.',
    },
    platforms: {
      css: 'One animation-delay per index on a transform and opacity pair; the stagger is a custom property, never a per-row literal.',
      compose:
        'A staggered reveal on the item layout; a LazyColumn does not animate its own content and must not be asked to.',
      wear: 'Never used on a watch list. A round screen has no arc to reveal along, and scrolling competes with the reveal for the same gesture.',
    },
    settlesTo: 'Every item at its row, at full opacity, in the final order.',
  },
  {
    id: 'horizonSweep',
    job: 'Carry time or progress across a wide surface without a spinner.',
    trigger: 'A timeline, countdown or long-running task advances to a new reading.',
    steps: [
      'A band translates across the horizon gradient, leaving the previous reading behind it.',
      'The band resolves at the position the stored reading gives it.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'context',
    easing: ['standard'],
    tiers: {
      full: 'The band travels over the context duration.',
      reduced:
        'The band is placed at its final position and its opacity resolves over the feedback duration; no travel.',
      static: 'The band is placed at its final position.',
    },
    platforms: {
      css: 'A gradient overlay inside overflow:hidden, moved with transform so the horizon itself is never repainted.',
      compose: 'A translated overlay layer; the gradient is a brush on the parent, not an animated shader.',
      wear: 'Never used on a watch. There is no horizon band wide enough to sweep, and a travelling element on a round screen reads as a second hand.',
    },
    settlesTo: 'The band at the position of the stored reading, with the earlier reading no longer drawn.',
  },
  {
    id: 'spineAdvance',
    job: 'Move context forward one step, so the person always knows which step they are on.',
    trigger: 'The context spine advances from the current step to the next one.',
    steps: [
      'The outgoing step resolves to inactive.',
      'The incoming step resolves to active.',
      'The connector between them fills to its new length.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'state',
    easing: ['standard'],
    tiers: {
      full: 'The outgoing and incoming steps cross-resolve and the connector grows over the state duration.',
      reduced:
        'The connector is placed at its final length and the two steps cross-resolve in opacity over the feedback duration.',
      static: 'The step and the connector are set together, with no cross-resolve.',
    },
    platforms: {
      css: 'Transform on the connector so growth is a scale rather than a width; opacity on the two labels.',
      compose: 'A scaleX on the connector inside a fixed-width track; labels cross-fade.',
      wear: 'One step at a time. The spine is the round bezel: the connector is the bezel arc and only the active step label changes.',
    },
    settlesTo: 'The incoming step active, the outgoing step inactive, the connector at its final length.',
  },
  {
    id: 'sheetRise',
    job: 'Show which context a layer came from, and leave it dismissible.',
    trigger: 'A sheet, dialog or popover is presented over the current context.',
    steps: [
      'The scrim resolves its opacity.',
      'The surface travels in from the edge it is anchored to.',
      'The drag or close affordance resolves with the surface, never after it.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'state',
    easing: ['arrival', 'departure'],
    tiers: {
      full: 'Scrim and surface travel together over the state duration; dismissal uses the departure purpose.',
      reduced: 'The scrim resolves over the feedback duration and the surface is presented at its resting offset.',
      static:
        'The surface is presented at its resting offset with the scrim at its resting opacity. It is still a dismissible layer with a real close target.',
    },
    platforms: {
      css: 'A dialog element and its backdrop, transformed rather than resized, with the surface kept inside the viewport on every breakpoint.',
      compose: 'A ModalBottomSheet or Dialog; the sheet does its own scrim and must not be given a second one.',
      wear: 'A single full-bleed layer. A bottom sheet on a round screen covers the bezel, so the watch presents its surface as a replacement view with a back target instead.',
    },
    settlesTo: 'The surface at its resting offset, the scrim at its resting opacity, dismissal still available.',
  },
  {
    id: 'dataStale',
    job: 'Say that a reading is no longer fresh, without asking for attention.',
    trigger: 'A stored reading falls outside its freshness window.',
    steps: [
      'The reading resolves to the dimmed treatment.',
      'The stale label appears with the reading, and is the only new element.',
    ],
    properties: ['opacity'],
    duration: 'feedback',
    easing: ['standard'],
    tiers: {
      full: 'The dim resolves over the feedback duration. It runs once.',
      reduced: 'The dim resolves over the feedback duration, opacity only, once.',
      static: 'The reading is presented dimmed with its label.',
    },
    platforms: {
      css: 'An opacity step on the reading, plus a class or attribute that reveals the label. The label is never cross-faded into place.',
      compose:
        'An alpha change on the reading container, and the label as a sibling so its content is present for assistive technology from the first frame.',
      wear: 'Dimming only. A watch reading carries no label, because there is no room for one beside a value; the staleness is stated in the detail view instead.',
    },
    settlesTo: 'The reading dimmed, with its stale label present.',
    constraints: [
      'Never a pulse, a blink, a loop or a repeat. A reading that has gone stale has not become more urgent, and attention-pulsing a stored value inverts C21.',
      'The dim is opacity on the reading. The label is inserted, not animated, so a stale reading is never briefly readable as a fresh one.',
    ],
  },
  {
    id: 'routeChange',
    job: 'Carry the reader from one context to the next without losing the frame.',
    trigger: 'A route change resolves to a different context.',
    steps: [
      'The outgoing context resolves out on the departure purpose.',
      'The incoming context arrives on the arrival purpose.',
      'Any shared spine advances on its own motif, not on this timeline.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'context',
    easing: ['arrival', 'departure'],
    tiers: {
      full: 'Outgoing and incoming run on the context duration, in opposite directions.',
      reduced: 'Both resolve in opacity over the feedback duration, with no travel.',
      static: 'The incoming context is presented at once.',
    },
    platforms: {
      css: 'Two cross-resolving layers inside a stable frame; the route does not scroll, and a back navigation runs the same motif in reverse.',
      compose:
        'A shared NavHost frame whose outgoing and incoming composables cross-resolve; the host owns the duration so a screen cannot pick its own.',
      wear: 'A replacement view with the back target always visible. Cross-fading two contexts on a round screen hides the bezel that carries the gesture.',
    },
    settlesTo:
      'The incoming context at rest, the outgoing one gone, focus and scroll at the incoming context own values.',
  },
  {
    id: 'apertureMorph',
    job: 'Carry the single focal anchor between summary and detail.',
    trigger: 'A selected record, target marker or instrument tile opens its detail context or returns to its origin.',
    steps: [
      'The single named focal anchor morphs across the container frame.',
      'Surrounding chrome cross-resolves in opacity without blocking input.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'state',
    easing: ['arrival', 'departure'],
    tiers: {
      full: 'The focal anchor and its container frame travel over the state duration; dismissal reverses on departure.',
      reduced: 'Opacity cross-resolve over feedback; no travel.',
      static: 'The destination surface settles immediately with the focal anchor in place.',
    },
    platforms: {
      css: 'One view-transition-name per route on the focal anchor; animate transform and opacity only on snapshots, never blocking pointer input.',
      compose:
        'SharedTransitionLayout with sharedElement on the focal anchor inside graphicsLayer; avoid sibling re-layout.',
      wear: 'Never morph across a round bezel; present the destination directly with a visible back target.',
    },
    settlesTo: 'The focal object at its destination geometry, chrome settled and focus on the destination heading.',
  },
  {
    id: 'instrumentDetent',
    job: 'Confirm a dial, scrubber or sheet locking onto a detent.',
    trigger: 'A bearing enters target alignment, a rotary/slider crosses a cardinal detent, or a sheet settles onto an anchor.',
    steps: [
      'The lock reticle pulses subtly in scale and highlight opacity once.',
      'The reticle settles cleanly at rest with exact numeric confirmation.',
    ],
    properties: ['transform', 'opacity'],
    duration: 'feedback',
    easing: ['standard'],
    tiers: {
      full: 'One-shot reticle scale (1.0 -> 1.06 -> 1.0) and highlight peak over the feedback duration; paired haptic tick on native.',
      reduced: 'Opacity lock highlight over feedback; no scale travel.',
      static: 'Locked state and numeric confirmation settle immediately.',
    },
    platforms: {
      css: 'Compositor-only transform scale and opacity on the reticle layer; triggered once per crossing without shifting labels.',
      compose:
        'One-shot graphicsLayer scale/alpha via Animatable paired with debounced HapticFeedback when system haptics are enabled.',
      wear: 'Single reticle highlight paired with rotary CONFIRM detent; completely suppressed in ambient mode.',
    },
    settlesTo: 'The reticle at target alignment with exact numeric confirmation and no looping glow.',
  },
];

// When to fetch the motif document. Published twice — once as the pointer every
// token document carries, once by the document itself — so it is written once
// here and compared by the gate, for the reason the contrast document is.
export const MOTION_FETCH_WHEN = [
  'animating a state change, on any platform',
  'choosing between the tiers, or deciding whether a change animates at all',
  'porting a transition to a native toolkit and proving curve parity',
];

// Curve sampling, so a native port can prove its easing matches without reading
// this file. The starfield fixture works the same way: the algorithm is
// published, the samples are published, and verification regenerates them.
//
// A port has to evaluate the same cubic-bezier the browser does. The curve is
// sampled on x, and x(t) is monotonic for every published curve because both of
// its x control points lie within [0, 1], so a fixed number of bisection
// iterations finds the same t in any language. A fixed iteration count is the
// portability: an adaptive tolerance would stop at different points on
// different hardware.
const CURVE_SAMPLES = 21;
const CURVE_BISECTION_STEPS = 64;

const curveX = (curve, t) => {
  const mt = 1 - t;
  return 3 * mt * mt * t * curve[0] + 3 * mt * t * t * curve[2] + t * t * t;
};
const curveY = (curve, t) => {
  const mt = 1 - t;
  return 3 * mt * mt * t * curve[1] + 3 * mt * t * t * curve[3] + t * t * t;
};

/** The curve's progress at x, or the endpoint when x is outside 0..1. */
export function sampleCurve(curve, x) {
  if (x <= 0) return 0;
  if (x >= 1) return 1;
  let low = 0;
  let high = 1;
  for (let step = 0; step < CURVE_BISECTION_STEPS; step++) {
    const middle = (low + high) / 2;
    if (curveX(curve, middle) < x) low = middle;
    else high = middle;
  }
  return curveY(curve, (low + high) / 2);
}

/** Every published curve for a part, sampled at CURVE_SAMPLES points of x. */
export function sampledCurves(part) {
  const sampled = {};
  // The adapter carries $description and $extensions beside its groups; only a
  // curve is a curve.
  const keys = Object.keys(platformAdapters[part].easing).filter((key) => !key.startsWith('$'));
  for (const key of keys) {
    const curve = platformAdapters[part].easing[key].$value;
    sampled[key] = {
      curve,
      samples: Array.from({ length: CURVE_SAMPLES }, (_, index) =>
        six(sampleCurve(curve, index / (CURVE_SAMPLES - 1))),
      ),
    };
  }
  return sampled;
}

export const motionParityAlgorithm = [
  'For each published curve, sample x from 0 to 1 in equal steps of 1 / (count - 1).',
  'x(t) = 3(1-t)²t·x1 + 3(1-t)t²·x2 + t³, and the same form with y1, y2 for y(t). P0 is (0,0) and P3 is (1,1), so their terms drop out.',
  'Bisect t on [0, 1] a fixed number of iterations, keeping the half where x(t) is below x, and evaluate y(t) at the final midpoint.',
  'Publish y to six decimal places. The endpoints are 0 and 1 exactly; every sample is monotonic non-decreasing.',
];

/**
 * The published parity fixture: the sampled curve values a native port compares
 * against, and the timing each motif resolves to.
 *
 * The resolution is published per easing group rather than per part because the
 * parts declare their easing set by group — the two web parts share one set and
 * the two native parts share another — so a per-part copy would be four answers
 * where the mapping has two.
 */
export function motionFixture() {
  return {
    algorithm: motionParityAlgorithm,
    sampleCount: CURVE_SAMPLES,
    bisectionSteps: CURVE_BISECTION_STEPS,
    durations: Object.fromEntries(MOTION_DURATIONS.map((role) => [role, durationFor('webApp', role).$value.value])),
    curves: Object.fromEntries(MOTION_PARTS.map((part) => [part, sampledCurves(part)])),
    timing: motionMotifs.map((motif) => ({
      id: motif.id,
      duration: motif.duration,
      easing: {
        web: easingKeysFor('webPage', motif.easing),
        native: easingKeysFor('android', motif.easing),
      },
      properties: motif.properties,
      settlesTo: motif.settlesTo,
    })),
  };
}
