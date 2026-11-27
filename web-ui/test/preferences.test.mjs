import assert from "node:assert/strict";
import { test } from "node:test";
import { initMotion, initTheme } from "../src/preferences.mjs";

const button = () => {
  const listeners = new Map();
  const attributes = new Map([["aria-label", "fallback"]]);
  return {
    dataset: {},
    attributes,
    addEventListener: (name, callback) => listeners.set(name, callback),
    removeEventListener: (name, callback) => {
      if (listeners.get(name) === callback) listeners.delete(name);
    },
    click: () => listeners.get("click")?.(),
    getAttribute: (name) => attributes.get(name) ?? null,
    setAttribute: (name, value) => attributes.set(name, value),
  };
};

const media = (matches) => ({
  matches,
  listeners: new Map(),
  addEventListener(name, callback) { this.listeners.set(name, callback); },
  removeEventListener(name, callback) { this.listeners.delete(name); },
  change(matches) {
    this.matches = matches;
    this.listeners.get("change")?.();
  },
});

const storage = () => {
  const values = new Map();
  return {
    values,
    get: (key) => values.get(key) ?? null,
    set: (key, value) => values.set(key, value),
    remove: (key) => values.delete(key),
  };
};

test("theme toggle follows the system without persisting an override", () => {
  const control = button();
  const root = { dataset: {} };
  const store = storage();
  const query = media(false);
  const document = { querySelectorAll: () => [], querySelector: () => null };
  initTheme(control, { root, document, media: query, storage: store, key: "theme" });
  assert.equal(control.dataset.state, "dark");
  control.click();
  assert.equal(root.dataset.theme, "light");
  assert.equal(store.values.get("theme"), "light");
  control.click();
  assert.equal(Object.hasOwn(root.dataset, "theme"), false);
  assert.equal(control.dataset.state, "dark");
  assert.equal(store.values.has("theme"), false);
  query.change(true);
  assert.equal(Object.hasOwn(root.dataset, "theme"), false);
  assert.equal(control.dataset.state, "light");
});

test("fixed-default themes never inherit the operating-system theme", () => {
  const control = button();
  const root = { dataset: { theme: "dark" } };
  const query = media(true);
  initTheme(control, { root, media: query, followSystem: false, document: { querySelectorAll: () => [] } });
  query.change(false);
  assert.equal(root.dataset.theme, "dark");
  control.click();
  assert.equal(root.dataset.theme, "light");
  assert.equal(control.attributes.get("aria-pressed"), "true");
});

test("motion toggle exposes the shared full and reduced states", () => {
  const control = button();
  const root = { dataset: { motion: "full" } };
  const query = media(false);
  const cleanup = initMotion(control, {
    root,
    media: query,
    messages: { motionPause: "Pause", motionPlay: "Play" },
  });
  assert.equal(control.attributes.get("aria-label"), "Pause");
  control.click();
  assert.equal(root.dataset.motion, "reduced");
  assert.equal(control.attributes.get("aria-label"), "Play");
  assert.equal(control.attributes.get("aria-pressed"), "true");
  cleanup();
});
