import { test } from 'node:test';
import assert from 'node:assert/strict';
import { extract, parsePrompt, applyEdit, describeSpec } from '../src/generator/promptParser.js';
import { composeScene } from '../src/generator/sceneComposer.js';
import { validateScene } from '../src/core/pixelScene.js';
import { renderScene } from '../src/core/renderer.js';
import { opaqueBounds } from '../src/core/geometry.js';
import { sanitizeRaw, rawToSpecJSON } from '../src/ai/specSchema.js';

const O = { w: 96, h: 96, style: 'bit16', seed: 7, detail: 2 };
// Caja visible de un elemento de la escena (todas las capas de su parte, sin sombras ni halos).
function boxOf(prompt, key, o = O) {
  const scene = composeScene(parsePrompt(prompt).spec, o);
  assert.deepEqual(validateScene(scene).errors, [], prompt);
  const only = scene.layers.map((l, i) => i).filter((i) => scene.layers[i].k === key && !/^(sombra|halo)/.test(scene.layers[i].n));
  assert.ok(only.length, `${prompt}: falta ${key}`);
  const img = renderScene(scene, { transparent: true, only });
  const b = opaqueBounds(img.data, img.w, img.h);
  return { ...b, cx: b.x + b.w / 2, bottom: b.y + b.h, z: Math.min(...only) };
}
const e = (raw, id) => [...raw.subjects, ...raw.structures, ...raw.flora].find((x) => x.id === id);

test('parser: lados y profundidad', () => {
  let r = extract('un castillo a la izquierda y un dragón a la derecha');
  assert.equal(e(r, 'castle').side, 'left'); assert.equal(e(r, 'dragon').side, 'right');
  r = extract('a la derecha una torre, en el centro un mago');
  assert.equal(e(r, 'tower').side, 'right'); assert.equal(e(r, 'wizard').side, 'center');
  r = extract('un castillo al fondo y un mago en primer plano a la izquierda');
  assert.equal(e(r, 'castle').depth, 'back'); assert.deepEqual([e(r, 'wizard').depth, e(r, 'wizard').side], ['front', 'left']);
  r = extract('a knight on the left and a tower in the background');
  assert.equal(e(r, 'knight').side, 'left'); assert.equal(e(r, 'tower').depth, 'back');
});

test('parser: relaciones entre elementos', () => {
  const rel = (p, id) => e(extract(p), id).rel;
  assert.deepEqual(rel('un gato encima de una casa', 'cat'), { type: 'on', of: 'house' });
  assert.deepEqual(rel('un dragón volando sobre un castillo', 'dragon'), { type: 'on', of: 'castle' });
  assert.deepEqual(rel('un árbol detrás de la casa', 'tree'), { type: 'behind', of: 'house' });
  assert.equal(extract('un árbol detrás').flora[0].rel, null, 'sin segundo término no hay relación');
  assert.deepEqual(rel('una casa y un árbol detrás de la casa', 'tree'), { type: 'behind', of: 'house' });
  assert.deepEqual(rel('un perro delante de una casa', 'dog'), { type: 'front', of: 'house' });
  assert.deepEqual(rel('un zorro junto a un pino', 'fox'), { type: 'beside', of: 'pine' });
  assert.deepEqual(rel('un templo y un cerezo a la derecha del templo', 'cherry'), { type: 'rightof', of: 'temple' });
  assert.deepEqual(rel('un cerezo a la izquierda de un templo', 'cherry'), { type: 'leftof', of: 'temple' });
  assert.deepEqual(rel('a cat behind a red house', 'cat'), { type: 'behind', of: 'house' });
  assert.deepEqual(rel('un gato debajo de un caballo', 'horse'), { type: 'on', of: 'cat' }, '"A debajo de B" = B sobre A');
  assert.deepEqual(rel('un caballero con una espada frente a un castillo', 'knight'), { type: 'front', of: 'castle' }, 'la relación pasa del arma al personaje');
});

test('parser: una mención con artículo definido es una referencia, no un duplicado', () => {
  const r = extract('una casa, un árbol detrás de la casa y un perro delante de la casa');
  assert.equal(r.structures.length, 1);
  assert.deepEqual([e(r, 'tree').rel.type, e(r, 'dog').rel.type], ['behind', 'front']);
  assert.equal(extract('una casa y otra casa').structures.length, 2);
});

test('parser: las preposiciones no crean relaciones espurias', () => {
  let r = extract('un astronauta caminando sobre Marte con un cohete');
  assert.equal(e(r, 'astronaut').rel, null); assert.equal(r.biome, 'mars');
  r = extract('una ciudad bajo la lluvia con un coche');
  assert.equal(e(r, 'car').rel, null);
  r = extract('un castillo a la izquierda y un dragón');
  assert.equal(e(r, 'castle').rel, null); assert.equal(e(r, 'castle').side, 'left');
  assert.match(describeSpec(parsePrompt('un gato encima de una casa a la izquierda').spec), /cat→on:house.*house@left/);
});

test('composición: lados', () => {
  const p = 'un castillo a la izquierda y un caballero a la derecha';
  assert.ok(boxOf(p, 'struct:castle').cx < 44); assert.ok(boxOf(p, 'subj:knight').cx > 60);
  const q = 'un castillo a la derecha y un caballero a la izquierda';
  assert.ok(boxOf(q, 'struct:castle').cx > 52); assert.ok(boxOf(q, 'subj:knight').cx < 36);
  for (const seed of [1, 2, 3, 4]) assert.ok(boxOf('un dragón a la izquierda en una pradera', 'subj:dragon', { ...O, seed }).cx < 40, 'independiente de la semilla');
});

test('composición: profundidad', () => {
  const far = boxOf('un mago al fondo en una pradera', 'subj:wizard'), near = boxOf('un mago en primer plano en una pradera', 'subj:wizard'), def = boxOf('un mago en una pradera', 'subj:wizard');
  assert.ok(far.h < def.h && def.h < near.h, `${far.h} < ${def.h} < ${near.h}`);
  assert.ok(far.bottom < def.bottom && def.bottom < near.bottom);
  assert.ok(boxOf('un castillo al fondo de día', 'struct:castle').h < boxOf('un castillo de día', 'struct:castle').h);
});

test('composición: encima de (posado, montado y sobrevolando)', () => {
  let p = 'un gato encima de una casa';
  let a = boxOf(p, 'subj:cat'), b = boxOf(p, 'struct:house');
  assert.ok(a.bottom <= b.y + b.h * 0.5 && a.bottom >= b.y - 1 && a.cx > b.x && a.cx < b.x + b.w, 'el gato está sobre el tejado');
  assert.ok(a.z > b.z, 'y se dibuja delante');
  p = 'un caballero sobre un caballo';
  a = boxOf(p, 'subj:knight'); b = boxOf(p, 'subj:horse');
  assert.ok(Math.abs(a.cx - b.cx) < b.w * 0.3 && a.bottom < b.bottom && a.bottom > b.y, 'el jinete va montado (retrato)');
  a = boxOf(p + ' en una pradera', 'subj:knight'); b = boxOf(p + ' en una pradera', 'subj:horse');
  assert.ok(Math.abs(a.cx - b.cx) < b.w * 0.3 && a.bottom < b.bottom && a.bottom > b.y && a.z > b.z, 'el jinete va montado (escena)');
  p = 'un dragón volando sobre un castillo';
  a = boxOf(p, 'subj:dragon'); b = boxOf(p, 'struct:castle');
  assert.ok(a.cx > b.x && a.cx < b.x + b.w && a.y < b.y, 'el dragón sobrevuela el castillo');
});

test('composición: detrás, delante y a los lados', () => {
  let p = 'una casa y un oso detrás de la casa';
  let a = boxOf(p, 'subj:bear'), b = boxOf(p, 'struct:house');
  assert.ok(a.z < b.z, 'el oso se dibuja antes que la casa'); assert.ok(a.bottom <= b.bottom);
  p = 'una casa y un perro delante de la casa';
  a = boxOf(p, 'subj:dog'); b = boxOf(p, 'struct:house');
  assert.ok(a.z > b.z && a.bottom > b.bottom && Math.abs(a.cx - b.cx) < b.w * 0.5);
  p = 'una torre y un zorro a la izquierda de la torre';
  assert.ok(boxOf(p, 'subj:fox').cx < boxOf(p, 'struct:tower').x);
  p = 'una torre y un zorro a la derecha de la torre';
  a = boxOf(p, 'subj:fox'); b = boxOf(p, 'struct:tower');
  assert.ok(a.cx > b.x + b.w);
});

test('composición: cadenas de relaciones y vegetación', () => {
  const p = 'un caballero sobre un caballo junto a una torre';
  const k = boxOf(p, 'subj:knight'), hs = boxOf(p, 'subj:horse'), t = boxOf(p, 'struct:tower');
  assert.ok(Math.abs(k.cx - hs.cx) < hs.w * 0.3, 'el jinete sigue al caballo cuando este se mueve');
  assert.ok(hs.x + hs.w < t.cx || hs.x > t.cx, 'el caballo queda a un lado de la torre');
  const scene = composeScene(parsePrompt('una casa y un árbol detrás de la casa').spec, O);
  const names = scene.layers.map((l) => l.n);
  assert.ok(names.indexOf('vegetación de fondo') >= 0 && names.indexOf('vegetación de fondo') < names.indexOf('casa'));
});

test('sin posiciones, la composición por defecto no cambia', () => {
  const a = composeScene(parsePrompt('un caballero y un castillo en una pradera').spec, O);
  assert.ok(a.layers.every((l) => l.n !== 'vegetación de fondo'));
  const b = boxOf('un caballero y un castillo en una pradera', 'subj:knight'), c = boxOf('un caballero y un castillo en una pradera', 'struct:castle');
  assert.ok(b.bottom > c.bottom, 'el sujeto sigue en primer plano');
});

test('edición: mover un elemento no lo duplica', () => {
  const { raw } = parsePrompt('un dragón sobre un castillo de noche');
  let r = applyEdit(raw, 'mueve el dragón a la izquierda').raw;
  assert.equal(r.subjects.length, 1); assert.equal(r.subjects[0].side, 'left');
  r = applyEdit(raw, 'pon el castillo al fondo').raw;
  assert.equal(r.structures.length, 1); assert.equal(r.structures[0].depth, 'back');
});

test('modo híbrido: las posiciones viajan en el esquema y se sanean', () => {
  const { raw } = parsePrompt('una casa a la izquierda, un gato encima de la casa y un árbol detrás de la casa');
  const back = sanitizeRaw(rawToSpecJSON(raw));
  assert.equal(back.structures[0].side, 'left'); assert.deepEqual(back.subjects[0].rel, { type: 'on', of: 'house' }); assert.deepEqual(back.flora[0].rel, { type: 'behind', of: 'house' });
  const bad = sanitizeRaw({ subjects: [{ id: 'cat', side: 'arriba', depth: 7, relation: { type: 'on', to: 'house' } }, { id: 'dog', relation: { type: 'teleport', to: 'cat' } }, { id: 'fox', relation: { type: 'on', to: 'fox' } }] });
  assert.deepEqual(bad.subjects.map((s) => [s.side, s.depth, s.rel]), [[null, null, null], [null, null, null], [null, null, null]], 'destino ausente, tipo inválido o autorreferencia se descartan');
});
