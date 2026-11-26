// Optional progressive enhancements. Reading, navigation and downloads need no JavaScript.
const rootEl = document.documentElement;
const toastEl = document.querySelector('[data-cosmos-toast]');
let toastTimer = 0;
const notify = (msg) => {
  if (!toastEl) return;
  toastEl.textContent = msg;
  toastEl.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastEl.hidden = true;
  }, 2400);
};
const copyText = (value, label = value) => {
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(value).then(
      () => notify(`Copied ${label}`),
      () => notify(label),
    );
  } else {
    notify(label);
  }
};

// Live component workbench demo form
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

// Motion tier preference (C17) shared across workbench, gallery and observatory
const STORED_MOTION = 'simt:cosmos-motion';
const motionInputs = [...document.querySelectorAll('input[name="motion"]')];
const motionPills = [...document.querySelectorAll('[data-set-motion]')];
const MOTION_TIERS = new Set([
  ...motionInputs.map((i) => i.value),
  ...motionPills.map((b) => b.dataset.setMotion),
]);
const motionListeners = [];
const syncMotionUI = (tier) => {
  rootEl.dataset.motion = tier;
  motionInputs.forEach((input) => {
    input.checked = input.value === tier;
  });
  motionPills.forEach((btn) => {
    btn.setAttribute('aria-pressed', String(btn.dataset.setMotion === tier));
  });
  motionListeners.forEach((fn) => fn(tier));
};
const setMotion = (tier) => {
  if (!MOTION_TIERS.has(tier)) return;
  syncMotionUI(tier);
  try {
    localStorage.setItem(STORED_MOTION, tier);
  } catch {}
};
let initialMotion = rootEl.dataset.motion || 'system';
try {
  const saved = localStorage.getItem(STORED_MOTION);
  if (MOTION_TIERS.has(saved)) initialMotion = saved;
} catch {}
if (MOTION_TIERS.size) syncMotionUI(initialMotion);
motionInputs.forEach((input) => {
  input.addEventListener('change', () => setMotion(input.value));
  input.disabled = false;
});
motionPills.forEach((btn) => {
  btn.addEventListener('click', () => {
    setMotion(btn.dataset.setMotion);
    notify(`Motion tier: ${btn.textContent.trim()}`);
  });
});

// Ambient quasar disc pauses out of view and on hidden tabs (C27, P10)
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

// Gallery one-shot filmstrip playback (C31, C17)
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
    const chosen = rootEl.dataset.motion;
    if (chosen === 'static' || chosen === 'reduced') return chosen;
    return prefersStill.matches ? 'reduced' : 'full';
  };
  const playable = (strip) => {
    const level = tier();
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
    current.timer = setTimeout(() => step(current), current.stepMs);
  };
  const start = (current) => {
    if (running && running !== current) settle(running);
    if (!playable(current)) {
      current.status.textContent = 'The Static tier applies, so this motif does not run. The stills above are its steps.';
      return;
    }
    running = current;
    current.index = 0;
    clearTimeout(current.timer);
    current.timer = 0;
    current.stepMs = Math.max(
      60,
      Number.parseFloat(getComputedStyle(rootEl).getPropertyValue(`--motion-${current.strip.dataset.durationRole}`)) /
        current.parts.length || 75,
    );
    current.strip.dataset.playing = 'true';
    current.status.textContent = `Playing ${current.strip.dataset.motif} once, ${tier() === 'reduced' ? 'as a crossfade' : 'in full'}.`;
    step(current);
  };
  motionListeners.push(() => {
    for (const current of strips) playable(current);
  });
  for (const current of strips) {
    playable(current);
    current.play.addEventListener('click', () => (running === current ? settle(current) : start(current)));
  }
  const onScreen = new IntersectionObserver(
    ([entry]) => {
      const current = strips.find((c) => c.strip === entry.target);
      if (current && !entry.isIntersecting && running === current) settle(current);
    },
    { threshold: 0.2 },
  );
  for (const current of strips) onScreen.observe(current.strip);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible' && running) settle(running);
  });
}

// Theme profile selector (C06) synced across header select and observatory pills
const themeSelect = document.querySelector('[data-theme-select]');
const themePills = [...document.querySelectorAll('[data-set-theme]')];
const THEMES = new Set(
  [...document.querySelectorAll('[data-theme-select] option[value]')].map((o) => o.value).filter(Boolean),
);
const STORED_THEME = 'simt:cosmos-theme';
const themeSystem = () => (matchMedia('(prefers-color-scheme: light)').matches ? 'daylight' : 'deepSpace');
const themeMeta = [...document.querySelectorAll('meta[name="theme-color"]')];
const themeColors = new Map(
  [...document.querySelectorAll('[data-theme-select] option[value]')].map((o) => [o.value, o.dataset.color]),
);
const syncThemeColor = (theme) => {
  for (const meta of themeMeta) meta.content = meta.dataset.color ?? meta.content;
  const color = themeColors.get(theme);
  if (color && themeMeta[0]) themeMeta[0].content = color;
};
const syncThemePills = (active) => {
  themePills.forEach((btn) => {
    btn.setAttribute('aria-pressed', String(btn.dataset.setTheme === active));
  });
};
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
    } catch {}
  };
  const apply = (theme) => {
    if (theme) rootEl.dataset.cosmosTheme = theme;
    else delete rootEl.dataset.cosmosTheme;
    themeSelect.value = theme ?? '';
    const resolved = theme || themeSystem();
    syncThemeColor(resolved);
    syncThemePills(resolved);
  };
  const initial = stored();
  const choice = THEMES.has(initial) ? initial : null;
  if (initial && (choice !== initial || choice === themeSystem())) {
    store(null);
    delete rootEl.dataset.cosmosTheme;
  }
  const activeInitial = choice && choice !== themeSystem() ? choice : '';
  themeSelect.value = activeInitial;
  syncThemeColor(activeInitial || themeSystem());
  syncThemePills(activeInitial || themeSystem());
  themeSelect.addEventListener('change', () => {
    const next = themeSelect.value;
    store(next && next !== themeSystem() ? next : null);
    apply(next);
  });
  themePills.forEach((btn) => {
    btn.addEventListener('click', () => {
      const next = btn.dataset.setTheme;
      store(next === themeSystem() ? null : next);
      apply(next === themeSystem() ? '' : next);
      notify(`Theme profile: ${btn.textContent.trim()}`);
    });
  });
  matchMedia('(prefers-color-scheme: light)').addEventListener('change', (event) => {
    if (!rootEl.dataset.cosmosTheme) {
      const sys = event.matches ? 'daylight' : 'deepSpace';
      syncThemeColor(sys);
      syncThemePills(sys);
    }
  });
}

// Specification version switcher
const versionSelect = document.querySelector('[data-version-select]');
if (versionSelect) {
  versionSelect.addEventListener('change', () => {
    if (versionSelect.value) window.location.assign(versionSelect.value);
  });
}

// Interactive Observatory: RTL toggle, bearing instrument, and surface/motion studio
const observatory = document.querySelector('[data-observatory]');
if (observatory) {
  const dirButtons = [...observatory.querySelectorAll('[data-set-dir]')];
  dirButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const dir = btn.dataset.setDir;
      rootEl.dir = dir;
      dirButtons.forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
      notify(`Direction: ${dir.toUpperCase()} (compass stays true north)`);
    });
  });

  // Live SVG bearing instrument
  const instrument = observatory.querySelector('.lab-instrument');
  const range = observatory.querySelector('[data-lab-heading]');
  const needle = observatory.querySelector('[data-lab-needle]');
  const targetG = observatory.querySelector('[data-lab-target]');
  const arc = observatory.querySelector('[data-lab-arc]');
  const arcGrad = observatory.querySelector('#lab-turn');
  const headingVal = observatory.querySelector('[data-lab-heading-val]');
  const headingCode = observatory.querySelector('[data-lab-heading-code]');
  const targetVal = observatory.querySelector('[data-lab-target-val]');
  const turnBadge = observatory.querySelector('[data-lab-turn]');
  const targetBtns = [...observatory.querySelectorAll('[data-lab-target-set]')];
  let heading = Number(range?.value ?? 42);
  let target = 110;
  const padDeg = (n) => `${String(((n % 360) + 360) % 360).padStart(3, '0')}°`;
  const onDial = (deg) => {
    const r = (deg * Math.PI) / 180;
    return [160 + 128 * Math.sin(r), 160 - 128 * Math.cos(r)].map((v) => v.toFixed(1));
  };
  const renderDial = () => {
    if (!needle || !targetG || !arc) return;
    needle.setAttribute('transform', `rotate(${heading} 160 160)`);
    targetG.setAttribute('transform', `rotate(${target} 160 160)`);
    headingVal.textContent = padDeg(heading);
    headingCode.textContent = padDeg(heading);
    targetVal.textContent = padDeg(target);
    const delta = ((target - heading + 540) % 360) - 180;
    const abs = Math.abs(delta);
    const locked = abs <= 2;
    if (instrument) instrument.dataset.locked = locked ? 'true' : 'false';
    if (locked) {
      turnBadge.textContent = abs === 0 ? 'Aligned on target' : 'Aligned · detent';
      arc.setAttribute('d', '');
    } else {
      turnBadge.textContent = `Turn ${abs}° ${delta > 0 ? 'CW' : 'CCW'}`;
      const [sx, sy] = onDial(heading);
      const [ex, ey] = onDial(target);
      const sweep = delta > 0 ? 1 : 0;
      arc.setAttribute('d', `M${sx} ${sy}A128 128 0 0 ${sweep} ${ex} ${ey}`);
      if (arcGrad) {
        arcGrad.setAttribute('x1', sx);
        arcGrad.setAttribute('y1', sy);
        arcGrad.setAttribute('x2', ex);
        arcGrad.setAttribute('y2', ey);
      }
    }
  };
  if (range) {
    range.addEventListener('input', () => {
      heading = Number(range.value);
      renderDial();
    });
    targetBtns.forEach((btn) => {
      btn.addEventListener('click', () => {
        target = Number(btn.dataset.labTargetSet);
        targetBtns.forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
        renderDial();
      });
    });
    renderDial();
  }

  // Surface & choreography studio
  const stage = observatory.querySelector('[data-lab-stage]');
  const card = observatory.querySelector('[data-lab-card]');
  const radiusOut = observatory.querySelector('[data-lab-radius-readout]');
  const spaceOut = observatory.querySelector('[data-lab-space-readout]');
  const timingOut = observatory.querySelector('[data-lab-timing-readout]');
  const triggerBtn = observatory.querySelector('[data-lab-trigger]');
  const copyVarsBtn = observatory.querySelector('[data-lab-copy-vars]');
  const spaceMap = { compact: '--space-16', standard: '--space-24', generous: '--space-32' };
  const motifMap = { feedback: ['150ms', 'standard'], state: ['300ms', 'emphasis'], context: ['500ms', 'entrance'] };
  let activeMotif = 'state';
  let auditionTimer = 0;

  observatory.querySelectorAll('[data-lab-radius]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const r = btn.dataset.labRadius;
      stage.dataset.radius = r;
      radiusOut.textContent = `--radius-${r}`;
      observatory.querySelectorAll('[data-lab-radius]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    });
  });
  observatory.querySelectorAll('[data-lab-density]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const d = btn.dataset.labDensity;
      stage.dataset.density = d;
      spaceOut.textContent = spaceMap[d];
      observatory.querySelectorAll('[data-lab-density]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    });
  });
  observatory.querySelectorAll('[data-lab-motif]').forEach((btn) => {
    btn.addEventListener('click', () => {
      activeMotif = btn.dataset.labMotif;
      const [dur, ease] = motifMap[activeMotif];
      timingOut.textContent = `${dur} · ${ease}`;
      observatory.querySelectorAll('[data-lab-motif]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    });
  });
  if (triggerBtn && card) {
    triggerBtn.addEventListener('click', () => {
      const tier = rootEl.dataset.motion;
      if (tier === 'static') {
        notify('Static tier active: final state applied immediately (C17)');
        return;
      }
      const [dur, ease] = motifMap[activeMotif];
      card.style.transitionDuration = `var(--motion-${activeMotif})`;
      card.style.transitionTimingFunction = `var(--ease-${ease})`;
      card.classList.add('is-auditioning');
      clearTimeout(auditionTimer);
      auditionTimer = setTimeout(() => card.classList.remove('is-auditioning'), Number.parseInt(dur, 10) + 60);
      notify(`Auditioned ${activeMotif} motif (${dur}, ${ease})`);
    });
  }
  if (copyVarsBtn && stage) {
    copyVarsBtn.addEventListener('click', () => {
      const r = stage.dataset.radius;
      const s = spaceMap[stage.dataset.density];
      const [, ease] = motifMap[activeMotif];
      const snippet = `border-radius: var(--radius-${r}); padding: var(${s}); transition: transform var(--motion-${activeMotif}) var(--ease-${ease});`;
      copyText(snippet, 'Cosmos CSS snippet');
    });
  }
}

// Click-to-copy gradient tokens in Light with a job
document.querySelectorAll('[data-copy-token]').forEach((el) => {
  const act = () => copyText(el.dataset.copyToken, el.dataset.copyToken);
  el.addEventListener('click', act);
  el.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      act();
    }
  });
});

// Progressively enhanced Rule Filter + Scroll-Spy + Anchor Copy on Spec pages
const specContent = document.querySelector('.spec-content');
const allRules = specContent ? [...specContent.querySelectorAll('.rule[data-level]')] : [];
if (specContent && allRules.length) {
  const bar = document.createElement('div');
  bar.className = 'spec-toolbar';
  bar.setAttribute('role', 'search');
  bar.innerHTML =
    '<input type="search" class="spec-search" placeholder="Filter rules by ID or keyword (e.g. C05, contrast, motion)…" aria-label="Filter normative rules">' +
    '<div class="pill-row" role="group" aria-label="Filter by normative level">' +
    ['all', 'must', 'should', 'may']
      .map(
        (lvl, i) =>
          `<button type="button" class="pill pill-sm" data-filter-level="${lvl}" aria-pressed="${i === 0}">${lvl.toUpperCase()}</button>`,
      )
      .join('') +
    '</div><span class="spec-filter-count" aria-live="polite"></span>';
  specContent.prepend(bar);

  const search = bar.querySelector('.spec-search');
  const levelBtns = [...bar.querySelectorAll('[data-filter-level]')];
  const countEl = bar.querySelector('.spec-filter-count');
  const sections = [...specContent.querySelectorAll('.spec-section')];
  let activeLevel = 'all';

  const applyFilter = () => {
    const q = search.value.trim().toLowerCase();
    let shown = 0;
    for (const rule of allRules) {
      const matchLevel = activeLevel === 'all' || rule.dataset.level === activeLevel;
      const matchText = !q || rule.textContent.toLowerCase().includes(q) || rule.id.includes(q);
      const visible = matchLevel && matchText;
      rule.hidden = !visible;
      if (visible) shown += 1;
    }
    for (const sec of sections) {
      const secRules = [...sec.querySelectorAll('.rule[data-level]')];
      if (secRules.length) sec.hidden = secRules.every((r) => r.hidden);
    }
    countEl.textContent = `${shown} of ${allRules.length} rules`;
  };
  search.addEventListener('input', applyFilter);
  levelBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      activeLevel = btn.dataset.filterLevel;
      levelBtns.forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
      applyFilter();
    });
  });
  applyFilter();

  // Click rule ID badge to copy canonical rule link
  allRules.forEach((rule) => {
    const link = rule.querySelector('.rule-marker a');
    if (link) {
      link.addEventListener('click', () => {
        copyText(`${location.origin}${location.pathname}${link.getAttribute('href')}`, `Rule ${link.textContent} link`);
      });
    }
  });

  // Sticky local-nav scroll-spy
  const navLinks = new Map(
    [...document.querySelectorAll('.local-nav ol a')].map((a) => [a.getAttribute('href')?.slice(1), a]),
  );
  if (navLinks.size) {
    const spy = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          navLinks.forEach((link) => link.removeAttribute('aria-current'));
          navLinks.get(entry.target.id)?.setAttribute('aria-current', 'location');
        }
      },
      { rootMargin: '-20% 0px -65% 0px' },
    );
    for (const id of navLinks.keys()) {
      const target = document.getElementById(id);
      if (target) spy.observe(target);
    }
  }
}
