import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import product from '../src/site/product.js';

const readNotice = () => readFileSync(new URL('../src/design/NOTICE', import.meta.url));

test('Design retains complete Apache-2.0 and CC BY 4.0 license texts', () => {
  const { license, licenseDocs } = product.spec.licenseFiles;
  for (const [name, path, hash] of [
    ['LICENSE', license, 'c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4'],
    ['LICENSE-DOCS', licenseDocs, '9ba9550ad48438d0836ddab3da480b3b69ffa0aac7b7878b5a0039e7ab429411'],
  ]) {
    assert.equal(createHash('sha256').update(readFileSync(path)).digest('hex'), hash, name);
  }
});

test('Design notice defines owner, code/document scope and third-party exclusions', () => {
  const notice = readNotice().toString('utf8');
  for (const text of [
    'Copyright Said Elimam',
    '2026-11-17',
    'scripts/build.js',
    'Apache License, Version 2.0',
    'Creative Commons Attribution 4.0 International',
    'https://design.simt.app',
    'https://cosmos.simt.app',
    'identify any modifications',
    'does not license unrelated website/product assets',
    'No\ntrademark rights are granted',
  ])
    assert(notice.includes(text), text);
});
