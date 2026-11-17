// Exportación PNG con las capacidades nativas del navegador (canvas.toBlob).
// El escalado es siempre por vecino más cercano y con factor entero.
import { render, imageToCanvas, clampScale, MAX_EXPORT } from '../core/pixelEngine.js';
import { renderSprite } from '../core/renderer.js';
import { opaqueBounds } from '../core/geometry.js';
import { elementGroups } from '../editor/layerManager.js';

// Mayor factor entero cuyo resultado no supera `target` px en el lado mayor.
export function scaleForTarget(scene, target) {
  return clampScale(scene, Math.max(1, Math.floor(Math.min(target, MAX_EXPORT) / Math.max(scene.w, scene.h))));
}

// Imagen de la escena para exportar. transparent: sin color de fondo ni cielo.
export function exportImage(scene, { transparent = false } = {}) {
  if (!transparent) return render(scene);
  const only = scene.layers.map((l, i) => i).filter((i) => scene.layers[i].k !== 'sky');
  return render(scene, { transparent: true, only });
}

export function cropImage(img, b) {
  const data = new Uint8ClampedArray(b.w * b.h * 4);
  for (let y = 0; y < b.h; y++) data.set(img.data.subarray(((b.y + y) * img.w + b.x) * 4, ((b.y + y) * img.w + b.x + b.w) * 4), y * b.w * 4);
  return { w: b.w, h: b.h, data };
}

// Sprites individuales: cada elemento (sujetos, estructuras) recortado, más
// los sprites reutilizables definidos en la escena.
export function collectSprites(scene) {
  const out = [];
  for (const [key, idx] of elementGroups(scene)) {
    if (!/^(subj|struct):/.test(key)) continue;
    const only = idx.filter((i) => !/^(sombra|halo)/.test(scene.layers[i].n || ''));
    const img = render(scene, { transparent: true, only, showHidden: true });
    const b = opaqueBounds(img.data, img.w, img.h);
    if (b) out.push({ name: key.replace(':', '-'), img: cropImage(img, b) });
  }
  for (const id of Object.keys(scene.sprites || {})) { const img = renderSprite(scene, id); if (img) out.push({ name: id, img }); }
  return out;
}

// Hoja de sprites en una fila, con 1 px de separación.
export function buildSpriteSheet(sprites) {
  if (!sprites.length) return null;
  const pad = 1;
  const w = sprites.reduce((a, s) => a + s.img.w + pad, pad), h = Math.max(...sprites.map((s) => s.img.h)) + 2 * pad;
  const data = new Uint8ClampedArray(w * h * 4);
  const frames = [];
  let x = pad;
  for (const s of sprites) {
    for (let y = 0; y < s.img.h; y++) data.set(s.img.data.subarray(y * s.img.w * 4, (y + 1) * s.img.w * 4), ((pad + y) * w + x) * 4);
    frames.push({ name: s.name, x, y: pad, w: s.img.w, h: s.img.h });
    x += s.img.w + pad;
  }
  return { img: { w, h, data }, frames };
}

export function imageToBlob(img, scale = 1) {
  const canvas = imageToCanvas(img, document.createElement('canvas'), Math.max(1, Math.min(scale, Math.floor(MAX_EXPORT / Math.max(img.w, img.h)) || 1)));
  return new Promise((resolve, reject) => canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('No se pudo codificar el PNG'))), 'image/png'));
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement('a'), { href: url, download: filename });
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function downloadText(text, filename, type = 'application/json') { downloadBlob(new Blob([text], { type }), filename); }
