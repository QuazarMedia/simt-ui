import assert from 'node:assert/strict';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';
import { build, generatedOutputs } from './build.js';
import { authority, canonicalUrl } from '../src/site/authority.js';
import product, { allRoutes, budgetOf, latest, products, productResourceUrl, productRouteUrl } from '../src/site/product.js';
import { publishedLibraries, webLibraries } from '../src/design/library-previews.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (path) => readFileSync(join(root, path), 'utf8');
const bytes = (value) => Buffer.byteLength(value);
const routeOutput = (route) => (route.path ? `${route.path}/index.html` : 'index.html');
const resourceOutput = (resource) => resource.path;

function checkRoutes(outputs) {
  const routes = allRoutes;
  assert.equal(new Set(routes.map((route) => route.path)).size, routes.length, 'route paths must be unique');
  for (const route of routes) {
    const path = routeOutput(route);
    assert(outputs.has(path), `missing route output ${path}`);
    const html = outputs.get(path);
    const canonical = productRouteUrl(route);
    assert(html.includes(`<link rel="canonical" href="${canonical}">`), `${path}: wrong canonical`);
    assert.equal((html.match(/<h1[ >]/g) ?? []).length, 1, `${path}: exactly one H1`);
    assert.match(html, /<main\b[\s\S]*?<\/main>/, `${path}: static main content`);
    assert(!/<canvas\b|fonts\.(?:googleapis|gstatic)\.com/.test(html), `${path}: must render without remote or client drawing`);
    assert(html.includes('data-version-select'), `${path}: missing version switcher`);
    assert(bytes(html) <= budgetOf('htmlBytesPerRoute'), `${path}: HTML budget`);
  }
  assert.equal(new Set(routes.map(productRouteUrl)).size, routes.length, 'canonical routes must be unique');
  assert.equal(productRouteUrl(product.routes[0]), authority.origin, 'overview must be the canonical origin');
  assert.equal(authority.origin, 'https://design.simt.app');
  for (const route of routes) {
    const html = outputs.get(routeOutput(route));
    assert(html.includes('class="design-header"'), `${route.path || '/'}: Design header`);
    assert(html.includes('<span>SIMT <b>Design</b></span>'), `${route.path || '/'}: header brand`);
    assert(html.includes('class="design-footer"'), `${route.path || '/'}: Design footer`);
    assert(html.includes('>SIMT / Design</a>'), `${route.path || '/'}: footer brand`);
    assert(html.includes('data-design-toast'), `${route.path || '/'}: Design toast hook`);
    assert(html.includes('src="/assets/site.js"'), `${route.path || '/'}: site client`);
  }
}

function checkLibraries(outputs) {
  const overview = outputs.get('libraries/index.html');
  assert(overview, 'missing library overview');
  assert.equal((overview.match(/<h1[ >]/g) ?? []).length, 1, 'library overview: exactly one H1');
  for (const library of publishedLibraries) {
    assert(overview.includes(`href="${library.url}"`), `library overview: missing ${library.id}`);
    const page = outputs.get(`${library.route}/index.html`);
    assert(page, `missing library page ${library.route}`);
    assert(page.includes(library.metadata.name), `${library.route}: package name`);
    assert(page.includes(`v${library.metadata.version}`), `${library.route}: package version`);
    assert(page.includes('data-preview-library') || page.includes('library-preview'), `${library.route}: preview surface`);
    for (const component of library.manifest.components) {
      assert(page.includes(`id="component-${component.id}"`), `${library.route}: missing ${component.id} catalog entry`);
    }
    for (const file of library.files) {
      assert(page.includes(`${library.route}/runtime/src/${file}`), `${library.route}: missing source link ${file}`);
    }
    if (library.kind !== 'web') continue;
    assert(page.includes(`src="${library.previewUrl}"`), `${library.route}: isolated preview`);
    const preview = outputs.get(`${library.previewPath}/index.html`);
    assert(preview, `missing preview document ${library.previewPath}`);
    assert(preview.includes('noindex, nofollow'), `${library.previewPath}: preview must not be indexed`);
    assert(!preview.includes('rel="canonical"'), `${library.previewPath}: preview must not claim a canonical page`);
    assert.equal((preview.match(/<h1[ >]/g) ?? []).length, 1, `${library.previewPath}: exactly one H1`);
    assert(preview.includes('src="/assets/library-preview.js"'), `${library.previewPath}: generic client`);
    assert(preview.includes(library.stylesheet), `${library.previewPath}: package stylesheet`);
    assert(!/<(?:script|link)[^>]+(?:src|href)="https?:\/\//.test(preview), `${library.previewPath}: no remote assets`);
    const config = JSON.parse(preview.match(/<script type="application\/json" data-preview-config>([\s\S]*?)<\/script>/)[1]);
    assert.equal(config.runtimeUrl, library.runtimeUrl, `${library.previewPath}: runtime entry`);
    assert.deepEqual(config.features, library.manifest.features, `${library.previewPath}: feature contract`);
    for (const component of library.manifest.components) {
      assert(preview.includes(`data-preview-component="${component.id}"`), `${library.previewPath}: missing ${component.id}`);
    }
  }
}

function checkLibraryAssets() {
  const previewJs = gzipSync(readFileSync(join(root, 'dist/assets/library-preview.js'))).length;
  const previewCss = gzipSync(readFileSync(join(root, 'dist/styles/library-preview.css'))).length;
  assert(previewJs <= budgetOf('libraryPreviewJsGzipBytes'), `Library preview JS gzip budget: ${previewJs}`);
  assert(previewCss <= budgetOf('libraryPreviewCssGzipBytes'), `Library preview CSS gzip budget: ${previewCss}`);
  for (const library of webLibraries) {
    let runtimeBytes = 0;
    for (const file of library.files) {
      const path = join(root, 'dist', library.route, 'runtime', 'src', file);
      assert.equal(readFileSync(path, 'utf8'), readFileSync(join(library.packageRoot, 'src', file), 'utf8'), `runtime source drift: ${path}`);
      runtimeBytes += statSync(path).size;
    }
    assert(runtimeBytes <= budgetOf('libraryRuntimeBytes'), `${library.id}: runtime byte budget: ${runtimeBytes}`);
    assert.equal(
      readFileSync(join(root, 'dist', library.route, 'package.json'), 'utf8'),
      readFileSync(join(library.packageRoot, 'package.json'), 'utf8'),
      `${library.id}: package metadata drift`,
    );
    assert.equal(
      readFileSync(join(root, 'dist', library.route, 'preview.manifest.js'), 'utf8'),
      readFileSync(join(library.packageRoot, library.manifestFile), 'utf8'),
      `${library.id}: preview manifest drift`,
    );
  }
}

function checkResources(outputs) {
  for (const p of products) {
    const { pages, release } = p.spec;
    const prefix = p.basePath ? `${p.basePath.slice(1)}/` : '';
    for (const resource of p.resources) {
      assert(outputs.has(resourceOutput(resource)), `missing declared resource ${resource.path}`);
      assert.equal(productResourceUrl(resource), `${authority.origin}/${resource.path}`);
    }
    const manifest = JSON.parse(outputs.get(`${prefix}manifest.json`));
    const specification = JSON.parse(outputs.get(`${prefix}specification.json`));
    const rules = JSON.parse(outputs.get(`${prefix}rules.json`));
    assert.equal(manifest.canonical, p.base);
    assert.equal(manifest.version, p.version);
    assert.equal(manifest.updated, release.updated);
    assert.equal(specification.base, p.base);
    assert.equal(specification.version, p.version);
    assert.equal(specification.updated, release.updated);
    assert.equal(release.version, p.version);
    if (p.isLatest) {
      assert.equal(p.version, latest);
      assert.equal(p.updated, authority.updated);
    }
    assert.equal(rules.ruleCount, pages.flatMap((page) => page.sections).flatMap((section) => section.rules).length);
    assert.equal(rules.sections.length, 36, 'union section contract');
    for (const page of pages.filter((entry) => entry.slug !== 'common')) {
      const scoped = JSON.parse(outputs.get(`${prefix}${page.slug}/rules.json`));
      assert(scoped.sections.every((section) => section.platform === 'common' || section.platform === page.slug));
      assert(scoped.ruleCount < rules.ruleCount);
    }
  }
}

function checkRewrites(outputs) {
  const lines = outputs
    .get('_redirects')
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'));
  assert(lines.includes('/ /index.html 200'), 'root rewrite must be explicit and valid');
  assert(!lines.some((line) => line.includes('//') || line.startsWith(' ')), 'rewrites must not contain malformed paths');
  for (const line of lines.filter((entry) => entry.endsWith(' 200'))) {
    const [source, target, status, extra] = line.split(/\s+/);
    assert(source.startsWith('/') && target.startsWith('/') && status === '200' && extra === undefined, line);
    assert(outputs.has(target.slice(1)), `${line}: target must exist`);
  }
  assert(lines.includes('/cosmos-design / 301'));
  assert(lines.includes('/cosmos-design/* /:splat 301'));
  for (const line of [
    '/cosmos-design-skill.md /simt-design-skill.md 301',
    '/styles/cosmos-design.css /styles/site.css 301',
    '/assets/cosmos-design.js /assets/site.js 301',
    '/images/cosmos-design-system.png /images/simt-design-system.png 301',
    '/fonts/cosmos/* /fonts/:splat 301',
    '/v/2.4.0/cosmos-design-skill.md /v/2.4.0/simt-design-skill.md 301',
    '/v/2.3.0/cosmos-design-skill.md /v/2.3.0/simt-design-skill.md 301',
  ])
    assert(lines.includes(line), `missing legacy redirect ${line}`);
}

function checkEnvelope(outputs) {
  for (const path of [
    '404.html',
    'robots.txt',
    'sitemap.xml',
    'releases.xml',
    '.well-known/security.txt',
    'llms.txt',
    'llms-full.txt',
    'humans.txt',
    '.well-known/ard.json',
    '.well-known/ai-catalog.json',
    '_headers',
  ])
    assert(outputs.has(path), `missing discovery surface ${path}`);
  const notFound = outputs.get('404.html');
  assert(notFound.includes('noindex, nofollow'));
  assert(!notFound.includes('rel="canonical"'));
  const sitemap = outputs.get('sitemap.xml');
  const listed = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map((match) => match[1]);
  assert.deepEqual(listed.sort(), allRoutes.filter((route) => route.inSitemap).map(productRouteUrl).sort());
  assert(outputs.get('robots.txt').includes(`Sitemap: ${canonicalUrl('/sitemap.xml')}`));
  assert.equal(outputs.get('.well-known/ard.json'), outputs.get('.well-known/ai-catalog.json'));
  const catalog = JSON.parse(outputs.get('.well-known/ard.json'));
  assert.equal(catalog.host.documentationUrl, authority.origin);
  assert.equal(catalog.products[0].url, authority.origin);
  assert.equal(catalog.products[0].version, product.version);
  assert.equal(catalog.products[0].updatedAt, `${product.updated}T00:00:00Z`);
  for (const entry of catalog.entries) {
    assert.equal(entry.version, product.version);
    assert.equal(entry.updatedAt, `${product.updated}T00:00:00Z`);
    assert(entry.url.startsWith(`${authority.origin}/`));
  }
  const feed = outputs.get('releases.xml');
  assert(feed.includes(`<title>${product.label} ${product.version}</title>`));
  assert(feed.includes(`<updated>${product.updated}T00:00:00Z</updated>`));
  assert(feed.indexOf(product.version) < feed.indexOf('2.3.0'));
  const headers = outputs.get('_headers');
  for (const path of ['/robots.txt', '/sitemap.xml', '/releases.xml', '/llms.txt', '/llms-full.txt', '/.well-known/security.txt'])
    assert(headers.includes(`${path}\n`), `missing headers for ${path}`);
}

function checkAssets(outputs) {
  const stylesheet = read('styles/site.less');
  assert(!/(?:^|[;{\s])(left|right)\s*:/m.test(stylesheet), 'stylesheet must use logical placement');
  assert(!/(?:margin|padding|border)-(left|right)\s*:/.test(stylesheet), 'stylesheet must use logical box properties');
  assert(!/text-align\s*:\s*(?:left|right)\b/.test(stylesheet), 'stylesheet must use logical text alignment');
  for (const html of [...outputs].filter(([path]) => path.endsWith('.html')).map(([, value]) => value)) {
    for (const [, path] of html.matchAll(/(?:href|src)="(\/(?:assets|styles|images|fonts)\/[^" ]+)"/g))
      assert(existsSync(join(root, 'dist', path)), `missing self-hosted asset ${path}`);
  }
  const cssGzip = gzipSync(readFileSync(join(root, 'dist/styles/site.css'))).length;
  const jsGzip = gzipSync(readFileSync(join(root, 'dist/assets/site.js'))).length;
  assert(cssGzip <= budgetOf('cssGzipBytes'), `CSS gzip budget: ${cssGzip}`);
  assert(jsGzip <= budgetOf('jsGzipBytes'), `JS gzip budget: ${jsGzip}`);
  const initialFonts = [
    'dist/fonts/space-grotesk-latin-wght-normal.woff2',
    'dist/fonts/source-sans-3-latin-wght-normal.woff2',
  ].reduce((sum, path) => sum + statSync(join(root, path)).size, 0);
  assert(initialFonts <= budgetOf('initialFontsBytes'), `initial font budget: ${initialFonts}`);
}

function checkBuilt(outputs) {
  for (const [path, expected] of outputs) {
    const file = join(root, 'dist', path);
    assert(existsSync(file), `missing built output dist/${path}`);
    assert.equal(readFileSync(file, 'utf8'), expected, `built output drift: dist/${path}`);
  }
  const config = read('wrangler.jsonc');
  assert(config.includes('"name": "simt-design"'));
  assert(config.includes('"html_handling": "none"'));
  assert(config.includes('"workers_dev": false'));
  assert(config.includes('"pattern": "design.simt.app"'));
  assert(config.includes('"zone_name": "simt.app"'));
}

export function verify({ built = false } = {}) {
  build({ check: true });
  const outputs = generatedOutputs();
  for (const path of outputs.keys()) {
    assert(!path.startsWith('/') && !path.includes('//') && !path.includes('..'), `unsafe output path ${path}`);
  }
  checkRoutes(outputs);
  checkLibraries(outputs);
  checkResources(outputs);
  checkRewrites(outputs);
  checkEnvelope(outputs);
  if (built) {
    checkBuilt(outputs);
    checkAssets(outputs);
    checkLibraryAssets();
  }
  console.log(`Design: ${allRoutes.length} routes (${products.length - 1} versioned), ${JSON.parse(outputs.get('rules.json')).ruleCount} rules and ${outputs.size} deterministic outputs verified.`);
  return outputs.size;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    verify({ built: process.argv.includes('--built') });
  } catch (error) {
    console.error(`✗ ${error.message}`);
    process.exit(1);
  }
}
