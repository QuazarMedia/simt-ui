export function initLocaleSelect(control, { location = globalThis.location } = {}) {
  if (!control) return () => {};
  const change = () => {
    const target = control.value;
    if (target !== "" && target !== location.pathname) location.assign(target);
  };
  control.addEventListener("change", change);
  return () => control.removeEventListener("change", change);
}
