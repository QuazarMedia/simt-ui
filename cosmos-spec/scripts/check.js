import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalSchemaErrors, checkSnapshot, PROVENANCE_SHA256, readInventory, verifyPin } from './snapshot.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));

export function createArchive(snapshotRoot = root) {
  const snapshot = checkSnapshot(snapshotRoot);
  const entries = [...snapshot.provenance.inventory.map(({ path, sha256 }) => ({ path, sha256 })), { path: 'provenance.json', sha256: PROVENANCE_SHA256 }].sort((a, b) => a.path < b.path ? -1 : a.path > b.path ? 1 : 0);
  const result = spawnSync('python3', [join(root, 'scripts/archive.py')], {
    input: JSON.stringify({ root: snapshotRoot, entries }),
    encoding: 'utf8',
  });
  if (result.error) throw new Error('python3 (standard library only) is required for the local private archive', { cause: result.error });
  if (result.status !== 0) throw new Error(result.stderr || 'Private archive failed');
  return JSON.parse(result.stdout);
}

function main() {
  const mode = process.argv[2];
  if (mode === 'integrity') {
    const { counts } = checkSnapshot(root);
    console.log(`Pinned integrity and canonical schemas passed: ${JSON.stringify(counts)}`);
    console.log('Apache-2.0 code / CC BY 4.0 documents; licenses and attribution retained. No release/upload approval or normative iOS adapter is implied.');
  } else if (mode === 'canonical-schema') {
    const snapshot = readInventory(root);
    verifyPin(snapshot);
    const failures = canonicalSchemaErrors(snapshot.documents);
    if (failures.length) {
      console.error('STRICT CANONICAL-SCHEMA FAILED (upstream bytes preserved; no schema weakened):');
      for (const error of failures) console.error(`${error.file} ${error.instancePath}: ${error.message}; actual=${error.actualSections}, keyword=${error.keyword}, schema=${error.schemaPath}`);
      process.exitCode = 1;
    } else console.log('Every declared canonical schema passed. License permission is separate from release/upload approval.');
  } else if (mode === 'archive') {
    console.log(JSON.stringify(createArchive()));
    console.log('Local archive with pinned licenses/attribution. License permission does not authorize publishing, signing or uploading a release.');
  } else throw new Error('Use integrity, canonical-schema, or archive; dependency bootstrap is explicit: npm ci --ignore-scripts --no-audit --no-fund');
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  try {
    main();
  } catch (error) {
    console.error(`Cosmos ${process.argv[2]} failed: ${error.message}`);
    process.exitCode = 1;
  }
}
