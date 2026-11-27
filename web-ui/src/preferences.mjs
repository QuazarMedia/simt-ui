const noop = () => {};

const read = (storage, key) => {
  if (!storage || !key) return null;
  try {
    return storage.get(key);
  } catch {
    return null;
  }
};

const write = (storage, key, value) => {
  if (!storage || !key) return;
  try {
    if (value === null) storage.remove(key);
    else storage.set(key, value);
  } catch {}
};

function bindToggle({ button, values, key, media, systemValue, current, apply, label, pressed, followSystem, storage }) {
  const system = () => systemValue(media.matches);
  const fallbackLabel = button.getAttribute("aria-label");
  const sync = () => {
    const value = current();
    button.dataset.state = value;
    button.setAttribute("aria-label", label(value) ?? fallbackLabel ?? "");
    button.setAttribute("aria-pressed", String(value === pressed));
  };
  const onClick = () => {
    const next = current() === values[0] ? values[1] : values[0];
    const follows = followSystem && next === system();
    write(storage, key, follows ? null : next);
    apply(next, follows);
    sync();
  };
  const onMediaChange = () => {
    if (followSystem && read(storage, key) === null) apply(system(), true);
    sync();
  };

  button.dataset.values = values.join(" ");
  button.addEventListener("click", onClick);
  media.addEventListener?.("change", onMediaChange);
  sync();

  return () => {
    button.removeEventListener("click", onClick);
    media.removeEventListener?.("change", onMediaChange);
  };
}

function syncThemeColor(document, theme, followsSystem) {
  for (const meta of document.querySelectorAll('meta[name="theme-color"]')) {
    meta.dataset.color ??= meta.content;
    const chosen = document.querySelector(`meta[name="theme-color"][media*="${theme}"]`)?.dataset.color;
    meta.content = followsSystem || !chosen ? meta.dataset.color : chosen;
  }
}

export function initTheme(button, options = {}) {
  if (!button) return noop;
  const root = options.root ?? globalThis.document?.documentElement;
  const document = options.document ?? globalThis.document;
  const messages = options.messages ?? {};
  const media = options.media ?? globalThis.matchMedia?.("(prefers-color-scheme: light)") ?? { matches: false };
  const followSystem = options.followSystem ?? true;
  const defaultTheme = options.defaultTheme ?? "dark";
  const systemValue = (light) => (light ? "light" : "dark");
  const current = () => root.dataset.theme || (followSystem ? systemValue(media.matches) : defaultTheme);

  return bindToggle({
    button,
    values: ["dark", "light"],
    key: options.key ?? null,
    media,
    systemValue,
    current,
    followSystem,
    storage: options.storage ?? null,
    pressed: "light",
    label: (theme) => (theme === "light" ? messages.themeToDark : messages.themeToLight),
    apply: (theme, follows) => {
      if (follows) delete root.dataset.theme;
      else root.dataset.theme = theme;
      if (document) syncThemeColor(document, theme, follows);
    },
  });
}

export function initMotion(button, options = {}) {
  if (!button) return noop;
  const root = options.root ?? globalThis.document?.documentElement;
  const messages = options.messages ?? {};
  const media = options.media ?? globalThis.matchMedia?.("(prefers-reduced-motion: reduce)") ?? { matches: false };
  const systemValue = (reduce) => (reduce ? "reduced" : "full");

  return bindToggle({
    button,
    values: ["full", "reduced"],
    key: options.key ?? null,
    media,
    systemValue,
    current: () => root.dataset.motion || systemValue(media.matches),
    followSystem: options.followSystem ?? true,
    storage: options.storage ?? null,
    pressed: "reduced",
    label: (motion) => (motion === "full" ? messages.motionPause : messages.motionPlay),
    apply: (motion) => {
      root.dataset.motion = motion;
    },
  });
}
