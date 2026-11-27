import assert from 'node:assert/strict';

const entries = (object) => Object.entries(object).filter(([key]) => !key.startsWith('$'));
const keys = (object) => entries(object).map(([key]) => key).sort();
const ids = (sections) => sections.flatMap((section) => section.rules.map((rule) => rule.id));
const ext = (object) => object.$extensions['app.simt.cosmos'];
const sameSet = (actual, expected, message) => assert.deepEqual([...actual].sort(), [...expected].sort(), message);
function unique(values, label) {
  assert.equal(new Set(values).size, values.length, `${label}: duplicate IDs`);
}
function resolve(object, reference) {
  return reference.split('.').reduce((value, key) => value?.[key], object);
}

export function curveSamples(curve, count, steps) {
  assert.equal(curve.length, 4, 'Cubic Bezier requires four coordinates');
  assert(curve.every((number) => Number.isFinite(number) && number >= 0 && number <= 1), 'Invalid Bezier coordinates');
  assert(Number.isInteger(count) && count >= 3 && count <= 1000, 'Invalid sample count');
  assert(Number.isInteger(steps) && steps >= 24 && steps <= 128, 'Invalid bisection steps');
  const polynomial = (t, a, b) => 3 * (1 - t) ** 2 * t * a + 3 * (1 - t) * t ** 2 * b + t ** 3;
  return Array.from({ length: count }, (_, index) => {
    if (index === 0) return 0;
    if (index === count - 1) return 1;
    const x = index / (count - 1);
    let low = 0;
    let high = 1;
    for (let step = 0; step < steps; step++) {
      const midpoint = (low + high) / 2;
      if (polynomial(midpoint, curve[0], curve[2]) < x) low = midpoint;
      else high = midpoint;
    }
    return Number(polynomial((low + high) / 2, curve[1], curve[3]).toFixed(6));
  });
}

function luminance(hex) {
  assert(/^#[0-9A-Fa-f]{6}$/.test(hex), `Invalid contrast hex: ${hex}`);
  const rgb = [1, 3, 5].map((start) => {
    const value = Number.parseInt(hex.slice(start, start + 2), 16) / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
}

export function validateSemantics(documents, { version, updated, base, parts }) {
  const index = documents['specification.json'];
  const rules = documents['rules.json'];
  const manifest = documents['manifest.json'];
  const tokens = documents['tokens.json'];
  const motion = documents['motion.json'];
  const components = documents['components.json'];
  const contrast = documents['contrast-pairs.json'];
  for (const [name, document] of Object.entries(documents)) {
    if (name.endsWith('.schema.json') || name.endsWith('tokens.json')) continue;
    assert.equal(document.version, version, `${name}: version mismatch`);
    assert.equal(document.updated, updated, `${name}: date mismatch`);
    assert.equal(document.status, 'Normative specification', `${name}: status mismatch`);
  }
  assert.equal(ext(tokens).version, version, 'Token version mismatch');
  assert.equal(index.base, base);
  assert.equal(manifest.canonical, base);
  assert.equal(index.inheritance, manifest.authority, 'Common authority mismatch');
  assert.equal(rules.inheritance, index.inheritance, 'Global inheritance mismatch');
  assert.deepEqual(rules.normativeLanguage, index.normativeLanguage);
  assert.deepEqual(index.resources, manifest.resources, 'Resource index mismatch');
  for (const document of [index, rules, manifest]) assert.equal(document.ruleCount, 80, 'Global rule count must be 80');
  assert.equal(rules.sections.length, 36, 'Global section count must be 36');
  const allIds = ids(rules.sections);
  unique(allIds, 'Global rules');
  const numbered = (prefix, count) => Array.from({ length: count }, (_, i) => `${prefix}${String(i + 1).padStart(2, '0')}`);
  // C28 is not a published stable ID in this version; preserve the deliberate gap.
  sameSet(allIds, [...numbered('C', 27), 'C29', 'C30', 'C31', ...numbered('P', 11), ...numbered('A', 13), ...numbered('M', 14), ...numbered('W', 12)], 'Stable rule ID set mismatch');
  unique(index.pages.map((page) => page.slug), 'Index pages');
  sameSet(index.pages.map((page) => page.slug), ['common', ...parts], 'Index page membership mismatch');
  unique(rules.sections.map((section) => `${section.platform}/${section.id}`), 'Global sections');
  const indexedIds = [];
  for (const page of index.pages) {
    const sections = rules.sections.filter((section) => section.platform === page.slug);
    sameSet(page.sections.map((section) => section.id), sections.map((section) => section.id), `${page.slug}: section membership mismatch`);
    assert.equal(page.ruleCount, ids(sections).length, `${page.slug}: page count mismatch`);
    const pageManifest = page.slug === 'common' ? manifest.foundation : manifest.platforms.find((entry) => entry.id === page.slug);
    assert(pageManifest, `${page.slug}: missing manifest part`);
    assert.equal(pageManifest.url, page.url);
    assert.equal(pageManifest.markdown, page.markdown);
    assert.equal(page.url, `${base}/${page.slug}`);
    assert.deepEqual(pageManifest.sections, page.sections.map(({ id, url }) => ({ id, url })), `${page.slug}: manifest section mismatch`);
    sameSet(pageManifest.ruleIds, ids(sections), `${page.slug}: manifest rule IDs mismatch`);
    for (const section of page.sections) {
      const ruleSection = sections.find((candidate) => candidate.id === section.id);
      unique(section.ruleIds, `${page.slug}/${section.id}`);
      assert.equal(section.url, ruleSection.url, 'Section URL mismatch');
      assert.equal(section.title, ruleSection.title, 'Section title mismatch');
      assert.deepEqual(section.ruleIds, ruleSection.rules.map((rule) => rule.id), `${page.slug}/${section.id}: rule membership mismatch`);
      indexedIds.push(...section.ruleIds);
      for (const rule of ruleSection.rules) assert.equal(rule.url, `${page.url}#${rule.id.toLowerCase()}`, 'Rule URL mismatch');
    }
  }
  unique(indexedIds, 'Indexed rules');
  sameSet(indexedIds, allIds, 'Index/global rule membership mismatch');
  assert.equal(manifest.foundation.id, 'common');
  assert.equal(manifest.common, `${base}/common`);
  unique(manifest.platforms.map((platform) => platform.id), 'Manifest platforms');
  sameSet(manifest.platforms.map((platform) => platform.id), parts, 'Manifest platform IDs mismatch');
  const scopes = {
    'mobile-app': { adapter: 'android', profiles: ['deepSpace', 'daylight', 'astronomy'], stars: true, gradients: ['field', 'nebula', 'corona', 'accretion', 'horizon', 'rim', 'action'], colorSpace: 'srgb' },
    'web-page': { adapter: 'webPage', profiles: ['deepSpace', 'daylight'], stars: true, gradients: ['field', 'nebula', 'corona', 'accretion', 'horizon', 'rim', 'action', 'spectrum'], colorSpace: 'oklch' },
    'web-app': { adapter: 'webApp', profiles: ['deepSpace', 'daylight'], stars: true, gradients: ['field', 'corona', 'horizon', 'rim', 'action'], colorSpace: 'oklch' },
    'watch-app': { adapter: 'watch', profiles: ['deepSpace', 'watchAmbient'], stars: false, gradients: ['corona', 'rim', 'action'], colorSpace: 'srgb' },
  };
  sameSet(keys(tokens.platformAdapters), Object.values(scopes).map((scope) => scope.adapter), 'Canonical adapters mismatch (no normative iOS adapter)');
  sameSet(keys(tokens.adapters), ['deepSpace', 'daylight', 'astronomy', 'watchAmbient'], 'Canonical profile set mismatch');
  assert.deepEqual(ext(tokens).profiles, Object.keys(tokens.adapters), 'Token profile metadata mismatch');
  for (const part of parts) {
    const scopedRules = documents[`${part}/rules.json`];
    const scopedTokens = documents[`${part}/tokens.json`];
    const platform = manifest.platforms.find((entry) => entry.id === part);
    const scope = scopes[part];
    assert.equal(platform.adapter, scope.adapter, `${part}: adapter mismatch`);
    assert.deepEqual(platform.inherits, [manifest.common], `${part}: common inheritance mismatch`);
    for (const name of ['tokens', 'rules']) assert.equal(platform[name], `${base}/${part}/${name}.json`);
    assert.deepEqual(scopedRules.platform, { id: part, label: platform.label });
    assert.equal(scopedRules.inheritance, rules.inheritance, `${part}: scoped inheritance mismatch`);
    assert.deepEqual(scopedRules.normativeLanguage, rules.normativeLanguage);
    const expected = rules.sections.filter((section) => section.platform === 'common' || section.platform === part);
    assert.deepEqual(scopedRules.sections, expected, `${part}: exact common + platform union mismatch`);
    assert.equal(scopedRules.ruleCount, ids(expected).length, `${part}: scoped rule count mismatch`);
    unique(ids(scopedRules.sections), `${part} rules`);
    const metadata = ext(scopedTokens);
    assert.deepEqual(metadata.document, { version, updated, status: rules.status }, `${part}: token document metadata mismatch`);
    assert.deepEqual(metadata.part, { id: part, label: platform.label, adapter: scope.adapter }, `${part}: token part metadata mismatch`);
    assert.deepEqual(metadata.scope, { profiles: scope.profiles, stars: scope.stars, gradients: scope.gradients, colorSpace: scope.colorSpace, starfield: scope.stars }, `${part}: token scope mismatch`);
    assert.deepEqual(keys(scopedTokens.platformAdapters), [scope.adapter], `${part}: foreign adapter leaked`);
    assert.deepEqual(scopedTokens.platformAdapters[scope.adapter], tokens.platformAdapters[scope.adapter], `${part}: adapter subset mismatch`);
    assert.deepEqual(keys(scopedTokens.adapters), scope.profiles.toSorted(), `${part}: profile subset mismatch`);
    for (const profile of scope.profiles) assert.deepEqual(scopedTokens.adapters[profile], tokens.adapters[profile], `${part}: profile parity mismatch`);
    const gradients = entries(tokens.gradient).filter(([name, value]) => scope.gradients.includes(name) && ext(value).profiles.some((profile) => scope.profiles.includes(profile)));
    assert.deepEqual(scopedTokens.gradient, Object.fromEntries(gradients), `${part}: gradient subset mismatch`);
    for (const key of ['color', 'shadow', 'space', 'radius', 'motion', 'font', 'layer']) assert.deepEqual(scopedTokens[key], tokens[key], `${part}: common ${key} parity mismatch`);
    assert.equal('starfield' in scopedTokens, scope.stars, `${part}: starfield scope mismatch`);
    if (scope.stars) assert.deepEqual(scopedTokens.starfield, tokens.starfield);
    assert.equal('projection' in scopedTokens, scope.colorSpace === 'oklch', `${part}: projection scope mismatch`);
    if (scope.colorSpace === 'oklch') {
      assert.deepEqual(scopedTokens.projection.anchors, tokens.projection.anchors);
      assert.deepEqual(scopedTokens.projection.roles, Object.fromEntries(scope.profiles.map((profile) => [profile, tokens.projection.roles[profile]])), `${part}: projection subset mismatch`);
    }
  }
  for (const document of [tokens, ...parts.map((part) => documents[`${part}/tokens.json`])]) {
    for (const [pointer, name] of [['motionMotifs', 'motion'], ['components', 'components'], ['contrastPairs', 'contrast-pairs']]) {
      assert.deepEqual(Object.keys(document[pointer]).sort(), ['$description', '$ref', 'fetchWhen'], `Lightweight reference expected: ${pointer}`);
      assert.equal(document[pointer].$ref, `${base}/${name}.json`, `Offline reference mismatch: ${pointer}`);
      assert.deepEqual(document[pointer].fetchWhen, documents[`${name}.json`].fetchWhen, `${pointer}: fetch guidance mismatch`);
      assert(document[pointer].fetchWhen.length > 0);
    }
  }
  const fixture = motion.parityFixture;
  unique(motion.motifs.map((motif) => motif.id), 'Motion motifs');
  assert.deepEqual(fixture.timing.map((row) => row.id), motion.motifs.map((motif) => motif.id), 'Motion timing membership mismatch');
  sameSet(Object.keys(fixture.curves), keys(tokens.platformAdapters), 'Parity adapter membership mismatch');
  for (const [part, curves] of Object.entries(fixture.curves)) {
    const adapter = tokens.platformAdapters[part];
    sameSet(Object.keys(curves), keys(adapter.easing), `${part}: parity curve membership mismatch`);
    for (const [key, value] of Object.entries(curves)) {
      assert.deepEqual(value.curve, adapter.easing[key].$value, `${part}/${key}: curve/token mismatch`);
      assert.deepEqual(value.samples, curveSamples(value.curve, fixture.sampleCount, fixture.bisectionSteps), `${part}/${key}: recomputed parity samples mismatch`);
      assert(value.samples.every((value, i, samples) => i === 0 || value >= samples[i - 1]), 'Nonmonotonic parity samples');
    }
    for (const [key, duration] of Object.entries(fixture.durations)) {
      assert.deepEqual(adapter.duration[key], tokens.motion[key], `${part}/${key}: adapter duration mismatch`);
      assert.deepEqual(motion.durations[key], tokens.motion[key], `${key}: motion duration mismatch`);
      assert.equal(duration, tokens.motion[key].$value.value, `${key}: parity duration mismatch`);
      assert.equal(tokens.motion[key].$value.unit, 'ms');
    }
  }
  sameSet(Object.keys(fixture.durations), keys(motion.durations), 'Duration membership mismatch');
  for (const row of fixture.timing) {
    const motif = motion.motifs.find((entry) => entry.id === row.id);
    assert(motion.durations[motif.duration], `${motif.id}: unknown duration`);
    for (const key of ['duration', 'properties', 'settlesTo']) assert.deepEqual(row[key], motif[key], `${row.id}: timing ${key} mismatch`);
    assert(motif.properties.every((property) => motion.properties.includes(property)), 'Unknown animated property');
    sameSet(Object.keys(motif.tiers), motion.tiers, 'Motion tier membership mismatch');
    for (const purpose of motif.easing) assert(motion.easingPurposes[purpose], 'Unknown easing purpose');
    for (const [toolkit, part] of [['web', 'webPage'], ['native', 'android']]) {
      assert(row.easing[toolkit].length > 0, 'Timing must name an easing curve');
      for (const key of row.easing[toolkit]) assert(fixture.curves[part][key], `${row.id}: unresolved timing curve`);
      for (const purpose of motif.easing) {
        const curve = motion.easingPurposes[purpose][toolkit]?.$value;
        if (curve) assert(row.easing[toolkit].some((key) => JSON.stringify(fixture.curves[part][key].curve) === JSON.stringify(curve)), `${row.id}: easing purpose parity mismatch`);
      }
    }
  }
  assert.equal(contrast.ratioField, '$value');
  assert.equal(contrast.guidanceField, '$extensions.app.simt.cosmos');
  sameSet(contrast.profiles, keys(tokens.adapters).filter((profile) => profile !== 'deepSpace'), 'Contrast profile metadata mismatch');
  sameSet(keys(contrast.contrastPairs), keys(tokens.adapters), 'Contrast profile membership mismatch');
  let pairCount = 0;
  for (const [profile, pairs] of entries(contrast.contrastPairs)) {
    for (const [name, pair] of entries(pairs)) {
      pairCount++;
      const info = ext(pair);
      const colors = [info.foregroundToken, info.backgroundToken].map((reference) => {
        assert(reference.startsWith('color.') || reference.startsWith(`adapters.${profile}.`), `${profile}/${name}: foreign contrast reference`);
        const token = resolve(tokens, reference);
        assert(token?.$type === 'color', `Unresolved contrast reference: ${reference}`);
        return luminance(ext(token).hex);
      });
      const ratio = (Math.max(...colors) + 0.05) / (Math.min(...colors) + 0.05);
      assert.equal(pair.$type, 'number');
      assert(Math.abs(pair[contrast.ratioField] - ratio) < 0.000001, `${profile}/${name}: contrast ratio mismatch`);
      assert.equal(info.aaNormalText, ratio >= 4.5, `${profile}/${name}: contrast eligibility mismatch`);
      assert(!info.recommendedTextUse || (info.aaNormalText && typeof info.recommendedTextUse === 'string'), `${profile}/${name}: unsafe contrast guidance`);
    }
  }
  assert.equal(pairCount, 212, 'Contrast pair count must be 212');
  assert(!ext(contrast.contrastPairs.deepSpace['dim-on-void']).recommendedTextUse, 'Dim is not essential text');
  unique(components.components.map((component) => component.id), 'Components');
  assert.equal(components.components.length, 17, 'Component count mismatch');
  assert.equal(components.referencesOnly, true);
  sameSet(Object.keys(components.parts), keys(tokens.platformAdapters), 'Component part membership mismatch');
  const knownRules = new Set(allIds);
  for (const state of Object.values(components.states)) for (const id of state.rules) assert(knownRules.has(id), `Unknown state rule: ${id}`);
  for (const component of components.components) {
    for (const id of [...component.rules, ...component.a11y]) assert(knownRules.has(id), `${component.id}: unresolved component rule ${id}`);
    for (const role of component.roles) assert(components.roles.theme.includes(role), `${component.id}: unknown theme role`);
    assert(component.radius === null || components.roles.radius.includes(component.radius), `${component.id}: unknown radius`);
    for (const layer of component.layers ?? []) assert(components.roles.layer.includes(layer), 'Unknown component layer');
    for (const state of component.states) assert(components.states[state], 'Unknown component state');
    for (const [state, reason] of Object.entries(component.notApplicable)) assert(components.states[state] && components.notApplicable[reason], 'Unknown state exemption');
    sameSet([...component.states, ...Object.keys(component.notApplicable)], Object.keys(components.states), `${component.id}: state coverage mismatch`);
    for (const part of component.parts) assert(components.parts[part], 'Unknown component part');
    if (component.motion === null) assert.equal(component.timing, null);
    else {
      const motif = motion.motifs.find((entry) => entry.id === component.motion);
      assert(motif, `${component.id}: unresolved component motion`);
      assert.deepEqual(component.timing, { motif: motif.id, duration: motif.duration, easing: motif.easing }, `${component.id}: component timing mismatch`);
    }
  }
  for (const [part, slice] of Object.entries(components.parts)) {
    assert.deepEqual(slice.components, components.components.filter((component) => component.parts.includes(part)).map((component) => component.id), `${part}: component slice mismatch`);
  }
  return { exports: 23, sources: 7, rules: allIds.length, sections: rules.sections.length, profiles: keys(tokens.adapters).length, contrastPairs: pairCount, motifs: motion.motifs.length, curves: Object.values(fixture.curves).reduce((count, curves) => count + Object.keys(curves).length, 0), components: components.components.length };
}
