import assert from 'node:assert/strict';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';
import { build, generatedOutputs } from './build.js';
import { authority, canonicalUrl } from '../src/site/authority.js';
import product, { allRoutes, budgetOf, latest, products, productResourceUrl, productRouteUrl } from '../src/site/product.js';

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
    assert.equal(specification.base, p.base);
    assert.equal(specification.version, p.version);
    assert.equal(release.version, p.version);
    if (p.isLatest) assert.equal(p.version, latest);
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
  const headers = outputs.get('_headers');
  for (const path of ['/robots.txt', '/sitemap.xml', '/releases.xml', '/llms.txt', '/llms-full.txt', '/.well-known/security.txt'])
    assert(headers.includes(`${path}\n`), `missing headers for ${path}`);
}

function checkAssets(outputs) {
  const stylesheet = read('styles/cosmos-design.less');
  assert(!/(?:^|[;{\s])(left|right)\s*:/m.test(stylesheet), 'stylesheet must use logical placement');
  assert(!/(?:margin|padding|border)-(left|right)\s*:/.test(stylesheet), 'stylesheet must use logical box properties');
  assert(!/text-align\s*:\s*(?:left|right)\b/.test(stylesheet), 'stylesheet must use logical text alignment');
  for (const html of [...outputs].filter(([path]) => path.endsWith('.html')).map(([, value]) => value)) {
    for (const [, path] of html.matchAll(/(?:href|src)="(\/(?:assets|styles|images|fonts)\/[^" ]+)"/g))
      assert(existsSync(join(root, 'dist', path)), `missing self-hosted asset ${path}`);
  }
  const cssGzip = gzipSync(readFileSync(join(root, 'dist/styles/cosmos-design.css'))).length;
  const jsGzip = gzipSync(readFileSync(join(root, 'dist/assets/cosmos-design.js'))).length;
  assert(cssGzip <= budgetOf('cssGzipBytes'), `CSS gzip budget: ${cssGzip}`);
  assert(jsGzip <= budgetOf('jsGzipBytes'), `JS gzip budget: ${jsGzip}`);
  const initialFonts = [
    'dist/fonts/cosmos/space-grotesk-latin-wght-normal.woff2',
    'dist/fonts/cosmos/source-sans-3-latin-wght-normal.woff2',
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
  assert(config.includes('"name": "simt-cosmos"'));
  assert(config.includes('"html_handling": "none"'));
  assert(config.includes('"workers_dev": false'));
  assert(config.includes('"pattern": "cosmos.simt.app"'));
  assert(config.includes('"zone_name": "simt.app"'));
}

export function verify({ built = false } = {}) {
  build({ check: true });
  const outputs = generatedOutputs();
  for (const path of outputs.keys()) {
    assert(!path.startsWith('/') && !path.includes('//') && !path.includes('..'), `unsafe output path ${path}`);
  }
  checkRoutes(outputs);
  checkResources(outputs);
  checkRewrites(outputs);
  checkEnvelope(outputs);
  if (built) {
    checkBuilt(outputs);
    checkAssets(outputs);
  }
  console.log(`Cosmos: ${allRoutes.length} routes (${products.length - 1} versioned), ${JSON.parse(outputs.get('rules.json')).ruleCount} rules and ${outputs.size} deterministic outputs verified.`);
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
