import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { isAbsolute, relative, resolve } from 'node:path';

const esc = (value) =>
  String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
const codeBlock = (code, label) =>
  `<pre class="library-code" tabindex="0" role="region" aria-label="${esc(label)}"><code>${esc(code)}</code></pre>`;
const portable = (path) => path.split('\\').join('/');
const walk = (directory) =>
  readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = resolve(directory, entry.name);
    return entry.isDirectory() ? walk(path) : [path];
  });
const repositoryPath = (root, path) => {
  const value = portable(relative(root, resolve(root, path)));
  if (!value || value === '..' || value.startsWith('../') || isAbsolute(value)) {
    throw new Error(`Path escapes the repository: ${path}`);
  }
  return value;
};
const versionFromGradle = (packageRoot, metadata) => {
  const file = resolve(packageRoot, metadata.versionFile ?? '../gradle.properties');
  const property = metadata.versionProperty ?? 'VERSION_NAME';
  const match = readFileSync(file, 'utf8').match(new RegExp(`^${property}=(.*)$`, 'm'));
  if (!match?.[1]?.trim()) throw new Error(`Missing Gradle property ${property} in ${file}`);
  return match[1].trim();
};
const pngDimensions = (file) => {
  const bytes = readFileSync(file);
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  if (bytes.length < 24 || !bytes.subarray(0, 8).equals(signature)) {
    throw new Error(`Invalid native PNG capture: ${file}`);
  }
  return { width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20) };
};
const apiDeclaration = (line) => {
  const visible = line.split(' // ')[0].trim();
  if (!visible || /\$|stableprop|ComposableSingletons/.test(visible)) return null;
  const match = visible.match(
    /^(?:(?:public|final|abstract|open|sealed|data|enum|annotation|value|expect|actual|static|const|synthetic)\s+)*(class|interface|object|fun|val|var|typealias)\s+(.+)$/,
  );
  if (!match) return null;
  const [, kind, remainder] = match;
  const declaration = (kind === 'interface' ? remainder.replace(/^class\s+/, '') : remainder)
    .replace(/^<[^>]+>\s+/, '')
    .replace(/^\([^)]*\)\./, '');
  const name =
    kind === 'fun'
      ? declaration.match(/^([\w./]+)\s*\(/)?.[1]
      : kind === 'val' || kind === 'var'
        ? declaration.match(/^([\w./]+)/)?.[1]
        : declaration.match(/^([\w./]+)/)?.[1];
  if (!name || name.includes('$stable')) return null;
  return { kind, name: name.split('/').at(-1) };
};
const apiDeclarations = (source) => {
  const declarations = source
    .split('\n')
    .filter((line) => line && !/^\s/.test(line) && !line.startsWith('//'))
    .map(apiDeclaration)
    .filter(Boolean);
  return declarations;
};

export function loadNativeLibrary(packageRoot, rawManifest) {
  const declared = rawManifest.metadata;
  if (!declared?.name || !declared?.description || !Array.isArray(declared.platforms)) {
    throw new Error(`${rawManifest.id}: native metadata is incomplete`);
  }
  const version = versionFromGradle(packageRoot, declared);
  const bind = (value) => String(value).replaceAll('{{version}}', version);
  const manifest = {
    ...rawManifest,
    usage: bind(rawManifest.usage),
    components: rawManifest.components.map((component) => ({
      ...component,
      description: bind(component.description),
      usage: bind(component.usage),
    })),
  };
  const sourceRoot = resolve(packageRoot, 'src');
  const sourceRoots = declared.sourceRoots ?? ['commonMain/kotlin', 'androidMain/kotlin', 'iosMain/kotlin'];
  const files = [
    ...new Set(
      sourceRoots.flatMap((root) =>
        walk(resolve(sourceRoot, root))
          .filter((path) => path.endsWith('.kt'))
          .map((path) => portable(relative(sourceRoot, path))),
      ),
    ),
  ].sort();
  const sources = new Map(files.map((path) => [path, readFileSync(resolve(sourceRoot, path), 'utf8')]));

  const apiRoot = resolve(packageRoot, 'api');
  const apiFiles = existsSync(apiRoot)
    ? walk(apiRoot)
        .filter((path) => path.endsWith('.api'))
        .map((path) => portable(relative(apiRoot, path)))
        .sort()
    : [];
  const apiSurfaces = apiFiles.map((path) => {
    const source = readFileSync(resolve(apiRoot, path), 'utf8');
    return { path, source, declarations: apiDeclarations(source) };
  });

  const captureRoot = resolve(packageRoot, 'previews');
  const captureFiles = existsSync(captureRoot)
    ? walk(captureRoot)
        .filter((path) => path.endsWith('.png'))
        .map((path) => portable(relative(captureRoot, path)))
        .sort()
    : [];
  const expectedCaptures = manifest.components.map((component) => component.capture).sort();
  if (JSON.stringify(captureFiles) !== JSON.stringify(expectedCaptures)) {
    throw new Error(`${manifest.id}: every PNG capture needs exactly one manifest entry`);
  }
  const captures = manifest.components.map((component) => {
    if (!/^[a-z0-9-]+\.png$/.test(component.capture)) {
      throw new Error(`${manifest.id}: unsafe capture name ${component.capture}`);
    }
    const file = resolve(captureRoot, component.capture);
    return { ...component, ...pngDimensions(file) };
  });
  for (const component of captures) {
    for (const source of component.source ?? []) {
      if (!sources.has(source.replace(/^src\//, ''))) {
        throw new Error(`${manifest.id}: unknown production source ${source}`);
      }
    }
  }

  const repositoryRoot = resolve(packageRoot, '..');
  const captureSources = (manifest.evidence?.capture?.sources ?? []).map((source) => {
    const path = repositoryPath(repositoryRoot, resolve(packageRoot, source));
    return { path, contents: readFileSync(resolve(repositoryRoot, path), 'utf8') };
  });
  return {
    metadata: {
      name: declared.name,
      description: declared.description,
      version,
      platforms: declared.platforms,
      sourceRoots,
    },
    manifest,
    files,
    sources,
    apiFiles,
    apiSurfaces,
    captures,
    captureSources,
  };
}

export function nativeCoverage(library) {
  const referenced = new Set(
    library.manifest.components.flatMap((component) => component.source ?? []),
  );
  return {
    declarationCount: library.apiSurfaces.reduce((sum, surface) => sum + surface.declarations.length, 0),
    unreferencedFiles: library.files.filter((path) => !referenced.has(`src/${path}`)),
  };
}

const sourceUrl = (library, path) => `/${library.route}/source/${path}`;
const captureUrl = (library, file) => `/${library.route}/captures/${file}`;
const apiUrl = (library, file) => `/${library.route}/api/${file}`;
const sourceLinks = (library, paths = []) =>
  paths.length
    ? `<ul>${paths
        .map(
          (path) =>
            `<li><a href="${sourceUrl(library, path)}" target="_blank" rel="noopener">${esc(path)}</a></li>`,
        )
        .join('')}</ul>`
    : '<p>No authored example references this surface yet.</p>';

export function nativeLibraryPage(library) {
  const coverage = nativeCoverage(library);
  const evidence = library.manifest.evidence;
  const capture = evidence.capture;
  const facts = [
    ['Captures', library.captures.length],
    ['Kotlin files', library.files.length],
    ['Top-level ABI entries', coverage.declarationCount],
  ];
  return `<main id="main-content"><div class="breadcrumb"><a href="/">Design</a><span aria-hidden="true">/</span><a href="/libraries/">Libraries</a><span aria-hidden="true">/</span><span>${esc(library.id)}</span></div><section class="library-hero"><div><p class="eyebrow">${esc(library.metadata.name.toUpperCase())}</p><h1>${esc(library.manifest.title)}</h1><p class="hero-lede">${esc(library.manifest.description)}</p><ul class="library-tag-list" aria-label="Supported platforms">${library.metadata.platforms.map((platform) => `<li>${esc(platform)}</li>`).join('')}</ul><div class="hero-actions"><a class="button button-primary" href="#native-previews">Inspect native captures <span aria-hidden="true">↓</span></a><a class="button button-secondary" href="#integration">Integration <span aria-hidden="true">↗</span></a></div></div><aside class="library-package" aria-label="Native package facts"><span class="library-status"><span class="status-indicator" aria-hidden="true"></span>HOST-NATIVE EVIDENCE</span><strong>v${esc(library.metadata.version)}</strong><p>${esc(library.metadata.description)}</p><dl class="library-facts">${facts.map(([label, value]) => `<div><dt>${label}</dt><dd>${value}</dd></div>`).join('')}</dl></aside></section>
  <aside class="library-native-notice" aria-label="Native evidence limits"><div><p class="eyebrow">STATIC NATIVE CAPTURES</p><h2>Real Compose output, honestly scoped.</h2></div><div><p>${esc(capture.environment)}. ${esc(capture.behavior)}</p><ul>${evidence.limits.map((limit) => `<li>${esc(limit)}</li>`).join('')}</ul></div></aside>
  <section class="library-section" id="native-previews" aria-labelledby="native-previews-title"><p class="eyebrow">NATIVE PREVIEWS</p><h2 id="native-previews-title">Published artifact, rendered by its capture harness.</h2><p>Each image is generated by the package-owned capture harness using the provenance below. The cards pair the capture with its real production source and caller-owned usage contract.</p><div class="library-component-list">${library.captures
    .map(
      (component, index) => `<article class="library-component" id="component-${esc(component.id)}"><header><span>${String(index + 1).padStart(2, '0')}</span><div><h3>${esc(component.title)}</h3><p>${esc(component.description)}</p></div></header><div class="library-component-body"><div><figure class="native-capture"><img src="${captureUrl(library, component.capture)}" width="${component.width}" height="${component.height}" loading="lazy" decoding="async" alt="${esc(`${component.title}: ${capture.label}`)}"><figcaption>${esc(capture.label)} · ${component.width} × ${component.height} px</figcaption></figure><h4>Usage</h4>${codeBlock(component.usage, `${component.title} usage`)}</div><aside><h4>Contracts</h4><ul>${(component.contracts ?? []).map((contract) => `<li><code>${esc(contract)}</code></li>`).join('')}</ul><h4>Production source</h4>${sourceLinks(library, component.source)}<h4>Evidence</h4><p>${esc(component.evidence ?? capture.environment)}</p></aside></div></article>`,
    )
    .join('')}</div></section>
  <section class="library-section library-integration" id="integration" aria-labelledby="integration-title"><p class="eyebrow">INTEGRATION</p><h2 id="integration-title">Consume the published native artifact.</h2><p>The caller owns the theme, state, labels, callbacks, window policy, motion, visibility and lifecycle. Use the package coordinates and platform targets below with the integration shown here.</p>${codeBlock(library.manifest.usage, `${library.metadata.name} integration example`)}<div class="library-export-grid">${library.metadata.platforms.map((platform) => `<div><code>${esc(library.metadata.name)}</code><span>${esc(platform)}</span></div>`).join('')}</div></section>
  <section class="library-section" id="capture-evidence" aria-labelledby="evidence-title"><p class="eyebrow">CAPTURE PROVENANCE</p><h2 id="evidence-title">Regenerate the evidence from source.</h2><p>The package-owned command below documents the capture environment and build inputs. Captures are committed as reviewable evidence; rerun it whenever the rendered primitives or fixture content changes.</p>${codeBlock(capture.command, 'Native capture regeneration command')}<details><summary>Capture harness source</summary><ul>${library.captureSources.map((source) => `<li><a href="/${library.route}/capture-source/${esc(source.path)}" target="_blank" rel="noopener">${esc(source.path)}</a></li>`).join('')}</ul></details></section>
  <section class="library-section" id="source-coverage" aria-labelledby="coverage-title"><p class="eyebrow">AUTOMATIC COVERAGE</p><h2 id="coverage-title">New Kotlin source and ABI appear automatically.</h2><p>The build discovers every production <code>.kt</code> file under ${library.metadata.sourceRoots.map((root) => `<code>${esc(root)}</code>`).join(', ')}, publishes the committed ABI dumps, and marks files not yet referenced by an authored capture. Add a source file or refresh the ABI dump without editing this page template.</p><div class="library-coverage"><div><strong>${library.files.length}</strong><span>Production Kotlin files</span></div><div><strong>${coverage.declarationCount}</strong><span>Top-level ABI entries</span></div><div><strong>${coverage.unreferencedFiles.length}</strong><span>Files awaiting an example</span></div></div>${library.apiSurfaces
    .map(
      (surface) => `<details><summary>${esc(surface.path)} · ${surface.declarations.length} top-level entries</summary><p><a href="${apiUrl(library, surface.path)}" target="_blank" rel="noopener">Open the complete ABI dump</a></p><ul>${surface.declarations.map((entry) => `<li><code>${esc(entry.name)}</code> <span>${esc(entry.kind)}</span></li>`).join('')}</ul></details>`,
    )
    .join('')}<details><summary>All ${library.files.length} production Kotlin files</summary><ul>${library.files.map((path) => `<li><a href="${sourceUrl(library, `src/${path}`)}" target="_blank" rel="noopener">src/${esc(path)}</a>${coverage.unreferencedFiles.includes(path) ? ' <span>not yet referenced by an example</span>' : ''}</li>`).join('')}</ul></details></section></main>`;
}
