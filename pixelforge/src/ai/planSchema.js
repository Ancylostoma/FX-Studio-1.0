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
  features: ['mountains', 'hills', 'lake', 'river', 'fuji', 'waterfall'],
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
  balanced: 'Incluye todo lo pedido. Como mucho puedes añadir dos detalles pequeños de ambiente (flores, rocas, arbustos); nunca personajes, edificios ni pueblos que no se piden.',
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
6. Clima y cielo NO son objetos: lluvia, nieve, niebla y tormenta van en "weather"; montañas, ríos (river), lagos (lake) y cascadas en "features"; luna, sol, estrellas, aurora, arcoíris y planetas en "sky". Nunca los crees como "custom".
7. "town" solo si la descripción habla de un pueblo, aldea o ciudad. No añadas edificios que no se piden.
8. "montado en" / "a lomos de" un animal: relation {"type":"on","to": el animal}. "detrás de", "delante de", "junto a": behind, front, beside.
9. En "unsupported" anota en español lo que no se pudo representar.
${PREFS[prefs] || PREFS.balanced}`;
}

// Ejemplos de ida y vuelta para modelos pequeños (few-shot). Son varios y
// distintos a propósito: con uno solo, el modelo tiende a copiarlo (en la
// prueba real con qwen3.5:4b, el ejemplo único coló una "ciudad medieval" y
// una "luna roja" en escenas que no las pedían).
const EXAMPLES = [
  ['Una caballera con armadura plateada montada en un lobo gigante junto a un pozo, al atardecer',
    { subjects: [{ id: 'knight', armor: 'silver', relation: { type: 'on', to: 'wolf' }, facing: 'right' }, { id: 'wolf', size: 1, relation: { type: 'beside', to: 'well' } }], structures: [{ id: 'well', depth: 'back' }], biome: 'meadow', time: 'sunset', palette: 'medieval', focus: 'knight', unsupported: [] }],
  ['Un lago entre montañas nevadas bajo una tormenta, con pinos en la orilla',
    { subjects: [], props: [{ id: 'pine', n: 3, side: 'left' }], features: ['mountains', 'lake'], weather: ['storm'], biome: 'mountains', time: 'day', palette: 'winter', unsupported: [] }],
  ['Un pequeño espíritu de luz flotando en una cocina vieja con chimenea',
    { subjects: [{ id: 'custom', name: 'espíritu de luz', kit: { base: 'humanoid', body: 'white', robe: true, glow: true }, size: -1, fly: true }], props: [{ id: 'fireplace' }, { id: 'table', side: 'left' }], setting: 'interior', room: 'tavern', time: 'night', palette: 'fantasy', focus: 'espíritu de luz', unsupported: ['cocina → taberna'] }],
];
export function buildLocalMessages({ prompt, raw, instruction, prefs }) {
  const msgs = [{ role: 'system', content: localSystemPrompt(prefs) }];
  for (const [q, a] of EXAMPLES) msgs.push({ role: 'user', content: q }, { role: 'assistant', content: JSON.stringify(a) });
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

// ---------------- Fidelidad al prompt ----------------
// Lo que la descripción dice con certeza (palabras del vocabulario) prevalece
// sobre lo que el modelo omitió o inventó en aspectos estructurales. Corrige:
//  - clima, relieve y cielo mencionados y omitidos → se añaden;
//  - hora mencionada y cambiada → se respeta la del usuario;
//  - un pueblo o ciudad que nadie pidió → se quita (salvo modo creativo);
//  - un objeto "custom" que en realidad es cielo/clima/relieve → se convierte.
// Devuelve el plan corregido y la lista de correcciones, para mostrarlas.
export function applyCertainties(planRaw, prompt, prefs = 'balanced', parse) {
  const raw = JSON.parse(JSON.stringify(planRaw)), fixes = [];
  const p = parse(prompt).raw;
  const no = new Set(raw.no || []);
  for (const [k, label] of [['weather', 'clima'], ['features', 'relieve'], ['celestial', 'cielo']]) {
    for (const v of p[k] || []) if (!raw[k].includes(v) && !no.has(v)) { raw[k].push(v); fixes.push(`${label} «${v}» añadido (está en la descripción)`); }
  }
  // «río» no es «lago»: si la frase pide río y no lago, el lago del modelo era el río.
  if (p.features.includes('river') && !p.features.includes('lake') && raw.features.includes('lake') && !p.features.includes('waterfall')) { raw.features = raw.features.filter((f) => f !== 'lake'); fixes.push('«lago» → «río» (la descripción pide un río)'); }
  if (p.time && raw.time && p.time !== raw.time) { fixes.push(`hora «${raw.time}» → «${p.time}» (la de la descripción)`); raw.time = p.time; }
  if (!raw.time && p.time) raw.time = p.time;
  const askedTown = !!p.town || /\b(pueblo|aldea|villa|ciudad|ciudades|town|village|city)\b/i.test(prompt);
  if (raw.town && !askedTown && prefs !== 'creative') { fixes.push('pueblo o ciudad no pedido eliminado'); raw.town = null; if (raw.biome === 'town') raw.biome = p.biome || null; }
  for (const l of ['subjects', 'structures', 'flora']) {
    raw[l] = raw[l].filter((e) => {
      if (!e.kit || !e.name) return true;
      const q = parse(e.name).raw;
      const things = q.subjects.length + q.structures.length + q.flora.length;
      const atmos = [...q.celestial, ...q.weather, ...q.features];
      if (things || !atmos.length) return true;
      for (const v of q.celestial) if (!raw.celestial.includes(v)) raw.celestial.push(v);
      for (const v of q.weather) if (!raw.weather.includes(v)) raw.weather.push(v);
      for (const v of q.features) if (!raw.features.includes(v)) raw.features.push(v);
      fixes.push(`«${e.name}» no es un objeto: pasa al cielo/clima`);
      return false;
    });
  }
  // Hacia dónde mira: si la frase lo dice, manda.
  for (const pe of [...p.subjects, ...p.structures]) if (pe.facing) for (const e of raw.subjects.filter((x) => x.id === pe.id || x.id === raw.focus)) if (e.facing !== pe.facing) { e.facing = pe.facing; fixes.push(`«${e.id}» mira hacia la ${pe.facing === 'left' ? 'izquierda' : 'derecha'} (lo dice la descripción)`); }
  // Relaciones («montado en», «desde la torre») y cantidades («unos murciélagos») de la frase mandan.
  const all = [...raw.subjects, ...raw.structures, ...raw.flora];
  for (const pe of [...p.subjects, ...p.structures]) {
    const e = all.find((x) => x.id === pe.id);
    if (!e) continue;
    if (pe.rel && all.some((x) => x.id === pe.rel.of && x !== e) && (e.rel?.type !== pe.rel.type || e.rel?.of !== pe.rel.of)) { e.rel = { ...pe.rel }; fixes.push(`«${e.id}» ${pe.rel.type === 'on' ? 'encima de / montado en' : pe.rel.type} «${pe.rel.of}» (lo dice la descripción)`); }
    if (pe.n > 1 && (e.n || 1) < pe.n) { e.n = pe.n; fixes.push(`«${e.id}» ×${pe.n} (la descripción habla en plural)`); }
  }
  if (prefs !== 'creative') {
    // Objetos sueltos (espada, cofre, poción…) que la frase no pide.
    const ITEMS = new Set(['sword', 'shield', 'potion', 'heart', 'gem', 'key', 'crown', 'coin', 'apple', 'mug', 'chest', 'skull']);
    const asked = new Set(p.subjects.map((e) => e.id));
    const nItems = raw.subjects.length;
    raw.subjects = raw.subjects.filter((e) => !ITEMS.has(e.id) || asked.has(e.id) || raw.focus === e.id);
    if (raw.subjects.length < nItems) fixes.push(`${nItems - raw.subjects.length} objeto(s) suelto(s) no pedido(s) eliminado(s)`);
    // Atributos que la frase no menciona (alas, armadura, escudo, arma mágica).
    const ATTR = [['wings', /\b(alas|alad[oa]s?|wings?|hada|angel|ángel|demonio)\b/i, 'alas'], ['armor', /\b(armadura|coraza|armor|armour|blindad[oa])\b/i, 'armadura'], ['shield', /\b(escudo|shield)\b/i, 'escudo'], ['weaponFx', /\b(fuego|llamas?|ardiente|hielo|helad[oa]|fire|ice)\b/i, 'arma mágica']];
    for (const e of raw.subjects) for (const [k, re, label] of ATTR) if (e.a?.[k] && !re.test(prompt)) { delete e.a[k]; fixes.push(`${label} de «${e.id}» quitada (no se pide)`); }
    // Edificios del catálogo que ni la frase ni el intérprete procedural mencionan.
    const known = new Set([...p.subjects, ...p.structures, ...p.flora].map((e) => e.id));
    const before = raw.structures.length;
    raw.structures = raw.structures.filter((e) => e.kit || known.has(e.id) || raw.subjects.some((x) => x.rel?.of === e.id) || raw.focus === e.id);
    if (raw.structures.length < before) fixes.push(`${before - raw.structures.length} edificio(s) no pedido(s) eliminado(s)`);
  }
  // Notas copiadas de los ejemplos de las instrucciones.
  const EX_NOTES = new Set(EXAMPLES.flatMap(([, a]) => a.unsupported || []));
  raw.unknown = (raw.unknown || []).filter((n) => !EX_NOTES.has(n));
  return { raw, fixes };
}
