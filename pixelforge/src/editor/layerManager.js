// Operaciones puras sobre las capas de una PixelScene. Devuelven siempre una
// escena nueva (no mutan), lo que simplifica el historial.
import { LIMITS } from '../core/pixelScene.js';

const withLayers = (scene, layers) => ({ ...scene, layers });
const patch = (scene, i, fn) => withLayers(scene, scene.layers.map((l, k) => (k === i ? fn({ ...l }) : l)));
const clean = (l, key, keep) => { if (!keep) delete l[key]; return l; };

export function toggleLayer(scene, i) { return patch(scene, i, (l) => { if (l.v === 0) delete l.v; else l.v = 0; return l; }); }
export function setLayerOpacity(scene, i, a) { return patch(scene, i, (l) => { l.a = Math.max(0, Math.min(1, Math.round(a * 100) / 100)); return clean(l, 'a', l.a < 1); }); }
export function renameLayer(scene, i, name) { return patch(scene, i, (l) => { l.n = String(name).trim().slice(0, LIMITS.maxName) || l.n; return l; }); }
export function setLayerShading(scene, i, on) { return patch(scene, i, (l) => { l.sh = 1; return clean(l, 'sh', on); }); }
// mode: 'none' | 'sel' (selectivo) | índice de paleta
export function setLayerOutline(scene, i, mode) { return patch(scene, i, (l) => { if (mode === 'none') delete l.ol; else l.ol = mode === 'sel' ? -2 : mode; return l; }); }
export function setLayerLit(scene, i, lit) { return patch(scene, i, (l) => { l.nl = 1; return clean(l, 'nl', !lit); }); }

// dir = +1 sube la capa (se dibuja más tarde, queda por encima); -1 la baja.
export function moveLayer(scene, i, dir) {
  const j = i + dir;
  if (j < 0 || j >= scene.layers.length) return scene;
  const layers = scene.layers.slice();
  [layers[i], layers[j]] = [layers[j], layers[i]];
  return withLayers(scene, layers);
}
export function removeLayer(scene, i) { return withLayers(scene, scene.layers.filter((_, k) => k !== i)); }
export function duplicateLayer(scene, i) {
  if (scene.layers.length >= LIMITS.maxLayers) return scene;
  const layers = scene.layers.slice();
  const copy = JSON.parse(JSON.stringify(layers[i]));
  copy.n = (copy.n || 'capa').slice(0, LIMITS.maxName - 6) + ' copia';
  layers.splice(i + 1, 0, copy);
  return withLayers(scene, layers);
}

// Agrupa las capas por la parte del generador a la que pertenecen (k),
// para activar o desactivar un elemento completo (p. ej. "castillo" + sus luces).
export function elementGroups(scene) {
  const groups = new Map();
  scene.layers.forEach((l, i) => { const k = l.k || l.n || 'capa'; if (!groups.has(k)) groups.set(k, []); groups.get(k).push(i); });
  return groups;
}
export function toggleElement(scene, key) {
  const idx = elementGroups(scene).get(key) || [];
  const anyOn = idx.some((i) => scene.layers[i].v !== 0);
  return withLayers(scene, scene.layers.map((l, i) => { if (!idx.includes(i)) return l; const c = { ...l }; if (anyOn) c.v = 0; else delete c.v; return c; }));
}
