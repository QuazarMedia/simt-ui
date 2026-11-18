import { paintVariable } from './tokens.js';
import {
  COMPONENT_FAMILIES,
  componentRegistry,
  componentsForFamily,
  familyFor,
  PART_LABELS,
  motionRolesFor,
  slugForRule,
} from './components.js';
import { motionMotifs } from './motion.js';
import { STATE_TREATMENTS, releasePath } from './specification.js';
import { motionPicker } from './specimens.js';

// The component gallery.
//
// A gallery card is a rendering of a registry entry, not a second description of
// one. The rule is the same one the registry itself follows: the treatments live
// in the rules, the timing lives in the motif, and what is drawn here is the
// registry entry plus those two sources. A card that restated a treatment would be
// a second answer that could disagree with the rule it illustrates, which is the
// failure the whole registry exists to prevent.
//
// Everything on a card works without JavaScript. The state matrix is a static
// table, and a motion card is a filmstrip of still frames — the same frames a
// person would see if the page never ran a script, which is what the Static tier
// shows anyway. Playback is an addition, not the source of the explanation.
//
// Every colour below is a published token resolved through the themed page, so a
// card follows whichever profile the reader selected. C27 is why the specimens
// draw from the gradient grammar rather than inventing a glow.

// A painted name resolves to the variable that actually publishes it, and a name
// nothing publishes fails the build rather than silently painting black: the
// browser drops a declaration whose var() is unresolved, and an SVG fill then falls
// back to its initial value, which is black. That is how every primary action here
// rendered as a black rectangle while the colours that happened to be aliases
// painted correctly and hid the fault.
const variable = paintVariable;
const paint = (property, name) => `style="${property}:var(${variable(name, property)})"`;
// An element carries one style attribute: a browser honours the first and drops
// every later one, so a shape cannot be assembled out of two paint() calls. The
// shapes that needed a surface fill and a boundary stroke were emitting both and
// losing the stroke, which is invisible in the markup and total on screen.
const painted = (entries) =>
  `style="${Object.entries(entries)
    .map(([property, name]) => `${property}:var(${variable(name, property)})`)
    .join(';')}"`;

// The stage every card is drawn in: a fixed frame the animation transforms inside,
// so a card that moves never reflows the page around it.
const STAGE = 'viewBox="0 0 320 180"';

const round = (radius) => `rx="${radius}"`;

// Hover, pressed and disabled are the tone of a component's own surface rather
// than a mark on it, so they are drawn as tone. STATE_TREATMENTS calls them a
// tonal lift, an inset tone and a muted label, which in a schematic is one
// ladder; it is defined once here so seventeen shapes cannot each invent their own
// step, and a filled shape spends fill-opacity while a drawn one spends
// stroke-opacity, because a component made of strokes has no fill to lower.
// Disabled dims rather than lifts, and C13 forbids lowering the opacity of
// everything *around* a control, so only the control's own surface moves.
const FILL_TONE = { hover: '.82', pressed: '.66', disabled: '.5' };
const STROKE_TONE = { hover: '.8', pressed: '.55', disabled: '.35' };
const fillTone = (state) => `fill-opacity="${FILL_TONE[state] ?? '1'}"`;
const strokeTone = (state) => `stroke-opacity="${STROKE_TONE[state] ?? '1'}"`;

// The four states that are facts about a surface rather than about the shape
// inside it. A component whose own shape has no surface to change cannot show
// them at all, so they are drawn once in the stage gutter instead — which is how a
// card used to list states it could not show: thirty-two of sixty-eight drawings
// were byte-identical to their resting state, which is the fault this registry
// exists to prevent. The gutter sits to the right of every composition, so a card
// is the same size in every state and two states stay comparable.
const GUTTER = {
  pending: `<circle cx="288" cy="90" r="11" fill="none" ${paint('stroke', 'focus')} stroke-width="2" stroke-dasharray="5 4"/><path d="M288 101v5" ${paint('stroke', 'focus')} stroke-width="2"/>`,
  error: `<circle cx="288" cy="90" r="10" fill="none" ${paint('stroke', 'danger')} stroke-width="2"/><path d="M284 86l8 8M292 86l-8 8" ${paint('stroke', 'danger')} stroke-width="2"/>`,
  selected: `<path d="M282 90l5 5 9-11" fill="none" ${paint('stroke', 'identity')} stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`,
  stale: `<rect x="272" y="76" width="32" height="16" ${painted({ fill: 'raised', stroke: 'line' })} ${round(6)}/><text x="288" y="88" text-anchor="middle" ${paint('fill', 'muted')} font-family="sans-serif" font-size="9">STALE</text>`,
};

// Which states a component's own shape knows how to draw. A state a component
// declares, that its shape does not draw, and that is not in the gutter either,
// would be a card listing a treatment it cannot show. This record is a claim about
// the drawing rather than a substitute for it: a state claimed here that the shape
// does not actually draw leaves two identical pictures behind and fails the gate,
// which compares the published drawings rather than reading this table.
const SELF_DRAWN = {
  primaryAction: ['hover', 'pressed', 'pending', 'disabled', 'error'],
  secondaryAction: ['hover', 'pressed', 'disabled'],
  destructiveAction: ['hover', 'pressed', 'disabled', 'error'],
  clearableField: ['pending', 'disabled', 'error'],
  chipSelection: ['hover', 'pressed', 'selected', 'disabled'],
  recordRow: ['hover', 'pressed', 'selected', 'stale'],
  sheet: ['pressed', 'error'],
  dialog: ['disabled'],
  banner: ['error'],
  toast: [],
  emptyState: [],
  readingStat: ['stale'],
  compassDial: ['pressed'],
  targetMarker: ['pressed'],
  contextSpine: ['pressed', 'selected'],
  horizonRibbon: ['stale'],
  watchActionCapsule: ['pressed', 'pending', 'disabled'],
};

/**
 * The drawn form of one component in one state.
 *
 * Each shape is deliberately schematic and labelled illustrative: a card is a
 * diagram of a component, not a screenshot, and C21 asks for invented specimen
 * data to be labelled. The `state` argument changes what is drawn, never the
 * composition, so two states of one component are comparable.
 */
// One set of painters per state, built once and reused. Every one of them closes
// over the state, so building them per drawing meant seventeen functions thrown
// away a hundred and thirty-seven times to produce the same strings; the gallery
// is the largest single cost in the build and this is most of it.
const PAINTERS = new Map();

function paintersFor(state) {
  if (!PAINTERS.has(state)) PAINTERS.set(state, buildPainters(state));
  return PAINTERS.get(state);
}

function buildPainters(state) {
  const base = `${painted({ fill: 'hull', stroke: 'line' })}`;
  // A focus ring is drawn rather than described: C19 and C12 both make it a
  // visible obligation, and a card that only named it would not show it.
  const focus =
    state === 'focus'
      ? `<rect x="60" y="48" width="200" height="84" fill="none" ${paint('stroke', 'focus')} stroke-width="3" stroke-dasharray="6 5"/>`
      : '';
  const label = (x, y, text, role = 'text') =>
    `<text x="${x}" y="${y}" ${paint('fill', role)} font-family="sans-serif" font-size="13">${text}</text>`;

  const shapes = {
    primaryAction:
      () => `<rect x="84" y="62" width="152" height="52" ${painted({ fill: state === 'disabled' ? 'muted' : 'action', stroke: 'line' })} ${round(10)} ${fillTone(state)}/>
      <circle cx="112" cy="88" r="9" fill="none" ${paint('stroke', 'onAction')} stroke-width="2"/>
      ${label(132, 94, 'Confirm', 'onAction')}${focus}
      ${state === 'pending' ? `<circle cx="224" cy="88" r="11" fill="none" ${paint('stroke', 'onAction')} stroke-width="2.5" stroke-dasharray="5 4"/>` : ''}
      ${state === 'error' ? `<circle cx="224" cy="88" r="10" fill="none" ${paint('stroke', 'danger')} stroke-width="2.5"/><path d="M219 83l10 10M229 83l-10 10" ${paint('stroke', 'danger')} stroke-width="2"/>` : ''}`,

    secondaryAction: () => `<rect x="84" y="62" width="152" height="52" ${base} ${round(10)} ${fillTone(state)}/>
      <circle cx="112" cy="88" r="9" fill="none" ${paint('stroke', state === 'disabled' ? 'muted' : 'text')} ${strokeTone(state)} stroke-width="2"/>
      ${label(132, 94, 'Preview', state === 'disabled' ? 'muted' : 'text')}${focus}`,

    destructiveAction: () => `<rect x="84" y="62" width="152" height="52" ${base} ${round(10)} ${fillTone(state)}/>
      ${label(112, 94, 'Delete record', state === 'disabled' ? 'muted' : 'danger')}${focus}
      ${state === 'error' ? `<path d="M216 80l14 14M230 80l-14 14" ${paint('stroke', 'danger')} stroke-width="2.5"/>` : ''}`,

    clearableField:
      () => `<text x="60" y="62" ${paint('fill', 'muted')} font-family="sans-serif" font-size="12">Target name</text>
      <rect x="60" y="72" width="200" height="46" ${base} ${round(10)} ${fillTone(state)}/>
      ${label(76, 102, 'Riverside lookout', state === 'disabled' ? 'muted' : 'text')}
      <circle cx="236" cy="95" r="11" fill="none" ${paint('stroke', 'muted')} stroke-width="1.5"/>
      <path d="M232 91l8 8M240 91l-8 8" ${paint('stroke', 'muted')} stroke-width="1.5"/>
      ${focus}
      ${state === 'error' ? `<path d="M70 128h180" ${paint('stroke', 'danger')} stroke-width="2"/>` : ''}
      ${state === 'pending' ? `<circle cx="240" cy="95" r="13" fill="none" ${paint('stroke', 'focus')} stroke-width="2" stroke-dasharray="4 3"/>` : ''}`,

    // Selection is a border and a check, the two things C12 names, and the label
    // stays in text. It used to fill the chip with identity and set the label in
    // ink, which measured 2.00:1 in deep space, 2.96:1 in daylight and 1.00:1 in
    // astronomy red -- the label was its own fill. Void on identity clears 4.5 in
    // every profile that has one, but a tonal treatment needs no second answer at
    // all: text on raised is the pair the token file already publishes, and the
    // identity ring carries the selection on its own.
    chipSelection: () => `<g>
      <rect x="60" y="74" width="94" height="34" ${painted({ fill: 'raised', stroke: state === 'selected' ? 'identity' : 'line' })} ${round(17)} ${state === 'selected' ? '' : fillTone(state)}/>
      ${state === 'selected' ? `<path d="M76 91l4 4 7-8" fill="none" ${paint('stroke', 'identity')} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>` : ''}
      ${label(state === 'selected' ? 96 : 80, 96, 'Outdoors', state === 'selected' ? 'text' : 'muted')}
      <rect x="164" y="74" width="96" height="34" ${painted({ fill: 'raised', stroke: 'line' })} ${round(17)}/>
      ${label(184, 96, 'Indoor', 'muted')}${focus}</g>`,

    recordRow: () => `<g>
      <circle cx="82" cy="90" r="14" fill="none" ${paint('stroke', state === 'selected' ? 'identity' : 'moss')} ${strokeTone(state)} stroke-width="2"/>
      ${state === 'selected' ? `<path d="M76 90l4 4 8-9" fill="none" ${paint('stroke', 'identity')} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>` : ''}
      ${label(106, 86, 'Trail walk')}
      <text x="106" y="104" ${paint('fill', state === 'stale' ? 'faint' : 'muted')} font-family="sans-serif" font-size="11">${state === 'stale' ? 'Yesterday · reading may be old' : 'Today 18:30 · Riverside'}</text>
      <path d="M244 90l8 8-8 8" fill="none" ${paint('stroke', 'muted')} stroke-width="1.5"/>${focus}</g>`,

    sheet:
      () => `<rect x="60" y="34" width="200" height="126" ${painted({ fill: 'void', stroke: 'line' })} ${round(12)}/>
      <rect x="132" y="40" width="56" height="4" ${paint('fill', 'line')} ${round(2)}/>
      <path d="M72 160h176" ${paint('stroke', 'action')} stroke-width="2" stroke-opacity="${state === 'pressed' ? '.5' : '1'}"/>
      ${label(104, 100, 'Request to join', 'action')}${focus}
      ${state === 'error' ? `<path d="M72 172h176" ${paint('stroke', 'danger')} stroke-width="2"/>` : ''}`,

    // The scrim is the dimming veil C15 puts between a modal and what it covers,
    // so it is painted as a veil: the void anchor at half strength, laid down
    // before the modal and a little larger than it. It was drawn as a one-pixel
    // boundary in the layer token, which publishes a stacking tier rather than a
    // colour, so the declaration was dropped and the shape vanished — the one
    // component whose whole point is a modal over a scrim was the one drawing
    // with no scrim on it.
    dialog:
      () => `<rect x="52" y="26" width="216" height="142" ${paint('fill', 'void')} fill-opacity=".55" ${round(16)}/>
      <rect x="82" y="56" width="156" height="96" ${painted({ fill: 'raised', stroke: 'line' })} ${round(12)} ${fillTone(state)}/>
      ${label(100, 92, 'Join this activity?')}
      <rect x="100" y="112" width="60" height="24" ${paint('fill', 'action')} ${round(8)}/>
      <rect x="168" y="112" width="52" height="24" ${painted({ fill: 'hull', stroke: 'line' })} ${round(8)}/>${focus}`,

    banner:
      () => `<rect x="60" y="66" width="200" height="52" ${painted({ fill: state === 'error' ? 'danger' : 'raised', stroke: state === 'error' ? 'danger' : 'line' })} fill-opacity="${state === 'error' ? '.18' : '1'}" ${round(10)}/>
      <circle cx="84" cy="92" r="9" fill="none" ${paint('stroke', state === 'error' ? 'danger' : 'live')} stroke-width="2"/>
      ${label(102, 97, state === 'error' ? 'Location is shared' : 'Sync paused', state === 'error' ? 'danger' : 'text')}${focus}`,

    toast:
      () => `<rect x="76" y="72" width="168" height="44" ${painted({ fill: 'highest', stroke: 'line' })} ${round(10)}/>
      ${label(96, 99, 'Saved to your list')}${focus}`,

    emptyState:
      () => `<circle cx="160" cy="82" r="24" fill="none" ${paint('stroke', 'line')} stroke-width="1.5" stroke-dasharray="3 5"/>
      ${label(112, 128, 'Nothing saved yet', 'muted')}
      <rect x="120" y="138" width="80" height="26" ${painted({ fill: 'raised', stroke: 'line' })} ${round(8)}/>${focus}`,

    readingStat:
      () => `<text x="60" y="70" ${paint('fill', 'muted')} font-family="sans-serif" font-size="12">Heading</text>
      <text x="60" y="112" ${paint('fill', state === 'stale' ? 'faint' : 'ink')} font-family="sans-serif" font-size="42">042<tspan font-size="20">°</tspan></text>
      <text x="60" y="136" ${paint('fill', state === 'stale' ? 'faint' : 'muted')} font-family="sans-serif" font-size="11">${state === 'stale' ? 'Stale · last reading 09:41' : 'True north · ±3°'}</text>
      ${state === 'stale' ? `<rect x="176" y="96" width="52" height="18" ${painted({ fill: 'raised', stroke: 'line' })} ${round(6)}/><text x="202" y="109" text-anchor="middle" ${paint('fill', 'muted')} font-family="sans-serif" font-size="10">STALE</text>` : ''}${focus}`,

    compassDial:
      () => `<circle cx="160" cy="94" r="56" fill="none" ${paint('stroke', 'line')} ${strokeTone(state)} stroke-width="1.5"/>
      <circle cx="160" cy="94" r="42" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".45" stroke-width="1"/>
      <path d="M160 44l8 16h-16z" ${paint('fill', 'ink')} transform="rotate(42 160 94)"/>
      <circle cx="160" cy="94" r="5" ${paint('fill', 'focus')}/>
      ${label(60, 164, 'True north', 'muted')}${focus}`,

    targetMarker:
      () => `<circle cx="160" cy="94" r="34" fill="none" ${paint('stroke', 'ember')} stroke-opacity="${state === 'pressed' ? '.28' : '.35'}" stroke-width="1.5"/>
      <circle cx="160" cy="94" r="20" fill="none" ${paint('stroke', 'ember')} stroke-opacity="${state === 'pressed' ? '.4' : '.6'}" stroke-width="2"/>
      <circle cx="160" cy="94" r="6" ${paint('fill', 'ember')}/>
      ${label(96, 164, '110° · 68° to go', 'muted')}${focus}`,

    // The spine marks where a reader is with the identity accent and a filled
    // marker, not with gold focus: the same page draws a focus state on this
    // card, and C12 keeps the two independent. An accent also has to survive as
    // a boundary, so the ring is a stroke and the marker is a fill.
    contextSpine: () => `<g>
      <circle cx="72" cy="94" r="9" fill="${state === 'selected' ? 'var(--cosmos-identity)' : 'none'}" ${paint('stroke', state === 'selected' ? 'identity' : 'line')} stroke-width="2"/>
      <path d="M84 94h60" ${paint('stroke', state === 'selected' ? 'identity' : 'line')} ${strokeTone(state)} stroke-width="2"/>
      <circle cx="160" cy="94" r="12" fill="none" ${paint('stroke', state === 'selected' ? 'identity' : 'line')} ${strokeTone(state)} stroke-width="2.5"/>
      <path d="M172 94h48" ${paint('stroke', 'line')} stroke-width="2"/>
      <circle cx="236" cy="94" r="9" fill="none" ${paint('stroke', 'line')} stroke-width="2"/>
      ${label(60, 132, 'Context spine', 'muted')}${focus}</g>`,

    horizonRibbon:
      () => `<rect x="60" y="76" width="200" height="34" ${painted({ fill: 'void', stroke: 'line' })} ${round(17)}/>
      <path d="M60 93h200" stroke-width="2" stroke-linecap="round" stroke-dasharray="${state === 'stale' ? '2 10' : 'none'}" ${paint('stroke', state === 'stale' ? 'faint' : 'action')}/>
      <text x="72" y="66" ${paint('fill', 'muted')} font-family="sans-serif" font-size="11">${state === 'stale' ? 'Last updated 09:41' : 'In 20 minutes'}</text>${focus}`,

    watchActionCapsule:
      () => `<circle cx="160" cy="90" r="62" fill="none" ${paint('stroke', state === 'disabled' ? 'muted' : 'line')} ${strokeTone(state)} stroke-width="2"/>
      <path d="M118 124a56 56 0 0 1 84 0" fill="none" ${paint('stroke', state === 'pressed' ? 'action' : state === 'disabled' ? 'muted' : 'focus')} stroke-width="3"/>
      <text x="160" y="70" text-anchor="middle" ${paint('fill', state === 'disabled' ? 'muted' : 'text')} font-family="sans-serif" font-size="12">Target</text>
      ${state === 'pending' ? `<path d="M160 46a26 26 0 0 1 26 26" fill="none" ${paint('stroke', 'gold')} stroke-width="2.5"/>` : ''}${focus}`,
  };

  // Two states a component can declare and no shape drew: a confirmation and an
  // offline surface. Eleven components declare one or both, and every one of them
  // was rendering its confirmation or its offline state identically to its resting
  // state, which is the same fault as drawing only the first state — a card that
  // lists a treatment it cannot show. Both are drawn here rather than in seventeen
  // component bodies because neither is component-specific: a confirmation is a
  // mark on the surface, and an offline surface is dimmed with a notice beside it.
  const confirmed =
    state === 'confirmed'
      ? `<path d="M266 32l7 7 13-14" fill="none" ${paint('stroke', 'success')} stroke-width="3" stroke-linecap="round"/>`
      : '';
  const offline =
    state === 'offline'
      ? `<text x="160" y="170" text-anchor="middle" ${paint('fill', 'muted')} font-family="sans-serif" font-size="11">offline</text>`
      : '';
  return { shapes, confirmed, offline };
}

function draw(componentId, state, frameLabel) {
  const { shapes, confirmed, offline } = paintersFor(state);
  // A shape that already draws one of the gutter states gets no gutter mark, so no
  // state is ever drawn twice.
  const drawn = SELF_DRAWN[componentId] ?? [];
  const gutter = drawn.includes(state) ? '' : (GUTTER[state] ?? '');

  const body = shapes[componentId];
  // No <defs> here. Every drawing used to carry its own copy of the horizon
  // gradient and a fill="none" stage rect, and nothing ever referenced either: no
  // shape paints a gradient. Fifty-three drawings were publishing seventeen
  // kilobytes of dead markup, which is most of why a card could not afford to
  // draw the states it claims to show. A gradient belongs where it is painted, and
  // the one place that paints one (specimens.js) declares its own.
  // The internal id is not a name. componentRegistry already carries the name a
  // reader is given, and a name that leaks the registry key is the same objection
  // PART_LABELS answers for a part.
  const name = componentRegistry.find((entry) => entry.id === componentId).name;
  // A filmstrip frame passes its own name, because four frames of one picture
  // would otherwise carry one name four times.
  return `<svg class="gallery-drawing" ${STAGE} role="img" aria-label="${frameLabel ?? `${name}, ${state} state, illustrative`}">
    ${state === 'offline' ? '<g opacity=".45">' : ''}${body()}${state === 'offline' ? '</g>' : ''}
    ${confirmed}${offline}${gutter}
  </svg>`;
}

/**
 * How much of a motif one still has spent.
 *
 * The motif declares which properties it may spend and the strip spends exactly
 * those, so the four stills are the same drawing at four points along the motif
 * rather than four pictures of unrelated things. A property the motif does not
 * list is never spent, which is what makes C31 enforceable by reading motion.json.
 */
// A still publishes how far along its motif it is, and it publishes it on the
// progress bar beside the drawing — never on the drawing. Every motif in this
// file spends opacity, and every drawing in this gallery carries a label, so
// fading a frame fades that label with it: in Daylight the label is 4.64:1
// unfaded, which is under the 4.5 C05 requires only once it is dimmed by a
// fifth. There is no dim available to a drawing here, so the four stills are one
// drawing repeated and the share lives on the bar, which carries no words.
function frameMoment(progress) {
  return `--progress:${progress.toFixed(2)}`;
}

/**
 * The still filmstrip of a motion card.
 *
 * The frames are the motif's own steps, so the strip explains the choreography
 * without running it: three or four stills, each a moment of the transition the
 * motif describes. This is what the Static tier shows and what a reader with no
 * JavaScript sees, which is why the frames carry the explanation and playback
 * only repeats it.
 *
 * The strip publishes the motif's duration as a role, not as milliseconds: the
 * resolved value lives in the stylesheet as --motion-<role>, so the client reads
 * it instead of restating it here and in the script. A duration that changes in
 * tokens.json therefore changes the run, not just the label.
 */
function filmstrip(motion, component, id) {
  const name = component.name;
  const frameCount = 4;
  const frames = Array.from({ length: frameCount }, (_, index) => {
    // The progress of each still across the motif, so the strip reads left to
    // right as one transition rather than four unrelated pictures.
    const progress = index / (frameCount - 1);
    // Every frame draws the same state, and the frames differ only by how much of
    // the motif each one has spent. It used to switch the last frame to `pending`,
    // which made the strip look like four different pictures while inventing a
    // state the motif never claimed to pass through: nothing published maps a
    // motif to the states it crosses, so a strip that asserted one would be a
    // second answer. Spending the motif's own properties is not invented, and it
    // is what C31 already requires — a motif spends only the properties it lists.
    // Four frames of one picture, so four copies of one name announced the same
    // nothing four times. Each still is named by where it sits in the transition,
    // which is the difference the strip exists to show.
    return `<figure class="filmstrip-frame" aria-label="Frame ${index + 1} of ${frameCount}">
      <span class="filmstrip-progress" style="${frameMoment(progress)}" aria-hidden="true"></span>
      <div class="filmstrip-still">${draw(component.id, component.states[0], `${name}, frame ${index + 1} of ${frameCount} of the ${motion.id} motif`)}</div>
    </figure>`;
  });
  return `<div class="filmstrip" data-motif="${motion.id}" data-duration-role="${motion.duration}" role="group" aria-label="${name}: ${motion.job}">
    <div class="filmstrip-frames" data-filmstrip>${frames.join('')}</div>
    <div class="filmstrip-controls">
      <button type="button" class="button button-secondary gallery-play" data-play disabled aria-disabled="true" aria-describedby="${id}-motion-note ${id}-static-note">
        Play<span class="visually-hidden"> the ${motion.id} motif once</span>
      </button>
      <p class="gallery-status" role="status" data-play-status></p>
    </div>
    <p class="gallery-note" id="${id}-motion-note">Four stills of the same transition. Playback repeats it; the stills explain it on their own.</p>
    <p class="gallery-static-note" id="${id}-static-note" hidden>This tier settles every change at once, so the transition cannot be played here. Switch to System or Reduced to see it run.</p>
  </div>`;
}

/** One card: the drawn states, then the motif if the component has one. */
function card(component) {
  const family = familyFor(component);
  const timing = motionRolesFor(component);
  const motif = component.motion === null ? null : motionMotifs.find((m) => m.id === component.motion);
  const id = `gallery-${component.id}`;

  const rows = component.states
    .map((state) => {
      const treatment = STATE_TREATMENTS[state];
      return `<tr>
      <th scope="row"><code>${state}</code></th>
      <td>${treatment[0]}</td>
      <td>${treatment[1]}</td>
    </tr>`;
    })
    .join('');

  const rules = component.rules
    .map((rule) => `<a href="${releasePath}/${slugForRule(rule)}#${rule.toLowerCase()}"><code>${rule}</code></a>`)
    .join(' ');

  return `<article class="gallery-card" id="${component.id}" data-component="${component.id}" data-family="${family.id}">
    <header class="gallery-card-head">
      <h3>${component.name}</h3>
      <p class="gallery-card-rules">${rules} · ${component.parts.map((part) => PART_LABELS[part]).join(', ')}</p>
    </header>
    <div class="gallery-states">
      ${component.states
        .map(
          (state) => `<figure class="gallery-state">
        ${draw(component.id, state)}
        <figcaption><code>${state}</code> · illustrative</figcaption>
      </figure>`,
        )
        .join('')}
    </div>
    <div class="gallery-anatomy">
      <p class="eyebrow">ANATOMY</p>
      <ul>${component.anatomy.map((part) => `<li>${part}</li>`).join('')}</ul>
    </div>
    <div class="gallery-matrix-wrap" tabindex="0" role="region" aria-label="${component.name} state matrix">
      <table class="gallery-matrix">
        <caption>State matrix · static HTML, readable without JavaScript</caption>
        <thead><tr><th scope="col">State</th><th scope="col">Looks like</th><th scope="col">Does</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    ${motif === null ? '<p class="gallery-note">No motif: this component does not animate a state change (C31).</p>' : filmstrip(motif, component, id)}
    <footer class="gallery-card-foot">
      <p><strong>Timing</strong> ${timing === null ? 'none — it settles at once' : `${timing.duration} duration, ${timing.easing.join(' and ')} easing, ${motif.properties.join(' and ')} only`}</p>
      <p><strong>Access</strong> ${component.a11y.map((rule) => `<code>${rule}</code>`).join(' ')}</p>
      <p><strong>Native counterpart</strong> ${[component.native.web, component.native.android, component.native.wear].filter(Boolean).join(', ') || 'none — this part has no counterpart yet'}</p>
    </footer>
  </article>`;
}

/** The gallery index: one card per family, with its rule and component count. */
/**
 * The gallery index: a directory of the six families and how to read a card.
 *
 * It publishes no tier picker, and that is a decision rather than an omission.
 * The family pages are the pages that carry playback, so those are the pages
 * where the tier is a control the reader can use; this page has nothing to play.
 * The chosen tier is stored, so a reader who sets it on a family page arrives
 * here already carrying it, and a control here would only restate a choice they
 * have already made.
 */
export function galleryIndex() {
  const families = [...COMPONENT_FAMILIES]
    .sort((a, b) => a.order - b.order)
    .map((family) => {
      const members = componentsForFamily(family.id);
      return `<li class="gallery-family">
      <a href="${releasePath}/gallery/${family.id}">
        <strong>${family.label}</strong>
        <span>${members.length} ${members.length === 1 ? 'component' : 'components'} · ${family.rules.map((rule) => `<code>${rule}</code>`).join(' ')}</span>
      </a>
    </li>`;
    })
    .join('');
  return `<main id="main-content"><div class="breadcrumb"><a href="${releasePath}">Cosmos</a><span aria-hidden="true">/</span><span>Gallery</span></div>
  <section class="gallery-hero"><p class="eyebrow">COMPONENT GALLERY</p><h1>Every component, drawn from its own contract.</h1>
  <p>A card is a rendering of a registry entry: the state matrix is the rule's own wording, the timing is the motif's own duration and easing, and the colours are the published tokens. Nothing here restates a decision. Each page is static HTML, so it reads the same with scripting off and under every theme, and playback only repeats what the stills already show.</p></section>
  <ul class="gallery-families">${families}</ul>
  <section class="gallery-legend"><h2>How to read a card</h2>
  <ul>
    <li><strong>States.</strong> Each row is the visible treatment and the behaviour, written once in the rules.</li>
    <li><strong>Filmstrip.</strong> Four stills of one transition. Play runs it once; Reduced crossfades; Static leaves it at the last frame.</li>
    <li><strong>Timing.</strong> The duration, easing and properties come from the motif named on the card.</li>
    <li><strong>Native counterpart.</strong> The existing component in each product, so a port starts from what is already there.</li>
  </ul></section></main>`;
}

/** One family page: every component filed under that family. */
export function galleryFamily(familyId) {
  const family = COMPONENT_FAMILIES.find((candidate) => candidate.id === familyId);
  const members = componentsForFamily(familyId);
  return `<main id="main-content"><div class="breadcrumb"><a href="${releasePath}">Cosmos</a><span aria-hidden="true">/</span><a href="${releasePath}/gallery">Gallery</a><span aria-hidden="true">/</span><span>${family.label}</span></div>
  <section class="gallery-hero"><p class="eyebrow">COMPONENT FAMILY</p><h1>${family.label}</h1>
  <p>Governed by ${family.rules.map((rule) => `<code>${rule}</code>`).join(' and ')}. ${members.length} ${members.length === 1 ? 'component' : 'components'} in this family, each drawn from its registry entry.</p>
  <div class="gallery-tier">${motionPicker()}</div></section>
  <h2 class="visually-hidden">${family.label} components</h2>
  <div class="gallery-grid">${members.map(card).join('')}</div>
  <p class="gallery-back"><a class="text-link" href="${releasePath}/gallery">All families <span aria-hidden="true">↑</span></a></p></main>`;
}

export const GALLERY_FAMILIES = COMPONENT_FAMILIES;
