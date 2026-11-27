const noop = () => {};

const visible = (element) => element?.classList.add("is-revealed");

export function observeReveals(elements = [], { Observer = globalThis.IntersectionObserver } = {}) {
  const targets = [...elements].filter(Boolean);
  if (!targets.length) return noop;
  if (!Observer) {
    targets.forEach(visible);
    return noop;
  }

  const observer = new Observer(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        visible(entry.target);
        observer.unobserve(entry.target);
      }
    },
    { rootMargin: "0px 0px -8% 0px", threshold: 0.1 },
  );
  targets.forEach((element) => observer.observe(element));
  return () => observer.disconnect();
}

export function pauseOffscreen(elements = [], { Observer = globalThis.IntersectionObserver } = {}) {
  const targets = [...elements].filter(Boolean);
  if (!targets.length || !Observer) return noop;

  const observer = new Observer(
    (entries) => {
      for (const entry of entries) {
        entry.target.classList.toggle("is-offscreen", !entry.isIntersecting);
      }
    },
    { rootMargin: "120px 0px" },
  );
  targets.forEach((element) => observer.observe(element));
  return () => observer.disconnect();
}

export function trackHeader(header, { document = globalThis.document, Observer = globalThis.IntersectionObserver } = {}) {
  if (!header || !document?.body || !Observer) return noop;

  const sentinel = document.createElement("div");
  sentinel.setAttribute("aria-hidden", "true");
  document.body.prepend(sentinel);
  const observer = new Observer(([entry]) => {
    if (entry) header.classList.toggle("is-scrolled", !entry.isIntersecting);
  });
  observer.observe(sentinel);

  return () => {
    observer.disconnect();
    sentinel.remove();
  };
}

export function trackSections(links = [], { document = globalThis.document, Observer = globalThis.IntersectionObserver } = {}) {
  const byId = new Map();
  for (const link of links.filter(Boolean)) {
    const id = link.hash?.slice(1);
    if (!id) continue;
    const matches = byId.get(id) ?? [];
    matches.push(link);
    byId.set(id, matches);
  }
  if (!byId.size || !document || !Observer) return noop;

  const observer = new Observer(
    (entries) => {
      for (const entry of entries) {
        const matches = byId.get(entry.target.id) ?? [];
        for (const link of matches) {
          if (entry.isIntersecting) link.setAttribute("aria-current", "true");
          else link.removeAttribute("aria-current");
        }
      }
    },
    { rootMargin: "-45% 0px -50% 0px" },
  );

  for (const id of byId.keys()) {
    const section = document.getElementById(id);
    if (section) observer.observe(section);
  }

  return () => observer.disconnect();
}
