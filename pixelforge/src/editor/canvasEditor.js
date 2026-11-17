// Retoque manual píxel a píxel. Los trazos se guardan en una capa propia
// ("retoques") como una única operación M (matriz), de modo que siguen siendo
// datos PixelScene compactos e indexados.
import { SYMS, LIMITS } from '../core/pixelScene.js';
import { renderLayerIndices } from '../core/renderer.js';
import { EMPTY } from '../core/lighting.js';

export const RETOUCH_KEY = 'user';

function decode(rows) { return rows.map((r) => (typeof r === 'string' ? [...r].map((ch) => (ch === '.' ? -1 : SYMS.indexOf(ch))) : r.slice())); }
function encode(rows) {
  const fits = rows.every((r) => r.every((c) => c < SYMS.length));
  return fits ? rows.map((r) => r.map((c) => (c < 0 ? '.' : SYMS[c])).join('')) : rows;
}

export function retouchIndex(scene) { return scene.layers.findIndex((l) => l.k === RETOUCH_KEY); }

// Pinta (o borra con color = -1) una lista de píxeles [[x,y],...].
export function paintPixels(scene, pixels, color) {
  let li = retouchIndex(scene);
  const layers = scene.layers.slice();
  if (li < 0) {
    if (color < 0 || layers.length >= LIMITS.maxLayers) return scene;
    const blank = Array.from({ length: scene.h }, () => '.'.repeat(scene.w));
    layers.push({ n: 'retoques', k: RETOUCH_KEY, nl: 1, o: [['M', 0, 0, blank]] });
    li = layers.length - 1;
  }
  const layer = layers[li];
  const mi = layer.o.findIndex((op) => op[0] === 'M' && op[1] === 0 && op[2] === 0);
  if (mi < 0) return scene;
  const rows = decode(layer.o[mi][3]);
  let changed = false;
  for (const [x, y] of pixels) {
    if (x < 0 || y < 0 || x >= scene.w || y >= scene.h || !rows[y]) continue;
    const c = color < 0 ? -1 : Math.min(scene.pal.length - 1, color);
    if (rows[y][x] !== c) { rows[y][x] = c; changed = true; }
  }
  if (!changed) return scene;
  const o = layer.o.slice();
  o[mi] = ['M', 0, 0, encode(rows)];
  layers[li] = { ...layer, o };
  return { ...scene, layers };
}

// Cuentagotas: índice de paleta de la capa visible más alta en (x, y).
export function pickIndex(scene, x, y) {
  if (x < 0 || y < 0 || x >= scene.w || y >= scene.h) return -1;
  for (let i = scene.layers.length - 1; i >= 0; i--) {
    const l = scene.layers[i];
    if (l.v === 0) continue;
    const c = renderLayerIndices(scene, l)[y * scene.w + x];
    if (c !== EMPTY) return c;
  }
  return scene.bg;
}

// Píxeles de una línea entre dos puntos (para trazos continuos al arrastrar).
export function strokePixels(x0, y0, x1, y1) {
  const out = [];
  let dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
  for (;;) { out.push([x0, y0]); if (x0 === x1 && y0 === y1) break; const e2 = 2 * err; if (e2 >= dy) { err += dy; x0 += sx; } if (e2 <= dx) { err += dx; y0 += sy; } }
  return out;
}
