import assert from 'node:assert/strict';
import test from 'node:test';
import { generatedOutputs } from './build.js';
import { authority } from '../src/site/authority.js';
import { allRoutes, productRouteUrl } from '../src/site/product.js';

const outputs = generatedOutputs();
const pages = allRoutes.map((route) => [route.path ? `${route.path}/index.html` : 'index.html', outputs.get(route.path ? `${route.path}/index.html` : 'index.html'), route]);
const links = (html) =>
  [...html.matchAll(/<link\b[^>]*>/g)].map(([tag]) =>
    Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(([, key, value]) => [key, value])),
  );
const canonicalOf = (html) => links(html).find((link) => link.rel === 'canonical')?.href;
const meta = (html, name) => html.match(new RegExp(`<meta (?:name|property)="${name}" content="([^"]*)">`))?.[1];
const graphOf = (html) => JSON.parse(html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)[1])['@graph'];

 test('every route has unique canonical metadata and preview fields', () => {
  const titles = new Set();
  const descriptions = new Set();
  const canonicals = new Set();
  for (const [file, html, route] of pages) {
    const title = html.match(/<title>([^<]+)<\/title>/)?.[1];
    const description = meta(html, 'description');
    assert(title && description, file);
    assert(!titles.has(title), `${file}: repeated title`);
    assert(!descriptions.has(description), `${file}: repeated description`);
    titles.add(title);
    descriptions.add(description);
    assert.equal(canonicalOf(html), productRouteUrl(route), file);
    assert(!canonicals.has(canonicalOf(html)), `${file}: repeated canonical`);
    canonicals.add(canonicalOf(html));
    assert.equal(meta(html, 'og:url'), canonicalOf(html), file);
    assert.equal(meta(html, 'og:description'), description, file);
    assert.equal(meta(html, 'twitter:description'), description, file);
    assert.equal(meta(html, 'og:image'), meta(html, 'twitter:image'), file);
    assert(meta(html, 'og:image:alt') && meta(html, 'twitter:image:alt'), file);
  }
});

test('sitemap lists exactly canonical HTML routes', () => {
  const listed = [...outputs.get('sitemap.xml').matchAll(/<loc>([^<]+)<\/loc>/g)].map((match) => match[1]);
  assert.deepEqual(listed.sort(), pages.map(([, html]) => canonicalOf(html)).sort());
});

test('structured data resolves locally and names standalone authority', () => {
  for (const [file, html] of pages) {
    const graph = graphOf(html);
    const defined = new Set(graph.map((node) => node['@id']).filter(Boolean));
    const walk = (node) => {
      if (!node || typeof node !== 'object') return;
      if (node['@id']) assert(defined.has(node['@id']), `${file}: unresolved ${node['@id']}`);
      for (const value of Object.values(node)) walk(value);
    };
    walk(graph);
    const article = graph.find((node) => node['@type'] === 'TechArticle');
    if (article) assert.equal(article.publisher.url, `${authority.origin}/`, file);
  }
});

test('Markdown alternates transcribe their own routes with canonical headers', () => {
  const headerBlocks = outputs.get('_headers').split(/\n\n/);
  for (const [file, html, route] of pages) {
    const alternates = links(html).filter((link) => link.rel === 'alternate' && link.type === 'text/markdown');
    assert.deepEqual(alternates.map((link) => link.href), route.markdown ? [`${authority.origin}/${route.markdown}`] : [], file);
    if (!route.markdown) continue;
    assert(outputs.has(route.markdown), route.markdown);
    assert(headerBlocks.some((block) => block.startsWith(`/${route.markdown}\n`) && block.includes(`Link: <${canonicalOf(html)}>; rel="canonical"`)), route.markdown);
  }
});

test('404 is a true unindexable error page', () => {
  const html = outputs.get('404.html');
  assert.match(html, /content="noindex, nofollow"/);
  assert.equal(canonicalOf(html), undefined);
  assert.equal((html.match(/<h1[ >]/g) ?? []).length, 1);
});

test('all discovery references use the canonical origin', () => {
  for (const path of ['robots.txt', 'sitemap.xml', 'releases.xml', 'llms.txt', 'llms-full.txt', '.well-known/ard.json']) {
    const value = outputs.get(path);
    assert(value.includes(authority.origin), path);
    assert(!value.includes('https://simt.space/cosmos/design'), path);
  }
});

test('releases feed and asset headers contain no placeholder text or duplicate rules', () => {
  const releases = outputs.get('releases.xml');
  assert(!releases.includes('>undefined<'), 'releases.xml must not emit undefined summaries');
  assert(!releases.includes('>null<'), 'releases.xml must not emit null summaries');
  const headerPaths = outputs
    .get('_headers')
    .split(/\n\n+/)
    .map((block) => block.split('\n')[0].trim())
    .filter((line) => line && !line.startsWith('#'));
  assert.equal(new Set(headerPaths).size, headerPaths.length, '_headers must not contain duplicate path rules');
});
