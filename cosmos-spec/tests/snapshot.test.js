import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { createArchive } from '../scripts/check.js';
import { canonicalSchemaErrors, checkSnapshot, EXPORTS, PARTS, readInventory, REVIEWED_PATHS, sha256, SNAPSHOT, validateContent, verifyKnownDefects, verifyPin } from '../scripts/snapshot.js';
import { curveSamples } from '../scripts/semantics.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const fixtureParent = join(root, 'build', 'test-fixtures');

function fixture(t) {
  mkdirSync(fixtureParent, { recursive: true });
  const temporary = mkdtempSync(join(fixtureParent, 'snapshot-'));
  for (const path of [...REVIEWED_PATHS, 'provenance.json']) {
    mkdirSync(dirname(join(temporary, path)), { recursive: true });
    cpSync(join(root, path), join(temporary, path));
  }
  t.after(() => rmSync(temporary, { recursive: true, force: true }));
  return temporary;
}

// Only disposable fixtures are rehashed; the production frozen-provenance gate still rejects them.
function mutate(temporary, name, change) {
  const path = `${SNAPSHOT}/${name}`;
  const document = JSON.parse(readFileSync(join(temporary, path)));
  change(document);
  const bytes = Buffer.from(`${JSON.stringify(document, null, 2)}\n`);
  writeFileSync(join(temporary, path), bytes);
  const provenance = JSON.parse(readFileSync(join(temporary, 'provenance.json')));
  provenance.inventory.find((entry) => entry.path === path).sha256 = sha256(bytes);
  writeFileSync(join(temporary, 'provenance.json'), `${JSON.stringify(provenance, null, 2)}\n`);
  const snapshot = readInventory(temporary);
  assert.throws(() => verifyPin(snapshot), /Frozen provenance changed/);
  return snapshot;
}

test('intact pinned snapshot passes private integrity without full conformance', () => {
  const snapshot = checkSnapshot(root);
  assert.deepEqual(snapshot.counts, { exports: 23, sources: 7, rules: 80, sections: 36, profiles: 4, contrastPairs: 212, motifs: 7, curves: 16, components: 17 });
  assert.equal(EXPORTS.length, 23);
  assert.equal(REVIEWED_PATHS.length, 30);
  assert.equal(snapshot.failures.length, 4);
  assert.equal(snapshot.provenance.authority.repository, 'simt-space');
  assert.match(snapshot.provenance.authority.iosAdapter, /No normative iOS adapter/);
  assert.equal(snapshot.provenance.distribution.publicRedistribution, 'blocked');
  assert.equal(snapshot.provenance.licenses.code.status, 'unconfirmed');
  assert.equal(snapshot.provenance.licenses.documents.status, 'unconfirmed');
});

test('missing export is rejected', (t) => {
  const temporary = fixture(t);
  unlinkSync(join(temporary, SNAPSHOT, 'common.md'));
  assert.throws(() => checkSnapshot(temporary), /allowlisted inventory mismatch/);
});

test('extra export is rejected', (t) => {
  const temporary = fixture(t);
  writeFileSync(join(temporary, SNAPSHOT, 'tokens.css'), 'not allowed');
  assert.throws(() => checkSnapshot(temporary), /allowlisted inventory mismatch/);
});

test('extra source/version and even empty directories are rejected', (t) => {
  const temporary = fixture(t);
  mkdirSync(join(temporary, 'upstream', '2.4.0'));
  assert.throws(() => checkSnapshot(temporary), /Unexpected snapshot\/source directory/);
});

for (const path of [`${SNAPSHOT}/tokens.json`, 'upstream/2.3.0/cosmos/design/motion.js']) {
  test(`byte tampering rejected: ${path}`, (t) => {
    const temporary = fixture(t);
    writeFileSync(join(temporary, path), `${readFileSync(join(temporary, path), 'utf8')}\n`);
    assert.throws(() => checkSnapshot(temporary), /SHA256 mismatch/);
  });
}

test('file symlink is rejected without reading the target', (t) => {
  const temporary = fixture(t);
  unlinkSync(join(temporary, SNAPSHOT, 'tokens.json'));
  symlinkSync(join(root, SNAPSHOT, 'tokens.json'), join(temporary, SNAPSHOT, 'tokens.json'));
  assert.throws(() => checkSnapshot(temporary), /Symlink forbidden/);
});

test('directory symlink traversal is rejected', (t) => {
  const temporary = fixture(t);
  rmSync(join(temporary, SNAPSHOT, 'web-page'), { recursive: true });
  symlinkSync(join(root, SNAPSHOT, 'web-page'), join(temporary, SNAPSHOT, 'web-page'));
  assert.throws(() => checkSnapshot(temporary), /Symlink forbidden/);
});

test('provenance cannot silently accept a newer pin or refreshed inventory', (t) => {
  const temporary = fixture(t);
  const path = join(temporary, 'provenance.json');
  const provenance = JSON.parse(readFileSync(path));
  provenance.upstream.commit = '0'.repeat(40);
  writeFileSync(path, JSON.stringify(provenance));
  assert.throws(() => checkSnapshot(temporary), /Frozen provenance changed/);
});

test('source layout mapping cannot be changed in provenance', (t) => {
  const temporary = fixture(t);
  const path = join(temporary, 'provenance.json');
  const provenance = JSON.parse(readFileSync(path));
  provenance.inventory[0].upstreamPath = 'dist/not-cosmos.json';
  writeFileSync(path, JSON.stringify(provenance));
  assert.throws(() => readInventory(temporary), /Source layout mismatch/);
});

const semanticCases = [
  ['global count', 'rules.json', (d) => { d.ruleCount = 79; }, /Global rule count/],
  ['duplicate rule ID', 'rules.json', (d) => { d.sections[0].rules[1].id = d.sections[0].rules[0].id; }, /duplicate IDs/],
  ['stable ID gap', 'rules.json', (d) => { d.sections[0].rules[0].id = 'C28'; }, /Stable rule ID set/],
  ['page count', 'specification.json', (d) => { d.pages[0].ruleCount--; }, /page count mismatch/],
  ['index section membership', 'specification.json', (d) => { d.pages[0].sections[0].ruleIds.pop(); }, /rule membership mismatch/],
  ['manifest foundation ID membership', 'manifest.json', (d) => { d.foundation.ruleIds.pop(); }, /manifest rule IDs mismatch/],
  ['common inheritance', 'manifest.json', (d) => { d.platforms[0].inherits = []; }, /common inheritance mismatch/],
  ['scoped union', 'mobile-app/rules.json', (d) => { d.sections[0].rules[0].acceptance = 'invented'; }, /exact common \+ platform union mismatch/],
  ['scoped count', 'web-page/rules.json', (d) => { d.ruleCount--; }, /scoped rule count mismatch/],
  ['scoped token document', 'mobile-app/tokens.json', (d) => { d.$extensions['app.simt.cosmos'].document.version = '2.4.0'; }, /token document metadata mismatch/],
  ['scoped token adapter', 'mobile-app/tokens.json', (d) => { d.$extensions['app.simt.cosmos'].part.adapter = 'ios'; }, /token part metadata mismatch/],
  ['scoped token profiles', 'watch-app/tokens.json', (d) => { d.$extensions['app.simt.cosmos'].scope.profiles.push('daylight'); }, /token scope mismatch/],
  ['token subset parity', 'mobile-app/tokens.json', (d) => { d.space['1'] = {}; }, /common space parity mismatch/],
  ['profile subset parity', 'web-page/tokens.json', (d) => { d.adapters.daylight.text.$value.alpha = 0.2; }, /profile parity mismatch/],
  ['lightweight motion ref', 'tokens.json', (d) => { d.motionMotifs.$ref = 'https://unregistered.invalid/motion.json'; }, /Offline reference mismatch/],
  ['reference guidance', 'tokens.json', (d) => { d.components.fetchWhen = []; }, /fetch guidance mismatch/],
  ['motion curve sample parity', 'motion.json', (d) => { d.parityFixture.curves.android.enter.samples[3] = 0.5; }, /recomputed parity samples mismatch/],
  ['motion duration parity', 'motion.json', (d) => { d.parityFixture.durations.feedback = 151; }, /parity duration mismatch/],
  ['motion timing link', 'motion.json', (d) => { d.parityFixture.timing[0].duration = 'feedback'; }, /timing duration mismatch/],
  ['motion curve/token link', 'motion.json', (d) => { d.parityFixture.curves.android.enter.curve[0] = 0.07; }, /curve\/token mismatch/],
  ['contrast reference', 'contrast-pairs.json', (d) => { d.contrastPairs.deepSpace['ink-on-void'].$extensions['app.simt.cosmos'].foregroundToken = 'color.missing'; }, /Unresolved contrast reference/],
  ['contrast count', 'contrast-pairs.json', (d) => { delete d.contrastPairs.deepSpace['ink-on-void']; }, /Contrast pair count/],
  ['contrast guidance', 'contrast-pairs.json', (d) => { d.contrastPairs.deepSpace['ink-on-void'].$extensions['app.simt.cosmos'].aaNormalText = false; }, /contrast eligibility mismatch/],
  ['component rule link', 'components.json', (d) => { d.components[0].rules.push('C28'); }, /unresolved component rule/],
  ['component motion link', 'components.json', (d) => { d.components[0].motion = 'unpublished'; }, /unresolved component motion/],
];
for (const [label, name, change, expected] of semanticCases) {
  test(`rehashing cannot hide ${label} damage from semantic validation`, (t) => {
    const snapshot = mutate(fixture(t), name, change);
    assert.throws(() => validateContent(snapshot.documents), expected);
  });
}

test('unknown declared schema is rejected offline, never fetched', (t) => {
  const snapshot = mutate(fixture(t), 'rules.json', (d) => { d.$schema = 'https://unregistered.invalid/schema.json'; });
  t.mock.method(globalThis, 'fetch', () => { assert.fail('No network allowed'); });
  assert.throws(() => canonicalSchemaErrors(snapshot.documents), /Offline: unregistered declared schema/);
});

test('missing local schema $ref is rejected synchronously, never fetched', (t) => {
  const snapshot = mutate(fixture(t), 'rules.schema.json', (d) => { d.properties.version = { $ref: 'https://unregistered.invalid/schema.json' }; });
  t.mock.method(globalThis, 'fetch', () => { assert.fail('No network allowed'); });
  assert.throws(() => canonicalSchemaErrors(snapshot.documents), /can't resolve reference https:\/\/unregistered.invalid\/schema.json/);
});

test('legal strictRequired lint exception does not relax instance required validation', (t) => {
  const snapshot = mutate(fixture(t), 'specification.json', (d) => { delete d.implementationStatus; });
  const failures = canonicalSchemaErrors(snapshot.documents);
  assert(failures.some((error) => error.keyword === 'required' && error.params.missingProperty === 'implementationStatus'));
  assert.throws(() => verifyKnownDefects(snapshot, failures), /ONLY the four exact known/);
});

test('extra global schema defect is not accepted by private integrity', (t) => {
  const snapshot = mutate(fixture(t), 'rules.json', (d) => { d.version = 'invalid'; });
  const failures = canonicalSchemaErrors(snapshot.documents);
  assert(failures.some((error) => error.file === `${SNAPSHOT}/rules.json` && error.keyword === 'pattern'));
  assert.throws(() => verifyKnownDefects(snapshot, failures), /ONLY the four exact known/);
});

test('known-defect gate rejects weakening or repairing canonical bytes', (t) => {
  const snapshot = mutate(fixture(t), 'rules.schema.json', (d) => { d.properties.sections.minItems = 16; });
  assert.deepEqual(canonicalSchemaErrors(snapshot.documents), []);
  assert.throws(() => verifyKnownDefects(snapshot, []), /ONLY the four exact known/);
});

test('strict canonical gate reports exactly all four upstream scoped minItems errors', () => {
  const snapshot = readInventory(root);
  const failures = canonicalSchemaErrors(snapshot.documents);
  assert.equal(failures.length, 4);
  for (const part of PARTS) {
    const error = failures.find((entry) => entry.file === `${SNAPSHOT}/${part}/rules.json`);
    assert(error);
    assert.equal(error.keyword, 'minItems');
    assert.equal(error.instancePath, '/sections');
    assert.equal(error.schemaPath, '#/properties/sections/minItems');
    assert.deepEqual(error.params, { limit: 36 });
    assert.equal(error.actualSections, part === 'mobile-app' || part === 'web-app' ? 17 : 16);
  }
  verifyKnownDefects(snapshot, failures);
  const command = spawnSync(process.execPath, [join(root, 'scripts/check.js'), 'canonical-schema'], { encoding: 'utf8' });
  assert.equal(command.status, 1);
  assert.match(command.stderr, /STRICT CANONICAL-SCHEMA FAILED/);
  for (const part of PARTS) assert(command.stderr.includes(`${SNAPSHOT}/${part}/rules.json /sections:`));
});

test('Bezier recomputation uses supplied sample count/steps and exact endpoints', () => {
  assert.deepEqual(curveSamples([0, 0, 1, 1], 3, 64), [0, 0.5, 1]);
  assert.equal(curveSamples([0.2, 0, 0.2, 1], 21, 64)[10], 0.814074);
  assert.throws(() => curveSamples([0.2, 0, 0.2, 1], 21, 1), /Invalid bisection steps/);
});

test('actual archive hashes repeat, with sorted reviewed paths and fixed metadata', (t) => {
  const temporary = fixture(t);
  const first = createArchive(temporary);
  const firstBytes = readFileSync(first.archive);
  const second = createArchive(temporary);
  assert.equal(first.entries, 31);
  assert.equal(first.sha256, sha256(firstBytes));
  assert.equal(second.sha256, first.sha256);
  assert.deepEqual(readFileSync(second.archive), firstBytes);
  const inspection = spawnSync('python3', ['-c', 'import json,sys,zipfile; z=zipfile.ZipFile(sys.argv[1]); print(json.dumps([{"name": i.filename, "time": i.date_time, "mode": i.external_attr >> 16, "stored": i.compress_type == 0} for i in z.infolist()]))', first.archive], { encoding: 'utf8' });
  assert.equal(inspection.status, 0, inspection.stderr);
  const entries = JSON.parse(inspection.stdout);
  assert.deepEqual(entries.map((entry) => entry.name), [...REVIEWED_PATHS, 'provenance.json'].sort());
  for (const entry of entries) {
    assert.deepEqual(entry.time, [1980, 1, 1, 0, 0, 0]);
    assert.equal(entry.mode, 0o100644);
    assert.equal(entry.stored, true);
    assert(!/(^|\/)(\.git|build|node_modules|config)(\/|$)/.test(entry.name));
  }
  t.diagnostic(`Actual repeated private archive SHA256: ${first.sha256}`);
});

test('archive refuses a damaged snapshot rather than packaging unchecked bytes', (t) => {
  const temporary = fixture(t);
  writeFileSync(join(temporary, SNAPSHOT, 'common.md'), 'tampered');
  assert.throws(() => createArchive(temporary), /SHA256 mismatch/);
});
