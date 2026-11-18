import { esc, siteOutputs } from './site.js';
import { budgetOf, products } from '../src/site/product.js';
import { authority as space } from '../src/site/authority.js';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  colors,
  cssRoleAliases,
  cssRolePrefix,
  cssThemeAttribute,
  defaultProfile,
  tokens,
  adapters,
  platformAdapters,
  partScope,
  partColorSpace,
  colourProjection,
  gradients,
  metrics,
  themeProfiles,
  themeColorTokens,
  themeCSS,
} from '../src/design/tokens.js';

// Anchors no profile claims: they are brand values rather than semantic roles,
// so no profile can override them and they stay in the base block. Everything
// else is a role, and the profile blocks own it.
const brandOnlyAnchors = colors.filter(([name]) => !(name in cssRoleAliases));
// A scale group carries its own $description; only its tokens become CSS variables.
const scaleTokens = (group) => Object.entries(group).filter(([key]) => !key.startsWith('$'));
import { buildContrastPairs } from '../src/design/contrast.js';
import { pages, release, sources, provenance, faqs } from '../src/design/specification.js';
import { MOTION_TIERS, orbit, specimen, workbench, lightGrammar } from '../src/design/specimens.js';
import { skillMarkdown, skillRuleIds } from '../src/design/skill.js';
import { starfieldSVG, starfieldTokens, starfieldAlgorithm, starfieldFixture } from '../src/design/starfield.js';
import {
  durationFor,
  EASING_PURPOSES,
  easingFor,
  MOTION_DURATIONS,
  MOTION_FETCH_WHEN,
  MOTION_PROPERTIES,
  motionFixture,
  motionMotifs,
  motionParityAlgorithm,
} from '../src/design/motion.js';
import { galleryIndex, galleryFamily, GALLERY_FAMILIES } from '../src/design/gallery.js';
import {
  PART_SLUGS,
  COMPONENT_LAYERS,
  COMPONENT_PARTS,
  COMPONENT_RADII,
  COMPONENTS_FETCH_WHEN,
  COMPONENT_STATES,
  NOT_APPLICABLE,
  componentApplicability,
  componentRegistry,
  componentsForFamily,
  motionRolesFor,
  PART_LABELS,
  THEME_ROLES,
} from '../src/design/components.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
// Everything published lands under dist/, which is the Workers assets directory.
const DIST = resolve(root, 'dist');
// One product, one base. Every URL and every generated path in this file is
// derived from the descriptor rather than typed, so the space can publish a
// second sub-context under the same context root by adding a descriptor and
// nothing else.
const PRODUCT = products[0];
const PRODUCT_BASE = PRODUCT.basePath;
const PRODUCT_DIR = PRODUCT.basePath.replace(/^\//, '');
const productFile = (path) => (PRODUCT_DIR ? `${PRODUCT_DIR}/${path}` : path);

const pathFor = (slug) => (slug ? `/${slug}` : '/');
// The standalone authority owns every canonical URL generated here.
const urlFor = (slug) => (slug ? `${space.origin}/${slug}` : space.origin);
// Pretty JSON, except that a flat array or object of primitives stays on one
// line when it fits: one color component or one pair reference per indented
// line was about a fifth of every machine export an agent has to read.
const json = (value) =>
  JSON.stringify(value, null, 2).replace(/([[{])\n\s+([^[\]{}]*?)\n\s*([\]}])/g, (match, open, inner, close) => {
    const line = `${open}${open === '{' ? ' ' : ''}${inner.replace(/,\n\s+/g, ', ')}${open === '{' ? ' ' : ''}${close}`;
    return line.length <= 160 ? line : match;
  }) + '\n';
const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
const longDate = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return `${d} ${MONTHS[m - 1]} ${y}`;
};
// One hero sky, shared by every Cosmos route as a single cached image. The
// narrative column and the focal emblem are exclusion zones at the widths
// where they sit side by side, so stars stay at the edge of both.
export const HERO_SKY = productFile('sky-hero.svg');
const heroSky = () =>
  starfieldSVG('cosmos-design.hero', 1440, 760, {
    exclude: [
      [0.03, 0.14, 0.47, 0.9],
      [0.4, 0.06, 0.85, 0.97],
    ],
  }).replace('<svg ', '<svg width="1440" height="760" ');
const sky = '<div class="sky" aria-hidden="true"></div>';
// Filled by generatedOutputs(); cosmos-tokens.less is written from it as well.
let generatedCSS = '';

// The names an agent reads in tokens.json are the profile keys; the names a
// visitor reads are these. Kept here rather than in the stylesheet because the
// control is part of the document head the generator writes.
const THEME_LABELS = {
  deepSpace: 'Deep Space',
  daylight: 'Daylight',
  astronomy: 'Astronomy red',
  watchAmbient: 'Watch ambient',
};
// The pre-hydration script has to recognise every profile by name before the
// module that owns the theme runs, so its guard list is written out rather than
// imported -- but it is generated from the owner, so a fifth profile is
// restored on the first paint instead of flashing the default past it. The
// browser check compares the client against this same list.
const PROFILE_NAMES = Object.keys(themeProfiles);
// A profile with no label publishes an <option> reading undefined, and a label
// with no profile is a word in a picker that was never rendered. Build-time
// rather than a gate, because the gate reads the output this produces.
for (const name of PROFILE_NAMES) {
  if (!THEME_LABELS[name])
    throw new Error(
      `themeProfiles publishes ${name} and THEME_LABELS has no label for it, so the theme picker renders its value as undefined`,
    );
}
for (const name of Object.keys(THEME_LABELS)) {
  if (!PROFILE_NAMES.includes(name))
    throw new Error(`THEME_LABELS labels ${name}, which is not a profile the token file publishes`);
}
const PROFILE_GUARD = PROFILE_NAMES.map((name) => `s==='${name}'`).join('||');
// The same argument for the motion tier: the tier is restored before anything
// animates, which means before the module that owns it can run, so the list of
// tiers it will accept has to be written into the page from the file that owns
// it. If it were typed here instead, a fourth tier would restore on the first
// paint and then be discarded the moment the client read it back.
const MOTION_GUARD = MOTION_TIERS.map(({ value }) => `m==='${value}'`).join('||');

// The browser chrome a profile paints its toolbar and status area with. It is
// the profile's own background, read from the token file rather than typed, and
// a value the CSS parser would accept but a browser cannot use as a colour is
// refused here instead of published as a broken chrome.
const chromeColor = (profile) => {
  const background = themeProfiles[profile]?.background;
  if (!/^#[0-9A-F]{6}$/i.test(background ?? '')) {
    throw new Error(
      `theme profile ${profile} has background ${background}, which is not a six-digit hex the browser chrome can be painted`,
    );
  }
  return background;
};

// The gallery is a published route of the specification, not a part of it, so it
// carries one slug of its own. Navigation needs it to mark the gallery current,
// the way it marks a part current, and the alternates and structured data need it
// so a gallery page's canonical address is the same everywhere it is written.
const GALLERY_SLUG = 'gallery';

function navigation(slug) {
  return `<header class="cosmos-header"><div class="header-inner"><a class="brand" href="/" aria-label="Cosmos home"><img src="${space.logo}" width="32" height="32" alt=""><span>SIMT <b>Cosmos</b></span></a><span class="version-label">SPECIFICATION ${release.version}</span><label class="theme-picker"><select data-theme-select aria-label="Theme profile"><option value="">Follow system</option>${Object.keys(
    themeProfiles,
  )
    .map((name) => `<option value="${name}" data-color="${chromeColor(name)}">${THEME_LABELS[name]}</option>`)
    .join(
      '',
    )}</select></label><a class="header-download" href="${PRODUCT_BASE}/specification.md">Plain text <span aria-hidden="true">↗</span></a></div><nav class="platform-nav" aria-label="Cosmos parts"><a href="${pathFor('')}"${!slug ? ' aria-current="page"' : ''}>Overview</a>${pages.map((p) => `<a href="${pathFor(p.slug)}"${slug === p.slug ? ' aria-current="page"' : ''}><span>${p.number}</span>${p.label}</a>`).join('')}<a href="${PRODUCT_BASE}/gallery"${slug === GALLERY_SLUG ? ' aria-current="page"' : ''}>Gallery</a></nav></header>`;
}

// Each part inherits the common foundations and then reads exactly one adapter.
// A per-part payload is the union of those two things, so an agent working on a
// watch screen is never handed the web easing set or the dp radius scale.
export const PART_ADAPTERS = Object.fromEntries(
  Object.entries(PART_SLUGS)
    .filter(([adapter]) => adapter !== 'common')
    .map(([adapter, slug]) => [slug, adapter]),
);

// When to fetch the measured contrast document. Published twice — once as the
// pointer every token document carries, once by the document itself — so it is
// written once here and compared by the gate rather than restated as prose that
// can drift from the pointer.
const CONTRAST_FETCH_WHEN = [
  'introducing a colour or gradient',
  'pairing a translucent, blurred or layered surface',
  'checking or changing a theme profile',
];

// The per-part payloads are listed beside the union so an agent can see the
// smaller option without having to know that it exists.
function partPayloadNote() {
  return pages
    .filter((p) => PART_ADAPTERS[p.slug])
    .map(
      (p) =>
        `<li><a href="${PRODUCT_BASE}/${p.slug}/tokens.json">${p.label} tokens</a> and <a href="${PRODUCT_BASE}/${p.slug}/rules.json">rules</a> <span>JSON · common foundations plus the ${esc(p.label)} mapping only</span></li>`,
    )
    .join('');
}

function renderTable(t) {
  const swatch = (c) =>
    /^#[0-9A-F]{6}$/i.test(c)
      ? `<span class="token-swatch" style="--swatch:${c}" aria-hidden="true"></span>`
      : /^gradient\.[a-z]+$/.test(c)
        ? `<span class="token-swatch gradient-swatch" style="--swatch:var(--gradient-${c.slice(9)})" aria-hidden="true"></span>`
        : '';
  return `<div class="table-scroll" tabindex="0" role="region" aria-label="${esc(t.caption)}"><table><caption>${esc(t.caption)}</caption><thead><tr>${t.columns.map((c) => `<th scope="col">${esc(c)}</th>`).join('')}</tr></thead><tbody>${t.rows.map((row) => `<tr>${row.map((c, i) => `<${i ? 'td' : 'th scope="row"'}>${swatch(c)}${esc(c)}</${i ? 'td' : 'th'}>`).join('')}</tr>`).join('')}</tbody></table></div>`;
}

// A section carries one table or several. The motion section owns both the easing
// mapping and the motif index, and a renderer that read a single `table` would drop
// the second one silently — which is exactly the kind of omission a generated
// document cannot report on itself.
const tablesOf = (s) => (s.table === undefined || s.table === null ? [] : Array.isArray(s.table) ? s.table : [s.table]);

// One table publishes as `table`, several as `tables`, and a section with none
// publishes neither key: a reader asking for `tables` on a section that has one
// table gets `undefined`, and a reader asking for `table` on a section with three
// gets the first and believes it is the only one. Deciding that once, from one
// measurement, keeps the shape and the count from being derived separately and
// then disagreeing.
function tableFields(s) {
  const tables = tablesOf(s);
  if (tables.length === 1) return { table: tables[0] };
  if (tables.length === 0) return {};
  return { tables };
}

function renderSection(s, index) {
  return `<section class="spec-section" id="${s.id}" aria-labelledby="${s.id}-title"><header class="section-heading"><span class="section-number">${String(index + 1).padStart(2, '0')}</span><div><h2 id="${s.id}-title">${esc(s.title)}</h2><p>${esc(s.summary)}</p></div></header><ol class="rule-list">${s.rules.map((r) => `<li class="rule" id="${r.id.toLowerCase()}"><div class="rule-marker"><a href="#${r.id.toLowerCase()}" aria-label="Link to rule ${r.id}">${r.id}</a><span>${r.level.toUpperCase()}</span></div><div class="rule-body"><h3>${esc(r.title)}</h3><p>${esc(r.requirement)}</p><div class="acceptance"><span>Acceptance</span><p>${esc(r.acceptance)}</p></div></div></li>`).join('')}</ol>${tablesOf(s).map(renderTable).join('')}</section>`;
}

function downloads() {
  return `<section class="downloads" id="implementation" aria-labelledby="downloads-title"><div><p class="eyebrow">BUILT FOR IMPLEMENTATION</p><h2 id="downloads-title">Human-readable. Machine-ready.</h2><p>Every page and every rule is generated from one source. Load the common contract, then the platform you are building.</p></div><ul><li><a href="${PRODUCT_BASE}/manifest.json">Resource manifest <span>JSON · routes, versions, inheritance</span></a></li><li><a href="${PRODUCT_BASE}/specification.json">Specification index <span>JSON · pages, sections, rule IDs</span></a></li><li><a href="${PRODUCT_BASE}/rules.json">Normative rules <span>JSON · requirements and acceptance</span></a></li><li><a href="${PRODUCT_BASE}/tokens.json">Typed design tokens <span>JSON · exact colors and units</span></a></li><li><a href="${PRODUCT_BASE}/tokens.css">Web token variables <span>CSS · copy into a platform theme</span></a></li><li><a href="${PRODUCT_BASE}/specification.md">Complete plain-text guide <span>Markdown · readable without scripts</span></a></li><li><a href="${PRODUCT_BASE}/cosmos-design-skill.md">Agent design skill <span>Markdown · the failures generated UI makes</span></a></li><li><a href="${PRODUCT_BASE}/contrast-pairs.json">Composited contrast <span>JSON · measured role pairs per theme; fetch when a colour or theme is checked</span></a></li><li><a href="${PRODUCT_BASE}/specification.schema.json">Specification schema <span>JSON Schema · structural validation</span></a></li><li><a href="${PRODUCT_BASE}/changelog.md">Release changelog <span>Markdown · what each published version changed</span></a></li><li><a href="${PRODUCT_BASE}/motion.json">Motion motifs <span>JSON · named choreographies per tier and platform, with the curve parity fixture</span></a></li><li><a href="${PRODUCT_BASE}/components.json">Component index <span>JSON · rules, roles, states and native counterparts per component, sliced by platform</span></a></li></ul><ul class="part-payloads">${partPayloadNote()}</ul></section>`;
}

function overview() {
  return `<main id="main-content"><section class="overview-hero">${sky}<div class="hero-copy"><p class="eyebrow">A DESIGN LANGUAGE BY SIMT</p><h1>A universe<br>around <em>you.</em></h1><p class="hero-lede">Warm depth. Clear signals. A little wonder.<br>Interfaces that make the next step feel natural.</p><div class="hero-actions"><a class="button button-primary" href="${PRODUCT_BASE}/common">Start with the foundations <span aria-hidden="true">↗</span></a><a class="text-link" href="#platforms">Explore the four platforms <span aria-hidden="true">↓</span></a></div><div class="hero-meta"><span>Deep Space palette</span><span>Light with a job</span><span>One deterministic sky</span></div></div><div class="hero-art">${orbit}<span class="art-note">DESIGN THAT ORIENTS, REVEALS & RESPONDS</span></div></section>
  <div class="status-strip"><span class="status-indicator" aria-hidden="true"></span><p><strong>v${release.version} · ${esc(release.status)}</strong> Shared foundation + four platform parts. Updated <time datetime="${release.updated}">${longDate(release.updated)}</time>.</p></div>
  <section class="identity-intro" id="foundations"><p class="eyebrow">SAME COLORS. A RICHER COMPOSITION.</p><div><h2>Clarity is the center.<br>Wonder follows.</h2><p>The Cosmos idea stays: you are the center of your universe. The new system gives it a precise visual grammar—an aperture for attention, a spine for context, a horizon for time. It creates continuity without making every screen look the same.</p><p>One common contract governs color, type, accessibility and meaning. Four platform parts translate it into distinct experiences.</p></div>${specimen('common')}<a class="text-link" href="${PRODUCT_BASE}/common">Read the shared contract <span aria-hidden="true">↗</span></a></section>
  <section class="light-showcase" id="light" aria-labelledby="light-title"><div class="section-heading"><span class="section-number">+</span><div><h2 id="light-title">Light with a job.</h2><p>Eight gradients drawn only from the Deep Space anchors. Each has one job, so depth reads as meaning instead of decoration.</p></div></div>${lightGrammar()}<a class="text-link" href="${PRODUCT_BASE}/common#light">Read the gradient grammar <span aria-hidden="true">↗</span></a></section>
  <section class="platforms" id="platforms" aria-labelledby="platforms-title"><div class="section-heading"><span class="section-number">01–04</span><div><h2 id="platforms-title">Four expressions.<br>One unmistakable Cosmos.</h2><p>Each platform earns its own composition, density and rhythm.</p></div></div>${pages
    .filter((p) => p.slug !== 'common')
    .map(
      (p) =>
        `<article class="platform-feature"><div class="platform-copy"><p class="eyebrow">${p.number} / ${p.label.toUpperCase()}</p><h3>${esc(p.title).replace('\n', '<br>')}</h3><p>${esc(p.intro)}</p><a class="text-link" href="${pathFor(p.slug)}">Open ${p.label.toLowerCase()} specification <span aria-hidden="true">↗</span></a></div><figure class="specimen specimen-${p.slug}">${specimen(p.specimen)}<figcaption>Design specimen · illustrative data · static composition</figcaption></figure></article>`,
    )
    .join('')}</section>
  <section class="principles" id="patterns"><p class="eyebrow">THE PRODUCTION CONTRACT</p><h2>Beautiful is only the beginning.</h2><div class="principle-grid"><article><span>01</span><h3>Every state designed</h3><p>Loading, empty, denied, stale, offline, conflict and success have explicit behavior.</p></article><article><span>02</span><h3>Access built in</h3><p>Readable contrast, full input support, large text, logical RTL and reduced motion.</p></article><article><span>03</span><h3>Performance measured</h3><p>Asset budgets, frame pacing, lifecycle ownership and named release evidence.</p></article></div><p class="implementation-note">${esc(release.implementationStatus)}</p></section>
  ${downloads()}<section class="faq" aria-labelledby="faq-title"><p class="eyebrow">USING COSMOS</p><h2 id="faq-title">A few useful answers.</h2>${faqs.map(([q, a]) => `<details><summary>${esc(q)}</summary><p>${esc(a)}</p></details>`).join('')}</section></main>`;
}

function detail(p) {
  const next = pages[(pages.indexOf(p) + 1) % pages.length];
  const contents = [
    ...p.sections,
    ...(p.slug === 'common'
      ? [
          { id: 'workbench', title: 'Live component study' },
          { id: 'starfield', title: 'Starfield reference' },
          { id: 'theme-adapters', title: 'Theme adapters' },
          { id: 'references', title: 'Sources & provenance' },
        ]
      : []),
  ];
  return `<main id="main-content"><div class="breadcrumb"><a href="${pathFor('')}">Cosmos</a><span aria-hidden="true">/</span><span>${esc(p.label)}</span></div><section class="part-hero">${sky}<div><p class="eyebrow">${p.number} / ${esc(p.label.toUpperCase())}</p><h1>${esc(p.title).replace('\n', '<br>')}</h1><p class="hero-lede">${esc(p.intro)}</p><p class="signature">${esc(p.signature)}</p></div><figure class="specimen specimen-${p.specimen}">${specimen(p.specimen)}<figcaption>Design specimen · illustrative data · static composition</figcaption></figure></section>
  <div class="status-strip"><span class="status-indicator" aria-hidden="true"></span><p><strong>${esc(release.status)} · v${release.version}</strong> ${p.slug === 'common' ? 'Inherited by all four platform parts.' : 'Inherits the <a href="${PRODUCT_BASE}/common">common foundations</a>.'} Conformance is recorded rule by rule in a release record.</p></div>
  <div class="spec-layout"><nav class="local-nav" aria-label="On this page"><p>ON THIS PAGE</p><ol>${contents.map((s, i) => `<li><a href="#${s.id}"><span>${String(i + 1).padStart(2, '0')}</span>${esc(s.title)}</a></li>`).join('')}</ol><a class="local-export" href="${PRODUCT_BASE}/${p.slug}.md">This part as Markdown ↗</a></nav><article class="spec-content" aria-label="${esc(p.label)} specification">${p.sections.map(renderSection).join('')}${p.slug === 'common' ? workbench() + starfieldSection() + adaptersSection() + referencesSection() : ''}${PART_ADAPTERS[p.slug] ? `<section class="part-downloads"><p class="eyebrow">BUILD THIS PART</p><h2>Two files, this part only.</h2><p>The common foundations plus the ${esc(p.label)} mapping, with no other platform's geometry, easing or colour notation. Use these when you are building this part; the union of every part is at the canonical URLs.</p><ul><li><a href="${PRODUCT_BASE}/${p.slug}/tokens.json">This part's tokens <span>JSON · scoped to ${esc(p.label)}</span></a></li><li><a href="${PRODUCT_BASE}/${p.slug}/rules.json">This part's rules <span>JSON · common plus ${esc(p.label)}</span></a></li></ul></section>` : ''}<section class="part-release"><p class="eyebrow">CONFORMANCE RECORD</p><h2>From specification to evidence.</h2><p>Record the product build, applicable rule IDs, owner, fixtures, environment and results. Attach visual, accessibility and performance evidence before calling the implementation production-ready.</p><a class="text-link" href="${PRODUCT_BASE}/release-template.json">Download the release record template ↗</a></section></article></div>
  <div class="next-part"><span>Continue through the system</span><a href="${pathFor(next.slug)}">${next.number} / ${esc(next.label)} <span aria-hidden="true">↗</span></a></div></main>`;
}

function starfieldSection() {
  const f = starfieldTokens().$extensions['app.simt.cosmos'].fixture;
  return `<section class="spec-section" id="starfield"><header class="section-heading"><span class="section-number">+</span><div><h2>Starfield reference</h2><p>The algorithm behind C11. A port is correct when it reproduces the fixture published in tokens.json.</p></div></header><ol class="algorithm">${starfieldAlgorithm.map((step) => `<li>${esc(step).replace(/`([^`]+)`/g, '<code>$1</code>')}</li>`).join('')}</ol><p class="implementation-note">Fixture: surface <code>${f.surfaceId}</code> at ${f.width} × ${f.height} lands in the ${f.bucket.join(' × ')} bucket, draws ${f.count} stars and accepts ${f.accepted}, ${f.spiked} of them spiked. The first three stars, to six decimals, are in <a href="${PRODUCT_BASE}/tokens.json">tokens.json</a> under <code>starfield</code>.</p></section>`;
}

function adaptersSection() {
  return `<section class="spec-section" id="theme-adapters"><header class="section-heading"><span class="section-number">+</span><div><h2>Explicit theme adapters</h2><p>Existing palette roles, with accessible on-colors and essential boundaries.</p></div></header>${Object.entries(
    adapters,
  )
    .map(([name, roles]) => renderTable({ caption: name, columns: ['Role', 'Value'], rows: Object.entries(roles) }))
    .join(
      '',
    )}<p class="implementation-note">Astronomy foreground values are specified to retain contrast on the existing red surfaces. Watch ambient black is an explicit OLED exception. Measure all final component pairs; these values do not certify an entire screen.</p></section>`;
}

function referencesSection() {
  return `<section class="spec-section references" id="references"><header class="section-heading"><span class="section-number">+</span><div><h2>Sources & repository provenance</h2><p>The system is informed by real product responsibilities and primary platform standards. Existing implementations are evidence for migration, not proof of v2 conformance.</p></div></header><ul>${sources.map((s) => `<li><a href="${esc(s.url)}">${esc(s.name)}</a><p>${esc(s.use)}</p></li>`).join('')}</ul>${renderTable({ caption: 'Repository review · 2 October 2026', columns: ['Reference', 'Observed', 'Design direction'], rows: provenance.map((p) => [p.repository, p.observed, p.direction]) })}<p class="implementation-note">Detailed source paths are included in the JSON manifest and complete plain-text guide. Android and Wear physical-device validation belongs to the later product refactors.</p></section>`;
}

/**
 * The shell every HTML page on the site is built from.
 *
 * A gallery page is not a specification part: it owns no rules and no tokens, so
 * `document()` would have to invent a section list and a plain-text alternate for
 * it. What it does share is everything that makes the page a Cosmos page — the
 * same head, the same theme-restore script before paint, the same navigation,
 * footer and client script — so the head is factored out rather than duplicated.
 * A second copy of the head is a second answer to which scripts, alternate
 * formats and structured data a page publishes, and those are the details a
 * crawler and a screen reader both depend on.
 *
 * The shell owns the page chrome outright: the skip link, the site header and
 * its navigation, the footer and the client script come from here and nowhere
 * else, so a caller passes only the main content. `body` is the `<main>` element
 * and nothing else — a caller that also emits a skip link or a navigation adds a
 * second header to the page, which is what happened once already.
 */
function pageShell({ title, description, canonical, graph, body, alternate, skip, active }) {
  return `<!DOCTYPE html>
<!-- Generated by scripts/build.js. Edit src/design/, never this file. -->
<html lang="en" data-motion="system"><head><meta charset="UTF-8"><script>(function(){var r=document.documentElement;r.classList.add('js');var s=null,m=null;try{s=localStorage.getItem('simt:cosmos-theme');m=localStorage.getItem('simt:cosmos-motion')}catch(e){}if(${PROFILE_GUARD}){r.dataset.cosmosTheme=s}if(${MOTION_GUARD}){r.dataset.motion=m}})();</script><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="color-scheme" content="dark light"><title>${esc(title)}</title><meta name="description" content="${esc(description)}"><meta name="author" content="SIMT"><meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1"><link rel="canonical" href="${canonical}"><link rel="alternate" type="application/atom+xml" title="${esc(space.name)} releases" href="${space.origin}${space.feed}"><link rel="sitemap" type="application/xml" href="/sitemap.xml"><link rel="describedby" type="text/plain" title="Agent discovery" href="${space.origin}/llms.txt"><link rel="describedby" type="text/plain" title="${esc(space.name)}, in full" href="${space.origin}${space.discovery.fullText}"><meta property="og:type" content="article"><meta property="og:site_name" content="${esc(space.name)}"><meta property="og:locale" content="en_US"><meta property="og:url" content="${canonical}"><meta property="og:title" content="${esc(title)}"><meta property="og:description" content="${esc(description)}"><meta property="og:image" content="${space.origin}/images/cosmos-design-system.png"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630"><meta property="og:image:type" content="image/png"><meta property="og:image:secure_url" content="${space.origin}/images/cosmos-design-system.png"><meta property="og:image:alt" content="Cosmos Design System with Deep Space colors and an orbital instrument"><meta property="article:modified_time" content="${release.updated}T00:00:00Z"><meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="${esc(title)}"><meta name="twitter:description" content="${esc(description)}"><meta name="twitter:image" content="${space.origin}/images/cosmos-design-system.png"><meta name="twitter:image:alt" content="Cosmos Design System with Deep Space colors and an orbital instrument"><meta name="theme-color" content="${chromeColor('deepSpace')}" data-color="${chromeColor('deepSpace')}" media="(prefers-color-scheme: dark)"><meta name="theme-color" content="${chromeColor('daylight')}" data-color="${chromeColor('daylight')}" media="(prefers-color-scheme: light)"><link rel="icon" href="/favicon.ico"><link rel="preload" href="/fonts/cosmos/space-grotesk-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin><link rel="preload" href="/fonts/cosmos/source-sans-3-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin>${alternate}<link rel="stylesheet" href="${products[0].stylesheet}"><script type="application/ld+json">${JSON.stringify({ '@context': 'https://schema.org', '@graph': graph }, null, 2).replaceAll('<', '\\u003c')}</script></head>
<body class="cosmos-page"><a class="skip-link" href="#main-content">${esc(skip)}</a>${navigation(active)}${body}<footer class="cosmos-footer"><div><a class="footer-brand" href="${pathFor('')}">SIMT / Cosmos</a><p>You are the center of your universe.</p></div><div><span>Specification ${release.version} · ${release.updated}</span><a href="${PRODUCT_BASE}/manifest.json">Agent resources ↗</a><a href="${space.parent}">SIMT reference parent ↗</a></div></footer><script type="module" src="/assets/cosmos-design.js"></script></body></html>\n`;
}

// Only the page's own transcription is an alternate representation. Reference
// indexes and agent instructions describe the page rather than replace it.
const REFERENCE_LINKS = [
  ['application/json', 'specification.json', 'Specification index'],
  ['application/json', 'rules.json', 'Normative rules'],
  ['application/json', 'tokens.json', 'Design tokens'],
  ['application/json', 'components.json', 'Component index'],
  ['application/json', 'motion.json', 'Motion motifs'],
  ['text/markdown', 'cosmos-design-skill.md', 'Agent design skill'],
]
  .map(
    ([type, path, title]) => `<link rel="describedby" type="${type}" href="${release.base}/${path}" title="${title}">`,
  )
  .join('');

const DOCUMENT_ALTERNATES = (p) =>
  `<link rel="alternate" hreflang="en" type="text/markdown" href="${release.base}/${p ? `${p.slug}.md` : 'specification.md'}" title="Plain-text specification">${REFERENCE_LINKS}`;

function galleryPage(body, { title, description, path, breadcrumb }) {
  const canonical = urlFor(path);
  const graph = [
    {
      '@type': 'WebPage',
      '@id': `${canonical}#webpage`,
      url: canonical,
      name: title,
      description,
      inLanguage: 'en',
      dateModified: release.updated,
      isPartOf: { '@id': `${release.base}#system` },
      mainEntity: { '@id': `${canonical}#guide` },
    },
    {
      '@type': 'CollectionPage',
      '@id': `${canonical}#guide`,
      name: title,
      description,
      datePublished: '2026-07-31',
      dateModified: release.updated,
      author: { '@type': 'Organization', name: 'SIMT', url: 'https://simt.app' },
      publisher: { '@type': 'Organization', name: space.name, url: `${space.origin}/` },
      isPartOf: { '@id': `${release.base}#system` },
      hasPart: GALLERY_FAMILIES.map((family) => ({
        '@type': 'WebPageElement',
        name: family.label,
        url: urlFor(`gallery/${family.id}`),
      })),
    },
    {
      '@type': 'CreativeWork',
      '@id': `${release.base}#system`,
      name: 'Cosmos Design System',
      version: release.version,
      url: release.base,
    },
    {
      '@type': 'BreadcrumbList',
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: space.name, item: `${space.origin}/` },
        { '@type': 'ListItem', position: 2, name: 'Cosmos Design System', item: release.base },
        ...breadcrumb.map((name, index) => ({
          '@type': 'ListItem',
          position: index + 3,
          name,
          item: index === 0 ? urlFor('gallery') : canonical,
        })),
        { '@type': 'ListItem', position: breadcrumb.length + 3, name: title, item: canonical },
      ],
    },
  ];
  return pageShell({
    title: `${title} | Cosmos Design System by SIMT`,
    description,
    canonical,
    graph,
    body,
    alternate: REFERENCE_LINKS,
    skip: 'Skip to the gallery',
    active: GALLERY_SLUG,
  });
}

function document(p = null) {
  const title = p
    ? `${p.label} | Cosmos Design System by SIMT`
    : 'Cosmos Design System | Cross-platform UI specification by SIMT';
  const description =
    p?.description ||
    'Cosmos v2: a shared foundation and four production design specifications for public websites, social web apps, native mobile instruments and simplified watches.';
  const canonical = urlFor(p?.slug);
  const graph = [
    {
      '@type': 'WebPage',
      '@id': `${canonical}#webpage`,
      url: canonical,
      name: title,
      description,
      inLanguage: 'en',
      dateModified: release.updated,
      mainEntity: { '@id': `${canonical}#guide` },
      isPartOf: { '@id': `${release.base}#system` },
    },
    {
      '@type': 'TechArticle',
      '@id': `${canonical}#guide`,
      headline: title,
      description,
      version: release.version,
      datePublished: '2026-07-31',
      dateModified: release.updated,
      author: { '@type': 'Organization', name: 'SIMT', url: 'https://simt.app' },
      publisher: { '@type': 'Organization', name: space.name, url: `${space.origin}/` },
      inLanguage: 'en',
      mainEntityOfPage: { '@id': `${canonical}#webpage` },
      image: `${space.origin}/images/cosmos-design-system.png`,
      proficiencyLevel: 'Implementation reference',
      hasPart: (p ? p.sections : pages).map((x) => ({
        '@type': 'WebPageElement',
        name: x.title.replace('\n', ' '),
        url: p ? `${canonical}#${x.id}` : urlFor(x.slug),
      })),
    },
    {
      '@type': 'CreativeWork',
      '@id': `${release.base}#system`,
      name: 'Cosmos Design System',
      version: release.version,
      url: release.base,
    },
    {
      '@type': 'BreadcrumbList',
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: space.name, item: `${space.origin}/` },
        { '@type': 'ListItem', position: 2, name: 'Cosmos Design System', item: release.base },
        ...(p ? [{ '@type': 'ListItem', position: 3, name: p.label, item: canonical }] : []),
      ],
    },
    ...(!p
      ? [
          {
            '@type': 'FAQPage',
            mainEntity: faqs.map(([name, answer]) => ({
              '@type': 'Question',
              name,
              acceptedAnswer: { '@type': 'Answer', text: answer },
            })),
          },
        ]
      : []),
  ];
  return pageShell({
    title,
    description,
    canonical,
    graph,
    body: p ? detail(p) : overview(),
    alternate: DOCUMENT_ALTERNATES(p),
    skip: 'Skip to specification',
    active: p?.slug ?? null,
  });
}

function markdown(p) {
  return `# ${p.label} — Cosmos ${release.version}\n\nStatus: ${release.status}. Updated: ${release.updated}.\n\nCanonical: ${urlFor(p.slug)}\n\n${p.intro}\n\n${p.slug === 'common' ? 'Inherited by all four platform parts.' : `Inherits [Common foundations](${urlFor('common')}).`}\n\n${p.sections
    .map(
      (s) =>
        `## ${s.title}\n\nSection: ${urlFor(p.slug)}#${s.id}\n\n${s.summary}\n\n${s.rules.map((r) => `### ${r.id} — ${r.title} [${r.level.toUpperCase()}]\n\n${r.requirement}\n\nAcceptance: ${r.acceptance}\n\nRule: ${urlFor(p.slug)}#${r.id.toLowerCase()}\n`).join('\n')}\n${tablesOf(
          s,
        )
          .map(
            (t) =>
              `### ${t.caption}\n\n${t.rows.map((row) => `- ${row.map((v, i) => `${t.columns[i]}: ${v}`).join('; ')}`).join('\n')}\n`,
          )
          .join('')}`,
    )
    .join('\n')}\n`;
}

// The agent catalog is a site-level document, but the entries inside it belong
// to a product: the Cosmos skill and specification index are Cosmos facts, not
// SIMT Space facts.
function agentCatalogEntries({ ruleCount }) {
  return products.flatMap((p) => p.agentEntries({ ruleCount, skillRuleIds }));
}

// A release note may quote a size, but only a measured one: the numbers are read
// off the documents that ship with that release, so a note can never describe the
// build before the change it describes. `noteSizes` is what a `{name}` resolves to.
export function noteSizes(outputs) {
  // A size quoted in a release note is a claim about a document, so a document
  // that is not in the map has to stop the build rather than measure as zero.
  // `Buffer.byteLength(undefined)` used to throw a TypeError that named no file,
  // which is the one failure a reader cannot act on.
  const bytes = (path) => {
    const document = outputs.get(path);
    if (document === undefined) throw new Error(`noteSizes cannot quote ${path}: no such generated document`);
    return Buffer.byteLength(document);
  };
  const kib = (...paths) => `${(paths.reduce((sum, path) => sum + bytes(path), 0) / 1024).toFixed(1)} KiB`;
  const parts = [...outputs.keys()].filter(
    (path) => /\/tokens\.json$/.test(path) && path !== productFile('tokens.json'),
  );
  const payload = parts.map((path) => (bytes(path) + bytes(path.replace(/\/tokens\.json$/, '/rules.json'))) / 1024);
  // `Math.min()` of nothing is Infinity, which would have been published into a
  // note as the literal text "Infinity KiB". A part set that is empty is a
  // generator bug, and the note that would quote it is where it surfaces.
  if (payload.length === 0)
    throw new Error('noteSizes cannot quote a part payload range: no part published a tokens.json');
  return {
    tokens: kib(productFile('tokens.json')),
    // The wrist is the smallest part and the one whose scope is narrowest, so its
    // token payload is quoted on its own: it is the size the filtering is for.
    watchTokens: kib(productFile('watch-app/tokens.json')),
    partPayloadMin: `${Math.min(...payload).toFixed(1)} KiB`,
    partPayloadMax: `${Math.max(...payload).toFixed(1)} KiB`,
    union: kib(productFile('tokens.json'), productFile('rules.json')),
  };
}

const NOTE_SIZE = /\{(\w+)\}/g;

// The releases the specification has published, oldest first, with every note
// rendered against the sizes this build measured. Built from `release.version`
// rather than keyed by it: the current release is the last entry by
// construction, so the version is written once in the whole repository and a
// release cannot be recorded twice or claim to be another one.
export function releaseRecord(sizes) {
  return [...release.history, { version: release.version, updated: release.updated, notes: release.notes }].map(
    (entry) => ({
      version: entry.version,
      updated: entry.updated,
      notes: entry.notes.map((note) =>
        note.replace(NOTE_SIZE, (placeholder, name) => {
          const measured = sizes[name];
          if (measured === undefined)
            throw new Error(
              `Release ${entry.version} interpolates ${placeholder}, which is not a measured document size.`,
            );
          return measured;
        }),
      ),
    }),
  );
}

/** The rendered notes of the release currently being published. */
export function releaseNotes(sizes) {
  return releaseRecord(sizes).at(-1).notes;
}

// The history of every release, newest first. A version nobody shipped is not a
// version here either: its notes belong to the release that carried them.
function changelogMarkdown(sizes) {
  return `# Cosmos changelog\n\nEvery published Cosmos release, newest first. The current release's changes are the first section below.\n\n${releaseRecord(
    sizes,
  )
    .reverse()
    .map(
      ({ version, updated, notes }, index) =>
        `## ${version}${index === 0 ? ' · current' : ''} · ${updated}\n\n${notes.map((note) => `- ${note}`).join('\n')}\n`,
    )
    .join('\n')}`;
}

// Generated once per process. The gate asks for this twice -- build({check:true}) needs the
// contents to compare against disk, and the guard then reads them -- and generation
// is the expensive half of the run, so a second pass is a second answer computed to
// be identical. The map is never mutated outside this function: every write goes
// through put() below, which is why handing the same instance back is safe.
let generated = null;
export function generatedOutputs() {
  if (generated) return generated;
  generated = (() => {
    // The scale tokens only: they are profile-independent. Colors, gradients and
    // shadows follow, one block per profile, so a profile that does not run a
    // gradient resolves it to `none` instead of inheriting Deep Space's (C27).
    const css =
      (generatedCSS = `/* Generated Cosmos ${release.version}. CSS px map to dp in native adapters. */\n:root {\n${brandOnlyAnchors.map(([name, value]) => `  --${name}: ${value};`).join('\n')}\n${scaleTokens(
        tokens.space,
      )
        .map(([k, v]) => `  --space-${k}: ${v.$value.value}px;`)
        .join('\n')}\n${scaleTokens(tokens.radius)
        .map(([k, v]) => `  --radius-${k}: ${v.$value.value}px;`)
        .join('\n')}\n${Object.entries(tokens.motion)
        .filter(([, v]) => v.$type === 'duration')
        .map(([k, v]) => `  --motion-${k}: ${v.$value.value}${v.$value.unit};`)
        .join('\n')}\n  --ease-standard: cubic-bezier(${tokens.motion.easing.$value.join(', ')});\n${Object.entries(
        platformAdapters.webApp.easing,
      )
        .filter(([k]) => k !== 'standard')
        .map(([k, v]) => `  --ease-${k}: cubic-bezier(${v.$value.join(', ')});`)
        .join('\n')}\n${scaleTokens(tokens.layer)
        .map(([k, v]) => `  --layer-${k}: ${v.$value};`)
        .join('\n')}\n}\n\n${themeCSS()}`);
    const exportedTokens = {
      ...tokens,
      starfield: starfieldTokens(),
      adapters: themeColorTokens(),
      platformAdapters,
      // The oklch projection is published once, as real colour tokens, instead of
      // once per web adapter as a block of notation strings. A client reading this
      // document with Style Dictionary or Tokens Studio can emit it; a string it had
      // to parse was a string it would have skipped (C29).
      projection: colourProjection(Object.keys(themeProfiles)),
      // The measured pairs are published once, at contrast-pairs.json. They are the
      // largest single block in the token document and they are only needed when a
      // colour is introduced or a theme is checked, so the token files carry the
      // address and the reason instead of a copy (C05, C22).
      contrastPairs: contrastPairPointer(),
      // The motifs are published once, at motion.json, for the same reason the
      // measured pairs are: a state change is animated often, but a choreography is
      // only needed when one is being chosen or ported, and C31 makes naming a motif
      // the only way to animate. So the token files carry the address and the reason
      // rather than seven copies of the same steps (C05, C22, C31).
      motionMotifs: motionPointer(),
      components: componentsPointer(),
      $extensions: {
        'app.simt.cosmos': {
          version: release.version,
          defaultProfile,
          profiles: PROFILE_NAMES,
          cssThemeAttribute,
          cssRolePrefix,
          pairRatioField: '$value',
          pairGuidanceField: '$extensions.app.simt.cosmos',
        },
      },
    };
    // Every rule and section table is published once, in rules.json. The
    // specification is the index an agent reads to decide what to fetch: it
    // carries the page and section map with rule IDs, not the rule prose, and it
    // points at tokens.json instead of embedding a second copy of the tokens.
    // Embedding both cost roughly 240 KB of JSON — a large share of it a
    // byte-for-byte duplicate of documents that are already addressable.
    const ruleSections = pages.flatMap((p) =>
      p.sections.map((s) => ({
        id: s.id,
        title: s.title,
        summary: s.summary,
        platform: p.slug,
        url: `${urlFor(p.slug)}#${s.id}`,
        ...tableFields(s),
        rules: s.rules.map((r) => ({ ...r, url: `${urlFor(p.slug)}#${r.id.toLowerCase()}` })),
      })),
    );
    const rules = {
      $schema: `${release.base}/rules.schema.json`,
      version: release.version,
      updated: release.updated,
      status: release.status,
      inheritance: 'Platform rules are additive to common; product safety, privacy and accessibility take precedence.',
      normativeLanguage: {
        must: 'Required for conformance.',
        should: 'Expected unless an explicit evidence-backed exception is recorded.',
        may: 'Optional within the common constraints.',
      },
      ruleCount: ruleSections.reduce((sum, s) => sum + s.rules.length, 0),
      sections: ruleSections,
    };
    const pageIndex = pages.map((p) => ({
      slug: p.slug,
      number: p.number,
      label: p.label,
      title: p.title,
      description: p.description,
      url: urlFor(p.slug),
      markdown: `${release.base}/${p.slug}.md`,
      ruleCount: p.sections.reduce((sum, s) => sum + s.rules.length, 0),
      sections: p.sections.map((s) => ({
        id: s.id,
        title: s.title,
        url: `${urlFor(p.slug)}#${s.id}`,
        ruleIds: s.rules.map((r) => r.id),
      })),
    }));
    // Derived from the product's own resource list, because this map and that list
    // are the same twenty-two documents said twice: a payload added to one and not
    // the other is published, budgeted, format-checked and linked from every page,
    // and still absent from the one document an agent is told to load first.
    const resources = Object.fromEntries(
      products[0].resources.map((resource) => [resource.id, `${release.base}/${resource.path}`]),
    );
    // The byte budgets are published next to the documents they limit, because an
    // agent that cannot see the budget cannot tell whether it is being asked to
    // cut a feature or a reference. They are the same numbers verification
    // enforces, read from the product that owns them, not a second table.
    const spec = {
      $schema: `${release.base}/specification.schema.json`,
      ...release,
      // The release notes are published once, under `releaseNotes`, where an agent
      // reads them in order. Carrying the source lists here as well published them
      // twice and left a reader to guess which copy was the changelog.
      notes: undefined,
      history: undefined,
      inheritance: 'Platform rules are additive to common; product safety, privacy and accessibility take precedence.',
      normativeLanguage: rules.normativeLanguage,
      metrics,
      budgets: budgetOf(),
      resources,
      ruleCount: rules.ruleCount,
      pages: pageIndex,
      provenance,
      sources,
    };
    const manifest = {
      version: release.version,
      updated: release.updated,
      status: release.status,
      canonical: release.base,
      language: 'en',
      common: urlFor('common'),
      platforms: pages
        .filter((p) => p.slug !== 'common')
        .map((p) => ({
          id: p.slug,
          label: p.label,
          url: urlFor(p.slug),
          inherits: [urlFor('common')],
          markdown: `${release.base}/${p.slug}.md`,
          sections: p.sections.map((s) => ({ id: s.id, url: `${urlFor(p.slug)}#${s.id}` })),
        })),
      resources,
      gallery: {
        index: urlFor(GALLERY_SLUG),
        summary: 'Components and motion motifs shown as static state matrices, sliced by family.',
        families: GALLERY_FAMILIES.map((family) => ({
          id: family.id,
          label: family.label,
          url: urlFor(`gallery/${family.id}`),
          components: componentsForFamily(family.id).map((component) => component.id),
        })),
      },
      authority: spec.inheritance,
      implementationStatus: release.implementationStatus,
      provenance,
    };
    // The rule contract is the one shape both documents share, so it is defined
    // once and referenced from each schema rather than restated.
    const ruleDef = {
      type: 'object',
      additionalProperties: false,
      required: ['id', 'title', 'level', 'requirement', 'acceptance'],
      properties: {
        id: { type: 'string', pattern: '^[CPAMW][0-9]{2}$' },
        title: { type: 'string', minLength: 1 },
        level: { enum: ['must', 'should', 'may'] },
        requirement: { type: 'string', minLength: 1 },
        acceptance: { type: 'string', minLength: 1 },
      },
    };
    const normativeLanguage = {
      must: 'Required for conformance.',
      should: 'Expected unless an explicit evidence-backed exception is recorded.',
      may: 'Optional within the common constraints.',
    };
    const draft = 'https://json-schema.org/draft/2020-12/schema';
    const schema = {
      $schema: draft,
      $id: `${release.base}/specification.schema.json`,
      title: 'Cosmos specification index',
      description:
        'The map of the Cosmos specification: pages, sections and the rule IDs in each. Rule prose lives in rules.json and tokens in tokens.json; both are listed in resources.',
      type: 'object',
      required: [
        'version',
        'updated',
        'status',
        'implementationStatus',
        'releaseNotes',
        'resources',
        'ruleCount',
        'metrics',
        'budgets',
        'pages',
        'sources',
        'provenance',
      ],
      properties: {
        version: { type: 'string', pattern: '^\\d+\\.\\d+\\.\\d+$' },
        updated: { type: 'string', format: 'date' },
        status: { const: release.status },
        normativeLanguage: { type: 'object' },
        resources: {
          type: 'object',
          required: ['rules', 'tokens'],
          additionalProperties: { type: 'string', format: 'uri' },
        },
        ruleCount: { type: 'integer', minimum: 1 },
        pages: {
          type: 'array',
          // Read from the pages themselves rather than typed: a sixth part would
          // have failed this schema for the wrong reason, and the two numbers were
          // already out of step with what ships.
          minItems: pages.length,
          maxItems: pages.length,
          items: {
            type: 'object',
            required: ['slug', 'label', 'description', 'url', 'ruleCount', 'sections'],
            properties: {
              slug: { enum: pages.map((p) => p.slug) },
              sections: {
                type: 'array',
                minItems: 1,
                items: {
                  type: 'object',
                  required: ['id', 'title', 'url', 'ruleIds'],
                  properties: {
                    id: { type: 'string', pattern: '^[a-z][a-z0-9-]*$' },
                    ruleIds: {
                      type: 'array',
                      minItems: 1,
                      items: { $ref: `${release.base}/rules.schema.json#/$defs/ruleId` },
                    },
                  },
                },
              },
            },
          },
        },
        metrics: { type: 'object' },
        releaseNotes: {
          type: 'object',
          required: ['document', 'current', 'versions', 'count'],
          properties: {
            document: { type: 'string', format: 'uri' },
            current: { type: 'string' },
            versions: { type: 'array', minItems: 1, items: { type: 'string' } },
            count: { type: 'integer', minimum: 1 },
          },
        },
        budgets: { type: 'object', additionalProperties: { type: 'integer', minimum: 1 } },
        sources: { type: 'array' },
        provenance: { type: 'array' },
      },
    };
    const ruleScopes = [
      { id: null, sections: ruleSections },
      ...pages
        .filter((p) => PART_ADAPTERS[p.slug])
        .map((p) => ({
          id: p.slug,
          sections: ruleSections.filter((s) => s.platform === 'common' || s.platform === p.slug),
        })),
    ];
    const rulesSchema = {
      $schema: draft,
      $id: `${release.base}/rules.schema.json`,
      title: 'Cosmos rules',
      description: 'Every normative Cosmos rule with its acceptance criterion, grouped by platform section.',
      type: 'object',
      required: ['version', 'updated', 'status', 'ruleCount', 'sections'],
      properties: {
        version: { type: 'string', pattern: '^\\d+\\.\\d+\\.\\d+$' },
        updated: { type: 'string', format: 'date' },
        status: { const: release.status },
        normativeLanguage: { type: 'object' },
        ruleCount: { type: 'integer', minimum: 1 },
        platform: {
          type: 'object',
          required: ['id', 'label'],
          properties: {
            id: { enum: ruleScopes.filter((scope) => scope.id).map((scope) => scope.id) },
            label: { type: 'string', minLength: 1 },
          },
        },
        sections: {
          type: 'array',
          items: {
            type: 'object',
            required: ['id', 'title', 'platform', 'url', 'rules'],
            properties: {
              id: { type: 'string', pattern: '^[a-z][a-z0-9-]*$' },
              platform: { enum: pages.map((p) => p.slug) },
              table: { type: 'object' },
              tables: { type: 'array', minItems: 1, items: { type: 'object' } },
              rules: {
                type: 'array',
                minItems: 1,
                items: {
                  type: 'object',
                  required: ['id', 'title', 'level', 'requirement', 'acceptance'],
                  properties: { ...ruleDef.properties, url: { type: 'string', format: 'uri' } },
                  required: ruleDef.required,
                },
              },
            },
          },
        },
      },
      oneOf: ruleScopes.map(({ id, sections }) => ({
        ...(id ? { required: ['platform'] } : { not: { required: ['platform'] } }),
        properties: {
          ...(id ? { platform: { type: 'object', properties: { id: { const: id } } } } : {}),
          sections: {
            type: 'array',
            minItems: sections.length,
            maxItems: sections.length,
            allOf: sections.map((section) => ({
              contains: {
                type: 'object',
                required: ['id', 'platform'],
                properties: { id: { const: section.id }, platform: { const: section.platform } },
              },
              minContains: 1,
              maxContains: 1,
            })),
          },
        },
      })),
      $defs: { ruleId: { type: 'string', pattern: '^[CPAMW][0-9]{2}$' } },
    };
    // The measured composited contrast lives at its own address. Both the canonical
    // token document and every part payload point at it and say when it is needed, so
    // an agent that is laying out or spacing never pays for it (C05, C22).
    function contrastPairPointer() {
      return {
        $ref: `${release.base}/contrast-pairs.json`,
        fetchWhen: CONTRAST_FETCH_WHEN,
        $description: 'Measured composited contrast per theme profile, with the guidance for each pair.',
      };
    }
    function motionPointer() {
      return {
        $ref: `${release.base}/motion.json`,
        fetchWhen: MOTION_FETCH_WHEN,
        $description:
          'The named choreographies a state change animates with, per tier and per platform, with the curve parity fixture.',
      };
    }
    // The pointer every token document carries. The registry stays outside the part
    // payloads: it is references, so including it would grow every payload with
    // material each one would immediately filter back out, and a payload that
    // carries the union is the failure this architecture exists to prevent.
    function componentsPointer() {
      return {
        $ref: `${release.base}/components.json`,
        fetchWhen: COMPONENTS_FETCH_WHEN,
        $description:
          'The component index: which rules each component inherits, the theme roles and corner role it consumes, the states it can be in and the motif it moves with, sliced per platform part.',
      };
    }
    const partPayloads = pages
      .filter((p) => PART_ADAPTERS[p.slug])
      .map((p) => {
        const adapterKey = PART_ADAPTERS[p.slug];
        const adapter = platformAdapters[adapterKey];
        const scope = partScope[adapterKey];
        const colorSpace = partColorSpace[adapterKey];
        // Self-contained, and scoped to what this part actually runs. A payload must
        // never grow into the union, so the foreign adapters stay in tokens.json only;
        // and it must never carry a profile, a gradient or a star layer that the part's
        // own rules rule out, because a block an agent cannot use is a block it pays to
        // read. The scope is published in $extensions so an absent group is a decision
        // on the record rather than a gap to guess about.
        const keepGradient = (gradient) =>
          scope.gradients.includes(gradient.name) &&
          gradient.profiles.some((profile) => scope.profiles.includes(profile));
        const tokens = {
          $description: `Cosmos ${release.version} tokens for the ${p.label} part: the common foundations, the ${p.label} mapping (platformAdapters.${adapterKey}), and only the ${scope.profiles.join(', ')} profile${scope.profiles.length === 1 ? '' : 's'} this part runs. The union of every adapter and profile is tokens.json.`,
          color: exportedTokens.color,
          gradient: Object.fromEntries(
            Object.entries(exportedTokens.gradient).filter(([name]) =>
              keepGradient(gradients.find((g) => g.name === name)),
            ),
          ),
          shadow: exportedTokens.shadow,
          space: exportedTokens.space,
          radius: exportedTokens.radius,
          motion: exportedTokens.motion,
          font: exportedTokens.font,
          layer: exportedTokens.layer,
          ...(scope.stars ? { starfield: exportedTokens.starfield } : {}),
          adapters: Object.fromEntries(
            Object.entries(exportedTokens.adapters).filter(([profile]) => scope.profiles.includes(profile)),
          ),
          platformAdapters: { $description: platformAdapters.$description, [adapterKey]: adapter },
          // Only the web parts can emit an oklch value, so only they carry it (C29).
          ...(colorSpace === 'oklch' ? { projection: colourProjection(scope.profiles) } : {}),
          contrastPairs: contrastPairPointer(),
          // The motifs are published once, at motion.json, for the same reason the
          // measured pairs are: a state change is animated often, but a choreography is
          // only needed when one is being chosen or ported, and C31 makes naming a motif
          // the only way to animate. So the token files carry the address and the reason
          // rather than seven copies of the same steps (C05, C22, C31).
          motionMotifs: motionPointer(),
          components: componentsPointer(),
          $extensions: {
            'app.simt.cosmos': {
              document: { version: release.version, updated: release.updated, status: release.status },
              part: { id: p.slug, label: p.label, adapter: adapterKey },
              scope: { ...scope, colorSpace, starfield: scope.stars },
            },
            ...exportedTokens.$extensions['app.simt.cosmos'],
          },
        };
        const sections = ruleSections.filter((s) => s.platform === 'common' || s.platform === p.slug);
        const partRules = {
          $schema: `${release.base}/rules.schema.json`,
          $description: `Cosmos ${release.version} rules for the ${p.label} part: the common foundations and this part's own sections. Every part is in rules.json.`,
          version: release.version,
          updated: release.updated,
          status: release.status,
          platform: { id: p.slug, label: p.label },
          inheritance: rules.inheritance,
          normativeLanguage: rules.normativeLanguage,
          ruleCount: sections.reduce((sum, s) => sum + s.rules.length, 0),
          sections,
        };
        return {
          slug: p.slug,
          label: p.label,
          adapter: adapterKey,
          scope,
          colorSpace,
          tokens,
          rules: partRules,
          tokensPath: productFile(`${p.slug}/tokens.json`),
          rulesPath: productFile(`${p.slug}/rules.json`),
        };
      });

    const common = pages.find((p) => p.slug === 'common');
    manifest.foundation = {
      id: common.slug,
      url: urlFor(common.slug),
      markdown: `${release.base}/common.md`,
      ruleIds: common.sections.flatMap((s) => s.rules.map((r) => r.id)),
      sections: common.sections.map((s) => ({ id: s.id, url: `${urlFor(common.slug)}#${s.id}` })),
    };
    manifest.ruleCount = pages.reduce((sum, p) => sum + p.sections.reduce((n, s) => n + s.rules.length, 0), 0);
    manifest.platforms.forEach((p) => {
      p.ruleIds = pages.find((source) => source.slug === p.id).sections.flatMap((s) => s.rules.map((r) => r.id));
    });
    // An agent that knows its part fetches the part payload; the canonical union
    // stays the address of last resort. Both are published, so the choice is the
    // agent's and neither URL is a redirect.
    for (const payload of partPayloads) {
      const entry = manifest.platforms.find((p) => p.id === payload.slug);
      entry.adapter = payload.adapter;
      entry.tokens = `${release.base}/${payload.slug}/tokens.json`;
      entry.rules = `${release.base}/${payload.slug}/rules.json`;
      const index = spec.pages.find((p) => p.slug === payload.slug);
      index.tokens = entry.tokens;
      index.rules = entry.rules;
    }
    const template = {
      specificationVersion: release.version,
      product: '',
      build: '',
      owner: '',
      platform: '',
      status: 'pending',
      environment: {
        device: '',
        browserOrOS: '',
        viewportOrWindow: '',
        locale: '',
        input: '',
        motion: '',
        theme: '',
        network: '',
      },
      applicableRuleIds: [],
      evidence: { screenshots: [], accessibility: [], performance: [], stateJourneys: [] },
      contractDependencies: [],
      exceptions: [],
      acceptanceDate: null,
    };
    // Every published path is relative to dist/, which is what the Worker serves.
    // tokens.css and styles/cosmos-tokens.less are the same document: one is
    // published for consumers, the other is imported by cosmos-design.less.
    const literal = [
      [productFile('index.html'), document()],
      [productFile('tokens.css'), css],
      [
        productFile('tokens.json'),
        json({
          $description: `Cosmos ${release.version} base tokens, theme adapters and contrast pairs`,
          ...exportedTokens,
        }),
      ],
      [
        productFile('rules.json'),
        json({
          $description: `Cosmos ${release.version} normative rules with acceptance criteria, grouped by platform section`,
          ...rules,
        }),
      ],
      [
        productFile('contrast-pairs.json'),
        json({
          $description: `Cosmos ${release.version} composited contrast for every role pair in every theme profile`,
          version: release.version,
          updated: release.updated,
          status: release.status,
          ratioField: '$value',
          guidanceField: '$extensions.app.simt.cosmos',
          fetchWhen: CONTRAST_FETCH_WHEN,
          profiles: Object.keys(adapters),
          contrastPairs: buildContrastPairs(adapters),
        }),
      ],
      [
        productFile('components.json'),
        json({
          $description: `Cosmos ${release.version} component index: for every component the rules it inherits, the theme roles, layer and corner role it consumes, the states it can be in, the motif that animates it and the counterpart that already exists in each product. References only — every treatment stays in the rules the entries name. Sliced by platform part under parts.`,
          version: release.version,
          updated: release.updated,
          status: release.status,
          fetchWhen: COMPONENTS_FETCH_WHEN,
          referencesOnly: true,
          roles: { theme: THEME_ROLES, layer: COMPONENT_LAYERS, radius: COMPONENT_RADII },
          states: COMPONENT_STATES,
          notApplicable: NOT_APPLICABLE,
          parts: Object.fromEntries(
            COMPONENT_PARTS.map((part) => [
              part,
              {
                label: PART_LABELS[part],
                components: componentRegistry
                  .filter((component) => component.parts.includes(part))
                  .map((component) => component.id),
              },
            ]),
          ),
          components: componentRegistry.map((component) => ({
            ...component,
            timing: motionRolesFor(component),
            notApplicable: componentApplicability[component.id],
          })),
        }),
      ],
      [
        productFile('motion.json'),
        json({
          $description: `Cosmos ${release.version} named motion motifs: the trigger, steps, allowed properties, duration, easing and per-tier and per-platform behaviour of every state change the system animates, with the sampled curve values a native port reproduces.`,
          version: release.version,
          updated: release.updated,
          status: release.status,
          fetchWhen: MOTION_FETCH_WHEN,
          properties: MOTION_PROPERTIES,
          durations: Object.fromEntries(MOTION_DURATIONS.map((role) => [role, durationFor('webPage', role)])),
          easingPurposes: Object.fromEntries(
            Object.entries(EASING_PURPOSES).map(([purpose, { label, keys }]) => [
              purpose,
              { label, web: easingFor('webPage', purpose), native: easingFor('android', purpose) },
            ]),
          ),
          tiers: ['full', 'reduced', 'static'],
          motifs: motionMotifs,
          parityFixture: { algorithm: motionParityAlgorithm, ...motionFixture() },
        }),
      ],
      [productFile('manifest.json'), json(manifest)],
      [productFile('specification.schema.json'), json(schema)],
      [productFile('rules.schema.json'), json(rulesSchema)],
      [productFile('release-template.json'), json(template)],
      [productFile('cosmos-design-skill.md'), skillMarkdown()],
      [HERO_SKY, heroSky() + '\n'],
    ];
    // A Map built from a literal keeps the first entry for a repeated key and drops
    // the second without a word, so two generators claiming one path used to mean
    // the later one silently vanished: the page published was whichever ran first.
    // Counting the entries is the only way to notice.
    const outputs = new Map(literal);
    if (outputs.size !== literal.length) {
      const claimed = new Set();
      const twice = literal
        .map(([path]) => path)
        .filter((path) => (claimed.has(path) ? true : (claimed.add(path), false)));
      throw new Error(`Two generators claim ${twice.join(', ')}; the second copy would be dropped without a word`);
    }
    const put = (path, contents) => {
      if (outputs.has(path))
        throw new Error(`Two generators claim ${path}; the second copy would be dropped without a word`);
      outputs.set(path, contents);
    };
    const starfieldNotes = `## Starfield reference\n\nThe algorithm behind C11. Reproduce the \`starfield\` fixture in tokens.json (surface \`${starfieldFixture.surfaceId}\`, ${starfieldFixture.width} × ${starfieldFixture.height}) before shipping a port.\n\n${starfieldAlgorithm.map((step, i) => `${i + 1}. ${step}`).join('\n')}\n`;
    // The reference tail, written once. It closes the common part's Markdown and the
    // whole specification's, and it used to be two template literals that agreed
    // only until one of them was edited -- so an edit to the starfield notes, the
    // adapter table or the source list could land in one document and leave the
    // other quietly stating the previous version of the same reference.
    const referenceTail = () =>
      `${starfieldNotes}\n## Theme adapters\n\n${Object.entries(adapters)
        .map(
          ([name, roles]) =>
            `### ${name}\n\n${Object.entries(roles)
              .map(([k, v]) => `- ${k}: ${v}`)
              .join('\n')}\n`,
        )
        .join('\n')}\n## Reference sources\n\n${sources.map((s) => `- [${s.name}](${s.url}): ${s.use}`).join('\n')}\n`;
    // Each part's prose is rendered once and read by both documents that carry it:
    // the part's own Markdown and the specification's. Transcribing ~79 KiB of rule
    // prose twice per build is cheap, and the second copy is one more place a rule
    // can differ from the one the HTML page publishes.
    const partMarkdown = new Map(pages.map((p) => [p.slug, markdown(p)]));
    for (const payload of partPayloads) {
      put(payload.tokensPath, json(payload.tokens));
      put(payload.rulesPath, json(payload.rules));
    }
    for (const p of pages) {
      put(productFile(`${p.slug}/index.html`), document(p));
      put(
        productFile(`${p.slug}.md`),
        partMarkdown.get(p.slug) + (p.slug === 'common' ? `\n${referenceTail()}` : ''),
      );
    }
    // One gallery route per family, plus the index that lists them. The pages are
    // static HTML: a state matrix and a filmstrip are static, so a card is legible
    // with no client JavaScript and in the Static tier, and the tier picker only
    // decides whether the demo is allowed to run. C27 is kept because nothing here
    // draws a gradient of its own — every fill comes from a published gradient or
    // a theme role, the same rule the specimens follow.
    put(
      productFile(`${GALLERY_SLUG}/index.html`),
      galleryPage(galleryIndex(), {
        title: 'Component gallery',
        description:
          'Every Cosmos component with its state matrix, its motion filmstrip and the rules it applies, as static HTML that reads in every theme and without JavaScript.',
        path: GALLERY_SLUG,
        breadcrumb: [],
      }),
    );
    for (const family of GALLERY_FAMILIES) {
      put(
        productFile(`${GALLERY_SLUG}/${family.id}/index.html`),
        galleryPage(galleryFamily(family.id), {
          title: `${family.label} gallery`,
          description: `The ${family.label} components in Cosmos, with the state matrix, motion filmstrip and rule IDs each one applies.`,
          path: `${GALLERY_SLUG}/${family.id}`,
          breadcrumb: ['Gallery'],
        }),
      );
    }

    // The release notes quote document sizes, so they are rendered last: the sizes
    // are measured from the bytes the rest of this build produced.
    const sizes = noteSizes(outputs);
    const record = releaseRecord(sizes);
    const currentNotes = record.at(-1).notes;
    // The index carries where the release notes live, not the notes. They had grown
    // to be the larger half of this document, and the ceiling this file is held to
    // exists so the index stays a map of the specification rather than its prose.
    put(
      productFile('specification.json'),
      json({
        ...spec,
        // The same removal, on the second document that used to carry them.
        notes: undefined,
        history: undefined,
        releaseNotes: {
          document: `${release.base}/changelog.md`,
          current: release.version,
          versions: record.map((entry) => entry.version).reverse(),
          count: currentNotes.length,
        },
      }),
    );
    put(
      productFile('specification.md'),
      `# Cosmos Design System ${release.version}\n\n${release.scope}\n\n${release.implementationStatus}\n\n## What changed in ${release.version}\n\nThe ${currentNotes.length} changes in this release are listed in [changelog.md](${release.base}/changelog.md), which carries every published release newest first.\n\n${pages.map((p) => partMarkdown.get(p.slug)).join('\n---\n\n')}\n${referenceTail()}\n## Repository provenance\n\n${provenance.map((p) => `- ${p.repository}: ${p.paths.join(', ')}. Observed: ${p.observed} Target: ${p.direction}`).join('\n')}\n`,
    );
    put(productFile('changelog.md'), changelogMarkdown(sizes));
    // The envelope is generated from the registry after the product documents:
    // full-text discovery embeds the completed references, never a second index.
    for (const [path, contents] of siteOutputs({
      agentEntries: agentCatalogEntries({ ruleCount: rules.ruleCount }),
      documents: outputs,
    }))
      put(path, contents);
    return outputs;
  })();
  return generated;
}

function writeGenerated(absolute, contents) {
  mkdirSync(dirname(absolute), { recursive: true });
  writeFileSync(absolute, contents);
}

function assertGenerated(absolute, contents) {
  let actual;
  try {
    actual = readFileSync(absolute, 'utf8');
  } catch {
    throw new Error(`Missing generated file: ${absolute}`);
  }
  if (actual !== contents) throw new Error(`Generated output drift: ${absolute}. Run npm run build.`);
}

export function build({ check = false } = {}) {
  const outputs = generatedOutputs();
  // cosmos-tokens.less is repo source, not a published asset: cosmos-design.less
  // imports it and the Less compiler reads it from here.
  if (check) {
    assertGenerated(resolve(root, 'styles/cosmos-tokens.less'), generatedCSS);
  } else {
    // dist/ is fully owned by this build. A file left by an earlier release
    // would otherwise be published forever, since the assets Worker serves
    // whatever is in the directory.
    rmSync(DIST, { recursive: true, force: true });
    writeGenerated(resolve(root, 'styles/cosmos-tokens.less'), generatedCSS);
  }
  for (const [path, contents] of outputs) {
    const absolute = resolve(DIST, path);
    if (check) assertGenerated(absolute, contents);
    else writeGenerated(absolute, contents);
  }
  return outputs.size;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  console.log(
    `Cosmos: ${build({ check: process.argv.includes('--check') })} generated files ${process.argv.includes('--check') ? 'verified' : 'written'}.`,
  );
}
