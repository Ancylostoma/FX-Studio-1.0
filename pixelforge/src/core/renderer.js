// Renderizador puro: PixelScene → búfer RGBA. No depende del DOM, de modo que
// funciona igual en el navegador, en un Worker y en Node (pruebas).
// Cada capa se dibuja primero en un búfer de ÍNDICES de paleta; el color RGB
// solo aparece en la composición final.
import { SYMS } from './pixelScene.js';
import { rasterRect, rasterLine, rasterEllipse, rasterPolygon } from './geometry.js';
import { bayer, ditherBand } from './dithering.js';
import { hash2 } from './randomSeed.js';
import { EMPTY, litColor, fadeColor, rampTable, shiftRamp, shadeLayer, outlineLayer } from './lighting.js';
import { nearestIndex } from './paletteManager.js';

function rasterOps(ops, buf, w, h, cx) {
  for (const op of ops) {
    const spec = op[0];
    const plotter = (c, mode = 0) => (x, y) => {
      if (x < 0 || y < 0 || x >= w || y >= h) return;
      const i = y * w + x;
      if (mode === 1 && buf[i] === EMPTY) return;
      if (mode === 2 && buf[i] !== EMPTY) return;
      buf[i] = c < 0 ? EMPTY : c;
    };
    switch (spec) {
      case 'R': rasterRect(op[1], op[2], op[3], op[4], plotter(op[5], op[6])); break;
      case 'L': rasterLine(op[1], op[2], op[3], op[4], plotter(op[5], op[6])); break;
      case 'E': rasterEllipse(op[1], op[2], op[3], op[4], plotter(op[5], op[6])); break;
      case 'P': rasterPolygon(op[1], plotter(op[2], op[3])); break;
      case 'M': {
        const rows = op[3];
        for (let j = 0; j < rows.length; j++) {
          const row = rows[j];
          for (let i = 0; i < row.length; i++) {
            const c = typeof row === 'string' ? (row[i] === '.' ? -1 : SYMS.indexOf(row[i])) : row[i];
            if (c >= 0) plotter(c)(op[1] + i, op[2] + j);
          }
        }
        break;
      }
      case 'S': {
        const sp = cx.sprite(op[1]);
        if (!sp) break;
        const fx = op[4] & 1, fy = op[4] & 2, put = plotter(0);
        for (let j = 0; j < sp.h; j++) for (let i = 0; i < sp.w; i++) {
          const c = sp.buf[(fy ? sp.h - 1 - j : j) * sp.w + (fx ? sp.w - 1 - i : i)];
          if (c !== EMPTY) { const x = op[2] + i, y = op[3] + j; if (x >= 0 && y >= 0 && x < w && y < h) buf[y * w + x] = c; }
        }
        void put;
        break;
      }
      case 'B': {
        const sp = cx.sprite(op[5]);
        if (!sp) break;
        const mode = op[6] || 0;
        for (let j = 0; j < op[4]; j++) for (let i = 0; i < op[3]; i++) {
          const c = sp.buf[(j % sp.h) * sp.w + (i % sp.w)];
          if (c !== EMPTY) plotter(c, mode)(op[1] + i, op[2] + j);
        }
        break;
      }
      case 'G': {
        const cols = op[5], dir = op[6] || 0, mode = op[7] || 0, n = cols.length;
        const span = Math.max(1, (dir ? op[3] : op[4]) - 1);
        for (let j = 0; j < op[4]; j++) for (let i = 0; i < op[3]; i++) {
          const x = op[1] + i, y = op[2] + j;
          const t = ((dir ? i : j) / span) * (n - 1);
          plotter(cols[ditherBand(t, n, x, y, cx.dth)], mode)(x, y);
        }
        break;
      }
      case 'D': {
        const lvl = op[6] ?? 8, put = plotter(op[5], op[7]);
        for (let j = 0; j < op[4]; j++) for (let i = 0; i < op[3]; i++) {
          const x = op[1] + i, y = op[2] + j;
          if (bayer(x, y) < lvl) put(x, y);
        }
        break;
      }
      case 'T': {
        const put = plotter(op[5], op[8]);
        for (let j = 0; j < op[4]; j++) for (let i = 0; i < op[3]; i++) {
          const x = op[1] + i, y = op[2] + j;
          if (hash2(x, y, op[7]) % 100 < op[6]) put(x, y);
        }
        break;
      }
      case 'H': {
        for (let j = 0; j < op[4]; j++) for (let i = 0; i < op[3]; i++) {
          const x = op[1] + i, y = op[2] + j;
          if (x < 0 || y < 0 || x >= w || y >= h) continue;
          const c = buf[y * w + x];
          if (c !== EMPTY) buf[y * w + x] = shiftRamp(c, op[5], cx.rt);
        }
        break;
      }
    }
  }
}

function makeContext(scene) {
  const rt = rampTable(scene);
  const cache = new Map();
  const cx = {
    rt,
    dth: scene.fx?.dth ?? 2,
    ld: scene.fx?.ld ?? -1,
    sprite(id) {
      if (cache.has(id)) return cache.get(id);
      const def = scene.sprites?.[id];
      if (!def) return null;
      const buf = new Uint8Array(def.w * def.h).fill(EMPTY);
      const sp = { w: def.w, h: def.h, buf };
      cache.set(id, sp);
      rasterOps(def.o, buf, def.w, def.h, { ...cx, sprite: () => null });
      return sp;
    },
  };
  return cx;
}

// Búfer de índices de una capa (255 = transparente), con sombreado y contorno.
export function renderLayerIndices(scene, layer, cx = makeContext(scene), frame = 0) {
  const { w, h } = scene;
  let buf = new Uint8Array(w * h).fill(EMPTY);
  // Fotograma 0 = layer.o; los siguientes están en layer.fo.
  const fi = layer.fo ? frame % (layer.fo.length + 1) : 0;
  rasterOps(fi ? layer.fo[fi - 1] : layer.o, buf, w, h, cx);
  const mv = layer.mv ? layer.mv[frame % layer.mv.length] : null;
  if (mv && (mv[0] || mv[1])) {
    const moved = new Uint8Array(w * h).fill(EMPTY);
    for (let y = 0; y < h; y++) { const sy = y - mv[1]; if (sy < 0 || sy >= h) continue; for (let x = 0; x < w; x++) { const sx = x - mv[0]; if (sx >= 0 && sx < w) moved[y * w + x] = buf[sy * w + sx]; } }
    buf = moved;
  }
  if (layer.sh) shadeLayer(buf, w, h, cx.rt, cx.ld);
  if (layer.ol !== undefined) outlineLayer(buf, w, h, layer.ol, cx.rt, cx.ld);
  return buf;
}

// Paleta RGB efectiva de una capa: luz ambiental, bruma y cuantización.
function layerPalette(scene, layer, cache) {
  const key = (layer.nl ? 'n' : 'l') + (layer.f ? layer.f.join('_') : '');
  if (cache.has(key)) return cache.get(key);
  const amb = layer.nl ? null : scene.fx?.amb;
  const q = scene.q;
  const fadeTo = layer.f ? scene.pal[layer.f[0]] : null;
  const pal = scene.pal.map((c) => {
    let o = litColor(c, amb);
    if (fadeTo) o = fadeColor(o, fadeTo, layer.f[1]);
    if (q) o = q[nearestIndex(q, o)];
    return o;
  });
  cache.set(key, pal);
  return pal;
}

function blend(out, i, c, a) {
  const da = out[i + 3] / 255;
  const oa = a + da * (1 - a);
  if (oa <= 0) return;
  for (let k = 0; k < 3; k++) out[i + k] = Math.round((c[k] * a + out[i + k] * da * (1 - a)) / oa);
  out[i + 3] = Math.round(oa * 255);
}

// opts.transparent: ignora el color de fondo.
// opts.only: lista de índices de capa a dibujar (las demás se omiten).
// opts.frame: fotograma a dibujar en escenas animadas (por defecto 0).
export function renderScene(scene, opts = {}) {
  const { w, h } = scene;
  const out = new Uint8ClampedArray(w * h * 4);
  const cx = makeContext(scene);
  const pals = new Map();
  if (scene.bg >= 0 && !opts.transparent) {
    let c = scene.pal[scene.bg];
    if (scene.q) c = scene.q[nearestIndex(scene.q, c)];
    for (let i = 0; i < w * h; i++) { out[i * 4] = c[0]; out[i * 4 + 1] = c[1]; out[i * 4 + 2] = c[2]; out[i * 4 + 3] = 255; }
  }
  scene.layers.forEach((layer, li) => {
    if (layer.v === 0 && !opts.showHidden) return;
    if (opts.only && !opts.only.includes(li)) return;
    const buf = renderLayerIndices(scene, layer, cx, opts.frame || 0);
    const pal = layerPalette(scene, layer, pals);
    const a = layer.a ?? 1;
    const snap = layer.rf ? out.slice() : null;
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const c = buf[y * w + x];
      if (c === EMPTY) continue;
      const i = (y * w + x) * 4;
      blend(out, i, pal[c], a);
      if (snap && y >= layer.rf[0]) {
        // Reflejo: espejo vertical de lo ya compuesto, con ondulación por filas.
        const d = y - layer.rf[0];
        const sy = layer.rf[0] - 1 - d;
        const sx = x + (d > 2 ? ((d >> 1) & 1) * (((d >> 2) & 1) ? 1 : -1) : 0);
        if (sy >= 0 && sx >= 0 && sx < w && (d & 3) !== 3) {
          const s = (sy * w + sx) * 4;
          if (snap[s + 3]) blend(out, i, [snap[s], snap[s + 1], snap[s + 2]], (layer.rf[1] / 100) * a);
        }
      }
    }
  });
  return { w, h, data: out };
}

// Renderiza un sprite reutilizable de forma aislada (para exportarlo).
export function renderSprite(scene, id) {
  const def = scene.sprites?.[id];
  if (!def) return null;
  const sp = makeContext(scene).sprite(id);
  const pal = layerPalette(scene, { nl: 1 }, new Map());
  const data = new Uint8ClampedArray(def.w * def.h * 4);
  sp.buf.forEach((c, p) => { if (c !== EMPTY) { data.set(pal[c], p * 4); data[p * 4 + 3] = 255; } });
  return { w: def.w, h: def.h, data };
}
