import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { resolve, dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import less from 'less';
import { publishedLibraries, webLibraries } from '../src/design/library-previews.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const DIST = resolve(root, 'dist');
const STATIC = resolve(root, 'static');

/**
 * Publish everything that is not generated from src/design/:
 *   static/**            copied verbatim into dist/
 *   styles/site.less  compiled to dist/styles/site.css
 *   client/site.js    copied to dist/assets/site.js
 *
 * The site envelope (index, 404, robots, sitemap, _redirects, llms.txt,
 * _headers, agent catalogs) is not written here: scripts/site.js generates it
 * from the standalone product descriptor, so one pipeline owns the list.
 */
export async function buildAssets({ check = false } = {}) {
  const outputs = new Map();

  // Less is compiled here rather than by a bundler: the stylesheet is one file
  // with no JS, and the Worker serves static assets only.
  const entry = resolve(root, 'styles/site.less');
  const rendered = await less.render(readFileSync(entry, 'utf8'), {
    filename: entry,
    paths: [resolve(root, 'styles')],
    javascriptEnabled: true,
  });
  outputs.set('styles/site.css', rendered.css);

  for (const file of ['site.js']) {
    outputs.set(`assets/${file}`, readFileSync(resolve(root, 'client', file), 'utf8'));
  }

  outputs.set('assets/library-preview.js', readFileSync(resolve(root, 'client', 'library-preview.js'), 'utf8'));
  outputs.set('styles/library-preview.css', readFileSync(resolve(root, 'styles', 'library-preview.css'), 'utf8'));
  for (const library of webLibraries) {
    for (const file of library.files) {
      outputs.set(
        `${library.route}/runtime/src/${file}`,
        readFileSync(resolve(library.packageRoot, 'src', file), 'utf8'),
      );
    }
    outputs.set(`${library.route}/package.json`, readFileSync(resolve(library.packageRoot, 'package.json'), 'utf8'));
    outputs.set(
      `${library.route}/preview.manifest.js`,
      readFileSync(resolve(library.packageRoot, library.manifestFile), 'utf8'),
    );
  }
  for (const library of publishedLibraries.filter((entry) => entry.kind === 'native')) {
    for (const [file, source] of library.sources) {
      outputs.set(`${library.route}/source/src/${file}`, source);
    }
    for (const surface of library.apiSurfaces) {
      outputs.set(`${library.route}/api/${surface.path}`, surface.source);
    }
    for (const capture of library.captures) {
      outputs.set(
        `${library.route}/captures/${capture.capture}`,
        readFileSync(resolve(library.packageRoot, 'previews', capture.capture)),
      );
    }
    for (const source of library.captureSources) {
      outputs.set(`${library.route}/capture-source/${source.path}`, source.contents);
    }
    outputs.set(
      `${library.route}/preview.manifest.js`,
      readFileSync(resolve(library.packageRoot, library.manifestFile), 'utf8'),
    );
  }

  for (const [path, contents] of outputs) {
    const absolute = resolve(DIST, path);
    if (check) {
      let actual;
      try {
        actual = readFileSync(absolute);
      } catch {
        throw new Error(`Missing built file: dist/${path}`);
      }
      const matches = Buffer.isBuffer(contents)
        ? actual.equals(contents)
        : actual.equals(Buffer.from(contents));
      if (!matches) throw new Error(`Built output drift: dist/${path}. Run npm run build.`);
    } else {
      mkdirSync(dirname(absolute), { recursive: true });
      writeFileSync(absolute, contents);
    }
  }

  // static/ is authoritative for fonts, images and the icon. dist/ is emptied by
  // scripts/build.js before it writes; this only adds to it. It is checked as
  // well as written: a copy that silently did not happen left a page with no
  // stylesheet and no font, and nothing else in the pipeline looks at it.
  //
  // Finder writes .DS_Store into every directory it is shown, and static/ is
  // shown in Finder, so on any Mac that has visited this repository there is a
  // file here that is not an asset. Copying it verbatim would publish the
  // metadata at /.DS_Store and make both the checked set and the count the build
  // log reports depend on which machine built. A dotfile is therefore never an
  // asset: the two the origin genuinely answers, _headers and _redirects, are
  // written by scripts/site.js and never travel through here.
  const copied = readdirSync(STATIC, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile() && !entry.name.startsWith('.'))
    .map((entry) => `${entry.parentPath.slice(STATIC.length + 1)}/${entry.name}`.replace(/^\//, ''));
  if (check) {
    for (const file of copied) {
      let actual;
      try {
        actual = readFileSync(resolve(DIST, file));
      } catch {
        throw new Error(`Missing static asset: dist/${file}. Run npm run build.`);
      }
      if (!actual.equals(readFileSync(resolve(STATIC, file)))) {
        throw new Error(`Built output drift: dist/${file}. Run npm run build.`);
      }
    }
  } else {
    mkdirSync(DIST, { recursive: true });
    cpSync(STATIC, DIST, { recursive: true, filter: (from) => !basename(from).startsWith('.') });
  }

  // Every file this module puts on disk, because the count is what the build log
  // claims it wrote and `outputs.size` alone was two of the several dozen.
  return outputs.size + copied.length;
}

/**
 * Cache policy. Fingerprint-free URLs are revalidated so a specification release
 * is visible immediately; the fonts and the hero sky are content-stable and get
 * a long immutable window. The design exports also pin their content type,
 * because the extension alone leaves .md and .json at the mercy of whatever the
 * edge guesses for an unknown type.
 */
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  if (!existsSync(DIST)) mkdirSync(DIST, { recursive: true });
  buildAssets({ check: process.argv.includes('--check') })
    .then((count) =>
      console.log(`Assets: ${count} files ${process.argv.includes('--check') ? 'verified' : 'written'}.`),
    )
    .catch((error) => {
      console.error(error.message);
      process.exit(1);
    });
}
