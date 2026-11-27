import assert from "node:assert/strict";
import { test } from "node:test";
import { observeReveals, pauseOffscreen, trackHeader, trackSections } from "../src/observers.mjs";

const classes = () => {
  const values = new Set();
  return {
    values,
    add: (value) => values.add(value),
    remove: (value) => values.delete(value),
    contains: (value) => values.has(value),
    toggle: (value, force) => force ? values.add(value) : values.delete(value),
  };
};

const element = (id = "") => {
  const attributes = new Map();
  return {
    id,
    hash: id ? `#${id}` : "",
    classList: classes(),
    attributes,
    setAttribute: (key, value) => attributes.set(key, value),
    removeAttribute: (key) => attributes.delete(key),
  };
};

class FakeObserver {
  static instances = [];
  constructor(callback, options) {
    this.callback = callback;
    this.options = options;
    this.targets = [];
    this.disconnected = false;
    FakeObserver.instances.push(this);
  }
  observe(target) { this.targets.push(target); }
  unobserve(target) { this.targets = this.targets.filter((item) => item !== target); }
  disconnect() { this.disconnected = true; }
  emit(entries) { this.callback(entries); }
}

test("reveals become visible once and disconnect cleanly", () => {
  FakeObserver.instances = [];
  const target = element();
  const cleanup = observeReveals([target], { Observer: FakeObserver });
  const observer = FakeObserver.instances[0];
  observer.emit([{ target, isIntersecting: false }]);
  assert.equal(target.classList.contains("is-revealed"), false);
  observer.emit([{ target, isIntersecting: true }]);
  assert.equal(target.classList.contains("is-revealed"), true);
  assert.equal(observer.targets.length, 0);
  cleanup();
  assert.equal(observer.disconnected, true);
});

test("missing IntersectionObserver leaves reveal content visible", () => {
  const target = element();
  observeReveals([target], { Observer: null });
  assert.equal(target.classList.contains("is-revealed"), true);
});

test("ambient sections pause offscreen", () => {
  FakeObserver.instances = [];
  const target = element();
  pauseOffscreen([target], { Observer: FakeObserver });
  const observer = FakeObserver.instances[0];
  observer.emit([{ target, isIntersecting: false }]);
  assert.equal(target.classList.contains("is-offscreen"), true);
  observer.emit([{ target, isIntersecting: true }]);
  assert.equal(target.classList.contains("is-offscreen"), false);
});

test("header tracking adds and removes its sentinel", () => {
  FakeObserver.instances = [];
  const header = element();
  let sentinel;
  const document = {
    body: { prepend: (value) => { sentinel = value; } },
    createElement: () => ({ setAttribute() {}, remove() {} }),
  };
  const cleanup = trackHeader(header, { document, Observer: FakeObserver });
  assert.ok(sentinel);
  FakeObserver.instances[0].emit([{ target: sentinel, isIntersecting: false }]);
  assert.equal(header.classList.contains("is-scrolled"), true);
  cleanup();
  assert.equal(FakeObserver.instances[0].disconnected, true);
});

test("section tracking updates every navigation link for a section", () => {
  FakeObserver.instances = [];
  const first = element("how");
  const duplicate = element("how");
  const section = element("how");
  const document = { getElementById: (id) => id === "how" ? section : null };
  trackSections([first, duplicate], { document, Observer: FakeObserver });
  FakeObserver.instances[0].emit([{ target: section, isIntersecting: true }]);
  assert.equal(first.attributes.get("aria-current"), "true");
  assert.equal(duplicate.attributes.get("aria-current"), "true");
});
