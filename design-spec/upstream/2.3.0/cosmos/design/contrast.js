import { srgbChannels } from './color.js';
import { anchor } from './tokens.js';

// WCAG relative luminance for opaque sRGB hex colors. Translucency/gradients
// still require checking the actual composited component (common rule C05).
function luminance(hex) {
  const channels = srgbChannels(hex).map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

export function contrastRatio(foreground, background) {
  const [lighter, darker] = [luminance(foreground), luminance(background)].sort((a, b) => b - a);
  return (lighter + 0.05) / (darker + 0.05);
}

// One measurement, published as little as possible. Everything a consumer needs
// is either the ratio or derivable from it, so the pair carries only the two
// token references and the text-use recommendation:
//
//   $value                      the ratio; >= 4.5 is AA normal text, >= 3 is AA
//                               large text and non-text
//   foregroundToken/backgroundToken  resolve to the exact hex through the token
//                               reference in this same document
//   recommendedTextUse          null when the pair clears no threshold for the
//                               role it was measured for
//   aaNormalText                whether the pair clears 4.5:1, so a reader does not
//                               have to compare two numbers to answer the question
//                               the document already has an answer to
//
// The dropped fields were the measured hex values, the two identical
// ratio >= 3 flags and a per-pair sentence of prose. Together they were about
// two thirds of the published bytes for information the reader can already
// derive, which matters because an agent pays for the whole file. The one flag
// kept is aaNormalText: it is the threshold a reader acts on, and a pair that
// published a ratio but not its eligibility made every consumer recompute it.
function pair(foreground, background, foregroundToken, backgroundToken, textUse) {
  const ratio = contrastRatio(foreground, background);
  const aaNormalText = ratio >= 4.5;
  return {
    $type: 'number',
    $value: Number(ratio.toFixed(6)),
    $extensions: {
      'app.simt.cosmos': {
        foregroundToken,
        backgroundToken,
        aaNormalText,
        ...(aaNormalText && textUse ? { recommendedTextUse: textUse } : {}),
      },
    },
  };
}

export function buildContrastPairs(adapters) {
  const palette = anchor;
  const darkText = {
    ink: 'primary text',
    muted: 'supporting text',
    faint: 'tertiary text',
    dim: null,
    ember: 'agency label',
    'ember-soft': 'warm emphasis',
    rose: 'social label',
    violet: 'identity label',
    gold: 'precision label',
    cyan: 'live-state label',
    moss: 'outdoor label',
    danger: 'error label',
    success: 'completion label',
    boundary: null,
  };
  const deepSpace = {
    $description:
      'Base dark palette. Dim and boundary are not recommended as essential text even when a numeric threshold passes.',
  };
  for (const surface of ['void', 'void-edge', 'hull', 'hull-raised', 'hull-highest', 'petrol', 'water']) {
    for (const [foreground, use] of Object.entries(darkText)) {
      deepSpace[`${foreground}-on-${surface}`] = pair(
        palette[foreground],
        palette[surface],
        `color.${foreground}`,
        `color.${surface}`,
        use,
      );
    }
  }
  deepSpace['void-on-ember'] = pair(palette.void, palette.ember, 'color.void', 'color.ember', 'primary action text');
  // The action gradient's lighter stop; C27 measures text against every stop.
  deepSpace['void-on-ember-soft'] = pair(
    palette.void,
    palette['ember-soft'],
    'color.void',
    'color.ember-soft',
    'primary action text',
  );
  const profiles = { deepSpace };
  // accentText and faint are measured here for the same reason they exist: an
  // accent cannot be both a fill and small text on a light surface, so the step
  // that carries the text needs its own published pair rather than an
  // assumption that the fill's pair carries over.
  const textUses = {
    text: 'primary text',
    muted: 'supporting text',
    faint: 'tertiary text',
    accentText: 'accent label',
    social: 'social label',
    identity: 'identity label',
    precision: 'precision label',
    live: 'live-state label',
    outdoors: 'outdoor label',
    danger: 'error label',
    success: 'completion label',
    bearing: 'bearing label',
    focus: null,
    boundary: null,
  };
  for (const [name, roles] of Object.entries(adapters)) {
    const group = {
      $description: `${name} semantic role pairs. Status meaning still requires labels/icons, including monochrome astronomy.`,
    };
    for (const surface of ['background', 'surface', 'raised', 'highest'].filter((role) => role in roles)) {
      for (const [foreground, use] of Object.entries(textUses).filter(([role]) => role in roles)) {
        group[`${foreground}-on-${surface}`] = pair(
          roles[foreground],
          roles[surface],
          `adapters.${name}.${foreground}`,
          `adapters.${name}.${surface}`,
          use,
        );
      }
    }
    if (roles.action && roles.onAction) {
      group['onAction-on-action'] = pair(
        roles.onAction,
        roles.action,
        `adapters.${name}.onAction`,
        `adapters.${name}.action`,
        'primary action text',
      );
    }
    profiles[name] = group;
  }
  return {
    $description:
      'Opaque sRGB WCAG contrast ratios, rounded to six decimals. Eligibility is published once per pair as aaNormalText rather than restated in prose: >= 4.5 is AA normal text, >= 3 is AA large text and non-text, and recommendedTextUse names the role the pair clears it for. Each pair resolves its exact hex colors through foregroundToken and backgroundToken in this document, and carries recommendedTextUse only when the pair clears AA for the role it was measured for; an absent recommendation means the pair is unsuitable or reserved for a non-text role. These measurements do not certify gradients, opacity, imagery, font sizing or an entire component.',
    ...profiles,
  };
}
