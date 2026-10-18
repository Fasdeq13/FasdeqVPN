(function () {
  const listeners = [];
  const native = window.FasdeqNative;

  function emit(state) {
    for (const cb of listeners) cb(state);
  }

  if (native) {
    window.__fasdeqPush = function (json) {
      try { emit(JSON.parse(json)); } catch (e) {}
    };
    const pending = {};
    let seq = 0;
    window.__fasdeqResult = function (id, json) {
      const f = pending[id];
      delete pending[id];
      if (!f) return;
      try { f(json ? JSON.parse(json) : {}); } catch (e) { f({}); }
    };
    window.Bridge = {
      native: true,
      onState(cb) { listeners.push(cb); },
      state() { return Promise.resolve(JSON.parse(native.state())); },
      act(name, payload) {
        return new Promise((resolve) => {
          const id = ++seq;
          pending[id] = resolve;
          try { native.act(name, payload === undefined ? "" : JSON.stringify(payload), id); } catch (e) { delete pending[id]; resolve({}); }
        });
      },
      ready() { try { native.ready(); } catch (e) {} },
      bars(dark) { try { native.bars(dark); } catch (e) {} },
    };
    return;
  }

  const S = {
    status: "Off",
    selected: "s2",
    rx: 0, tx: 0, down: 0, up: 0, uptime: 0,
    error: null,
    dark: true,
    colors: [],
    servers: [
      { id: "s1", name: "Хельсинки 1", proto: "VLESS Reality", endpoint: "hel1.example.net:443", group: "Основная" },
      { id: "s2", name: "Нидерланды", proto: "VLESS Reality", endpoint: "ams.example.net:443", group: "Основная" },
      { id: "s3", name: "Германия, Франкфурт", proto: "Trojan", endpoint: "fra.example.net:443", group: "Основная" },
      { id: "s4", name: "Казахстан", proto: "Shadowsocks", endpoint: "kz.example.net:8388", group: "Запасные" },
      { id: "s5", name: "Домашний WireGuard", proto: "WireGuard", endpoint: "home.example.org:51820", group: "" },
    ],
    pings: { s1: { ms: 38 }, s2: { ms: 61 }, s3: { ms: 112 }, s4: { ms: 284 }, s5: { ms: null } },
    groups: [{ name: "Основная", url: "https://example.com/sub" }, { name: "Запасные", url: "" }],
    settings: {},
    services: { names: ["Discord", "Telegram", "YouTube"], checking: false, checks: {}, best: [] },
    cores: { active: "", builtin: "25.9.11", busy: null, list: [{ id: "c1", label: "Xray 25.8.3", version: "25.8.3" }], releases: null },
    update: { available: true, checking: false, tag: "v1.8", url: "https://github.com/Fasdeq13/FasdeqVPN/releases/", notes: "Новый интерфейс, проверка сервисов", current: "1.7" },
  };
  let timer = null;
  let ticker = null;

  function clone() { return JSON.parse(JSON.stringify(S)); }
  function push() { emit(clone()); }

  function startTicker() {
    clearInterval(ticker);
    ticker = setInterval(() => {
      if (S.status !== "On") return;
      S.uptime += 1;
      S.down = 180000 + Math.random() * 900000;
      S.up = 20000 + Math.random() * 160000;
      S.rx += S.down;
      S.tx += S.up;
      push();
    }, 1000);
  }

  window.Bridge = {
    native: false,
    ready() {},
    bars() {},
    onState(cb) { listeners.push(cb); },
    state() { return Promise.resolve(clone()); },
    act(name, payload) {
      switch (name) {
        case "connect":
          if (S.status === "Off") {
            S.status = "Connecting"; S.error = null; push();
            clearTimeout(timer);
            timer = setTimeout(() => { S.status = "On"; S.uptime = 0; S.rx = 0; S.tx = 0; startTicker(); push(); }, 1100);
          }
          break;
        case "disconnect":
          if (S.status === "On" || S.status === "Connecting") {
            clearTimeout(timer);
            S.status = "Stopping"; push();
            timer = setTimeout(() => { S.status = "Off"; S.down = 0; S.up = 0; push(); }, 450);
          }
          break;
        case "select":
          S.selected = payload; push();
          break;
        case "remove":
          S.servers = S.servers.filter((s) => s.id !== payload);
          if (S.selected === payload) S.selected = S.servers[0] ? S.servers[0].id : null;
          push();
          break;
        case "add": {
          const lines = String(payload || "").split(/\s+/).filter((l) => /^[a-z0-9+.-]+:\/\//i.test(l));
          lines.forEach((l, i) => {
            const id = "n" + Date.now() + i;
            S.servers.push({ id, name: "Новый сервер " + (S.servers.length + 1), proto: l.split(":")[0].toUpperCase(), endpoint: l.replace(/^[^:]+:\/\//, "").split(/[?#]/)[0], group: "" });
            if (!S.selected) S.selected = id;
          });
          push();
          return Promise.resolve({ added: lines.length });
        }
        case "ping":
          S.servers.forEach((s) => { S.pings[s.id] = { ms: Math.round(30 + Math.random() * 320) }; });
          return new Promise((r) => setTimeout(() => { push(); r({}); }, 900));
        case "set": {
          const path = payload.path.split(".");
          let o = S.settings;
          for (let i = 0; i < path.length - 1; i++) { o[path[i]] = o[path[i]] || {}; o = o[path[i]]; }
          o[path[path.length - 1]] = payload.value;
          push();
          break;
        }
        case "pingGroup":
          S.servers.filter((s) => s.group === payload).forEach((s) => { S.pings[s.id] = { ms: Math.round(30 + Math.random() * 320) }; });
          push();
          break;
        case "refreshGroup":
          return Promise.resolve({});
        case "removeGroup":
          S.servers = S.servers.filter((s) => s.group !== payload);
          S.groups = S.groups.filter((g) => g.name !== payload);
          if (!S.servers.some((s) => s.id === S.selected)) S.selected = S.servers[0] ? S.servers[0].id : null;
          push();
          break;
        case "checkServices":
          S.services.checking = true;
          S.servers.forEach((x) => { S.services.checks[x.id] = { ms: [null, null, null], busy: true }; });
          push();
          setTimeout(() => {
            S.servers.forEach((x) => { S.services.checks[x.id] = { ms: [0, 1, 2].map(() => (Math.random() < 0.2 ? -1 : Math.round(80 + Math.random() * 500))), busy: false }; });
            S.services.checking = false;
            S.services.best = [0, 1, 2].map((i) => { let b = null, m = 1e9; S.servers.forEach((x) => { const v = S.services.checks[x.id].ms[i]; if (v > 0 && v < m) { m = v; b = x.id; } }); return b; });
            push();
          }, 1200);
          break;
        case "selectCore": S.cores.active = payload; push(); break;
        case "deleteCore": S.cores.list = S.cores.list.filter((c) => c.id !== payload); if (S.cores.active === payload) S.cores.active = ""; push(); break;
        case "pickCore": return Promise.resolve({});
        case "loadReleases":
          S.cores.busy = "Загружаю список версий…"; push();
          return new Promise((r) => setTimeout(() => { S.cores.busy = null; S.cores.releases = [{ tag: "v25.9.11", size: 14000000 }, { tag: "v25.8.3", size: 13800000 }, { tag: "v25.7.26", size: 13700000 }]; push(); r({}); }, 600));
        case "downloadCore": return Promise.resolve({});
        case "checkUpdate": return Promise.resolve({ available: S.update.available, tag: S.update.tag });
        case "dismissUpdate": S.update.available = false; push(); break;
        case "renameServer": { const x = S.servers.find((q) => q.id === payload.id); if (x) x.name = payload.name; push(); break; }
        case "renameGroup": S.servers.forEach((x) => { if (x.group === payload.old) x.group = payload.name; }); S.groups.forEach((g) => { if (g.name === payload.old) g.name = payload.name; }); push(); break;
        case "clearGroup": break;
        case "open": return Promise.resolve({});
        case "switchUi":
          return Promise.resolve({});
        case "clipboard":
          return Promise.resolve({ text: "vless://uuid@demo.example.net:443?security=reality#Демо" });
        case "haptic":
        default:
      }
      return Promise.resolve({});
    },
  };
})();
