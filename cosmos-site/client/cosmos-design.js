// Optional local interactions. Reading, navigation and downloads need no JavaScript.
const form = document.querySelector('[data-demo-form]');
if (form) {
  const input = form.querySelector('[data-clearable-input]');
  const clear = form.querySelector('[data-clear-input]');
  const button = form.querySelector('[data-loading-button]');
  const status = form.querySelector('[data-demo-status]');
  let pending = false;
  let timer;
  const syncClear = () => {
    clear.hidden = !input.value || input.disabled || input.readOnly;
  };
  const idle = () => {
    clearTimeout(timer);
    pending = false;
    button.removeAttribute('aria-busy');
    button.removeAttribute('aria-disabled');
    input.disabled = false;
    syncClear();
  };
  input.addEventListener('input', syncClear);
  clear.addEventListener('click', () => {
    input.value = '';
    syncClear();
    input.focus();
    status.textContent = 'Target name cleared.';
  });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (pending || !form.reportValidity()) return;
    if (!input.value.trim()) {
      status.textContent = 'Enter a target name with at least one visible character.';
      input.focus();
      return;
    }
    pending = true;
    button.setAttribute('aria-busy', 'true');
    button.setAttribute('aria-disabled', 'true');
    input.disabled = true;
    syncClear();
    status.textContent = 'Demonstrating the pending state…';
    timer = setTimeout(() => {
      idle();
      status.textContent = 'Demo complete. Target name retained locally; no data was sent or saved.';
    }, 650);
  });
  form.querySelector('[data-reset-demo]').addEventListener('click', () => {
    idle();
    input.value = 'Riverside lookout';
    syncClear();
    input.focus();
    status.textContent = 'Demo reset. Ready to try.';
  });
  input.disabled = false;
  button.disabled = false;
  clear.disabled = false;
  form.querySelector('[data-reset-demo]').disabled = false;
  syncClear();
  window.addEventListener('pagehide', idle);
}
// The tier a reader chooses is a preference about their own reading, the same
// kind of thing as the chosen theme, so it is stored and restored like one. It has
// to be read back before anything animates, because the first thing that moves on
// a page is the disc below and C17 asks a reader who asked for still frames not to
// watch one spin while the galleries load. That is why the tier is restored by the
// inline script in the document head rather than here: a module is deferred, so
// everything between the first paint and its arrival would animate against the
// preference the reader already gave. All this does is mark the control, so it has
// to accept exactly the tiers the page offers.
const STORED_MOTION = 'simt:cosmos-motion';
const motionInputs = [...document.querySelectorAll('input[name="motion"]')];
const MOTION_TIERS = new Set(motionInputs.map((input) => input.value));
let storedMotion = null;
try {
  const saved = localStorage.getItem(STORED_MOTION);
  if (MOTION_TIERS.has(saved)) storedMotion = saved;
} catch {
  storedMotion = null;
}
if (storedMotion !== null) {
  const chosen = motionInputs.find((input) => input.value === storedMotion);
  if (chosen) chosen.checked = true;
}

// The disc revolution is the page's one ambient motion (C27, P10). It runs only
// while the art is on screen: the observer carries the scroll half, and
// visibilitychange carries the tab half. CSS cannot know a tab went to the
// background, but its animation keeps running on the compositor, and C17 asks
// that hidden screens stop animating.
const quasar = document.querySelector('.quasar');
if (quasar) {
  const pause = () => {
    quasar.dataset.ambient = 'paused';
  };
  const resume = () => {
    quasar.dataset.ambient = document.visibilityState === 'visible' ? 'running' : 'paused';
  };
  if (navigator.connection?.saveData) pause();
  else {
    new IntersectionObserver(([entry]) => (entry.isIntersecting ? resume() : pause())).observe(quasar);
    document.addEventListener('visibilitychange', resume);
  }
}
motionInputs.forEach((input) => {
  input.addEventListener('change', () => {
    document.documentElement.dataset.motion = input.value;
    try {
      localStorage.setItem(STORED_MOTION, input.value);
    } catch {
      // A reader with storage refused still gets the tier for this page.
    }
  });
  input.disabled = false;
});

// Gallery playback (C31, C17). The card explains its motif with four stills,
// and this only walks them once on request. Everything the reader needs is
// already on the page: the strip carries the frames, the tier is the same
// data-motion attribute the rest of the site uses, and the duration is read out
// of the stylesheet rather than written here, so a motif that changes in
// motion.json plays at its published timing without this file knowing it.
const strips = [...document.querySelectorAll('[data-filmstrip]')].map((frames) => {
  const strip = frames.closest('.filmstrip');
  return {
    strip,
    frames,
    parts: [...frames.querySelectorAll('.filmstrip-frame')],
    play: strip.querySelector('[data-play]'),
    status: strip.querySelector('[data-play-status]'),
    staticNote: strip.querySelector('.gallery-static-note'),
    timer: 0,
  };
});
if (strips.length) {
  const prefersStill = matchMedia('(prefers-reduced-motion: reduce)');
  const tier = () => {
    const chosen = document.documentElement.dataset.motion;
    if (chosen === 'static' || chosen === 'reduced') return chosen;
    return prefersStill.matches ? 'reduced' : 'full';
  };
  // The tier decides whether the run exists at all, so it is read per run rather
  // than bound once: the radios above can change it while the page is open.
  const playable = (strip) => {
    const level = tier();
    // Static says so with aria-disabled rather than with disabled, because a
    // disabled button is not in the tab order and the stylesheet's own reason for
    // keeping the control is that a motif which cannot run is stated by it. The
    // note it points at appears only in this tier, so the reason for the refusal
    // is on the control and not only in the stylesheet.
    strip.play.setAttribute('aria-disabled', level === 'static' ? 'true' : 'false');
    strip.play.disabled = false;
    if (strip.staticNote) strip.staticNote.hidden = level !== 'static';
    return level !== 'static';
  };
  let running = null;
  const settle = (current) => {
    if (!current) return;
    current.parts.forEach((part) => delete part.dataset.current);
    delete current.strip.dataset.playing;
    current.play.disabled = false;
    // The timer is cancelled, not overwritten. A run that was settled by a second
    // Play press, by its card leaving the screen or by the tab being hidden still
    // has a step queued, and letting it fire walks the frames of a run that is
    // supposed to have stopped — which is how two strips ended up playing at once
    // and how an orphan run cleared `running` while another was still live.
    clearTimeout(current.timer);
    current.timer = 0;
    running = null;
    playable(current);
    current.status.textContent = `Played once and returned to the stills. ${current.strip.dataset.motif} does not loop and nothing is left moving.`;
  };
  const step = (current) => {
    if (current.index >= current.parts.length) return settle(current);
    current.parts.forEach((part, at) => {
      if (at === current.index) part.dataset.current = 'true';
      else delete part.dataset.current;
    });
    current.index += 1;
    // One frame per duration step: the stills are the motif's own moments, so a
    // longer run spends longer on each of them rather than reaching the last one
    // sooner and then waiting. The run ends one interval after the last still,
    // which is what leaves the final frame on screen long enough to read.
    current.timer = setTimeout(() => step(current), current.stepMs);
  };
  const start = (current) => {
    if (running && running !== current) settle(running);
    if (!playable(current)) {
      current.status.textContent =
        'The Static tier applies, so this motif does not run. The stills above are its steps.';
      return;
    }
    running = current;
    current.index = 0;
    clearTimeout(current.timer);
    current.timer = 0;
    current.stepMs = Math.max(
      60,
      Number.parseFloat(
        getComputedStyle(document.documentElement).getPropertyValue(`--motion-${current.strip.dataset.durationRole}`),
      ) / current.parts.length || 75,
    );
    current.strip.dataset.playing = 'true';
    current.status.textContent = `Playing ${current.strip.dataset.motif} once, ${tier() === 'reduced' ? 'as a crossfade' : 'in full'}.`;
    step(current);
  };
  // The tier can change while the page is open, and whether a control is
  // available is a property of the tier, so a change to it re-reads every
  // control rather than leaving a Static page with a live Play.
  motionInputs.forEach((input) => {
    input.addEventListener('change', () => {
      for (const current of strips) playable(current);
    });
  });
  for (const current of strips) {
    playable(current);
    current.play.addEventListener('click', () => {
      // A second press while a run is going restarts it rather than queueing, so
      // the control never leaves a reader waiting on something they cannot see.
      if (running === current) return settle(current);
      start(current);
    });
  }
  // Nothing plays on its own, and nothing plays on a screen nobody is looking
  // at: the observer holds the run at its first frame while the strip is off
  // screen, which is the same rule the ambient disc follows.
  const onScreen = new IntersectionObserver(
    ([entry]) => {
      const current = strips.find((candidate) => candidate.strip === entry.target);
      // A run is one pass of four stills, not a loop, so a card that leaves the
      // screen ends its run and falls back to the resting strip rather than
      // resuming later where the reader cannot see what moved.
      if (current && !entry.isIntersecting && running === current) settle(current);
    },
    { threshold: 0.2 },
  );
  for (const current of strips) onScreen.observe(current.strip);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible' && running) settle(running);
  });
}

// Theme profile (C06). The theme itself is CSS, so this control only records a
// choice: the attribute it writes is what the token layer already keys on, and
// with no choice the media query applies. Choosing the value the system already
// asks for clears the stored value, so a visitor who returns the site to "what my
// system wants" stops carrying an explicit answer.
// Read from the picker the page already publishes, because the profile list has
// one owner -- themeProfiles in the token file -- and a literal here is a second
// copy that stops recognising a profile the picker offers. An empty set means
// there is no picker on this page, and an unknown stored value is then discarded
// rather than written onto the document.
const THEMES = new Set(
  [...document.querySelectorAll('[data-theme-select] option[value]')].map((option) => option.value).filter(Boolean),
);
const STORED_THEME = 'simt:cosmos-theme';
const themeSystem = () => (matchMedia('(prefers-color-scheme: light)').matches ? 'daylight' : 'deepSpace');
const themeMeta = [...document.querySelectorAll('meta[name="theme-color"]')];
// The chrome a profile paints with is published on the picker's own options, so
// the browser agrees with the page about which colour is behind it. It used to
// reset both metas and then select the light one, which left a browser in light
// mode showing a cream toolbar above a page painted #0A0000 whenever the profile
// was one of the two that are dark without being the default.
const themeColors = new Map(
  [...document.querySelectorAll('[data-theme-select] option[value]')].map((option) => [option.value, option.dataset.color]),
);
const syncThemeColor = (theme) => {
  for (const meta of themeMeta) meta.content = meta.dataset.color ?? meta.content;
  const color = themeColors.get(theme);
  if (color && themeMeta[0]) themeMeta[0].content = color;
};
const themeSelect = document.querySelector('[data-theme-select]');
if (themeSelect) {
  const stored = () => {
    try {
      return localStorage.getItem(STORED_THEME);
    } catch {
      return null;
    }
  };
  const store = (value) => {
    try {
      value ? localStorage.setItem(STORED_THEME, value) : localStorage.removeItem(STORED_THEME);
    } catch {
      /* Private mode: the choice lasts for this page only. */
    }
  };
  const apply = (theme) => {
    if (theme) document.documentElement.dataset.cosmosTheme = theme;
    else delete document.documentElement.dataset.cosmosTheme;
    themeSelect.value = theme ?? '';
    syncThemeColor(theme ?? themeSystem());
  };
  // Reconcile the stored value on load. An unknown value is discarded, and so is
  // a known one the system already asks for: that visitor is no longer expressing
  // a preference, so the attribute comes off and the media query owns the page
  // again, mid-session system changes included. Both cases end on the colour the
  // page already has, so reconciling cannot flash.
  const initial = stored();
  const choice = THEMES.has(initial) ? initial : null;
  if (initial && (choice !== initial || choice === themeSystem())) {
    store(null);
    delete document.documentElement.dataset.cosmosTheme;
  }
  themeSelect.value = choice && choice !== themeSystem() ? choice : '';
  syncThemeColor(choice ?? themeSystem());
  themeSelect.addEventListener('change', () => {
    const next = themeSelect.value;
    store(next && next !== themeSystem() ? next : null);
    apply(next);
  });
  // Follow a live system change while the visitor has expressed no preference,
  // so the browser chrome and the page cannot disagree.
  matchMedia('(prefers-color-scheme: light)').addEventListener('change', (event) => {
    if (!document.documentElement.dataset.cosmosTheme) syncThemeColor(event.matches ? 'daylight' : 'deepSpace');
  });
}
