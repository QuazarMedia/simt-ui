import { anchor, paintVariable } from './tokens.js';

// The reference sky. Native adapters port generateStars() and must reproduce
// the published fixture exactly: every step is 32-bit integer math or an IEEE
// double operation that Kotlin and JavaScript evaluate identically.
export const starfield = {
  band: 0.2,
  bucket: 160,
  attempts: 8,
  lowPowerFactor: 0.4,
  density: { screen: { areaPerStar: 7500, min: 24, max: 160 }, dialog: { areaPerStar: 10000, min: 12, max: 72 } },
  // Ordered by cumulative probability so one uniform draw selects a magnitude.
  magnitudes: [
    { name: 'bright', below: 0.05, radius: [1.6, 2.2], alpha: [0.85, 1] },
    { name: 'mid', below: 0.3, radius: [1, 1.6], alpha: [0.55, 0.85] },
    { name: 'faint', below: 1, radius: [0.7, 1], alpha: [0.3, 0.55] },
  ],
  maxSpikes: 3,
  inkShare: 0.7,
  maxAccentShare: 0.1,
  accents: ['gold', 'ember-soft', 'violet', 'cyan'],
  drift: { minPeriodS: 90, maxPeriodS: 160, maxTravel: 0.015 },
};

export const starfieldAlgorithm = [
  'Bucket: bw = ceil(width / bucket) * bucket and bh likewise, in CSS px or dp.',
  'Seed: FNV-1a 32-bit over the UTF-8 bytes of "<surfaceId>@<bw>x<bh>"; random numbers come from mulberry32(seed) as uint32 / 2^32.',
  'Count: round-half-up(bw * bh / areaPerStar) clamped to [min, max] for the surface kind.',
  'Per star, in this order: one draw selects the magnitude, one the radius, one the alpha, one the color; then up to `attempts` (u, v) pairs.',
  'Color: draw < inkShare is ink; otherwise accents[min(accents.length - 1, floor((draw - inkShare) / ((1 - inkShare) / accents.length)))], demoted to ink when that accent already holds floor(count * maxAccentShare) stars or all accents hold floor(count * (1 - inkShare)).',
  'Position: reject (u, v) when both lie strictly inside (band, 1 - band), or when the squared bucket-pixel distance to an accepted star is below (2 * max(r, rOther))^2. A star with no accepted attempt is skipped.',
  'Spikes: the first `maxSpikes` accepted bright stars carry a four-point diffraction cross 5 * r long.',
  'Render at x = u * width, y = v * height. Exclusion zones filter accepted stars afterwards and never reseed.',
];

function fnv1a(text) {
  let hash = 0x811c9dc5;
  for (const byte of new TextEncoder().encode(text)) {
    hash ^= byte;
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash;
}

function mulberry32(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function generateStars(surfaceId, width, height, kind = 'screen') {
  const { band, bucket, attempts, density, magnitudes, maxSpikes, inkShare, maxAccentShare, accents } = starfield;
  const bw = Math.ceil(width / bucket) * bucket;
  const bh = Math.ceil(height / bucket) * bucket;
  const { areaPerStar, min, max } = density[kind];
  const count = Math.min(max, Math.max(min, Math.floor((bw * bh) / areaPerStar + 0.5)));
  const random = mulberry32(fnv1a(`${surfaceId}@${bw}x${bh}`));
  const accentCap = Math.floor(count * maxAccentShare);
  const accentTotalCap = Math.floor(count * (1 - inkShare));
  const accentCounts = Object.fromEntries(accents.map((a) => [a, 0]));
  let accentTotal = 0;
  let spikes = 0;
  const stars = [];
  for (let i = 0; i < count; i++) {
    const magnitudeDraw = random();
    const magnitude = magnitudes.find((m) => magnitudeDraw < m.below);
    const r = magnitude.radius[0] + random() * (magnitude.radius[1] - magnitude.radius[0]);
    const alpha = magnitude.alpha[0] + random() * (magnitude.alpha[1] - magnitude.alpha[0]);
    const colorDraw = random();
    let color =
      colorDraw < inkShare
        ? 'ink'
        : accents[
            Math.min(accents.length - 1, Math.floor((colorDraw - inkShare) / ((1 - inkShare) / accents.length)))
          ];
    if (color !== 'ink' && (accentCounts[color] >= accentCap || accentTotal >= accentTotalCap)) color = 'ink';
    for (let attempt = 0; attempt < attempts; attempt++) {
      const u = random();
      const v = random();
      if (u > band && u < 1 - band && v > band && v < 1 - band) continue;
      const collides = stars.some((s) => {
        const dx = (s.u - u) * bw;
        const dy = (s.v - v) * bh;
        const limit = 2 * Math.max(r, s.r);
        return dx * dx + dy * dy < limit * limit;
      });
      if (collides) continue;
      const spike = magnitude.name === 'bright' && spikes < maxSpikes;
      if (spike) spikes++;
      if (color !== 'ink') {
        accentCounts[color]++;
        accentTotal++;
      }
      stars.push({ u, v, r, alpha, color, magnitude: magnitude.name, spike });
      break;
    }
  }
  return { surfaceId, bucket: [bw, bh], count, stars };
}

const round = (n) => Math.round(n * 100) / 100;

// One <path> per color and alpha step, never one element per star, so an
// illustration obeys the same rule the products do.
// `themed` paints through the profile's custom properties instead of the baked
// brand hex, for the specimens that are inlined into the themed page. The sky
// written to its own file cannot use it: a CSS `background-image` has no access
// to the page's custom properties, which is also why that sky is the one
// atmosphere layer a profile other than Deep Space does not draw.
export function starfieldSVG(
  surfaceId,
  width,
  height,
  { kind = 'screen', exclude = [], className = '', themed = false } = {},
) {
  // The themed form names its variables through the same resolver every specimen
  // uses, so a colour nothing publishes is a build failure rather than a sky that
  // paints itself black -- which is what writing var(--name) directly did.
  // Both forms resolve through a check. The themed one names a variable, and the
  // baked one names an anchor; either way a name nothing publishes used to reach
  // the document as `var(--typo)` or `fill="undefined"`, which is a sky that
  // silently drops its stars rather than a build that fails.
  const paint = (property, name) => {
    if (themed) return `style="${property}:var(${paintVariable(name, property)})"`;
    if (!Object.hasOwn(anchor, name))
      throw new Error(
        `starfieldSVG paints ${property} with "${name}", which is not a published anchor; a sky that drops its stars is not a drawing anybody can debug.`,
      );
    return `${property}="${anchor[name]}"`;
  };
  const { stars } = generateStars(surfaceId, width, height, kind);
  const visible = stars.filter(
    (s) => !exclude.some(([u0, v0, u1, v1]) => s.u >= u0 && s.u <= u1 && s.v >= v0 && s.v <= v1),
  );
  const groups = new Map();
  for (const s of visible) {
    const key = `${s.color}|${(Math.round(s.alpha * 10) / 10).toFixed(1)}`;
    const x = s.u * width;
    const y = s.v * height;
    groups.set(
      key,
      `${groups.get(key) || ''}M${round(x - s.r)} ${round(y)}a${round(s.r)} ${round(s.r)} 0 1 0 ${round(2 * s.r)} 0a${round(s.r)} ${round(s.r)} 0 1 0 ${round(-2 * s.r)} 0`,
    );
  }
  const spiked = visible.filter((s) => s.spike);
  const spikes = spiked
    .map((s) => {
      const x = round(s.u * width);
      const y = round(s.v * height);
      const l = round(s.r * 5);
      return `M${round(x - l)} ${y}H${round(x + l)}M${x} ${round(y - l)}V${round(y + l)}`;
    })
    .join('');
  const id = `${surfaceId.replace(/\W/g, '-')}-halo`;
  const halos = spiked
    .map(
      (s) =>
        `<circle cx="${round(s.u * width)}" cy="${round(s.v * height)}" r="${round(s.r * 4)}" fill="url(#${id})"/>`,
    )
    .join('');
  const paths = [...groups]
    .map(([key, d]) => {
      const [color, alpha] = key.split('|');
      return `<path ${paint('fill', color)} fill-opacity="${alpha}" d="${d}"/>`;
    })
    .join('');
  return `<svg${className ? ` class="${className}"` : ''} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false"><defs><radialGradient id="${id}"><stop ${paint('stop-color', 'ink')} stop-opacity=".55"/><stop offset="1" ${paint('stop-color', 'ink')} stop-opacity="0"/></radialGradient></defs>${halos}${paths}${spikes ? `<path d="${spikes}" ${paint('stroke', 'ink')} stroke-opacity=".45" stroke-width=".6" stroke-linecap="round"/>` : ''}</svg>`;
}

export const starfieldFixture = { surfaceId: 'design.reference', width: 390, height: 844, kind: 'screen' };

// One rounding for both parity fixtures: a native port that reproduces the star
// field and the motion curve is following the same arithmetic in both, so the
// precision they publish at is stated once and here.
export const six = (n) => Number(n.toFixed(6));

// Published so a native port can prove parity without reading this file: the
// fixture's count, accepted stars and first three stars are what the
// reference produces, and the site verifier regenerates them.
export function starfieldTokens() {
  const f = starfieldFixture;
  const sky = generateStars(f.surfaceId, f.width, f.height, f.kind);
  const n = (value, description) => ({
    $type: 'number',
    $value: value,
    ...(description ? { $description: description } : {}),
  });
  return {
    $description: 'The deterministic edge star layer shared by every platform that draws one. Watch screens draw none.',
    band: n(
      starfield.band,
      'Outer band, as a fraction of each edge, where stars may sit. The central rectangle stays clear.',
    ),
    bucket: {
      $type: 'dimension',
      $value: { value: starfield.bucket, unit: 'px' },
      $description: 'Size quantum; a resize regenerates only when it crosses a bucket.',
    },
    lowPowerFactor: n(
      starfield.lowPowerFactor,
      'Share of stars drawn under low power, thermal pressure or a low-performance tier.',
    ),
    $extensions: {
      'app.simt.cosmos': {
        density: starfield.density,
        attempts: starfield.attempts,
        magnitudes: starfield.magnitudes,
        maxSpikes: starfield.maxSpikes,
        inkShare: starfield.inkShare,
        maxAccentShare: starfield.maxAccentShare,
        accents: starfield.accents,
        drift: starfield.drift,
        algorithm: starfieldAlgorithm,
        fixture: {
          ...f,
          bucket: sky.bucket,
          count: sky.count,
          accepted: sky.stars.length,
          spiked: sky.stars.filter((s) => s.spike).length,
          first: sky.stars
            .slice(0, 3)
            .map((s) => ({ u: six(s.u), v: six(s.v), r: six(s.r), alpha: six(s.alpha), color: s.color })),
        },
      },
    },
  };
}
