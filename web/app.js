(function () {
  const ICONS = {
    power: "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83z",
    home: "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
    servers: "M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z",
    settings: "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 00.12-.61l-1.92-3.32a.488.488 0 00-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 00-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 00-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
    add: "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
    down: "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z",
    up: "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z",
    clock: "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z",
    del: "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
    paste: "M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z",
    bolt: "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z",
    chevron: "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z",
    refresh: "M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z",
    edit: "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 000-1.41l-2.34-2.34a.996.996 0 00-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
    cpu: "M15 9H9v6h6V9zm-2 4h-2v-2h2v2zm8-2V9h-2V7c0-1.1-.9-2-2-2h-2V3h-2v2h-2V3H9v2H7c-1.1 0-2 .9-2 2v2H3v2h2v2H3v2h2v2c0 1.1.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-.9 2-2v-2h2v-2h-2v-2h2zm-4 6H7V7h10v10z",
    download: "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
    check: "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
    info: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
    close: "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
  };

  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));

  function h(tag, attrs, ...kids) {
    const el = document.createElement(tag);
    for (const k in attrs || {}) {
      const v = attrs[k];
      if (v === false || v == null) continue;
      if (k === "class") el.className = v;
      else if (k === "html") el.innerHTML = v;
      else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
      else el.setAttribute(k, v === true ? "" : v);
    }
    for (const kid of kids.flat(Infinity)) {
      if (kid == null || kid === false) continue;
      el.append(kid.nodeType ? kid : document.createTextNode(kid));
    }
    return el;
  }

  function icon(name, cls) {
    const s = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    s.setAttribute("viewBox", "0 0 24 24");
    s.setAttribute("class", cls || "i");
    const p = document.createElementNS("http://www.w3.org/2000/svg", "path");
    p.setAttribute("d", ICONS[name]);
    s.append(p);
    return s;
  }

  let S = null;
  const refreshers = [];

  function fmtBytes(n) {
    n = Number(n) || 0;
    if (n < 1024) return Math.round(n) + " Б";
    const u = ["КБ", "МБ", "ГБ", "ТБ"];
    let i = -1;
    do { n /= 1024; i++; } while (n >= 1024 && i < u.length - 1);
    return (n >= 100 ? n.toFixed(0) : n >= 10 ? n.toFixed(1) : n.toFixed(2)) + " " + u[i];
  }
  function fmtTime(s) {
    s = Math.max(0, Math.floor(s));
    const hh = Math.floor(s / 3600), mm = Math.floor((s % 3600) / 60), ss = s % 60;
    const p = (x) => String(x).padStart(2, "0");
    return (hh ? hh + ":" : "") + p(mm) + ":" + p(ss);
  }
  function selectedServer() {
    return S.servers.find((x) => x.id === S.selected) || null;
  }

  const DEFAULTS = {
    theme: 0, dynamic: false, amoled: false, tint: true,
    accent: { r: 64, g: 216, b: 208 },
    anim: { all: true, pages: true, controls: true, sheets: true, enabled: true, speed: 1, intensity: 1, tempo: 1, burst: true, waves: 3, idle: true, iconFx: true, press: true, haptics: true, glow: 1 },
    button: { size: 224, lobes: 10, shape: 0 },
    textScale: 1, autoCheck: true, checkInterval: 60, updates: { auto: true },
    autoConnect: false, dns: 0, dnsCustom: "", mtu: 1500,
    pingMode: 0, pingTimeout: 1, autoPing: false, sortPing: true,
    tuning: { ipv6: true, log: 2, sniff: false, lan: false, quic: false, ds: 0, am: 0, apps: "", unm: false, rc: true, rt: 3, rd: 2, idle: 300, buf: 0, mux: false, muxc: 8 },
  };
  const PRESETS = [
    ["Бирюза", 64, 216, 208], ["Синий", 92, 150, 255], ["Фиолетовый", 178, 140, 255], ["Розовый", 255, 125, 170],
    ["Красный", 255, 100, 90], ["Оранжевый", 255, 160, 60], ["Зелёный", 110, 220, 120], ["Белый", 255, 255, 255],
  ];

  function merge(base, over) {
    if (over === undefined || over === null) return base;
    if (typeof base !== "object" || base === null || Array.isArray(base)) return over;
    const out = {};
    for (const k in base) out[k] = merge(base[k], over[k]);
    for (const k in over) if (!(k in out)) out[k] = over[k];
    return out;
  }

  function toLin(c) { c /= 255; return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); }
  function rgbToOklch(r, g, b) {
    const R = toLin(r), G = toLin(g), B = toLin(b);
    const l = Math.cbrt(0.4122214708 * R + 0.5363325363 * G + 0.0514459929 * B);
    const m = Math.cbrt(0.2119034982 * R + 0.6806995451 * G + 0.1073969566 * B);
    const s = Math.cbrt(0.0883024619 * R + 0.2817188376 * G + 0.6299787005 * B);
    const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
    const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
    const bb = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
    return [L, Math.hypot(A, bb), (Math.atan2(bb, A) * 180 / Math.PI + 360) % 360];
  }
  function hex2(n) { return clamp(Math.round(n), 0, 255).toString(16).padStart(2, "0"); }
  function toHex(c) { return "#" + hex2(c.r) + hex2(c.g) + hex2(c.b); }
  function parseHex(t) {
    const m = /^#?([0-9a-f]{6})$/i.exec(String(t).trim());
    if (!m) return null;
    const n = parseInt(m[1], 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  }

  const THEME_KEYS = ["--primary", "--on-primary", "--primary-container", "--on-primary-container", "--secondary-container", "--on-secondary-container", "--tertiary",
    "--surface", "--sc-low", "--sc", "--sc-high", "--sc-highest", "--on-surface", "--on-surface-variant", "--outline", "--outline-variant", "--accent-text"];

  let themeKey = "", barsDark = null;
  function applyTheme() {
    const st = S.settings;
    const key = JSON.stringify([st.theme, st.dynamic, st.amoled, st.tint, st.accent, S.dark, S.colors]);
    if (key === themeKey) return;
    themeKey = key;
    const sysDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    const dark = st.theme === 1 ? false : st.theme === 2 ? true : (S.dark !== undefined ? S.dark : sysDark);
    document.body.classList.toggle("light", !dark);
    if (barsDark !== dark) { barsDark = dark; Bridge.bars(dark); }
    document.body.classList.toggle("amoled", dark && !!st.amoled);
    const root = document.documentElement.style;
    const set = (k, v) => root.setProperty(k, v);
    if (st.dynamic && S.colors && S.colors.length === 8) {
      THEME_KEYS.forEach((k) => root.removeProperty(k));
      const c = S.colors.map((x) => "#" + (x & 0xffffff).toString(16).padStart(6, "0"));
      set("--primary", c[0]); set("--on-primary", c[1]); set("--primary-container", c[2]); set("--on-primary-container", c[3]);
      set("--secondary-container", c[4]); set("--on-secondary-container", c[5]); set("--tertiary", c[6]);
      return;
    }
    const acc = st.accent;
    const hex = toHex(acc);
    const [L, C, H] = rgbToOklch(acc.r, acc.g, acc.b);
    const grey = C < 0.025;
    const tc = grey ? 0 : st.tint ? clamp(C * 0.1, 0.004, 0.022) : 0.004;
    const o = (l, c) => "oklch(" + l + " " + c + " " + H.toFixed(1) + ")";
    set("--primary", hex);
    set("--on-primary", L > 0.62 ? "oklch(.2 0 0)" : "oklch(.99 0 0)");
    if (dark) {
      set("--surface", o(0.17, tc)); set("--sc-low", o(0.2, tc)); set("--sc", o(0.225, tc * 1.1)); set("--sc-high", o(0.255, tc * 1.2)); set("--sc-highest", o(0.285, tc * 1.3));
      set("--on-surface", o(0.92, tc)); set("--on-surface-variant", o(0.77, tc * 1.5)); set("--outline", o(0.58, tc * 1.5)); set("--outline-variant", o(0.38, tc * 1.5));
      set("--primary-container", "color-mix(in oklab, " + hex + " 30%, " + o(0.2, tc) + ")");
      set("--on-primary-container", grey ? "oklch(.94 0 0)" : o(0.92, Math.min(C, 0.05)));
      set("--secondary-container", "color-mix(in oklab, " + hex + " 16%, " + o(0.27, tc) + ")");
      set("--on-secondary-container", o(0.92, tc));
      set("--accent-text", "oklch(" + Math.max(L, 0.74).toFixed(3) + " " + (grey ? 0 : C.toFixed(3)) + " " + H.toFixed(1) + ")");
    } else {
      set("--surface", o(0.985, tc)); set("--sc-low", o(0.968, tc)); set("--sc", o(0.955, tc * 1.1)); set("--sc-high", o(0.94, tc * 1.2)); set("--sc-highest", o(0.92, tc * 1.3));
      set("--on-surface", o(0.22, tc)); set("--on-surface-variant", o(0.42, tc * 1.5)); set("--outline", o(0.55, tc * 1.5)); set("--outline-variant", o(0.8, tc * 1.5));
      set("--primary-container", "color-mix(in oklab, " + hex + " 28%, white)");
      set("--on-primary-container", o(0.25, Math.min(C, 0.08)));
      set("--secondary-container", "color-mix(in oklab, " + hex + " 16%, white)");
      set("--on-secondary-container", o(0.25, tc));
      set("--accent-text", "oklch(" + Math.min(L, 0.5).toFixed(3) + " " + (grey ? 0 : C.toFixed(3)) + " " + H.toFixed(1) + ")");
    }
    set("--tertiary", "oklch(" + (dark ? 0.8 : 0.5) + " " + (grey ? 0 : 0.1) + " " + ((H + 60) % 360).toFixed(1) + ")");
  }

  let motionKey = "";
  function applyMotionFlags() {
    const a = S.settings.anim;
    const mk = JSON.stringify([a, S.settings.button, S.settings.textScale]);
    if (mk === motionKey) return;
    motionKey = mk;
    const b = document.body.classList;
    b.toggle("no-motion", !a.all);
    b.toggle("no-pages", !a.pages);
    b.toggle("no-controls", !a.controls);
    b.toggle("no-sheets", !a.sheets);
    b.toggle("no-burst", !a.burst || !a.enabled || !a.all);
    const root = document.documentElement.style;
    root.setProperty("--ts", String(clamp(Number(S.settings.textScale) || 1, 0.7, 1.6)));
    root.setProperty("--tempo", String(clamp(a.tempo, 0.2, 3)));
    root.setProperty("--k", String(1 / clamp(a.speed, 0.25, 3)));
    root.setProperty("--glow", String(a.all ? clamp(a.glow, 0, 1) : clamp(a.glow, 0, 1)));
    root.setProperty("--waves", String(clamp(Math.round(a.waves), 1, 5)));
    root.setProperty("--power", clamp(S.settings.button.size, 160, 280) + "px");
    $$(".ring").forEach((r, i) => r.classList.toggle("used", i < clamp(Math.round(a.waves), 1, 5)));
  }

  const Power = (() => {
    const POINTS = 96;
    const V = { on: 0, busy: 0, press: 0, phase: 0, idle: 0, t: 0 };
    let hold = 0, shape, shape2, icon_, iconLit, iconWrap, root, raf = 0, last = 0, pressing = false;

    function build() {
      const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
      svg.setAttribute("class", "shape");
      svg.setAttribute("viewBox", "-112 -112 224 224");
      const NS = "http://www.w3.org/2000/svg";
      shape = document.createElementNS(NS, "path");
      shape.setAttribute("class", "fill-base");
      shape2 = document.createElementNS(NS, "path");
      shape2.setAttribute("class", "fill-on");
      svg.append(shape, shape2);
      const mk = (cls) => {
        const ic = document.createElementNS(NS, "svg");
        ic.setAttribute("class", "icon " + cls);
        ic.setAttribute("viewBox", "0 0 24 24");
        const ip = document.createElementNS(NS, "path");
        ip.setAttribute("d", ICONS.power);
        ic.append(ip);
        return ic;
      };
      icon_ = mk("base");
      iconLit = mk("lit");
      const wrap = h("div", { class: "icon-wrap", style: "position:relative;width:84px;height:84px" }, icon_, iconLit);
      iconWrap = wrap;
      root = h("button", { class: "power", "aria-label": "Подключить или отключить" }, svg, wrap);
      const down = () => { pressing = true; kick(); };
      const up = () => { pressing = false; kick(); };
      root.addEventListener("pointerdown", down);
      root.addEventListener("pointerup", up);
      root.addEventListener("pointercancel", up);
      root.addEventListener("pointerleave", up);
      root.addEventListener("click", toggle);
      return root;
    }

    function cfg() { return S.settings.anim; }

    function target() {
      const s = S.status;
      return { on: s === "On" ? 1 : 0, busy: s === "Connecting" || s === "Stopping" ? 1 : 0 };
    }

    function ease(cur, to, rate, dt) { return cur + (to - cur) * (1 - Math.exp(-dt * rate)); }

    function live() { const c = cfg(); return c.all && c.enabled; }

    function draw() {
      const c = cfg();
      const bt = S.settings.button;
      const on = live();
      const k = on ? clamp(c.intensity, 0, 2) : 0;
      const idleAmp = c.idle && on ? 0.03 * (1 - V.on) : 0;
      const amp = (idleAmp + V.on * 0.07 + V.busy * 0.04) * k;
      const R = 92 * (1 - 0.06 * V.press + 0.012 * V.on * k * Math.sin(V.t * 1.6 * c.tempo));
      const N = clamp(Math.round(bt.lobes), 3, 16);
      const ph = V.phase * Math.PI * 2;
      const style = bt.shape;
      const ex = 4 - 1.6 * clamp(V.on, 0, 1);
      let d = "";
      for (let i = 0; i < POINTS; i++) {
        const a = (i / POINTS) * Math.PI * 2;
        let r;
        if (style === 1) r = R * (1 + amp * 0.35 * Math.cos(2 * a + ph));
        else if (style === 2) {
          const q = a - ph * 0.25;
          const sq = 0.9 / Math.pow(Math.pow(Math.abs(Math.cos(q)), ex) + Math.pow(Math.abs(Math.sin(q)), ex), 1 / ex);
          r = R * sq * (1 + amp * 0.4 * Math.cos(N * a + ph));
        } else if (style === 3) r = R * (1 + amp * 1.5 * (2 * Math.abs(Math.cos(N * a / 2 + ph / 2)) - 1));
        else r = R * (1 + amp * Math.cos(N * a + ph));
        d += (i ? "L" : "M") + (Math.cos(a) * r).toFixed(2) + " " + (Math.sin(a) * r).toFixed(2);
      }
      d += "Z";
      shape.setAttribute("d", d);
      shape2.setAttribute("d", d);
      const mix = clamp(V.on, 0, 1);
      shape2.style.opacity = mix.toFixed(3);
      iconLit.style.opacity = mix.toFixed(3);
      icon_.style.opacity = (1 - mix).toFixed(3);
      let scale = 1, rot = 0;
      if (on && c.iconFx) {
        scale = 1 - 0.07 * V.busy + 0.05 * V.on;
        rot = Math.sin(V.t * 5 * c.tempo) * 7 * V.busy;
      }
      iconWrap.style.transform = "rotate(" + rot.toFixed(2) + "deg) scale(" + scale.toFixed(3) + ")";
    }

    function frame(now) {
      raf = 0;
      if (hold > now) { raf = requestAnimationFrame(frame); last = now; return; }
      const dt = clamp((now - last) / 1000 || 0.016, 0.001, 0.05);
      last = now;
      const c = cfg();
      const tg = target();
      if (live()) {
        V.on = ease(V.on, tg.on, 7, dt);
        V.busy = ease(V.busy, tg.busy, 9, dt);
        V.press = ease(V.press, pressing && c.press ? 1 : 0, 20, dt);
      } else {
        V.on = tg.on;
        V.busy = 0;
        V.press = 0;
      }
      V.t += dt;
      const speed = live() ? ((c.idle ? 0.08 : 0) * (1 - V.on) + V.on * 0.35 + V.busy * 1.6) * c.tempo : 0;
      V.phase = (V.phase + dt * speed) % 1;
      draw();
      const settled = Math.abs(V.on - tg.on) < 0.002 && Math.abs(V.busy - tg.busy) < 0.002 && V.press < 0.002 && !pressing
        && !(live() && (V.on > 0.5 || (c.idle && tg.on === 0) || tg.busy));
      if (!settled && !document.hidden) raf = requestAnimationFrame(frame);
    }

    function kick() {
      if (!raf) { last = performance.now(); raf = requestAnimationFrame(frame); }
    }

    function toggle() {
      if (cfg().haptics) Bridge.act("haptic", S.status === "Off" ? "strong" : "light");
      if (S.status === "Off") {
        if (!S.selected) { snack("Сначала добавьте сервер"); go("servers"); return; }
        S.status = "Connecting";
        updateHome();
        Bridge.act("connect").then(() => setTimeout(syncNow, 700));
      } else if (S.status === "On" || S.status === "Connecting") {
        S.status = "Stopping";
        updateHome();
        Bridge.act("disconnect").then(() => setTimeout(syncNow, 700));
      }
    }

    document.addEventListener("visibilitychange", () => { if (!document.hidden) kick(); });
    function pause(ms) { hold = performance.now() + ms; }
    return { build, kick, pause };
  })();

  function snack(text) {
    const el = $("#snack");
    el.textContent = text;
    el.classList.add("show");
    clearTimeout(snack.t);
    snack.t = setTimeout(() => el.classList.remove("show"), 2600);
  }

  let current = "home";
  const order = ["home", "servers", "settings"];
  function go(name) {
    if (name === current) return;
    const from = order.indexOf(current), to = order.indexOf(name);
    $$(".view").forEach((v) => {
      const idx = order.indexOf(v.dataset.view);
      v.classList.toggle("active", v.dataset.view === name);
      v.classList.toggle("left", idx < to);
    });
    $$(".nav-item").forEach((n) => n.classList.toggle("active", n.dataset.go === name));
    current = name;
    Power.pause(380);
    $("#fab") && $("#fab").classList.toggle("hide", name !== "servers");
    if (name === "home") Power.kick();
    void from;
  }

  async function syncNow() {
    try { onState(await Bridge.state()); } catch (e) {}
  }

  const home = {};
  function buildHome() {
    const v = $("#view-home");
    home.word = h("span", { class: "status-word" }, "Отключено");
    home.sub = h("div", { class: "body sub" }, "");
    home.card = h("button", { class: "chip-card", onclick: () => go("servers") });
    home.avatar = h("div", { class: "avatar" }, "");
    home.name = h("div", { class: "name" }, "");
    home.meta = h("div", { class: "body" }, "");
    home.card.append(home.avatar, h("div", { class: "grow" }, home.name, home.meta), icon("chevron"));
    home.svc = h("div", { class: "svc-card" });
    home.svcSig = "";
    home.down = h("b", {}, "0 Б/с");
    home.up = h("b", {}, "0 Б/с");
    home.time = h("b", {}, "00:00");
    const stat = (ic, label, b) => h("div", { class: "stat" }, h("div", { class: "label" }, icon(ic), label), b);
    v.append(
      h("div", { class: "home-head" }, h("h1", { class: "headline" }, home.word), home.sub),
      h("div", { class: "stage" }, h("div", { class: "rings" }, [0, 1, 2, 3, 4].map((n) => h("i", { class: "ring", style: "--i:" + n }))), Power.build()),
      home.card,
      home.svc,
      h("div", { class: "stats" }, stat("down", "Загрузка", home.down), stat("up", "Отдача", home.up), stat("clock", "В сети", home.time))
    );
  }

  const WORDS = { Off: "Отключено", Connecting: "Подключаю…", On: "Подключено", Stopping: "Отключаю…" };
  function updateHome() {
    const sv = selectedServer();
    document.body.classList.toggle("on", S.status === "On");
    const word = WORDS[S.status] || "";
    if (home.word.textContent !== word) {
      home.word.classList.add("swap");
      setTimeout(() => { home.word.textContent = word; home.word.classList.remove("swap"); }, 130);
    }
    if (S.error) home.sub.textContent = S.error;
    else if (!sv) home.sub.textContent = "Добавьте сервер, чтобы начать";
    else if (S.status === "On") home.sub.textContent = "Защищённое соединение через " + sv.name;
    else home.sub.textContent = "Нажмите, чтобы подключиться к " + sv.name;
    home.avatar.textContent = sv ? Array.from(sv.name)[0].toUpperCase() : "+";
    home.name.textContent = sv ? sv.name : "Сервер не выбран";
    home.meta.textContent = sv ? sv.proto + " · " + sv.endpoint : "Нажмите, чтобы добавить";
    home.down.textContent = fmtBytes(S.down) + "/с";
    home.up.textContent = fmtBytes(S.up) + "/с";
    home.time.textContent = fmtTime(S.uptime);
    renderSvc();
    Power.kick();
  }

  const SVC_COLORS = ["#5865F2", "#27A7E7", "#FF0033"];
  const SVC_LETTERS = ["D", "T", "Y"];
  function serverName(id) {
    const x = S.servers.find((q) => q.id === id);
    return x ? x.name : "";
  }
  function renderSvc() {
    const sv = S.selected;
    const sig = JSON.stringify([S.services, sv, S.servers.length]);
    if (sig === home.svcSig) return;
    home.svcSig = sig;
    const checking = !!S.services.checking;
    const c = sv ? checkOf(sv) : { ms: [null, null, null], busy: false };
    const btn = h("button", { class: "btn tonal sm" + (checking ? " busy" : ""), onclick: () => { if (!S.servers.length) { snack("Сначала добавьте сервер"); return; } Bridge.act("checkServices"); } }, icon(checking ? "refresh" : "bolt"), checking ? "Проверяю…" : "Проверить");
    const cells = S.services.names.map((n, i) => {
      const v = c.ms[i];
      const text = c.busy && v == null ? "…" : v == null ? "—" : v > 0 ? v + " мс" : "нет";
      const cls = v != null && v > 0 ? (v < 400 ? "ok" : "slow") : v != null ? "bad" : "";
      const bestId = S.services.best[i];
      const better = bestId && bestId !== sv;
      return h("div", { class: "svc-cell" },
        h("div", { class: "svc-logo", style: "background:" + SVC_COLORS[i] }, SVC_LETTERS[i]),
        h("div", { class: "svc-name" }, n),
        h("div", { class: "svc-ms " + cls }, text),
        better ? h("button", { class: "svc-best", onclick: () => Bridge.act("select", bestId) }, "лучший: " + serverName(bestId)) : h("div", { class: "svc-best none" }, bestId && bestId === sv ? "лучший" : ""));
    });
    home.svc.replaceChildren(h("div", { class: "svc-head" }, h("div", { class: "title" }, "Сервисы"), btn), h("div", { class: "svc-grid" }, cells));
  }

  function pingOf(id) {
    const v = S.pings ? S.pings[id] : null;
    if (v && typeof v === "object") return { ms: v.ms == null ? null : v.ms, busy: !!v.busy };
    return { ms: v == null ? null : v, busy: false };
  }

  function pingChip(id) {
    const p = pingOf(id);
    if (p.busy) return h("span", { class: "ping" }, "…");
    if (p.ms == null || p.ms <= 0) return h("span", { class: "ping" }, "—");
    const c = p.ms < 150 ? "var(--good)" : p.ms < 350 ? "var(--warn)" : "var(--error)";
    return h("span", { class: "ping", style: "--c:" + c }, p.ms + " мс");
  }

  function sortKey(id) {
    const p = pingOf(id);
    return p.ms == null || p.ms <= 0 ? 1e9 : p.ms;
  }

  function groupUrl(name) {
    const g = (S.groups || []).find((x) => x.name === name);
    return g ? g.url : "";
  }

  const srv = { local: false, sig: "" };
  function pingBusy() { return srv.local || Object.keys(S.pings || {}).some((k) => pingOf(k).busy); }
  function checkOf(id) {
    const c = S.services && S.services.checks ? S.services.checks[id] : null;
    return c || { ms: [null, null, null], busy: false };
  }
  function dotsFor(id) {
    const c = checkOf(id);
    const names = S.services.names;
    return h("span", { class: "dots", title: names.join(", ") }, [0, 1, 2].map((i) => {
      const v = c.ms[i];
      const cls = c.busy && v == null ? "busy" : v == null ? "" : v > 0 ? (v < 400 ? "ok" : "slow") : "bad";
      return h("i", { class: "dot " + cls });
    }));
  }
  function renameSheet(title, current, onSave) {
    const input = h("input", { class: "hex wide", value: current, spellcheck: "false", maxlength: 60 });
    const ok = h("button", { class: "btn filled", onclick: () => { const t = input.value.trim(); if (!t) return; closeSheet(); onSave(t); } }, "Сохранить");
    openSheet(h("div", {}, h("h2", {}, title), h("div", { style: "margin:16px 0" }, input), h("div", { class: "actions" }, h("button", { class: "btn text", onclick: closeSheet }, "Отмена"), ok)));
    setTimeout(() => input.focus(), 350);
  }
  function renderServers() {
    const sig = JSON.stringify([S.servers, S.groups, S.selected, S.pings, S.settings.sortPing, S.services, pingBusy()]);
    if (sig === srv.sig) return;
    srv.sig = sig;
    const v = $("#view-servers");
    const keep = v.scrollTop;
    v.replaceChildren();
    const busy = pingBusy();
    const pingBtn = h("button", { class: "btn tonal" + (busy ? " busy" : ""), onclick: async () => {
      if (pingBusy()) return;
      srv.local = true; srv.sig = ""; renderServers();
      await Bridge.act("ping");
      srv.local = false; srv.sig = ""; renderServers();
    } }, icon(busy ? "refresh" : "bolt"), busy ? "Проверяю…" : "Проверить пинг");
    const chk = !!S.services.checking;
    const svcBtn = h("button", { class: "btn tonal" + (chk ? " busy" : ""), onclick: () => { if (!S.services.checking && S.servers.length) Bridge.act("checkServices"); } }, icon(chk ? "refresh" : "check"), chk ? "Проверяю…" : "Сервисы");
    v.append(h("div", { class: "top-row two" }, h("h1", { class: "headline" }, "Серверы"), h("div", { class: "tools" }, svcBtn, pingBtn)));
    if (!S.servers.length) {
      v.append(h("div", { class: "empty" }, h("div", { class: "title-large" }, "Серверов пока нет"), h("div", { class: "body" }, "Вставьте ссылку vless://, trojan://, ss:// или адрес подписки")));
    }
    const groups = {};
    S.servers.forEach((s) => { (groups[s.group || ""] = groups[s.group || ""] || []).push(s); });
    Object.keys(groups).forEach((g) => {
      const list = groups[g].slice();
      if (S.settings.sortPing) list.sort((x, y) => sortKey(x.id) - sortKey(y.id));
      const actions = h("div", { class: "group-actions" },
        h("button", { class: "icon-btn sm", "aria-label": "Проверить пинг группы", onclick: () => { Bridge.act("pingGroup", g); } }, icon("bolt")),
        g ? h("button", { class: "icon-btn sm", "aria-label": "Переименовать группу", onclick: () => renameSheet("Название группы", g, async (t) => { const r = await Bridge.act("renameGroup", { old: g, name: t }); if (r && r.error) snack(r.error); }) }, icon("edit")) : null,
        g && groupUrl(g) ? h("button", { class: "icon-btn sm", "aria-label": "Обновить подписку", onclick: async () => { const r = await Bridge.act("refreshGroup", g); snack(r && r.error ? r.error : "Подписка обновлена"); } }, icon("refresh")) : null,
        g ? h("button", { class: "icon-btn sm", "aria-label": "Удалить группу", onclick: () => { Bridge.act("removeGroup", g); snack("Группа удалена"); } }, icon("del")) : null);
      v.append(h("div", { class: "group-title" }, h("span", {}, g || "Мои серверы"), actions));
      v.append(h("div", { class: "list" }, list.map((s) => h("button", { class: "row" + (s.id === S.selected ? " selected" : ""), onclick: () => Bridge.act("select", s.id) },
        h("div", { class: "avatar" }, Array.from(s.name)[0].toUpperCase()),
        h("div", { class: "grow" }, h("div", { class: "t" }, s.name), h("div", { class: "s" }, s.proto + " · " + s.endpoint)),
        h("div", { class: "side" }, pingChip(s.id), dotsFor(s.id)),
        h("span", { class: "icon-btn", role: "button", "aria-label": "Переименовать", onclick: (e) => { e.stopPropagation(); renameSheet("Название сервера", s.name, (t) => Bridge.act("renameServer", { id: s.id, name: t })); } }, icon("edit")),
        h("span", { class: "icon-btn", role: "button", "aria-label": "Удалить", onclick: (e) => { e.stopPropagation(); Bridge.act("remove", s.id); snack("Сервер удалён"); } }, icon("del"))
      ))));
    });
    v.scrollTop = keep;
  }

  let liveRender = null;
  function openLive(render) {
    openSheet(render());
    liveRender = render;
  }
  function refreshLive() {
    if (!liveRender || !$("#sheet").classList.contains("show")) return;
    const sh = $("#sheet");
    const top = sh.scrollTop;
    sh.replaceChildren(h("div", { class: "grab" }), liveRender());
    sh.scrollTop = top;
  }
  function openSheet(content) {
    liveRender = null;
    const sh = $("#sheet");
    sh.replaceChildren(h("div", { class: "grab" }), content);
    $("#scrim").classList.add("show");
    requestAnimationFrame(() => requestAnimationFrame(() => sh.classList.add("show")));
  }
  function closeSheet() {
    liveRender = null;
    $("#sheet").classList.remove("show");
    $("#scrim").classList.remove("show");
  }

  function addSheet() {
    const ta = h("textarea", { class: "field", placeholder: "vless://…  trojan://…  ss://…  https://подписка", spellcheck: "false" });
    const paste = h("button", { class: "btn text", onclick: async () => {
      const r = await Bridge.act("clipboard");
      if (r && r.text) ta.value = ta.value ? ta.value + "\n" + r.text : r.text;
    } }, icon("paste"), "Вставить");
    const ok = h("button", { class: "btn filled", onclick: async () => {
      const text = ta.value.trim();
      if (!text) return;
      const r = await Bridge.act("add", text);
      closeSheet();
      snack(r && r.error ? r.error : r && r.added ? "Добавлено: " + r.added : "Уже есть в списке");
    } }, "Добавить");
    openSheet(h("div", {}, h("h2", {}, "Добавить сервер"), h("div", { class: "body" }, "Одна или несколько ссылок, либо адрес подписки"), ta,
      h("div", { class: "actions" }, paste, h("button", { class: "btn text", onclick: closeSheet }, "Отмена"), ok)));
  }

  const coreUi = { rel: false, msg: "" };
  function fmtMb(n) { return (Number(n) / 1048576).toFixed(1) + " МБ"; }
  function openCores() {
    coreUi.msg = "";
    openLive(() => {
      const C = S.cores;
      const busy = !!C.busy;
      const coreRow = (selected, title, sub, onSelect, onDelete) => h("button", { class: "row" + (selected ? " selected" : ""), onclick: onSelect },
        h("div", { class: "avatar" }, selected ? icon("check") : icon("cpu")),
        h("div", { class: "grow" }, h("div", { class: "t" }, title), sub ? h("div", { class: "s" }, sub) : null),
        onDelete ? h("span", { class: "icon-btn", role: "button", "aria-label": "Удалить ядро", onclick: (e) => { e.stopPropagation(); onDelete(); } }, icon("del")) : null);
      const rows = [coreRow(!C.active, "Встроенное", C.builtin ? "Xray " + C.builtin : "Идёт в комплекте с приложением", () => Bridge.act("selectCore", ""), null)];
      C.list.forEach((c) => rows.push(coreRow(C.active === c.id, c.label, c.version ? "Xray " + c.version : "", () => Bridge.act("selectCore", c.id), () => Bridge.act("deleteCore", c.id))));
      const relList = C.releases === null || C.releases === undefined
        ? h("div", { class: "body" }, "Список версий загружается…")
        : h("div", { class: "list" }, C.releases.map((r) => h("div", { class: "row" },
          h("div", { class: "grow" }, h("div", { class: "t" }, r.tag), h("div", { class: "s" }, fmtMb(r.size))),
          h("button", { class: "btn tonal", disabled: busy, onclick: async () => { const x = await Bridge.act("downloadCore", r.tag); coreUi.msg = x && x.error ? x.error : "Ядро " + r.tag + " установлено и выбрано"; refreshLive(); } }, icon("download"), "Скачать"))));
      const out = h("div", {},
        h("h2", {}, "Ядро Xray"),
        h("div", { class: "body" }, "Своё ядро запускается отдельным процессом: встроенное создаёт туннель и отправляет трафик в запущенный Xray. Нужен исполняемый файл Xray для Android (arm64) или zip с ним. Применяется при следующем подключении."),
        busy ? h("div", { class: "busy-line" }, h("div", { class: "bar" }), h("div", { class: "body" }, C.busy)) : null,
        h("div", { class: "group-title" }, h("span", {}, "Активное ядро")),
        h("div", { class: "list" }, rows),
        h("div", { class: "actions split" },
          h("button", { class: "btn tonal", disabled: busy, onclick: async () => { const x = await Bridge.act("pickCore"); if (x && x.cancel) return; coreUi.msg = x && x.error ? x.error : "Ядро добавлено"; refreshLive(); } }, icon("add"), "Из файла"),
          h("button", { class: "btn tonal", disabled: busy, onclick: async () => { coreUi.rel = !coreUi.rel; if (coreUi.rel && !S.cores.releases) { const x = await Bridge.act("loadReleases"); if (x && x.error) coreUi.msg = x.error; } refreshLive(); } }, icon("download"), "Скачать версию")),
        coreUi.rel ? [h("div", { class: "group-title" }, h("span", {}, "Версии Xray-core")), relList] : null,
        coreUi.msg ? h("div", { class: "msg" }, coreUi.msg) : null);
      return out;
    });
  }

  let shownUpdate = "";
  function openUpdate() {
    const u = S.update;
    shownUpdate = u.tag;
    openSheet(h("div", {},
      h("h2", {}, "Доступна новая версия"),
      h("div", { class: "body" }, "Установлена " + (u.current || "—") + ", на GitHub вышла " + u.tag + "."),
      u.notes ? h("div", { class: "notes" }, u.notes) : null,
      h("div", { class: "actions" },
        h("button", { class: "btn text", onclick: () => { closeSheet(); Bridge.act("dismissUpdate", u.tag); } }, "Позже"),
        h("button", { class: "btn filled", onclick: () => { closeSheet(); Bridge.act("open", u.url || "https://github.com/Fasdeq13/FasdeqVPN/releases/"); } }, "Перейти к релизам"))));
  }

  function getPath(path) { return path.split(".").reduce((o, k) => (o == null ? o : o[k]), S.settings); }
  const pend = new Map();
  function applyPend(settings) {
    const now = performance.now();
    pend.forEach((e, path) => {
      if (now - e.t > 1800) { pend.delete(path); return; }
      const keys = path.split(".");
      let o = settings;
      for (let i = 0; i < keys.length - 1; i++) { if (o[keys[i]] == null) o[keys[i]] = {}; o = o[keys[i]]; }
      o[keys[keys.length - 1]] = e.v;
    });
  }
  function setPath(path, value) {
    pend.set(path, { v: value, t: performance.now() });
    const keys = path.split(".");
    let o = S.settings;
    for (let i = 0; i < keys.length - 1; i++) o = o[keys[i]];
    o[keys[keys.length - 1]] = value;
    Bridge.act("set", { path, value });
    applyTheme();
    applyMotionFlags();
    Power.kick();
    refreshers.forEach((f) => f());
    fixCorners();
  }

  function row(title, sub, control, onTap) {
    return h(onTap ? "button" : "div", { class: "row", onclick: onTap }, h("div", { class: "grow" }, h("div", { class: "t" }, title), sub ? h("div", { class: "s" }, sub) : null), control);
  }
  function sw(path, title, sub, show) {
    const s = h("span", { class: "switch" });
    const r = row(title, sub, s, () => setPath(path, !getPath(path)));
    let last;
    refreshers.push(() => {
      const v = !!getPath(path);
      if (v !== last) { last = v; s.classList.toggle("on", v); }
      if (show) { const hd = !show(); if (r.hidden !== hd) r.hidden = hd; }
    });
    return r;
  }
  function seg(path, options, title, show) {
    const thumb = h("i", { class: "thumb" });
    const buttons = options.map((o, i) => h("button", { onclick: () => setPath(path, i) }, o));
    const box = h("div", { class: "seg" }, thumb, buttons);
    const r = h("div", { class: "row block" }, h("div", { class: "head" }, h("div", { class: "t" }, title)), box);
    let lastI = -1, lastW = 0;
    refreshers.push(() => {
      if (show) { const hd = !show(); if (r.hidden !== hd) r.hidden = hd; }
      const i = clamp(Number(getPath(path)) || 0, 0, options.length - 1);
      const w = box.clientWidth;
      if (i === lastI && w === lastW) return;
      if (r.hidden || !w) return;
      buttons.forEach((b, j) => b.classList.toggle("on", j === i));
      const b = buttons[i];
      if (b && b.offsetWidth) {
        if (lastW === 0 || w !== lastW) { thumb.style.transition = "none"; }
        thumb.style.width = b.offsetWidth + "px";
        thumb.style.transform = "translate3d(" + b.offsetLeft + "px,0,0)";
        if (thumb.style.transition === "none") { void thumb.offsetWidth; thumb.style.transition = ""; }
        lastI = i; lastW = w;
      }
    });
    return r;
  }
  function slider(path, title, min, max, step, fmt, show) {
    const input = h("input", { type: "range", min, max, step });
    const val = h("div", { class: "v" }, "");
    const paint = (v) => {
      input.style.setProperty("--p", ((v - min) / (max - min)) * 100 + "%");
      val.textContent = fmt(v);
    };
    const r = h("div", { class: "row block" }, h("div", { class: "head" }, h("div", { class: "t" }, title), val), input);
    input.addEventListener("input", () => { const v = Number(input.value); lastV = v; paint(v); setPath(path, v); });
    let lastV;
    refreshers.push(() => {
      if (show) { const hd = !show(); if (r.hidden !== hd) r.hidden = hd; }
      const v = Number(getPath(path));
      if (v === lastV || document.activeElement === input) return;
      lastV = v; input.value = v; paint(v);
    });
    return r;
  }
  function textBox(path, title, placeholder, show) {
    const ta = h("textarea", { class: "inline", placeholder, spellcheck: "false" });
    const r = h("div", { class: "row block" }, h("div", { class: "head" }, h("div", { class: "t" }, title)), ta);
    let t = 0;
    ta.addEventListener("input", () => { clearTimeout(t); t = setTimeout(() => setPath(path, ta.value), 250); });
    refreshers.push(() => { if (show) r.hidden = !show(); if (document.activeElement !== ta) ta.value = String(getPath(path) || ""); });
    return r;
  }
  function action(title, sub, onTap) {
    return row(title, sub, h("span", { class: "icon-btn" }, icon("chevron")), onTap);
  }
  function group(title, ...rows) {
    return [h("div", { class: "group-title" }, h("span", {}, title)), h("div", { class: "list" }, rows)];
  }

  function colorEditor() {
    const prev = h("div", { class: "rgb-preview" });
    const name = h("span", {}, "");
    const code = h("span", {}, "");
    prev.append(name, code);
    const sw_ = PRESETS.map((p) => h("button", { class: "swatch", "aria-label": p[0], style: "background:" + toHex({ r: p[1], g: p[2], b: p[3] }), onclick: () => setPath("accent", { r: p[1], g: p[2], b: p[3] }) }));
    const swBox = h("div", { class: "swatches" }, sw_);
    const chan = (key, label, tint) => {
      const input = h("input", { type: "range", min: 0, max: 255, step: 1 });
      const val = h("div", { class: "v" }, "");
      const paint = (v) => { input.style.setProperty("--p", (v / 255) * 100 + "%"); val.textContent = String(v); };
      input.addEventListener("input", () => {
        const c = Object.assign({}, S.settings.accent);
        c[key] = Number(input.value);
        paint(c[key]);
        setPath("accent", c);
      });
      input.style.setProperty("--c", tint);
      const box = h("div", { class: "rgb-slider" }, h("div", { class: "head", style: "display:flex;justify-content:space-between" }, h("div", { class: "t" }, label), val), input);
      return { box, input, paint };
    };
    const ch = [chan("r", "Красный", "#ff5c5c"), chan("g", "Зелёный", "#5cdc7a"), chan("b", "Синий", "#5c8cff")];
    const hex = h("input", { class: "hex", maxlength: 7, spellcheck: "false", "aria-label": "Цвет в формате HEX" });
    hex.addEventListener("change", () => {
      const c = parseHex(hex.value);
      if (c) setPath("accent", c); else hex.value = toHex(S.settings.accent).toUpperCase();
    });
    const r = h("div", { class: "row block" }, h("div", { class: "head" }, h("div", { class: "t" }, "Свой цвет")), prev, swBox, h("div", { class: "field-row" }, h("div", { class: "body", style: "flex:1" }, "Код цвета"), hex), ch.map((c) => c.box));
    refreshers.push(() => {
      const c = S.settings.accent;
      const hx = toHex(c).toUpperCase();
      prev.style.background = hx;
      const [L] = rgbToOklch(c.r, c.g, c.b);
      prev.style.color = L > 0.62 ? "#111" : "#fff";
      name.textContent = "RGB " + c.r + ", " + c.g + ", " + c.b;
      code.textContent = hx;
      if (document.activeElement !== hex) hex.value = hx;
      ch[0].input.value = c.r; ch[0].paint(c.r);
      ch[1].input.value = c.g; ch[1].paint(c.g);
      ch[2].input.value = c.b; ch[2].paint(c.b);
      sw_.forEach((b, i) => b.classList.toggle("on", PRESETS[i][1] === c.r && PRESETS[i][2] === c.g && PRESETS[i][3] === c.b));
      r.classList.toggle("dim", !!S.settings.dynamic);
    });
    return r;
  }

  function buildSettings() {
    const v = $("#view-settings");
    const put = (...x) => v.append(...x.flat(Infinity));
    const anim = () => S.settings.anim.all;
    put(
      h("div", { class: "top-row" }, h("h1", { class: "headline" }, "Настройки")),
      group("Цвет и тема",
        seg("theme", ["Система", "Светлая", "Тёмная"], "Тема"),
        colorEditor(),
        sw("dynamic", "Цвета системы", "Палитра из обоев вместо своего цвета. Android 12 и новее"),
        sw("tint", "Оттенять фон цветом", "Выключите для нейтральных серых поверхностей"),
        sw("amoled", "Чёрный фон", "Для OLED-экранов в тёмной теме")),
      group("Анимации",
        sw("anim.all", "Все анимации", "Главный выключатель: выключите, и всё станет мгновенным"),
        sw("anim.pages", "Переходы между экранами", "Сдвиг при переключении вкладок", anim),
        sw("anim.controls", "Переключатели и нажатия", "Движение переключателей, ползунков, кнопок", anim),
        sw("anim.sheets", "Панели и уведомления", "Выезд нижних панелей и сообщений", anim),
        sw("anim.enabled", "Анимация кнопки питания", "Деформация, волны, вращение", anim),
        slider("anim.speed", "Скорость интерфейса", 0.25, 3, 0.05, (x) => "×" + x.toFixed(2), anim)),
      group("Кнопка питания",
        slider("button.size", "Размер", 160, 280, 4, (x) => x + " px"),
        seg("button.shape", ["Печенье", "Круг", "Квадрат", "Звезда"], "Форма"),
        slider("button.lobes", "Количество лепестков", 3, 16, 1, (x) => String(x)),
        slider("anim.intensity", "Сила деформации", 0, 2, 0.05, (x) => Math.round(x * 100) + "%", anim),
        slider("anim.tempo", "Темп движения", 0.2, 3, 0.05, (x) => "×" + x.toFixed(2), anim),
        slider("anim.glow", "Свечение при подключении", 0, 1, 0.05, (x) => Math.round(x * 100) + "%"),
        sw("anim.burst", "Волны при подключении", "Расходящиеся кольца", anim),
        slider("anim.waves", "Количество волн", 1, 5, 1, (x) => String(x), anim),
        sw("anim.idle", "Движение в покое", "Едва заметное дыхание", anim),
        sw("anim.iconFx", "Анимация значка", "Сжатие и покачивание при подключении", anim),
        sw("anim.press", "Эффект нажатия", "Кнопка уменьшается под пальцем", anim),
        sw("anim.haptics", "Вибрация", "Отклик при нажатии")),
      group("Подключение",
        sw("autoConnect", "Подключаться при запуске"),
        seg("dns", ["Cloudflare", "Google", "Quad9", "AdGuard", "Свой"], "DNS"),
        textBox("dnsCustom", "Свои DNS через запятую", "1.1.1.1, 8.8.8.8", () => S.settings.dns === 4),
        slider("mtu", "MTU", 1280, 1500, 10, (x) => String(x))),
      group("Низкоуровневые",
        sw("tuning.ipv6", "IPv6", "Выключите, если сайты зависают"),
        sw("tuning.sniff", "Sniffing", "Определять домен по HTTP, TLS и QUIC"),
        sw("tuning.quic", "Блокировать QUIC", "Браузеры перейдут на TCP"),
        sw("tuning.lan", "Локальная сеть напрямую", "10.x, 172.16.x, 192.168.x мимо VPN"),
        sw("tuning.unm", "Сеть без лимита", "Система не считает трафик VPN мобильным"),
        sw("tuning.mux", "Mux", "Не работает с XTLS Vision"),
        slider("tuning.muxc", "Потоков Mux", 1, 64, 1, (x) => String(x), () => S.settings.tuning.mux),
        seg("tuning.log", ["Нет", "Ошибки", "Предупр.", "Инфо", "Отладка"], "Журнал Xray"),
        seg("tuning.ds", ["AsIs", "IPIfNonMatch", "IPOnDemand"], "Domain strategy"),
        slider("tuning.idle", "Таймаут простоя", 30, 1800, 30, (x) => x + " с"),
        slider("tuning.buf", "Буфер на соединение", 0, 2048, 64, (x) => (x === 0 ? "по умолчанию" : x + " КБ"))),
      group("Приложения",
        seg("tuning.am", ["Все", "Исключить", "Только"], "Через VPN идут"),
        textBox("tuning.apps", "Пакеты через запятую", "org.telegram.messenger, com.discord", () => S.settings.tuning.am !== 0)),
      group("Переподключение",
        sw("tuning.rc", "Переподключаться при обрыве"),
        slider("tuning.rt", "Попыток за 2 минуты", 1, 20, 1, (x) => String(x), () => S.settings.tuning.rc),
        slider("tuning.rd", "Пауза между попытками", 1, 30, 1, (x) => x + " с", () => S.settings.tuning.rc)),
      group("Ядро",
        action("Ядро Xray", "Встроенное или своё, версия и загрузка", openCores)),
      group("Сервисы",
        sw("autoCheck", "Проверять сервисы автоматически", "Discord, Telegram и YouTube через каждый сервер, пока VPN включён"),
        slider("checkInterval", "Интервал проверки", 5, 240, 5, (x) => x + " мин", () => S.settings.autoCheck),
        action("Проверить сейчас", "Запустить проверку сервисов для всех серверов", () => { Bridge.act("checkServices"); snack("Проверяю сервисы…"); go("home"); })),
      group("Проверка пинга",
        seg("pingMode", ["Авто", "TCP", "UDP", "Оба", "ICMP"], "Способ"),
        seg("pingTimeout", ["2 с", "4 с", "8 с"], "Таймаут"),
        sw("autoPing", "Проверять автоматически"),
        sw("sortPing", "Сортировать по пингу")),
      group("Обновления",
        sw("updates.auto", "Сообщать о новых версиях", "Проверка релизов на GitHub раз в несколько часов"),
        action("Проверить обновления", "github.com/Fasdeq13/FasdeqVPN/releases", async () => {
          snack("Проверяю…");
          const r = await Bridge.act("checkUpdate");
          await syncNow();
          if (r && r.error) snack(r.error);
          else if (r && r.available) openUpdate();
          else snack("Установлена последняя версия");
        }),
        action("Открыть страницу релизов", "Скачать APK вручную", () => Bridge.act("open", "https://github.com/Fasdeq13/FasdeqVPN/releases/"))),
      group("Интерфейс",
        slider("textScale", "Размер текста", 0.8, 1.5, 0.05, (x) => Math.round(x * 100) + "%"),
        action("Обычный интерфейс", "Вернуться к интерфейсу на Compose. Приложение перезапустится", () => Bridge.act("switchUi", "compose")))
    );
    v.append(h("div", { class: "about" }, h("div", { class: "title-large" }, "FasdeqVPN"), h("div", { class: "body" }, "Версия 1.7 · веб-интерфейс")));
  }

  function fixCorners() {
    $$(".list").forEach((l) => {
      const vis = Array.from(l.children).filter((x) => !x.hidden);
      Array.from(l.children).forEach((x) => x.classList.remove("first", "last"));
      if (vis.length) { vis[0].classList.add("first"); vis[vis.length - 1].classList.add("last"); }
    });
  }

  function fillIcons() {
    $("#i-nav-home").setAttribute("d", ICONS.home);
    $("#i-nav-servers").setAttribute("d", ICONS.servers);
    $("#i-nav-settings").setAttribute("d", ICONS.settings);
  }

  function onState(next) {
    const first = !S || !S.__ready;
    next.settings = merge(DEFAULTS, next.settings || {});
    applyPend(next.settings);
    next.servers = next.servers || [];
    next.services = next.services || { names: ["Discord", "Telegram", "YouTube"], checking: false, checks: {}, best: [] };
    next.cores = next.cores || { active: "", builtin: "", busy: null, list: [], releases: null };
    next.update = next.update || { available: false };
    next.groups = next.groups || [];
    next.pings = next.pings || {};
    next.__ready = true;
    S = next;
    applyTheme();
    applyMotionFlags();
    updateHome();
    renderServers();
    refreshers.forEach((f) => f());
    fixCorners();
    refreshLive();
    if (S.update.available && S.settings.updates.auto && S.update.tag !== shownUpdate && !$("#sheet").classList.contains("show")) openUpdate();
    if (first) requestAnimationFrame(() => { refreshers.forEach((f) => f()); fixCorners(); });
  }

  async function boot() {
    fillIcons();
    S = await Bridge.state();
    S.settings = merge(DEFAULTS, S.settings || {});
    S.__ready = false;
    buildHome();
    buildSettings();
    const fab = h("button", { id: "fab", class: "fab hide", onclick: addSheet }, icon("add"), "Добавить");
    $("#app").append(fab);
    $$(".nav-item").forEach((n) => n.addEventListener("click", () => go(n.dataset.go)));
    $("#scrim").addEventListener("click", closeSheet);
    Bridge.onState(onState);
    $$(".view").forEach((v) => v.classList.toggle("left", v.dataset.view !== "home"));
    $("#view-home").classList.add("active");
    $(".nav-item[data-go=home]").classList.add("active");
    onState(S);
    const q = new URLSearchParams(location.search);
    if (q.get("view")) { document.body.style.setProperty("--x", "1"); go(q.get("view")); }
    window.addEventListener("resize", () => refreshers.forEach((f) => f()));
    window.__back = function () {
      if ($("#sheet").classList.contains("show")) { closeSheet(); return true; }
      if (current !== "home") { go("home"); return true; }
      return false;
    };
    Bridge.ready();
    window.__go = go;
    window.__addSheet = addSheet;
    window.__Power = Power;
  }
  boot();
})();
