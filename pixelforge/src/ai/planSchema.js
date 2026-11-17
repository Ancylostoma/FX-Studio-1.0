// Plan de dirección artística: lo que produce el modelo de lenguaje local
// (Ollama). El modelo NO dibuja: decide qué hay en la escena, dónde, con qué
// jerarquía, paleta, luz y vista. Para lo que no está en el catálogo describe
// el objeto con un kit paramétrico. El motor procedural compone y dibuja.
//
// El esquema es deliberadamente compacto (solo "id" es obligatorio) porque
// cada token generado cuesta tiempo en un ordenador sin GPU.
import { LIB } from '../generator/spriteLibrary.js';
import { BIOMES, COLORS, MATERIALS, THEMES } from '../generator/templates.js';
import { emptyRaw, REL_TYPES } from '../generator/promptParser.js';
import { KIT_BASES, KIT_PARAMS, sanitizeKit } from '../generator/kits.js';
import { ROOM_KINDS } from '../generator/environments.js';

const idsOf = (...cats) => Object.values(LIB).filter((d) => cats.includes(d.cat)).map((d) => d.id);
export const PLAN_CATALOG = Object.freeze({
  subjects: idsOf('humanoid', 'animal', 'creature', 'vehicle', 'object', 'effect'),
  structures: idsOf('structure'),
  props: idsOf('flora'),
  biomes: Object.keys(BIOMES).filter((b) => b !== 'interior'),
  colors: Object.keys(COLORS),
  materials: [...new Set([...Object.keys(MATERIALS), ...Object.keys(COLORS)])],
  features: ['mountains', 'hills', 'lake', 'fuji', 'waterfall'],
  times: ['day', 'night', 'sunset', 'dawn'],
  weather: ['rain', 'storm', 'snow', 'fog'],
  sky: ['moon', 'sun', 'stars', 'clouds', 'aurora', 'rainbow', 'comet', 'planet'],
  effects: ['fireflies', 'sparkles'],
  actions: ['idle', 'walk', 'attack', 'cast'],
  palettes: Object.keys(THEMES),
  views: ['side', 'front', 'iso', 'panorama', 'top'],
  rooms: ROOM_KINDS,
  towns: ['medieval', 'tropical', 'japanese', 'desert', 'nordic'],
  relations: REL_TYPES,
});
const P = PLAN_CATALOG;

const en = (list) => ({ type: 'string', enum: list });
const arr = (items) => ({ type: 'array', items });
const obj = (properties, required = []) => ({ type: 'object', properties, required, additionalProperties: false });
const num = { type: 'number' };
const bool = { type: 'boolean' };

// Kit: unión de los parámetros de todas las bases (todos opcionales).
const kitProps = { base: en(KIT_BASES), name: { type: 'string' } };
for (const spec of Object.values(KIT_PARAMS)) for (const [k, rule] of Object.entries(spec)) {
  if (kitProps[k]) continue;
  if (rule === 'mat') kitProps[k] = en(P.materials);
  else if (rule === 'bool') kitProps[k] = bool;
  else if (Array.isArray(rule)) kitProps[k] = en(rule);
  else if (rule.startsWith('num:')) kitProps[k] = num;
  else if (rule === 'shapes') kitProps[k] = arr(obj({ s: en(['ellipse', 'rect', 'tri']), x: num, y: num, w: num, h: num, mat: en(P.materials) }, ['s', 'x', 'y', 'w', 'h', 'mat']));
}
const KIT = obj(kitProps, ['base']);
const place = { side: en(['left', 'center', 'right']), depth: en(['back', 'front']), relation: obj({ type: en(P.relations), to: { type: 'string' } }, ['type', 'to']) };
const entity = (ids, extra = {}) => obj({ id: en([...ids, 'custom']), name: { type: 'string' }, kit: KIT, color: en(P.colors), color2: en(P.colors), n: { type: 'integer' }, size: { type: 'integer', enum: [-1, 0, 1] }, ...place, ...extra }, ['id']);

export const PLAN_SCHEMA = obj({
  subjects: arr(entity(P.subjects, { fly: bool, action: en(P.actions), facing: en(['left', 'right']), armor: en(P.colors), weapon: en(['sword', 'staff', 'spear', 'axe', 'club', 'hammer', 'bow', 'wand']), weaponFx: en(['fire', 'ice']), shield: bool, cape: bool, wings: en(['fairy', 'angel', 'bat']), breathesFire: bool })),
  structures: arr(entity(P.structures)),
  props: arr(entity(P.props)),
  setting: en(['outdoor', 'interior']),
  room: en(P.rooms),
  biome: en(P.biomes),
  town: obj({ style: en(P.towns), size: { type: 'integer' } }, ['style']),
  features: arr(en(P.features)),
  time: en(P.times),
  weather: arr(en(P.weather)),
  sky: arr(en(P.sky)),
  effects: arr(en(P.effects)),
  skyColor: en(P.colors),
  palette: en(P.palettes),
  view: en(P.views),
  light: en(['left', 'right']),
  focus: { type: 'string' },
  battle: bool,
  planets: { type: 'integer' },
  exclude: arr(en([...P.sky, ...P.weather, ...P.effects])),
  unsupported: arr({ type: 'string' }),
}, ['subjects']);

// ---------------- Instrucciones ----------------
const names = (ids) => ids.map((id) => `${id}=${LIB[id].name}`).join(', ');
// prefs: 'strict' (solo lo pedido), 'balanced', 'creative' (enriquece la escena).
const PREFS = {
  strict: 'Incluye solo lo que la descripción pide o implica claramente; no añadas adornos.',
  balanced: 'Incluye lo pedido y, si ayuda a la composición, uno o dos elementos ambientales coherentes.',
  creative: 'Puedes enriquecer la escena con elementos ambientales, efectos y una paleta expresiva, sin contradecir lo pedido.',
};
export function localSystemPrompt(prefs = 'balanced') {
  return `Eres el director artístico de PixelForge, un motor de pixel art. Lees una descripción y diseñas la escena como un plan JSON. No dibujas: decides qué aparece, dónde y cómo. Responde solo con el JSON.

Catálogo (usa estos id):
- subjects: ${names(P.subjects)}
- structures: ${names(P.structures)}
- props: ${names(P.props)}
- biome: ${P.biomes.join(', ')} · room (si setting=interior): ${P.rooms.join(', ')} · town.style: ${P.towns.join(', ')}
- features: ${P.features.join(', ')} · sky: ${P.sky.join(', ')} · weather: ${P.weather.join(', ')} · effects: ${P.effects.join(', ')}
- palette: ${P.palettes.join(', ')} · view: ${P.views.join(', ')} · colores: ${P.colors.join(', ')}

Reglas:
1. Si algo no está en el catálogo, NO lo cambies por otra cosa parecida: créalo con id "custom", un "name" y un "kit". Bases de kit: humanoid (body, skin, hair, head, robe, wings, horns, halo, tail, glow, weapon, armor, bulky, thin), quadruped (col, stripes, spots, spotsMany, neck 0-2, leg, ears, tail, mane, trunk, tusks, horns, antlers, slim, fat), building (mat, roof, floors 1-6, width, towers 0-2, rose, columns, stilts, arches), flyer (col, wingCol, insect). Ejemplo: un hada es {"id":"custom","name":"hada","kit":{"base":"humanoid","body":"pink","robe":true,"wings":"fairy","glow":true},"size":-1,"fly":true}.
2. Jerarquía: pon "focus" con el id (o name) del protagonista. Usa depth "back" para lo lejano y "front" para el primer plano; side left/center/right; relation {type: on|behind|front|beside|leftof|rightof, to: id o name de otro elemento}.
3. Acción: action attack en combates (y battle true), walk si camina, cast si lanza un hechizo. facing indica hacia dónde mira.
4. Interiores: setting "interior" y room. Pueblos y ciudades: town {style, size 1-3}. "Ciudad" moderna o futurista: biome city o cyber.
5. Elige "palette" según el ambiente y "time" según la hora. No repitas lo que el motor ya añade solo (luna y estrellas de noche, nubes de día).
6. En "unsupported" anota en español lo que no se pudo representar.
${PREFS[prefs] || PREFS.balanced}`;
}

// Ejemplo de ida y vuelta para modelos pequeños (few-shot).
const EXAMPLE_IN = 'Un dragón negro volando sobre una ciudad medieval, con una luna roja detrás del castillo y un hada mirándolo desde un árbol';
const EXAMPLE_OUT = JSON.stringify({
  subjects: [{ id: 'dragon', color: 'black', fly: true, breathesFire: true, size: 1, relation: { type: 'on', to: 'castle' } }, { id: 'custom', name: 'hada', kit: { base: 'humanoid', body: 'pink', robe: true, wings: 'fairy', glow: true }, size: -1, fly: true, relation: { type: 'beside', to: 'tree' }, facing: 'right' }],
  structures: [{ id: 'castle', depth: 'back' }], props: [{ id: 'tree', side: 'left' }],
  biome: 'town', town: { style: 'medieval', size: 3 }, time: 'night', sky: ['moon'], skyColor: 'red', palette: 'dark', focus: 'dragon', light: 'right', unsupported: [],
});
export function buildLocalMessages({ prompt, raw, instruction, prefs }) {
  const msgs = [{ role: 'system', content: localSystemPrompt(prefs) }, { role: 'user', content: EXAMPLE_IN }, { role: 'assistant', content: EXAMPLE_OUT }];
  if (raw) msgs.push({ role: 'user', content: `Plan actual:\n${JSON.stringify(rawToPlan(raw))}\nAplica este cambio y devuelve el plan completo, sin tocar lo que no se menciona: ${instruction}` });
  else msgs.push({ role: 'user', content: prompt });
  return msgs;
}

// ---------------- Saneado: plan del modelo → raw del motor ----------------
const pick = (v, list) => (list.includes(v) ? v : null);
const uniq = (v, list) => [...new Set((Array.isArray(v) ? v : []).filter((x) => list.includes(x)))];
const int = (v, a, b, d) => (Number.isFinite(+v) ? Math.max(a, Math.min(b, Math.round(+v))) : d);
const slug = (s) => String(s || 'objeto').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '').slice(0, 20) || 'objeto';
const CAT_OF_BASE = { building: 'structures', humanoid: 'subjects', quadruped: 'subjects', flyer: 'subjects', shapes: 'subjects' };

export function sanitizePlan(o) {
  if (!o || typeof o !== 'object' || Array.isArray(o)) return null;
  const raw = emptyRaw(), warnings = [];
  const lists = { subjects: [], structures: [], flora: [] };
  const byName = new Map();
  const take = (e, ids, target) => {
    if (!e || typeof e !== 'object') return;
    let id = e.id, kit = null, name = null;
    if (id === 'custom' || (!ids.includes(id) && e.kit)) {
      kit = sanitizeKit(e.kit);
      if (!kit) { warnings.push(`Objeto a medida sin kit válido: ${e.name || e.id}`); return; }
      name = String(e.name || kit.name || kit.base).slice(0, 40); kit.name = name;
      id = 'c:' + slug(name);
      target = CAT_OF_BASE[kit.base] || target;
    } else if (!ids.includes(id)) {
      const where = ['subjects', 'structures', 'flora'].find((l) => (l === 'subjects' ? P.subjects : l === 'structures' ? P.structures : P.props).includes(id));
      if (!where) { if (id) warnings.push(`Elemento desconocido descartado: ${String(id).slice(0, 30)}`); return; }
      target = where;
    }
    const x = { id, color: pick(e.color, P.colors), color2: pick(e.color2, P.colors), n: int(e.n, 1, target === 'flora' ? 6 : 5, 1), size: int(e.size, -1, 1, 0), fly: typeof e.fly === 'boolean' ? e.fly : null,
      side: pick(e.side, ['left', 'center', 'right']), depth: pick(e.depth, ['back', 'front']), rel: null, action: pick(e.action, P.actions), facing: pick(e.facing, ['left', 'right']), a: {} };
    if (kit) { x.kit = kit; x.name = name; }
    if (e.relation && typeof e.relation === 'object' && pick(e.relation.type, P.relations) && typeof e.relation.to === 'string') x._to = { type: e.relation.type, to: e.relation.to };
    const human = (x.kit ? x.kit.base === 'humanoid' : LIB[id]?.cat === 'humanoid');
    if (human) {
      if (pick(e.armor, P.colors)) x.a.armor = e.armor;
      if (['sword', 'staff', 'spear', 'axe', 'club', 'hammer', 'bow', 'wand'].includes(e.weapon)) x.a.weapon = e.weapon;
      if (e.shield === true) x.a.shield = true;
      if (e.cape === true) x.a.cape = true;
      if (['fairy', 'angel', 'bat'].includes(e.wings)) x.a.wings = e.wings;
    }
    if (pick(e.weaponFx, ['fire', 'ice']) && (x.a.weapon || id === 'sword')) x.a.weaponFx = e.weaponFx;
    if (id === 'dragon' && e.breathesFire === true) x.a.fire = true;
    lists[target].push(x);
    if (name) byName.set(slug(name), id);
    byName.set(slug(id), id);
  };
  for (const e of (Array.isArray(o.subjects) ? o.subjects : []).slice(0, 8)) take(e, P.subjects, 'subjects');
  for (const e of (Array.isArray(o.structures) ? o.structures : []).slice(0, 4)) take(e, P.structures, 'structures');
  for (const e of (Array.isArray(o.props) ? o.props : []).slice(0, 8)) take(e, P.props, 'flora');
  Object.assign(raw, lists);
  // Relaciones: el destino debe existir en la escena (por id o por nombre).
  const all = [...raw.subjects, ...raw.structures, ...raw.flora];
  for (const x of all) {
    if (x._to) { const of = byName.get(slug(x._to.to)); if (of && of !== x.id) x.rel = { type: x._to.type, of }; else warnings.push(`Relación con un elemento inexistente: ${x._to.to}`); }
    delete x._to;
  }
  raw.setting = o.setting === 'interior' ? 'interior' : null;
  if (raw.setting) raw.room = pick(o.room, P.rooms) || 'room';
  raw.biome = pick(o.biome, P.biomes);
  if (o.town && typeof o.town === 'object' && pick(o.town.style, P.towns)) raw.town = { kind: 'village', style: o.town.style, size: int(o.town.size, 1, 3, 2) };
  raw.features = uniq(o.features, P.features);
  raw.time = pick(o.time, P.times);
  raw.weather = uniq(o.weather, P.weather);
  raw.celestial = uniq(o.sky, P.sky);
  raw.fx = uniq(o.effects, P.effects);
  if (pick(o.skyColor, P.colors)) raw.sky.color = o.skyColor;
  raw.theme = pick(o.palette, P.palettes);
  raw.view = pick(o.view, P.views);
  raw.light = pick(o.light, ['left', 'right']);
  raw.battle = o.battle === true;
  raw.planets = int(o.planets, 0, 5, 0);
  if (raw.planets && !raw.celestial.includes('planet')) raw.celestial.push('planet');
  if (typeof o.focus === 'string') raw.focus = byName.get(slug(o.focus)) || null;
  raw.no = uniq(o.exclude, [...P.sky, ...P.weather, ...P.effects]);
  raw.celestial = raw.celestial.filter((c) => !raw.no.includes(c));
  raw.unknown = (Array.isArray(o.unsupported) ? o.unsupported : []).filter((s) => typeof s === 'string' && s.trim()).slice(0, 8).map((s) => s.trim().slice(0, 80));
  raw.interpreted = 'ai';
  return { raw, warnings };
}

// Inversa aproximada: el plan que corresponde a un raw (para pedir ediciones).
export function rawToPlan(raw) {
  const rel = (x) => (x.rel ? { relation: { type: x.rel.type, to: x.rel.of } } : {});
  const e = (x) => ({ id: x.kit ? 'custom' : x.id, ...(x.kit ? { name: x.name || x.kit.name, kit: x.kit } : {}), ...(x.color ? { color: x.color } : {}), ...(x.n > 1 ? { n: x.n } : {}), ...(x.size ? { size: x.size } : {}), ...(x.side ? { side: x.side } : {}), ...(x.depth ? { depth: x.depth } : {}), ...(x.action ? { action: x.action } : {}), ...rel(x) });
  return {
    subjects: (raw.subjects || []).map((x) => ({ ...e(x), ...(x.fly != null ? { fly: x.fly } : {}), ...(x.a?.armor ? { armor: x.a.armor } : {}), ...(x.a?.weapon ? { weapon: x.a.weapon } : {}), ...(x.a?.fire ? { breathesFire: true } : {}) })),
    structures: (raw.structures || []).map(e), props: (raw.flora || []).map(e),
    ...(raw.setting === 'interior' ? { setting: 'interior', room: raw.room } : {}), ...(raw.biome && raw.biome !== 'interior' ? { biome: raw.biome } : {}),
    ...(raw.town ? { town: { style: raw.town.style || 'medieval', size: raw.town.size || 2 } } : {}),
    features: raw.features || [], ...(raw.time ? { time: raw.time } : {}), weather: raw.weather || [], sky: raw.celestial || [], effects: raw.fx || [],
    ...(raw.sky?.color ? { skyColor: raw.sky.color } : {}), ...(raw.theme ? { palette: raw.theme } : {}), ...(raw.view ? { view: raw.view } : {}),
    ...(raw.light ? { light: raw.light } : {}), ...(raw.focus ? { focus: raw.focus } : {}), ...(raw.battle ? { battle: true } : {}), ...(raw.planets ? { planets: raw.planets } : {}),
  };
}

// Fusión del modo híbrido: lo que el parser reconoció con certeza (palabras del
// catálogo) y el modelo omitió se añade al plan. Protege frente a olvidos de
// modelos pequeños sin duplicar lo que el modelo ya representó.
export function mergeWithParser(planRaw, parserRaw) {
  const out = JSON.parse(JSON.stringify(planRaw)), added = [];
  for (const l of ['subjects', 'structures', 'flora']) for (const e of parserRaw[l] || []) {
    if ([...out.subjects, ...out.structures, ...out.flora].some((x) => x.id === e.id)) continue;
    out[l].push(JSON.parse(JSON.stringify(e))); added.push(LIB[e.id]?.name || e.id);
  }
  for (const k of ['features', 'weather', 'celestial', 'fx']) for (const v of parserRaw[k] || []) if (!out[k].includes(v) && !(out.no || []).includes(v)) out[k].push(v);
  if (!out.time && parserRaw.time) out.time = parserRaw.time;
  if (!out.biome && parserRaw.biome && !out.setting) out.biome = parserRaw.biome;
  return { raw: out, added };
}
