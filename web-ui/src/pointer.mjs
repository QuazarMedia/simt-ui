const noop = () => {};
const clamp = (value) => Math.min(1, Math.max(0, value));

export function initPointerScene(options = {}) {
  const area = options.area;
  if (!area) return noop;
  const root = options.root ?? globalThis.document?.documentElement;
  const bounds = options.bounds ?? area;
  const spotlight = options.spotlight ?? area;
  const tilt = options.tilt ?? null;
  const maxTilt = options.maxTilt ?? 6;
  const finePointer = options.finePointer ?? globalThis.matchMedia?.("(hover: hover) and (pointer: fine)");
  const requestFrame = options.requestFrame ?? globalThis.requestAnimationFrame?.bind(globalThis);
  const cancelFrame = options.cancelFrame ?? globalThis.cancelAnimationFrame?.bind(globalThis);
  const MutationObserver = options.MutationObserver ?? globalThis.MutationObserver;
  let frame = 0;
  let x = 0.5;
  let y = 0.5;

  const enabled = () => Boolean(finePointer?.matches && root?.dataset.motion === "full" && root.dataset.perf !== "lite");
  const render = () => {
    frame = 0;
    if (!enabled()) return;
    spotlight.style.setProperty("--lx", `${x * 100}%`);
    spotlight.style.setProperty("--ly", `${y * 100}%`);
    tilt?.style.setProperty("--tilt-x", `${(0.5 - y) * maxTilt}deg`);
    tilt?.style.setProperty("--tilt-y", `${(x - 0.5) * maxTilt}deg`);
  };
  const reset = () => {
    if (frame && cancelFrame) cancelFrame(frame);
    frame = 0;
    spotlight.classList.remove("is-lit");
    spotlight.style.removeProperty("--lx");
    spotlight.style.removeProperty("--ly");
    tilt?.style.removeProperty("--tilt-x");
    tilt?.style.removeProperty("--tilt-y");
  };
  const onMove = (event) => {
    if (!enabled()) return;
    const box = bounds.getBoundingClientRect();
    if (!box.width || !box.height) return;
    x = clamp((event.clientX - box.left) / box.width);
    y = clamp((event.clientY - box.top) / box.height);
    spotlight.classList.add("is-lit");
    if (requestFrame) frame ||= requestFrame(render);
    else render();
  };

  area.addEventListener("pointermove", onMove);
  area.addEventListener("pointerleave", reset);
  const observer = root && MutationObserver
    ? new MutationObserver(() => {
        if (!enabled()) reset();
      })
    : null;
  observer?.observe(root, { attributes: true, attributeFilter: ["data-motion", "data-perf"] });

  return () => {
    area.removeEventListener("pointermove", onMove);
    area.removeEventListener("pointerleave", reset);
    observer?.disconnect();
    reset();
  };
}
