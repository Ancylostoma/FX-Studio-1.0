// Control de calidad de la composición. Analiza la escena YA RENDERIZADA
// (píxeles reales, no intenciones) y, si encuentra problemas corregibles,
// ajusta los parámetros de composición y recompone. Nunca declara una imagen
// correcta sin haberla medido: cada comprobación devuelve su dato.
import { composeSceneEx } from './sceneComposer.js';
import { renderLayerIndices, renderScene } from '../core/renderer.js';
import { EMPTY } from '../core/lighting.js';
import { LIMITS } from '../core/pixelScene.js';
import { defOf } from './spriteLibrary.js';

const MAX_COLORS = 64;

// Mide una escena: píxeles propios y visibles de cada entidad y contraste.
export function measure(scene, G) {
  const { w, h } = scene, N = w * h;
  const owner = new Array(N).fill(null);
  const own = new Map();     // clave de entidad → { count, x0, y0, x1, y1, pix:Set }
  const entityKey = (l) => (/^(subj|struct):/.test(l.k || '') ? l.k : null);
  scene.layers.forEach((l) => {
    if (l.v === 0) return;
    const buf = renderLayerIndices(scene, l);
    const key = entityKey(l), shadowOrHalo = /^(sombra|halo)/.test(l.n || '');
    const occludes = (l.a ?? 1) >= 0.6 && !shadowOrHalo && !/^(weather|stars|celestial)$/.test(l.k || '');
    for (let p = 0; p < N; p++) {
      if (buf[p] === EMPTY) continue;
      if (key && !shadowOrHalo) {
        let m = own.get(key);
        if (!m) own.set(key, (m = { count: 0, x0: w, y0: h, x1: -1, y1: -1, pix: new Set() }));
        if (!m.pix.has(p)) { m.pix.add(p); m.count++; const x = p % w, y = (p / w) | 0; if (x < m.x0) m.x0 = x; if (x > m.x1) m.x1 = x; if (y < m.y0) m.y0 = y; if (y > m.y1) m.y1 = y; }
      }
      if (occludes) owner[p] = key && !shadowOrHalo ? key : 'other';
    }
  });
  for (const [k, m] of own) { let vis = 0; for (const p of m.pix) if (owner[p] === k) vis++; m.visible = vis; }
  return { own, owner, w, h, G };
}

// Contraste en el borde de la silueta: diferencia de color entre cada píxel
// del contorno del protagonista y su vecino exterior (imagen final). Es lo
// que hace legible una silueta; el tono medio no basta (naranja sobre verde).
function contrast(scene, m) {
  const img = renderScene(scene).data, { w, h } = scene;
  const col = (p) => [img[p * 4], img[p * 4 + 1], img[p * 4 + 2]];
  const d = (a, b) => Math.sqrt((2 * (a[0] - b[0]) ** 2 + 4 * (a[1] - b[1]) ** 2 + 3 * (a[2] - b[2]) ** 2) / 9);
  let sum = 0, n = 0;
  for (const p of m.pix) {
    const x = p % w, y = (p / w) | 0;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nx = x + dx, ny = y + dy;
      if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
      const q = ny * w + nx;
      if (m.pix.has(q)) continue;
      sum += d(col(p), col(q)); n++;
    }
  }
  return n ? sum / n : 255;
}

// Claves de capa de una entidad del plan: subj:<id>[n] o struct:<id>[n].
const keysFor = (M, e) => { const id = defOf(e)?.id || e?.id; const re = new RegExp('^(subj|struct):' + String(id).replace(/[^\w:]/g, '') + '\\d*$'); return [...M.own.keys()].filter((k) => re.test(k)); };

// Ejecuta las comprobaciones. Devuelve la lista y las correcciones propuestas.
export function checkScene(scene, spec, G) {
  const M = measure(scene, G), { w, h } = M, checks = [], fix = { scale: {}, dx: {}, outline: {}, clearFocus: false };
  const add = (id, label, ok, detail) => checks.push({ id, label, ok, detail });
  const all = [...spec.subjects, ...spec.structures], byId = (id) => all.find((e) => e.id === id);
  const keyOf = (id) => (byId(id) ? keysFor(M, byId(id)) : []);
  // 1. Presencia de los elementos pedidos
  const wanted = [...spec.subjects, ...spec.structures].filter((e) => !e.clash);
  const missing = wanted.filter((e) => !keyOf(e.id).some((k) => (M.own.get(k)?.count || 0) >= 4)).map((e) => e.id);
  add('presence', 'Elementos principales presentes', !missing.length, missing.length ? 'Faltan: ' + missing.join(', ') : `${wanted.length} de ${wanted.length}`);
  // 2. Dentro del lienzo (cajas de maquetación)
  const out = [];
  for (const b of [...G.subs, ...G.structs]) {
    if (b.def.span) continue;
    const ix = Math.max(0, Math.min(b.x + b.w, w) - Math.max(b.x, 0)), iy = Math.max(0, Math.min(b.y + b.h, h) - Math.max(b.y, 0));
    const inside = (ix * iy) / Math.max(1, b.w * b.h);
    if (inside < 0.75) { out.push(b.def.id); const cx = b.x + b.w / 2; fix.dx[b.e.id] = (cx < w / 2 ? 1 : -1) * Math.min(0.25, (1 - inside) * 0.6); }
  }
  add('bounds', 'Elementos dentro del lienzo', !out.length, out.length ? 'Recortados: ' + [...new Set(out)].join(', ') : 'sí');
  // 3. Protagonista: tamaño, jerarquía, visibilidad, contraste
  const focusKeys = spec.focus ? keyOf(spec.focus) : [];
  const fm = focusKeys.map((k) => M.own.get(k)).filter(Boolean).sort((a, b) => b.count - a.count)[0];
  const fb = spec.focus && [...G.subs, ...G.structs].find((b) => b.e.id === spec.focus);
  if (fm && fb && G.layoutKind === 'scene') {
    // Prominencia: altura, o media anchura si el objeto es alargado (un tren).
    const area = fm.count / (w * h), height = Math.max((fm.y1 - fm.y0 + 1) / h, ((fm.x1 - fm.x0 + 1) / w) * 0.5);
    const others = [...M.own.entries()].filter(([k]) => !focusKeys.includes(k) && k.startsWith('subj:')).map(([, m]) => m.count);
    const biggestOther = others.length ? Math.max(...others) : 0;
    const isStruct = focusKeys[0].startsWith('struct:');
    const minH = isStruct ? 0.3 : fb.air ? 0.14 : 0.2;
    const tooSmall = height < minH, outranked = !isStruct && biggestOther > fm.count * 1.25;
    add('hierarchy', 'Jerarquía visual del protagonista', !tooSmall && !outranked, `tamaño ${(height * 100).toFixed(0)} % del lienzo, área ${(area * 100).toFixed(1)} %${outranked ? ', otro sujeto es mayor' : ''}`);
    if (tooSmall) fix.scale[spec.focus] = Math.min(1.7, minH / Math.max(0.05, height));
    else if (outranked) fix.scale[spec.focus] = Math.min(1.5, Math.sqrt((biggestOther * 1.1) / fm.count));
    const ratio = fm.visible / Math.max(1, fm.count), wantsBehind = [...spec.subjects].find((e) => e.id === spec.focus)?.rel?.type === 'behind';
    add('visible', 'El fondo no oculta al protagonista', ratio >= 0.6 || wantsBehind, `${(ratio * 100).toFixed(0)} % visible`);
    if (ratio < 0.6 && !wantsBehind) { fix.clearFocus = true; if (!fb.e.side && !fb.e.rel) fix.dx[spec.focus] = (fb.x + fb.w / 2 < w / 2 ? -1 : 1) * 0.12; }
    const c = contrast(scene, fm);
    add('contrast', 'Contraste de la silueta con el fondo', c >= 40, `contraste de borde ${c.toFixed(0)} (mínimo 40)`);
    if (c < 40) fix.outline[spec.focus] = true;
  }
  // 4. Solapamientos indebidos entre sujetos sin relación
  const subs = G.subs.filter((b) => b.ground && !b.onTop && b.def.cat !== 'effect');
  const bad = [];
  for (let i = 0; i < subs.length; i++) for (let j = i + 1; j < subs.length; j++) {
    const a = subs[i], b = subs[j];
    if (a.e === b.e || a.e.rel?.of === b.e.id || b.e.rel?.of === a.e.id) continue;
    const ox = Math.max(0, Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x)), oy = Math.max(0, Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y));
    const r = (ox * oy) / Math.max(1, Math.min(a.w * a.h, b.w * b.h));
    if (r > 0.45) { bad.push(`${a.def.id}/${b.def.id}`); const left = a.x < b.x ? a : b, right = left === a ? b : a; if (!left.e.side && !left.e.rel) fix.dx[left.e.id] = -0.1; if (!right.e.side && !right.e.rel) fix.dx[right.e.id] = 0.1; }
  }
  add('overlap', 'Sin solapamientos indebidos', !bad.length, bad.length ? bad.join(', ') : 'sí');
  // 5. Separación de planos: lo del fondo no debe ser mayor que lo de delante
  const backs = [...spec.subjects, ...spec.structures].filter((e) => e.depth === 'back');
  const planeBad = backs.filter((e) => { const bm = keyOf(e.id).map((k) => M.own.get(k)?.count || 0)[0] || 0; const fr = [...spec.subjects].filter((x) => x.depth === 'front').map((x) => keyOf(x.id).map((k) => M.own.get(k)?.count || 0)[0] || 0); return fr.some((f) => f && bm > f); }).map((e) => e.id);
  add('planes', 'Separación de planos', !planeBad.length, planeBad.length ? 'Demasiado grandes al fondo: ' + planeBad.join(', ') : 'sí');
  for (const id of planeBad) fix.scale[id] = 0.75;
  // 6. Sombras coherentes con la luz
  const ld = scene.fx?.ld ?? -1;
  let wrong = 0, total = 0;
  for (const l of scene.layers) {
    if (!/^sombra de /.test(l.n || '') || !l.o[0] || l.o[0][0] !== 'E') continue;
    const m = M.own.get(l.k); if (!m) continue;
    total++; const dx = l.o[0][1] - (m.x0 + m.x1) / 2;
    if (Math.sign(dx) === Math.sign(ld) && Math.abs(dx) > 1) wrong++;
  }
  add('shadows', 'Sombras coherentes con la luz', !wrong, total ? `${total - wrong} de ${total}` : 'sin sombras proyectadas');
  // 7. Paleta contenida
  add('palette', 'Paleta limitada', scene.pal.length <= MAX_COLORS, `${scene.pal.length} colores`);
  const fixable = Object.keys(fix.scale).length || Object.keys(fix.dx).length || Object.keys(fix.outline).length || fix.clearFocus;
  return { checks, fix: fixable ? fix : null };
}

// Compone con control de calidad: hasta 3 pasadas de corrección.
export function composeChecked(spec, options) {
  let adjust = { scale: {}, dx: {}, outline: {}, clearFocus: false }, last = null;
  const history = [];
  for (let pass = 0; pass < 3; pass++) {
    const { scene, G } = composeSceneEx(spec, { ...options, adjust });
    const r = checkScene(scene, spec, G);
    history.push(r.checks.filter((c) => !c.ok).map((c) => c.id));
    last = { scene, checks: r.checks };
    if (!r.fix) break;
    // Acumula las correcciones (las escalas se multiplican, los desplazamientos se suman).
    for (const [k, v] of Object.entries(r.fix.scale)) adjust.scale[k] = Math.max(0.5, Math.min(2, (adjust.scale[k] || 1) * v));
    for (const [k, v] of Object.entries(r.fix.dx)) adjust.dx[k] = Math.max(-0.4, Math.min(0.4, (adjust.dx[k] || 0) + v));
    Object.assign(adjust.outline, r.fix.outline);
    adjust.clearFocus ||= r.fix.clearFocus;
  }
  const initial = history[0] || [];
  for (const c of last.checks) c.fixed = !c.ok ? false : initial.includes(c.id);
  if (last.scene.pal.length > LIMITS.maxPal) last.scene.pal = last.scene.pal.slice(0, LIMITS.maxPal);
  return { scene: last.scene, qc: { passes: history.length, checks: last.checks, ok: last.checks.every((c) => c.ok) } };
}
