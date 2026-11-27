export function preferenceBootstrap({ themeKey = null, motionKey = null } = {}) {
  const config = JSON.stringify({ themeKey, motionKey }).replaceAll("<", "\\u003c");
  const readPreferences = themeKey || motionKey
    ? `let t=null,m=null;try{${themeKey ? "t=localStorage.getItem(k.themeKey);" : ""}${motionKey ? "m=localStorage.getItem(k.motionKey);" : ""}}catch{}`
    : "const t=null,m=null;";

  return `(()=>{const r=document.documentElement,k=${config};${readPreferences}r.classList.add("js");if(t==="light"||t==="dark")r.dataset.theme=t;r.dataset.motion=m==="full"||m==="reduced"?m:matchMedia("(prefers-reduced-motion: reduce)").matches?"reduced":"full"})()`;
}
