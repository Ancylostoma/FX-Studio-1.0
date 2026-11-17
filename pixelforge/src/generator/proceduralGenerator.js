// Algoritmos procedurales: pluma en coordenadas unitarias, kit de color y
// generadores de entorno (cielo, astros, relieve, suelo, agua, clima).
// Todos emiten operaciones PixelScene; ninguno toca píxeles directamente.
import { fromHex, mix } from '../core/paletteManager.js';
import { MATERIALS, COLORS } from './templates.js';

// Pluma: dibuja en una caja (x,y,w,h) con coordenadas 0..1, de forma que el
// mismo sprite procedural sirve para cualquier resolución.
export class Pen {
  constructor(ops, x, y, w, h, flip = false) { this.o = ops; this.x = x; this.y = y; this.w = w; this.h = h; this.flip = flip; }
  on(ops) { return new Pen(ops, this.x, this.y, this.w, this.h, this.flip); }
  X(u) { return this.x + Math.round((this.flip ? 1 - u : u) * this.w); }
  Y(v) { return this.y + Math.round(v * this.h); }
  rect(u, v, uw, vh, c, mode) {
    const xa = this.X(u), xb = this.X(u + uw), y0 = this.Y(v), y1 = this.Y(v + vh);
    const x0 = Math.min(xa, xb);
    this.o.push(tail(['R', x0, y0, Math.max(1, Math.abs(xb - xa)), Math.max(1, y1 - y0), c], mode));
    return this;
  }
  ell(u, v, ru, rv, c, mode) {
    this.o.push(tail(['E', this.X(u), this.Y(v), Math.max(0, Math.round(ru * this.w)), Math.max(0, Math.round(rv * this.h)), c], mode));
    return this;
  }
  poly(pts, c, mode) {
    const flat = [];
    for (const [u, v] of pts) flat.push(this.X(u), this.Y(v));
    this.o.push(tail(['P', flat, c], mode));
    return this;
  }
  line(u1, v1, u2, v2, c, mode) { this.o.push(tail(['L', this.X(u1), this.Y(v1), this.X(u2), this.Y(v2), c], mode)); return this; }
  px(u, v, c) { this.o.push(['R', Math.min(this.X(u), this.x + this.w - 1), Math.min(this.Y(v), this.y + this.h - 1), 1, 1, c]); return this; }
  shade(u, v, uw, vh, delta) {
    const xa = this.X(u), xb = this.X(u + uw);
    this.o.push(['H', Math.min(xa, xb), this.Y(v), Math.max(1, Math.abs(xb - xa)), Math.max(1, this.Y(v + vh) - this.Y(v)), delta]);
    return this;
  }
  tex(c, dens, seed, mode = 1) { this.o.push(['T', this.x, this.y, this.w + 1, this.h + 1, c, dens, seed >>> 0, mode]); return this; }
}
const tail = (op, mode) => { if (mode) op.push(mode); return op; };

// Kit de color: traduce materiales y colores con nombre a rampas indexadas,
// aplicando los ajustes del estilo.
export class Kit {
  constructor(pb, style) { this.pb = pb; this.style = style; }
  hsl(h, s, l) { const st = this.style; return this.pb.ramp(h + (st.hue || 0), Math.min(100, s * (st.sat || 1)), l, st.map); }
  mat(name) { return this.hsl(...(MATERIALS[name] || COLORS[name] || MATERIALS.stone)); }
  col(name) { return this.hsl(...(COLORS[name] || COLORS.gray)); }
  // Color pedido por el usuario o, en su defecto, el material por omisión.
  tint(color, fallback) { return typeof color === 'string' && COLORS[color] ? this.col(color) : this.mat(fallback); }
  rgb(c) { return this.pb.idx(typeof c === 'string' ? fromHex(c) : c); }
  get dark() { return this.rgb([24, 20, 37]); }
}

export const L = (n, k, o, props = {}) => ({ n, k, ...props, o });

// ---------- Cielo ----------
export function skyColors(G) {
  const { spec, sky } = G;
  let cols = sky.g.map(fromHex);
  if (spec.sky?.color && COLORS[spec.sky.color]) {
    const [h, s] = COLORS[spec.sky.color];
    const night = G.night, n = 5;
    cols = Array.from({ length: n }, (_, i) => {
      const t = i / (n - 1);
      const l = night ? 7 + t * 30 : 30 + t * 48;
      return hslRgb(h + (t - 0.5) * 30, Math.min(85, s * (night ? 0.9 : 0.75)), l);
    });
  }
  return cols;
}
function hslRgb(h, s, l) { // local para no depender del estilo
  h = ((h % 360) + 360) % 360; s /= 100; l /= 100;
  const k = (n) => (n + h / 30) % 12, a = s * Math.min(l, 1 - l);
  const f = (n) => Math.round(255 * (l - a * Math.max(-1, Math.min(k(n) - 3, 9 - k(n), 1))));
  return [f(0), f(8), f(4)];
}

export function genSky(G) {
  const { w, h, H } = G;
  const o = [];
  if (G.layoutKind === 'portrait') {
    o.push(['G', 0, 0, w, h, G.skyIdx]);
    return [L('fondo', 'sky', o, { nl: 1 })];
  }
  const sh = G.biome.noGround ? h : Math.min(h, H + 1);
  o.push(['G', 0, 0, w, sh, G.skyIdx]);
  if (sh < h) o.push(['R', 0, sh, w, h - sh, G.skyIdx[G.skyIdx.length - 1]]);
  return [L('cielo', 'sky', o, { nl: 1 })];
}

export function genStars(G) {
  const dens = G.starDensity;
  if (dens <= 0) return [];
  const { w, H, C } = G, rng = G.rng('stars');
  const sh = Math.round((G.biome.noGround ? G.h : H) * 0.92);
  const o = [], tw = [];
  const dim = C.rgb([150, 160, 200]), bright = C.rgb([240, 240, 255]), warm = C.rgb([255, 230, 170]);
  const seed = rng.int(1, 999999);
  if (dens >= 1) {
    o.push(['T', 0, 0, w, sh, dim, 1, seed], ['T', 0, 0, w, Math.round(sh * 0.8), bright, 1, seed + 7]);
    if (dens > 1.3) o.push(['T', 0, 0, w, sh, C.rgb([120, 110, 190]), 2, seed + 13]);
  }
  const n = Math.round(((w * sh) / 900) * dens * (dens >= 1 ? 1 : 4));
  if (w >= 48) {
    G.sprites['star'] = { w: 3, h: 3, o: [['R', 1, 0, 1, 3, bright], ['R', 0, 1, 3, 1, bright]] };
    for (let i = 0; i < n; i++) { const op = ['S', 'star', rng.int(0, w - 3), rng.int(0, Math.round(sh * 0.75))]; if (G.frames > 1 && i % 3 === 0) { if ((i / 3 + G.frame) % G.frames) tw.push(op); } else o.push(op); }
  }
  for (let i = 0; i < n; i++) { const op = ['R', rng.int(0, w - 1), rng.int(0, sh), 1, 1, rng.chance(0.3) ? warm : bright]; if (G.frames > 1 && i % 3 === 1) { if (((i - 1) / 3 + G.frame + 2) % G.frames) tw.push(op); } else o.push(op); }
  // Animado: un tercio de las estrellas va a una capa aparte que parpadea; el resto no se duplica.
  return G.frames > 1 ? [L('estrellas', 'stars', o, { nl: 1 }), L('centelleo', 'stars', tw, { nl: 1 })] : [L('estrellas', 'stars', o, { nl: 1 })];
}

export function genCelestial(G) {
  const { w, h, H, C, spec } = G, rng = G.rng('celestial');
  const out = [];
  const u = Math.min(w, h);
  const has = (id) => spec.celestial.includes(id);
  const skyH = G.biome.noGround ? h * 0.8 : H;
  if (has('aurora')) {
    const o = [], cols = [C.rgb([60, 230, 160]), C.rgb([70, 190, 220]), C.rgb([150, 100, 230])];
    for (let b = 0; b < 3; b++) {
      const pts = [], y0 = skyH * (0.12 + b * 0.1), amp = skyH * 0.07, ph = rng.range(0, 6);
      const n = 10;
      for (let i = 0; i <= n; i++) pts.push(Math.round((i / n) * w), Math.round(y0 + Math.sin(ph + i * 0.9) * amp));
      for (let i = n; i >= 0; i--) pts.push(Math.round((i / n) * w), Math.round(y0 + skyH * 0.16 + Math.sin(ph + i * 0.9 + 0.6) * amp));
      o.push(['P', pts, cols[b]]);
      o.push(['D', 0, Math.round(y0 + skyH * 0.08), w, Math.round(skyH * 0.2), -1, 8, 1]);
    }
    out.push(L('aurora', 'celestial', o, { nl: 1, a: 0.4 }));
  }
  if (has('rainbow')) {
    const o = [], cx = Math.round(w * (G.moonSide > 0 ? 0.35 : 0.65)), cy = Math.round(skyH), r = Math.round(Math.min(w * 0.45, skyH * 0.85));
    const cols = ['e8504a', 'f29a3c', 'f5d94e', '6ccf6a', '4aa3e8', '8a62d6'].map((c) => C.rgb(c));
    const band = Math.max(1, Math.round(u / 48));
    cols.forEach((c, i) => o.push(['E', cx, cy, r - i * band, r - i * band, c]));
    o.push(['E', cx, cy, r - 6 * band, r - 6 * band, -1], ['R', 0, cy, w, h, -1]);
    out.push(L('arcoíris', 'celestial', o, { nl: 1, a: 0.55 }));
  }
  const body = has('moon') ? 'moon' : has('sun') ? 'sun' : null;
  if (body) {
    const low = G.timeKey === 'sunset' || G.timeKey === 'dawn';
    const r = Math.max(2, Math.round(u * (body === 'sun' ? (low ? 0.11 : 0.085) : 0.095)));
    const cx = Math.round(w * (G.moonSide > 0 ? 0.76 : 0.24));
    const cy = body === 'sun' && low ? Math.round(skyH - r * 0.9) : Math.round(skyH * (body === 'sun' ? 0.24 : 0.26));
    const glow = [], o = [];
    if (body === 'moon') {
      const m = C.rgb([238, 236, 214]), ms = C.rgb([204, 204, 190]), mh = C.rgb([252, 252, 240]);
      glow.push(['E', cx, cy, Math.round(r * 1.7), Math.round(r * 1.7), m]);
      o.push(['E', cx, cy, r, r, m]);
      if (r >= 4) {
        o.push(['E', cx - Math.round(r * 0.3), cy - Math.round(r * 0.3), Math.round(r * 0.55), Math.round(r * 0.55), mh, 1]);
        o.push(['E', cx + Math.round(r * 0.35), cy - Math.round(r * 0.2), Math.max(0, Math.round(r * 0.18)), Math.max(0, Math.round(r * 0.18)), ms]);
        o.push(['E', cx - Math.round(r * 0.15), cy + Math.round(r * 0.45), Math.max(0, Math.round(r * 0.24)), Math.max(0, Math.round(r * 0.2)), ms]);
        o.push(['E', cx + Math.round(r * 0.5), cy + Math.round(r * 0.4), Math.max(0, Math.round(r * 0.12)), Math.max(0, Math.round(r * 0.12)), ms]);
      }
      if (spec.moonPhase === 'crescent') o.push(['E', cx + Math.round(r * 0.55), cy - Math.round(r * 0.2), r, r, -1]);
    } else {
      const s1 = C.rgb(low ? [255, 214, 120] : [255, 246, 196]), s2 = C.rgb(low ? [255, 170, 80] : [255, 232, 140]);
      glow.push(['E', cx, cy, Math.round(r * 2.1), Math.round(r * 2.1), s2], ['E', cx, cy, Math.round(r * 1.5), Math.round(r * 1.5), s1]);
      o.push(['E', cx, cy, r, r, s2], ['E', cx, cy, Math.max(1, r - Math.max(1, Math.round(r * 0.25))), Math.max(1, r - Math.max(1, Math.round(r * 0.25))), s1]);
      if (low && !G.biome.noGround) { o.push(['R', 0, H + 1, w, h, -1]); glow.push(['R', 0, H + 1, w, h, -1]); }
    }
    out.push(L(body === 'moon' ? 'resplandor lunar' : 'resplandor solar', 'celestial', glow, { nl: 1, a: body === 'moon' ? 0.14 : 0.3 }));
    out.push(L(body === 'moon' ? 'luna' : 'sol', 'celestial', o, { nl: 1 }));
    G.bodyPos = { cx, cy, r };
  }
  if (has('planet')) {
    const o = [], n = Math.min(5, Math.max(spec.planets || 0, G.biome.noGround ? 2 : 1));
    const SPOTS = [[0.74, 0.3, 0.15], [0.2, 0.62, 0.07], [0.16, 0.18, 0.09], [0.84, 0.78, 0.08], [0.5, 0.12, 0.05]];
    for (let i = 0; i < n; i++) {
      const [sx, sy, sr] = SPOTS[i];
      const r = Math.max(3, Math.round(u * sr));
      const cx = Math.round(w * (G.moonSide > 0 ? sx : 1 - sx)), cy = Math.round(skyH * sy);
      const pr = C.hsl(rng.pick([20, 190, 280, 130, 40]), 55, 52);
      o.push(['E', cx, cy, r, r, pr[2]]);
      for (let b = -2; b <= 2; b++) if (rng.chance(0.6)) o.push(['R', cx - r, cy + Math.round((b * r) / 3), 2 * r + 1, Math.max(1, Math.round(r / 6)), pr[b & 1 ? 3 : 1], 1]);
      o.push(['E', cx - Math.round(r * 0.35), cy - Math.round(r * 0.35), Math.round(r * 0.45), Math.round(r * 0.45), pr[4], 1]);
      o.push(['D', cx, cy - r, r + 1, 2 * r + 1, pr[0], 9, 1]);
      if (i === 0 && r >= 5) {
        const ring = C.rgb([226, 205, 160]);
        o.push(['L', cx - Math.round(r * 1.7), cy + Math.round(r * 0.35), cx - r, cy + Math.round(r * 0.12), ring]);
        o.push(['L', cx + r, cy - Math.round(r * 0.12), cx + Math.round(r * 1.7), cy - Math.round(r * 0.35), ring]);
        o.push(['L', cx - Math.round(r * 0.9), cy + Math.round(r * 0.3), cx + Math.round(r * 0.9), cy - Math.round(r * 0.1), ring]);
      }
    }
    out.push(L('planetas', 'celestial', o, { nl: 1 }));
  }
  if (has('comet')) {
    const c = C.rgb([235, 245, 255]), c2 = C.rgb([140, 180, 240]);
    const x = Math.round(w * (G.moonSide > 0 ? 0.3 : 0.7)), y = Math.round(skyH * 0.2), len = Math.round(u * 0.22), d = G.moonSide > 0 ? -1 : 1;
    out.push(L('cometa', 'celestial', [['L', x, y, x + d * len, y - Math.round(len * 0.5), c2], ['L', x, y, x + d * Math.round(len * 0.5), y - Math.round(len * 0.25), c], ['E', x, y, u >= 96 ? 1 : 0, u >= 96 ? 1 : 0, c]], { nl: 1 }));
  }
  return out;
}

export function genClouds(G) {
  if (!G.spec.celestial.includes('clouds') || G.biome.noGround || G.layoutKind === 'portrait') return [];
  const { w, H, C } = G, rng = G.rng('clouds');
  const c = C.hsl(...G.sky.cloud);
  const stormy = G.spec.weather.includes('storm') || G.spec.weather.includes('rain');
  const o = [];
  const kinds = 3;
  for (let k = 0; k < kinds; k++) {
    const cw = Math.max(6, Math.round(w * rng.range(0.16, 0.3))), ch = Math.max(3, Math.round(cw * rng.range(0.28, 0.4)));
    const so = [], n = rng.int(3, 5);
    const blobs = [];
    for (let i = 0; i < n; i++) {
      const t = (i + 0.5) / n;
      const rx = Math.max(1, Math.round(cw * rng.range(0.14, 0.2))), ry = Math.max(1, Math.round(ch * (0.35 + 0.45 * Math.sin(t * Math.PI)) * rng.range(0.8, 1)));
      blobs.push([Math.round(rx + t * (cw - 2 * rx - 1) + 0), ch - 1 - Math.round(ry * 0.6), rx, ry]);
    }
    for (const [x, y, rx, ry] of blobs) so.push(['E', x, y, rx, ry, c[stormy ? 1 : 2]]);
    for (const [x, y, rx, ry] of blobs) so.push(['E', x, y - Math.max(1, Math.round(ry * 0.3)), rx, Math.max(0, ry - 1), c[stormy ? 2 : 4], 1]);
    so.push(['R', 0, ch - 1, cw, 1, c[stormy ? 1 : 2], 1]);
    G.sprites['cloud' + k] = { w: cw, h: ch, o: so };
  }
  const count = Math.max(1, Math.round((stormy ? 6 : 3.2) * G.detail));
  for (let i = 0; i < count; i++) {
    const id = 'cloud' + rng.int(0, kinds - 1), sp = G.sprites[id];
    o.push(['S', id, rng.int(-sp.w >> 2, w - (sp.w >> 1)), rng.int(1, Math.max(2, Math.round(H * (stormy ? 0.4 : 0.55)) - sp.h)), rng.int(0, 1)]);
  }
  return [L('nubes', 'clouds', o, { nl: 1, a: stormy ? 0.95 : 0.9 })];
}

// ---------- Relieve lejano ----------
function peak(o, px, py, base, wl, wr, m, snow, rng, capFrac) {
  const hgt = base - py;
  o.push(['P', [px, py, px - wl, base, px + wr, base], m[1]]);
  const j = (f) => Math.round(px + rng.range(-0.08, 0.1) * wl * f);
  o.push(['P', [px, py, px - wl, base, Math.round(px + wl * 0.12), base, j(1), Math.round(py + hgt * 0.72), j(0.8) + 1, Math.round(py + hgt * 0.45), j(0.4), Math.round(py + hgt * 0.2)], m[2]]);
  o.push(['L', px, py, px - Math.round(wl * 0.5), py + Math.round(hgt * 0.5), m[3]]);
  if (snow && hgt > 6) {
    const cf = capFrac, cy = Math.round(py + hgt * cf), lx = Math.round(px - wl * cf), rx = Math.round(px + wr * cf);
    const zz = [px, py, lx, cy];
    const n = 4;
    for (let i = 1; i < n; i++) zz.push(Math.round(lx + ((rx - lx) * i) / n), cy + (i & 1 ? -Math.max(1, Math.round(hgt * 0.07)) : Math.round(hgt * 0.04)));
    zz.push(rx, cy);
    o.push(['P', zz, snow[1], 1]);
    o.push(['P', [px, py, lx, cy, Math.round(px - wl * 0.05), cy - Math.max(1, Math.round(hgt * 0.06)), px + Math.round(wl * 0.04), Math.round(py + hgt * cf * 0.5)], snow[3], 1]);
  }
}

export function genFar(G) {
  const { w, h, H, C } = G, rng = G.rng('far');
  const out = [];
  const feats = G.farFeatures;
  const sky = G.skyHorizon;
  for (const f of feats) {
    const o = [];
    if (f === 'mountains' || f === 'fuji') {
      const m = C.hsl(232, 22, G.biome.snowy ? 58 : 46), snow = G.style.flat ? null : C.mat('snow');
      if (f === 'fuji') {
        const px = Math.round(w * (G.moonSide > 0 ? 0.36 : 0.64)), ph = Math.round(H * 0.62), py = H - ph, wl = Math.round(w * 0.42);
        const top = Math.max(2, Math.round(w * 0.035));
        const side = (s) => [px + s * top, py, px + s * Math.round(wl * 0.3), py + Math.round(ph * 0.45), px + s * Math.round(wl * 0.62), py + Math.round(ph * 0.8), px + s * wl, H];
        const lft = side(-1), rgt = side(1);
        const outline = [...lft];
        for (let i = rgt.length - 2; i >= 0; i -= 2) outline.push(rgt[i], rgt[i + 1]);
        o.push(['P', outline, m[2]]);
        if (snow) {
          const cy = py + Math.round(ph * 0.36), zz = [px - wl, py - 1];
          const n = 7;
          for (let i = 0; i <= n; i++) zz.push(Math.round(px - wl * 0.34 + ((wl * 0.68) * i) / n), cy + (i & 1 ? Math.round(ph * 0.1) : -Math.round(ph * 0.02)));
          zz.push(px + wl, py - 1);
          o.push(['P', zz, snow[3], 1]);
        }
        // Cara en sombra: baja un tono roca y nieve por igual.
        o.push(['H', px + Math.round(top * 0.4), py, wl, ph + 1, -1]);
        void rgt;
        out.push(L('monte', 'far', o, { f: [sky, 30] }));
      } else {
        const back = [], n = Math.max(2, Math.round(3 * Math.min(1.5, G.detail)));
        for (let i = 0; i < n; i++) {
          const px = Math.round(((i + rng.range(0.2, 0.8)) / n) * w), ph = Math.round(H * rng.range(0.34, 0.56));
          peak(back, px, H - ph, H, Math.round(ph * rng.range(0.9, 1.4)), Math.round(ph * rng.range(0.9, 1.4)), m, snow, rng, rng.range(0.26, 0.36));
        }
        out.push(L('montañas lejanas', 'far', back, { f: [sky, 52] }));
        const front = [];
        for (let i = 0; i < n; i++) {
          const px = Math.round(((i + rng.range(0, 1)) / n) * w), ph = Math.round(H * rng.range(0.18, 0.34));
          peak(front, px, H - ph, H, Math.round(ph * rng.range(1.1, 1.7)), Math.round(ph * rng.range(1.1, 1.7)), m, G.biome.snowy ? snow : null, rng, 0.3);
        }
        out.push(L('montañas', 'far', front, { f: [sky, 28] }));
      }
    } else if (f === 'hills' || f === 'dunes') {
      const g = f === 'dunes' ? C.mat('sand') : C.mat(G.biome.ground === 'snow' ? 'snow' : 'grass');
      for (let k = 0; k < 2; k++) {
        const pts = [0, H], n = 14, amp = H * (k ? 0.07 : 0.13), ph = rng.range(0, 6), fq = rng.range(0.5, 0.9);
        for (let i = 0; i <= n; i++) pts.push(Math.round((i / n) * w), Math.round(H - amp * (0.55 + 0.45 * Math.sin(ph + i * fq) + 0.2 * Math.sin(ph * 2 + i * fq * 2.3))));
        pts.push(w, H);
        const ho = [['P', pts, g[k ? 2 : 1]]];
        if (f === 'dunes' && !G.style.flat) ho.push(['D', 0, Math.round(H - amp), w, Math.round(amp), g[k ? 3 : 2], 5, 1]);
        out.push(L(f === 'dunes' ? (k ? 'dunas' : 'dunas lejanas') : (k ? 'colinas' : 'colinas lejanas'), 'far', ho, { f: [sky, k ? 22 : 45] }));
      }
    } else if (f === 'mesas') {
      const m = C.mat(G.biome.ground || 'mars');
      const n = Math.max(2, Math.round(3 * G.detail));
      for (let i = 0; i < n; i++) {
        const mw = Math.round(w * rng.range(0.14, 0.3)), mh = Math.round(H * rng.range(0.1, 0.24)), x = Math.round(((i + rng.range(0, 0.7)) / n) * w - mw * 0.3), s = Math.max(1, Math.round(mw * 0.12));
        o.push(['P', [x, H, x + s, H - mh, x + mw - s, H - mh + rng.int(0, 1), x + mw, H], m[1]]);
        o.push(['P', [x, H, x + s, H - mh, x + Math.round(mw * 0.45), H - mh, x + Math.round(mw * 0.35), H], m[2]]);
      }
      out.push(L('mesetas', 'far', o, { f: [sky, 40] }));
    } else if (f === 'treeline') {
      const p = C.mat('pine');
      for (let k = 0; k < 2; k++) {
        const pts = [0, H], step = Math.max(2, Math.round(w / 24));
        const base = H * (k ? 0.16 : 0.27);
        for (let x = 0; x <= w + step; x += step) {
          pts.push(x, Math.round(H - base * rng.range(0.55, 0.8)));
          pts.push(x + (step >> 1), Math.round(H - base * rng.range(0.85, 1.15)));
        }
        pts.push(w + step, H);
        while (pts.length > 126) pts.splice(pts.length - 4, 2);
        out.push(L(k ? 'arboleda' : 'arboleda lejana', 'far', [['P', pts, p[k ? 1 : 2]]], { f: [sky, k ? 22 : 50] }));
      }
    } else if (f === 'skyline') {
      for (let k = 0; k < 2; k++) {
        const bo = [], lo = [], b = C.hsl(232, 22, k ? 22 : 34);
        const neon = G.biome.neon;
        const winC = neon ? [C.rgb([255, 90, 200]), C.rgb([70, 230, 240]), C.rgb([255, 220, 120])] : [C.rgb([255, 226, 140])];
        const tile = Math.max(3, Math.round(w / 40));
        winC.forEach((c, i) => { G.sprites['win' + k + i] = { w: tile, h: tile, o: [['R', 1, 1, Math.max(1, tile - 2), Math.max(1, tile >> 1), c]] }; });
        let x = -rng.int(0, 3);
        while (x < w) {
          const bw = Math.max(4, Math.round(w * rng.range(0.07, 0.15))), bh = Math.round(H * (k ? rng.range(0.3, 0.72) : rng.range(0.45, 0.9)) * (neon ? 1.05 : 0.8));
          bo.push(['R', x, H - bh, bw, bh, b[2]], ['R', x, H - bh, Math.max(1, Math.round(bw * 0.25)), bh, b[3]]);
          if (rng.chance(0.4)) bo.push(['R', x + (bw >> 1), H - bh - Math.round(bh * 0.12), 1, Math.round(bh * 0.12), b[2]]);
          if (rng.chance(0.3)) bo.push(['R', x + Math.round(bw * 0.2), H - bh - 2, Math.round(bw * 0.6), 2, b[2]]);
          if (G.night || neon) {
            lo.push(['B', x + 1, H - bh + 2, bw - 2, bh - 3, 'win' + k + rng.int(0, winC.length - 1)]);
            if (neon && k && rng.chance(0.5)) {
              const sc = rng.pick(winC), sy = H - bh + rng.int(3, Math.max(4, bh >> 1));
              lo.push(['R', x + 1, sy, Math.max(2, bw - 2), Math.max(2, Math.round(tile * 0.8)), sc]);
            }
          }
          x += bw + rng.int(0, 2);
        }
        if (lo.length) lo.push(['T', 0, 0, w, H, -1, k ? 35 : 55, rng.int(1, 99999)]);
        out.push(L(k ? 'edificios' : 'edificios lejanos', 'far', bo, { f: [sky, k ? 12 : 45] }));
        if (lo.length) out.push(L(k ? 'ventanas' : 'ventanas lejanas', 'far', lo, { nl: 1, a: k ? 1 : 0.6 }));
      }
    } else if (f === 'waterfall') {
      // Acantilado con cascada que cae al lago, en el lado contrario al astro.
      const side = G.moonSide > 0 ? -1 : 1, st = C.mat('stone'), wt = C.mat('water'), foam = C.rgb([235, 245, 255]);
      const x0 = side < 0 ? 0 : w, cw = Math.round(w * 0.42), top = Math.round(H * 0.32), fx = side < 0 ? Math.round(w * 0.3) : w - Math.round(w * 0.3), fw = Math.max(3, Math.round(w * 0.07));
      const edge = (t) => Math.round(x0 - side * cw * t);
      o.push(['P', [x0, H + 1, x0, top, edge(0.55), top - Math.round(H * 0.04), edge(0.8), top + Math.round(H * 0.08), edge(1), H + 1], st[1]]);
      o.push(['P', [x0, H + 1, x0, top, edge(0.4), top, edge(0.45), H + 1], st[2]]);
      o.push(['T', Math.min(x0, edge(1)), top, cw, H - top + 1, st[3], 8, 31, 1], ['T', Math.min(x0, edge(1)), top, cw, H - top + 1, st[0], 6, 37, 1]);
      const lush = C.mat('leaf'); o.push(['E', edge(0.5), top, Math.round(cw * 0.35), Math.max(2, Math.round(H * 0.05)), lush[2]], ['E', edge(0.2), top + 1, Math.round(cw * 0.25), Math.max(2, Math.round(H * 0.04)), lush[3]]);
      out.push(L('acantilado', 'far', o, { f: [sky, 12] }));
      const wo = [['R', fx - (fw >> 1), top, fw, H - top + 2, wt[3]], ['R', fx - (fw >> 1), top, 1, H - top + 2, wt[4]]];
      const F = G.frames > 1 ? G.frame : 0;
      for (let y = top + ((F * 3) % 6); y < H; y += 6) wo.push(['R', fx - (fw >> 1) + 1, y, fw - 2, 2, wt[4]]);
      wo.push(['E', fx, H + 1, fw, Math.max(1, Math.round(fw * 0.4)), foam], ['T', fx - fw * 2, H - 3, fw * 4, 5, foam, 25, 11 + F]);
      out.push(L('cascada', 'far', wo, { nl: 1 }));
    } else if (f === 'volcano') {
      const m = C.mat('ash'), lava = C.rgb([255, 120, 40]), lava2 = C.rgb([255, 214, 90]);
      const px = Math.round(w * (G.moonSide > 0 ? 0.34 : 0.66)), ph = Math.round(H * 0.55), py = H - ph, wl = Math.round(w * 0.36), top = Math.max(2, Math.round(w * 0.05));
      o.push(['P', [px - top, py, px - Math.round(wl * 0.45), py + Math.round(ph * 0.6), px - wl, H, px + wl, H, px + Math.round(wl * 0.45), py + Math.round(ph * 0.6), px + top, py], m[2]]);
      o.push(['P', [px, py, px + top, py, px + Math.round(wl * 0.45), py + Math.round(ph * 0.6), px + wl, H, px + Math.round(wl * 0.2), H], m[1], 1]);
      out.push(L('volcán', 'far', o, { f: [sky, 15] }));
      const lo = [['R', px - top + 1, py, 2 * top - 1, Math.max(1, Math.round(ph * 0.05)), lava2]];
      let lx = px - Math.round(top * 0.4), ly = py;
      for (let i = 0; i < 5; i++) { const nx = lx + rng.int(-2, 2) * Math.max(1, Math.round(w / 64)), ny = ly + Math.round(ph * 0.14); lo.push(['L', lx, ly, nx, ny, lava], ['L', lx + 1, ly, nx + 1, ny, i < 2 ? lava2 : lava]); lx = nx; ly = ny; }
      out.push(L('lava', 'far', lo, { nl: 1 }));
      const smoke = C.hsl(260, 8, 42), so = [];
      for (let i = 0; i < 4; i++) so.push(['E', px + Math.round(i * top * 0.5 * G.moonSide), py - Math.round((i + 0.6) * ph * 0.12), Math.round(top * (0.7 + i * 0.3)), Math.round(top * (0.5 + i * 0.2)), smoke[i & 1 ? 2 : 3]]);
      out.push(L('humo', 'far', so, { nl: 1, a: 0.75 }));
      out.push(L('fulgor', 'far', [['E', px, py, top * 3, Math.round(top * 2), lava]], { nl: 1, a: 0.22 }));
    }
  }
  return out;
}

// ---------- Suelo ----------
export function genGround(G) {
  const { w, h, H, C, biome } = G, rng = G.rng('ground');
  if (biome.noGround) return [];
  const o = [];
  const flat = G.style.flat;
  if (G.layoutKind === 'portrait') {
    const cx = w >> 1, gy = G.baseY;
    return [L('sombra', 'ground', [['E', cx, gy, Math.round(w * 0.26), Math.max(1, Math.round(h * 0.035)), C.dark]], { nl: 1, a: 0.3 })];
  }
  if (G.layoutKind === 'iso') {
    const g = C.mat(biome.ground || 'grass'), d = C.mat(biome.ground === 'snow' ? 'stone' : 'dirt');
    const { cx, cy, rx, ry, th } = G.iso;
    o.push(['P', [cx - rx, cy, cx, cy + ry, cx, cy + ry + th, cx - Math.round(rx * 0.6), cy + Math.round(th * 1.4) + Math.round(ry * 0.35), cx - rx, cy + Math.round(th * 0.6)], d[2]]);
    o.push(['P', [cx + rx, cy, cx, cy + ry, cx, cy + ry + th, cx + Math.round(rx * 0.55), cy + Math.round(th * 1.5) + Math.round(ry * 0.3), cx + rx, cy + Math.round(th * 0.6)], d[1]]);
    if (!flat) o.push(['T', cx - rx, cy, 2 * rx, ry + 2 * th, d[0], 8, rng.int(1, 9999), 1]);
    o.push(['P', [cx - rx, cy, cx, cy - ry, cx + rx, cy, cx, cy + ry], g[2]]);
    o.push(['L', cx - rx, cy, cx, cy + ry, g[1]], ['L', cx + rx, cy, cx, cy + ry, g[1]], ['L', cx - rx, cy, cx, cy - ry, g[3]]);
    const top = [['P', [cx - rx + 2, cy, cx, cy - ry + 1, cx + rx - 2, cy, cx, cy + ry - 1], g[2]]];
    if (!flat) top.push(['T', cx - rx, cy - ry, 2 * rx, 2 * ry, g[3], 7, rng.int(1, 9999), 1], ['T', cx - rx, cy - ry, 2 * rx, 2 * ry, g[1], 6, rng.int(1, 9999), 1]);
    return [L('isla', 'ground', o, { sh: 0 }), L('césped', 'ground', top)];
  }
  const water = G.waterKind;
  const g = C.hsl(...shiftL(MATERIALS[biome.ground], biome.groundL || 0));
  let top = H;
  if (water === 'sea') {
    // Sin orilla: solo islotes bajo lo que deba apoyarse en tierra.
    const r = C.mat('stone');
    for (const s of G.islets) {
      const rw = Math.round(s.w * 0.75), y = s.y;
      o.push(['P', [s.x - rw, y + Math.round(rw * 0.35), s.x - Math.round(rw * 0.7), y - 1, s.x + Math.round(rw * 0.6), y - 2, s.x + rw, y + Math.round(rw * 0.35)], r[2]]);
      o.push(['P', [s.x, y - 2, s.x + Math.round(rw * 0.6), y - 2, s.x + rw, y + Math.round(rw * 0.35), s.x + Math.round(rw * 0.2), y + Math.round(rw * 0.35)], r[1]]);
    }
    return o.length ? [L('islote', 'ground', o, { sh: G.style.sh })] : [];
  }
  if (water) {
    top = G.shoreY;
    const pts = [0, h], n = 12, ph = rng.range(0, 6);
    for (let i = 0; i <= n; i++) pts.push(Math.round((i / n) * w), Math.round(top + Math.sin(ph + i * 0.8) * (h - top) * 0.12 + (h - top) * 0.1));
    pts.push(w, h);
    o.push(['P', pts, g[2]]);
    if (!flat) {
      o.push(['G', 0, top, w, h - top, [g[3], g[2], g[1]], 0, 1]);
      o.push(['T', 0, top, w, h - top, g[3], 2, rng.int(1, 99999), 1], ['T', 0, top, w, h - top, g[1], 2, rng.int(1, 99999), 1]);
    }
    return [L('orilla', 'ground', o, { sh: G.style.sh })];
  }
  o.push(['R', 0, H, w, h - H, g[2]]);
  if (biome.urban) {
    const side = C.mat('concrete'), sw = Math.max(2, Math.round((h - H) * 0.22));
    o.push(['R', 0, H, w, sw, side[2]], ['R', 0, H, w, 1, side[3]], ['R', 0, H + sw - 1, w, 1, side[0]]);
    if (!flat) o.push(['G', 0, H + sw, w, h - H - sw, [g[3], g[2], g[1]]]);
    const ly = H + sw + Math.round((h - H - sw) * 0.5), dash = Math.max(3, Math.round(w / 12)), lc = C.rgb([214, 200, 120]);
    for (let x = dash >> 1; x < w; x += dash * 2) o.push(['R', x, ly, dash, Math.max(1, Math.round(h / 96)), lc]);
    return [L('calle', 'ground', o, biome.wet ? { rf: [H + sw, 32] } : {})];
  }
  if (!flat) {
    o.push(['G', 0, H, w, h - H, [g[3], g[2], g[2], g[1]]]);
    o.push(['T', 0, H + 1, w, h - H, g[3], 2, rng.int(1, 99999)], ['T', 0, H + 2, w, h - H, g[1], 2, rng.int(1, 99999)]);
    o.push(['R', 0, H, w, 1, g[3]]);
    if (biome.ground === 'grass') o.push(['T', 0, H - 1, w, 1, g[2], 45, rng.int(1, 99999)]);
  }
  // Camino hacia la estructura principal.
  if (G.path) {
    const d = C.mat(biome.ground === 'snow' ? 'stone' : 'dirt'), { x0, y0, x1, wide } = G.path;
    const mx = Math.round((x0 + x1) / 2 + (x1 - x0) * 0.2), my = Math.round((y0 + h) / 2);
    o.push(['P', [x0 - 1, y0, x0 + 2, y0, mx + Math.round(wide * 0.5), my, x1 + wide, h, x1 - wide, h, mx - Math.round(wide * 0.5), my], d[3]]);
    
  }
  return [L('suelo', 'ground', o)];
}
const shiftL = (m, d) => [m[0], m[1], m[2] + d];

// ---------- Agua ----------
export function genWater(G) {
  if (!G.waterKind) return [];
  const { w, h, H, C } = G, rng = G.rng('water');
  const wt = C.mat('water');
  const bottom = G.waterKind === 'sea' ? h : Math.min(h, G.shoreY + Math.round((h - G.shoreY) * 0.35));
  const o = [];
  if (G.style.flat) o.push(['R', 0, H, w, bottom - H, wt[2]]);
  else o.push(['G', 0, H, w, bottom - H, [wt[3], wt[2], wt[2], wt[1]]]);
  o.push(['R', 0, H, w, 1, wt[4]]);
  const n = Math.round(((w * (bottom - H)) / 160) * G.detail);
  // Animado: los brillos alternan entre dos estados (rielar del agua).
  const drng = G.frames > 1 ? G.rng('water:' + (G.frame & 1)) : rng;
  for (let i = 0; i < n; i++) {
    const y = drng.int(H + 2, bottom - 1), len = Math.max(1, Math.round((1 + ((y - H) / (bottom - H)) * 4) * (w / 64)));
    o.push(['R', drng.int(0, w - 1), y, len, 1, wt[drng.chance(0.5) ? 3 : 4]]);
  }
  const layers = [L(G.waterKind === 'lake' ? 'lago' : 'mar', 'water', o, { rf: [H + 1, G.style.flat ? 22 : 38] })];
  // Estela de luz del astro sobre el agua.
  if (G.bodyX != null && !G.style.flat) {
    const lo = [], c = C.rgb(G.spec.celestial.includes('moon') ? [238, 236, 214] : [255, 220, 140]);
    for (let y = H + 2; y < bottom; y += 2) {
      const sp = Math.round((1 + (y - H) * 0.12) * (w / 64));
      lo.push(['R', G.bodyX - sp + drng.int(-1, 1), y, Math.max(1, sp * 2 - drng.int(0, 2)), 1, c]);
    }
    layers.push(L('destello', 'water', lo, { nl: 1, a: 0.55 }));
  }
  if (G.waterKind === 'beach' || G.waterKind === 'lake') {
    const foam = [], c = C.rgb([240, 248, 255]);
    foam.push(['T', 0, G.shoreY + Math.round((h - G.shoreY) * 0.05), w, Math.max(2, Math.round((h - G.shoreY) * 0.2)), c, G.waterKind === 'beach' ? 30 : 8, rng.int(1, 9999)]);
    layers.push(L('espuma', 'water', foam, { a: 0.6 }));
  }
  return layers;
}

// ---------- Clima y partículas ----------
export function genWeather(G) {
  const { w, h, H, C, spec } = G, rng = G.rng('weather');
  const out = [];
  const anim = G.frames > 1, F = G.frame;
  // Con animación la tesela es múltiplo del nº de fotogramas para que el ciclo cierre sin salto.
  let tile = Math.max(8, Math.min(32, Math.round(w / 4)));
  if (anim) tile = Math.max(G.frames * 2, Math.round(tile / G.frames) * G.frames);
  const off = anim ? (F * tile) / G.frames : 0;
  const scroll = (id, dx, dy) => (anim ? ['B', -2 * tile + dx, -2 * tile + dy, w + 4 * tile, h + 4 * tile, id] : ['B', 0, 0, w, h, id]);
  if (spec.weather.includes('rain') || spec.weather.includes('storm')) {
    const c = C.rgb([176, 196, 235]), so = [], len = Math.max(2, Math.round(tile / 5));
    for (let i = 0; i < Math.round(4 * G.detail) + 2; i++) { const x = rng.int(0, tile - 1), y = rng.int(0, tile - 1); so.push(['L', x, y, x - Math.max(1, len >> 1), y + len, c]); }
    G.sprites['rain'] = { w: tile, h: tile, o: so };
    out.push(L('lluvia', 'weather', [scroll('rain', -off, 2 * off)], { nl: 1, a: 0.5 }));
  }
  if (spec.weather.includes('storm')) {
    const c = C.rgb([255, 252, 220]), o = [];
    let x = Math.round(w * (G.moonSide > 0 ? 0.3 : 0.7)), y = 0;
    const seg = Math.max(3, Math.round(H / 6));
    while (y < H * 0.8) { const nx = x + rng.int(-seg, seg), ny = y + seg; o.push(['L', x, y, nx, ny, c]); if (w >= 96) o.push(['L', x + 1, y, nx + 1, ny, c]); if (rng.chance(0.3)) o.push(['L', nx, ny, nx + rng.int(-seg, seg), ny + (seg >> 1), c]); x = nx; y = ny; }
    if (!anim || F === 0) out.push(L('relámpago', 'weather', o, { nl: 1 }));
  }
  if (spec.weather.includes('snow')) {
    const c = C.rgb([250, 252, 255]), so = [];
    for (let i = 0; i < Math.round(5 * G.detail) + 2; i++) so.push(['R', rng.int(0, tile - 1), rng.int(0, tile - 1), w >= 128 && rng.chance(0.3) ? 2 : 1, 1, c]);
    G.sprites['snowfall'] = { w: tile, h: tile, o: so };
    out.push(L('nevada', 'weather', [scroll('snowfall', [0, 1, 0, -1][F % 4], off)], { nl: 1, a: 0.9 }));
  }
  if (spec.weather.includes('fog')) {
    const c = C.rgb([226, 232, 240]), o = [], y0 = Math.round(H * 0.55), bands = 5, bh = Math.max(2, Math.round((h * 0.5) / bands));
    for (let i = 0; i < bands; i++) o.push(['D', 0, y0 + i * bh, w, bh, c, Math.min(15, 4 + i * 3)]);
    out.push(L('niebla', 'weather', o, { nl: 1, a: 0.42 }));
  }
  if (spec.fx.includes('fireflies')) {
    const c = C.rgb([232, 255, 130]), o = [], go = [];
    const n = Math.round(((w * h) / 380) * Math.min(1.6, G.detail));
    const yTop = G.biome.noGround ? 0 : H * 0.55;
    for (let i = 0; i < n; i++) {
      let x = rng.int(1, w - 2); const y = rng.int(Math.round(yTop), h - 3);
      if (anim) { if ((i + F) % 3 === 0) continue; x += [0, 1, 0, -1][(i + F) % 4]; }
      o.push(['R', x, y, 1, 1, c]);
      if (w >= 48) go.push(['R', x - 1, y, 3, 1, c], ['R', x, y - 1, 1, 3, c]);
    }
    if (go.length) out.push(L('halo de luciérnagas', 'weather', go, { nl: 1, a: 0.3 }));
    out.push(L('luciérnagas', 'weather', o, { nl: 1 }));
  }
  if (spec.fx.includes('sparkles')) {
    const cols = [C.rgb([255, 240, 170]), C.rgb([200, 170, 255]), C.rgb([150, 240, 255])], o = [];
    const n = Math.round(((w * h) / 700) * Math.min(1.6, G.detail));
    for (let i = 0; i < n; i++) {
      const x = rng.int(1, w - 2), y = rng.int(2, h - 3), c = rng.pick(cols), big = rng.chance(0.4);
      if (anim && (i + F) % 2) continue;
      o.push(['R', x, y, 1, 1, c]);
      if (w >= 64 && big) o.push(['R', x - 1, y, 3, 1, c], ['R', x, y - 1, 1, 3, c]);
    }
    out.push(L('destellos mágicos', 'weather', o, { nl: 1, a: 0.85 }));
  }
  return out;
}

export { mix };
