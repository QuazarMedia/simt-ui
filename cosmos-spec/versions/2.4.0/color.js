// sRGB and Oklab conversions, used for one reason: the anchors stay authored in
// sRGB hex, because that is what WCAG contrast, the published contrast pairs and
// every non-colour-managed tool read, and a projected oklch value is publishable
// alongside them for authoring and theming (C29).
//
// The projection is derived, never authored. Authoring in oklch would put a second
// source of truth in the file, and any oklch coordinate outside the sRGB gamut has
// to be clipped before it can be measured, which is exactly the rounding the
// contrast checks are there to catch.
//
// Values come from Björn Ottosson's Oklab matrices; the inverse is used by the
// verification pass, so a published oklch is proven to name the hex it came from.
const clamp = (n) => Math.min(1, Math.max(0, n));
const round = (n, places) => Number(n.toFixed(places));
const encode = (channel) => (channel <= 0.0031308 ? 12.92 * channel : 1.055 * channel ** (1 / 2.4) - 0.055);
const decode = (channel) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4);

/** `#rrggbb` to Oklab, in the 0..1 range Oklab uses. */
export function srgbChannels(hex) {
  return hex
    .slice(1)
    .match(/../g)
    .map((channel) => parseInt(channel, 16) / 255);
}

function hexToOklab(hex) {
  return componentsToOklab(srgbChannels(hex));
}

/**
 * The projection as DTCG colour-token components, not as CSS text. A client that
 * reads the token file with Style Dictionary or Tokens Studio needs a `$value` it
 * can emit; a string it has to parse is one it will skip. Rounded to the same
 * precision as oklchNotation, so a token and its notation name one colour.
 */
export function srgbOklchComponents(components, alpha = 1) {
  const [L, a, b] = componentsToOklab(components);
  const hue = (Math.atan2(b, a) * (180 / Math.PI) + 360) % 360;
  return [round(L * 100, 2), round(Math.hypot(a, b), 4), round(hue, 2)];
}

function componentsToOklab([red, green, blue]) {
  const r = decode(red);
  const g = decode(green);
  const b = decode(blue);

  const l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
  const m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
  const s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;
  const l_ = Math.cbrt(l);
  const m_ = Math.cbrt(m);
  const s_ = Math.cbrt(s);

  return [
    0.2104542553 * l_ + 0.793617785 * m_ - 0.0040720468 * s_,
    1.9779984951 * l_ - 2.428592205 * m_ + 0.4505937099 * s_,
    0.0259040371 * l_ + 0.7827717662 * m_ - 0.808675766 * s_,
  ];
}

/** Oklab back to `#rrggbb`, with the gamut clipped per channel. */
export function oklabToHex([L, a, b]) {
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = L - 0.0894841775 * a - 1.291485548 * b;
  const l = l_ ** 3;
  const m = m_ ** 3;
  const s = s_ ** 3;

  const channels = [
    encode(clamp(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s)),
    encode(clamp(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s)),
    encode(clamp(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s)),
  ];
  return `#${channels
    .map((channel) =>
      Math.round(channel * 255)
        .toString(16)
        .padStart(2, '0'),
    )
    .join('')}`;
}

/**
 * The published CSS notation. Four decimals of chroma and two of hue are more
 * than an 8-bit channel can resolve, and a shorter form would drift between
 * engines that round intermediate values differently — which would make a
 * screenshot baseline change without a colour changing.
 */
export function oklch(hex) {
  return oklchNotation(hexToOklab(hex));
}

function oklchNotation([L, a, b]) {
  const chroma = Math.hypot(a, b);
  // Hue is normalized into 0-360 rather than left negative, because that is the
  // notation every Cosmos client already writes and a diff between two spellings
  // of one colour is noise.
  const hue = (Math.atan2(b, a) * (180 / Math.PI) + 360) % 360;
  // No alpha branch, because nothing published a translucent oklch: the only
  // caller was srgbOklch, which had none either.
  return `oklch(${(L * 100).toFixed(2)}% ${chroma.toFixed(4)} ${hue.toFixed(2).replace(/\.?0+$/, '')}deg)`;
}

/** The inverse of srgbOklchComponents, so a published token can be checked. */
export function oklchComponentsToOklab([lightness, chroma, hue]) {
  const radians = hue * (Math.PI / 180);
  return [lightness / 100, chroma * Math.cos(radians), chroma * Math.sin(radians)];
}

/** Parses the notation this module publishes, so verification can invert it. */
export function parseOklch(notation) {
  const [, lightness, chroma, hue, alpha] =
    /oklch\(([\d.]+)% ([\d.]+) ([\d.-]+)deg(?: \/ ([\d.]+))?\)/.exec(notation) ?? [];
  if (lightness === undefined) throw new Error(`Not an oklch notation: ${notation}`);
  const radians = Number(hue) * (Math.PI / 180);
  const value = Number(chroma);
  return {
    lab: [Number(lightness) / 100, value * Math.cos(radians), value * Math.sin(radians)],
    alpha: alpha === undefined ? 1 : Number(alpha),
  };
}
