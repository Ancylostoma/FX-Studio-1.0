// Modo híbrido: la IA solo INTERPRETA el prompt y devuelve una especificación
// de escena (unas decenas de tokens); el motor offline la compone y dibuja.
// Este módulo define el catálogo que se le ofrece al modelo, el esquema JSON
// de su respuesta y el saneado que convierte esa respuesta en un "raw" válido.
import { LIB } from '../generator/spriteLibrary.js';
import { BIOMES, COLORS } from '../generator/templates.js';
import { emptyRaw, REL_TYPES } from '../generator/promptParser.js';

const idsOf = (...cats) => Object.values(LIB).filter((d) => cats.includes(d.cat)).map((d) => d.id);
export const CATALOG = Object.freeze({
  subjects: idsOf('humanoid', 'animal', 'creature', 'vehicle', 'object', 'effect'),
  structures: idsOf('structure'),
  flora: idsOf('flora'),
  biomes: Object.keys(BIOMES),
  colors: Object.keys(COLORS),
  features: ['mountains', 'hills', 'lake', 'fuji'],
  times: ['day', 'night', 'sunset', 'dawn'],
  weather: ['rain', 'storm', 'snow', 'fog'],
  celestial: ['moon', 'sun', 'stars', 'clouds', 'aurora', 'rainbow', 'comet', 'planet'],
  fx: ['fireflies', 'sparkles'],
  weapons: ['sword', 'staff'],
  weaponFx: ['fire', 'ice'],
  sides: ['left', 'center', 'right'],
  depths: ['back', 'front'],
  relations: REL_TYPES,
});
const C = CATALOG;

const nullable = (schema) => ({ anyOf: [schema, { type: 'null' }] });
const en = (list) => ({ type: 'string', enum: list });
const obj = (properties) => ({ type: 'object', properties, required: Object.keys(properties), additionalProperties: false });
const arr = (items) => ({ type: 'array', items });
const color = nullable(en(C.colors));
const size = { type: 'integer', enum: [-1, 0, 1] };
const count = { type: 'integer', enum: [1, 2, 3, 4, 5] };
// Colocación: lado, profundidad y relación con otro elemento de la escena.
const place = { side: nullable(en(C.sides)), depth: nullable(en(C.depths)), relation: nullable(obj({ type: en(C.relations), to: en([...C.subjects, ...C.structures, ...C.flora]) })) };

// Esquema para salidas estructuradas (todas las propiedades son obligatorias;
// lo opcional se expresa con null).
export const SPEC_SCHEMA = obj({
  subjects: arr(obj({
    id: en(C.subjects), color, color2: color, n: count, size, fly: nullable({ type: 'boolean' }),
    armor: color, weapon: nullable(en(C.weapons)), weaponFx: nullable(en(C.weaponFx)),
    shield: { type: 'boolean' }, cape: { type: 'boolean' }, wizardHat: { type: 'boolean' }, breathesFire: { type: 'boolean' }, ...place,
  })),
  structures: arr(obj({ id: en(C.structures), color, size, ...place })),
  flora: arr(obj({ id: en(C.flora), color, n: count, size, ...place })),
  biome: nullable(en(C.biomes)),
  features: arr(en(C.features)),
  time: nullable(en(C.times)),
  weather: arr(en(C.weather)),
  celestial: arr(en(C.celestial)),
  fx: arr(en(C.fx)),
  skyColor: color,
  crescentMoon: { type: 'boolean' },
  exclude: arr(en([...C.celestial, ...C.weather, ...C.fx])),
  unsupported: arr({ type: 'string' }),
});

const names = (ids) => ids.map((id) => `${id} (${LIB[id].name})`).join(', ');
export const INTERPRET_SYSTEM = `Eres el intérprete de prompts de PixelForge Studio. Un motor local de pixel art dibuja escenas a partir de un catálogo cerrado de elementos. Tu trabajo es traducir la descripción del usuario (en cualquier idioma y con cualquier redacción) a ese catálogo. No dibujas nada: solo eliges elementos.

Catálogo:
- Sujetos: ${names(C.subjects)}.
- Estructuras: ${names(C.structures)}.
- Naturaleza y atrezo: ${names(C.flora)}.
- Entornos (biome): ${C.biomes.map((b) => `${b} (${BIOMES[b].name})`).join(', ')}. Usa null cuando solo se pide un personaje u objeto suelto, sin escenario.
- Relieve (features): mountains, hills, lake (también ríos y estanques), fuji (monte nevado único).
- Hora (time): ${C.times.join(', ')}; null si no se indica.
- Clima: ${C.weather.join(', ')}. Cielo: ${C.celestial.join(', ')}. Efectos: fireflies, sparkles (destellos mágicos).
- Colores: ${C.colors.join(', ')}.

Cómo decidir:
- Cuando algo no está en el catálogo, elige el elemento más cercano que conserve la intención (un tigre → lion con color orange; una catedral → temple o castle; un hada → butterfly o elf) y, si la sustitución cambia de forma apreciable lo pedido, anótalo en "unsupported" con una frase corta en español ("tigre → león naranja"). Si no hay nada razonablemente cercano, omítelo y anótalo igual.
- Incluye solo lo que el usuario pide o lo que su descripción implica con claridad. El motor añade por su cuenta lo habitual de cada escena (luna y estrellas de noche, nubes de día, vegetación del entorno), así que no lo repitas salvo que se pida de forma expresa.
- "exclude" sirve para lo que el usuario dice expresamente que no quiere ("sin luna", "que no llueva").
- En sujetos: "n" es la cantidad (máximo 5); "size" es -1 pequeño, 0 normal, 1 grande; "fly" es true o false solo si se indica, y null para dejar el comportamiento natural. "armor", "weapon", "shield", "cape" y "wizardHat" solo tienen efecto en personajes humanoides; "breathesFire" solo en dragon. "color" es el color principal y "color2" el secundario.
- Colocación, solo cuando el usuario la indica: "side" (left, center, right), "depth" (back = al fondo, front = primer plano) y "relation" {type, to}, donde "to" es el id de otro elemento presente en la escena y "type" es on (encima de, montado en o sobrevolando), behind (detrás de), front (delante de), beside (junto a), leftof o rightof. "A debajo de B" se expresa como B con relation on → A. Si no se indica posición, deja los tres campos en null: el motor compone por su cuenta.`;

export function buildInterpretMessage({ prompt, raw, instruction }) {
  if (raw) return `Especificación actual de la escena:\n${JSON.stringify(rawToSpecJSON(raw))}\n\nAplica esta instrucción del usuario y devuelve la especificación completa resultante, sin tocar lo que la instrucción no menciona: ${instruction}`;
  return `Descripción del usuario: ${prompt}`;
}

const pick = (v, list) => (list.includes(v) ? v : null);
const uniq = (v, list) => [...new Set((Array.isArray(v) ? v : []).filter((x) => list.includes(x)))];
const int = (v, a, b, d) => (Number.isInteger(v) ? Math.max(a, Math.min(b, v)) : d);

// Convierte la respuesta del modelo en un "raw" del parser. No se confía en
// nada: cada identificador se comprueba contra el catálogo.
export function sanitizeRaw(o) {
  if (!o || typeof o !== 'object' || Array.isArray(o)) return null;
  const raw = emptyRaw();
  const ent = (e, ids, max) => {
    if (!e || typeof e !== 'object' || !ids.includes(e.id)) return null;
    const rel = e.relation && typeof e.relation === 'object' && pick(e.relation.type, C.relations) && typeof e.relation.to === 'string' && e.relation.to !== e.id ? { type: e.relation.type, of: e.relation.to } : null;
    return { id: e.id, color: pick(e.color, C.colors), color2: pick(e.color2, C.colors), n: int(e.n, 1, max, 1), size: int(e.size, -1, 1, 0), fly: typeof e.fly === 'boolean' ? e.fly : null, side: pick(e.side, C.sides), depth: pick(e.depth, C.depths), rel, a: {} };
  };
  const list = (v, ids, max, cap, each) => (Array.isArray(v) ? v : []).slice(0, cap).map((e) => { const x = ent(e, ids, max); if (x && each) each(x, e); return x; }).filter(Boolean);
  raw.subjects = list(o.subjects, C.subjects, 5, 6, (x, e) => {
    const human = LIB[x.id].cat === 'humanoid';
    if (human && pick(e.armor, C.colors)) x.a.armor = e.armor;
    if (human && pick(e.weapon, C.weapons)) x.a.weapon = e.weapon;
    if (pick(e.weaponFx, C.weaponFx) && (x.a.weapon || x.id === 'sword' || LIB[x.id].cat === 'humanoid')) x.a.weaponFx = e.weaponFx;
    if (human && e.shield === true) x.a.shield = true;
    if (human && e.cape === true) x.a.cape = true;
    if (human && e.wizardHat === true) x.a.hat = 'wizard';
    if (x.id === 'dragon' && e.breathesFire === true) x.a.fire = true;
  });
  raw.structures = list(o.structures, C.structures, 1, 3);
  raw.flora = list(o.flora, C.flora, 6, 8);
  raw.biome = pick(o.biome, C.biomes);
  raw.features = uniq(o.features, C.features);
  raw.time = pick(o.time, C.times);
  raw.weather = uniq(o.weather, C.weather);
  raw.celestial = uniq(o.celestial, C.celestial);
  raw.fx = uniq(o.fx, C.fx);
  if (pick(o.skyColor, C.colors)) raw.sky.color = o.skyColor;
  if (o.crescentMoon === true) raw.moonPhase = 'crescent';
  // Una relación solo vale si su destino quedó en la escena.
  const present = new Set([...raw.subjects, ...raw.structures, ...raw.flora].map((e) => e.id));
  for (const e of [...raw.subjects, ...raw.structures, ...raw.flora]) if (e.rel && !present.has(e.rel.of)) e.rel = null;
  raw.no = uniq(o.exclude, [...C.celestial, ...C.weather, ...C.fx]);
  raw.celestial = raw.celestial.filter((c) => !raw.no.includes(c));
  raw.weather = raw.weather.filter((c) => !raw.no.includes(c));
  // "unknown" alimenta el aviso de limitaciones que ve el usuario.
  raw.unknown = (Array.isArray(o.unsupported) ? o.unsupported : []).filter((s) => typeof s === 'string' && s.trim()).slice(0, 8).map((s) => s.trim().slice(0, 80));
  raw.interpreted = 'ai';
  return raw;
}

// Inversa de sanitizeRaw: presenta al modelo la escena actual en su propio formato.
export function rawToSpecJSON(raw) {
  const e = (x) => ({ id: x.id, color: x.color ?? null, color2: x.color2 ?? null, n: x.n || 1, size: x.size || 0, side: x.side ?? null, depth: x.depth ?? null, relation: x.rel ? { type: x.rel.type, to: x.rel.of } : null });
  return {
    subjects: (raw.subjects || []).map((x) => ({ ...e(x), fly: x.fly ?? null, armor: typeof x.a?.armor === 'string' ? x.a.armor : null, weapon: x.a?.weapon ?? null, weaponFx: x.a?.weaponFx ?? null, shield: !!x.a?.shield, cape: !!x.a?.cape, wizardHat: x.a?.hat === 'wizard', breathesFire: !!x.a?.fire })),
    structures: (raw.structures || []).map((x) => { const { n, ...r } = e(x); return r; }),
    flora: (raw.flora || []).map(e),
    biome: raw.biome ?? null, features: raw.features || [], time: raw.time ?? null, weather: raw.weather || [], celestial: raw.celestial || [], fx: raw.fx || [],
    skyColor: raw.sky?.color ?? null, crescentMoon: raw.moonPhase === 'crescent', exclude: (raw.no || []).filter((n) => [...C.celestial, ...C.weather, ...C.fx].includes(n)), unsupported: [],
  };
}
