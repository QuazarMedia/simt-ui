import assert from 'node:assert/strict';
import test from 'node:test';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import { generatedOutputs } from './build.js';
import { products } from '../src/site/product.js';

const outputs = generatedOutputs();
const document = (name) => JSON.parse(outputs.get(`${name}.json`));
const ajv = new Ajv2020({ allErrors: true, strict: true, strictRequired: false });
addFormats(ajv);
for (const p of products) {
  const prefix = p.basePath ? `${p.basePath.slice(1)}/` : '';
  for (const name of ['rules', 'specification']) ajv.addSchema(document(`${prefix}${name}.schema`));
}
const validate = ajv.getSchema(document('rules.schema').$id);
const counts = { 'mobile-app': 17, 'web-page': 16, 'web-app': 17, 'watch-app': 16 };
const scopes = [['rules', 36], ...Object.entries(counts).map(([part, count]) => [`${part}/rules`, count])];

test('every generated declared Cosmos schema validates locally', (t) => {
  t.mock.method(globalThis, 'fetch', () => assert.fail('Schema validation must remain offline'));
  for (const [path, bytes] of outputs) {
    if (!path.endsWith('.json') || path.endsWith('.schema.json') || path.startsWith('.well-known/')) continue;
    const value = JSON.parse(bytes);
    if (!value.$schema) continue;
    const validator = ajv.getSchema(value.$schema);
    assert(validator, path);
    assert(validator(value), `${path}: ${JSON.stringify(validator.errors)}`);
  }
});

for (const [name, count] of scopes) {
  test(`${name} retains its exact ${count}-section contract`, () => {
    const value = document(name);
    assert.equal(value.sections.length, count);
    assert(validate(value), JSON.stringify(validate.errors));
    for (const mutate of [
      (d) => d.sections.pop(),
      (d) => d.sections.push(structuredClone(d.sections[0])),
      (d) => {
        d.sections[1] = structuredClone(d.sections[0]);
      },
      (d) => {
        d.sections[0].id = 'invented-section';
      },
      (d) => {
        d.sections[0].platform = 'unknown-platform';
      },
      (d) => {
        delete d.sections[0].rules[0].acceptance;
      },
      (d) => {
        d.version = 'invalid';
      },
    ]) {
      const invalid = structuredClone(value);
      mutate(invalid);
      assert.equal(validate(invalid), false, `${name}: ${mutate}`);
    }
  });
}

for (const part of Object.keys(counts)) {
  test(`${part} rejects foreign sections and scope substitution`, () => {
    const value = document(`${part}/rules`);
    const global = document('rules');
    const foreign = global.sections.find((section) => !['common', part].includes(section.platform));
    for (const mutate of [
      (d) => {
        d.sections[d.sections.length - 1] = structuredClone(foreign);
      },
      (d) => {
        d.sections = structuredClone(global.sections);
      },
      (d) => {
        delete d.platform;
      },
      (d) => {
        d.platform.id = 'common';
      },
      (d) => {
        d.platform = {};
      },
      (d) => {
        d.platform = null;
      },
    ]) {
      const invalid = structuredClone(value);
      mutate(invalid);
      assert.equal(validate(invalid), false, `${part}: ${mutate}`);
    }
  });
}

test('a union cannot opt into a scoped branch', () => {
  const value = document('rules');
  value.platform = { id: 'mobile-app', label: 'Mobile app' };
  assert.equal(validate(value), false);
});
