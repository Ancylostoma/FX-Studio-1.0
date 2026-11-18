// Intérprete de prompts offline: lenguaje natural → especificación de escena.
// Dos fases: extract() obtiene solo lo que el usuario dijo ("raw") y
// finalize() añade los valores por defecto. Guardar el raw permite editar la
// escena después ("agrega una luna") sin perder la intención original.
import { PHRASES, STOPWORDS, MAX_PHRASE, normalize } from './vocabulary.js';
import { LIB, defOf } from './spriteLibrary.js';
import { checkTheme, inferTheme } from './environments.js';
import { BIOMES, COLOR_NAMES } from './templates.js';

export const MAX_PROMPT = 600;

export function emptyRaw() {
  return { subjects: [], structures: [], flora: [], features: [], biome: null, time: null, weather: [], celestial: [], fx: [], themes: [], sky: {}, style: null, moonPhase: null, anim: null, no: [], unknown: [],
    // Dirección artística (la rellenan el parser o la IA): escenario interior,
    // asentamiento, batalla, vista, luz, tema de paleta, protagonista, nº de planetas.
    setting: null, room: null, town: null, battle: false, crowd: false, view: null, light: null, theme: null, focus: null, planets: 0 };
}

function tokenize(text) {
  const words = normalize(text.slice(0, MAX_PROMPT)).split(' ').filter(Boolean);
  const out = [];
  for (let i = 0; i < words.length;) {
    let hit = null;
    for (let n = Math.min(MAX_PHRASE, words.length - i); n >= 1 && !hit; n--) {
      const ph = words.slice(i, i + n).join(' ');
      let tag = PHRASES.get(ph);
      if (!tag && n === 1) tag = PHRASES.get(ph.replace(/es$/, '')) || PHRASES.get(ph.replace(/s$/, ''));
      if (tag) hit = { tag, n, word: ph };
    }
    if (hit) { out.push(hit); i += hit.n; continue; }
    const w = words[i++];
    if (/^\d+$/.test(w)) out.push({ tag: 'n:' + Math.min(6, +w), word: w });
    else out.push({ tag: STOPWORDS.has(w) ? 'stop' : 'unk', word: w });
  }
  return out;
}

// side: 'left'|'center'|'right' · depth: 'back'|'front' · rel: { type, of } respecto a otra entidad
export const ent = (id) => ({ id, color: null, color2: null, n: 1, size: 0, fly: null, side: null, depth: null, rel: null, a: {} });
export const REL_TYPES = ['on', 'behind', 'front', 'beside', 'leftof', 'rightof'];
const HUMANOID = (e) => defOf(e)?.cat === 'humanoid';

export function extract(text) {
  const raw = emptyRaw();
  const toks = tokenize(text);
  const ents = [];               // entidades coloreables en orden de aparición
  let pre = {};                  // modificadores que esperan al siguiente sustantivo
  const parts = [];
  const isNoun = (t) => t && /^(s|b|f|part):/.test(t.tag);
  const lastEnt = () => ents[ents.length - 1];
  const lastBody = () => { for (let j = ents.length - 1; j >= 0; j--) if (!ents[j].part) return ents[j]; return null; };
  // Quien actúa: «un mago lanza un hechizo desde la torre» → el mago, no el hechizo.
  const FX_SUBJ = new Set(['magic', 'fireball', 'explosion', 'smoke']);
  const lastActor = () => { for (let j = ents.length - 1; j >= 0; j--) if (!ents[j].part && !FX_SUBJ.has(ents[j].id)) return ents[j]; return lastBody(); };
  // Resuelve la relación pendiente con el sustantivo que acaba de aparecer.
  // «desde» solo cuenta si apunta a una construcción («desde la torre» → encima de la torre).
  const link = (e, isStruct) => {
    if (!pend || pend.a === e) return;
    if (pend.type === 'from') { if (isStruct) pend.a.rel = { type: 'on', of: e.id }; }
    else if (pend.type === 'below') e.rel = { type: 'on', of: pend.a.id }; else pend.a.rel = { type: pend.type, of: e.id };
    pend = null;
  };
  const nounSoon = (i) => { let j = i + 1, skipped = 0; while (toks[j] && skipped < 3 && /^(stop|unk$|col:|size:|n:)/.test(toks[j].tag)) { j++; skipped++; } return isNoun(toks[j]); };
  const rooms = [];
  let pend = null;               // relación a la espera de su segundo término ("detrás de" → …)
  // ¿Sigue "de/del/of" + sustantivo? Distingue "a la izquierda del castillo" de "a la izquierda".
  const ofNoun = (i) => { let j = i + 1; if (!(toks[j]?.tag === 'stop' && /^(de|del|of)$/.test(toks[j].word))) return false; j++; if (toks[j]?.tag === 'stop') j++; while (toks[j] && /^(col|size|n):/.test(toks[j].tag)) j++; return isNoun(toks[j]) && !toks[j].tag.startsWith('part:'); };
  const setColor = (e, c) => { if (!e.color) e.color = c; else if (!e.color2) e.color2 = c; };
  toks.forEach((t, i) => {
    const [kind, id] = t.tag.split(':');
    const prev = toks[i - 1], next = toks[i + 1];
    // "la casa", "del castillo": un artículo definido ante algo ya mencionado es una
    // referencia a ese elemento, no uno nuevo.
    const refer = (list) => {
      if (!(prev?.tag === 'stop' && /^(el|la|los|las|del|al|the)$/.test(prev.word))) return false;
      const cur = list.find((x) => x.id === id);
      if (!cur) return false;
      link(cur, list === raw.structures);
      pend = null; pre = {}; cur._i = i; ents.push(cur);
      return true;
    };
    const add = (list, e) => {
      if (pre.n) e.n = pre.n;
      if (pre.size) e.size = pre.size;
      if (pre.color) e.color = pre.color;
      if (pre.fly) e.fly = true;
      if (pre.side) e.side = pre.side;
      if (pre.depth) e.depth = pre.depth;
      if (list !== parts) link(e, list === raw.structures);
      // «unos murciélagos», «unas setas»: plural sin número → unos cuantos.
      if (!pre.n && e.n === 1 && /s$/.test(t.word) && /^(unos|unas|algunos|algunas|some)$/.test(prev?.word || '')) e.n = 3;
      pre = {};
      e._i = i;
      list.push(e); ents.push(e);
      return e;
    };
    if (pend && (--pend.ttl < 0 || /^(biome|feat|time|w|c|fx)$/.test(kind))) pend = null;
    switch (kind) {
      case 'side': case 'depth': {
        const a = lastBody();
        if (kind === 'side' && id !== 'center' && a && ofNoun(i)) pend = { type: id === 'left' ? 'leftof' : 'rightof', a, ttl: 5 };
        // Si el elemento anterior ya tiene ese dato y enseguida viene otro sustantivo,
        // la posición es del que viene ("…a la izquierda, a la derecha un dragón").
        else if (a && !isNoun(next) && !(a[kind] && nounSoon(i))) a[kind] = id;
        else pre[kind] = id;
        break;
      }
      case 'rel': { const a = id === 'from' || id === 'on' ? lastActor() : lastBody(); if (a) pend = { type: id, a, ttl: 5 }; break; }
      case 's': {
        // «guerrera elfa», «elfa guerrera», «orco guerrero»: «guerrero» describe al otro, no es un caballero aparte.
        const RACE = /^s:(elf|orc|goblin|troll|viking|samurai|ninja|skeleton|zombie|vampire|demon|angel|fairy|robot|alien|pirate|princess|king|archer|witch|wizard|monk|golem)$/;
        if (id === 'knight' && /^(guerrer[oa]s?|warriors?)$/.test(t.word) && (RACE.test(next?.tag || '') || RACE.test(prev?.tag || ''))) break;
        if (!refer(raw.subjects)) add(raw.subjects, ent(id));
        break;
      }
      case 'b': if (!refer(raw.structures)) add(raw.structures, ent(id)); break;
      case 'f': if (!refer(raw.flora)) add(raw.flora, ent(id)); break;
      case 'part': { const p = add(parts, ent(id)); p.part = id; break; }
      case 'biome': raw.biome = id; if (/^polar(es)?$/.test(t.word) && prev?.tag.startsWith('s:')) setColor(lastEnt(), 'white'); break;   // «oso polar» → blanco
      case 'feat': if (!raw.features.includes(id)) raw.features.push(id); break;
      case 'time': raw.time = id; break;
      case 'w': if (!raw.weather.includes(id)) raw.weather.push(id); break;
      case 'c': {
        const cid = id === 'crescent' ? 'moon' : id;
        if (id === 'crescent') raw.moonPhase = 'crescent';
        if (!raw.celestial.includes(cid)) raw.celestial.push(cid);
        break;
      }
      case 'fx': if (!raw.fx.includes(id)) raw.fx.push(id); break;
      case 'theme': if (!raw.themes.includes(id)) raw.themes.push(id); break;
      case 'style': raw.style = id; break;
      case 'setting': raw.setting = 'interior'; break;
      case 'room': rooms.push(id); break;
      case 'town': raw.town = { kind: id }; break;
      case 'battle': raw.battle = true; break;
      case 'bright': raw.bright = Math.max(-2, Math.min(2, (raw.bright || 0) + (id === 'up' ? 1 : -1))); break;
      case 'facing': { const a = raw.subjects[raw.subjects.length - 1] || lastBody(); if (a) a.facing = id; break; }
      case 'jungle': raw.biome = 'forest'; if (!raw.themes.includes('tropical')) raw.themes.push('tropical'); break;
      case 'crowd': raw.crowd = true; break;
      case 'view': raw.view = id; break;
      case 'light': raw.light = id; break;
      case 'planets': raw.planets = 4; if (!raw.celestial.includes('planet')) raw.celestial.push('planet'); break;
      case 'anim': raw.anim = id; break;
      case 'col':
        if (isNoun(prev) && lastEnt()) setColor(lastEnt(), id);
        else if (isNoun(next)) pre.color = id;
        else if (lastEnt() && i - lastEnt()._i <= 4) setColor(lastEnt(), id);
        else pre.color = id;
        break;
      case 'size': {
        const v = id === 'big' ? 1 : -1;
        if (isNoun(next) || !lastEnt()) pre.size = v; else lastEnt().size = v;
        break;
      }
      case 'n': pre.n = +id; break;
      case 'fly': { const s = raw.subjects[raw.subjects.length - 1]; if (s) s.fly = true; else pre.fly = true; break; }
      case 'mat': {
        const e = lastEnt();
        if (e && (e.id === 'sword' || e.part === 'staff')) e.a.weaponFx = id;
        else if (e && e.id === 'dragon' && id === 'fire') e.a.fire = true;
        else if (id === 'fire' && !e) add(raw.flora, ent('campfire'));
        else if (e) e.a.weaponFx = id;
        break;
      }
      case 'unk': if (t.word.length > 2 && !raw.unknown.includes(t.word)) raw.unknown.push(t.word); break;
      default: break;
    }
  });
  if (pre.color) { const e = ents.find((x) => !x.color) || ents[0]; if (e) setColor(e, pre.color); }
  // Partes y equipo: se asignan al personaje humanoide si lo hay.
  const needsBody = parts.some((p) => ['armor', 'helmet', 'hat', 'cape', 'staff'].includes(p.part));
  let hero = raw.subjects.find(HUMANOID);
  if (!hero && needsBody) { hero = ent(parts.some((p) => p.part === 'armor') ? 'knight' : 'character'); raw.subjects.unshift(hero); }
  for (const p of parts) {
    if (p.part === 'sky') { if (p.color) raw.sky.color = p.color; continue; }
    if (!hero) continue;
    if (p.part === 'armor') hero.a.armor = p.color || 'silver';
    if (p.part === 'helmet') hero.a.helmet = true;
    if (p.part === 'hat') hero.a.hat = 'wizard';
    if (p.part === 'cape') hero.a.cape = p.color || true;
    if (p.part === 'staff') { hero.a.weapon = 'staff'; if (p.a.weaponFx) hero.a.weaponFx = p.a.weaponFx; }
  }
  if (hero) {
    raw.subjects = raw.subjects.filter((s) => {
      if (s === hero) return true;
      const inherit = () => { for (const f of ['rel', 'side', 'depth']) if (s[f] && !hero[f]) hero[f] = s[f]; };
      if (s.id === 'sword') { hero.a.weapon = 'sword'; if (s.a.weaponFx) hero.a.weaponFx = s.a.weaponFx; inherit(); return false; }
      if (s.id === 'shield') { hero.a.shield = true; if (s.color) hero.color2 = s.color; inherit(); return false; }
      return true;
    });
  }
  // Estancias: mazmorra, sala del trono, laboratorio o habitación implican un
  // interior; biblioteca y taberna solo si se dice "interior de…" (si no, son edificios).
  const INDOOR = ['dungeon', 'throne', 'lab', 'room'];
  if (rooms.length) {
    if (raw.setting === 'interior' || rooms.some((r) => INDOOR.includes(r))) { raw.setting = 'interior'; raw.room = rooms.find((r) => INDOOR.includes(r) || raw.setting) || rooms[0]; }
    else for (const r of rooms) { const id = r === 'library' ? 'library' : r === 'tavern' ? 'medievalhouse' : null; if (id && !raw.structures.some((e) => e.id === id)) raw.structures.push(ent(id)); }
  }
  // Una relación solo es válida si su destino sigue en la escena y no es la propia entidad.
  const present = new Set([...raw.subjects, ...raw.structures, ...raw.flora].map((e) => e.id));
  for (const list of [raw.subjects, raw.structures, raw.flora]) for (const e of list) { delete e._i; if (e.rel && (!present.has(e.rel.of) || e.rel.of === e.id)) e.rel = null; }
  return raw;
}

const uniq = (a) => [...new Set(a)];

// Aplica valores por defecto y coherencia de escena. No muta el raw.
export function finalize(raw) {
  const s = JSON.parse(JSON.stringify(raw));
  const no = new Set(s.no || []);
  const ids = (l) => l.map((e) => e.id);
  const sub = ids(s.subjects), str = ids(s.structures), flo = ids(s.flora);
  s.notes = [];
  if (s.themes.includes('future') && (s.biome === 'city' || str.includes('skyscraper'))) s.biome = 'cyber';
  if (s.themes.includes('japan')) {
    s.features = uniq(s.features.map((f) => (f === 'mountains' ? 'fuji' : f)));
    if (!s.features.includes('fuji') && !no.has('fuji')) s.features.push('fuji');
    if (!flo.includes('cherry') && !no.has('cherry')) s.flora.push(ent('cherry'));
    if (!str.length && !no.has('torii')) s.structures.push(ent('torii'));
  }
  // --- Dirección artística por reglas (modo procedural) ---
  if (s.setting === 'interior') { s.room ||= 'room'; s.biome = 'interior'; }
  if (s.town) {
    const fut = s.themes.includes('future') || s.biome === 'cyber';
    if (fut) { s.biome = 'cyber'; s.town = null; }
    else {
      s.town.style ||= s.themes.includes('tropical') || s.biome === 'beach' || s.biome === 'sea' ? 'tropical' : s.themes.includes('japan') ? 'japanese' : s.biome === 'desert' ? 'desert' : s.biome === 'snow' ? 'nordic' : 'medieval';
      s.town.size ||= s.town.kind === 'city' ? 3 : 2;
      if (s.biome === 'sea') s.biome = 'beach';
      if (!s.biome || s.biome === 'city') s.biome = s.town.style === 'tropical' ? 'beach' : s.town.style === 'desert' ? 'desert' : 'town';
      if (!s.themes.includes('medieval') && s.town.style === 'medieval') s.themes.push('medieval');
    }
  }
  if (s.biome === 'city' && s.themes.includes('medieval')) { s.biome = 'town'; s.town = { kind: 'city', style: 'medieval', size: 3 }; }
  // Tren que cruza montañas: va sobre un puente.
  const train = s.subjects.find((e) => e.id === 'train');
  if (train) {
    if (s.themes.includes('future')) { train.a.style = 'futurist'; }
    if ((s.features.includes('mountains') || s.structures.some((e) => e.id === 'bridge')) && !no.has('bridge')) {
      let br = s.structures.find((e) => e.id === 'bridge');
      if (!br) { br = { id: 'bridge', color: null, color2: null, n: 1, size: 0, fly: null, side: null, depth: null, rel: null, a: {} }; s.structures.push(br); }
      if (s.themes.includes('future')) br.a.future = true;
      train.rel ||= { type: 'on', of: 'bridge' };
    }
  }
  // Batalla: los contendientes atacan; si faltan, se eligen rivales.
  if (s.battle || s.crowd) {
    const fighters = s.subjects.filter((e) => defOf(e)?.cat !== 'effect' && !['object'].includes(defOf(e)?.cat));
    const mk = (id) => ({ id, color: null, color2: null, n: 1, size: 0, fly: null, side: null, depth: null, rel: null, a: {} });
    if (s.battle && fighters.length < 2) {
      const pool = fighters.length ? (defOf(fighters[0])?.cat === 'humanoid' ? ['dragon', 'troll', 'orc'] : ['knight', 'golem', 'griffin']) : ['dragon', 'troll'];
      for (const id of pool) { if (s.subjects.filter((e) => defOf(e)?.cat !== 'effect').length >= 2) break; if (!s.subjects.some((e) => e.id === id)) s.subjects.push(mk(id)); }
      s.notes.push('Añado contendientes para la batalla: ' + s.subjects.filter((e) => !fighters.includes(e)).map((e) => LIB[e.id]?.name || e.id).join(' y ') + '.');
    }
    if (s.battle) {
      for (const e of s.subjects) if (defOf(e)?.cat !== 'effect') e.action ||= 'attack';
      if (!s.subjects.some((e) => defOf(e)?.cat === 'effect')) s.subjects.push({ ...mk('magic'), clash: true, fly: true, size: -1 });
      if (s.subjects.some((e) => e.id === 'dragon')) s.subjects.find((e) => e.id === 'dragon').a.fire = true;
    }
  }
  if (s.features.includes('waterfall') && !s.features.includes('lake')) s.features.push('lake');
  const sceneHints = s.biome || s.town || s.setting || s.battle || s.features.length || str.length || flo.length || s.time || s.weather.length || s.celestial.length || s.fx.length || s.sky.color || s.themes.includes('japan');
  if (!s.biome && sceneHints) {
    if (sub.includes('astronaut') || sub.includes('alien') || ((sub.includes('rocket') || sub.includes('spaceship') || sub.includes('ufo')) && s.celestial.includes('planet'))) s.biome = s.celestial.includes('planet') || sub.includes('rocket') || sub.includes('spaceship') ? 'space' : 'moonland';
    else if (sub.includes('boat') || sub.includes('whale') || sub.includes('shark') || str.includes('lighthouse')) s.biome = 'sea';
    else if (flo.includes('cactus') || str.includes('pyramid')) s.biome = 'desert';
    else if (flo.includes('palm')) s.biome = 'beach';
    else if (str.includes('skyscraper') || sub.includes('car')) s.biome = 'city';
    else if (s.weather.includes('snow')) s.biome = 'snow';
    else if (s.features.includes('mountains') && !s.features.includes('lake')) s.biome = 'mountains';
    else if (flo.includes('pine') && !str.length) s.biome = 'forest';
    else s.biome = 'meadow';
  }
  if (!sceneHints && !sub.length) { s.biome = 'meadow'; s.notes.push('No reconocí ningún elemento concreto: genero un paisaje de pradera.'); }
  if (s.biome === 'snow' && !s.weather.includes('snow') && !no.has('snow')) { /* nieve en el suelo, sin nevada */ }
  const b = s.biome ? BIOMES[s.biome] : null;
  if (!s.time && s.setting === 'interior') s.time = 'night';
  if (!s.time) s.time = (s.biome === 'cyber' || s.fx.includes('fireflies') || s.themes.includes('spooky') || s.celestial.includes('moon') || s.celestial.includes('aurora') || (s.celestial.includes('stars') && !s.celestial.includes('sun'))) ? 'night' : 'day';
  if (s.themes.includes('magic')) { if (!no.has('sparkles')) s.fx = uniq([...s.fx, 'sparkles']); if (s.time === 'night' && s.biome === 'forest' && !no.has('fireflies')) s.fx = uniq([...s.fx, 'fireflies']); }
  if (s.biome === 'cyber' && !s.weather.length && !no.has('rain')) s.weather.push('rain');
  const addC = (id) => { if (!s.celestial.includes(id) && !no.has(id)) s.celestial.push(id); };
  if (b && !b.noSky) {
    if (b.sky === 'space') { addC('stars'); if (s.biome === 'space') addC('planet'); }
    else {
      if (s.time === 'night') { addC('stars'); if (!s.celestial.includes('sun')) addC('moon'); }
      if (s.time === 'sunset' || s.time === 'dawn') { if (!s.celestial.includes('moon')) addC('sun'); addC('clouds'); if (s.time === 'sunset') addC('stars'); }
      if (s.time === 'day') { if (s.biome !== 'mars') addC('clouds'); if (['desert', 'beach', 'sea', 'mars'].includes(s.biome)) addC('sun'); }
      if (s.weather.includes('rain') || s.weather.includes('storm')) { addC('clouds'); s.celestial = s.celestial.filter((c) => c !== 'sun'); }
    }
  }
  s.celestial = s.celestial.filter((c) => !no.has(c));
  s.weather = s.weather.filter((c) => !no.has(c));
  s.fx = s.fx.filter((c) => !no.has(c));
  for (const e of [...s.subjects, ...s.structures, ...s.flora]) { e.side ??= null; e.depth ??= null; e.rel ??= null; e.action ??= null; e.facing ??= null; }
  // Protagonista: el indicado o el primer sujeto (o estructura).
  s.focus ||= s.subjects.find((e) => defOf(e)?.cat !== 'effect')?.id || s.structures[0]?.id || null;
  // Paleta: la propuesta (si es coherente con la escena) o la que corresponda.
  const th = checkTheme(s.theme || inferTheme(s), s);
  s.theme = th.theme; if (th.note) s.notes.push(th.note);
  if (s.planets) s.planets = Math.max(1, Math.min(5, s.planets | 0));
  for (const e of s.subjects) {
    const def = defOf(e);
    if (e.fly == null) e.fly = !!def?.air;
    e.n = Math.max(1, Math.min(5, e.n || 1));
  }
  if (s.unknown.length) s.notes.push((s.interpreted === 'ai' ? 'Adaptado al catálogo del motor: ' : 'Palabras no reconocidas por el motor offline: ') + s.unknown.join(s.interpreted === 'ai' ? '; ' : ', ') + '.');
  return s;
}

export function parsePrompt(text) {
  const raw = extract(text);
  return { raw, spec: finalize(raw) };
}

// Representación conceptual compacta, p. ej.
// SUBJECT=dragon(red);SCENE=castle;BIOME=meadow;LIGHT=night;SKY=stars,moon
export function describeSpec(spec, opts = {}) {
  const e = (x) => x.id + (x.n > 1 ? '×' + x.n : '') + (x.color ? `(${x.color})` : '') + (x.side ? '@' + x.side : '') + (x.depth ? '@' + x.depth : '') + (x.rel ? `→${x.rel.type}:${x.rel.of}` : '') + (Object.keys(x.a || {}).length ? '{' + Object.entries(x.a).map(([k, v]) => (v === true ? k : `${k}:${v}`)).join(',') + '}' : '');
  const out = [];
  if (spec.subjects.length) out.push('SUBJECT=' + spec.subjects.map(e).join('+'));
  if (spec.structures.length) out.push('SCENE=' + spec.structures.map(e).join('+'));
  if (spec.flora.length) out.push('PROPS=' + spec.flora.map(e).join('+'));
  if (spec.biome) out.push('BIOME=' + spec.biome);
  if (spec.features.length) out.push('RELIEF=' + spec.features.join(','));
  if (spec.setting === 'interior') out.push('ROOM=' + spec.room);
  if (spec.town) out.push(`TOWN=${spec.town.style}×${spec.town.size}`);
  if (spec.battle) out.push('ACTION=battle');
  out.push('LIGHT=' + spec.time + (spec.light === 'right' ? '→' : ''));
  if (spec.view && spec.view !== 'side') out.push('VIEW=' + spec.view);
  if (spec.theme && spec.theme !== 'natural') out.push('PALETTE=' + spec.theme);
  if (spec.focus) out.push('FOCUS=' + spec.focus);
  if (spec.celestial.length) out.push('SKY=' + spec.celestial.join(',') + (spec.sky.color ? `(${spec.sky.color})` : ''));
  else if (spec.sky.color) out.push(`SKY=(${spec.sky.color})`);
  if (spec.weather.length) out.push('WEATHER=' + spec.weather.join(','));
  if (spec.fx.length) out.push('FX=' + spec.fx.join(','));
  if (opts.style) out.push('STYLE=' + opts.style);
  if (opts.seed != null) out.push('SEED=' + opts.seed);
  return out.join(';');
}

// Resumen legible en español de lo que el motor entendió.
export function explainSpec(spec) {
  const name = (x) => (x.n > 1 ? x.n + '× ' : '') + (defOf(x)?.name || x.id) + (x.color ? ' ' + (COLOR_NAMES[x.color] || x.color) : '');
  const parts = [];
  if (spec.subjects.length) parts.push('Sujeto: ' + spec.subjects.map(name).join(', '));
  if (spec.structures.length) parts.push('Estructuras: ' + spec.structures.map(name).join(', '));
  if (spec.flora.length) parts.push('Elementos: ' + spec.flora.map(name).join(', '));
  if (spec.setting === 'interior') parts.push('Interior: ' + ({ library: 'biblioteca', tavern: 'taberna', dungeon: 'mazmorra', throne: 'sala del trono', lab: 'laboratorio', room: 'habitación' }[spec.room] || spec.room));
  else if (spec.town) parts.push('Entorno: ' + ({ medieval: 'ciudad medieval', tropical: 'aldea tropical', japanese: 'pueblo japonés', desert: 'ciudad del desierto', nordic: 'aldea nórdica' }[spec.town.style]));
  else if (spec.biome) parts.push('Entorno: ' + BIOMES[spec.biome].name);
  if (spec.battle) parts.push('Acción: batalla');
  const T = { day: 'día', night: 'noche', sunset: 'atardecer', dawn: 'amanecer' };
  parts.push('Luz: ' + T[spec.time]);
  return parts.join(' · ');
}

// ---------- Instrucciones de edición ----------
// "Conserva el castillo, cambia el cielo a púrpura y agrega una luna."
export function applyEdit(rawIn, instruction) {
  const raw = JSON.parse(JSON.stringify(rawIn));
  raw.no ||= [];
  const done = [];
  const clauses = String(instruction).slice(0, MAX_PROMPT).split(/[,;.]|\s+y\s+|\s+and\s+|\s+e\s+/i).map((c) => c.trim()).filter(Boolean);
  let verb = 'add';
  for (const clause of clauses) {
    const toks = tokenize(clause);
    const v = toks.find((t) => t.tag.startsWith('verb:'));
    if (v) verb = v.tag.split(':')[1];
    const part = extract(clause);
    if (verb === 'keep') { done.push('Conservo: ' + clause); continue; }
    const lists = ['subjects', 'structures', 'flora'];
    if (verb === 'remove') {
      for (const l of lists) for (const e of part[l]) { raw[l] = raw[l].filter((x) => x.id !== e.id); raw.no.push(e.id); }
      for (const l of ['celestial', 'weather', 'fx', 'features']) for (const id of part[l]) { raw[l] = raw[l].filter((x) => x !== id); raw.no.push(id); }
      if (part.biome && raw.biome === part.biome) raw.biome = null;
      done.push('Elimino: ' + clause);
      continue;
    }
    for (const l of lists) for (const e of part[l]) {
      const cur = raw[l].find((x) => x.id === e.id);
      raw.no = raw.no.filter((n) => n !== e.id);
      const moved = e.side || e.depth || e.rel;
      if (cur && (verb === 'change' || moved)) { if (e.color) cur.color = e.color; if (e.color2) cur.color2 = e.color2; if (e.size) cur.size = e.size; if (e.fly != null) cur.fly = e.fly; if (e.side) cur.side = e.side; if (e.depth) cur.depth = e.depth; if (e.rel) cur.rel = e.rel; Object.assign(cur.a, e.a); }
      else if (cur && e.color) cur.color = e.color;
      else if (!cur) raw[l].push(e);
      else cur.n = Math.min(5, cur.n + e.n);
    }
    for (const l of ['celestial', 'weather', 'fx', 'features', 'themes']) for (const id of part[l]) { raw.no = raw.no.filter((n) => n !== id); if (!raw[l].includes(id)) raw[l].push(id); }
    if (part.celestial.includes('moon')) raw.celestial = raw.celestial.filter((c) => c !== 'sun');
    if (part.celestial.includes('sun')) raw.celestial = raw.celestial.filter((c) => c !== 'moon');
    if (part.moonPhase) raw.moonPhase = part.moonPhase;
    if (part.biome) raw.biome = part.biome;
    if (part.time) { raw.time = part.time; raw.celestial = raw.celestial.filter((c) => !['sun', 'moon', 'stars'].includes(c) || part.celestial.includes(c)); }
    if (part.sky.color) raw.sky.color = part.sky.color;
    if (part.bright) raw.bright = Math.max(-2, Math.min(2, (raw.bright || 0) + part.bright));
    // Un color suelto tras "cambia" se aplica al sujeto principal.
    done.push((verb === 'change' ? 'Cambio: ' : 'Añado: ') + clause);
  }
  return { raw, done };
}
