import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalSchemaErrors, checkSnapshot, PROVENANCE_SHA256, readInventory, verifyPin } from './snapshot.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));

export function createArchive(snapshotRoot = root) {
  const snapshot = checkSnapshot(snapshotRoot);
  const entries = [...snapshot.provenance.inventory.map(({ path, sha256 }) => ({ path, sha256 })), { path: 'provenance.json', sha256: PROVENANCE_SHA256 }].sort((a, b) => a.path.localeCompare(b.path));
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
    const { counts, failures } = checkSnapshot(root);
    console.log(`Private integrity passed: ${JSON.stringify(counts)}`);
    console.log(`Known upstream canonical-schema defects: ${failures.length} scoped /sections minItems failures (16/17 < 36). NOT full conformance; public redistribution BLOCKED; no normative iOS adapter.`);
  } else if (mode === 'canonical-schema') {
    const snapshot = readInventory(root);
    verifyPin(snapshot);
    const failures = canonicalSchemaErrors(snapshot.documents);
    if (failures.length) {
      console.error('STRICT CANONICAL-SCHEMA FAILED (upstream bytes preserved; no schema weakened):');
      for (const error of failures) console.error(`${error.file} ${error.instancePath}: ${error.message}; actual=${error.actualSections}, keyword=${error.keyword}, schema=${error.schemaPath}`);
      process.exitCode = 1;
    } else console.log('Every declared canonical schema passed (public redistribution still blocked by unconfirmed licenses).');
  } else if (mode === 'archive') {
    console.log(JSON.stringify(createArchive()));
    console.log('Private-development archive only. Public redistribution BLOCKED. License notice is in provenance.json.');
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
