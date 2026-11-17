// Colores, rampas con desplazamiento de tono y gestión de paletas indexadas.
import { LIMITS, mapSceneColors } from './pixelScene.js';

export const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

export function hslToRgb(h, s, l) {
  h = ((h % 360) + 360) % 360; s = clamp(s, 0, 100) / 100; l = clamp(l, 0, 100) / 100;
  const k = (n) => (n + h / 30) % 12;
  const a = s * Math.min(l, 1 - l);
  const f = (n) => Math.round(255 * (l - a * Math.max(-1, Math.min(k(n) - 3, 9 - k(n), 1))));
  return [f(0), f(8), f(4)];
}

export function rgbToHsl([r, g, b]) {
  r /= 255; g /= 255; b /= 255;
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn;
  let h = 0;
  if (d) {
    if (mx === r) h = ((g - b) / d) % 6; else if (mx === g) h = (b - r) / d + 2; else h = (r - g) / d + 4;
    h *= 60; if (h < 0) h += 360;
  }
  const l = (mx + mn) / 2;
  const s = d ? d / (1 - Math.abs(2 * l - 1)) : 0;
  return [h, s * 100, l * 100];
}

export const mix = (a, b, t) => [0, 1, 2].map((i) => Math.round(a[i] + (b[i] - a[i]) * t));
export const toHex = (c) => '#' + c.map((v) => v.toString(16).padStart(2, '0')).join('');
export const fromHex = (h) => { const n = parseInt(h.replace('#', ''), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255]; };

function hueToward(h, target, deg) {
  let d = ((target - h + 540) % 360) - 180;
  return h + clamp(d, -deg, deg);
}

// Rampa de 5 tonos (oscuro → claro). Las sombras viran hacia el azul-violeta
// y las luces hacia el amarillo, como en el pixel art pintado a mano.
const OFFS = [-25, -12, 0, 11, 21];
export function makeRamp(h, s, l) {
  return OFFS.map((d) => {
    const t = d < 0 ? -d / 25 : d / 21;
    const hh = d < 0 ? hueToward(h, 255, 22 * t) : hueToward(h, 55, 14 * t);
    const ss = d < 0 ? s * (1 + 0.12 * t) : s * (1 - 0.3 * t);
    const scale = l > 80 ? 0.6 : l < 25 ? 0.7 : 1;
    return hslToRgb(hh, clamp(ss, 0, 100), clamp(l + d * scale, 3, 97));
  });
}

export function colorDist(a, b) {
  const dr = a[0] - b[0], dg = a[1] - b[1], db = a[2] - b[2];
  return 2 * dr * dr + 4 * dg * dg + 3 * db * db;
}

export function nearestIndex(pal, rgb) {
  let best = 0, bd = Infinity;
  for (let i = 0; i < pal.length; i++) { const d = colorDist(pal[i], rgb); if (d < bd) { bd = d; best = i; } }
  return best;
}

// Construye la paleta de una escena sin duplicados.
export class PaletteBuilder {
  constructor(pal = [], rmp = [], max = LIMITS.maxPal) { this.pal = pal; this.rmp = rmp; this.max = max; this.cache = new Map(); }
  idx(rgb) {
    for (let i = 0; i < this.pal.length; i++) { const p = this.pal[i]; if (p[0] === rgb[0] && p[1] === rgb[1] && p[2] === rgb[2]) return i; }
    if (this.pal.length >= this.max) return nearestIndex(this.pal, rgb);
    this.pal.push([rgb[0], rgb[1], rgb[2]]);
    return this.pal.length - 1;
  }
  // map reduce el número de tonos distintos según el estilo (p. ej. [0,0,2,4,4]).
  ramp(h, s, l, map = [0, 1, 2, 3, 4]) {
    const key = [Math.round(h), Math.round(s), Math.round(l), map.join('')].join(',');
    if (this.cache.has(key)) return this.cache.get(key);
    const cols = makeRamp(h, s, l);
    const out = map.map((m) => this.idx(cols[m]));
    const uniq = [...new Set(out)];
    if (uniq.length > 1) this.rmp.push(uniq);
    this.cache.set(key, out);
    return out;
  }
}

// Elimina colores sin uso y fusiona duplicados, reindexando toda la escena.
export function compactPalette(scene) {
  const used = new Set();
  mapSceneColors({ ...scene, rmp: undefined }, (c) => { used.add(c); return c; });
  const map = new Map(), pal = [], seen = new Map();
  scene.pal.forEach((c, i) => {
    if (!used.has(i)) return;
    const key = c.join(',');
    if (!seen.has(key)) { seen.set(key, pal.length); pal.push(c); }
    map.set(i, seen.get(key));
  });
  if (!pal.length) pal.push([0, 0, 0]);
  const rmp = (scene.rmp || []).map((r) => [...new Set(r.filter((c) => map.has(c)).map((c) => map.get(c)))]).filter((r) => r.length > 1);
  const out = mapSceneColors({ ...scene, rmp: undefined }, (c) => map.get(c) ?? 0);
  out.pal = pal;
  out.rmp = rmp;
  return out;
}

const P = (s) => s.split(',').map(fromHex);

// Paletas de cuantización seleccionables. "auto" = sin restricción.
export const PALETTES = {
  auto: { name: 'Automática (libre)', colors: null },
  pf16: { name: 'PixelForge 16', colors: P('141021,3b2a4d,6b3a5b,b0455a,e8764a,f5c56b,f7f0d0,9bd16a,3f9e5c,22606b,1f3a6e,3c6fd1,5fc4ee,c9d6e3,7d8ca3,454c66') },
  retro: { name: 'Consola 8-bit', colors: P('000000,fcfcfc,bcbcbc,7c7c7c,a80020,f83800,fca044,f8b800,f8d878,00a800,58d854,005800,0058f8,3cbcfc,0000bc,6844fc,d800cc,f878f8,503000,ac7c00,00e8d8,008888,f0d0b0,2c2c2c') },
  gb4: { name: 'Portátil 4 verdes', colors: P('0f2a1d,2f5d3a,7fa650,d5e8a0') },
  neon: { name: 'Neón', colors: P('0d0221,1b0b3a,3d1a78,7b2ff7,f72585,ff6ad5,00f5d4,00bbf9,fee440,0a1128,14213d,2a3b6b,e0e0ff,9d4edd,240046,ff9e00') },
  pastel: { name: 'Pastel', colors: P('f8edeb,fcd5ce,ffb4a2,e5989b,b5838d,6d6875,cdeac0,a8dadc,84a9c0,457b9d,fff1c1,ffd6a5,cdb4db,bde0fe,ffffff,3d405b') },
  tierra: { name: 'Tierra', colors: P('1a1410,2b1d0e,4a3320,6f4e2c,a0703a,c99a5b,e8cc9a,f6ecd0,5a6b2f,86943c,b8bf66,3b4a3f,6d8a7c,a9c4b0,8a3b23,c5562d') },
  gris: { name: 'Escala de grises', colors: P('0a0a0a,2a2a2a,4a4a4a,6e6e6e,929292,b6b6b6,dadada,f5f5f5') },
  bit1: { name: '1-bit', colors: P('12131a,f2f0e6') },
};
