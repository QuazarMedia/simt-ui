const configElement = document.querySelector('[data-preview-config]');
if (!configElement) throw new Error('Library preview configuration is missing');

const config = JSON.parse(configElement.textContent);
const runtime = await import(config.runtimeUrl);
const root = document.documentElement;
const features = new Set(config.features ?? []);
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const feedback = $('[data-preview-feedback]');

if (features.has('theme')) {
  runtime.initTheme($('[data-theme-toggle]'), {
    messages: { themeToDark: 'Switch to dark theme', themeToLight: 'Switch to light theme' },
  });
}
if (features.has('motion')) {
  runtime.initMotion($('[data-motion-toggle]'), {
    messages: { motionPause: 'Pause decorative motion', motionPlay: 'Play decorative motion' },
  });
}
if (features.has('performance')) runtime.detectPerformance();
if (features.has('reveal')) runtime.observeReveals($$('[data-reveal]'));
if (features.has('ambient')) runtime.pauseOffscreen($$('[data-ambient]'));
if (features.has('header')) runtime.trackHeader($('[data-header]'));
if (features.has('sections')) runtime.trackSections($$('[data-nav-link]'));
if (features.has('pointer')) {
  runtime.initPointerScene({
    area: $('[data-pointer-area]'),
    bounds: $('[data-pointer-area]'),
    spotlight: $('[data-pointer-spotlight]'),
    tilt: $('[data-pointer-tilt]'),
  });
}

const syncThemeAdapter = () => {
  const adapter = config.themeAdapter;
  if (!adapter?.attribute) return;
  const value = adapter.values?.[root.dataset.theme];
  if (value) root.setAttribute(adapter.attribute, value);
  else root.removeAttribute(adapter.attribute);
};
const resolvedTheme = () =>
  root.dataset.theme ?? (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
const syncRuntime = () => {
  $$('[data-runtime-theme]').forEach((element) => {
    element.textContent = resolvedTheme();
  });
  $$('[data-runtime-motion]').forEach((element) => {
    element.textContent = root.dataset.motion ?? 'system';
  });
  $$('[data-runtime-performance]').forEach((element) => {
    element.textContent = root.dataset.perf ?? 'auto';
  });
};

syncThemeAdapter();
syncRuntime();
new MutationObserver(() => {
  syncThemeAdapter();
  syncRuntime();
}).observe(root, { attributes: true, attributeFilter: ['data-theme', 'data-motion', 'data-perf'] });

$$('[data-preview-action]').forEach((button) => {
  button.addEventListener('click', () => {
    if (feedback) feedback.textContent = button.dataset.previewAction;
  });
});
