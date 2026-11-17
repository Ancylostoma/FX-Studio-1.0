import { test } from 'node:test';
import assert from 'node:assert/strict';
import { extract, finalize, parsePrompt, describeSpec, applyEdit } from '../src/generator/promptParser.js';

const ids = (l) => l.map((e) => e.id);

test('dragón rojo volando sobre un castillo de noche', () => {
  const { spec } = parsePrompt('Un dragón rojo volando sobre un castillo medieval durante la noche.');
  assert.deepEqual(ids(spec.subjects), ['dragon']); assert.equal(spec.subjects[0].color, 'red'); assert.equal(spec.subjects[0].fly, true);
  assert.deepEqual(ids(spec.structures), ['castle']); assert.equal(spec.time, 'night'); assert.equal(spec.biome, 'meadow');
  assert.ok(spec.celestial.includes('moon') && spec.celestial.includes('stars'));
  assert.deepEqual(spec.unknown, []);
  assert.match(describeSpec(spec, { style: 'bit16' }), /^SUBJECT=dragon\(red\)→on:castle;SCENE=castle;BIOME=meadow;LIGHT=night;SKY=.*STYLE=bit16$/);
});

test('caballero con armadura dorada y espada frente a un castillo al atardecer', () => {
  const { spec } = parsePrompt('Un caballero medieval con armadura dorada, sosteniendo una espada, frente a un castillo durante el atardecer.');
  assert.deepEqual(ids(spec.subjects), ['knight']);
  assert.equal(spec.subjects[0].a.armor, 'gold'); assert.equal(spec.subjects[0].a.weapon, 'sword');
  assert.equal(spec.time, 'sunset'); assert.deepEqual(ids(spec.structures), ['castle']);
});

test('personaje RPG con armadura dorada y espada de fuego (sin escenario)', () => {
  const { spec } = parsePrompt('Un personaje RPG con armadura dorada y una espada de fuego');
  assert.deepEqual(ids(spec.subjects), ['character']);
  assert.deepEqual(spec.subjects[0].a, { weaponFx: undefined, armor: 'gold', weapon: 'sword', ...spec.subjects[0].a });
  assert.equal(spec.subjects[0].a.weaponFx, 'fire'); assert.equal(spec.biome, null); assert.equal(spec.style, 'rpg');
});

test('biomas, clima y temas', () => {
  let s = parsePrompt('Una ciudad futurista cyberpunk bajo la lluvia').spec;
  assert.equal(s.biome, 'cyber'); assert.ok(s.weather.includes('rain')); assert.equal(s.time, 'night');
  s = parsePrompt('Un astronauta caminando sobre Marte').spec;
  assert.equal(s.biome, 'mars'); assert.deepEqual(ids(s.subjects), ['astronaut']); assert.equal(s.subjects[0].fly, false);
  s = parsePrompt('Un bosque encantado con una pequeña casa y luciérnagas').spec;
  assert.equal(s.biome, 'forest'); assert.equal(s.structures[0].size, -1); assert.ok(s.fx.includes('fireflies') && s.fx.includes('sparkles')); assert.equal(s.time, 'night');
  s = parsePrompt('Un paisaje japonés con montañas, un lago y un árbol de cerezo').spec;
  assert.deepEqual(s.features.sort(), ['fuji', 'lake']); assert.deepEqual(ids(s.flora), ['cherry']); assert.deepEqual(ids(s.structures), ['torii']);
});

test('colores: adjetivo detrás (español) y delante (inglés)', () => {
  let r = extract('una casa azul y un árbol rosa');
  assert.equal(r.structures[0].color, 'blue'); assert.equal(r.flora[0].color, 'pink');
  r = extract('a green dragon over a white castle');
  assert.equal(r.subjects[0].color, 'green'); assert.equal(r.structures[0].color, 'white');
  r = extract('castillo con cielo púrpura');
  assert.equal(r.sky.color, 'purple'); assert.equal(r.structures[0].color, null);
});

test('cantidades, tamaños y plurales', () => {
  const r = extract('tres ovejas, 2 pinos y un dragón enorme');
  assert.equal(r.subjects[0].id, 'sheep'); assert.equal(r.subjects[0].n, 3);
  assert.equal(r.flora[0].n, 2); assert.equal(r.subjects[1].size, 1);
});

test('informa de lo que no entiende y no inventa', () => {
  const { spec } = parsePrompt('un ornitorrinco tocando el saxofón');
  assert.deepEqual(spec.unknown.sort(), ['ornitorrinco', 'saxofon', 'tocando']);
  assert.ok(spec.notes.some((n) => /no reconocidas/i.test(n)));
  assert.equal(spec.biome, 'meadow'); assert.equal(spec.subjects.length, 0);
});

test('entradas hostiles: no fallan ni ejecutan nada', () => {
  for (const t of ['', '   ', '<script>alert(1)</script>', '__proto__ constructor', 'a'.repeat(5000), '🐉🔥', '"; DROP TABLE x; --']) {
    const { spec } = parsePrompt(t);
    assert.ok(spec.time); assert.equal(({}).polluted, undefined);
  }
  assert.ok(parsePrompt('dragon '.repeat(900)).spec.subjects.length <= 100);
});

test('edición: conserva, cambia el cielo y agrega una luna', () => {
  const { raw } = parsePrompt('Un castillo de día');
  const { raw: r2, done } = applyEdit(raw, 'Conserva el castillo, cambia el cielo a color púrpura y agrega una luna.');
  assert.equal(r2.sky.color, 'purple'); assert.ok(r2.celestial.includes('moon')); assert.deepEqual(ids(r2.structures), ['castle']);
  assert.equal(done.length, 3); assert.equal(raw.sky.color, undefined, 'no muta el original');
});

test('edición: quitar y recolorear', () => {
  const { raw } = parsePrompt('Un dragón rojo sobre un castillo de noche');
  let r = applyEdit(raw, 'quita la luna').raw;
  assert.ok(!finalize(r).celestial.includes('moon'));
  r = applyEdit(raw, 'cambia el dragón a verde').raw;
  assert.equal(r.subjects[0].color, 'green'); assert.equal(r.subjects.length, 1);
  r = applyEdit(raw, 'elimina el dragón y añade dos pinos').raw;
  assert.equal(r.subjects.length, 0); assert.equal(r.flora[0].n, 2);
});
