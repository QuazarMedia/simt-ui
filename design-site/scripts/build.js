import { esc, siteOutputs } from './site.js';
import { budgetOf, latest, products, versions } from '../src/site/product.js';
import { authority as space } from '../src/site/authority.js';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createSpecimens, MOTION_TIERS } from '../src/design/specimens.js';
import { createSkill, skillRuleIds } from '../src/design/skill.js';
import { createGallery } from '../src/design/gallery.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
// Everything published lands under dist/, which is the Workers assets directory.
const DIST = resolve(root, 'dist');
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
export const HERO_SKY = 'sky-hero.svg';
const sky = '<div class="sky" aria-hidden="true"></div>';
// Filled by generatedOutputs() from the latest product; tokens.less is written from it as well.
let generatedCSS = '';

const THEME_LABELS = {
  deepSpace: 'Deep Space',
  daylight: 'Daylight',
  astronomy: 'Astronomy red',
  watchAmbient: 'Watch ambient',
};
const MOTION_GUARD = MOTION_TIERS.map(({ value }) => `m==='${value}'`).join('||');
const GALLERY_SLUG = 'gallery';
const CONTRAST_FETCH_WHEN = [
  'introducing a colour or gradient',
  'pairing a translucent, blurred or layered surface',
  'checking or changing a theme profile',
];
const NOTE_SIZE = /\{(\w+)\}/g;

const tablesOf = (s) => (s.table === undefined || s.table === null ? [] : Array.isArray(s.table) ? s.table : [s.table]);
function tableFields(s) {
  const tables = tablesOf(s);
  if (tables.length === 1) return { table: tables[0] };
  if (tables.length === 0) return {};
  return { tables };
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

function renderSection(s, index) {
  return `<section class="spec-section" id="${s.id}" aria-labelledby="${s.id}-title"><header class="section-heading"><span class="section-number">${String(index + 1).padStart(2, '0')}</span><div><h2 id="${s.id}-title">${esc(s.title)}</h2><p>${esc(s.summary)}</p></div></header><ol class="rule-list">${s.rules.map((r) => `<li class="rule" id="${r.id.toLowerCase()}" data-level="${r.level}"><div class="rule-marker"><a href="#${r.id.toLowerCase()}" aria-label="Link to rule ${r.id}">${r.id}</a><span class="level-${r.level}">${r.level.toUpperCase()}</span></div><div class="rule-body"><h3>${esc(r.title)}</h3><p>${esc(r.requirement)}</p><div class="acceptance"><span>Acceptance</span><p>${esc(r.acceptance)}</p></div></div></li>`).join('')}</ol>${tablesOf(s).map(renderTable).join('')}</section>`;
}

export function noteSizes(outputs, baseDir = '') {
  const productFile = (path) => (baseDir ? `${baseDir}/${path}` : path);
  const bytes = (path) => {
    const document = outputs.get(path);
    if (document === undefined) throw new Error(`noteSizes cannot quote ${path}: no such generated document`);
    return Buffer.byteLength(document);
  };
  const kib = (...paths) => `${(paths.reduce((sum, path) => sum + bytes(path), 0) / 1024).toFixed(1)} KiB`;
  const parts = [...outputs.keys()].filter(
    (path) => path.startsWith(baseDir ? `${baseDir}/` : '') && /\/tokens\.json$/.test(path) && path !== productFile('tokens.json'),
  );
  const payload = parts.map((path) => (bytes(path) + bytes(path.replace(/\/tokens\.json$/, '/rules.json'))) / 1024);
  if (payload.length === 0)
    throw new Error('noteSizes cannot quote a part payload range: no part published a tokens.json');
  return {
    tokens: kib(productFile('tokens.json')),
    watchTokens: kib(productFile('watch-app/tokens.json')),
    partPayloadMin: `${Math.min(...payload).toFixed(1)} KiB`,
    partPayloadMax: `${Math.max(...payload).toFixed(1)} KiB`,
    union: kib(productFile('tokens.json'), productFile('rules.json')),
  };
}

export function releaseRecord(sizes, release = products[0].spec.release) {
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

export function releaseNotes(sizes, release = products[0].spec.release) {
  return releaseRecord(sizes, release).at(-1).notes;
}

function changelogMarkdown(sizes, release) {
  return `# Design changelog\n\nEvery published Design release, newest first. The current release's changes are the first section below.\n\n${releaseRecord(
    sizes,
    release,
  )
    .reverse()
    .map(
      ({ version, updated, notes }, index) =>
        `## ${version}${index === 0 ? ' · current' : ''} · ${updated}\n\n${notes.map((note) => `- ${note}`).join('\n')}\n`,
    )
    .join('\n')}`;
}

function generateProductCss(PRODUCT) {
  const { spec } = PRODUCT;
  const {
    colors,
    cssRoleAliases,
    tokens,
    platformAdapters,
    themeCSS,
  } = spec.tokens;
  const brandOnlyAnchors = colors.filter(([name]) => !(name in cssRoleAliases));
  const scaleTokens = (group) => Object.entries(group).filter(([key]) => !key.startsWith('$'));
  return `/* Generated Design ${spec.release.version}. CSS px map to dp in native adapters. */\n:root {\n${brandOnlyAnchors.map(([name, value]) => `  --${name}: ${value};`).join('\n')}\n${scaleTokens(
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
    .join('\n')}\n}\n\n${themeCSS()}`;
}

function generateProductOutputs(PRODUCT) {
  const { spec } = PRODUCT;
  const {
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
  } = spec.tokens;
  const { buildContrastPairs } = spec.contrast;
  const { pages, release, sources, provenance, faqs } = spec;
  const { orbit, specimen, workbench, lightGrammar, observatory } = createSpecimens(spec);
  const { skillMarkdown } = createSkill(spec, budgetOf());
  const { starfieldSVG, starfieldTokens, starfieldAlgorithm, starfieldFixture } = spec.starfield;
  const heroSky = () =>
    starfieldSVG('design.hero', 1440, 760, {
      exclude: [
        [0.03, 0.14, 0.47, 0.9],
        [0.4, 0.06, 0.85, 0.97],
      ],
    }).replace('<svg ', '<svg width="1440" height="760" ');
  const {
    durationFor,
    EASING_PURPOSES,
    easingFor,
    MOTION_DURATIONS,
    MOTION_FETCH_WHEN,
    MOTION_PROPERTIES,
    motionFixture,
    motionMotifs,
    motionParityAlgorithm,
  } = spec.motion;
  const { galleryIndex, galleryFamily, GALLERY_FAMILIES } = createGallery(spec);
  const {
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
  } = spec.components;

  const PRODUCT_BASE = PRODUCT.basePath;
  const PRODUCT_DIR = PRODUCT.basePath.replace(/^\//, '');
  const productFile = (path) => (PRODUCT_DIR ? `${PRODUCT_DIR}/${path}` : path);
  const pathFor = (slug) => (slug ? (PRODUCT_BASE ? `${PRODUCT_BASE}/${slug}` : `/${slug}`) : PRODUCT_BASE || '/');
  const urlFor = (slug) => (slug ? `${PRODUCT.base}/${slug}` : PRODUCT.base);
  const versionSuffix = PRODUCT.isLatest ? '' : ` (v${release.version})`;

  const PROFILE_NAMES = Object.keys(themeProfiles);
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
  const chromeColor = (profile) => {
    const background = themeProfiles[profile]?.background;
    if (!/^#[0-9A-F]{6}$/i.test(background ?? '')) {
      throw new Error(
        `theme profile ${profile} has background ${background}, which is not a six-digit hex the browser chrome can be painted`,
      );
    }
    return background;
  };
  const PART_ADAPTERS = Object.fromEntries(
    Object.entries(PART_SLUGS)
      .filter(([adapter]) => adapter !== 'common')
      .map(([adapter, slug]) => [slug, adapter]),
  );

  function versionHref(targetVersion, slug) {
    const base = targetVersion === latest ? '' : `/v/${targetVersion}`;
    return slug ? `${base}/${slug}` : base || '/';
  }

  function navigation(slug, versionSlug = slug) {
    return `<header class="design-header"><div class="header-inner"><a class="brand" href="${pathFor('')}" aria-label="Design home"><img src="${space.logo}" width="32" height="32" alt=""><span>SIMT <b>Design</b></span></a><div class="header-controls"><span class="version-label">SPECIFICATION ${release.version}</span><label class="version-picker"><select data-version-select aria-label="Specification version">${versions
      .map(
        (v) =>
          `<option value="${versionHref(v, versionSlug)}"${v === release.version ? ' selected' : ''}>v${v}${v === latest ? ' (latest)' : ''}</option>`,
      )
      .join('')}</select></label><label class="theme-picker"><select data-theme-select aria-label="Theme profile"><option value="">Follow system</option>${Object.keys(
      themeProfiles,
    )
      .map((name) => `<option value="${name}" data-color="${chromeColor(name)}">${THEME_LABELS[name]}</option>`)
      .join(
        '',
      )}</select></label><a class="header-download" href="${PRODUCT_BASE}/specification.md">Plain text <span aria-hidden="true">↗</span></a></div></div><nav class="platform-nav" aria-label="Design parts"><a href="${pathFor('')}"${!slug ? ' aria-current="page"' : ''}>Overview</a>${pages.map((p) => `<a href="${pathFor(p.slug)}"${slug === p.slug ? ' aria-current="page"' : ''}><span>${p.number}</span>${p.label}</a>`).join('')}<a href="${PRODUCT_BASE}/gallery"${slug === GALLERY_SLUG ? ' aria-current="page"' : ''}>Gallery</a></nav></header>`;
  }

  function partPayloadNote() {
    return pages
      .filter((p) => PART_ADAPTERS[p.slug])
      .map(
        (p) =>
          `<li><a href="${PRODUCT_BASE}/${p.slug}/tokens.json">${p.label} tokens</a> and <a href="${PRODUCT_BASE}/${p.slug}/rules.json">rules</a> <span>JSON · common foundations plus the ${esc(p.label)} mapping only</span></li>`,
      )
      .join('');
  }

  function downloads() {
    return `<section class="downloads" id="implementation" aria-labelledby="downloads-title"><div><p class="eyebrow">BUILT FOR IMPLEMENTATION</p><h2 id="downloads-title">Human-readable. Machine-ready.</h2><p>Every page and every rule is generated from one source. Load the common contract, then the platform you are building.</p></div><ul><li><a href="${PRODUCT_BASE}/manifest.json">Resource manifest <span>JSON · routes, versions, inheritance</span></a></li><li><a href="${PRODUCT_BASE}/specification.json">Specification index <span>JSON · pages, sections, rule IDs</span></a></li><li><a href="${PRODUCT_BASE}/rules.json">Normative rules <span>JSON · requirements and acceptance</span></a></li><li><a href="${PRODUCT_BASE}/tokens.json">Typed design tokens <span>JSON · exact colors and units</span></a></li><li><a href="${PRODUCT_BASE}/tokens.css">Web token variables <span>CSS · copy into a platform theme</span></a></li><li><a href="${PRODUCT_BASE}/specification.md">Complete plain-text guide <span>Markdown · readable without scripts</span></a></li><li><a href="${PRODUCT_BASE}/simt-design-skill.md">Agent design skill <span>Markdown · the failures generated UI makes</span></a></li><li><a href="${PRODUCT_BASE}/contrast-pairs.json">Composited contrast <span>JSON · measured role pairs per theme; fetch when a colour or theme is checked</span></a></li><li><a href="${PRODUCT_BASE}/specification.schema.json">Specification schema <span>JSON Schema · structural validation</span></a></li><li><a href="${PRODUCT_BASE}/changelog.md">Release changelog <span>Markdown · what each published version changed</span></a></li><li><a href="${PRODUCT_BASE}/motion.json">Motion motifs <span>JSON · named choreographies per tier and platform, with the curve parity fixture</span></a></li><li><a href="${PRODUCT_BASE}/components.json">Component index <span>JSON · rules, roles, states and native counterparts per component, sliced by platform</span></a></li></ul><ul class="part-payloads">${partPayloadNote()}</ul></section>`;
  }

  function overview() {
    return `<main id="main-content"><section class="overview-hero">${sky}<div class="hero-copy"><p class="eyebrow">A DESIGN LANGUAGE BY SIMT</p><h1>A universe<br>around <em>you.</em></h1><p class="hero-lede">Warm depth. Clear signals. A little wonder.<br>Interfaces that make the next step feel natural.</p><div class="hero-actions"><a class="button button-primary" href="${PRODUCT_BASE}/common">Start with the foundations <span aria-hidden="true">↗</span></a><a class="button button-secondary" href="#observatory">Open live observatory <span aria-hidden="true">↓</span></a><a class="text-link" href="#platforms">Four platforms <span aria-hidden="true">↓</span></a></div><div class="hero-meta"><span>Deep Space palette</span><span>Light with a job</span><span>One deterministic sky</span></div></div><div class="hero-art">${orbit}<span class="art-note">DESIGN THAT ORIENTS, REVEALS & RESPONDS</span></div></section>
  <div class="status-strip"><span class="status-indicator" aria-hidden="true"></span><p><strong>v${release.version} · ${esc(release.status)}</strong> Shared foundation + four platform parts. Updated <time datetime="${release.updated}">${longDate(release.updated)}</time>.</p></div>
  ${observatory()}
  <section class="identity-intro" id="foundations"><p class="eyebrow">SAME COLORS. A RICHER COMPOSITION.</p><div><h2>Clarity is the center.<br>Wonder follows.</h2><p>The Design idea stays: you are the center of your universe. The new system gives it a precise visual grammar—an aperture for attention, a spine for context, a horizon for time. It creates continuity without making every screen look the same.</p><p>One common contract governs color, type, accessibility and meaning. Four platform parts translate it into distinct experiences.</p></div>${specimen('common')}<a class="text-link" href="${PRODUCT_BASE}/common">Read the shared contract <span aria-hidden="true">↗</span></a></section>
  <section class="light-showcase" id="light" aria-labelledby="light-title"><div class="section-heading"><span class="section-number">+</span><div><h2 id="light-title">Light with a job.</h2><p>Eight gradients drawn only from the Deep Space anchors. Each has one job, so depth reads as meaning instead of decoration.</p></div></div>${lightGrammar()}<a class="text-link" href="${PRODUCT_BASE}/common#light">Read the gradient grammar <span aria-hidden="true">↗</span></a></section>
  <section class="platforms" id="platforms" aria-labelledby="platforms-title"><div class="section-heading"><span class="section-number">01–04</span><div><h2 id="platforms-title">Four expressions.<br>One unmistakable system.</h2><p>Each platform earns its own composition, density and rhythm.</p></div></div>${pages
    .filter((p) => p.slug !== 'common')
    .map(
      (p) =>
        `<article class="platform-feature"><div class="platform-copy"><p class="eyebrow">${p.number} / ${p.label.toUpperCase()}</p><h3>${esc(p.title).replace('\n', '<br>')}</h3><p>${esc(p.intro)}</p><a class="text-link" href="${pathFor(p.slug)}">Open ${p.label.toLowerCase()} specification <span aria-hidden="true">↗</span></a></div><figure class="specimen specimen-${p.slug}">${specimen(p.specimen)}<figcaption>Design specimen · illustrative data · static composition</figcaption></figure></article>`,
    )
    .join('')}</section>
  <section class="principles" id="patterns"><p class="eyebrow">THE PRODUCTION CONTRACT</p><h2>Beautiful is only the beginning.</h2><div class="principle-grid"><article><span>01</span><h3>Every state designed</h3><p>Loading, empty, denied, stale, offline, conflict and success have explicit behavior.</p></article><article><span>02</span><h3>Access built in</h3><p>Readable contrast, full input support, large text, logical RTL and reduced motion.</p></article><article><span>03</span><h3>Performance measured</h3><p>Asset budgets, frame pacing, lifecycle ownership and named release evidence.</p></article></div><p class="implementation-note">${esc(release.implementationStatus)}</p></section>
  ${downloads()}<section class="faq" aria-labelledby="faq-title"><p class="eyebrow">USING SIMT DESIGN</p><h2 id="faq-title">A few useful answers.</h2>${faqs.map(([q, a]) => `<details><summary>${esc(q)}</summary><p>${esc(a)}</p></details>`).join('')}</section></main>`;
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
    return `<main id="main-content"><div class="breadcrumb"><a href="${pathFor('')}">Design</a><span aria-hidden="true">/</span><span>${esc(p.label)}</span></div><section class="part-hero">${sky}<div><p class="eyebrow">${p.number} / ${esc(p.label.toUpperCase())}</p><h1>${esc(p.title).replace('\n', '<br>')}</h1><p class="hero-lede">${esc(p.intro)}</p><p class="signature">${esc(p.signature)}</p></div><figure class="specimen specimen-${p.specimen}">${specimen(p.specimen)}<figcaption>Design specimen · illustrative data · static composition</figcaption></figure></section>
  <div class="status-strip"><span class="status-indicator" aria-hidden="true"></span><p><strong>${esc(release.status)} · v${release.version}</strong> ${p.slug === 'common' ? 'Inherited by all four platform parts.' : `Inherits the <a href="${PRODUCT_BASE}/common">common foundations</a>.`} Conformance is recorded rule by rule in a release record.</p></div>
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

  function pageShell({ title, description, canonical, graph, body, alternate, skip, active, versionPath = active }) {
    return `<!DOCTYPE html>
<!-- Generated by scripts/build.js. Edit design-spec/ or src/design/, never this file. -->
<html lang="en" data-motion="system"><head><meta charset="UTF-8"><script>(function(){var r=document.documentElement;r.classList.add('js');var s=null,m=null;try{s=localStorage.getItem('simt:design-theme');m=localStorage.getItem('simt:design-motion')}catch(e){}if(${PROFILE_GUARD}){r.dataset.cosmosTheme=s}if(${MOTION_GUARD}){r.dataset.motion=m}})();</script><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="color-scheme" content="dark light"><title>${esc(title)}</title><meta name="description" content="${esc(description)}"><meta name="author" content="SIMT"><meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1"><link rel="canonical" href="${canonical}"><link rel="alternate" type="application/atom+xml" title="${esc(space.name)} releases" href="${space.origin}${space.feed}"><link rel="sitemap" type="application/xml" href="/sitemap.xml"><link rel="describedby" type="text/plain" title="Agent discovery" href="${space.origin}/llms.txt"><link rel="describedby" type="text/plain" title="${esc(space.name)}, in full" href="${space.origin}${space.discovery.fullText}"><meta property="og:type" content="article"><meta property="og:site_name" content="${esc(space.name)}"><meta property="og:locale" content="en_US"><meta property="og:url" content="${canonical}"><meta property="og:title" content="${esc(title)}"><meta property="og:description" content="${esc(description)}"><meta property="og:image" content="${space.origin}/images/simt-design-system.png"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630"><meta property="og:image:type" content="image/png"><meta property="og:image:secure_url" content="${space.origin}/images/simt-design-system.png"><meta property="og:image:alt" content="SIMT Design with Deep Space colors and an orbital instrument"><meta property="article:modified_time" content="${release.updated}T00:00:00Z"><meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="${esc(title)}"><meta name="twitter:description" content="${esc(description)}"><meta name="twitter:image" content="${space.origin}/images/simt-design-system.png"><meta name="twitter:image:alt" content="SIMT Design with Deep Space colors and an orbital instrument"><meta name="theme-color" content="${chromeColor('deepSpace')}" data-color="${chromeColor('deepSpace')}" media="(prefers-color-scheme: dark)"><meta name="theme-color" content="${chromeColor('daylight')}" data-color="${chromeColor('daylight')}" media="(prefers-color-scheme: light)"><link rel="icon" href="/favicon.ico"><link rel="preload" href="/fonts/space-grotesk-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin><link rel="preload" href="/fonts/source-sans-3-latin-wght-normal.woff2" as="font" type="font/woff2" crossorigin>${alternate}<link rel="stylesheet" href="${PRODUCT.stylesheet}"><script type="application/ld+json">${JSON.stringify({ '@context': 'https://schema.org', '@graph': graph }, null, 2).replaceAll('<', '\\u003c')}</script></head>
<body class="design-page"><a class="skip-link" href="#main-content">${esc(skip)}</a>${navigation(active, versionPath)}${body}<footer class="design-footer"><div><a class="footer-brand" href="${pathFor('')}">SIMT / Design</a><p>You are the center of your universe.</p></div><div><span>Specification ${release.version} · ${release.updated}</span><a href="/v/${release.version}">Version ${release.version} permalink</a><a href="${PRODUCT_BASE}/manifest.json">Agent resources ↗</a><a href="${space.parent}">Legacy reference ↗</a></div></footer><div class="design-toast" role="status" aria-live="polite" data-design-toast hidden></div><script type="module" src="/assets/site.js"></script></body></html>\n`;
  }

  const REFERENCE_LINKS = [
    ['application/json', 'specification.json', 'Specification index'],
    ['application/json', 'rules.json', 'Normative rules'],
    ['application/json', 'tokens.json', 'Design tokens'],
    ['application/json', 'components.json', 'Component index'],
    ['application/json', 'motion.json', 'Motion motifs'],
    ['text/markdown', 'simt-design-skill.md', 'Agent design skill'],
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
        name: 'SIMT Design',
        version: release.version,
        url: release.base,
      },
      {
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: space.name, item: `${space.origin}/` },
          { '@type': 'ListItem', position: 2, name: 'SIMT Design', item: release.base },
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
      title: `${title}${versionSuffix} | SIMT Design`,
      description: `${description}${PRODUCT.isLatest ? '' : ` (Design v${release.version})`}`,
      canonical,
      graph,
      body,
      alternate: REFERENCE_LINKS,
      skip: 'Skip to the gallery',
      active: GALLERY_SLUG,
      versionPath: path,
    });
  }

  function document(p = null) {
    const title = p
      ? `${p.label}${versionSuffix} | SIMT Design`
      : `SIMT Design${versionSuffix} | Cross-platform UI specification`;
    const baseDescription =
      p?.description ||
      'Design v2: a shared foundation and four production design specifications for public websites, social web apps, native mobile instruments and simplified watches.';
    const description = PRODUCT.isLatest ? baseDescription : `${baseDescription} (Design v${release.version})`;
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
        image: `${space.origin}/images/simt-design-system.png`,
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
        name: 'SIMT Design',
        version: release.version,
        url: release.base,
      },
      {
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: space.name, item: `${space.origin}/` },
          { '@type': 'ListItem', position: 2, name: 'SIMT Design', item: release.base },
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
    return `# ${p.label} — Design ${release.version}\n\nStatus: ${release.status}. Updated: ${release.updated}.\n\nCanonical: ${urlFor(p.slug)}\n\n${p.intro}\n\n${p.slug === 'common' ? 'Inherited by all four platform parts.' : `Inherits [Common foundations](${urlFor('common')}).`}\n\n${p.sections
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

  const css = generateProductCss(PRODUCT);
  if (PRODUCT.isLatest) generatedCSS = css;

  const exportedTokens = {
    ...tokens,
    starfield: starfieldTokens(),
    adapters: themeColorTokens(),
    platformAdapters,
    projection: colourProjection(Object.keys(themeProfiles)),
    contrastPairs: contrastPairPointer(),
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
  const resources = Object.fromEntries(
    PRODUCT.resources.map((resource) => [resource.id, `${release.base}/${resource.relativePath}`]),
  );
  const specIndex = {
    $schema: `${release.base}/specification.schema.json`,
    ...release,
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
    authority: specIndex.inheritance,
    implementationStatus: release.implementationStatus,
    provenance,
  };
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
  const draft = 'https://json-schema.org/draft/2020-12/schema';
  const schema = {
    $schema: draft,
    $id: `${release.base}/specification.schema.json`,
    title: 'Design specification index',
    description:
      'The map of the Design specification: pages, sections and the rule IDs in each. Rule prose lives in rules.json and tokens in tokens.json; both are listed in resources.',
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
    title: 'Design rules',
    description: 'Every normative Design rule with its acceptance criterion, grouped by platform section.',
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
                required: ruleDef.required,
                properties: { ...ruleDef.properties, url: { type: 'string', format: 'uri' } },
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
      const keepGradient = (gradient) =>
        scope.gradients.includes(gradient.name) &&
        gradient.profiles.some((profile) => scope.profiles.includes(profile));
      const partTokens = {
        $description: `Design ${release.version} tokens for the ${p.label} part: the common foundations, the ${p.label} mapping (platformAdapters.${adapterKey}), and only the ${scope.profiles.join(', ')} profile${scope.profiles.length === 1 ? '' : 's'} this part runs. The union of every adapter and profile is tokens.json.`,
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
        ...(colorSpace === 'oklch' ? { projection: colourProjection(scope.profiles) } : {}),
        contrastPairs: contrastPairPointer(),
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
        $description: `Design ${release.version} rules for the ${p.label} part: the common foundations and this part's own sections. Every part is in rules.json.`,
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
        tokens: partTokens,
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
  for (const payload of partPayloads) {
    const entry = manifest.platforms.find((p) => p.id === payload.slug);
    entry.adapter = payload.adapter;
    entry.tokens = `${release.base}/${payload.slug}/tokens.json`;
    entry.rules = `${release.base}/${payload.slug}/rules.json`;
    const index = specIndex.pages.find((p) => p.slug === payload.slug);
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
  const literal = [
    [productFile('index.html'), document()],
    [productFile('tokens.css'), css],
    [
      productFile('tokens.json'),
      json({
        $description: `Design ${release.version} base tokens, theme adapters and contrast pairs`,
        ...exportedTokens,
      }),
    ],
    [
      productFile('rules.json'),
      json({
        $description: `Design ${release.version} normative rules with acceptance criteria, grouped by platform section`,
        ...rules,
      }),
    ],
    [
      productFile('contrast-pairs.json'),
      json({
        $description: `Design ${release.version} composited contrast for every role pair in every theme profile`,
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
        $description: `Design ${release.version} component index: for every component the rules it inherits, the theme roles, layer and corner role it consumes, the states it can be in, the motif that animates it and the counterpart that already exists in each product. References only — every treatment stays in the rules the entries name. Sliced by platform part under parts.`,
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
        $description: `Design ${release.version} named motion motifs: the trigger, steps, allowed properties, duration, easing and per-tier and per-platform behaviour of every state change the system animates, with the sampled curve values a native port reproduces.`,
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
    [productFile('simt-design-skill.md'), skillMarkdown()],
    ...(PRODUCT.isLatest ? [[HERO_SKY, heroSky() + '\n']] : []),
  ];
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
  const referenceTail = () =>
    `${starfieldNotes}\n## Theme adapters\n\n${Object.entries(adapters)
      .map(
        ([name, roles]) =>
          `### ${name}\n\n${Object.entries(roles)
            .map(([k, v]) => `- ${k}: ${v}`)
            .join('\n')}\n`,
      )
      .join('\n')}\n## Reference sources\n\n${sources.map((s) => `- [${s.name}](${s.url}): ${s.use}`).join('\n')}\n`;
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
  put(
    productFile(`${GALLERY_SLUG}/index.html`),
    galleryPage(galleryIndex(), {
      title: 'Component gallery',
      description:
        'Every Design component with its state matrix, its motion filmstrip and the rules it applies, as static HTML that reads in every theme and without JavaScript.',
      path: GALLERY_SLUG,
      breadcrumb: [],
    }),
  );
  for (const family of GALLERY_FAMILIES) {
    put(
      productFile(`${GALLERY_SLUG}/${family.id}/index.html`),
      galleryPage(galleryFamily(family.id), {
        title: `${family.label} gallery`,
        description: `The ${family.label} components in Design, with the state matrix, motion filmstrip and rule IDs each one applies.`,
        path: `${GALLERY_SLUG}/${family.id}`,
        breadcrumb: ['Gallery'],
      }),
    );
  }

  const sizes = noteSizes(outputs, PRODUCT_DIR);
  const record = releaseRecord(sizes, release);
  const currentNotes = record.at(-1).notes;
  put(
    productFile('specification.json'),
    json({
      ...specIndex,
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
    `# SIMT Design ${release.version}\n\n${release.scope}\n\n${release.implementationStatus}\n\n## What changed in ${release.version}\n\nThe ${currentNotes.length} changes in this release are listed in [changelog.md](${release.base}/changelog.md), which carries every published release newest first.\n\n${pages.map((p) => partMarkdown.get(p.slug)).join('\n---\n\n')}\n${referenceTail()}\n## Repository provenance\n\n${provenance.map((p) => `- ${p.repository}: ${p.paths.join(', ')}. Observed: ${p.observed} Target: ${p.direction}`).join('\n')}\n`,
  );
  put(productFile('changelog.md'), changelogMarkdown(sizes, release));
  return { outputs, ruleCount: rules.ruleCount };
}

function agentCatalogEntries({ ruleCount }) {
  return products.filter((p) => p.isLatest).flatMap((p) => p.agentEntries({ ruleCount, skillRuleIds }));
}

let generated = null;
export function generatedOutputs() {
  if (generated) return generated;
  generated = (() => {
    const outputs = new Map();
    const put = (path, contents) => {
      if (outputs.has(path))
        throw new Error(`Two generators claim ${path}; the second copy would be dropped without a word`);
      outputs.set(path, contents);
    };
    let latestRuleCount = 0;
    for (const PRODUCT of products) {
      const result = generateProductOutputs(PRODUCT);
      if (PRODUCT.isLatest) latestRuleCount = result.ruleCount;
      for (const [path, contents] of result.outputs) put(path, contents);
    }
    for (const [path, contents] of siteOutputs({
      agentEntries: agentCatalogEntries({ ruleCount: latestRuleCount }),
      documents: outputs,
    })) {
      put(path, contents);
    }
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
  // tokens.less is repo source, not a published asset: site.less
  // imports it and the Less compiler reads it from here.
  if (check) {
    assertGenerated(resolve(root, 'styles/tokens.less'), generatedCSS);
  } else {
    // dist/ is fully owned by this build. A file left by an earlier release
    // would otherwise be published forever, since the assets Worker serves
    // whatever is in the directory.
    rmSync(DIST, { recursive: true, force: true });
    writeGenerated(resolve(root, 'styles/tokens.less'), generatedCSS);
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
    `Design: ${build({ check: process.argv.includes('--check') })} generated files ${process.argv.includes('--check') ? 'verified' : 'written'}.`,
  );
}
