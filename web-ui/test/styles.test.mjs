import assert from "node:assert/strict";
import { test } from "node:test";
import { readFile } from "node:fs/promises";

const css = await readFile(new URL("../src/styles.css", import.meta.url), "utf8");

test("marquee section owns a full-width viewport and a static list fallback", () => {
  assert.match(css, /\.marquee\s*\{[^}]*width:\s*100%;/s);
  assert.match(css, /\.marquee__viewport\s*\{[^}]*width:\s*100%;[^}]*overflow:\s*clip;/s);
  assert.match(css, /\.marquee__track\s*\{[^}]*margin:\s*0;[^}]*list-style:\s*none;/s);
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\) \.marquee__rail\s*\{[^}]*animation:\s*simt-marquee/s);
});

test("finale section owns the colored corona and skips animation in lite mode", () => {
  assert.match(css, /\.finale\s*\{[^}]*width:\s*100%;/s);
  assert.match(css, /\.finale::before\s*\{[^}]*var\(--ember,[^}]*var\(--violet,/s);
  assert.match(css, /\.finale__corona span\s*\{[^}]*width:\s*min\(92vw, 920px\);/s);
  assert.equal(css.match(/\.finale__corona span:nth-child\([1-4]\)/g)?.length, 4);
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\) \.finale__corona span\s*\{[^}]*animation:\s*simt-finale-corona/s);
  assert.match(css, /@keyframes simt-finale-corona\s*\{[^}]*\}\s*25%\s*\{[^}]*opacity:\s*0\.8;/s);
});

test("FAQ section owns a responsive sticky heading and native card rows", () => {
  assert.match(css, /\.faq__grid\s*\{[^}]*grid-template-columns:\s*minmax\(0, 1fr\) minmax\(0, 1\.6fr\);/s);
  assert.match(css, /\.faq__heading\s*\{[^}]*position:\s*sticky;[^}]*var\(--header-height, 68px\)/s);
  assert.match(css, /@media \(width < 960px\)\s*\{\s*\.faq__grid\s*\{[^}]*grid-template-columns:\s*minmax\(0, 1fr\);[^}]*\}\s*\.faq__heading\s*\{[^}]*position:\s*static;/s);
  assert.match(css, /\.faq-item\s*\{[^}]*border-radius:\s*var\(--radius-lg, 20px\);[^}]*background:\s*var\(--surface-subtle,/s);
  assert.match(css, /\.faq-item\[open\]\s*\{[^}]*border-color:\s*var\(--outline-strong,[^}]*background:\s*var\(--surface,/s);
  assert.match(css, /\.faq-item > summary::-webkit-details-marker\s*\{\s*display:\s*none;/s);
  assert.match(css, /\.faq-item__a\s*\{[^}]*padding:\s*0 var\(--space-6, 24px\) var\(--space-6, 24px\);/s);
});

test("header owns the fixed glass shell, shared navigation and native locale control", () => {
  assert.match(css, /\.site-header\s*\{[^}]*position:\s*fixed;[^}]*height:\s*var\(--header-height, 68px\);/s);
  assert.match(css, /\.site-header\.is-scrolled\s*\{[^}]*background:\s*var\(--surface-glass, var\(--glass,[^}]*backdrop-filter:\s*blur/s);
  assert.match(css, /:root\[data-perf="lite"\] \.site-header\.is-scrolled\s*\{[^}]*backdrop-filter:\s*none;/s);
  assert.match(css, /\.site-nav\s*\{[^}]*margin-inline:\s*auto;/s);
  assert.match(css, /\.site-nav a\[aria-current="true"\]\s*\{[^}]*box-shadow:\s*inset/s);
  assert.match(css, /\.locale-switch__select\s*\{[^}]*min-height:\s*40px;[^}]*border-radius:\s*var\(--radius-pill, 999px\);/s);
  assert.match(css, /@media \(width < 1000px\)\s*\{\s*\.site-nav\s*\{\s*display:\s*none;/s);
});

test("header preserves no-JavaScript locale links and gates decorative brand motion", () => {
  assert.match(css, /:root:not\(\.js\) \.locale-switch__select\s*\{\s*display:\s*none;/s);
  assert.match(css, /:root:not\(\.js\) \.locale-switch__links\s*\{\s*display:\s*flex;/s);
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\) \.brand:hover \.brand__mark\s*\{\s*transform:\s*rotate\(45deg\);/s);
  assert.match(css, /@media \(width < 360px\)\s*\{\s*\.site-header \[data-motion-toggle\]\s*\{\s*display:\s*none;/s);
});

test("FAQ disclosure and icon motion require full motion outside lite mode", () => {
  const gate = /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\)/;
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\)\s*\{\s*interpolate-size:\s*allow-keywords;/s);
  assert.match(css, new RegExp(`${gate.source} \\.faq-item::details-content\\s*\\{[^}]*height:\\s*0;[^}]*transition:\\s*height var\\(--medium, 300ms\\)[^}]*content-visibility var\\(--medium, 300ms\\) allow-discrete;`, "s"));
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\) \.faq-item\[open\]::details-content\s*\{\s*height:\s*auto;/s);
  assert.match(css, /:root\[data-motion="full"\]:not\(\[data-perf="lite"\]\) \.faq-item__icon\s*\{[^}]*transition:\s*rotate var\(--medium, 300ms\)/s);
  assert.match(css, /\.faq-item\[open\] > summary \.faq-item__icon\s*\{[^}]*rotate:\s*45deg;[^}]*background:\s*var\(--ember,/s);
});
