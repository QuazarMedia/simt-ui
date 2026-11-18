/** Standalone publication authority for the Cosmos site. */
export const authority = Object.freeze({
  name: 'SIMT Cosmos',
  origin: 'https://cosmos.simt.app',
  logo: '/images/simt_logo_500.webp',
  parent: 'https://simt.space/cosmos',
  tagline: 'The design contract for every SIMT product.',
  description:
    'SIMT Cosmos publishes the normative cross-platform design specification and its deterministic machine-readable resources.',
  published: '2026-10-03',
  updated: '2026-10-03',
  security: Object.freeze({
    contact: 'mailto:said@saidelimam.com',
    expires: '2027-10-05',
  }),
  discovery: Object.freeze({
    robots: '/robots.txt',
    sitemap: '/sitemap.xml',
    llms: '/llms.txt',
    fullText: '/llms-full.txt',
    ard: '/.well-known/ard.json',
    legacyCatalog: '/.well-known/ai-catalog.json',
  }),
  feed: '/releases.xml',
  stylesheet: '/styles/cosmos-design.css',
});

export function canonicalUrl(path = '') {
  if (path === '' || path === '/') return authority.origin;
  return `${authority.origin}/${String(path).replace(/^\/+|\/+$/g, '')}`;
}
