import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { resolve, dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import less from 'less';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const DIST = resolve(root, 'dist');
const STATIC = resolve(root, 'static');

/**
 * Publish everything that is not generated from src/design/:
 *   static/**            copied verbatim into dist/
 *   styles/cosmos-design.less  compiled to dist/styles/cosmos-design.css
 *   client/cosmos-design.js    copied to dist/assets/cosmos-design.js
 *
 * The site envelope (index, 404, robots, sitemap, _redirects, llms.txt,
 * _headers, agent catalogs) is not written here: scripts/site.js generates it
 * from the standalone product descriptor, so one pipeline owns the list.
 */
export async function buildAssets({ check = false } = {}) {
  const outputs = new Map();

  // Less is compiled here rather than by a bundler: the stylesheet is one file
  // with no JS, and the Worker serves static assets only.
  const entry = resolve(root, 'styles/cosmos-design.less');
  const rendered = await less.render(readFileSync(entry, 'utf8'), {
    filename: entry,
    paths: [resolve(root, 'styles')],
    javascriptEnabled: true,
  });
  outputs.set('styles/cosmos-design.css', rendered.css);

  for (const file of ['cosmos-design.js']) {
    outputs.set(`assets/${file}`, readFileSync(resolve(root, 'client', file), 'utf8'));
  }

  for (const [path, contents] of outputs) {
    const absolute = resolve(DIST, path);
    if (check) {
      let actual;
      try {
        actual = readFileSync(absolute, 'utf8');
      } catch {
        throw new Error(`Missing built file: dist/${path}`);
      }
      if (actual !== contents) throw new Error(`Built output drift: dist/${path}. Run npm run build.`);
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
