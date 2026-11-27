import { readFileSync, readdirSync } from 'node:fs';
import { resolve, dirname, relative } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const esc = (value) =>
  String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
const siteRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const registry = JSON.parse(readFileSync(resolve(siteRoot, 'libraries.json'), 'utf8'));
const featureExports = {
  theme: ['initTheme'],
  motion: ['initMotion'],
  reveal: ['observeReveals'],
  ambient: ['pauseOffscreen'],
  header: ['trackHeader'],
  sections: ['trackSections'],
  pointer: ['initPointerScene'],
  performance: ['detectPerformance'],
};
const featureLabels = {
  theme: 'Theme preference',
  motion: 'Motion preference',
  reveal: 'Viewport reveals',
  ambient: 'Offscreen pause',
  header: 'Scrolled header',
  sections: 'Active section',
  pointer: 'Pointer scene',
  performance: 'Performance tier',
};
const previewChrome = (library) => `<header class="preview-header" data-header>
  <a class="preview-brand" href="${library.url}" target="_top">${esc(library.metadata.name)} <span>v${esc(library.metadata.version)}</span></a>
  <div class="preview-controls">
    <span class="preview-performance">Performance: <strong data-runtime-performance>Auto</strong></span>
    <button class="icon-button" type="button" data-theme-toggle aria-label="Switch theme">
      <svg class="icon icon--to-light" aria-hidden="true" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/></svg>
      <svg class="icon icon--to-dark" aria-hidden="true" viewBox="0 0 24 24"><path d="M20 15a8 8 0 0 1-11-11 8 8 0 1 0 11 11Z"/></svg>
    </button>
    <button class="icon-button" type="button" data-motion-toggle aria-label="Pause motion">
      <svg class="icon icon--play" aria-hidden="true" viewBox="0 0 24 24"><path d="m8 5 11 7-11 7z"/></svg>
      <svg class="icon icon--pause" aria-hidden="true" viewBox="0 0 24 24"><path d="M8 5v14M16 5v14"/></svg>
    </button>
  </div>
</header>`;

const packagePath = (value) => String(value).replace(/^\.\//, '');
const sourceFiles = (packageRoot) => {
  const sourceRoot = resolve(packageRoot, 'src');
  const walk = (directory) =>
    readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
      const absolute = resolve(directory, entry.name);
      return entry.isDirectory() ? walk(absolute) : [relative(sourceRoot, absolute).split('\\').join('/')];
    });
  return walk(sourceRoot)
    .filter((path) => /\.(?:mjs|css)$/.test(path))
    .sort();
};
const exportNames = (source) =>
  [...source.matchAll(/export\s*\{([^}]+)\}/g)]
    .flatMap(([, names]) => names.split(','))
    .map((name) => name.trim().split(/\s+as\s+/).at(-1))
    .filter(Boolean);
const cssClasses = (source) =>
  [...new Set([...source.matchAll(/\.(-?[_a-zA-Z]+[\w-]*)/g)].map(([, name]) => name))].sort();
const markupClasses = (markup) =>
  new Set(
    [...markup.matchAll(/class="([^"]+)"/g)].flatMap(([, value]) => value.split(/\s+/)).filter(Boolean),
  );
const bindPackageManifest = (manifest, packageName) => {
  const bind = (value) =>
    typeof value === 'string' ? value.replaceAll(`@simt/${manifest.id}`, packageName) : value;
  return {
    ...manifest,
    description: bind(manifest.description),
    usage: bind(manifest.usage),
    components: manifest.components.map((component) => ({
      ...component,
      description: bind(component.description),
      usage: bind(component.usage),
      markup: bind(component.markup),
    })),
  };
};

async function loadLibrary(config) {
  if (!/^[a-z0-9-]+$/.test(config.id)) throw new Error(`Unsafe library id: ${config.id}`);
  const packageRoot = resolve(siteRoot, config.packageDirectory);
  const metadata = JSON.parse(readFileSync(resolve(packageRoot, 'package.json'), 'utf8'));
  const manifestPath = resolve(packageRoot, config.manifest ?? 'preview.manifest.js');
  const rawManifest = (await import(pathToFileURL(manifestPath).href)).default;
  const manifest = bindPackageManifest(rawManifest, metadata.name);
  if (manifest.id !== config.id) throw new Error(`${config.id}: manifest id does not match the registry`);
  const ids = manifest.components.map((component) => component.id);
  if (new Set(ids).size !== ids.length) throw new Error(`${config.id}: preview component ids must be unique`);
  const files = config.kind === 'web' ? sourceFiles(packageRoot) : [];
  const sources = new Map(
    files.map((path) => [path, readFileSync(resolve(packageRoot, 'src', path), 'utf8')]),
  );
  const runtimeEntry = packagePath(metadata.exports?.['.'] ?? 'src/index.mjs');
  const bootstrapEntry = packagePath(metadata.exports?.['./bootstrap'] ?? 'src/bootstrap.mjs');
  const bootstrap =
    config.kind === 'web'
      ? (await import(pathToFileURL(resolve(packageRoot, bootstrapEntry)).href)).preferenceBootstrap()
      : null;
  const route = `libraries/${config.id}`;
  return {
    ...config,
    packageRoot,
    metadata,
    manifest,
    manifestFile: config.manifest ?? 'preview.manifest.js',
    files,
    sources,
    runtimeEntry,
    bootstrap,
    route,
    path: route,
    previewPath: `${route}/preview`,
    url: `/${route}/`,
    previewUrl: `/${route}/preview/`,
    runtimeUrl: `/${route}/runtime/${runtimeEntry}`,
    packageUrl: `/${route}/package.json`,
    manifestUrl: `/${route}/preview.manifest.js`,
    stylesheet: metadata.exports?.['./styles.css']
      ? `/${route}/runtime/${packagePath(metadata.exports['./styles.css'])}`
      : null,
  };
}

export const libraries = await Promise.all((registry.libraries ?? []).map(loadLibrary));
export const publishedLibraries = libraries.filter((library) => library.published);
export const webLibraries = publishedLibraries.filter((library) => library.kind === 'web');

export function libraryPreviewRewrites(library) {
  if (library.kind !== 'web') return [];
  const clean = `/${library.previewPath}`;
  return [`${clean} ${clean}/index.html 200`, `${clean}/ ${clean}/index.html 200`];
}

export function libraryCoverage(library) {
  const markup = `${previewChrome(library)}\n${library.manifest.components.map((component) => component.markup).join('\n')}`;
  const demonstrated = markupClasses(markup);
  const stylesheetSource = library.files
    .filter((path) => path.endsWith('.css'))
    .map((path) => library.sources.get(path))
    .join('\n');
  const declared = cssClasses(stylesheetSource);
  const rootSource = library.sources.get(library.runtimeEntry) ?? '';
  const exports = exportNames(rootSource);
  const activeExports = new Set((library.manifest.features ?? []).flatMap((feature) => featureExports[feature] ?? []));
  const referenced = new Set(library.manifest.components.flatMap((component) => component.source ?? []));
  return {
    classCount: declared.length,
    undemonstratedClasses: declared.filter((name) => !demonstrated.has(name)),
    exports,
    undemonstratedExports: exports.filter((name) => !activeExports.has(name)),
    unreferencedFiles: library.files.filter((path) => !referenced.has(`src/${path}`)),
  };
}

const codeBlock = (code, label) =>
  `<pre class="library-code" tabindex="0" role="region" aria-label="${esc(label)}"><code>${esc(code)}</code></pre>`;
const kindLabel = (library) => (library.kind === 'web' ? 'Live web playground' : 'Native preview catalog');

export function librariesOverviewBody() {
  return `<main id="main-content"><section class="library-hero"><div><p class="eyebrow">SHARED LIBRARIES</p><h1>Libraries you can inspect.</h1><p class="hero-lede">The Design site discovers published SIMT libraries from one registry, reads their package metadata and renders their preview catalog without a framework.</p></div><aside class="library-package" aria-label="Preview registry"><span class="library-status"><span class="status-indicator" aria-hidden="true"></span>${publishedLibraries.length} published</span><strong>Package-owned manifests</strong><p>Add a library once. Add a component with one preview entry.</p></aside></section><section class="library-grid" aria-label="Published libraries">${publishedLibraries
    .map(
      (library) => `<article class="library-card"><div class="library-card-head"><span class="library-kind">${kindLabel(library)}</span><span class="library-version">v${esc(library.metadata.version)}</span></div><h2><a href="${library.url}">${esc(library.metadata.name)}</a></h2><p>${esc(library.manifest.description)}</p><dl class="library-facts"><div><dt>Components</dt><dd>${library.manifest.components.length}</dd></div><div><dt>Source files</dt><dd>${library.files.length}</dd></div><div><dt>Runtime</dt><dd>${library.kind === 'web' ? 'Local ESM + CSS' : 'Native captures'}</dd></div></dl><a class="text-link" href="${library.url}">Open ${esc(library.id)} catalog <span aria-hidden="true">↗</span></a></article>`,
    )
    .join('')}</section><section class="library-section library-note"><p class="eyebrow">AUTOMATIC BY DEFAULT</p><h2>One registry. Package-owned previews.</h2><p>The registry supplies the package location and preview kind. Package metadata supplies the name, version, exports and entry points. The preview manifest supplies only the examples that need authored markup; source publication and coverage are discovered from the package itself.</p></section></main>`;
}

export function libraryPageBody(library) {
  if (library.kind !== 'web') throw new Error(`${library.id}: native catalog rendering is not registered`);
  const coverage = libraryCoverage(library);
  const exports = Object.entries(library.metadata.exports ?? {});
  return `<main id="main-content"><div class="breadcrumb"><a href="/">Design</a><span aria-hidden="true">/</span><a href="/libraries/">Libraries</a><span aria-hidden="true">/</span><span>${esc(library.id)}</span></div><section class="library-hero"><div><p class="eyebrow">${esc(library.metadata.name.toUpperCase())}</p><h1>${esc(library.manifest.title)}</h1><p class="hero-lede">${esc(library.manifest.description)}</p><div class="hero-actions"><a class="button button-primary" href="#live-preview">Open live preview <span aria-hidden="true">↓</span></a><a class="button button-secondary" href="${library.packageUrl}">Package metadata <span aria-hidden="true">↗</span></a></div></div><aside class="library-package" aria-label="Package facts"><span class="library-status"><span class="status-indicator" aria-hidden="true"></span>LIVE SOURCE</span><strong>v${esc(library.metadata.version)}</strong><p>${esc(library.metadata.description)}</p><dl class="library-facts"><div><dt>Components</dt><dd>${library.manifest.components.length}</dd></div><div><dt>Source files</dt><dd>${library.files.length}</dd></div><div><dt>Dependencies</dt><dd>${Object.keys(library.metadata.dependencies ?? {}).length}</dd></div></dl></aside></section>
  <figure class="library-preview" id="live-preview"><figcaption class="library-preview-bar"><span><span class="status-indicator" aria-hidden="true"></span>Actual package source · isolated same-origin iframe</span><a href="${library.previewUrl}" target="_blank" rel="noopener">Open standalone <span aria-hidden="true">↗</span></a></figcaption><div class="library-frame"><iframe src="${library.previewUrl}" title="${esc(`${library.metadata.name} interactive playground`)}" loading="lazy"></iframe></div></figure>
  <section class="library-section" id="components" aria-labelledby="components-title"><p class="eyebrow">COMPONENT CATALOG</p><h2 id="components-title">Every authored example, generated.</h2><p>The cards below come from the package preview manifest. Their source references and contracts are rendered beside the example, so adding a component never requires editing the Design page template.</p><div class="library-component-list">${library.manifest.components
    .map(
      (component, index) => `<article class="library-component" id="component-${esc(component.id)}"><header><span>${String(index + 1).padStart(2, '0')}</span><div><h3>${esc(component.title)}</h3><p>${esc(component.description)}</p></div></header><div class="library-component-body"><div><h4>Usage</h4>${codeBlock(component.usage, `${component.title} usage`)}</div><aside><h4>Contracts</h4><ul>${(component.contracts ?? []).map((contract) => `<li><code>${esc(contract)}</code></li>`).join('')}</ul><h4>Source</h4><ul>${(component.source ?? []).map((path) => `<li><a href="/${library.route}/runtime/${esc(path)}" target="_blank" rel="noopener">${esc(path)}</a></li>`).join('')}</ul></aside></div></article>`,
    )
    .join('')}</div></section>
  <section class="library-section library-integration" id="integration" aria-labelledby="integration-title"><p class="eyebrow">INTEGRATION</p><h2 id="integration-title">Use the public entry points.</h2><p>The playground imports the package root, exactly as a consumer does. Browser storage remains a separate optional adapter and is not loaded here.</p>${codeBlock(library.manifest.usage, `${library.metadata.name} integration example`)}<div class="library-export-grid">${exports
    .map(
      ([name, target]) => `<a href="${name === './package.json' ? library.packageUrl : `/${library.route}/runtime/${esc(packagePath(target))}`}" target="_blank" rel="noopener"><code>${esc(name)}</code><span>${esc(packagePath(target))}</span></a>`,
    )
    .join('')}</div></section>
  <section class="library-section" id="runtime-contracts" aria-labelledby="runtime-title"><p class="eyebrow">RUNTIME CONTRACTS</p><h2 id="runtime-title">Behavior is explicit.</h2><div class="library-contract-grid">${(library.manifest.features ?? [])
    .map(
      (feature) => `<article><span>${esc(feature)}</span><h3>${esc(featureLabels[feature] ?? feature)}</h3><p>${esc((featureExports[feature] ?? []).join(' + ') || 'Preview manifest behavior')}</p></article>`,
    )
    .join('')}</div></section>
  <section class="library-section" id="source-coverage" aria-labelledby="coverage-title"><p class="eyebrow">AUTOMATIC COVERAGE</p><h2 id="coverage-title">New source appears without a template edit.</h2><p>The build publishes every <code>.mjs</code> and <code>.css</code> file under <code>src/</code>, lists every root export and compares declared CSS classes with the preview markup. A new component needs only a manifest entry; until then, its source remains visible in this coverage report.</p><div class="library-coverage"><div><strong>${coverage.classCount}</strong><span>CSS classes discovered</span></div><div><strong>${coverage.exports.length}</strong><span>Root exports discovered</span></div><div><strong>${library.files.length}</strong><span>Source files published</span></div></div><details><summary>Inspect undemonstrated surface</summary><h3>CSS classes</h3><p>${coverage.undemonstratedClasses.length ? coverage.undemonstratedClasses.map((name) => `<code>.${esc(name)}</code>`).join(' ') : 'Every declared class appears in the playground chrome or a component example.'}</p><h3>Root exports</h3><p>${coverage.undemonstratedExports.length ? coverage.undemonstratedExports.map((name) => `<code>${esc(name)}</code>`).join(' ') : 'Every root export is initialized by a playground feature.'}</p><h3>Source files</h3><ul>${library.files.map((path) => `<li><a href="/${library.route}/runtime/src/${esc(path)}" target="_blank" rel="noopener">src/${esc(path)}</a>${coverage.unreferencedFiles.includes(path) ? ' <span>not yet referenced by an example</span>' : ''}</li>`).join('')}</ul></details></section></main>`;
}

export function libraryPreviewDocument(library) {
  if (library.kind !== 'web') throw new Error(`${library.id}: native preview documents are not registered`);
  if (!library.stylesheet) throw new Error(`${library.id}: no public stylesheet export`);
  const config = {
    runtimeUrl: library.runtimeUrl,
    features: library.manifest.features,
    themeAdapter: library.themeAdapter ?? null,
  };
  const configJson = JSON.stringify(config).replaceAll('<', '\\u003c');
  const navigation = library.manifest.components
    .map((component) => `<a href="#${esc(component.id)}" data-nav-link>${esc(component.title)}</a>`)
    .join('');
  return `<!DOCTYPE html>
<!-- Generated from ${esc(library.metadata.name)} package metadata and preview.manifest.js. -->
<html lang="en" data-motion="system"><head><meta charset="UTF-8"><script>${library.bootstrap.replaceAll('<', '\\u003c')}</script><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="robots" content="noindex, nofollow"><meta name="color-scheme" content="dark light"><title>${esc(library.metadata.name)} playground</title><meta name="description" content="${esc(library.manifest.description)}"><link rel="icon" href="/favicon.ico"><link rel="stylesheet" href="${library.stylesheet}"><link rel="stylesheet" href="/styles/library-preview.css">${library.tokenStylesheet ? `<link rel="stylesheet" href="${library.tokenStylesheet}">` : ''}</head>
<body class="preview-page" data-preview-library="${esc(library.id)}"><a class="preview-skip" href="#preview-main">Skip to playground</a>${previewChrome(library)}<nav class="preview-nav" aria-label="Playground components">${navigation}</nav><main class="preview-shell" id="preview-main"><section class="preview-hero"><p class="preview-kicker">LIVE PACKAGE SOURCE · v${esc(library.metadata.version)}</p><h1>Motion with a contract.</h1><p>Real ${esc(library.metadata.name)} modules and CSS, running locally. Toggle theme or motion, scroll the examples and move a fine pointer across the scene.</p><div class="preview-hero-actions"><a class="button button--primary" href="#actions">Explore controls <span aria-hidden="true">↓</span></a><a class="button button--ghost" href="${library.url}" target="_top">Read the catalog <span aria-hidden="true">↗</span></a></div></section>${library.manifest.components.map((component) => component.markup).join('\n')}</main><footer class="preview-footer"><span>${esc(library.metadata.name)} v${esc(library.metadata.version)}</span><a href="${library.url}" target="_top">Back to the Design catalog ↗</a></footer><script type="application/json" data-preview-config>${configJson}</script><script type="module" src="/assets/library-preview.js"></script></body></html>\n`;
}
