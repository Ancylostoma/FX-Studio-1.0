// Utilidades de interfaz: creación de nodos, selectores y avisos.
import { RESOLUTIONS, ASPECTS } from '../generator/sceneComposer.js';
import { STYLES, DETAIL_LEVELS, ANIMS } from '../generator/templates.js';
import { PALETTES } from '../core/paletteManager.js';
import { AMBIENTS } from '../core/lighting.js';
import { LIB, CATEGORY_NAMES } from '../generator/spriteLibrary.js';
import { vocabularySummary } from '../generator/vocabulary.js';

export const $ = (sel) => document.querySelector(sel);

// h('button', { class: 'btn', onclick: fn }, 'Texto')  — siempre textContent, nunca innerHTML.
export function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'style') Object.assign(el.style, v);
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k in el) el[k] = v;
    else el.setAttribute(k, v);
  }
  for (const c of children.flat()) if (c != null) el.append(c.nodeType ? c : document.createTextNode(String(c)));
  return el;
}

export function fillSelect(sel, entries, value) {
  sel.replaceChildren(...entries.map(([v, label]) => h('option', { value: String(v) }, label)));
  if (value != null) sel.value = String(value);
}

export function toast(message, kind = '') {
  const el = h('div', { class: 'toast ' + kind }, message);
  $('#toasts').append(el);
  setTimeout(() => el.remove(), kind === 'error' ? 6500 : 3500);
}

export const EXAMPLES = [
  'Un dragón rojo volando sobre un castillo medieval durante la noche',
  'Una ciudad futurista cyberpunk bajo la lluvia',
  'Un bosque encantado con una pequeña casa y luciérnagas',
  'Un astronauta caminando sobre Marte',
  'Un paisaje japonés con montañas, un lago y un árbol de cerezo',
  'Un personaje RPG con armadura dorada y una espada de fuego',
  'Un velero en el mar al atardecer',
  'Una cabaña en un paisaje nevado con pinos',
];

export function initControls() {
  fillSelect($('#optRes'), RESOLUTIONS.map((r) => [r, `${r} px`]), 64);
  fillSelect($('#optAspect'), Object.keys(ASPECTS).map((a) => [a, a]), '1:1');
  fillSelect($('#optStyle'), Object.entries(STYLES).map(([k, s]) => [k, s.name]), 'bit16');
  const pals = Object.entries(PALETTES).map(([k, p]) => [k, p.name]);
  fillSelect($('#optPalette'), pals, 'auto');
  fillSelect($('#optPalette2'), pals, 'auto');
  fillSelect($('#optDetail'), Object.entries(DETAIL_LEVELS), 2);
  fillSelect($('#optAnim'), Object.entries(ANIMS).map(([k, a]) => [k, a.name]), 'none');
  fillSelect($('#ambPreset'), [...Object.entries(AMBIENTS).map(([k, a]) => [k, a.name]), ['custom', 'Personalizada']], 'day');
  fillSelect($('#zoomSel'), [['fit', 'Ajustar'], ['1', 'Original (1×)'], ...[2, 3, 4, 6, 8, 12, 16, 24].map((z) => [z, z + '×'])], 'fit');
  const ex = $('#examples');
  ex.replaceChildren(...EXAMPLES.map((t) => h('button', { class: 'chip', type: 'button', title: t, onclick: () => { $('#prompt').value = t; $('#prompt').focus(); } }, t.length > 34 ? t.slice(0, 32) + '…' : t)));
  // Ayuda: vocabulario disponible en el motor offline.
  const byCat = {};
  for (const d of Object.values(LIB)) (byCat[d.cat] ||= []).push(d.name);
  const voc = vocabularySummary();
  const para = (title, list) => h('p', {}, h('b', {}, title + ': '), list.join(', ') + '.');
  $('#vocab').replaceChildren(
    ...Object.entries(byCat).map(([c, names]) => para(CATEGORY_NAMES[c], names)),
    para('Entornos', voc.biome), para('Relieve', voc.feat), para('Hora', voc.time), para('Clima', voc.w), para('Cielo', voc.c), para('Colores', voc.col),
    h('p', {}, 'Lo que no esté en estas listas se ignora y se avisa. Para descripciones libres usa el modo IA.'),
  );
}

export function readOptions() {
  return {
    res: +$('#optRes').value, aspect: $('#optAspect').value, style: $('#optStyle').value, palette: $('#optPalette').value,
    detail: +$('#optDetail').value, anim: $('#optAnim').value, seed: Math.max(1, Math.min(999999, +$('#optSeed').value || 1)), mode: $('#optMode').value,
  };
}

export function writeOptions(o) {
  if (!o) return;
  const set = (id, v) => { const el = $(id); if (v != null && [...el.options].some((op) => op.value === String(v))) el.value = String(v); };
  set('#optRes', o.res); set('#optAspect', o.aspect); set('#optStyle', o.style); set('#optPalette', o.palette); set('#optPalette2', o.palette); set('#optDetail', o.detail); set('#optAnim', o.anim || 'none');
  if (o.seed) $('#optSeed').value = o.seed;
}
