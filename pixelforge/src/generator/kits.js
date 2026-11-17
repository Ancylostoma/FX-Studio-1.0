// Kits paramétricos: permiten representar objetos que no tienen sprite propio
// combinando plantillas (humanoide, cuadrúpedo, edificio, volador) o, como
// último recurso, primitivas descritas en coordenadas unitarias.
//
// Un kit es un objeto de datos { base, ...parámetros }. La IA (o el parser)
// puede describir "un tigre" como { base:'quadruped', col:'orange',
// stripes:'black', ... } sin que exista un sprite "tigre". Los preajustes de
// KIT_PRESETS se registran además como sprites normales del catálogo.
import { LIB, reg, humanoidKit, quadrupedKit, HUM, QUAD, bricks, lightOrDark, flapper, poseOf } from './spriteBase.js';
import { MATERIALS, COLORS } from './templates.js';

const pickMat = (C, name, fallback) => (name && (MATERIALS[name] || COLORS[name]) ? C.mat(name) : C.mat(fallback));

// ---------------- Edificio paramétrico ----------------
// roof: gable | flat | dome | spire | pagoda | crenel | thatch | cone
// mat: stone | wood | brick | concrete | glass | sand | white | straw | timber
const BUILDING_DEFAULTS = { mat: 'stone', roof: 'gable', floors: 2, width: 'normal', towers: 0, rose: false, stilts: false, chimney: false, color: null, arches: false };
export function buildingAr(o) {
  const w = { narrow: 0.55, normal: 0.85, wide: 1.25 }[o.width] || 0.85;
  const h = 0.35 + o.floors * 0.22 + (['spire', 'pagoda', 'cone'].includes(o.roof) ? 0.35 : 0.2) + (o.stilts ? 0.2 : 0);
  return Math.max(0.35, Math.min(2.2, (w + o.towers * 0.18) / h * 1.3));
}
function buildingKit(o0) {
  const o = { ...BUILDING_DEFAULTS, ...o0 };
  return (p, k) => {
    const C = k.C, a = k.a;
    const timber = o.mat === 'timber';
    const wall = timber ? C.mat('cream') : pickMat(C, a.color && !o.color ? null : o.mat === 'brick' ? 'roof' : o.mat, 'stone');
    const roofC = C.tint(a.color || o.color, o.roof === 'thatch' ? 'sand' : o.roof === 'dome' ? 'gold' : o.mat === 'concrete' || o.mat === 'glass' ? 'darkstone' : 'roof');
    const dark = C.dark, wood = C.mat('wood');
    const tw = o.towers ? (o.roof === 'dome' ? 0.1 : 0.17) : 0;   // ancho de cada torre (alminar si hay cúpula)
    const x0 = tw, x1 = 1 - tw;
    const roofH = { gable: 0.24, flat: 0.04, dome: 0.34, spire: 0.42, pagoda: 0.36, crenel: 0.06, thatch: 0.26, cone: 0.36 }[o.roof] ?? 0.2;
    // Con torres, estas sobresalen por encima de la nave: el cuerpo empieza más abajo.
    const towerTop = o.towers ? 0.2 : 0;
    const bodyBottom = o.stilts ? 0.82 : 1, top = roofH + (o.towers && o.roof !== 'dome' ? 0.14 : 0);
    const fh = (bodyBottom - top) / Math.max(1, o.floors);
    // Torres laterales (detrás del cuerpo principal)
    for (let t = 0; t < o.towers; t++) {
      const u = t ? 1 - tw : 0, tt = towerTop;
      p.rect(u, tt, tw, bodyBottom - tt, wall[t ? 1 : 2]); p.rect(u, tt, tw * 0.3, bodyBottom - tt, wall[3]);
      const pointy = o.roof === 'spire' || o.roof === 'cone' || o.roof === 'gable';
      if (pointy) { p.poly([[u - 0.015, tt], [u + tw / 2, 0], [u + tw + 0.015, tt]], roofC[2]); p.poly([[u + tw / 2, 0], [u + tw + 0.015, tt], [u + tw * 0.55, tt]], roofC[1]); p.rect(u - 0.015, tt - 0.01, tw + 0.03, 0.025, wall[1]); }
      else if (o.roof === 'dome') { p.ell(u + tw / 2, tt, tw * 0.6, 0.05, roofC[2]); p.rect(u + tw / 2 - 0.005, 0.0, 0.01, tt - 0.04, roofC[1]); }
      else for (let i = 0; i < 3; i++) p.rect(u + i * tw / 2.5, tt - 0.04, tw / 5, 0.04, wall[2]);
      for (let f = 0; f < o.floors; f++) lightOrDark(k, u + tw * 0.35, tt + 0.08 + f * fh * 0.9, tw * 0.3, Math.min(0.08, fh * 0.4), p);
    }
    // Cúpula (antes del cuerpo, para que este tape su mitad inferior)
    if (o.roof === 'dome') { const rx = (x1 - x0) * 0.3; p.ell(0.5, top + 0.02, rx, roofH * 0.92, roofC[2]); p.ell(0.44, top - roofH * 0.45, rx * 0.4, roofH * 0.25, roofC[4], 1); p.ell(0.6, top - roofH * 0.2, rx * 0.35, roofH * 0.5, roofC[1], 1); p.rect(0.495, top - roofH - 0.06, 0.01, 0.08, roofC[1]); p.ell(0.5, top - roofH - 0.06, 0.012, 0.012, roofC[3]); }
    // Cuerpo
    p.rect(x0, top, x1 - x0, bodyBottom - top, wall[2]);
    p.rect(x0, top, (x1 - x0) * 0.18, bodyBottom - top, wall[3]);
    p.rect(x1 - (x1 - x0) * 0.2, top, (x1 - x0) * 0.2, bodyBottom - top, wall[1]);
    if (o.mat === 'stone' || o.mat === 'brick' || o.mat === 'sand') bricks(p, k, wall);
    if (o.mat === 'wood' || o.mat === 'straw') for (let v = top + 0.06; v < bodyBottom; v += 0.07) p.rect(x0, v, x1 - x0, 0.012, wall[1], 1);
    if (timber) {
      const b = C.mat('bark');
      for (let f = 0; f <= o.floors; f++) p.rect(x0, top + f * fh - (f === o.floors ? 0.02 : 0), x1 - x0, 0.025, b[1]);
      for (const u of [x0, (x0 + x1) / 2 - 0.0125, x1 - 0.025]) p.rect(u, top, 0.025, bodyBottom - top, b[1]);
      for (let f = 0; f < o.floors; f++) { p.line(x0, top + f * fh, (x0 + x1) / 2, top + (f + 1) * fh, b[1]); p.line(x1, top + f * fh, (x0 + x1) / 2, top + (f + 1) * fh, b[1]); }
    }
    // Ventanas por planta (la planta baja deja sitio a la puerta)
    const cols = o.width === 'wide' ? 4 : o.width === 'narrow' ? 1 : 2;
    for (let f = 0; f < o.floors; f++) for (let c = 0; c < cols; c++) {
      const cw = (x1 - x0) / cols, u = x0 + c * cw + cw * 0.32, v = top + f * fh + fh * 0.3;
      if (f === o.floors - 1 && (cols === 1 || c === Math.floor(cols / 2) - (cols === 4 ? 0 : 0) && cols % 2 === 1)) continue;
      if (o.mat === 'glass') { p.rect(u - cw * 0.2, v - fh * 0.15, cw * 0.76, fh * 0.6, C.mat('glass')[k.night ? 0 : 3]); continue; }
      if (o.arches) p.ell(u + cw * 0.18, v, cw * 0.18, fh * 0.12, k.night ? C.rgb([255, 222, 120]) : dark);
      lightOrDark(k, u, v, cw * 0.36, Math.min(fh * 0.4, 0.14), p);
    }
    // Rosetón
    if (o.rose) {
      const cx = 0.5, cy = top + fh * 0.6, r = Math.min(0.2, (x1 - x0) * 0.22);
      p.ell(cx, cy, r, r * 0.8 * p.w / Math.max(1, p.h) * (p.h / p.w), wall[0]);
      const glass = [C.rgb([200, 60, 80]), C.rgb([70, 110, 220]), C.rgb([240, 200, 80])];
      (k.night ? k.lit : p).ell(cx, cy, r * 0.78, r * 0.62, glass[1]);
      (k.night ? k.lit : p).ell(cx, cy, r * 0.4, r * 0.32, glass[2]);
      for (let i = 0; i < 4; i++) { const an = (i * Math.PI) / 4; p.line(cx - Math.cos(an) * r * 0.78, cy - Math.sin(an) * r * 0.62, cx + Math.cos(an) * r * 0.78, cy + Math.sin(an) * r * 0.62, glass[0]); }
    }
    // Puerta
    const dw = Math.min(0.2, (x1 - x0) * 0.24), du = 0.5 - dw / 2, dh = Math.min(fh * 0.7, 0.24);
    if (o.arches || o.rose) p.ell(0.5, bodyBottom - dh, dw / 2, dh * 0.3, dark);
    p.rect(du, bodyBottom - dh, dw, dh, o.mat === 'glass' ? C.mat('glass')[1] : wood[1]);
    p.rect(0.5 - 0.005, bodyBottom - dh, 0.01, dh, dark);
    // Tejado
    const rl = x0 - 0.04, rr = x1 + 0.04;
    if (o.roof === 'gable' || o.roof === 'thatch') {
      const ap = Math.max(0, top - roofH); p.poly([[rl, top + 0.02], [0.5, ap], [rr, top + 0.02]], roofC[2]); p.poly([[0.5, ap], [rr, top + 0.02], [0.6, top + 0.02]], roofC[1]); p.line(rl, top + 0.02, 0.5, ap, roofC[3]);
      if (o.roof === 'thatch') { p.rect(rl, top - 0.01, rr - rl, 0.04, roofC[1]); for (let u = rl + 0.04; u < rr; u += 0.07) p.line(u, top + 0.02, 0.5 + (u - 0.5) * 0.3, 0.04, roofC[3], 1); }
    } else if (o.roof === 'dome') p.rect(x0 - 0.01, top - 0.02, x1 - x0 + 0.02, 0.03, wall[1]);
    else if (o.roof === 'spire' || o.roof === 'cone') { p.rect(x0 - 0.02, top - 0.03, x1 - x0 + 0.04, 0.04, wall[1]); p.poly([[0.5 - (x1 - x0) * (o.roof === 'cone' ? 0.5 : 0.18), top - 0.02], [0.5, 0], [0.5 + (x1 - x0) * (o.roof === 'cone' ? 0.5 : 0.18), top - 0.02]], roofC[2]); p.poly([[0.5, 0], [0.5 + (x1 - x0) * (o.roof === 'cone' ? 0.5 : 0.18), top - 0.02], [0.52, top - 0.02]], roofC[1]); if (o.roof === 'spire') p.rect(0.495, -0.05, 0.01, 0.06, C.mat('gold')[3]); }
    else if (o.roof === 'pagoda') { for (let i = 0; i < 3; i++) { const v = top - i * roofH * 0.33, ww = (x1 - x0) * (0.62 - i * 0.14); p.poly([[0.5 - ww - 0.04, v], [0.5 - ww * 0.7, v - roofH * 0.16], [0.5 + ww * 0.7, v - roofH * 0.16], [0.5 + ww + 0.04, v]], roofC[i & 1 ? 1 : 2]); if (i < 2) p.rect(0.5 - ww * 0.55, v - roofH * 0.33, ww * 1.1, roofH * 0.17, wall[2]); } }
    else if (o.roof === 'crenel') { const n = 7, st = (x1 - x0) / (2 * n - 1); for (let i = 0; i < n; i++) p.rect(x0 + i * 2 * st, top - 0.05, st, 0.05, wall[2]); }
    else p.rect(x0 - 0.01, top - 0.02, x1 - x0 + 0.02, 0.03, wall[1]);
    // Pórtico de columnas con frontón (estilo clásico)
    if (o.columns) {
      const n = o.width === 'wide' ? 6 : 4, cu = (x1 - x0) / n, cv = top + (bodyBottom - top) * 0.28, light = C.mat('white');
      p.poly([[x0 - 0.03, cv], [0.5, cv - 0.14], [x1 + 0.03, cv]], light[3]); p.poly([[x0 + 0.04, cv - 0.01], [0.5, cv - 0.11], [x1 - 0.04, cv - 0.01]], light[2]);
      p.rect(x0 - 0.03, cv, x1 - x0 + 0.06, 0.04, light[3]);
      for (let i = 0; i < n; i++) { const u = x0 + (i + 0.5) * cu - cu * 0.15; p.rect(u, cv + 0.04, cu * 0.3, bodyBottom - cv - 0.08, light[3]); p.rect(u + cu * 0.2, cv + 0.04, cu * 0.1, bodyBottom - cv - 0.08, light[1]); }
      p.rect(x0 - 0.05, bodyBottom - 0.05, x1 - x0 + 0.1, 0.05, light[2]);
    }
    if (o.chimney) { p.rect(0.68, top * 0.3, 0.08, top * 0.6, C.mat('stone')[2]); }
    if (o.stilts) { for (const u of [x0 + 0.04, 0.48, x1 - 0.08]) p.rect(u, bodyBottom, 0.04, 1 - bodyBottom, wood[1]); p.rect(x0 - 0.06, bodyBottom - 0.02, x1 - x0 + 0.12, 0.03, wood[2]); }
  };
}

// ---------------- Composición por primitivas ----------------
// shapes: [{ s:'ellipse'|'rect'|'tri'|'poly', x, y, w, h, pts?, mat, tone? }]
// en coordenadas 0..1 dentro de la caja del objeto. Último recurso para algo
// que ninguna plantilla cubre; cada forma recibe sombreado por rampa.
function shapesKit(o) {
  return (p, k) => {
    const C = k.C;
    for (const sh of (o.shapes || []).slice(0, 14)) {
      const r = pickMat(C, sh.mat, 'stone'), tone = Math.max(0, Math.min(4, sh.tone ?? 2));
      const x = num(sh.x), y = num(sh.y), w = num(sh.w, 0.2), h = num(sh.h, 0.2);
      if (sh.s === 'ellipse') { p.ell(x + w / 2, y + h / 2, w / 2, h / 2, r[tone]); p.ell(x + w * 0.4, y + h * 0.38, w * 0.28, h * 0.24, r[Math.min(4, tone + 1)], 1); }
      else if (sh.s === 'tri') p.poly([[x, y + h], [x + w / 2, y], [x + w, y + h]], r[tone]);
      else if (sh.s === 'poly' && Array.isArray(sh.pts) && sh.pts.length >= 3) p.poly(sh.pts.slice(0, 12).map(([u, v]) => [num(u), num(v)]), r[tone]);
      else { p.rect(x, y, w, h, r[tone]); p.rect(x, y, Math.max(0.01, w * 0.18), h, r[Math.min(4, tone + 1)], 1); }
    }
  };
}
const num = (v, d = 0) => (Number.isFinite(+v) ? Math.max(-0.5, Math.min(1.5, +v)) : d);

// ---------------- Volador genérico (insectos, aves fantásticas) ----------------
function flyerKit(o) {
  return (p, k) => {
    const C = k.C, c = C.tint(k.a.color, o.col || 'gray'), wc = C.tint(k.a.color2, o.wingCol || o.col || 'white'), W = flapper(k, 0.5);
    if (o.insect) { const s = k.pose ? [1, 0.55, 0.2, 0.55][k.pose.f % 4] : 1; p.ell(0.4, 0.42 - 0.14 * s, 0.13, 0.16 * s + 0.02, wc[4]); p.ell(0.56, 0.4 - 0.14 * s, 0.11, 0.14 * s + 0.02, wc[3]); p.ell(0.46, 0.6, 0.3, 0.13, c[2]); p.ell(0.42, 0.55, 0.2, 0.05, c[3], 1); for (let i = 0; i < 3; i++) p.rect(0.26 + i * 0.12, 0.5, 0.05, 0.2, C.mat('black')[1], 1); p.ell(0.8, 0.56, 0.1, 0.1, c[1]); p.px(0.84, 0.53, C.dark); return; }
    p.poly(W([[0.36, 0.52], [0.16, 0.06], [0.52, 0.34], [0.58, 0.52]]), wc[1]);
    p.ell(0.48, 0.6, 0.26, 0.16, c[2]); p.poly([[0.24, 0.56], [0.0, 0.62], [0.06, 0.76], [0.28, 0.68]], c[1]);
    p.ell(0.76, 0.46, 0.12, 0.14, c[3]); p.poly([[0.86, 0.42], [1.0, 0.48], [0.86, 0.54]], C.col(o.beak || 'orange')[2]); p.px(0.79, 0.43, C.dark);
    p.poly(W([[0.42, 0.56], [0.3, 0.06], [0.68, 0.4]]), wc[3]);
  };
}

export const KIT_BASES = ['humanoid', 'quadruped', 'building', 'flyer', 'shapes'];
const CAT = { humanoid: 'humanoid', quadruped: 'animal', building: 'structure', flyer: 'animal', shapes: 'object' };

// Parámetros permitidos por base (lo demás se descarta al sanear).
export const KIT_PARAMS = {
  humanoid: { body: 'mat', skin: 'mat', legs: 'mat', boots: 'mat', hair: 'mat', head: ['hair', 'helm', 'wizard', 'crown', 'skull', 'alien', 'robot', 'astro', 'ninja', 'pirate', 'kabuto', 'bald'], robe: 'bool', cape: 'mat', armor: 'bool', weapon: ['sword', 'staff', 'spear', 'axe', 'club', 'hammer', 'bow', 'wand'], shield: 'bool', wings: ['fairy', 'angel', 'bat'], wingColor: 'mat', horns: 'bool', halo: 'bool', tail: ['devil', 'lizard'], glow: 'bool', ears: 'bool', bulky: 'bool', thin: 'bool', long: 'bool', tall: 'bool' },
  quadruped: { col: 'mat', neck: 'num:0:2', head: 'num:0.06:0.16', snout: 'num:0:0.1', ears: ['pointy', 'round', 'floppy', 'long', 'none'], tail: ['cat', 'bushy', 'short', 'thin', 'horse', 'none'], leg: 'num:0.15:0.55', slim: 'bool', fat: 'bool', mane: 'mat', spots: 'mat', spotsMany: 'bool', stripes: 'mat', belly: 'mat', horns: 'bool', horn: 'bool', antlers: 'bool', trunk: 'bool', tusks: 'bool', lion: 'bool', wool: 'bool', face: 'mat' },
  building: { mat: ['stone', 'wood', 'brick', 'concrete', 'glass', 'sand', 'white', 'straw', 'timber'], roof: ['gable', 'flat', 'dome', 'spire', 'pagoda', 'crenel', 'thatch', 'cone'], floors: 'num:1:6', width: ['narrow', 'normal', 'wide'], towers: 'num:0:2', rose: 'bool', stilts: 'bool', chimney: 'bool', arches: 'bool', columns: 'bool', color: 'mat' },
  flyer: { col: 'mat', wingCol: 'mat', insect: 'bool', beak: 'mat' },
  shapes: { shapes: 'shapes', ar: 'num:0.3:3' },
};
const isMat = (v) => typeof v === 'string' && (MATERIALS[v] || COLORS[v]);

// Sanea un kit procedente de la IA o de un archivo: solo pasan los parámetros conocidos y válidos.
export function sanitizeKit(kit) {
  if (!kit || typeof kit !== 'object' || !KIT_BASES.includes(kit.base)) return null;
  const spec = KIT_PARAMS[kit.base], out = { base: kit.base };
  if (typeof kit.name === 'string') out.name = kit.name.slice(0, 40);
  for (const [key, rule] of Object.entries(spec)) {
    const v = kit[key];
    if (v == null || v === false) continue;
    if (rule === 'mat') { if (isMat(v)) out[key] = v; }
    else if (rule === 'bool') { if (v === true) out[key] = 1; }
    else if (Array.isArray(rule)) { if (rule.includes(v)) out[key] = v; }
    else if (rule.startsWith('num:')) { const [, a, b] = rule.split(':').map(Number); if (Number.isFinite(+v)) out[key] = Math.max(a, Math.min(b, +v)); }
    else if (rule === 'shapes' && Array.isArray(v)) out.shapes = v.slice(0, 14).filter((s) => s && ['ellipse', 'rect', 'tri', 'poly'].includes(s.s)).map((s) => ({ s: s.s, x: num(s.x), y: num(s.y), w: num(s.w, 0.2), h: num(s.h, 0.2), mat: isMat(s.mat) ? s.mat : 'stone', ...(Array.isArray(s.pts) ? { pts: s.pts.slice(0, 12).map((q) => [num(q?.[0]), num(q?.[1])]) } : {}), ...(Number.isInteger(s.tone) ? { tone: Math.max(0, Math.min(4, s.tone)) } : {}) }));
  }
  if (kit.base === 'quadruped' && out.floors) delete out.floors;
  if (kit.base === 'building') { out.floors = Math.round(out.floors || 2); out.towers = Math.round(out.towers || 0); }
  if (kit.base === 'shapes' && !out.shapes?.length) return null;
  return out;
}

// Definición de dibujo para un kit (con caché por contenido).
const cache = new Map();
export function kitDef(kit) {
  const key = JSON.stringify(kit);
  if (cache.has(key)) return cache.get(key);
  let def;
  if (kit.base === 'humanoid') def = { ar: kit.wings ? 0.9 : 0.6, sz: 0.3, build: humanoidKit({ body: 'cloth', head: 'hair', legs: 'brown', boots: 'bark', ...kit }) };
  else if (kit.base === 'quadruped') def = { ar: kit.neck > 1.2 ? 1.0 : 1.4, sz: kit.trunk ? 0.3 : kit.fat ? 0.2 : 0.17, build: quadrupedKit({ col: 'brown', neck: 0.2, head: 0.11, snout: 0.06, ears: 'pointy', tail: 'thin', leg: 0.3, ...kit }) };
  else if (kit.base === 'building') { const o = { ...BUILDING_DEFAULTS, ...kit }; def = { ar: buildingAr(o), sz: 0.22 + o.floors * 0.07 + (o.roof === 'spire' ? 0.08 : 0), build: buildingKit(kit) }; }
  else if (kit.base === 'flyer') def = { ar: kit.insect ? 1.2 : 1.5, sz: kit.insect ? 0.06 : 0.1, air: 1, build: flyerKit(kit) };
  else def = { ar: kit.ar || 1, sz: 0.18, build: shapesKit(kit) };
  def = { id: 'kit:' + hash(key), name: kit.name || kit.base, cat: CAT[kit.base], kit, ...def };
  if (cache.size > 200) cache.clear();
  cache.set(key, def);
  return def;
}
const hash = (s) => { let h = 2166136261; for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619); return (h >>> 0).toString(36); };

// Definición de una entidad de escena: kit a medida o sprite del catálogo.
export function defOf(e) {
  if (e?.kit) return kitDef(e.kit);
  return LIB[e?.id] || null;
}

// ---------------- Preajustes registrados como sprites del catálogo ----------------
// [id, nombre, kit, extras]
export const KIT_PRESETS = [
  ['tiger', 'tigre', { base: 'quadruped', col: 'orange', stripes: 'black', neck: 0.15, head: 0.12, snout: 0.06, ears: 'round', tail: 'thin', leg: 0.3 }],
  ['leopard', 'leopardo', { base: 'quadruped', col: 'gold', spots: 'black', spotsMany: 1, neck: 0.15, head: 0.11, snout: 0.05, ears: 'round', tail: 'thin', leg: 0.3, slim: 1 }],
  ['panther', 'pantera', { base: 'quadruped', col: 'black', neck: 0.15, head: 0.11, snout: 0.05, ears: 'round', tail: 'thin', leg: 0.3, slim: 1 }],
  ['zebra', 'cebra', { base: 'quadruped', col: 'white', stripes: 'black', neck: 1, head: 0.09, snout: 0.08, ears: 'pointy', tail: 'horse', mane: 'black', leg: 0.46, slim: 1, hoof: 'dark' }],
  ['giraffe', 'jirafa', { base: 'quadruped', col: 'gold', spots: 'brown', spotsMany: 1, neck: 2, head: 0.08, snout: 0.05, ears: 'pointy', tail: 'thin', leg: 0.5, slim: 1 }, { ar: 0.95, sz: 0.38 }],
  ['elephant', 'elefante', { base: 'quadruped', col: 'gray', neck: 0, head: 0.15, ears: 'round', tail: 'thin', leg: 0.34, fat: 1, trunk: 1, tusks: 1 }, { sz: 0.3 }],
  ['boar', 'jabalí', { base: 'quadruped', col: 'brown', neck: 0, head: 0.12, snout: 0.06, ears: 'pointy', tail: 'thin', leg: 0.22, fat: 1, tusks: 1 }],
  ['camel', 'camello', { base: 'quadruped', col: 'sand', neck: 1.1, head: 0.08, snout: 0.07, ears: 'round', tail: 'thin', leg: 0.5, slim: 1, fat: 1 }, { ar: 1.1, sz: 0.3 }],
  ['fairy', 'hada', { base: 'humanoid', body: 'pink', robe: 1, hair: 'gold', long: 1, wings: 'fairy', glow: 1, weapon: 'wand' }, { sz: 0.16, air: 1 }],
  ['angel', 'ángel', { base: 'humanoid', body: 'white', robe: 1, hair: 'gold', wings: 'angel', halo: 1 }, { air: 1 }],
  ['demon', 'demonio', { base: 'humanoid', body: 'black', skin: 'red', head: 'bald', wings: 'bat', horns: 1, tail: 'devil', bulky: 1 }],
  ['troll', 'trol', { base: 'humanoid', body: 'brown', skin: 'green', head: 'bald', legs: 'brown', boots: 'brown', bulky: 1, weapon: 'club' }, { sz: 0.4 }],
  ['orc', 'orco', { base: 'humanoid', body: 'darkstone', skin: 'green', head: 'hair', hair: 'black', legs: 'brown', armor: 1, weapon: 'axe' }],
  ['goblin', 'goblin', { base: 'humanoid', body: 'brown', skin: 'green', head: 'bald', ears: 1, thin: 1, weapon: 'spear' }, { sz: 0.18 }],
  ['golem', 'gólem', { base: 'humanoid', body: 'stone', skin: 'stone', head: 'bald', legs: 'stone', boots: 'darkstone', bulky: 1, glow: 1 }, { sz: 0.42 }],
  ['vampire', 'vampiro', { base: 'humanoid', body: 'black', skin: 'white', head: 'hair', hair: 'black', cape: 'red', legs: 'black', boots: 'black' }],
  ['witch', 'bruja', { base: 'humanoid', body: 'black', robe: 1, head: 'wizard', hair: 'gray', weapon: 'staff' }],
  ['archer', 'arquero', { base: 'humanoid', body: 'green', head: 'hair', hair: 'brown', legs: 'brown', boots: 'bark', weapon: 'bow', cape: 'green' }],
  ['viking', 'vikingo', { base: 'humanoid', body: 'brown', head: 'helm', legs: 'brown', armor: 1, weapon: 'axe', shield: 1 }],
  ['villager', 'aldeano', { base: 'humanoid', body: 'cream', head: 'hair', hair: 'brown', legs: 'brown', boots: 'bark' }],
  ['monk', 'monje', { base: 'humanoid', body: 'orange', robe: 1, head: 'bald' }],
  ['cathedral', 'catedral', { base: 'building', mat: 'stone', roof: 'gable', floors: 3, width: 'wide', towers: 2, rose: 1, arches: 1 }, { sz: 0.6 }],
  ['church', 'iglesia', { base: 'building', mat: 'white', roof: 'spire', floors: 2, width: 'narrow', arches: 1 }, { sz: 0.48 }],
  ['hut', 'cabaña tropical', { base: 'building', mat: 'wood', roof: 'thatch', floors: 1, width: 'normal', stilts: 1 }, { sz: 0.26 }],
  ['medievalhouse', 'casa medieval', { base: 'building', mat: 'timber', roof: 'gable', floors: 2, width: 'normal', chimney: 1 }, { sz: 0.34 }],
  ['mosque', 'mezquita', { base: 'building', mat: 'white', roof: 'dome', floors: 2, width: 'wide', towers: 2, arches: 1 }, { sz: 0.48 }],
  ['palace', 'palacio', { base: 'building', mat: 'white', roof: 'dome', floors: 3, width: 'wide', towers: 2, arches: 1, color: 'gold' }, { sz: 0.55 }],
  ['library', 'biblioteca', { base: 'building', mat: 'stone', roof: 'flat', floors: 2, width: 'wide', columns: 1 }, { sz: 0.42 }],
  ['museum', 'museo', { base: 'building', mat: 'white', roof: 'flat', floors: 2, width: 'wide', columns: 1 }, { sz: 0.42 }],
  ['owl', 'búho', { base: 'flyer', col: 'brown', wingCol: 'brown', beak: 'yellow' }],
  ['eagle', 'águila', { base: 'flyer', col: 'brown', wingCol: 'brown', beak: 'yellow' }, { sz: 0.12 }],
  ['bee', 'abeja', { base: 'flyer', col: 'yellow', wingCol: 'white', insect: 1 }],
  ['dragonfly', 'libélula', { base: 'flyer', col: 'cyan', wingCol: 'white', insect: 1 }],
];
for (const [id, name, kit, extra = {}] of KIT_PRESETS) {
  const d = kitDef(kit);
  reg(id, name, d.cat, extra.ar ?? d.ar, extra.sz ?? d.sz, d.build, { kit, ...(d.air || extra.air ? { air: 1 } : {}) });
}
void HUM; void QUAD; void poseOf;
