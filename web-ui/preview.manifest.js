export default {
  id: 'web-ui',
  title: 'Web UI playground',
  description:
    'Framework-agnostic controls and motion primitives from the published @simt/web-ui package source, running locally with no network or storage dependency.',
  features: ['theme', 'motion', 'reveal', 'ambient', 'header', 'sections', 'pointer', 'performance'],
  usage: `import {
  detectPerformance,
  initMotion,
  initPointerScene,
  initLocaleSelect,
  initTheme,
  observeReveals,
  pauseOffscreen,
  trackHeader,
  trackSections,
} from '@simt/web-ui';
import '@simt/web-ui/styles.css';

detectPerformance();
initTheme(document.querySelector('[data-theme-toggle]'));
initMotion(document.querySelector('[data-motion-toggle]'));
initLocaleSelect(document.querySelector('[data-locale-select]'));
observeReveals(document.querySelectorAll('[data-reveal]'));
pauseOffscreen(document.querySelectorAll('[data-ambient]'));
trackHeader(document.querySelector('[data-header]'));
trackSections(document.querySelectorAll('[data-nav-link]'));
initPointerScene({ area: document.querySelector('[data-pointer-area]') });`,
  components: [
    {
      id: 'actions',
      title: 'Actions and icon buttons',
      description:
        'Primary, ghost, outline, halo and size variants use the package stylesheet directly. Icon controls keep an accessible name and a 44px target.',
      source: ['src/styles.css'],
      contracts: ['.button', '.button--primary', '.button--ghost', '.button--outline', '.icon-button'],
      usage: `<button class="button button--primary" type="button">Launch orbit</button>
<button class="icon-button" type="button" aria-label="Favorite">
  <svg aria-hidden="true" viewBox="0 0 24 24">…</svg>
</button>`,
      markup: `<section class="preview-section" id="actions" data-preview-component="actions" aria-labelledby="actions-title">
  <header class="preview-section-heading">
    <p class="preview-kicker">01 / CONTROLS</p>
    <h2 id="actions-title">Actions with a clear next step.</h2>
    <p>Every control below is styled by <code>src/styles.css</code>. The demo buttons only update the local status line.</p>
  </header>
  <div class="preview-actions">
    <button class="button button--primary" type="button" data-preview-action="Primary action selected">Launch orbit <svg class="icon icon--flip" aria-hidden="true" viewBox="0 0 24 24"><path d="M5 12h14m-6-6 6 6-6 6"/></svg></button>
    <button class="button button--ghost" type="button" data-preview-action="Ghost action selected">Inspect signal</button>
    <button class="button button--outline" type="button" data-preview-action="Outline action selected">Save view</button>
    <button class="button button--ghost button--halo" type="button" data-preview-action="Halo action selected">Hold orbit</button>
  </div>
  <div class="preview-action-row">
    <button class="button button--ghost button--small" type="button" data-preview-action="Small action selected">Small</button>
    <button class="button button--ghost button--large" type="button" data-preview-action="Large action selected">Large</button>
    <button class="button button--ghost" type="button" disabled>Unavailable</button>
    <span class="preview-icon-row" aria-label="Icon button examples">
      <button class="icon-button" type="button" aria-label="Favorite" data-preview-action="Favorite selected"><svg aria-hidden="true" viewBox="0 0 24 24"><path d="m12 20-8-8a5 5 0 0 1 8-6 5 5 0 0 1 8 6z"/></svg></button>
      <button class="icon-button" type="button" aria-label="Share" data-preview-action="Share selected"><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m9 10 6-4M9 14l6 4"/></svg></button>
      <button class="icon-button" type="button" aria-label="Settings" data-preview-action="Settings selected"><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-1-4l2-2-2-2-2 2a7 7 0 0 0-8 0l-2-2-2 2 2 2a7 7 0 0 0-1 4 7 7 0 0 0 1 4l-2 2 2 2 2-2a7 7 0 0 0 8 0l2 2 2-2-2-2a7 7 0 0 0 1-4Z"/></svg></button>
    </span>
  </div>
  <p class="preview-feedback" role="status" aria-live="polite" data-preview-feedback>Choose an action to inspect its local response.</p>
</section>`,
    },
    {
      id: 'marquee',
      title: 'Ambient marquee',
      description:
        'The duplicated track loops only in full motion, pauses on hover and stops whenever its ambient container is offscreen.',
      source: ['src/styles.css', 'src/observers.mjs'],
      contracts: ['.marquee', '.marquee__rail', '.marquee__track--clone', '[data-ambient]'],
      usage: `<div class="marquee" data-ambient>
  <div class="marquee__viewport">
    <div class="marquee__rail">
      <ul class="marquee__track">…</ul>
      <ul class="marquee__track marquee__track--clone" aria-hidden="true">…</ul>
    </div>
  </div>
</div>`,
      markup: `<section class="preview-section preview-section--flush" id="marquee" data-preview-component="marquee" data-ambient aria-labelledby="marquee-title">
  <header class="preview-section-heading">
    <p class="preview-kicker">02 / AMBIENT</p>
    <h2 id="marquee-title">A signal that knows when to rest.</h2>
    <p>Hover to pause. Switch motion to Reduced to return to the static list.</p>
  </header>
  <div class="marquee">
    <p class="marquee__title" id="marquee-list-title">One shared motion contract</p>
    <div class="marquee__viewport" aria-labelledby="marquee-list-title">
      <div class="marquee__rail">
        <ul class="marquee__track" role="list">
          <li class="marquee__item" role="listitem">Reveal</li>
          <li class="marquee__item" role="listitem">Ambient</li>
          <li class="marquee__item" role="listitem">Pointer</li>
          <li class="marquee__item" role="listitem">Performance</li>
          <li class="marquee__item" role="listitem">Preference</li>
        </ul>
        <ul class="marquee__track marquee__track--clone" aria-hidden="true">
          <li class="marquee__item">Reveal</li>
          <li class="marquee__item">Ambient</li>
          <li class="marquee__item">Pointer</li>
          <li class="marquee__item">Performance</li>
          <li class="marquee__item">Preference</li>
        </ul>
      </div>
    </div>
  </div>
</section>`,
    },
    {
      id: 'reveal',
      title: 'Staggered reveals',
      description:
        'IntersectionObserver adds the revealed state once. Reduced motion keeps every card visible without a transition.',
      source: ['src/styles.css', 'src/observers.mjs'],
      contracts: ['[data-reveal]', '.is-revealed', '.stagger'],
      usage: `<div class="stagger">
  <article data-reveal>…</article>
  <article data-reveal>…</article>
  <article data-reveal>…</article>
</div>`,
      markup: `<section class="preview-section" id="reveal" data-preview-component="reveal" aria-labelledby="reveal-title">
  <header class="preview-section-heading">
    <p class="preview-kicker">03 / REVEAL</p>
    <h2 id="reveal-title">Entrance motion with a final state.</h2>
    <p>Scroll the cards into view. The shared observer never hides content in Reduced mode.</p>
  </header>
  <div class="preview-card-grid stagger">
    <article class="preview-card" data-reveal><span>01</span><h3>Observe</h3><p>Reveal only when the element enters the viewport.</p></article>
    <article class="preview-card" data-reveal><span>02</span><h3>Resolve</h3><p>Add the final class once and stop observing the target.</p></article>
    <article class="preview-card" data-reveal><span>03</span><h3>Respect</h3><p>Keep the same content readable when motion is reduced.</p></article>
  </div>
  <div class="preview-ambient" data-ambient aria-hidden="true"><span></span><span></span><span></span></div>
</section>`,
    },
    {
      id: 'faq',
      title: 'Native animated accordions',
      description:
        'Semantic details and summary cards animate their disclosure height where supported. Reduced motion, Lite mode and unsupported browsers keep native instant disclosure.',
      source: ['src/styles.css'],
      contracts: ['.faq__grid', '.faq__heading', '.faq__list', '.faq-item', '.faq-item__icon', '.faq-item__a'],
      usage: `<section class="faq" aria-labelledby="faq-title">
  <div class="container faq__grid">
    <header class="faq__heading"><h2 id="faq-title">Questions</h2></header>
    <div class="faq__list">
      <details class="faq-item">
        <summary><span class="faq-item__q">Question</span><span class="faq-item__icon" aria-hidden="true">+</span></summary>
        <div class="faq-item__a"><p>Answer</p></div>
      </details>
    </div>
  </div>
</section>`,
      markup: `<section class="preview-section preview-section--flush" id="faq" data-preview-component="faq" aria-labelledby="faq-title">
  <div class="container faq__grid">
    <header class="preview-section-heading faq__heading" data-reveal>
      <p class="preview-kicker">04 / QUESTIONS</p>
      <h2 id="faq-title">Open only what you need.</h2>
      <p>Use Tab and Enter or Space. Native disclosure still works without JavaScript.</p>
    </header>
    <div class="faq__list stagger">
      <details class="faq-item" data-reveal>
        <summary><span class="faq-item__q">Is accordion behavior scripted?</span><span class="faq-item__icon" aria-hidden="true"><svg class="icon" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></span></summary>
        <div class="faq-item__a"><p>No. The browser owns keyboard operation, focus and the open state.</p></div>
      </details>
      <details class="faq-item" data-reveal>
        <summary><span class="faq-item__q">What changes in Reduced or Lite mode?</span><span class="faq-item__icon" aria-hidden="true"><svg class="icon" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></span></summary>
        <div class="faq-item__a"><p>Transitions stop. Questions and answers remain immediately available.</p></div>
      </details>
      <details class="faq-item" data-reveal>
        <summary><span class="faq-item__q">What if height animation is unsupported?</span><span class="faq-item__icon" aria-hidden="true"><svg class="icon" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></span></summary>
        <div class="faq-item__a"><p>The native details element opens and closes without an animation.</p></div>
      </details>
    </div>
  </div>
</section>`,
    },
    {
      id: 'pointer',
      title: 'Pointer scene',
      description:
        'A fine pointer moves the lantern and tilt variables through requestAnimationFrame. Touch, Reduced and Lite modes stay still.',
      source: ['src/pointer.mjs', 'src/styles.css'],
      contracts: ['[data-pointer-area]', '--lx', '--ly', '--tilt-x', '--tilt-y', '.is-lit'],
      usage: `initPointerScene({
  area: document.querySelector('[data-pointer-area]'),
  bounds: document.querySelector('[data-pointer-area]'),
  spotlight: document.querySelector('[data-pointer-spotlight]'),
  tilt: document.querySelector('[data-pointer-tilt]'),
});`,
      markup: `<section class="preview-section" id="pointer" data-preview-component="pointer" aria-labelledby="pointer-title">
  <header class="preview-section-heading">
    <p class="preview-kicker">05 / POINTER</p>
    <h2 id="pointer-title">Depth that follows attention.</h2>
    <p>Move a fine pointer across the stage. The effect is decorative and never required to understand the content.</p>
  </header>
  <div class="preview-pointer-stage" data-pointer-area>
    <div class="preview-pointer-spotlight" data-pointer-spotlight aria-hidden="true"></div>
    <div class="preview-pointer-tilt" data-pointer-tilt>
      <p class="preview-kicker">LIVE SCENE</p>
      <h3>Lantern / horizon</h3>
      <p>Pointer coordinates become local CSS variables. No pointer data leaves this document.</p>
      <span class="preview-pointer-mark" aria-hidden="true">◎</span>
    </div>
  </div>
</section>`,
    },
    {
      id: 'runtime',
      title: 'Preference and performance state',
      description:
        'The runtime reports the active theme, motion and performance tier. Save-data, low-memory, low-core and slow-frame probes can select Lite automatically.',
      source: ['src/preferences.mjs', 'src/navigation.mjs', 'src/performance.mjs', 'src/observers.mjs'],
      contracts: ['[data-theme-toggle]', '[data-motion-toggle]', '[data-locale-select]', '[data-perf="lite"]', '[data-header]', '[data-nav-link]'],
      usage: `detectPerformance();
initTheme(document.querySelector('[data-theme-toggle]'));
initMotion(document.querySelector('[data-motion-toggle]'));
initLocaleSelect(document.querySelector('[data-locale-select]'));
trackHeader(document.querySelector('[data-header]'));
trackSections(document.querySelectorAll('[data-nav-link]'));`,
      markup: `<section class="preview-section" id="runtime" data-preview-component="runtime" aria-labelledby="runtime-title">
  <header class="preview-section-heading">
    <p class="preview-kicker">06 / RUNTIME</p>
    <h2 id="runtime-title">Visible state, not hidden policy.</h2>
    <p>The header controls call the real package functions. This playground does not persist the choices.</p>
  </header>
  <dl class="preview-runtime">
    <div><dt>Theme</dt><dd data-runtime-theme>System</dd></div>
    <div><dt>Motion</dt><dd data-runtime-motion>System</dd></div>
    <div><dt>Performance</dt><dd data-runtime-performance>Auto</dd></div>
    <div><dt>Storage</dt><dd>Not used</dd></div>
  </dl>
</section>`,
    },
  ],
};
