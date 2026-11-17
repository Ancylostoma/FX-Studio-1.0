// Entornos compuestos: asentamientos, interiores, vista cenital, temas de
// paleta y resolución adaptativa. Todo emite operaciones PixelScene.
import { Pen, L } from './proceduralGenerator.js';
import { kitDef } from './kits.js';
import { LIB } from './spriteBase.js';
import { rgbToHsl, hslToRgb } from '../core/paletteManager.js';
import { THEMES, THEME_CONFLICTS, BIOMES } from './templates.js';

// ---------------- Temas de paleta ----------------
const toward = (h, target, deg) => { const d = ((target - h + 540) % 360) - 180; return h + Math.max(-deg, Math.min(deg, d)); };
export function gradeColor(c, th) {
  if (!th || (!th.amt && !th.sat && !th.lit)) return c;
  let [h, s, l] = rgbToHsl(c);
  if (s > 4) {
    const k = l < 45 ? (45 - l) / 45 : 0, j = l > 55 ? (l - 55) / 45 : 0;
    if (th.shadowHue != null) h = toward(h, th.shadowHue, (th.amt || 0) * k);
    if (th.lightHue != null) h = toward(h, th.lightHue, (th.amt || 0) * j * 0.7);
  }
  s = Math.min(100, s * (th.sat || 1));
  l = Math.max(0, Math.min(100, l * (th.lit || 1)));
  return hslToRgb(h, s, l);
}
export function applyTheme(scene, themeKey) {
  const th = THEMES[themeKey];
  if (!th || themeKey === 'natural') return scene;
  return { ...scene, pal: scene.pal.map((c) => gradeColor(c, th)) };
}

// Tema por defecto según la escena (modo procedural) y comprobación de coherencia
// del tema que proponga la IA.
export function inferTheme(spec) {
  const t = spec.themes || [];
  if (spec.style === 'bit8') return 'retro';
  if (spec.biome === 'cyber') return 'cyberpunk';
  if (t.includes('postapoc')) return 'postapoc';
  if (t.includes('japan')) return 'japanese';
  if (t.includes('tropical') || spec.town?.style === 'tropical') return 'tropical';
  if (t.includes('dark') || t.includes('spooky')) return 'dark';
  if (spec.biome === 'snow' || spec.weather?.includes('snow')) return 'winter';
  if (spec.biome === 'desert' || spec.biome === 'mars') return 'desert';
  if (spec.biome === 'space' || spec.biome === 'moonland') return 'space';
  if (t.includes('magic')) return 'fantasy';
  const ids = [...(spec.subjects || []), ...(spec.structures || [])].map((e) => e.id);
  if (t.includes('medieval') || spec.town?.style === 'medieval' || ids.some((i) => ['castle', 'knight', 'king', 'princess', 'cathedral', 'medievalhouse', 'wall'].includes(i))) return 'medieval';
  if (ids.some((i) => ['dragon', 'wizard', 'fairy', 'unicorn', 'griffin', 'elf', 'witch'].includes(i))) return 'fantasy';
  return 'natural';
}
export function checkTheme(theme, spec) {
  if (!THEMES[theme]) return { theme: inferTheme(spec), note: null };
  const clash = (THEME_CONFLICTS[theme] || []).includes(spec.biome);
  if (!clash) return { theme, note: null };
  const fixed = inferTheme(spec);
  return { theme: fixed, note: `La paleta «${THEMES[theme].name}» no encaja con ${BIOMES[spec.biome]?.name || spec.biome}; uso «${THEMES[fixed].name}».` };
}

// ---------------- Resolución adaptativa ----------------
// Más elementos y relaciones → más píxeles para que los detalles quepan.
export function autoResolution(spec) {
  const n = (spec.subjects || []).reduce((a, e) => a + (e.n || 1), 0) + (spec.structures || []).length * 1.5 + (spec.flora || []).length * 0.5;
  if (!spec.biome) return n > 2 ? 64 : 48;
  let score = n + (spec.town ? 3 : 0) + (spec.setting === 'interior' ? 3 : 0) + (spec.battle ? 2 : 0) + (spec.features || []).length * 0.5
    + [...(spec.subjects || []), ...(spec.structures || []), ...(spec.flora || [])].filter((e) => e.rel || e.side || e.depth).length * 0.5;
  if (score <= 1.5) return 96;
  if (score <= 4) return 128;
  return 160;
}

// ---------------- Asentamientos ----------------
const TOWN_STYLES = {
  medieval: (r) => r.pick([
    { base: 'building', mat: 'timber', roof: 'gable', floors: r.int(2, 3), width: r.pick(['narrow', 'normal']), chimney: r.chance(0.5), color: r.pick(['roof', 'brown', 'roof']) },
    { base: 'building', mat: 'stone', roof: 'gable', floors: r.int(1, 2), width: 'normal', color: r.pick(['roof', 'darkstone']) },
    { base: 'building', mat: 'timber', roof: 'gable', floors: r.int(1, 2), width: 'wide', color: 'brown' },
  ]),
  tropical: (r) => ({ base: 'building', mat: 'wood', roof: 'thatch', floors: 1, width: r.pick(['narrow', 'normal']), stilts: r.chance(0.6) }),
  japanese: (r) => r.pick([{ base: 'building', mat: 'wood', roof: 'pagoda', floors: r.int(1, 2), width: 'normal', color: 'darkstone' }, { base: 'building', mat: 'white', roof: 'gable', floors: 1, width: 'normal', color: 'darkstone' }]),
  desert: (r) => r.pick([{ base: 'building', mat: 'sand', roof: 'flat', floors: r.int(1, 3), width: r.pick(['narrow', 'normal']) }, { base: 'building', mat: 'sand', roof: 'dome', floors: 1, width: 'normal', color: 'white' }]),
  nordic: (r) => ({ base: 'building', mat: 'wood', roof: 'gable', floors: r.int(1, 2), width: 'normal', color: 'darkstone', chimney: true }),
};
const LANDMARK = {
  medieval: { base: 'building', mat: 'stone', roof: 'spire', floors: 3, width: 'narrow', arches: 1 },
  japanese: { base: 'building', mat: 'wood', roof: 'pagoda', floors: 3, width: 'narrow', color: 'darkstone' },
  desert: { base: 'building', mat: 'white', roof: 'dome', floors: 2, width: 'normal', towers: 2, color: 'gold' },
};

// Dos hileras de edificios (fondo y plano medio) con alturas variadas; deja
// libres las zonas ocupadas por las estructuras principales.
// phase 'far': detrás del suelo (tierra adentro); 'shore': sobre la orilla, delante del agua.
export function genTown(G, phase = 'far') {
  if (!G.spec.town || G.layoutKind !== 'scene') return [];
  const shore = !!G.waterKind && G.waterKind !== 'sea';
  if ((phase === 'shore') !== shore) return [];
  const { w, h, H, C } = G, rng = G.rng('town'), style = TOWN_STYLES[G.spec.town.style] ? G.spec.town.style : 'medieval';
  const out = [];
  const busy = G.structs.map((b) => [b.x - 2, b.x + b.w + 2]);
  const free = (x0, x1) => !busy.some(([a, b]) => x1 > a && x0 < b);
  const rows = shore
    ? [{ name: 'aldea', base: G.shoreY + Math.round((h - G.shoreY) * 0.42), hMin: 0.2, hMax: 0.28, fog: 0 }]
    : [{ name: 'ciudad lejana', base: H + 1, hMin: 0.1, hMax: 0.2, fog: 40 }, { name: 'ciudad', base: H + Math.round((h - H) * 0.12), hMin: 0.16, hMax: 0.27, fog: 0 }];
  const size = Math.max(1, Math.min(3, G.spec.town.size || 2));
  rows.forEach((row, ri) => {
    const o = [], lit = [], glow = [];
    let x = -rng.int(0, Math.round(w * 0.05));
    let landmark = ri === 0 && !shore && LANDMARK[style] && size >= 2 ? rng.int(1, 3) : -1, i = 0;
    while (x < w) {
      const isLm = i === landmark;
      const kit = isLm ? LANDMARK[style] : TOWN_STYLES[style](rng);
      const def = kitDef(kit);
      const bh = Math.round(h * (isLm ? row.hMax * 1.35 : rng.range(row.hMin, row.hMax)) * (0.8 + size * 0.1));
      const bw = Math.max(5, Math.round(bh * def.ar));
      if ((ri === 0 && !shore) || free(x, x + bw) || rng.chance(0.15)) {
        const p = new Pen(o, x, row.base - bh, bw, bh, rng.chance(0.5));
        def.build(p, { C, a: {}, lit: p.on(lit), glow, night: G.lights, rng: G.rng('house' + ri + i), sprites: G.sprites, pose: null });
        if (style === 'tropical' && (shore || ri === 1) && rng.chance(0.55)) {
          const pd = LIB.palm, ph = Math.round(bh * 1.25), pw = Math.round(ph * pd.ar), pp = new Pen(o, x + bw - Math.round(pw * 0.3), row.base - ph, pw, ph);
          pd.build(pp, { C, a: {}, lit: pp.on(lit), glow, night: G.lights, rng: G.rng('palm' + i), sprites: G.sprites });
          x += Math.round(pw * 0.5);
        }
      }
      x += bw + (ri === 0 && !shore ? rng.int(-2, 1) : rng.int(Math.max(1, Math.round(w * 0.02)), Math.max(2, Math.round(w * 0.07))));
      i++;
    }
    const props = { ...(row.fog ? { f: [G.skyHorizon, row.fog] } : {}), ...(G.style.sh ? { sh: 1 } : {}) };
    out.push(L(row.name, 'town', o, props));
    if (lit.length) out.push(L('luces de ' + row.name, 'town', lit, { nl: 1, ...(row.fog ? { a: 0.7 } : {}) }));
    if (glow.length) out.push(L('halos de ' + row.name, 'town', glow, { nl: 1, a: 0.18 }));
  });
  return out;
}

// ---------------- Interiores ----------------
// wall: material de la pared; floor: suelo; back: muebles contra la pared;
// floorItems: muebles sobre el suelo. Las posiciones son fracciones del ancho.
const ROOMS = {
  library: { name: 'biblioteca', wall: 'bark', floor: 'wood', window: 'arch', windows: 1, back: [['bookshelf', 0.1], ['bookshelf', 0.27], ['bookshelf', 0.73], ['bookshelf', 0.9]], floorItems: [['rug', 0.5, 0.55], ['table', 0.5, 0.42], ['globe', 0.8, 0.3], ['chair', 0.33, 0.45]], magic: [['magic', 0.25, -0.2], ['candle', 0.62, -0.3], ['candle', 0.4, -0.38]] },
  tavern: { name: 'taberna', wall: 'wood', floor: 'wood', window: 'square', back: [['fireplace', 0.5], ['banner', 0.18, 'wall'], ['barrel', 0.85], ['barrel', 0.92]], floorItems: [['table', 0.25, 0.4], ['chair', 0.13, 0.42], ['table', 0.75, 0.55], ['chair', 0.88, 0.57], ['barrel', 0.06, 0.2]] },
  dungeon: { name: 'mazmorra', wall: 'darkstone', floor: 'stone', window: 'bars', back: [['torch', 0.2, 'wall'], ['torch', 0.8, 'wall'], ['barrel', 0.9]], floorItems: [['barrel', 0.1, 0.3]] },
  throne: { name: 'sala del trono', wall: 'stone', floor: 'stone', window: 'arch', pillars: 1, back: [['throne', 0.5], ['banner', 0.3, 'wall'], ['banner', 0.7, 'wall'], ['torch', 0.12, 'wall'], ['torch', 0.88, 'wall']], floorItems: [['rug', 0.5, 0.5]] },
  lab: { name: 'laboratorio', wall: 'stone', floor: 'stone', window: 'arch', back: [['bookshelf', 0.15], ['bookshelf', 0.85]], floorItems: [['cauldron', 0.5, 0.4], ['table', 0.78, 0.5], ['candle', 0.2, 0.3], ['globe', 0.3, 0.2]] },
  room: { name: 'habitación', wall: 'cream', floor: 'wood', window: 'square', back: [['bookshelf', 0.85]], floorItems: [['rug', 0.45, 0.55], ['table', 0.25, 0.4], ['chair', 0.12, 0.42], ['candle', 0.27, -0.1]] },
};
export const ROOM_KINDS = Object.keys(ROOMS);

export function genInterior(G) {
  const { w, h, C, spec } = G, rng = G.rng('interior');
  const room = ROOMS[spec.room] || ROOMS.room;
  const wb = G.H;                                   // línea entre pared y suelo
  const wall = C.mat(room.wall), floor = C.mat(room.floor), dark = C.dark;
  const o = [], win = [], light = [], back = [], backLit = [], floorO = [], floorLit = [], glow = [];
  // Pared
  o.push(['R', 0, 0, w, wb, wall[2]], ['G', 0, 0, w, wb, [wall[1], wall[2], wall[2]]]);
  if (room.wall === 'bark' || room.wall === 'wood') { const pw = Math.max(4, Math.round(w / 10)); for (let x = pw; x < w; x += pw) o.push(['R', x, 0, 1, wb, wall[1]]); o.push(['R', 0, Math.round(wb * 0.08), w, Math.max(1, Math.round(h / 64)), wall[3]]); }
  else o.push(['T', 0, 0, w, wb, wall[1], 8, rng.int(1, 9999), 1], ['T', 0, 0, w, wb, wall[3], 4, rng.int(1, 9999), 1]);
  o.push(['R', 0, wb - Math.max(2, Math.round(h / 48)), w, Math.max(2, Math.round(h / 48)), wall[0]]);
  // Vigas del techo
  const beam = C.mat('bark'); o.push(['R', 0, 0, w, Math.max(2, Math.round(h / 40)), beam[1]]);
  // Columnas
  if (room.pillars) for (const u of [0.06, 0.94]) { const pw2 = Math.round(w * 0.07), x = Math.round(u * w - pw2 / 2); o.push(['R', x, 0, pw2, wb, wall[3]], ['R', x + pw2 - 2, 0, 2, wb, wall[1]], ['R', x - 1, wb - 3, pw2 + 2, 3, wall[1]]); }
  // Suelo con perspectiva: líneas más separadas cuanto más cerca
  o.push(['R', 0, wb, w, h - wb, floor[2]], ['G', 0, wb, w, h - wb, [floor[1], floor[2], floor[3]]]);
  for (let i = 1, y = wb + 2; y < h; i++, y += 2 + i * 2) o.push(['R', 0, y, w, 1, floor[1]]);
  if (room.floor === 'stone') for (let i = 0, y = wb + 2; y < h; i++, y += 3 + i) { const off = (i % 2) * Math.round(w / 12); for (let x = off; x < w; x += Math.round(w / 6)) o.push(['R', x, y, 1, 2 + i, floor[1]]); }
  // Ventanas
  const nWin = room.windows || (room.window === 'bars' ? 1 : 2), skyC = G.night ? C.hsl(225, 45, 22) : C.hsl(205, 60, 70);
  for (let i = 0; i < nWin; i++) {
    const cx = Math.round(w * (nWin === 1 ? 0.5 : i ? 0.62 : 0.38)), ww = Math.max(4, Math.round(w * (room.window === 'bars' ? 0.12 : nWin === 1 ? 0.18 : 0.1))), wy = Math.round(wb * 0.2), wh = Math.round(wb * (room.window === 'bars' ? 0.18 : nWin === 1 ? 0.5 : 0.42));
    win.push(['R', cx - (ww >> 1) - 1, wy - 1, ww + 2, wh + 2, wall[0]], ['R', cx - (ww >> 1), wy, ww, wh, skyC[2]]);
    if (room.window === 'arch') win.push(['E', cx, wy, ww >> 1, Math.round(ww * 0.6), skyC[2]], ['E', cx, wy, (ww >> 1) + 1, Math.round(ww * 0.6) + 1, wall[0], 2]);
    if (G.night) win.push(['R', cx + 1, wy + 2, 1, 1, C.rgb([240, 240, 255])]); else win.push(['R', cx - (ww >> 1), wy + Math.round(wh * 0.7), ww, Math.round(wh * 0.3), skyC[3]]);
    win.push(['R', cx, wy - (room.window === 'arch' ? Math.round(ww * 0.6) : 0), 1, wh + Math.round(ww * 0.6), wall[0]], ['R', cx - (ww >> 1), wy + (wh >> 1), ww, 1, wall[0]]);
    if (room.window === 'bars') for (let x = cx - (ww >> 1) + 1; x < cx + (ww >> 1); x += 2) win.push(['R', x, wy, 1, wh, C.mat('metal')[1]]);
    // Haz de luz sobre el suelo
    const lc = G.night ? C.rgb([170, 190, 255]) : C.rgb([255, 240, 190]), d = G.spec.light === 'right' ? -1 : 1;
    light.push(['P', [cx - (ww >> 1), wy + wh, cx + (ww >> 1), wy + wh, cx + (ww >> 1) + d * Math.round(w * 0.18), h, cx - (ww >> 1) + d * Math.round(w * 0.1), h], lc]);
  }
  const putItem = (id, u, dz, list, litL) => {
    const def = LIB[id]; if (!def) return;
    const onWall = dz === 'wall';
    const depth = onWall ? 0 : typeof dz === 'number' ? dz : 0;
    const scale = onWall ? 0.9 : dz === undefined ? 1 : 0.85 + Math.max(-0.4, depth) * 0.5;
    const bh = Math.max(3, Math.round(h * def.sz * 1.15 * scale)), bw = Math.max(3, Math.round(bh * def.ar));
    const by = onWall ? Math.round(wb * 0.75) : dz === undefined ? wb + 1 : depth < 0 ? Math.round(wb + (h - wb) * 0.45 + depth * h * 0.3) : Math.round(wb + (h - wb) * (0.15 + depth * 0.8));
    const x = Math.round(u * w - bw / 2) + rng.int(-1, 1);
    const p = new Pen(list, x, by - bh, bw, bh, rng.chance(0.3));
    def.build(p, { C, a: {}, lit: p.on(litL), glow, night: true, rng: G.rng('it' + id + u), sprites: G.sprites, interior: true, pose: G.pose });
  };
  for (const [id, u, z] of room.back) putItem(id, u, z, back, backLit);
  for (const [id, u, z] of room.floorItems) putItem(id, u, z, floorO, floorLit);
  if (G.spec.themes?.includes('magic') || G.style.magic || spec.room === 'library' && G.spec.fx?.includes('sparkles')) for (const [id, u, z] of room.magic || []) putItem(id, u, z, floorO, floorLit);
  return [
    L('pared', 'interior', o),
    L('ventanas', 'interior', win, { nl: 1 }),
    L('luz de ventanas', 'interior', light, { nl: 1, a: G.night ? 0.12 : 0.18 }),
    L('mobiliario de fondo', 'interior', back, G.style.sh ? { sh: 1 } : {}),
    L('luces de mobiliario', 'interior', backLit, { nl: 1 }),
    L('halos', 'interior', glow, { nl: 1, a: 0.16 }),
    L('mobiliario', 'interior', floorO, { ...(G.style.sh ? { sh: 1 } : {}), ...(G.style.ol === 'dark' ? { ol: dark } : {}) }),
    L('luces del mobiliario', 'interior', floorLit, { nl: 1 }),
  ];
}
