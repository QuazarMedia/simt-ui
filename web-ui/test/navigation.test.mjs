import assert from "node:assert/strict";
import { test } from "node:test";
import { initLocaleSelect } from "../src/navigation.mjs";

const select = () => {
  const listeners = new Map();
  return {
    value: "",
    listeners,
    addEventListener: (name, callback) => listeners.set(name, callback),
    removeEventListener: (name, callback) => listeners.delete(name),
    change: () => listeners.get("change")?.(),
  };
};

test("locale selector navigates only to a different non-empty route", () => {
  const control = select();
  const assignments = [];
  const location = { pathname: "/fr/", assign: (target) => assignments.push(target) };
  const cleanup = initLocaleSelect(control, { location });

  control.change();
  control.value = "/fr/";
  control.change();
  control.value = "/ar/";
  control.change();

  assert.deepEqual(assignments, ["/ar/"]);
  cleanup();
  assert.equal(control.listeners.has("change"), false);
});

test("missing locale selector is a no-op", () => {
  assert.doesNotThrow(() => initLocaleSelect(null)());
});
