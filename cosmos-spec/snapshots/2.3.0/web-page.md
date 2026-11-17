# Web page — Cosmos 2.3.0

Status: Normative specification. Updated: 2026-10-03.

Canonical: https://simt.space/cosmos/design/web-page

Based on simt-social-landing. Create an invitation to real places and real people, with an open editorial composition and a glimpse of the product that feels usable.

Inherits [Common foundations](https://simt.space/cosmos/design/common).

## The editorial horizon

Section: https://simt.space/cosmos/design/web-page#composition

The first screen feels spacious and explains the product immediately.

### P01 — Two complementary planes [MUST]

Use a left-aligned narrative column and an asymmetric atlas on wide screens. The horizon is a fine shared baseline linking headline, time and place. On compact screens, show the proposition, one CTA and a cropped but meaningful atlas in normal reading order. The hero uses content height, not forced 100vh. It owns the page sky and its one nebula sits behind the atlas, never behind the headline.

Acceptance: At 390×844, the product purpose and primary CTA appear without waiting for animation. At 320px and large text, the hero grows with content rather than clipping it.

Rule: https://simt.space/cosmos/design/web-page#p01

### P02 — Product evidence over decoration [MUST]

The atlas shows representative activity records, time labels and approximate places. Label illustrative data. Use contour lines and a few weighted nodes to show relationships; do not mimic a production map with fake live participants. Include one real product screenshot or an accessible product walkthrough below the hero.

Acceptance: Every showcased interaction has an honest response or is a noninteractive figure. No fabricated member counts, testimonials, countdown urgency or unverifiable availability claims.

Rule: https://simt.space/cosmos/design/web-page#p02

### Public page sequence

- Sequence: 01 Invitation; Composition: Narrative + atlas + primary CTA; Decision answered: What can I do here?
- Sequence: 02 Proof; Composition: One real experience with annotated outcome; Decision answered: Does the product deliver it?
- Sequence: 03 Path; Composition: Discover → inspect → participate as a connected spine; Decision answered: How does it work?
- Sequence: 04 Possibilities; Composition: Role/category rows with real examples; Decision answered: Is this relevant to me?
- Sequence: 05 Trust; Composition: Plain privacy and location explanation; Decision answered: What am I sharing?
- Sequence: 06 Questions; Composition: Native disclosure FAQ; Decision answered: What still stops me?
- Sequence: 07 Next step; Composition: A calm final CTA and legal/locale links; Decision answered: Where do I go next?

## Responsive composition

Section: https://simt.space/cosmos/design/web-page#layout

Keep the visual rhythm while changing the arrangement.

### P03 — Grid & navigation [MUST]

Use a 1200px content maximum, 12 columns expanded, 6 medium and 4 compact. Gutters are 48px at ≥1200, 32px at 768–1199 and 20px below 768. Narrative blocks alternate open field, ruled rows and a single focused evidence panel. Header is 68px minimum with real anchor links; mobile menus are accessible disclosures.

Acceptance: At 320, 390, 768, 1024 and 1440px no page overflow or collision. Header can grow at large text; anchor headings remain unobscured. At 400% zoom navigation stays reachable.

Rule: https://simt.space/cosmos/design/web-page#p03

### P04 — Type & emphasis [MUST]

Use display 48–88px expressed in fluid rem, maximum 12–16 words, line-height ≥1.05. Body is 18px at comfortable widths and ≥16px compact. Keep the headline solid Ink; Ember accents one meaningful phrase. Show the primary action in the action gradient; allow a secondary text link with an explicit destination.

Acceptance: Headline wraps naturally in French and Arabic. No critical information is gradient text or microsized metadata. Equal prominence is not given to every link.

Rule: https://simt.space/cosmos/design/web-page#p04


## An atlas that tells the truth

Section: https://simt.space/cosmos/design/web-page#atlas

Give place and time an understandable structure.

### P05 — Narrative atlas behavior [MUST]

Use a small inline SVG or pre-rendered image for the public hero, with a nearby text list of representative records. If interactive, use native buttons to select a record and show a labeled detail region; all records work by keyboard/touch. Map controls must have real meaning. Reuse the same selected record in text and visual representations.

Acceptance: The page remains complete with JavaScript disabled and reduced motion. Selection is visible by label and shape. Decorative geography is marked schematic; exact private coordinates never enter the illustration.

Rule: https://simt.space/cosmos/design/web-page#p05

### P06 — Timelines & categories [MUST]

Use a horizon ribbon to order real dates or steps, with direct labels and connected milestones. Category color follows common meanings; category filters pair a label with count or selected state. Do not add an auto-scrolling marquee to communicate essential content.

Acceptance: All milestone and category content is available as text. Horizontal overflow has an obvious scroll or navigation affordance without fading essential labels.

Rule: https://simt.space/cosmos/design/web-page#p06


## Confident, calm conversion

Section: https://simt.space/cosmos/design/web-page#conversion

Make the destination and commitment clear.

### P07 — CTA contract [MUST]

Primary CTA names the next action and its actual destination. Preserve direct hrefs, keyboard activation and expected new-tab behavior. Explain sign-in or install requirements beside the relevant action. Use no looping CTA pulse, hover-only action, modal interruption or surprise auto-navigation.

Acceptance: A CTA works without client JavaScript. Track only allowlisted coarse events; never block navigation on analytics. External destinations and download labels match what opens.

Rule: https://simt.space/cosmos/design/web-page#p07

### P08 — Public content & localization [MUST]

Use one H1, a meaningful H2 outline, semantic landmarks, purpose-based alt text and visible legal/contact links. Localized pages use correct lang/dir, self-canonical URLs and reciprocal hreflang. Dates and assertions have a source/owner; legal copy is maintained independently of promotional language.

Acceptance: All key text is in the initial HTML, not canvas or image-only type. Arabic reading order and DOM order agree. Each locale has a complete primary journey and no mixed untranslated navigation.

Rule: https://simt.space/cosmos/design/web-page#p08


## Fast by construction

Section: https://simt.space/cosmos/design/web-page#delivery

The atmosphere should be cheaper than the story.

### P09 — Delivery budgets [MUST]

Target ≤30KiB gzip initial JavaScript, ≤35KiB gzip CSS, ≤120KiB initial font files and ≤250KiB initial hero media. Ship responsive AVIF/WebP with dimensions; load the actual LCP image eagerly and later media lazily. Load no full map engine, animation framework or video before it is requested.

Acceptance: Check production compression and request waterfall on a cold load. No unused font preload. Measure Core Web Vitals with constrained mobile lab diagnostics and real field cohorts after launch.

Rule: https://simt.space/cosmos/design/web-page#p09

### P10 — Enhancement lifecycle [MUST]

At most one optional compositor-only ambient hero motion in Full; stop it when out of view or page hidden. Content never starts invisible awaiting an observer. Reduced and data-saving modes show final static compositions. Third-party tags are isolated from critical rendering and governed by the product consent policy.

Acceptance: No animation-frame loop runs while scrolling the reference page. The first paint remains useful if script fails. Layout shift stays within the shared CLS budget.

Rule: https://simt.space/cosmos/design/web-page#p10


## Web page acceptance

Section: https://simt.space/cosmos/design/web-page#release

Verify the narrative and the real path out of it.

### P11 — Public release matrix [MUST]

Verify home, FAQ, legal, locale switch and each CTA at compact/medium/expanded widths; English, French and Arabic; keyboard, screen reader, no-JS, reduced motion and forced colors. Run focused Chromium/Firefox/WebKit checks for shared layout and browser APIs. Test real mobile Safari/Android at release.

Acceptance: Evidence includes first viewport and full page, link/status checks, metadata parity, accessibility findings and asset budgets. Do not advertise runtime or accessibility conformance based on screenshots alone.

Rule: https://simt.space/cosmos/design/web-page#p11


