import assert from 'node:assert/strict';
import test from 'node:test';
import {
  MAJOR_VERSION_NAMES,
  latestVersion,
  listVersions,
  loadSpec,
  majorVersion,
  versionName,
} from '../registry.js';

test('major version names are explicit and generic', () => {
  assert(Object.isFrozen(MAJOR_VERSION_NAMES));
  assert.deepEqual(MAJOR_VERSION_NAMES, { 2: 'Cosmos' });
  assert.equal(majorVersion('2.4.0'), 2);
  assert.equal(versionName('2.4.0'), 'Cosmos');
  assert.equal(versionName('2.3.0'), 'Cosmos');
  assert.equal(versionName('2.99.12'), 'Cosmos');
  assert.throws(() => versionName('3.0.0'), /no name for major version 3.*MAJOR_VERSION_NAMES/);
  assert.throws(() => versionName('2.4'), /Invalid Design specification version/);
});

test('every discovered version resolves a name and loaded release identity', async () => {
  const versions = listVersions();
  assert.deepEqual(versions, ['2.4.0', '2.3.0']);
  assert.equal(latestVersion(), '2.4.0');
  for (const version of versions) {
    assert.equal(majorVersion(version), 2);
    assert.equal(versionName(version), 'Cosmos');
    const spec = await loadSpec(version);
    assert.equal(spec.majorVersion, 2);
    assert.equal(spec.versionName, 'Cosmos');
    assert.equal(spec.release.majorVersion, 2);
    assert.equal(spec.release.versionName, 'Cosmos');
    assert.equal(spec.release.base, 'https://design.simt.app');
  }
});
