/**
 * The Cosmos design skill: a short, opinionated brief for an agent that is
 * about to write or change SIMT user interface.
 *
 * This is deliberately not another copy of the specification. rules.json
 * publishes all 77 rules once, and specification.md publishes them in prose;
 * this file answers a narrower question — which rules does generated interface
 * break most often — and then gets out of the way.
 *
 * The prose below is authored, but every rule title, requirement, acceptance
 * criterion and number is interpolated from the same source that renders the
 * HTML and the JSON, so the brief cannot drift from the specification it
 * summarizes. scripts/verify.js asserts that every rule ID cited here exists
 * and that the file stays inside its byte budget.
 */

/** The failures this brief exists to prevent, in the order they show up in review. */
const FAILURES = [
  ['C01', 'The decoration becomes the interface'],
  ['C06', 'Text assumed legible on an action fill'],
  ['C05', 'Contrast assumed instead of measured'],
  ['C07', 'A font request Cosmos did not sanction'],
  ['C16', 'Only the default state was designed'],
  ['C11', 'Atmosphere owned by the wrong element'],
  ['C27', 'Gradients used as texture, or a boundary animated'],
  ['C10', 'Blur everywhere'],
  ['C09', 'One size fitted to every screen'],
  ['C19', 'Targets shrunk to fit'],
  ['C17', 'Motion that cannot be turned off'],
  ['C20', 'Direction mirrored, geography not'],
  ['C21', 'Data invented to fill a layout'],
  ['C12', 'Two competing primary actions'],
  ['C22', 'A machine request answered with the home page'],
  ['C23', '"Looks right" offered as proof'],
];

// A share printed straight from a fraction prints 1.4999999999999998, and the
// brief is read by an agent that will take it as a figure. Rounded the same way
// the specification rounds it, so the two documents cannot disagree about the
// number they both publish.
const percent = (share) => String(Number((share * 100).toFixed(4)));

// One line per profile, each naming what that profile is for. The mapping is
// authored here rather than derived, because the role tables in tokens.js list
// colors, not intent, and the intent is the part an agent needs.
const PROFILE_PURPOSE = {
  deepSpace: 'the signature dark theme and the default',
  daylight: 'a light theme for bright environments, keeping the brand anchors',
  astronomy: 'red-luminance instruments for night observing, user-selected',
  watchAmbient: 'the always-on watch state, background and content only — interactive roles are deliberately absent',
};

export const skillRuleIds = FAILURES.map(([id]) => id);

export function createSkill(spec, budgets) {
  const { release, pages } = spec;
  const {
    colors,
    cssRolePrefix,
    cssThemeAttribute,
    metrics,
    themeProfiles,
    tokens,
    platformAdapters,
    gradients,
    shadows,
  } = spec.tokens;
  const { starfield } = spec.starfield;
  const { motionMotifs } = spec.motion;
  const { componentRegistry } = spec.components;

const byId = new Map(
  pages.flatMap((p) => p.sections.flatMap((s) => s.rules.map((r) => [r.id, { ...r, slug: p.slug, section: s }]))),
);

const ruleUrl = (slug, id) => `${release.base}/${slug}#${id.toLowerCase()}`;

function cite(id) {
  const rule = byId.get(id);
  if (!rule) throw new Error(`Cosmos skill cites a rule that does not exist: ${id}`);
  return {
    id,
    title: rule.title,
    requirement: rule.requirement,
    acceptance: rule.acceptance,
    url: ruleUrl(rule.slug, id),
  };
}

const duration = (name) => `${tokens.motion[name].$value.value}${tokens.motion[name].$value.unit}`;
const units = Object.values(tokens.space)
  .filter((t) => t.$type === 'dimension')
  .map((t) => t.$value.value);
// The per-part mapping is the answer to the two things an agent otherwise invents:
// a dp corner value and a cubic-bezier (C09, C30). A dp radius is a number token, so
// the roles come from the keys rather than from a filter on the type.
const dpRadius = (part) =>
  Object.values(platformAdapters[part].radius)
    .map((t) => t.$value)
    .join(' / ');
const easingNames = (part) => Object.keys(platformAdapters[part].easing).join(', ');

function numbers() {
  const v = metrics.webVitals;
  const d = budgets;
  const n = metrics.native;
  return `| Constraint | Value | Rule |
| --- | --- | --- |
| Minimum touch target | ${metrics.targets.web.value} CSS px web, ${metrics.targets.android.value} dp Android and Wear | C19 |
| Spacing and sizing units | ${[...new Set(units)].join(', ')} (1 unit = 1 CSS px or 1 dp) | C09 |
| Web radii, control / record / overlay | ${tokens.radius.control.$value.value} / ${tokens.radius.record.$value.value} / ${tokens.radius.overlay.$value.value} | C09 |
| Native radii, the part's own scale | Android ${dpRadius('android')} dp; Wear uses the native round and capsule shape, so it publishes no dp radius | C09 |
| Motion durations | ${duration('feedback')} feedback, ${duration('state')} local state, ${duration('context')} context, easing ${tokens.motion.easing.$value.map((n) => n.toFixed(1)).join(', ')} | C17 |
| Easings, the part's own set | web ${easingNames('webApp')}; Android and Wear ${easingNames('android')} | C30 |
| State-change motifs | ${motionMotifs.length}, one job each; animate a state change by naming the motif whose job it is, and a change with no motif does not animate | C31 |
| Component index | ${componentRegistry.length} components, sliced by platform part; a component names the rules, roles, states and motif it uses, and no treatment is restated there | C12 |
| Field web vitals (p${v.percentile}) | LCP ≤ ${v.lcpMs} ms, INP ≤ ${v.inpMs} ms, CLS ≤ ${v.cls} | C23 |
| Document budgets per route | HTML ≤ ${d.htmlBytesPerRoute} B, CSS ≤ ${d.cssGzipBytes} B gzip, JS ≤ ${d.jsGzipBytes} B gzip, first fonts ≤ ${d.initialFontsBytes} B | C23 |
| Native frame deadlines | ${n.frameDeadline120HzMs} ms at 120 Hz, ${n.frameDeadline90HzMs} ms at 90 Hz, ${n.frameDeadline60HzMs} ms at 60 Hz, ≤ ${n.allowedJankPercent}% jank | M12 |
| Contrast | 4.5:1 normal text, 3:1 large text, essential boundaries, focus and graphical objects | C05 |
| Light | ${gradients.length} gradients (${gradients.map((g) => g.name).join(', ')}) and ${shadows.length} shadows; at most one corona, one accretion ring and one nebula per screen | C27 |
| Star layer | Outer ${percent(starfield.band)}% band; area / ${starfield.density.screen.areaPerStar} clamped ${starfield.density.screen.min}–${starfield.density.screen.max} for screens, area / ${starfield.density.dialog.areaPerStar} clamped ${starfield.density.dialog.min}–${starfield.density.dialog.max} for dialogs; ≤ ${percent(starfield.maxAccentShare)}% of the layer in any one accent, so Ink is the majority; ${starfield.bucket} px/dp size bucket | C11 |`;
}

function failures() {
  return FAILURES.map(([id, headline]) => {
    const { title, requirement, acceptance } = cite(id);
    return `### ${headline} — ${id} ${title}

${requirement}

Conforms when: ${acceptance}`;
  }).join('\n\n');
}

// Every rule ID in this brief resolves at one address, stated once here instead
// of repeated as a full URL after each citation: a rule ID is a citation, and the
// brief is read far more often than it is followed to a rule. Spelling out
// ${release.base}/common#<rule id> fifteen times cost more bytes than the brief's
// entire failure list.
function ruleAnchorLine() {
  return `Rule IDs resolve at \`${release.base}/common#<lowercased rule id>\`.`;
}

function platforms() {
  return pages
    .filter((p) => p.slug !== 'common')
    .map((p) => {
      const ids = p.sections.flatMap((s) => s.rules.map((r) => r.id));
      return `- **${p.label}** (${p.slug}) — ${p.description} Rules: ${ids.join(', ')}. Read this part's own token and rule payload when you build it; each carries the common foundations and this adapter only.`;
    })
    .join('\n');
}

function themes() {
  return Object.keys(themeProfiles)
    .map((name) => `- \`${name}\` — ${PROFILE_PURPOSE[name] ?? 'published profile'}`)
    .join('\n');
}

function skillMarkdown() {
  const rules = FAILURES.map(([id]) => cite(id));

  return `---
name: cosmos
description: Apply the Cosmos ${release.version} design system when building or reviewing SIMT interface — marketing pages, the social web app, the Android app or the Wear OS app.
version: ${release.version}
updated: ${release.updated}
---

# Cosmos ${release.version} design skill

Cosmos ${release.version} is SIMT's normative design target, updated ${release.updated}. It carries ${byId.size} rules across five pages: one set of common foundations that every platform inherits, plus four platform parts. Platform rules are additive to the common rules, and product safety, privacy and accessibility take precedence over both.

Use this brief when you are about to write or change interface. It names the ${rules.length} failures generated interface produces most often. It is not the specification: read [${byId.size} rules](${release.base}/rules.json) when you need the rest, and cite the rule IDs you actually applied in your implementation notes (C02).

## Failures to avoid

${ruleAnchorLine()}

${failures()}

## Numbers you should not guess

${numbers()}

## Themes

Four profiles are published. Select one with \`${cssThemeAttribute}\` on the root or a container and consume the scoped \`${cssRolePrefix}*\` roles rather than raw colors; deepSpace is the default and dark is the signature.

${themes()}

Selection, in the order it resolves: an explicit \`${cssThemeAttribute}\` wins; with no attribute, \`prefers-color-scheme: light\` selects daylight and anything else selects deepSpace.

Five things that profile switch will break if you ignore them:

- **A role a profile does not publish resolves to \`initial\`, not to a color.** \`watchAmbient\` publishes no interactive roles, so \`${cssRolePrefix}action\` is \`initial\` and an element painted with it is transparent. Hide the control in that profile rather than inventing a fill.
- **Gradients and shadows are per-profile.** A gradient a profile does not run resolves to \`none\`. Split \`background-color\` from \`background-image\` so the flat role color is what shows through; the \`background\` shorthand takes the color down with the image.
- **Never bake a hex into an inline SVG.** \`var()\` is not resolved inside a presentation attribute, so it has to go in an inline \`style\`, and a fixture referenced from \`tokens.css\` cannot see the page's variables at all. A baked Deep Space value measures 1.1:1 on a Daylight surface.
- **A role can carry text.** An accent used as a fill and as small text cannot be the same value: \`${cssRolePrefix}action\` clears 4.5:1 against \`${cssRolePrefix}on-action\`, but the same accent on the Daylight background is 3.9:1. Take the accent-text role for text.
- **Measure the pair per profile.** The same two published values are not the same contrast under every profile; resolve both hexes through the profile's roles before you claim a ratio (C05).

Before composing a pair of colors, look it up in [\`tokens.json\`](${release.base}/tokens.json) → \`contrastPairs\`. Each pair carries its ratio in \`$value\` and the two token references in \`$extensions["app.simt.cosmos"]\`; resolve the hex through those references, and remember that \`$value >= 3\` is the large-text and non-text threshold. A pair with no \`recommendedTextUse\` is a measured pass that is still not text.

All ${colors.length} brand anchors are published in [\`tokens.css\`](${release.base}/tokens.css) and \`tokens.json\`.

## Motion

Three tiers, and only these three: **Full** (${duration('feedback')} feedback, ${duration('state')} local state, up to ${duration('context')} context), **Reduced** (no spatial travel, no ambient loops, opacity feedback ≤ 150 ms), **Static** (final state immediately). An explicit Static choice wins over everything; reduced-motion preference, data saving, low power, thermal pressure and a hidden page can only lower the tier. All three tiers must produce identical outcomes — reduced motion never disables accurate data.

A screen runs **one** ambient motion: the focal object's accretion disc, revolving once every 40–90 s. Move geometry the visitor reads as depth, never the outline around it — an animated ring or focus stroke is a boundary in motion, and C27 keeps boundaries static. Core, corona, labelled arcs and their marks hold still; a marker that drifts off its label makes the label a lie. Keep it on the compositor as a bare \`rotate\` with an explicit \`transform-origin\`, and stop it out of view and on a hidden tab — CSS keeps compositing in the background.

## Platform parts

${platforms()}

## Before you call it done

- The primary object, its state and the next action are readable in the first viewport in grayscale (C01).
- Every primary surface has its first-load, empty, denied, offline, error, unavailable and success treatment designed (C16).
- Every text and boundary pair you introduced is measured composited, not assumed (C05).
- Keyboard, touch, screen reader and — on watch — rotary reach every control; focus is visible and never clipped (C19, W04).
- English, French, Spanish and Arabic RTL were checked with long content; geography, north and compass mathematics are identical in both directions (C20).
- The numbers above are met with a recorded measurement: route, build, device, browser or OS, network, locale, motion tier, method (C23).

## Where the truth lives

- [Specification index](${release.base}/specification.json) — the page and section map with the rule ID in each; read this first.
- [All rules](${release.base}/rules.json) — every rule with its requirement and acceptance criterion.
- [Tokens](${release.base}/tokens.json) and [token variables](${release.base}/tokens.css) — typed values and theme adapters.
- [Composited contrast](${release.base}/contrast-pairs.json) — every measured role pair in every theme. Fetch it when you introduce a colour, pair a translucent or gradient surface, or check a theme; it is not part of the token payloads, because most layout, type and motion work never needs it (C05).
- [Motion motifs](${release.base}/motion.json) — the named choreographies, each with its trigger, steps, allowed properties, duration, easing, three tiers and CSS, Compose and Wear mapping, plus the sampled curve values a port reproduces. Fetch it when you animate a state change, when you choose between the tiers or decide whether a change animates at all, or when you port a transition to a native toolkit and prove curve parity; it is not part of the token payloads, because most layout, type and motion work needs no motif (C31).
- [Complete specification in Markdown](${release.base}/specification.md).
- Per-part payloads — \`${release.base}/{part}/tokens.json\` and \`${release.base}/{part}/rules.json\`, where \`{part}\` is one of ${pages
    .filter((p) => p.slug !== 'common')
    .map((p) => `\`${p.slug}\``)
    .join(
      ', ',
    )}. Fetch these instead of the canonical union when you know the platform: same common foundations, one adapter.
- [Component index](${release.base}/components.json) — for every component the rule IDs it inherits, the theme roles, layer and corner role it consumes, the states it can be in, the states it has opted out of and why, the motif that animates it and the counterpart that already exists in each product, with a slice per platform part. Fetch it when you start a component and need to know which rules apply to the thing in front of you; it carries references only, so a treatment is still read from the rule (C12).
- [Release record template](${release.base}/release-template.json) — the evidence a conformance claim needs.
`;
}

return { skillMarkdown, skillRuleIds };
}
