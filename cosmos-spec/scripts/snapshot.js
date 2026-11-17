import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { lstatSync, readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import { validateSemantics } from './semantics.js';

export const VERSION = '2.3.0';
export const UPDATED = '2026-10-03';
export const COMMIT = '637edcf633b4e7d6407b4f9a715dad359c41fce8';
export const PROVENANCE_SHA256 = 'f9410dee5344bd3a101c66941dd4fb245c455d30fb659dab5e3732532ab8f86d';
export const BASE = 'https://simt.space/cosmos/design';
export const SNAPSHOT = `snapshots/${VERSION}`;
export const PARTS = ['mobile-app', 'web-page', 'web-app', 'watch-app'];
export const SOURCE_NAMES = ['specification', 'tokens', 'motion', 'components', 'contrast', 'color', 'starfield'];
export const EXPORTS = [
  ...['manifest', 'specification', 'rules', 'tokens', 'contrast-pairs', 'motion', 'components'].map((name) => `${name}.json`),
  ...['specification', 'rules'].map((name) => `${name}.schema.json`),
  ...['specification', 'common', ...PARTS].map((name) => `${name}.md`),
  ...PARTS.flatMap((part) => ['tokens', 'rules'].map((name) => `${part}/${name}.json`)),
].sort();
export const REVIEWED_PATHS = [
  ...SOURCE_NAMES.map((name) => `upstream/${VERSION}/cosmos/design/${name}.js`),
  ...EXPORTS.map((name) => `${SNAPSHOT}/${name}`),
].sort();
export const sha256 = (bytes) => createHash('sha256').update(bytes).digest('hex');

function walk(root, relative, files, directories) {
  const stat = lstatSync(join(root, relative));
  assert(!stat.isSymbolicLink(), `Symlink forbidden: ${relative}`);
  if (stat.isDirectory()) {
    directories.push(relative);
    for (const name of readdirSync(join(root, relative)).sort()) walk(root, `${relative}/${name}`, files, directories);
  } else {
    assert(stat.isFile(), `Non-regular file forbidden: ${relative}`);
    files.push(relative);
  }
}

// Separate inventory reads let tests reach semantic validators without bypassing the production pin.
export function readInventory(root) {
  assert(lstatSync(root).isDirectory() && !lstatSync(root).isSymbolicLink(), 'Snapshot root must be a real directory');
  const files = [];
  const directories = [];
  for (const path of ['snapshots', 'upstream', 'provenance.json']) walk(root, path, files, directories);
  assert.deepEqual(files.sort(), [...REVIEWED_PATHS, 'provenance.json'].sort(), 'Exact allowlisted inventory mismatch');
  const expectedDirectories = new Set();
  for (const path of REVIEWED_PATHS) {
    for (let dir = dirname(path); dir !== '.'; dir = dirname(dir)) expectedDirectories.add(dir);
  }
  assert.deepEqual(directories.sort(), [...expectedDirectories].sort(), 'Unexpected snapshot/source directory');
  const provenanceBytes = readFileSync(join(root, 'provenance.json'));
  const provenance = JSON.parse(provenanceBytes);
  assert.deepEqual(provenance.inventory.map((entry) => entry.path), REVIEWED_PATHS, 'Provenance inventory mismatch');
  const documents = {};
  for (const entry of provenance.inventory) {
    const upstreamPath = entry.path.startsWith('upstream/')
      ? entry.path.slice(`upstream/${VERSION}/`.length)
      : `dist/cosmos/design/${entry.path.slice(`${SNAPSHOT}/`.length)}`;
    assert.equal(entry.upstreamPath, upstreamPath, `Source layout mismatch: ${entry.path}`);
    const bytes = readFileSync(join(root, entry.path));
    assert.equal(sha256(bytes), entry.sha256, `SHA256 mismatch: ${entry.path}`);
    if (entry.path.startsWith(`${SNAPSHOT}/`) && entry.path.endsWith('.json')) {
      documents[entry.path.slice(`${SNAPSHOT}/`.length)] = JSON.parse(bytes);
    }
  }
  return { root, provenance, provenanceBytes, documents };
}

export function verifyPin(snapshot) {
  const { provenance: p, provenanceBytes, root, documents } = snapshot;
  assert.equal(sha256(provenanceBytes), PROVENANCE_SHA256, 'Frozen provenance changed; a build cannot refresh the source pin or hashes');
  assert.equal(p.upstream.commit, COMMIT, 'Upstream commit pin mismatch');
  assert.equal(p.generator.commit, COMMIT, 'Generator commit pin mismatch');
  assert.equal(p.generator.version, VERSION, 'Generator version mismatch');
  assert.equal(p.upstream.version, VERSION, 'Upstream version mismatch');
  assert.equal(p.upstream.updated, UPDATED, 'Upstream date mismatch');
  assert.equal(p.upstream.canonical, documents['manifest.json'].canonical, 'Canonical source mismatch');
  assert.equal(p.distribution.scope, 'private-development-only');
  assert.equal(p.distribution.publicRedistribution, 'blocked');
  assert.equal(p.licenses.code.status, 'unconfirmed');
  assert.equal(p.licenses.documents.status, 'unconfirmed');
  const authored = readFileSync(join(root, `upstream/${VERSION}/cosmos/design/specification.js`), 'utf8');
  // Read inert bytes only: specification.js imports the excluded site descriptor.
  assert.equal(authored.match(/export const release = \{\s*version: '([^']+)',\s*updated: '([^']+)'/)?.[1], VERSION);
  assert.equal(authored.match(/export const release = \{\s*version: '([^']+)',\s*updated: '([^']+)'/)?.[2], UPDATED);
}

export function localValidator(documents) {
  // strictRequired only lints the legal undeclared implementationStatus property; required validation stays strict.
  const ajv = new Ajv2020({ allErrors: true, strict: true, strictRequired: false });
  addFormats(ajv);
  for (const name of ['specification', 'rules']) {
    const schema = documents[`${name}.schema.json`];
    assert.equal(schema.$id, `${BASE}/${name}.schema.json`, `Unregistered schema ID: ${name}`);
    ajv.addSchema(schema);
  }
  // Synchronous compilation; no loadSchema and no remote resolver or network.
  for (const name of ['specification', 'rules']) ajv.getSchema(`${BASE}/${name}.schema.json`);
  return ajv;
}

export function canonicalSchemaErrors(documents) {
  const validator = localValidator(documents);
  const failures = [];
  for (const [file, document] of Object.entries(documents).sort(([a], [b]) => a.localeCompare(b))) {
    if (file.endsWith('.schema.json') || !document.$schema) continue;
    assert([`${BASE}/specification.schema.json`, `${BASE}/rules.schema.json`].includes(document.$schema), `Offline: unregistered declared schema in ${file}: ${document.$schema}`);
    const validate = validator.getSchema(document.$schema);
    if (!validate(document)) {
      failures.push(...validate.errors.map((error) => ({
        file: `${SNAPSHOT}/${file}`,
        instancePath: error.instancePath,
        schemaPath: error.schemaPath,
        keyword: error.keyword,
        params: error.params,
        message: error.message,
        ...(error.instancePath === '/sections' ? { actualSections: document.sections.length } : {}),
      })));
    }
  }
  return failures;
}

export function verifyKnownDefects(snapshot, failures) {
  const expected = PARTS.map((part) => ({
    file: `${SNAPSHOT}/${part}/rules.json`,
    instancePath: '/sections',
    schemaPath: '#/properties/sections/minItems',
    keyword: 'minItems',
    params: { limit: 36 },
    message: 'must NOT have fewer than 36 items',
    actualSections: part === 'mobile-app' || part === 'web-app' ? 17 : 16,
  }));
  const byFile = (a, b) => a.file.localeCompare(b.file);
  assert.deepEqual(failures.toSorted(byFile), expected.toSorted(byFile), 'Private integrity permits ONLY the four exact known upstream scoped minItems defects; this is not full conformance');
  for (const defect of snapshot.provenance.knownSchemaDefects) {
    const observed = failures.find((entry) => entry.file === defect.file);
    assert(observed && observed.actualSections === defect.actualSections && observed.params.limit === defect.limit, `Provenance defect mismatch: ${defect.file}`);
  }
}

export function validateContent(documents) {
  return validateSemantics(documents, { version: VERSION, updated: UPDATED, base: BASE, parts: PARTS });
}

export function checkSnapshot(root) {
  const snapshot = readInventory(root);
  verifyPin(snapshot);
  const failures = canonicalSchemaErrors(snapshot.documents);
  verifyKnownDefects(snapshot, failures);
  const counts = validateContent(snapshot.documents);
  return { ...snapshot, failures, counts };
}
