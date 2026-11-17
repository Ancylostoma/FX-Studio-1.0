// Compositor: especificación → PixelScene por capas.
// La escena se genera por "partes" (cielo, astros, relieve, suelo, cada
// estructura, cada sujeto, clima…). Cada parte usa su propio sub-generador
// aleatorio, así que cambiar una no altera las demás: es lo que permite
// "conserva el castillo, cambia el cielo" sin regenerarlo todo.
import { RNG } from '../core/randomSeed.js';
import { PaletteBuilder, compactPalette, PALETTES } from '../core/paletteManager.js';
import { AMBIENTS } from '../core/lighting.js';
import { VERSION, LIMITS, validateScene, mapLayerColors, mapOpColors, layerSignature, spriteRefs } from '../core/pixelScene.js';
import { STYLES, BIOMES, SKIES, DETAIL_FACTOR } from './templates.js';
import { LIB } from './spriteLibrary.js';
import { Pen, Kit, L, skyColors, genSky, genStars, genCelestial, genClouds, genFar, genGround, genWater, genWeather } from './proceduralGenerator.js';
import { finalize } from './promptParser.js';

export const RESOLUTIONS = [16, 32, 48, 64, 96, 128, 256];
export const ASPECTS = { '1:1': [1, 1], '16:9': [16, 9], '9:16': [9, 16], '4:3': [4, 3], '3:2': [3, 2] };

// La resolución es el lado mayor; el otro se deriva de la proporción.
export function dimensions(res, aspect = '1:1') {
  const [a, b] = ASPECTS[aspect] || [1, 1];
  const r = Math.max(LIMITS.minDim, Math.min(LIMITS.maxDim, res | 0));
  const fit = (v) => Math.max(LIMITS.minDim, Math.round(v));
  return a >= b ? { w: r, h: fit((r * b) / a) } : { w: fit((r * a) / b), h: r };
}

export function normalizeOptions(o = {}) {
  return {
    w: o.w ?? 64, h: o.h ?? 64,
    style: STYLES[o.style] ? o.style : 'bit16',
    detail: DETAIL_FACTOR[o.detail] ? +o.detail : 2,
    seed: (o.seed >>> 0) || 1,
    palette: PALETTES[o.palette] ? o.palette : 'auto',
  };
}

const SIZE = { '-1': 0.72, 0: 1, 1: 1.35 };

function buildContext(spec, o) {
  const st = STYLES[o.style];
  const root = new RNG(o.seed);
  const pb = new PaletteBuilder();
  const C = new Kit(pb, st);
  const { w, h } = o;
  const G = { w, h, spec, style: st, C, pb, sprites: {}, detail: st.detail * DETAIL_FACTOR[o.detail] * (w < 48 ? 0.6 : 1), rng: (l) => root.fork(l) };
  G.layoutKind = st.layout === 'sprite' || !spec.biome ? 'portrait' : st.layout === 'iso' ? 'iso' : 'scene';
  G.transparent = st.layout === 'sprite';
  G.biome = BIOMES[spec.biome || 'meadow'];
  G.timeKey = spec.time;
  const bad = spec.weather.includes('rain') || spec.weather.includes('storm');
  const skyKey = G.biome.sky || (bad && spec.time !== 'night' ? 'storm' : spec.time);
  G.sky = SKIES[skyKey];
  G.night = spec.time === 'night';
  G.lights = G.night || spec.time === 'sunset' || bad || skyKey === 'space' || !!G.biome.neon;
  G.amb = G.biome.sky === 'space' ? null : AMBIENTS[G.sky.amb]?.amb || null;
  if (bad && spec.time === 'night') G.amb = AMBIENTS.night.amb;
  G.H = G.biome.noGround ? h : Math.round(h * Math.min(0.8, G.biome.hz + (st.vista ? 0.03 : 0)));
  const lay = root.fork('layout');
  G.moonSide = lay.chance(0.5) ? 1 : -1;
  if (G.layoutKind === 'portrait') {
    G.skyIdx = (G.night ? ['12142b', '1c2145', '2a3266'] : ['39406b', '566199', '8fa3cf']).map((c) => C.rgb(c));
    G.baseY = Math.round(h * 0.9);
    G.starDensity = 0;
    G.farFeatures = [];
    G.amb = null;
  } else {
    G.skyIdx = skyColors(G).map((c) => C.rgb(c));
    G.starDensity = spec.celestial.includes('stars') ? G.sky.stars || (spec.time === 'day' ? 0 : 0.4) : 0;
    G.waterKind = G.biome.water || (spec.features.includes('lake') ? 'lake' : null);
    if (G.layoutKind === 'iso') { G.waterKind = null; G.H = Math.round(h * 0.86); }
    if (G.waterKind === 'lake') G.shoreY = G.H + Math.round((h - G.H) * 0.52);
    if (G.waterKind === 'beach') G.shoreY = G.H + Math.round((h - G.H) * 0.42);
    let far = spec.features.filter((f) => f !== 'lake');
    if (!far.length || (far.length === 1 && far[0] === 'hills')) far = [...new Set([...G.biome.far, ...far])];
    if (G.biome.snowy && !far.includes('mountains')) far.unshift('mountains');
    if (far.includes('fuji')) far = far.filter((f) => f !== 'mountains' && f !== 'hills');
    if (spec.features.includes('mountains') && far.includes('hills')) far = far.filter((f) => f !== 'hills');
    G.farFeatures = G.layoutKind === 'iso' || G.biome.noGround ? [] : far;
    if (G.layoutKind === 'iso') G.iso = { cx: w >> 1, cy: Math.round(h * 0.6), rx: Math.round(w * 0.45), ry: Math.round(w * 0.2), th: Math.round(h * 0.13) };
  }
  G.skyHorizon = G.skyIdx[G.skyIdx.length - 1];
  layout(G, lay);
  return G;
}

// Calcula las cajas de estructuras y sujetos (composición por tercios).
function layout(G, rng) {
  const { w, h, H, spec } = G;
  const side = G.moonSide;
  G.boxes = []; G.islets = [];
  const subs = [];
  const pool = G.layoutKind === 'portrait' ? [...spec.subjects, ...spec.structures, ...spec.flora] : spec.subjects;
  for (const s of pool) for (let i = 0; i < Math.min(5, s.n || 1); i++) subs.push({ e: s, def: LIB[s.id], i });
  const grounded = subs.filter((s) => !s.e.fly && !(s.def.float && G.waterKind));
  const fliers = subs.filter((s) => s.e.fly);
  const floaters = subs.filter((s) => !s.e.fly && s.def.float && G.waterKind);
  const fit = (def, hh, maxW) => { let bh = Math.round(hh); let bw = Math.round(bh * def.ar); if (bw > maxW) { bw = Math.round(maxW); bh = Math.round(bw / def.ar); } return { bw: Math.max(3, bw), bh: Math.max(3, bh) }; };
  G.structs = []; G.subs = [];
  if (G.layoutKind === 'portrait') {
    const n = subs.length || 1;
    subs.forEach((s, i) => {
      const { bw, bh } = fit(s.def, h * (s.def.cat === 'object' ? 0.62 : 0.72) * (n > 1 ? 0.8 : 1), (w * 0.86) / n);
      const cx = Math.round((w * (i + 0.5)) / n);
      G.subs.push({ ...s, x: cx - (bw >> 1), y: (s.e.fly && s.def.cat !== 'vehicle' ? Math.round(h * 0.82) : G.baseY) - bh, w: bw, h: bh, flip: false, air: s.e.fly, depth: 1 });
    });
    return;
  }
  if (G.layoutKind === 'iso') {
    const { cx, cy, rx, ry } = G.iso;
    spec.structures.slice(0, 2).forEach((e, i) => {
      const def = LIB[e.id], { bw, bh } = fit(def, h * def.sz * 0.85 * SIZE[e.size], rx * 1.0);
      const x = cx + (i ? -side : side) * Math.round(rx * (spec.structures.length > 1 ? 0.35 : 0.12)), by = cy - Math.round(ry * 0.1);
      G.structs.push({ e, def, x: x - (bw >> 1), y: by - bh, w: bw, h: bh, by });
      G.boxes.push([x - (bw >> 1), x + (bw >> 1), by]);
    });
    grounded.concat(floaters).forEach((s, i, arr) => {
      const { bw, bh } = fit(s.def, h * s.def.sz * 0.9 * SIZE[s.e.size], rx * 0.5);
      const x = cx - side * Math.round(rx * 0.3) + Math.round((i - (arr.length - 1) / 2) * bw * 1.1), by = cy + Math.round(ry * 0.45);
      G.subs.push({ ...s, x: x - (bw >> 1), y: by - bh, w: bw, h: bh, flip: x > cx, depth: 1 });
    });
    fliers.forEach((s, i) => {
      const { bw, bh } = fit(s.def, h * s.def.sz * 0.9 * SIZE[s.e.size], w * 0.5);
      const x = Math.round(w * (0.5 - side * 0.2)) + i * Math.round(bw * 0.6), y = Math.round(h * 0.08) + i * Math.round(bh * 0.5);
      G.subs.push({ ...s, x: x - (bw >> 1), y, w: bw, h: bh, flip: x > w / 2, air: true, depth: 0 });
    });
    return;
  }
  const lake = G.waterKind === 'lake', sea = G.waterKind === 'sea';
  const gTop = G.waterKind && !sea ? G.shoreY + Math.round((h - G.shoreY) * 0.2) : H;
  const hasFg = grounded.length > 0;
  // Estructuras
  spec.structures.slice(0, 3).forEach((e, i) => {
    const def = LIB[e.id];
    const farShore = lake || G.waterKind === 'beach';
    const scale = (farShore ? 0.62 : 1) * (i ? 0.78 : 1) * (fliers.length ? 0.88 : 1) * SIZE[e.size];
    const by = farShore ? H + 1 : sea ? H + Math.round((h - H) * 0.3) : H + Math.round((h - H) * (def.id === 'castle' ? 0.16 : 0.24));
    const { bw, bh } = fit(def, Math.min(h * def.sz * scale, by - h * 0.04), w * (i ? 0.4 : 0.72));
    const n = Math.min(3, spec.structures.length);
    let cx = n === 1 ? 0.5 + side * (hasFg ? 0.16 : fliers.length ? 0.1 : 0.04) : [0.5 + side * 0.2, 0.5 - side * 0.24, 0.5][i];
    cx = Math.round(w * cx);
    const b = { e, def, x: cx - (bw >> 1), y: by - bh, w: bw, h: bh, by };
    G.structs.push(b); G.boxes.push([b.x - 1, b.x + bw + 1, by]);
    if (sea) G.islets.push({ x: cx, y: by, w: bw });
  });
  const main = G.structs[0];
  // Sujetos en tierra
  grounded.forEach((s, i) => {
    const by = sea ? H + Math.round((h - H) * 0.55) : gTop + Math.round((h - gTop) * 0.62);
    const { bw, bh } = fit(s.def, Math.min(h * s.def.sz * 1.2 * SIZE[s.e.size], by - 2), w * 0.5);
    const base = main ? 0.5 - side * 0.22 : 0.5 + (grounded.length > 1 ? 0 : -side * 0.06);
    const cx = Math.max((bw >> 1) + 1, Math.min(w - (bw >> 1) - 1, Math.round(w * (grounded.length > 2 && !main ? 0.5 : base) + (i - (grounded.length - 1) / 2) * Math.min(bw * 1.25, (w * 0.9) / grounded.length))));
    const b = { ...s, x: cx - (bw >> 1), y: by - bh, w: bw, h: bh, by, flip: main ? cx > main.x + main.w / 2 : cx > w / 2, depth: 1 };
    G.subs.push(b); G.boxes.push([b.x - 1, b.x + bw + 1, by]);
    if (sea) G.islets.push({ x: cx, y: by, w: bw });
  });
  floaters.forEach((s, i) => {
    const bottom = sea ? h : G.shoreY;
    const by = H + Math.round((bottom - H) * 0.5);
    const { bw, bh } = fit(s.def, h * s.def.sz * SIZE[s.e.size], w * 0.45);
    const cx = Math.round(w * (0.5 - side * 0.16) + i * bw * 1.3);
    G.subs.push({ ...s, x: cx - (bw >> 1), y: by - bh, w: bw, h: bh, by, flip: cx > w / 2, depth: 0.5, floating: true });
  });
  fliers.forEach((s, i) => {
    const skyH = G.biome.noGround ? h : H;
    const { bw, bh } = fit(s.def, Math.min(h * s.def.sz * SIZE[s.e.size], skyH * 0.62), w * 0.58);
    const cx = Math.round(w * (0.5 - side * (main ? 0.2 : 0.08)) + (i % 2 ? 1 : -1) * Math.ceil(i / 2) * bw * 0.9);
    const cy = Math.round(skyH * (G.biome.noGround ? 0.5 : main ? 0.26 : 0.34) + (i ? rng.range(-0.1, 0.12) * skyH : 0));
    G.subs.push({ ...s, x: cx - (bw >> 1), y: Math.max(1, cy - (bh >> 1)), w: bw, h: bh, flip: side < 0 ? false : true, air: true, depth: 0 });
  });
  const pathOk = main && !G.waterKind && ['grass', 'snow'].includes(G.biome.ground) && ['castle', 'house', 'tower', 'temple', 'windmill'].includes(main.def.id);
  if (pathOk) G.path = { x0: main.x + (main.w >> 1), y0: main.by, x1: Math.round(w * (0.5 - side * 0.1)), wide: Math.max(2, Math.round(w * 0.07)) };
  G.gTop = gTop;
}

function outlineProps(G, kind) {
  const p = {};
  if (G.style.sh) p.sh = 1;
  if (G.style.ol === 'dark') p.ol = G.C.dark;
  else if (G.style.ol === 'sel' && kind !== 'struct') p.ol = -2;
  return p;
}

// Dibuja una entidad de la biblioteca en una caja y devuelve sus capas.
function drawEntity(G, box, key, rngLabel) {
  const { def, e } = box;
  const main = [], lit = [], glow = [];
  const p = new Pen(main, box.x, box.y, box.w, box.h, box.flip);
  const k = { C: G.C, a: { ...e.a, color: e.color, color2: e.color2, fly: box.air, snowy: G.biome.snowy, mat: G.biome.ground === 'mars' ? 'mars' : G.biome.ground === 'moon' ? 'moon' : undefined }, lit: p.on(lit), glow, night: G.lights, rng: G.rng(rngLabel), neon: G.biome.neon, space: G.biome.noGround, beamDir: box.flip ? -1 : 1, sprites: G.sprites };
  def.build(p, k);
  const name = def.name + (box.i ? ' ' + (box.i + 1) : '');
  const out = [];
  if (box.by != null && !box.air && !box.floating && G.layoutKind !== 'portrait' && def.cat !== 'structure') {
    out.push(L('sombra de ' + name, key, [['E', box.x + (box.w >> 1), box.by, Math.max(1, Math.round(box.w * 0.42)), Math.max(0, Math.round(box.w * 0.07)), G.C.dark]], { a: 0.28, nl: 1 }));
  }
  if (glow.length) out.push(L('halo de ' + name, key, glow, { nl: 1, a: 0.2 }));
  const props = outlineProps(G, def.cat === 'structure' ? 'struct' : 'subj');
  if (def.alpha) props.a = def.alpha;
  out.push(L(name, key, main, props));
  if (lit.length) out.push(L('luces de ' + name, key, lit, { nl: 1 }));
  return out;
}

// Flora y atrezo: instancias de sprites reutilizables (S) en tres tamaños.
function genProps(G) {
  const { w, h, H, spec } = G, rng = G.rng('props');
  if (G.layoutKind === 'portrait' || G.biome.noGround) return { back: [], front: [] };
  const items = [];
  const iso = G.layoutKind === 'iso';
  const sea = G.waterKind === 'sea';
  const gTop = iso ? 0 : G.gTop, gH = h - gTop;
  const blocked = (x, bw, by) => G.boxes.some(([a, b, y]) => x + bw > a && x < b && Math.abs(by - y) < h * 0.2);
  const place = (id, big, explicit, e) => {
    const def = LIB[id];
    for (let tries = 0; tries < 14; tries++) {
      let x, by, t;
      if (iso) {
        const { cx, cy, rx, ry } = G.iso;
        const a = rng.range(-1, 1), b = rng.range(-1, 1);
        if (Math.abs(a) + Math.abs(b) > 0.82) continue;
        x = cx + a * rx; by = Math.round(cy + b * ry); t = (b + 1) / 2;
      } else {
        t = big ? (!G.biome.dense || rng.chance(0.75) ? rng.range(0.02, 0.3) : rng.range(0.55, 0.95)) : rng.range(0.08, 1);
        if (sea) return;
        by = Math.round(gTop + gH * (0.04 + t * 0.9));
        x = rng.range(-0.03, 1.03) * w;
        if (big && t > 0.5) x = rng.chance(0.5) ? rng.range(-0.04, 0.14) * w : rng.range(0.86, 1.04) * w;
        if (explicit) { x = (0.5 + (tries & 1 ? 1 : -1) * rng.range(0.12, 0.4)) * w; t = rng.range(0.2, 0.6); by = Math.round(gTop + gH * (0.04 + t * 0.9)); }
      }
      const lvl = t < 0.33 ? 0 : t < 0.66 ? 1 : 2;
      const scale = (iso ? 0.7 : [0.72, 0.95, 1.25][lvl]) * (explicit ? 1.15 : 1) * (e ? SIZE[e.size] : 1);
      const bh = Math.max(3, Math.round(h * def.sz * scale)), bw = Math.max(3, Math.round(bh * def.ar));
      x = Math.round(x - bw / 2);
      if (blocked(x, bw, by) && !(big && t > 0.5)) continue;
      if (G.waterKind && !iso && by < G.shoreY + 2) continue;
      items.push({ id, def, x, by, bw, bh, lvl, e: e || { a: {} }, flip: rng.chance(0.5), explicit });
      if (explicit) G.boxes.push([x - 1, x + bw + 1, by]);
      return;
    }
  };
  for (const e of spec.flora) for (let i = 0; i < Math.max(1, Math.min(6, e.n)); i++) place(e.id, LIB[e.id].sz > 0.15, true, e);
  const pool = G.biome.flora.filter(([id]) => !spec.no?.includes(id));
  const total = pool.length ? Math.round((G.biome.dense ? 15 : 8) * G.detail * (iso ? 0.7 : 1) * Math.max(0.6, w / h)) : 0;
  const sum = pool.reduce((a, [, wt]) => a + wt, 0);
  for (let i = 0; i < total; i++) {
    let r = rng.range(0, sum), id = pool[0][0];
    for (const [pid, wt] of pool) { if ((r -= wt) <= 0) { id = pid; break; } }
    place(id, LIB[id].sz > 0.15, false);
  }
  items.sort((a, b) => a.by - b.by);
  const frontY = Math.max(...G.subs.filter((s) => s.by != null).map((s) => s.by), ...G.structs.map((s) => s.by + 2), 0);
  const mk = () => ({ main: [], lit: [], glow: [] });
  const B = mk(), F = mk();
  for (const it of items) {
    const T = it.by > frontY ? F : B;
    const attrs = { ...it.e.a, color: it.e.color, color2: it.e.color2, snowy: G.biome.snowy, mat: G.biome.ground === 'mars' ? 'mars' : G.biome.ground === 'moon' ? 'moon' : undefined };
    const pad = Math.ceil(it.bh * 0.3);
    const sid = `${it.id}${it.lvl}${it.explicit ? 'x' : ''}${it.e.color ? '_' + it.e.color : ''}_${it.bw}x${it.bh}`;
    const kOf = (lit, glow) => ({ C: G.C, a: attrs, lit, glow, night: G.lights, rng: G.rng('prop:' + sid), neon: G.biome.neon });
    if (!G.sprites[sid] && !G.noSprite?.has(sid)) {
      const so = [], lo = [], go = [];
      const sp = new Pen(so, pad, pad, it.bw, it.bh);
      it.def.build(sp, kOf(sp.on(lo), go));
      if (lo.length || go.length || it.id === 'flower') (G.noSprite ||= new Set()).add(sid);
      else G.sprites[sid] = { w: Math.min(LIMITS.maxSpriteDim, it.bw + 2 * pad), h: Math.min(LIMITS.maxSpriteDim, it.bh + 2 * pad), o: so };
    }
    if (G.sprites[sid]) T.main.push(['S', sid, it.x - pad, it.by - it.bh - pad, it.flip ? 1 : 0]);
    else { const p = new Pen(T.main, it.x, it.by - it.bh, it.bw, it.bh, it.flip); it.def.build(p, { ...kOf(p.on(T.lit), T.glow), rng: G.rng('prop:' + it.x + ':' + it.by) }); }
  }
  const pack = (T, name, key) => {
    const out = [];
    if (T.glow.length) out.push(L('halos de ' + name, key, T.glow, { nl: 1, a: 0.2 }));
    if (T.main.length) out.push(L(name, key, T.main, outlineProps(G, 'subj')));
    if (T.lit.length) out.push(L('luces de ' + name, key, T.lit, { nl: 1 }));
    return out;
  };
  return { back: pack(B, 'vegetación', 'props'), front: pack(F, 'primer plano', 'front') };
}

// Genera la escena completa a partir de una especificación ya finalizada.
export function composeScene(spec, options) {
  const o = normalizeOptions(options);
  const G = buildContext(spec, o);
  const { C } = G;
  let layers = [];
  const add = (ls) => { for (const l of ls) if (l.o.length) layers.push(l); };
  if (!G.transparent) add(genSky(G));
  if (G.layoutKind !== 'portrait') {
    add(genStars(G));
    add(genCelestial(G));
    G.bodyX = G.bodyPos?.cx ?? null;
    add(genClouds(G));
    add(genFar(G));
  }
  const water = G.waterKind && G.layoutKind === 'scene';
  if (!water) add(genGround(G));
  else if (G.waterKind !== 'sea') {
    const g = C.mat(G.biome.ground);
    add([L('orilla lejana', 'far', [['R', 0, G.H - 1, G.w, 3, g[1]], ['T', 0, G.H - 2, G.w, 1, g[1], 40, 7]], { f: [G.skyHorizon, 20] })]);
  }
  const props = genProps(G);
  G.structs.forEach((b, i) => add(drawEntity(G, b, 'struct:' + b.def.id + (i ? i : ''), 'struct' + i)));
  if (water) { add(genWater(G)); add(genGround(G)); }
  add(props.back);
  const order = [...G.subs].sort((a, b) => (a.air ? 1 : 0) - (b.air ? 1 : 0) || (a.by || 0) - (b.by || 0));
  order.forEach((b) => add(drawEntity(G, b, 'subj:' + b.def.id + (b.i ? b.i : ''), 'subj:' + b.def.id + b.i)));
  add(props.front);
  if (G.layoutKind !== 'portrait') add(genWeather(G));
  if (G.style.magic && G.layoutKind === 'portrait' && !G.transparent) add(genWeather({ ...G, spec: { ...spec, weather: [], fx: ['sparkles'] }, biome: { ...G.biome, noGround: 1 } }));
  if (layers.length > LIMITS.maxLayers) layers = layers.slice(0, LIMITS.maxLayers);
  let scene = {
    v: VERSION, w: G.w, h: G.h, pal: G.pb.pal, bg: G.transparent ? -1 : G.skyHorizon, rmp: G.pb.rmp,
    fx: { ld: -1, dth: G.style.dth, ...(G.amb ? { amb: G.amb } : {}) },
    sprites: G.sprites, layers,
  };
  const qKey = o.palette !== 'auto' ? o.palette : G.style.q;
  if (qKey && PALETTES[qKey]?.colors) scene.q = PALETTES[qKey].colors;
  scene = compactPalette(pruneSprites(scene));
  const v = validateScene(scene);
  if (!v.ok) throw new Error('El generador produjo una escena inválida: ' + v.errors.join('; '));
  return scene;
}

function pruneSprites(scene) {
  const used = new Set();
  for (const l of scene.layers) for (const id of spriteRefs(l.o)) used.add(id);
  const sprites = {};
  for (const id of Object.keys(scene.sprites || {})) if (used.has(id)) sprites[id] = scene.sprites[id];
  return { ...scene, sprites };
}

// Edición parcial: regenera solo las partes cuya salida cambia entre la
// especificación anterior y la nueva; el resto de capas (incluidas las
// editadas a mano) se conserva tal cual.
export function editScene(scene, rawOld, rawNew, options) {
  const A = composeScene(finalize(rawOld), options), B = composeScene(finalize(rawNew), options);
  const sigs = (S) => { const m = new Map(); for (const l of S.layers) { const k = l.k || l.n; if (!m.has(k)) m.set(k, []); m.get(k).push(layerSignature(S, l)); } return m; };
  const sa = sigs(A), sb = sigs(B);
  const changed = new Set();
  for (const k of new Set([...sa.keys(), ...sb.keys()])) if (JSON.stringify(sa.get(k) || []) !== JSON.stringify(sb.get(k) || [])) changed.add(k);
  const pb = new PaletteBuilder(scene.pal.map((c) => [...c]), (scene.rmp || []).map((r) => [...r]));
  const map = (i) => pb.idx(B.pal[i]);
  const sprites = { ...(scene.sprites || {}) };
  const layers = [], seen = new Set();
  const cur = new Map();
  for (const l of scene.layers) { const k = l.k || l.n; if (!cur.has(k)) cur.set(k, []); cur.get(k).push(l); }
  for (const l of B.layers) {
    const k = l.k || l.n;
    if (changed.has(k)) {
      layers.push(mapLayerColors(l, map));
      for (const id of spriteRefs(l.o)) sprites[id] = { ...B.sprites[id], o: B.sprites[id].o.map((op) => mapOpColors(op, map)) };
    } else if (!seen.has(k)) { for (const c of cur.get(k) || []) layers.push(c); }
    seen.add(k);
  }
  // Capas propias del usuario (retoques, importadas) que el generador no conoce.
  for (const l of scene.layers) { const k = l.k || l.n; if (!sb.has(k) && !sa.has(k)) layers.push(l); }
  for (const r of B.rmp || []) pb.rmp.push(r.map(map));
  let out = { ...scene, pal: pb.pal, rmp: pb.rmp, bg: B.bg < 0 ? -1 : map(B.bg), fx: { ...B.fx, ld: scene.fx?.ld ?? B.fx.ld, dth: scene.fx?.dth ?? B.fx.dth }, sprites, layers: layers.slice(0, LIMITS.maxLayers) };
  if (out.pal.length > LIMITS.maxPal) out.pal = out.pal.slice(0, LIMITS.maxPal);
  out = compactPalette(pruneSprites(out));
  const v = validateScene(out);
  if (!v.ok) return { scene: B, changed: [...changed], rebuilt: true };
  return { scene: out, changed: [...changed], rebuilt: false };
}

// Punto de entrada del modo offline.
export function generate(raw, options) {
  const spec = finalize(raw);
  return { scene: composeScene(spec, options), spec };
}
