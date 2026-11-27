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
  nativeLibraries,
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

test('native catalogs derive version, source, ABI and captures from package-owned evidence', () => {
  assert(nativeLibraries.length > 0);
  for (const library of nativeLibraries) {
    assert.equal(library.metadata.name, 'app.simt:app-ui');
    const version = readFileSync(resolve(library.packageRoot, '../gradle.properties'), 'utf8')
      .match(/^VERSION_NAME=(.*)$/m)[1]
      .trim();
    assert.equal(library.metadata.version, version);
    assert(library.files.includes('commonMain/kotlin/app/simt/ui/components/forms/ClearableTextField.kt'));
    assert(library.files.every((file) => /^(?:commonMain|androidMain|iosMain)\/kotlin\//.test(file)));
    assert.equal(library.apiFiles.length, 2);
    assert(libraryCoverage(library).declarationCount > 50);
    for (const surface of library.apiSurfaces) assert(surface.declarations.length > 0);
    const declarations = library.apiSurfaces.flatMap((surface) => surface.declarations);
    assert(declarations.some((entry) => entry.kind === 'interface' && entry.name === 'BlockingLoaderController'));
    assert(declarations.every((entry) => entry.name !== 'class'));
    assert(declarations.filter((entry) => entry.kind === 'fun' && entry.name === 'WindowedList').length >= 2);
    assert.deepEqual(library.metadata.sourceRoots, ['commonMain/kotlin', 'androidMain/kotlin', 'iosMain/kotlin']);
    assert.equal(library.captures.length, library.manifest.components.length);
    assert.deepEqual(
      library.captures.map((capture) => capture.capture).sort(),
      readdirSync(resolve(library.packageRoot, 'previews')).filter((file) => file.endsWith('.png')).sort(),
    );
    for (const capture of library.captures) {
      const bytes = readFileSync(resolve(library.packageRoot, 'previews', capture.capture));
      assert.deepEqual([...bytes.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
      assert(capture.width > 0 && capture.height > 0);
    }
  }
});

test('native catalogs show real captures without implying browser execution or device evidence', () => {
  for (const library of nativeLibraries) {
    const page = outputs.get(`${library.route}/index.html`);
    assert(page.includes('id="native-previews"'));
    assert(page.includes('Robolectric Android host capture'));
    assert(page.includes('not emulator or physical-device evidence'));
    assert(page.includes('iOS launch, bundled-resource rendering'));
    assert(!page.includes('<iframe'));
    assert(!outputs.has(`${library.route}/preview/index.html`));
    for (const component of library.manifest.components) {
      assert(page.includes(`id="component-${component.id}"`));
      assert(page.includes(`${library.route}/captures/${component.capture}`));
      for (const source of component.source) assert(page.includes(`${library.route}/source/${source}`));
    }
    for (const file of library.files) assert(page.includes(`${library.route}/source/src/${file}`));
    for (const surface of library.apiSurfaces) assert(page.includes(`${library.route}/api/${surface.path}`));
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
