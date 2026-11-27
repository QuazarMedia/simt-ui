const PROBE_MS = 1000;
const MIN_FPS = 40;
const noop = () => {};

export function detectPerformance(options = {}) {
  const root = options.root ?? globalThis.document?.documentElement;
  const document = options.document ?? globalThis.document;
  const navigator = options.navigator ?? globalThis.navigator ?? {};
  if (!root) return noop;

  const { connection, deviceMemory = 8, hardwareConcurrency = 8 } = navigator;
  if (connection?.saveData || deviceMemory <= 2 || hardwareConcurrency <= 2) {
    root.dataset.perf = "lite";
    return noop;
  }

  const requestFrame = options.requestFrame ?? globalThis.requestAnimationFrame?.bind(globalThis);
  const setTimer = options.setTimer ?? globalThis.setTimeout?.bind(globalThis);
  const addListener = options.addListener ?? globalThis.addEventListener?.bind(globalThis);
  if (!document || !requestFrame || !setTimer) return noop;

  let active = true;
  let timeout = 0;
  const probe = () => {
    if (!active || document.visibilityState !== "visible" || root.dataset.motion !== "full") return;
    let frames = 0;
    let start = null;
    const tick = (now) => {
      if (!active) return;
      start ??= now;
      frames += 1;
      if (now - start < PROBE_MS) {
        requestFrame(tick);
      } else if ((frames * 1000) / (now - start) < MIN_FPS) {
        root.dataset.perf = "lite";
      }
    };
    requestFrame(tick);
  };
  const schedule = () => {
    timeout = setTimer(probe, 400);
  };

  if (document.readyState === "complete") schedule();
  else addListener?.("load", schedule, { once: true });

  return () => {
    active = false;
    if (timeout) clearTimeout(timeout);
  };
}
