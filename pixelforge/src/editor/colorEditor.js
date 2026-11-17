// Edición de color e iluminación: todo ocurre sobre la paleta y los
// parámetros fx, nunca sobre píxeles, así que cualquier cambio es inmediato.
import { PALETTES } from '../core/paletteManager.js';
import { AMBIENTS } from '../core/lighting.js';
import { LIMITS } from '../core/pixelScene.js';

const byte = (v) => Math.max(0, Math.min(255, Math.round(+v) || 0));

export function setPaletteColor(scene, i, rgb) {
  if (i < 0 || i >= scene.pal.length) return scene;
  return { ...scene, pal: scene.pal.map((c, k) => (k === i ? rgb.map(byte) : c)) };
}
export function addPaletteColor(scene, rgb) {
  if (scene.pal.length >= LIMITS.maxPal) return { scene, index: -1 };
  return { scene: { ...scene, pal: [...scene.pal, rgb.map(byte)] }, index: scene.pal.length };
}
export function setBackground(scene, index) {
  return { ...scene, bg: Math.max(-1, Math.min(scene.pal.length - 1, index | 0)) };
}
// Paleta de cuantización ("auto" la elimina).
export function setQuantPalette(scene, key) {
  const s = { ...scene };
  const colors = PALETTES[key]?.colors;
  if (colors) s.q = colors; else delete s.q;
  return s;
}
export function quantKey(scene) {
  if (!scene.q) return 'auto';
  const sig = JSON.stringify(scene.q);
  return Object.keys(PALETTES).find((k) => PALETTES[k].colors && JSON.stringify(PALETTES[k].colors) === sig) || 'auto';
}
export function setDither(scene, level) { return { ...scene, fx: { ...scene.fx, dth: Math.max(0, Math.min(3, level | 0)) } }; }
export function setLightDirection(scene, ld) { return { ...scene, fx: { ...scene.fx, ld: ld > 0 ? 1 : -1 } }; }
// preset: clave de AMBIENTS; intensity (0..100) opcional.
export function setAmbient(scene, preset, intensity) {
  const fx = { ...scene.fx };
  const base = AMBIENTS[preset]?.amb;
  if (!base) delete fx.amb;
  else fx.amb = [base[0], base[1], base[2], intensity == null ? base[3] : Math.max(0, Math.min(100, intensity | 0))];
  return { ...scene, fx };
}
export function setAmbientColor(scene, rgb, intensity) {
  return { ...scene, fx: { ...scene.fx, amb: [byte(rgb[0]), byte(rgb[1]), byte(rgb[2]), Math.max(0, Math.min(100, intensity | 0))] } };
}
export function ambientKey(scene) {
  const a = scene.fx?.amb;
  if (!a) return 'day';
  return Object.keys(AMBIENTS).find((k) => AMBIENTS[k].amb && AMBIENTS[k].amb.slice(0, 3).join() === a.slice(0, 3).join()) || 'custom';
}
