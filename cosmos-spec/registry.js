import { existsSync, readdirSync } from 'node:fs';
import { registerHooks } from 'node:module';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const SPEC_ROOT = dirname(fileURLToPath(import.meta.url));
const SEMVER_RE = /^(\d+)\.(\d+)\.(\d+)$/;

function compareSemverDesc(a, b) {
  const pa = a.match(SEMVER_RE);
  const pb = b.match(SEMVER_RE);
  if (!pa || !pb) return b.localeCompare(a);
  for (let i = 1; i <= 3; i += 1) {
    const diff = Number(pb[i]) - Number(pa[i]);
    if (diff !== 0) return diff;
  }
  return 0;
}

// Upstream 2.3.0 specification.js imports ../../products/space.js from the
// simt-space repository. Keep the pinned upstream file byte-identical and
// satisfy that import with an inert placeholder; loadSpec() overrides
// release.base and releasePath per target.
let hooksRegistered = false;
function ensureUpstreamHooks() {
  if (hooksRegistered) return;
  hooksRegistered = true;
  const virtualId = 'cosmos-spec:upstream-space-shim';
  registerHooks({
    resolve(specifier, context, nextResolve) {
      if (
        specifier === '../../products/space.js' &&
        context.parentURL &&
        context.parentURL.includes('/cosmos-spec/upstream/')
      ) {
        return { url: virtualId, shortCircuit: true };
      }
      return nextResolve(specifier, context);
    },
    load(url, context, nextLoad) {
      if (url === virtualId) {
        return {
          format: 'module',
          shortCircuit: true,
          source:
            "export const space = { origin: 'https://cosmos.simt.app' };\nexport const subContextPath = () => '';\n",
        };
      }
      return nextLoad(url, context);
    },
  });
}

function versionSourceDir(version) {
  const authored = resolve(SPEC_ROOT, 'versions', version);
  if (existsSync(join(authored, 'specification.js'))) {
    return { dir: authored, upstream: false };
  }
  const upstream = resolve(SPEC_ROOT, 'upstream', version, 'cosmos', 'design');
  if (existsSync(join(upstream, 'specification.js'))) {
    return { dir: upstream, upstream: true };
  }
  return null;
}

/**
 * List all Cosmos specification versions available in cosmos-spec, newest first.
 */
export function listVersions() {
  const versions = new Set();
  for (const [base, subdir] of [
    ['upstream', (v) => join(SPEC_ROOT, 'upstream', v, 'cosmos', 'design', 'specification.js')],
    ['versions', (v) => join(SPEC_ROOT, 'versions', v, 'specification.js')],
  ]) {
    const parent = resolve(SPEC_ROOT, base);
    if (!existsSync(parent)) continue;
    for (const entry of readdirSync(parent, { withFileTypes: true })) {
      if (entry.isDirectory() && SEMVER_RE.test(entry.name) && existsSync(subdir(entry.name))) {
        versions.add(entry.name);
      }
    }
  }
  return [...versions].sort(compareSemverDesc);
}

export function latestVersion() {
  const versions = listVersions();
  if (versions.length === 0) throw new Error('No Cosmos specification versions found in cosmos-spec');
  return versions[0];
}

const rawModuleCache = new Map();

async function importVersionModules(version) {
  if (rawModuleCache.has(version)) return rawModuleCache.get(version);
  const found = versionSourceDir(version);
  if (!found) {
    throw new Error(`Unknown Cosmos specification version: ${version}. Available: ${listVersions().join(', ')}`);
  }
  if (found.upstream) ensureUpstreamHooks();

  const importFrom = (file) => import(pathToFileURL(join(found.dir, file)).href);
  const [tokens, color, motion, components, contrast, starfield, specification] = await Promise.all([
    importFrom('tokens.js'),
    importFrom('color.js'),
    importFrom('motion.js'),
    importFrom('components.js'),
    importFrom('contrast.js'),
    importFrom('starfield.js'),
    importFrom('specification.js'),
  ]);

  const bundle = {
    dir: found.dir,
    tokens,
    color,
    motion,
    components,
    contrast,
    starfield,
    specification,
    licenseFiles: {
      license: join(found.dir, 'LICENSE'),
      licenseDocs: join(found.dir, 'LICENSE-DOCS'),
    },
  };
  rawModuleCache.set(version, bundle);
  return bundle;
}

/**
 * Load a Cosmos specification version scoped to a target origin and basePath.
 */
export async function loadSpec(
  version = latestVersion(),
  { origin = 'https://cosmos.simt.app', basePath = '', owner } = {},
) {
  const normalizedBasePath = basePath ? `/${String(basePath).replace(/^\/+|\/+$/g, '')}` : '';
  const base = normalizedBasePath ? `${origin}${normalizedBasePath}` : origin;
  const raw = await importVersionModules(version);
  const { specification } = raw;

  return {
    version,
    origin,
    basePath: normalizedBasePath,
    releasePath: normalizedBasePath,
    release: {
      ...specification.release,
      base,
      ...(owner ? { owner } : {}),
    },
    pages: specification.pages,
    sources: specification.sources,
    provenance: specification.provenance,
    faqs: specification.faqs,
    STATE_TREATMENTS: specification.STATE_TREATMENTS,
    tokens: raw.tokens,
    color: raw.color,
    motion: raw.motion,
    components: raw.components,
    contrast: raw.contrast,
    starfield: raw.starfield,
    licenseFiles: raw.licenseFiles,
  };
}
