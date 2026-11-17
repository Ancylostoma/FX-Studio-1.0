// PixelScene v1 — formato intermedio compacto entre el prompt y el renderizador.
//
// {
//   "v":1, "w":64, "h":64,
//   "pal":[[r,g,b],...],          paleta; todo color se referencia por índice
//   "bg":0,                       índice de fondo, o -1 = transparente
//   "rmp":[[i0,i1,...],...],      rampas (oscuro → claro) para sombreado
//   "q":[[r,g,b],...],            (opc.) paleta de cuantización final
//   "fx":{"ld":-1,"amb":[r,g,b,k],"dth":2},  luz: dirección, ambiente, dithering
//   "sprites":{"id":{"w":8,"h":8,"o":[ops]}},
//   "layers":[{"n":"cielo","k":"sky","v":1,"a":1,"sh":0,"ol":-2,"nl":0,
//              "f":[c,k],"rf":[y,k],"o":[ops]}]
// }
//
// Operaciones (tuplas; el último parámetro `modo` es opcional:
// 0 = siempre, 1 = solo sobre píxeles opacos de la capa, 2 = solo sobre vacíos):
//   ["R",x,y,w,h,c]            rectángulo
//   ["L",x1,y1,x2,y2,c]        línea
//   ["E",cx,cy,rx,ry,c]        elipse rellena
//   ["P",[x1,y1,x2,y2,...],c]  polígono relleno
//   ["M",x,y,rows]             matriz de píxeles (filas de símbolos; "." = vacío)
//   ["S",id,x,y,flip]          instancia de sprite (flip: 1 = X, 2 = Y, 3 = ambos)
//   ["G",x,y,w,h,[c...],dir]   degradado tramado (dir 0 vertical, 1 horizontal)
//   ["D",x,y,w,h,c,nivel]      trama de Bayer (nivel 1..15 de 16)
//   ["T",x,y,w,h,c,dens,seed]  textura de motas determinista (dens en %)
//   ["B",x,y,w,h,id]           patrón: sprite repetido en mosaico
//   ["H",x,y,w,h,delta]        desplaza los píxeles por su rampa (luz/sombra)
// Un color c = -1 borra (deja transparente).

export const VERSION = 1;

export const LIMITS = Object.freeze({
  minDim: 8, maxDim: 256, maxPal: 96, maxQ: 64, maxLayers: 64, maxOps: 8000,
  maxSprites: 64, maxSpriteDim: 128, maxPts: 64, maxCoord: 1024,
  maxJson: 2_000_000, maxName: 48, maxGradient: 16,
});

export const SYMS = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';

// [nombre, tipo, opcional]
export const OP_SPEC = Object.freeze({
  R: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['c', 'c'], ['modo', 'm', 1]],
  L: [['x1', 'i'], ['y1', 'i'], ['x2', 'i'], ['y2', 'i'], ['c', 'c'], ['modo', 'm', 1]],
  E: [['cx', 'i'], ['cy', 'i'], ['rx', 'n'], ['ry', 'n'], ['c', 'c'], ['modo', 'm', 1]],
  P: [['pts', 'pts'], ['c', 'c'], ['modo', 'm', 1]],
  M: [['x', 'i'], ['y', 'i'], ['rows', 'rows']],
  S: [['id', 'id'], ['x', 'i'], ['y', 'i'], ['flip', 'f', 1]],
  G: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['cols', 'C'], ['dir', 'b', 1], ['modo', 'm', 1]],
  D: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['c', 'c'], ['nivel', 'lv', 1], ['modo', 'm', 1]],
  T: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['c', 'c'], ['dens', 'pc'], ['seed', 'sd'], ['modo', 'm', 1]],
  B: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['id', 'id'], ['modo', 'm', 1]],
  H: [['x', 'i'], ['y', 'i'], ['w', 'n'], ['h', 'n'], ['delta', 'dl']],
});

const isInt = Number.isInteger;
const intIn = (v, a, b) => isInt(v) && v >= a && v <= b;
const ID_RE = /^[\w:.\-]{1,32}$/;

const CHECK = {
  i: (v) => intIn(v, -LIMITS.maxCoord, LIMITS.maxCoord) || 'coordenada entera fuera de rango',
  n: (v) => intIn(v, 0, LIMITS.maxCoord) || 'tamaño inválido',
  c: (v, cx) => intIn(v, -1, cx.palN - 1) || `índice de color ${v} inexistente`,
  m: (v) => intIn(v, 0, 2) || 'modo inválido (0..2)',
  f: (v) => intIn(v, 0, 3) || 'flip inválido (0..3)',
  b: (v) => intIn(v, 0, 1) || 'dirección inválida (0|1)',
  lv: (v) => intIn(v, 1, 15) || 'nivel de trama inválido (1..15)',
  pc: (v) => intIn(v, 1, 100) || 'densidad inválida (1..100)',
  sd: (v) => intIn(v, 0, 0xffffffff) || 'semilla inválida',
  dl: (v) => (intIn(v, -4, 4) && v !== 0) || 'delta inválido (-4..4, ≠0)',
  id: (v, cx) => {
    if (typeof v !== 'string' || !ID_RE.test(v)) return 'id de sprite inválido';
    if (cx.inSprite) return 'los sprites no pueden anidar sprites';
    return Object.prototype.hasOwnProperty.call(cx.sprites, v) || `sprite "${v}" no definido`;
  },
  C: (v, cx) => {
    if (!Array.isArray(v) || v.length < 1 || v.length > LIMITS.maxGradient) return 'lista de colores inválida';
    return v.every((c) => intIn(c, 0, cx.palN - 1)) || 'índice de color inexistente en degradado';
  },
  pts: (v) => {
    if (!Array.isArray(v) || v.length < 4 || v.length % 2 || v.length > LIMITS.maxPts * 2) return 'lista de puntos inválida';
    return v.every((p) => intIn(p, -LIMITS.maxCoord, LIMITS.maxCoord)) || 'punto fuera de rango';
  },
  rows: (v, cx) => {
    if (!Array.isArray(v) || !v.length || v.length > LIMITS.maxDim) return 'matriz inválida';
    const len = v[0].length;
    if (!len || len > LIMITS.maxDim) return 'ancho de matriz inválido';
    for (const row of v) {
      if (row.length !== len) return 'filas de matriz con anchos distintos';
      if (typeof row === 'string') {
        for (const ch of row) {
          if (ch === '.') continue;
          const i = SYMS.indexOf(ch);
          if (i < 0 || i >= cx.palN) return `símbolo "${ch}" sin color en la paleta`;
        }
      } else if (Array.isArray(row)) {
        if (!row.every((c) => intIn(c, -1, cx.palN - 1))) return 'índice de color inexistente en matriz';
      } else return 'fila de matriz inválida';
    }
    return true;
  },
};

function checkOp(op, cx) {
  if (!Array.isArray(op) || typeof op[0] !== 'string') return 'operación mal formada';
  const spec = Object.prototype.hasOwnProperty.call(OP_SPEC, op[0]) ? OP_SPEC[op[0]] : null;
  if (!spec) return `operación "${op[0]}" no permitida`;
  const required = spec.filter((s) => !s[2]).length;
  if (op.length - 1 < required || op.length - 1 > spec.length) return `"${op[0]}": número de parámetros incorrecto`;
  for (let i = 1; i < op.length; i++) {
    const [name, kind] = spec[i - 1];
    const r = CHECK[kind](op[i], cx);
    if (r !== true) return `"${op[0]}".${name}: ${r}`;
  }
  return null;
}

const isRGB = (c) => Array.isArray(c) && c.length === 3 && c.every((v) => intIn(v, 0, 255));
const isObj = (o) => o !== null && typeof o === 'object' && !Array.isArray(o);

export function validateScene(s) {
  const errors = [];
  const err = (m) => { if (errors.length < 25) errors.push(m); };
  if (!isObj(s)) return { ok: false, errors: ['La escena debe ser un objeto JSON'] };
  if (s.v !== VERSION) err(`Versión no soportada (v=${s.v})`);
  for (const d of ['w', 'h']) if (!intIn(s[d], LIMITS.minDim, LIMITS.maxDim)) err(`"${d}" debe ser un entero entre ${LIMITS.minDim} y ${LIMITS.maxDim}`);
  if (!Array.isArray(s.pal) || !s.pal.length || s.pal.length > LIMITS.maxPal) err(`"pal" debe tener entre 1 y ${LIMITS.maxPal} colores`);
  else s.pal.forEach((c, i) => { if (!isRGB(c)) err(`pal[${i}] no es un color RGB válido`); });
  const palN = Array.isArray(s.pal) ? s.pal.length : 0;
  if (!intIn(s.bg, -1, palN - 1)) err('"bg" debe ser un índice de la paleta o -1');
  if (s.q !== undefined && (!Array.isArray(s.q) || !s.q.length || s.q.length > LIMITS.maxQ || !s.q.every(isRGB))) err('"q" inválida');
  if (s.rmp !== undefined) {
    if (!Array.isArray(s.rmp) || s.rmp.length > LIMITS.maxPal) err('"rmp" inválida');
    else s.rmp.forEach((r, i) => { if (!Array.isArray(r) || r.length < 2 || r.length > 16 || !r.every((c) => intIn(c, 0, palN - 1))) err(`rmp[${i}] inválida`); });
  }
  if (s.fx !== undefined) {
    if (!isObj(s.fx)) err('"fx" inválido');
    else {
      if (s.fx.ld !== undefined && s.fx.ld !== 1 && s.fx.ld !== -1) err('fx.ld debe ser -1 o 1');
      if (s.fx.dth !== undefined && !intIn(s.fx.dth, 0, 3)) err('fx.dth debe estar entre 0 y 3');
      if (s.fx.amb !== undefined && s.fx.amb !== null) {
        const a = s.fx.amb;
        if (!Array.isArray(a) || a.length !== 4 || !isRGB(a.slice(0, 3)) || !intIn(a[3], 0, 100)) err('fx.amb debe ser [r,g,b,k]');
      }
    }
  }
  let ops = 0;
  const sprites = isObj(s.sprites) ? s.sprites : {};
  if (s.sprites !== undefined) {
    if (!isObj(s.sprites)) err('"sprites" inválido');
    else {
      const ids = Object.keys(sprites);
      if (ids.length > LIMITS.maxSprites) err('Demasiados sprites');
      for (const id of ids) {
        const sp = sprites[id];
        if (!ID_RE.test(id)) { err(`id de sprite inválido: ${id}`); continue; }
        if (!isObj(sp) || !intIn(sp.w, 1, LIMITS.maxSpriteDim) || !intIn(sp.h, 1, LIMITS.maxSpriteDim) || !Array.isArray(sp.o)) { err(`sprite "${id}" mal formado`); continue; }
        ops += sp.o.length;
        sp.o.forEach((op, i) => { const e = checkOp(op, { palN, sprites, inSprite: true }); if (e) err(`sprite ${id}[${i}] ${e}`); });
      }
    }
  }
  if (!Array.isArray(s.layers) || s.layers.length > LIMITS.maxLayers) err(`"layers" debe ser una lista de hasta ${LIMITS.maxLayers} capas`);
  else s.layers.forEach((l, li) => {
    const at = `capa ${li}`;
    if (!isObj(l) || !Array.isArray(l.o)) return err(`${at}: estructura incorrecta`);
    if (l.n !== undefined && (typeof l.n !== 'string' || l.n.length > LIMITS.maxName)) err(`${at}: nombre inválido`);
    if (l.k !== undefined && (typeof l.k !== 'string' || l.k.length > LIMITS.maxName)) err(`${at}: clave inválida`);
    for (const flag of ['v', 'sh', 'nl']) if (l[flag] !== undefined && l[flag] !== 0 && l[flag] !== 1) err(`${at}: "${flag}" debe ser 0 o 1`);
    if (l.a !== undefined && !(typeof l.a === 'number' && l.a >= 0 && l.a <= 1)) err(`${at}: opacidad fuera de 0..1`);
    if (l.ol !== undefined && !(l.ol === -2 || intIn(l.ol, 0, palN - 1))) err(`${at}: contorno inválido`);
    if (l.f !== undefined && !(Array.isArray(l.f) && l.f.length === 2 && intIn(l.f[0], 0, palN - 1) && intIn(l.f[1], 0, 100))) err(`${at}: "f" inválido`);
    if (l.rf !== undefined && !(Array.isArray(l.rf) && l.rf.length === 2 && intIn(l.rf[0], 0, LIMITS.maxDim) && intIn(l.rf[1], 0, 100))) err(`${at}: "rf" inválido`);
    ops += l.o.length;
    l.o.forEach((op, i) => { const e = checkOp(op, { palN, sprites, inSprite: false }); if (e) err(`${at}, op ${i}: ${e}`); });
  });
  if (ops > LIMITS.maxOps) err(`Demasiadas operaciones (${ops} > ${LIMITS.maxOps})`);
  return { ok: errors.length === 0, errors };
}

// Análisis seguro de texto JSON: nunca se evalúa código.
export function parseScene(text) {
  if (typeof text !== 'string') return { ok: false, errors: ['Se esperaba texto JSON'] };
  if (text.length > LIMITS.maxJson) return { ok: false, errors: ['JSON demasiado grande'] };
  let scene;
  try { scene = JSON.parse(text); } catch (e) { return { ok: false, errors: ['JSON inválido: ' + e.message] }; }
  const r = validateScene(scene);
  return r.ok ? { ok: true, errors: [], scene } : r;
}

// Saneado tolerante para entradas no fiables (p. ej. la respuesta de un modelo
// de IA): redondea, recorta a los límites y descarta lo que no se puede reparar.
export function sanitizeScene(raw) {
  const warnings = [];
  if (!isObj(raw)) return { scene: null, warnings: ['La respuesta no es un objeto'] };
  const ri = (v, a, b, d) => (Number.isFinite(+v) ? Math.max(a, Math.min(b, Math.round(+v))) : d);
  const s = { v: VERSION, w: ri(raw.w, LIMITS.minDim, LIMITS.maxDim, 64), h: ri(raw.h, LIMITS.minDim, LIMITS.maxDim, 64) };
  s.pal = (Array.isArray(raw.pal) ? raw.pal : []).slice(0, LIMITS.maxPal)
    .filter((c) => Array.isArray(c) && c.length >= 3).map((c) => [ri(c[0], 0, 255, 0), ri(c[1], 0, 255, 0), ri(c[2], 0, 255, 0)]);
  if (!s.pal.length) { s.pal = [[0, 0, 0]]; warnings.push('Paleta vacía'); }
  const palN = s.pal.length;
  s.bg = ri(raw.bg, -1, palN - 1, -1);
  if (Array.isArray(raw.rmp)) s.rmp = raw.rmp.filter((r) => Array.isArray(r) && r.length >= 2 && r.length <= 16 && r.every((c) => intIn(c, 0, palN - 1)));
  if (isObj(raw.fx)) {
    s.fx = {};
    if (raw.fx.ld === 1 || raw.fx.ld === -1) s.fx.ld = raw.fx.ld;
    if (raw.fx.dth !== undefined) s.fx.dth = ri(raw.fx.dth, 0, 3, 2);
    if (Array.isArray(raw.fx.amb) && raw.fx.amb.length === 4) s.fx.amb = [0, 1, 2].map((i) => ri(raw.fx.amb[i], 0, 255, 255)).concat(ri(raw.fx.amb[3], 0, 100, 0));
  }
  let budget = LIMITS.maxOps;
  const fixOp = (op, cx) => {
    if (!Array.isArray(op) || !OP_SPEC[op[0]]) return null;
    const spec = OP_SPEC[op[0]];
    const out = [op[0]];
    for (let i = 0; i < spec.length && i + 1 < op.length; i++) {
      let v = op[i + 1];
      const kind = spec[i][1];
      if (typeof v === 'number') v = Math.round(v);
      if (kind === 'i') v = ri(v, -LIMITS.maxCoord, LIMITS.maxCoord, 0);
      else if (kind === 'n') v = ri(v, 0, LIMITS.maxCoord, 0);
      else if (kind === 'pts' && Array.isArray(v)) v = v.map((p) => ri(p, -LIMITS.maxCoord, LIMITS.maxCoord, 0));
      out.push(v);
    }
    return checkOp(out, cx) ? null : out;
  };
  const fixOps = (list, cx) => {
    const out = [];
    for (const op of Array.isArray(list) ? list : []) {
      if (budget <= 0) break;
      const f = fixOp(op, cx);
      if (f) { out.push(f); budget--; } else warnings.push('Operación descartada: ' + JSON.stringify(op).slice(0, 60));
    }
    return out;
  };
  s.sprites = {};
  if (isObj(raw.sprites)) {
    for (const id of Object.keys(raw.sprites).slice(0, LIMITS.maxSprites)) {
      const sp = raw.sprites[id];
      if (!ID_RE.test(id) || !isObj(sp)) continue;
      s.sprites[id] = { w: ri(sp.w, 1, LIMITS.maxSpriteDim, 8), h: ri(sp.h, 1, LIMITS.maxSpriteDim, 8), o: [] };
    }
    for (const id of Object.keys(s.sprites)) s.sprites[id].o = fixOps(raw.sprites[id].o, { palN, sprites: s.sprites, inSprite: true });
  }
  s.layers = [];
  for (const l of (Array.isArray(raw.layers) ? raw.layers : []).slice(0, LIMITS.maxLayers)) {
    if (!isObj(l)) continue;
    const nl = { n: String(l.n ?? 'capa').slice(0, LIMITS.maxName), o: fixOps(l.o, { palN, sprites: s.sprites, inSprite: false }) };
    if (typeof l.k === 'string') nl.k = l.k.slice(0, LIMITS.maxName);
    if (l.v === 0) nl.v = 0;
    if (typeof l.a === 'number' && l.a >= 0 && l.a < 1) nl.a = l.a;
    if (l.sh === 1) nl.sh = 1;
    if (l.nl === 1) nl.nl = 1;
    if (l.ol === -2 || intIn(l.ol, 0, palN - 1)) nl.ol = l.ol;
    if (Array.isArray(l.f) && l.f.length === 2 && intIn(l.f[0], 0, palN - 1)) nl.f = [l.f[0], ri(l.f[1], 0, 100, 0)];
    if (Array.isArray(l.rf) && l.rf.length === 2) nl.rf = [ri(l.rf[0], 0, LIMITS.maxDim, 0), ri(l.rf[1], 0, 100, 0)];
    s.layers.push(nl);
  }
  if (!s.layers.length) warnings.push('La escena no contiene capas');
  const r = validateScene(s);
  if (!r.ok) return { scene: null, warnings: warnings.concat(r.errors) };
  return { scene: s, warnings };
}

// Devuelve una copia de la operación con sus colores transformados por fn.
export function mapOpColors(op, fn) {
  const spec = OP_SPEC[op[0]];
  const out = [op[0]];
  const f = (c) => (c < 0 ? c : fn(c));
  for (let i = 1; i < op.length; i++) {
    const kind = spec[i - 1][1];
    let v = op[i];
    if (kind === 'c') v = f(v);
    else if (kind === 'C') v = v.map(f);
    else if (kind === 'rows') {
      const rows = v.map((row) => (typeof row === 'string' ? [...row].map((ch) => (ch === '.' ? -1 : SYMS.indexOf(ch))) : row).map(f));
      const fits = rows.every((r) => r.every((c) => typeof c === 'number' && c < SYMS.length));
      v = fits ? rows.map((r) => r.map((c) => (c < 0 ? '.' : SYMS[c])).join('')) : rows;
    }
    out.push(v);
  }
  return out;
}

export function mapLayerColors(layer, fn) {
  const l = { ...layer, o: layer.o.map((op) => mapOpColors(op, fn)) };
  if (l.ol !== undefined && l.ol >= 0) l.ol = fn(l.ol);
  if (l.f) l.f = [fn(l.f[0]), l.f[1]];
  return l;
}

// Recorre todos los índices de color usados por la escena.
export function mapSceneColors(scene, fn) {
  const s = { ...scene };
  if (s.bg >= 0) s.bg = fn(s.bg);
  s.layers = scene.layers.map((l) => mapLayerColors(l, fn));
  if (scene.sprites) {
    s.sprites = {};
    for (const [id, sp] of Object.entries(scene.sprites)) s.sprites[id] = { ...sp, o: sp.o.map((op) => mapOpColors(op, fn)) };
  }
  if (scene.rmp) s.rmp = scene.rmp.map((r) => r.map(fn));
  return s;
}

export function spriteRefs(ops) {
  const ids = new Set();
  for (const op of ops) { if (op[0] === 'S') ids.add(op[1]); else if (op[0] === 'B') ids.add(op[5]); }
  return ids;
}

// Firma de una capa independiente de la numeración de la paleta: permite
// saber si dos capas generadas son visualmente idénticas.
export function layerSignature(scene, layer) {
  const hex = (i) => '#' + scene.pal[i].map((v) => v.toString(16).padStart(2, '0')).join('');
  const res = (l) => { const m = mapLayerColorsRaw(l, hex); return m; };
  const sprites = {};
  for (const id of spriteRefs(layer.o)) sprites[id] = { ...scene.sprites[id], o: scene.sprites[id].o.map((op) => mapOpColorsRaw(op, hex)) };
  return JSON.stringify([res(layer), sprites]);
}

function mapOpColorsRaw(op, fn) {
  const spec = OP_SPEC[op[0]];
  return op.map((v, i) => {
    if (i === 0) return v;
    const kind = spec[i - 1][1];
    if (kind === 'c') return v < 0 ? v : fn(v);
    if (kind === 'C') return v.map(fn);
    if (kind === 'rows') return v.map((row) => (typeof row === 'string' ? [...row].map((ch) => (ch === '.' ? -1 : SYMS.indexOf(ch))) : row).map((c) => (c < 0 ? c : fn(c))));
    return v;
  });
}
function mapLayerColorsRaw(layer, fn) {
  const l = { ...layer, o: layer.o.map((op) => mapOpColorsRaw(op, fn)) };
  if (l.ol !== undefined && l.ol >= 0) l.ol = fn(l.ol);
  if (l.f) l.f = [fn(l.f[0]), l.f[1]];
  return l;
}

export function sceneStats(scene) {
  let ops = 0;
  for (const l of scene.layers) ops += l.o.length;
  for (const sp of Object.values(scene.sprites || {})) ops += sp.o.length;
  return { ops, layers: scene.layers.length, colors: scene.pal.length, sprites: Object.keys(scene.sprites || {}).length, bytes: JSON.stringify(scene).length };
}

export const cloneScene = (s) => JSON.parse(JSON.stringify(s));
