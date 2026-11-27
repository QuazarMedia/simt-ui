import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { generatedOutputs } from './build.js';
import product, { versionedProducts } from '../src/site/product.js';
import {
  libraryCoverage,
  publishedLibraries,
  webLibraries,
} from '../src/design/library-previews.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outputs = generatedOutputs();

test('the registry publishes libraries only at the current authority root', () => {
  assert(publishedLibraries.length > 0);
  assert(product.routes.some((route) => route.path === 'libraries'));
  for (const library of publishedLibraries) {
    assert(product.routes.some((route) => route.path === library.route));
    assert.equal(outputs.get(`${library.route}/index.html`)?.includes(library.metadata.name), true);
  }
  assert(versionedProducts.every((entry) => entry.routes.every((route) => !route.path.startsWith('libraries'))));
  assert(outputs.get('sitemap.xml').includes('https://design.simt.app/libraries'));
});

test('package source and exports are discovered without a second component registry', () => {
  for (const library of webLibraries) {
    const discovered = readdirSync(resolve(library.packageRoot, 'src'))
      .filter((file) => /\.(?:mjs|css)$/.test(file))
      .sort();
    assert.deepEqual(library.files, discovered);
    const rootSource = readFileSync(resolve(library.packageRoot, library.runtimeEntry), 'utf8');
    const coverage = libraryCoverage(library);
    for (const name of coverage.exports) assert(rootSource.includes(name));
    assert.equal(library.metadata.name, JSON.parse(readFileSync(resolve(library.packageRoot, 'package.json'), 'utf8')).name);
    assert(library.manifest.usage.includes(library.metadata.name));
    assert.equal(library.metadata.version, '0.1.0');
  }
});

test('every manifest component reaches both the catalog and the live document', () => {
  for (const library of webLibraries) {
    const page = outputs.get(`${library.route}/index.html`);
    const preview = outputs.get(`${library.previewPath}/index.html`);
    for (const component of library.manifest.components) {
      assert(page.includes(`id="component-${component.id}"`), `${library.id}: catalog ${component.id}`);
      assert(preview.includes(`data-preview-component="${component.id}"`), `${library.id}: preview ${component.id}`);
    }
    assert(preview.includes('noindex, nofollow'));
    assert(!preview.includes('rel="canonical"'));
    assert(!/<(?:script|link)[^>]+(?:src|href)="https?:\/\//.test(preview));
  }
});

test('the playground client and prepaint script do not persist or transmit state', () => {
  const client = readFileSync(resolve(root, 'client/library-preview.js'), 'utf8');
  assert(!/fetch\(|XMLHttpRequest|document\.cookie|localStorage|sessionStorage/.test(client));
  for (const library of webLibraries) {
    const preview = outputs.get(`${library.previewPath}/index.html`);
    const bootstrap = preview.match(/<script>([\s\S]*?)<\/script>/)?.[1] ?? '';
    assert(!/fetch\(|XMLHttpRequest|document\.cookie|localStorage|sessionStorage/.test(bootstrap));
  }
});
