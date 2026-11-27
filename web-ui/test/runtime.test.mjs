import assert from "node:assert/strict";
import { test } from "node:test";
import { preferenceBootstrap } from "../src/bootstrap.mjs";
import { createBrowserStorage } from "../src/storage.mjs";
import { detectPerformance } from "../src/performance.mjs";
import { initPointerScene } from "../src/pointer.mjs";

test("storage-free bootstrap contains no storage API", () => {
  const source = preferenceBootstrap();
  assert.doesNotMatch(source, /localStorage|sessionStorage/);
  assert.match(source, /dataset\.motion/);
  assert.match(source, /classList\.add\("js"\)/);
});

test("bootstrap can read opt-in preference keys", () => {
  const source = preferenceBootstrap({ themeKey: "theme", motionKey: "motion" });
  assert.match(source, /localStorage\.getItem\(k\.themeKey\)/);
  assert.match(source, /localStorage\.getItem\(k\.motionKey\)/);
});

test("browser storage adapter fails closed when storage is blocked", () => {
  const original = globalThis.localStorage;
  Object.defineProperty(globalThis, "localStorage", {
    configurable: true,
    get() { throw new Error("blocked"); },
  });
  const store = createBrowserStorage();
  assert.equal(store.get("theme"), null);
  assert.doesNotThrow(() => store.set("theme", "light"));
  assert.doesNotThrow(() => store.remove("theme"));
  Object.defineProperty(globalThis, "localStorage", { configurable: true, value: original });
});

test("performance hints select lite mode without a frame probe", () => {
  const root = { dataset: { motion: "full" } };
  detectPerformance({
    root,
    navigator: { deviceMemory: 2 },
    setTimer: () => assert.fail("low-memory devices must not schedule a probe"),
  });
  assert.equal(root.dataset.perf, "lite");
});

test("pointer scene coalesces updates and resets tilt", () => {
  const listeners = new Map();
  const style = () => {
    const values = new Map();
    return {
      values,
      setProperty: (key, value) => values.set(key, value),
      removeProperty: (key) => values.delete(key),
    };
  };
  const classList = {
    values: new Set(),
    add(value) { this.values.add(value); },
    remove(value) { this.values.delete(value); },
  };
  const area = {
    style: style(),
    classList,
    addEventListener: (name, callback) => listeners.set(name, callback),
    removeEventListener: (name, callback) => listeners.delete(name),
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 200, height: 100 }),
  };
  const spotlight = { style: style(), classList };
  const tilt = { style: style() };
  const root = { dataset: { motion: "full" } };
  const frames = [];
  const cleanup = initPointerScene({
    area,
    spotlight,
    tilt,
    root,
    finePointer: { matches: true },
    MutationObserver: null,
    requestFrame: (callback) => { frames.push(callback); return frames.length; },
    cancelFrame: () => {},
  });
  listeners.get("pointermove")({ clientX: 150, clientY: 25 });
  listeners.get("pointermove")({ clientX: 100, clientY: 50 });
  assert.equal(frames.length, 1);
  frames.shift()();
  assert.equal(spotlight.style.values.get("--lx"), "50%");
  assert.equal(spotlight.style.values.get("--ly"), "50%");
  assert.equal(tilt.style.values.get("--tilt-x"), "0deg");
  assert.equal(tilt.style.values.get("--tilt-y"), "0deg");
  listeners.get("pointerleave")();
  assert.equal(classList.values.has("is-lit"), false);
  assert.equal(tilt.style.values.has("--tilt-x"), false);
  cleanup();
});
