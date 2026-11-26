// Static, accessible design illustrations. Fictional records are labeled by the surrounding figure.
// Every gradient below is drawn from a published token so the specimens obey C27 themselves.

export const MOTION_TIERS = [
  { value: 'system', label: 'System' },
  { value: 'reduced', label: 'Reduced' },
  { value: 'static', label: 'Static' },
];

export function motionPicker() {
  const options = MOTION_TIERS.map(
    (tier, index) =>
      `<label><input type="radio" name="motion" value="${tier.value}"${index === 0 ? ' checked' : ''} disabled> ${tier.label}</label>`,
  ).join('');
  return `<fieldset class="mode-demo"><legend>Motion preference</legend>${options}</fieldset>`;
}

export function workbench() {
  return `<section class="workbench" id="workbench" aria-labelledby="workbench-title"><div><p class="eyebrow">LIVE COMPONENT STUDY</p><h2 id="workbench-title">Feel the difference.</h2><p>Try the focus, field, selection and action states. This is a local demonstration; no data is sent.</p></div>
  <div class="workbench-controls"><form data-demo-form><label for="specimen-name">Target name</label><div class="clear-field"><input id="specimen-name" value="Riverside lookout" required maxlength="60" aria-describedby="target-hint" data-clearable-input disabled><button type="button" data-clear-input aria-label="Clear target name" hidden disabled>×</button></div><p class="field-hint" id="target-hint">Keep the label useful at a glance.</p><div class="action-row"><button class="button button-primary" type="submit" data-loading-button disabled>Save target</button><button class="button button-secondary" type="button" data-reset-demo disabled>Reset</button></div><p class="demo-status" role="status" data-demo-status>Ready to try.</p><noscript><p>Interactive examples need JavaScript. The complete specification is available above and as plain text.</p></noscript></form>
  ${motionPicker()}</div></section>`;
}

export function createSpecimens(spec) {
  const { gradients, paintVariable, tokens } = spec.tokens;
  const { starfieldSVG } = spec.starfield;
  const byName = Object.fromEntries(gradients.map((g) => [g.name, g]));
  // See paintVariable: a name nothing publishes is a build failure, not a black shape.
  const variable = paintVariable;
// A painting instruction rather than a baked hex. `var()` is not resolved inside a
// presentation attribute, so the declaration goes in an inline style. Every specimen
// here is inlined into the themed page and so resolves through this: baking the color
// stopped it tracking the profile the moment the profiles became themeable, which left
// the compass reading at 1.1:1 and the orbit labels at 2.0:1 under Daylight (C05).
const paint = (property, name) => `style="${property}:var(${variable(name, property)})"`;
// One attribute per element. Two paint() calls on one shape mean two style=
// attributes, and a browser keeps the first and discards the second without a
// word — so a stroke declared next to a fill simply never painted.
const painted = (entries) =>
  `style="${Object.entries(entries)
    .map(([property, name]) => `${property}:var(${variable(name, property)})`)
    .join(';')}"`;
const stops = (name) =>
  byName[name].stops
    .map(
      ([color, alpha, position]) =>
        `<stop offset="${position}" ${paint('stop-color', color)}${alpha === 1 ? '' : ` stop-opacity="${alpha}"`}/>`,
    )
    .join('');
const radial = (id, name) => `<radialGradient id="${id}">${stops(name)}</radialGradient>`;
const linear = (id, name) => `<linearGradient id="${id}">${stops(name)}</linearGradient>`;
// User-space geometry: a bounding-box gradient paints nothing on a perfectly straight line.
const along = (id, name, x1, y1, x2, y2) =>
  `<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}">${stops(name)}</linearGradient>`;

// Heading 042° and target 110°: the horizon arc between them is the 68° turn, not ornament.
const HEADING = 42;
const TARGET = 110;
const DIAL_R = 128;
const onDial = (deg) =>
  [160 + DIAL_R * Math.sin((deg * Math.PI) / 180), 160 - DIAL_R * Math.cos((deg * Math.PI) / 180)].map((n) =>
    Number(n.toFixed(1)),
  );
// An untransformed path: a user-space gradient inherits the referencing element's transform.
const turn = `M${onDial(HEADING).join(' ')}A${DIAL_R} ${DIAL_R} 0 0 1 ${onDial(TARGET).join(' ')}`;

// The focal target is a quasar aperture: corona, a precision ring and a solid core.
// On a watch the title and the action own the top and bottom of the round face,
// so the dial gives up its north/south ticks and the south label there.
const compass = (id, small = false) => `<svg class="compass-dial" viewBox="0 0 320 320" aria-hidden="true">
  <defs>${radial(`${id}-corona`, 'corona')}${along(`${id}-turn`, 'horizon', ...onDial(HEADING - 8), ...onDial(TARGET + 8))}</defs>
  <circle cx="160" cy="160" r="140" fill="none" stroke="currentColor" stroke-opacity=".2"/>
  <path d="${turn}" fill="none" stroke="url(#${id}-turn)" stroke-width="2.5" stroke-linecap="round"/>
  <circle cx="160" cy="160" r="115" fill="none" stroke="currentColor" stroke-opacity=".14" stroke-dasharray="1 10"/>
  <path d="${small ? '' : 'M160 20v14M160 300v-14'}M300 160h-14M20 160h14" ${paint('stroke', 'gold')} stroke-width="2"/>
  <g fill="currentColor" font-size="${small ? 19 : 14}" text-anchor="middle"><text x="160" y="58">N</text><text x="266" y="166">E</text>${small ? '' : '<text x="160" y="273">S</text>'}<text x="54" y="166">W</text></g>
  <path d="M160 37l7 14h-14z" ${paint('fill', 'ink')} transform="rotate(${HEADING} 160 160)"/>
  <g transform="rotate(${TARGET} 160 160)"><circle cx="160" cy="58" r="${small ? 30 : 34}" fill="url(#${id}-corona)" opacity=".75"/><circle cx="160" cy="58" r="9" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".8"/><circle cx="160" cy="58" r="4" ${paint('fill', 'gold')}/></g>
  <circle cx="160" cy="160" r="81" ${painted({ fill: 'void', stroke: 'ink' })} stroke-opacity=".1"/>
</svg>`;

const orbit = `<div class="quasar"><svg class="orbit-art" viewBox="0 0 600 600" aria-hidden="true">
  <defs>${radial('q-corona', 'corona')}${radial('q-nebula', 'nebula')}${linear('q-horizon', 'horizon')}${along('q-jet', 'horizon', 300, 96, 300, 504)}</defs>
  <circle cx="300" cy="300" r="300" fill="url(#q-nebula)"/>
  <g fill="none" ${paint('stroke', 'muted')} stroke-opacity=".16"><circle cx="300" cy="300" r="240"/><circle cx="300" cy="300" r="180" stroke-dasharray="2 6"/><circle cx="300" cy="300" r="116"/><path d="M40 300h520" stroke-dasharray="2 10"/></g>
  <circle cx="300" cy="300" r="230" fill="url(#q-corona)" opacity=".42"/>
  <g class="orbit-disc" fill="none">
    <path d="M300 96V504" stroke="url(#q-jet)" opacity=".55"/>
    <ellipse cx="300" cy="300" rx="206" ry="46" stroke="url(#q-horizon)" stroke-width="1.5"/>
    <ellipse cx="300" cy="300" rx="158" ry="33" stroke="url(#q-horizon)" stroke-opacity=".6"/>
    <ellipse cx="300" cy="300" rx="112" ry="22" stroke="url(#q-horizon)" stroke-opacity=".35"/>
  </g>
  <path d="M91 182A240 240 0 0 1 510 184" fill="none" ${paint('stroke', 'ember')} stroke-width="2"/>
  <path d="M412 160A180 180 0 0 1 423 431" fill="none" ${paint('stroke', 'violet')} stroke-width="2"/>
  <circle cx="300" cy="300" r="54" ${painted({ fill: 'hull', stroke: 'ember' })} stroke-opacity=".5"/>
  <circle cx="300" cy="300" r="14" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".6"/>
  <circle cx="300" cy="300" r="6" ${paint('fill', 'ink')}/>
  <g ${paint('fill', 'cyan')}><circle cx="510" cy="184" r="5"/><circle cx="510" cy="184" r="12" fill="none" ${paint('stroke', 'cyan')} stroke-opacity=".45"/></g>
  <circle cx="423" cy="431" r="5" ${paint('fill', 'violet')}/><circle cx="122" cy="326" r="4" ${paint('fill', 'rose')}/>
  <g ${paint('fill', 'muted')} font-family="sans-serif" font-size="11" letter-spacing="1"><text x="300" y="37" text-anchor="middle">YOU ARE THE ORIGIN</text><text x="300" y="578" text-anchor="middle">COMMON CORE / FOUR EXPRESSIONS</text><text x="347" y="262">PERSON</text><text x="433" y="461">CONTEXT</text><text x="485" y="151">ACTION</text></g>
</svg><span class="accretion" aria-hidden="true"></span></div>`;

const atlas = (id) => `<svg class="atlas-map" viewBox="0 0 640 390" aria-hidden="true">
  <defs>${radial(`${id}-corona`, 'corona')}</defs>
  <rect width="640" height="390" ${paint('fill', 'petrol')}/>
  <path d="M0 280C70 240 100 320 190 268S278 116 370 147s124 27 270-60v98c-97 52-132 10-223 41s-132 133-228 117S91 315 0 352z" ${paint('fill', 'water')}/>
  <g fill="none" ${paint('stroke', 'moss')} stroke-opacity=".2"><path d="M-10 89q180-62 310 21t350-64M-10 122q180-62 310 21t350-64M-10 155q180-62 310 21t350-64M-10 188q180-62 310 21t350-64M-10 221q180-62 310 21t350-64"/></g>
  <g fill="none" ${paint('stroke', 'ink')} stroke-opacity=".14"><path d="M64 0l95 390M285 0l-75 390M422 0l-74 390M566 0l-64 390M0 99l640 80M0 236l640 31"/></g>
  <path d="M162 166l165 79 164-123" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".45" stroke-dasharray="3 6"/>
  <circle cx="327" cy="245" r="74" fill="url(#${id}-corona)" opacity=".7"/>
  <g stroke-width="2"><circle cx="162" cy="166" r="15" ${painted({ fill: 'hull', stroke: 'moss' })}/><circle cx="162" cy="166" r="4" ${paint('fill', 'moss')}/><circle cx="327" cy="245" r="18" ${painted({ fill: 'hull', stroke: 'ember' })}/><circle cx="327" cy="245" r="25" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".55" stroke-width="1"/><circle cx="327" cy="245" r="5" ${paint('fill', 'ember')}/><circle cx="491" cy="122" r="13" ${painted({ fill: 'hull', stroke: 'violet' })}/><circle cx="491" cy="122" r="4" ${paint('fill', 'violet')}/></g>
  <g ${paint('fill', 'ink')} font-family="sans-serif" font-size="13"><text x="185" y="161">Trail walk</text><text x="360" y="245">Sunset together</text><text x="511" y="116">Sketch club</text></g>
</svg>`;

function common() {
  return `<div class="foundation-preview"><div class="preview-kicker">THE COSMOS GRAMMAR</div>
  <div class="grammar-row"><span class="grammar-symbol aperture"><i></i></span><div><strong>Quasar aperture</strong><p>Frame one meaningful object.</p></div><span class="grammar-token">01</span></div>
  <div class="grammar-row"><span class="grammar-symbol spine"></span><div><strong>Context spine</strong><p>Where you are. What comes next.</p></div><span class="grammar-token">02</span></div>
  <div class="grammar-row"><span class="grammar-symbol horizon"></span><div><strong>Horizon ribbon</strong><p>Time and distance, made legible.</p></div><span class="grammar-token">03</span></div>
  <div class="color-spectrum" role="img" aria-label="Ember, Rose, Violet, Gold, Cyan and Moss"></div></div>`;
}

function webPage() {
  return `<div class="landing-preview"><div class="mini-header"><span class="mini-brand">SIMT Social</span><span>Places · People · Possibilities</span></div>
  <div class="landing-preview-grid"><div><p class="preview-kicker">YOUR NEXT GOOD MOMENT</p><p class="mini-display">Go somewhere.<br><em>Find your people.</em></p><p class="mini-copy">A walk, a shared sunset, an idea.<br>Real moments start nearby.</p><span class="visual-action">Explore nearby <span aria-hidden="true">↗</span></span></div>
  <div class="landing-atlas">${atlas('wp')}<div class="atlas-label"><span>18:30</span><strong>Sunset together</strong><small>Riverside · Outdoors</small></div></div></div>
  <div class="mini-horizon"><span>Discover</span><span>Find a moment</span><span>Make it yours</span></div></div>`;
}

function webApp() {
  return `<div class="app-preview"><div class="mini-rail"><span class="mini-brand">S.</span><span class="rail-selected">◎ <span>Discover</span></span><span>◌ <span>Inbox</span></span><span>◇ <span>Saved</span></span><span>○ <span>Profile</span></span></div>
  <div class="app-field"><div class="mini-context"><strong>Good moments nearby</strong><span>Today · 3 activities</span></div><div class="app-map">${atlas('wa')}<span class="map-mode">Map <span>List</span></span><div class="map-caption">Illustrative atlas</div></div>
  <div class="activity-row"><span class="activity-dot"></span><span><strong>Sunset together</strong><small>18:30 · Riverside · 4 places available</small></span><span class="record-arrow" aria-hidden="true">↗</span></div></div>
  <div class="app-context"><span class="preview-kicker">SELECTED MOMENT</span><span class="detail-art" aria-hidden="true"></span><p class="app-title">Sunset together</p><p>Today, 18:30<br>Riverside · Approximate place</p><div class="detail-fact"><span>Participation</span><strong>Approval required</strong></div><span class="visual-action">Request to join</span><small>Exact place is shared after approval.</small></div></div>`;
}

// The phone screen owns its sky; header and command zones are exclusion zones.
const phoneSky = starfieldSVG('specimen.mobile', 232, 520, {
  exclude: [
    [0, 0, 1, 0.3],
    [0, 0.78, 1, 1],
  ],
  className: 'phone-sky',
  themed: true,
});

function mobile() {
  return `<div class="mobile-preview"><div class="phone">${phoneSky}<div class="phone-status"><span>9:41</span><span aria-hidden="true">▰</span></div><div class="phone-header"><span>‹</span><strong>Compass</strong><span>⋮</span></div><div class="phone-target"><span class="preview-kicker">YOUR TARGET</span><strong>Riverside lookout</strong><span class="live-label">● Location locked</span></div>
  <div class="phone-instrument">${compass('mc')}<div class="instrument-reading"><strong>042<span>°</span></strong><span>Heading · True north</span></div></div>
  <div class="phone-metrics"><div><span>Target bearing</span><strong>110°</strong></div><div><span>Turn clockwise</span><strong>68°</strong></div></div>
  <div class="phone-command"><span class="visual-action">Open target</span><span>Calibrate · Accuracy ±3°</span></div><div class="phone-home" aria-hidden="true"></div></div>
  <div class="mobile-notes"><span class="preview-kicker">THE INSTRUMENT FIELD</span><p><strong>01</strong> A stable context</p><p><strong>02</strong> One clear reading</p><p><strong>03</strong> Commands within reach</p><div class="signal-caption">The target glows.<br>The sky stays at the edge.</div></div></div>`;
}

function watch() {
  return `<div class="watch-preview"><div class="watch-pair"><div class="watch-device"><div class="watch-face"><span class="watch-title">Compass</span><div class="watch-instrument">${compass('wc', true)}<div class="watch-reading"><strong>042°</strong><span>True north</span><span class="watch-qualifier">±3° · Located</span></div></div><span class="watch-action">Target</span></div></div>
  <div class="watch-device ambient-device"><div class="watch-face ambient-face"><i class="ambient-plate" aria-hidden="true"></i><span class="watch-title">Compass</span><strong class="ambient-reading">042°</strong><span class="ambient-qualifier">Last reading 09:41</span><span class="ambient-tick" aria-hidden="true"></span></div></div></div>
  <div class="watch-mode-labels"><span>Interactive · one useful answer</span><span>Ambient · one quiet reading</span></div></div>`;
}

function specimen(kind) {
  return { common, 'web-page': webPage, 'web-app': webApp, 'mobile-app': mobile, 'watch-app': watch }[kind]();
}

// A live index of the light grammar: each sample paints its own token.
function lightGrammar() {
  return `<ul class="light-grammar" aria-label="Cosmos gradient tokens">${gradients
    .map(
      (g) =>
        `<li data-light="${g.name}" data-copy-token="var(--gradient-${g.name})" tabindex="0" role="button" aria-label="Copy --gradient-${g.name}: ${g.job}"><span class="light-sample" aria-hidden="true"><i></i></span><div class="light-meta"><strong>${g.name}</strong><code>--gradient-${g.name}</code></div><p>${g.job}</p></li>`,
    )
    .join('')}</ul>`;
}

function observatory() {
  return `<section class="observatory" id="observatory" aria-labelledby="observatory-title" data-observatory>
  <div class="observatory-head">
    <div>
      <p class="eyebrow">INTERACTIVE OBSERVATORY</p>
      <h2 id="observatory-title">Test the system live.</h2>
      <p>Switch real theme profiles, flip logical RTL direction while true north holds steady (C20), scrub the orbital bearing instrument, and audition Cosmos geometry and motion tiers in place.</p>
    </div>
    <div class="observatory-dock" role="group" aria-label="Live environment controls">
      <div class="dock-group">
        <span class="dock-label">Theme profile</span>
        <div class="pill-row" data-lab-themes>
          <button type="button" class="pill" data-set-theme="deepSpace" aria-pressed="false"><i class="swatch-dot swatch-deepSpace" aria-hidden="true"></i>Deep Space</button>
          <button type="button" class="pill" data-set-theme="daylight" aria-pressed="false"><i class="swatch-dot swatch-daylight" aria-hidden="true"></i>Daylight</button>
          <button type="button" class="pill" data-set-theme="astronomy" aria-pressed="false"><i class="swatch-dot swatch-astronomy" aria-hidden="true"></i>Astronomy</button>
          <button type="button" class="pill" data-set-theme="watchAmbient" aria-pressed="false"><i class="swatch-dot swatch-watchAmbient" aria-hidden="true"></i>Ambient</button>
        </div>
      </div>
      <div class="dock-group">
        <span class="dock-label">Motion tier</span>
        <div class="pill-row" data-lab-motion>
          ${MOTION_TIERS.map((t) => `<button type="button" class="pill" data-set-motion="${t.value}" aria-pressed="false">${t.label}</button>`).join('')}
        </div>
      </div>
      <div class="dock-group">
        <span class="dock-label">Script direction</span>
        <div class="pill-row" data-lab-dir>
          <button type="button" class="pill" data-set-dir="ltr" aria-pressed="true">LTR</button>
          <button type="button" class="pill" data-set-dir="rtl" aria-pressed="false">RTL</button>
        </div>
      </div>
    </div>
  </div>
  <div class="observatory-grid">
    <article class="lab-card lab-instrument" aria-labelledby="lab-instrument-title">
      <header class="lab-card-head">
        <div>
          <span class="preview-kicker">INSTRUMENT · C08 / C20</span>
          <h3 id="lab-instrument-title">True-north bearing dial</h3>
        </div>
        <span class="lab-badge" data-lab-turn>Turn 68° CW</span>
      </header>
      <div class="lab-dial-stage" dir="ltr">
        <svg class="lab-dial" viewBox="0 0 320 320" aria-hidden="true" data-lab-dial>
          <defs>${radial('lab-corona', 'corona')}${along('lab-turn', 'horizon', ...onDial(HEADING - 8), ...onDial(TARGET + 8))}</defs>
          <circle cx="160" cy="160" r="140" fill="none" stroke="currentColor" stroke-opacity=".2"/>
          <path data-lab-arc d="${turn}" fill="none" stroke="url(#lab-turn)" stroke-width="3" stroke-linecap="round"/>
          <circle cx="160" cy="160" r="115" fill="none" stroke="currentColor" stroke-opacity=".14" stroke-dasharray="1 10"/>
          <path d="M160 20v14M160 300v-14M300 160h-14M20 160h14" ${paint('stroke', 'gold')} stroke-width="2"/>
          <g fill="currentColor" font-size="14" text-anchor="middle"><text x="160" y="58">N</text><text x="266" y="166">E</text><text x="160" y="273">S</text><text x="54" y="166">W</text></g>
          <g data-lab-needle transform="rotate(${HEADING} 160 160)"><path d="M160 35l8 16h-16z" ${paint('fill', 'ink')}/></g>
          <g data-lab-target transform="rotate(${TARGET} 160 160)"><circle cx="160" cy="58" r="34" fill="url(#lab-corona)" opacity=".75"/><circle cx="160" cy="58" r="9" fill="none" ${paint('stroke', 'gold')} stroke-opacity=".85"/><circle cx="160" cy="58" r="4" ${paint('fill', 'gold')}/></g>
          <circle cx="160" cy="160" r="78" ${painted({ fill: 'void', stroke: 'ink' })} stroke-opacity=".12"/>
        </svg>
        <div class="lab-dial-readout">
          <strong data-lab-heading-val>042°</strong>
          <span>Target <b data-lab-target-val>110°</b></span>
        </div>
      </div>
      <div class="lab-controls">
        <label class="lab-range">
          <span>Heading <code data-lab-heading-code>042°</code></span>
          <input type="range" min="0" max="359" value="${HEADING}" data-lab-heading aria-label="Compass heading in degrees">
        </label>
        <div class="pill-row lab-presets" role="group" aria-label="Target bearing presets">
          <button type="button" class="pill pill-sm" data-lab-target-set="42">042°</button>
          <button type="button" class="pill pill-sm" data-lab-target-set="110" aria-pressed="true">110°</button>
          <button type="button" class="pill pill-sm" data-lab-target-set="248">248°</button>
          <button type="button" class="pill pill-sm" data-lab-target-set="315">315°</button>
        </div>
      </div>
    </article>
    <article class="lab-card lab-surface-studio" aria-labelledby="lab-surface-title">
      <header class="lab-card-head">
        <div>
          <span class="preview-kicker">SURFACE &amp; MOTION · C09 / C27 / C31</span>
          <h3 id="lab-surface-title">Geometry &amp; choreography</h3>
        </div>
        <button type="button" class="pill pill-sm" data-lab-copy-vars>Copy CSS</button>
      </header>
      <div class="lab-stage" data-lab-stage data-radius="record" data-density="standard">
        <div class="lab-specimen-card" data-lab-card>
          <div class="lab-specimen-top">
            <span class="preview-kicker">LIVE COSMOS RECORD</span>
            <span class="lab-signal-dot" aria-hidden="true"></span>
          </div>
          <h4>Riverside lookout</h4>
          <p>Approximate place · 18:30 · 4 places left</p>
          <div class="lab-token-chips">
            <span data-lab-radius-readout>--radius-record</span>
            <span data-lab-space-readout>--space-24</span>
          </div>
          <div class="lab-specimen-actions">
            <button type="button" class="button button-primary lab-pulse-btn" data-lab-trigger="state">Audition motion</button>
            <span class="lab-timing-tag" data-lab-timing-readout>300ms · emphasis</span>
          </div>
        </div>
      </div>
      <div class="lab-controls">
        <div class="dock-group">
          <span class="dock-label">Corner role</span>
          <div class="pill-row">
            <button type="button" class="pill pill-sm" data-lab-radius="control" aria-pressed="false">Control · ${tokens.radius.control.$value.value}</button>
            <button type="button" class="pill pill-sm" data-lab-radius="record" aria-pressed="true">Record · ${tokens.radius.record.$value.value}</button>
            <button type="button" class="pill pill-sm" data-lab-radius="overlay" aria-pressed="false">Overlay · ${tokens.radius.overlay.$value.value}</button>
          </div>
        </div>
        <div class="dock-group">
          <span class="dock-label">Surface rhythm</span>
          <div class="pill-row">
            <button type="button" class="pill pill-sm" data-lab-density="compact" aria-pressed="false">Compact · 16</button>
            <button type="button" class="pill pill-sm" data-lab-density="standard" aria-pressed="true">Standard · 24</button>
            <button type="button" class="pill pill-sm" data-lab-density="generous" aria-pressed="false">Generous · 32</button>
          </div>
        </div>
        <div class="dock-group">
          <span class="dock-label">Motif duration</span>
          <div class="pill-row">
            <button type="button" class="pill pill-sm" data-lab-motif="feedback" aria-pressed="false">150ms feedback</button>
            <button type="button" class="pill pill-sm" data-lab-motif="state" aria-pressed="true">300ms state</button>
            <button type="button" class="pill pill-sm" data-lab-motif="context" aria-pressed="false">500ms context</button>
          </div>
        </div>
      </div>
    </article>
  </div>
</section>`;
}

return { MOTION_TIERS, orbit, specimen, lightGrammar, observatory, motionPicker, workbench };
}
