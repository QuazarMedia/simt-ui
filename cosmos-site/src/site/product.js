import { authority, canonicalUrl } from './authority.js';
import { pages, release } from '../design/specification.js';
import { GALLERY_FAMILIES } from '../design/gallery.js';

// A gallery route per family is only worth publishing if each stays inside the
// per-route HTML budget, so the family list is owned by the registry rather than
// typed here: a family added there gets a route here, and `npm run check` refuses
// a family page that would breach the budget.

// This Worker publishes one product directly at its canonical origin.
export const slug = 'cosmos';
export const basePath = '';
const base = authority.origin;

// The product's name, stated once: the descriptor, the agent catalog and a feed
// entry that names the product it belongs to are three places that would
// otherwise each carry their own copy of it.
const label = 'Cosmos Design System';

/** Canonical URL of a document inside this product. */
const url = (path) => canonicalUrl(path);

// The agent-skill media type is a property of that document, not of this module.
const AGENT_SKILL_MEDIA_TYPE = 'text/markdown; profile="urn:air:agent-skills"';

/**
 * Every human-readable route this product publishes, in the order a reader
 * meets them. `inSitemap` is explicit rather than inferred: a route can be
 * canonical and still not belong in a sitemap (a workbench, a specimen).
 */
export const routes = [
  {
    path: '',
    markdown: 'specification.md',
    label: 'Overview',
    summary:
      'One shared foundation and four platform parts. The four theme profiles are the single definition of a surface; Deep Space is the signature and Daylight, Astronomy red and Watch ambient are real, selectable adapters the documentation itself renders in.',
    inSitemap: true,
  },
  ...pages.map((p) => ({
    path: p.slug,
    markdown: `${p.slug}.md`,
    label: p.label,
    summary: p.description,
    inSitemap: true,
  })),
  // The gallery is a reading surface, not a contract: it renders the registry and
  // the rules, so it is in the sitemap as a way in, and every family route is its
  // own address so a reader can link to one family without linking to all of them.
  {
    path: 'gallery',
    label: 'Gallery',
    summary:
      "Every component drawn from its own contract: the state matrix in the rule's own wording, the timing from the named motif, and the colours from the published tokens.",
    inSitemap: true,
  },
  ...GALLERY_FAMILIES.map((family) => ({
    path: `gallery/${family.id}`,
    label: `Gallery · ${family.label}`,
    summary: `The ${family.label.toLowerCase()} family, governed by ${family.rules.join(' and ')}, every component drawn from its registry entry.`,
    inSitemap: true,
  })),
];

/**
 * The machine-readable documents this product publishes, each with the reason an
 * agent would open it. The agent brief and llms.txt are generated from this
 * list, so a document cannot be published without being described somewhere.
 */
const partPages = pages.filter((page) => page.slug !== 'common');
const partSlug = (slug) => slug.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());

export const resources = [
  {
    id: 'manifest',
    path: 'manifest.json',
    label: 'Resource manifest',
    mediaType: 'application/json',
    description:
      'Version, authority, platform inheritance, stable section URLs and download links. Start here for automated implementation.',
  },
  {
    id: 'index',
    path: 'specification.json',
    label: 'Specification index',
    mediaType: 'application/json',
    description:
      'The pages, their sections and the rule IDs in each. Read this first, then fetch only what the task needs.',
  },
  {
    id: 'rules',
    path: 'rules.json',
    label: 'Normative rules',
    mediaType: 'application/json',
    description:
      'Every rule once, grouped by platform section, each with its normative level, requirement, acceptance criterion and canonical URL.',
  },
  {
    id: 'tokens',
    path: 'tokens.json',
    label: 'Typed design tokens',
    mediaType: 'application/json',
    description:
      'Self-contained typed base tokens plus the theme adapters, the per-platform geometry and easing mapping, and the address of the composited contrast measurements.',
  },
  {
    id: 'contrastPairs',
    path: 'contrast-pairs.json',
    label: 'Composited contrast',
    mediaType: 'application/json',
    description:
      'Measured composited contrast for every role pair in every theme profile, with the guidance for each pair. Fetch it when introducing a colour, pairing a translucent surface or checking a theme.',
  },
  {
    id: 'motion',
    path: 'motion.json',
    label: 'Motion motifs',
    mediaType: 'application/json',
    description:
      'The named choreographies a state change animates with, each with its trigger, steps, allowed properties, duration, easing and per-tier and per-platform behaviour, plus the sampled curve values a native port reproduces. Fetch it when choosing a choreography, deciding between motion tiers or porting a transition to a native toolkit.',
  },
  {
    id: 'components',
    path: 'components.json',
    label: 'Component index',
    mediaType: 'application/json',
    description:
      'Rule IDs, theme roles, states, motifs and native counterparts per component, sliced by platform part.',
  },
  {
    id: 'css',
    path: 'tokens.css',
    label: 'Web token variables',
    mediaType: 'text/css',
    description: 'Scoped CSS custom properties, one set per theme profile, plus the brand anchors.',
  },
  {
    id: 'skill',
    path: 'cosmos-design-skill.md',
    label: 'Agent design skill',
    mediaType: 'text/markdown',
    description:
      'A short Markdown brief for an agent about to build or review SIMT interface. Quoted from the specification source, so it cannot drift.',
  },
  {
    id: 'markdown',
    path: 'specification.md',
    label: 'Complete plain-text specification',
    mediaType: 'text/markdown',
    description: 'The full readable contract in Markdown.',
  },
  {
    id: 'changelog',
    path: 'changelog.md',
    label: 'Release changelog',
    mediaType: 'text/markdown',
    description:
      'What every published Cosmos version changed, newest first. The current version also states its own changes in specification.md.',
  },
  {
    id: 'schema',
    path: 'specification.schema.json',
    label: 'Specification schema',
    mediaType: 'application/json',
    description: 'JSON Schema for the index.',
  },
  {
    id: 'rulesSchema',
    path: 'rules.schema.json',
    label: 'Rules schema',
    mediaType: 'application/json',
    description: 'JSON Schema for the normative rules document.',
  },
  // The per-part payloads are declared here rather than left to the generator,
  // because this list is what llms.txt and the agent catalog are built from: a
  // document that exists but is not declared is a document no agent is told
  // about, and the four parts are the whole reason a reader fetches one payload
  // instead of the whole specification.
  ...partPages.flatMap((part) => [
    {
      id: `${partSlug(part.slug)}Tokens`,
      label: `${part.label} tokens`,
      path: `${part.slug}/tokens.json`,
      mediaType: 'application/json',
      description: `The ${part.label} part's tokens: the common foundations plus only that platform's adapter, geometry and easing, so a ${part.label} build never reads the other three parts.`,
    },
    {
      id: `${partSlug(part.slug)}Rules`,
      label: `${part.label} rules`,
      path: `${part.slug}/rules.json`,
      mediaType: 'application/json',
      description: `The rules the ${part.label} part inherits: the common rules plus only those that constrain it, each with its normative level, requirement and acceptance criterion.`,
    },
  ]),
  {
    id: 'releaseTemplate',
    path: 'release-template.json',
    label: 'Release record template',
    mediaType: 'application/json',
    description: 'Product, build, environment, applicable rules, evidence, dependencies and exceptions.',
  },
];

/**
 * Byte budgets for this product's published documents. They live with the
 * product rather than in a shared constant so each product is held to its own
 * size, and so a number nobody intends to hold can never be the reason a new
 * product ships. The measured contrast pairs get their own budget because they
 * are the one block a routine build does not read (C05, C22).
 */
export const budgets = {
  htmlBytesPerRoute: 81920,
  cssGzipBytes: 20480,
  jsGzipBytes: 6144,
  initialFontsBytes: 122880,
  // The index is what an agent reads first; keep it small enough that it always is.
  // Raised twice, from 24 KiB and then 26 KiB, and the growth is content rather
  // than rules or tokens: the index measured 17 766 B when the ceiling was set,
  // and the phases that added motion motifs, the component registry, the
  // galleries and the verification each added a generated table to it and the
  // release notes for that phase — 17 766, then 21 577, 22 949, 24 599, 25 265
  // and 26 829. Rules and tokens still live in their own documents; what grew
  // here is the map and the record of how it changed.
  //
  // This raise is the point at which the ceiling should be read as a smell
  // rather than obeyed. The release notes are 10 846 of these bytes, so four
  // fifths of the growth across four phases has been prose about the phases, and
  // Back to the original 24 KiB. The ceiling had to be raised twice, to 26 and then
  // 28, because the release notes had grown to be the larger half of this index and
  // a defect fix worth 205 B was nearly deleted to stay under a limit the notes
  // themselves created. The notes now live only in changelog.md and the index points
  // at them, so this measures the map again: 15.6 KiB, of which the pages, the rule
  // index and the provenance are the whole content.
  specificationBytes: 24 * 1024,
  // Raised from 72 KiB when the oklch projection became real colour tokens rather
  // than a block of notation strings. It was previously quoted at roughly 6 KiB and
  // actually cost about 12, because it was published once per web adapter; it is now
  // one group of typed values, which is a third larger again but is a colour a token
  // tool can emit instead of a string it has to parse (C29). A restated colour is
  // what this budget normally refuses; the projection earns the exception because a
  // client cannot convert a hex to oklch correctly by hand. Lowered from 132 KiB
  // when the measured contrast pairs moved to their own address (C05, C22).
  tokensBytes: 84 * 1024,
  // Per-part payloads exist so an agent never reads another platform, and now also
  // never reads a profile, gradient or star layer that platform does not draw. That
  // promise only holds while a payload stays materially smaller than the union it was
  // carved out of, so each payload has its own budget. Raised from 56 to 60 KiB when
  // the oklch projection became typed colour tokens: the wrist payload fell by half
  // while the two web payloads, the ones that can emit oklch, grew by the difference
  // between a notation string and a colour token. This ceiling is set by the web
  // parts, which is the honest reading of a budget for the largest payload.
  // partTokensBytes was 124 KiB until the contrast pairs left it, which is exactly
  // the block it was carrying. Raised again from 60 to 64 KiB when a payload had to
  // carry the action gradient of C12 and the rim of C10: both are common rules, all
  // four parts inherit them, and a payload that filtered them out could not satisfy
  // the rules it inherits from the only document C22 tells it to read. The web page
  // is the largest payload at 59 215 B and sets this ceiling, so the raise is that
  // content and not padding.
  partTokensBytes: 64 * 1024,
  // Sized for the next rule, not for today's payload. It was set to the measured
  // size of the largest part — 51.1 KiB of a 52 KiB ceiling — which meant every
  // additional rule forced a raise, and a budget that must be raised on every
  // change is not a budget, it is a snapshot. The component registry consumed
  // that headroom rather than filling it: the shared state vocabulary grew from
  // eight states to the eleven the registry exposes, and the components section
  // now carries a generated component index. Neither is padding — the index is two
  // columns, and the parts, corner and motion detail a reader would want next to
  // it are published as per-part slices in components.json instead of repeated in
  // every rules payload. The headroom below is roughly three mobile rules again.
  partRulesBytes: 60 * 1024,
  // A complete pass reads every token, so the contrast matrix has to stay cheap per
  // measurement rather than merely cheap per byte over the wire. The file carries 19
  // semantic roles per profile and 212 measurements; the headroom is deliberately
  // thin so the next role added has to argue for the bytes.
  contrastPairBytes: 72 * 1024,
  // A motion port reads this document once per transition it implements, so it is
  // sized against what a port actually needs rather than against the token payloads:
  // seven motifs carrying their steps, tiers and three platform mappings, and a
  // parity fixture of 21 sampled values for each of the 15 published curves. The
  // sampled values are what make this expensive — they are 2.8 KiB of the file — and
  // they are the part that cannot be summarised, because matching them is how a port
  // proves it agrees. Measured at 23.2 KiB, so the headroom is roughly one motif: a
  // second choreography is a normal edit and doubling the set is not.
  motionBytes: 28 * 1024,
  // The registry is references, so it is small where the token documents are not:
  // every entry is names and IDs rather than prose, and the treatments it points
  // at are already published in the rules. The per-part slices are id lists derived
  // from the entries, so a part that gains a component grows the union by one entry
  // and one line in a slice, never by a fourth copy of the registry.
  // Raised 20 to 24 KiB. The index now answers for all eleven states on all
  // seventeen components, which added 2.9 KiB of opt-out codes and 1.3 KiB of the
  // reason each code is defined. That is the whole point of the document: a
  // reader who cannot tell a component that deliberately has no `selected` from
  // one nobody has thought about has to read seventeen entries to find out. The
  // 20 KiB ceiling was set against 13 833 B with headroom for the next entry; the
  // entries did not grow, the coverage did.
  componentBytes: 24 * 1024,
  // The skill is the only thing an agent is expected to read start to finish, so it
  // has to stay a brief. Growing it toward the full specification would defeat the
  // reason it exists. This ceiling was raised three times for one reason: 20 to
  // 21 KiB when each platform line gained two per-part payload URLs, 21 to 22 KiB
  // for a line naming the contrast document, and 22 to 23 KiB when the canonical
  // origin moved to simt.space and every absolute URL it quoted grew two
  // characters. URLs and address guidance, never prose — so the mechanism is fixed
  // rather than the ceiling raised a fourth time: a rule ID now resolves at a single
  // address stated once, and one pattern line covers all four part payloads. The
  // brief quotes 11 absolute URLs where it used to quote 29, and 22 KiB is now too
  // small to hold it, which is the honest reading: the saving went into the two
  // descriptions a part payload needs to be self-explanatory, not into slack.
  //
  // Raised once more, to 24 KiB, for the one addition that is not an address: C31
  // makes a motif the only way to animate a state change, so a brief that omits the
  // rule sends an agent to invent a transition — the exact failure the document
  // exists to prevent. The addition is a numbers row and a fetch-when line, both
  // derived from the published values rather than typed, and both naming one
  // document the same way the contrast bullet does. It is content, not URL creep,
  // which is why the three previous raises were all about URLs and this one is
  // stated separately.
  //
  // The component index added a fourth numbers row and a fourth fetch-when
  // line, for the same reason: a brief that cannot tell an agent which rules apply
  // to the component in front of it sends it to guess. That is content again, and
  // it leaves 684 B under this ceiling — thin on purpose, because the next thing
  // that belongs in a brief should be argued here rather than absorbed by raising
  // a number nobody reads.
  skillBytes: 24 * 1024,
};

/** This product's sections of /llms.txt. */
export function llms() {
  const line = (r) => `- [${r.label}](${url(r.path ? `${r.path}/` : '')}): ${r.summary}`;
  const doc = (r) => `- [${r.label}](${url(r.path)}): ${r.description}`;
  return `## Cosmos Design System

${routes.map(line).join('\n')}

### Machine-readable resources

${resources.map(doc).join('\n')}`;
}

/**
 * Entries this product contributes to /.well-known/ard.json and its
 * predecessor /.well-known/ai-catalog.json. The catalog is the discovery
 * surface an agent reads before it knows a product exists, so an entry states
 * what it answers as well as what it is.
 */
export function agentEntries({ ruleCount, skillRuleIds }) {
  return [
    {
      identifier: 'urn:air:cosmos.simt.app:design-skill',
      displayName: 'Cosmos design skill',
      type: AGENT_SKILL_MEDIA_TYPE,
      url: url('cosmos-design-skill.md'),
      description: `A short Markdown brief for an agent about to build or review SIMT interface: the ${skillRuleIds.length} Cosmos rules generated interface breaks most often, the numbers it should not guess, how to read the theme adapters and contrast pairs, and the checks to run before claiming conformance. Every rule text and number is generated from the Cosmos specification, so it cannot drift from it.`,
      tags: ['design-system', 'accessibility', 'implementation', 'cosmos'],
      capabilities: ['design-review', 'ui-authoring', 'conformance-preparation'],
      representativeQueries: [
        'Which Cosmos rules should an agent follow when building SIMT interface?',
        'How do I pick accessible colors from the Cosmos design tokens?',
        'What are the Cosmos touch target and web vitals budgets?',
        'How do I design the Cosmos reduced-motion tier?',
      ],
      version: release.version,
      updatedAt: `${release.updated}T00:00:00Z`,
    },
    {
      identifier: 'urn:air:cosmos.simt.app:specification',
      displayName: 'Cosmos specification index',
      type: 'application/json',
      url: url('specification.json'),
      description: `The machine-readable map of the Cosmos specification: ${pages.length} platform parts, their sections, the ${ruleCount} normative rules in total, the metrics they constrain, the byte budget each published document is held to, and the provenance of the system. Rules, tokens and the JSON schemas are listed under resources.`,
      tags: ['design-system', 'specification', 'tokens', 'cosmos'],
      capabilities: ['specification-lookup', 'rule-lookup', 'token-lookup'],
      representativeQueries: [
        'Which Cosmos rule covers a given interface decision?',
        'What are the Cosmos acceptance criteria for this rule ID?',
        'Which token publishes the value for this metric?',
      ],
      version: release.version,
      updatedAt: `${release.updated}T00:00:00Z`,
    },
  ];
}

/**
 * Route rewrites for this product. Wrangler's asset handler does not resolve a
 * directory to its index.html, so each clean route is declared as a 200 rewrite:
 * never a redirect, because the bare form is canonical and appears in published
 * JSON. Both spellings are listed so a visitor who types the other one gets the
 * same bytes.
 *
 * There is deliberately no wildcard rule. A `/cosmos-design/*.html` redirect was
 * here and it broke every published page: a Cloudflare splat matches `/` as well
 * as any other character, so it matched the nested per-part `index.html` files
 * this build actually writes and 301'd each to a path that is neither published
 * nor rewritten — which, under `html_handling: "none"`, is the noindex 404 page.
 * A single splat cannot express the intent either: one anchored on
 * `index.html` would capture `common/index` into `:splat`. And it was the one
 * rule verification could not check, because a rule with a `*` or a `:splat` has
 * no resolvable target. `checkRewrites` now refuses any rule it would have to
 * skip, so the cheapest way to serve the `.html` spelling is a 200 rewrite that
 * resolves.
 */
export function rewrites() {
  return routes.flatMap((route) => {
    if (route.path === '') return ['/ /index.html 200'];
    const clean = `/${route.path}`;
    return [`${clean} ${clean}/index.html 200`, `${clean}/ ${clean}/index.html 200`];
  });
}

/**
 * Where this sub-context published before it moved under the context root. The
 * move is a permanent one, so the old address answers with a 301 rather than a
 * second copy of the document: a link already in the wild -- in another
 * repository's documentation, in a published changelog, in somebody's notes --
 * arrives, and no agent finds two canonical URLs for one specification.
 */
export const previousBasePath = '/cosmos-design';

/**
 * The redirects that carry the old address to the new one. Two rules rather
 * than one per published document, because the source is a prefix that
 * published nothing and the destination is the whole sub-context: a splat here
 * is safe for the reason the wildcard above is not. That rule redirected a
 * published `index.html` to a path this build does not write; this one redirects
 * a path this build does not publish to one it does, and the two prefixes cannot
 * match each other, so there is no request that can loop.
 *
 * `scripts/verify.js` checks the shape of both rules and proves the coverage
 * against what the build published, so the one rule no gate would otherwise be
 * able to resolve is checked the only way a splat can be.
 */
/**
 * Every version this product has published, newest first, for the origin's
 * release feed.
 *
 * The versions are the release record's own — `release.history` plus the release
 * currently being published — rather than a second list, so a version can be
 * named in two places only if it were already named twice in the changelog. The
 * notes are not carried: a release note here is a paragraph written for the
 * changelog and read in context, and a feed entry that repeats all of them is a
 * document nobody can subscribe to. The feed links the changelog, which is where
 * the notes are read.
 *
 * Each entry states its own `summary` because a feed entry with only a title is
 * a list and not a feed; the current release takes the scope the specification
 * already publishes, and a release added to `release.history` supplies the line
 * that describes it. `scripts/verify.js` holds every entry to the shape, so a
 * historical release moved in without one fails the build rather than shipping a
 * titleless entry.
 */
export const releases = [
  ...release.history,
  { version: release.version, updated: release.updated, summary: release.scope },
]
  .reverse()
  .map((entry) => ({ ...entry, url: base, title: label }));

export function redirects() {
  return [`${previousBasePath} / 301`, `${previousBasePath}/* /:splat 301`];
}

/** Cache and content-type rules for this product's published paths. */
export function headerRules() {
  const immutable = (days) => `Cache-Control: public, max-age=${60 * 60 * 24 * days}${days > 30 ? ', immutable' : ''}`;
  const served = (contentType) =>
    `Content-Type: ${contentType}; charset=utf-8\nCache-Control: public, max-age=0, must-revalidate\nX-Content-Type-Options: nosniff`;
  return [
    `/sky-hero.svg\n  ${immutable(7)}`,
    `/*.json\n  ${served('application/json')}`,
    `/*.md\n  ${served('text/markdown')}`,
    `/*.css\n  ${served('text/css')}`,
  ];
}

/**
 * What this product claims about accessibility, and the agent brief that carries
 * the detail. It lives here rather than in humans.txt because the claim belongs
 * to the product: an origin can publish several, and an envelope paragraph that
 * names one of them is a paragraph the next product makes false. The brief is
 * named as a path rather than written as a sentence, and scripts/site.js refuses
 * to publish a brief this product does not declare in `resources` above.
 */
export const accessibility = {
  target: 'WCAG 2.2 AA on the web, with equivalent behaviour natively',
  brief: 'cosmos-design-skill.md',
};

const product = {
  slug,
  accessibility,
  label,
  summary:
    'The design contract for every SIMT product: one specification, five parts, and the machine-readable tokens, rules and agent brief derived from it.',
  version: release.version,
  updated: release.updated,
  releases,
  base,
  basePath,
  // The legacy path remains a same-host compatibility redirect.
  previousBasePath,
  stylesheet: authority.stylesheet,
  routes,
  resources,
  fullTextResources: ['specification.md', 'cosmos-design-skill.md'],
  budgets,
  llms,
  agentEntries,
  rewrites,
  redirects,
  headerRules,
};

export default product;
export const products = [product];
export const productRouteUrl = (route) => (route.path ? canonicalUrl(route.path) : authority.origin);
export const productResourceUrl = (resource) => canonicalUrl(resource.path);
export function budgetOf(key) {
  if (key === undefined) return budgets;
  if (budgets[key] === undefined) throw new Error(`Cosmos declares no ${key} budget`);
  return budgets[key];
}
