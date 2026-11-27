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
