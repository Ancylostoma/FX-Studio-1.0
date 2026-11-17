import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateScene, parseScene, sanitizeScene, mapSceneColors, LIMITS } from '../src/core/pixelScene.js';
import { compactPalette } from '../src/core/paletteManager.js';

const base = () => ({ v: 1, w: 64, h: 64, pal: [[12, 18, 34], [35, 48, 80], [230, 190, 100]], bg: 0, layers: [{ n: 'sky', o: [['R', 0, 0, 64, 64, 0], ['E', 42, 12, 8, 8, 2]] }, { n: 'castle', o: [['R', 12, 30, 40, 22, 1]] }] });
const errs = (s) => validateScene(s).errors.join(' | ');

test('acepta el ejemplo del formato', () => assert.deepEqual(validateScene(base()), { ok: true, errors: [] }));

test('rechaza dimensiones fuera de límites', () => {
  for (const [w, h] of [[4, 64], [64, 1024], [64.5, 64], ['64', 64]]) assert.equal(validateScene({ ...base(), w, h }).ok, false);
});

test('rechaza índices de color inexistentes', () => {
  const s = base(); s.layers[0].o.push(['R', 0, 0, 1, 1, 3]);
  assert.match(errs(s), /índice de color 3 inexistente/);
  assert.equal(validateScene({ ...base(), bg: 7 }).ok, false);
});

test('rechaza operaciones no permitidas o mal formadas', () => {
  const bad = [['X', 0, 0], ['R', 0, 0, 5], ['R', 0, 0, -1, 5, 0], ['L', 0, 0, 5000, 0, 0], ['P', [0, 0, 1], 0], ['S', 'nope', 0, 0], ['M', 0, 0, ['01', '0']], ['M', 0, 0, ['0z']], 'R', ['G', 0, 0, 4, 4, []], ['__proto__', 1]];
  for (const op of bad) { const s = base(); s.layers[0].o = [op]; assert.equal(validateScene(s).ok, false, JSON.stringify(op)); }
});

test('acepta todas las operaciones válidas', () => {
  const s = base();
  s.rmp = [[0, 1, 2]];
  s.sprites = { star: { w: 3, h: 3, o: [['R', 1, 0, 1, 3, 2], ['R', 0, 1, 3, 1, 2]] } };
  s.layers.push({ n: 'todo', a: 0.5, sh: 1, ol: -2, f: [0, 30], o: [['L', 0, 0, 9, 9, 2], ['P', [0, 0, 9, 0, 4, 9], 1, 1], ['M', 2, 2, ['01.', '.20']], ['M', 0, 0, [[0, -1, 2]]], ['S', 'star', 5, 5, 1], ['G', 0, 0, 8, 8, [0, 1, 2], 1], ['D', 0, 0, 8, 8, 2, 8, 2], ['T', 0, 0, 8, 8, 1, 10, 42], ['B', 0, 0, 9, 9, 'star'], ['H', 0, 0, 8, 8, -1], ['R', 0, 0, 2, 2, -1]] });
  assert.deepEqual(validateScene(s).errors, []);
});

test('los sprites no pueden anidarse', () => {
  const s = base(); s.sprites = { a: { w: 2, h: 2, o: [['S', 'a', 0, 0]] } };
  assert.match(errs(s), /anidar/);
});

test('estructura de capas y límites globales', () => {
  assert.equal(validateScene({ ...base(), layers: [{ n: 'x' }] }).ok, false);
  assert.equal(validateScene({ ...base(), layers: [{ o: [], a: 2 }] }).ok, false);
  const many = base(); many.layers[0].o = Array.from({ length: LIMITS.maxOps + 1 }, () => ['R', 0, 0, 1, 1, 0]);
  assert.match(errs(many), /Demasiadas operaciones/);
});

test('parseScene: JSON inválido, enorme o con código no se ejecuta', () => {
  assert.equal(parseScene('{no json').ok, false);
  assert.equal(parseScene('x'.repeat(LIMITS.maxJson + 1)).ok, false);
  assert.equal(parseScene('{"v":1,"w":64,"h":64,"pal":[[0,0,0]],"bg":0,"layers":[{"o":[["R",0,0,1,1,"alert(1)"]]}]}').ok, false);
  assert.equal(parseScene(JSON.stringify(base())).ok, true);
});

test('sanitizeScene repara lo reparable y descarta el resto', () => {
  const raw = { w: 64.4, h: 9999, pal: [[300, -5, 10.6]], bg: 5, layers: [{ n: 'a', o: [['R', 1.4, 2.6, 5, 5, 0], ['R', 0, 0, 5, 5, 9], ['EVAL', 'x'], ['E', 5, 5, 2, 2, 0]] }, 'basura'] };
  const { scene, warnings } = sanitizeScene(raw);
  assert.ok(scene); assert.equal(validateScene(scene).ok, true);
  assert.equal(scene.h, LIMITS.maxDim); assert.deepEqual(scene.pal[0], [255, 0, 11]);
  assert.deepEqual(scene.layers[0].o, [['R', 1, 3, 5, 5, 0], ['E', 5, 5, 2, 2, 0]]);
  assert.equal(warnings.length, 2);
  assert.equal(sanitizeScene('hola').scene, null);
});

test('compactPalette elimina colores sin uso y fusiona duplicados', () => {
  const s = base(); s.pal.push([9, 9, 9], [230, 190, 100]); s.layers[1].o.push(['R', 0, 0, 1, 1, 4]);
  const c = compactPalette(s);
  assert.equal(c.pal.length, 3); assert.equal(validateScene(c).ok, true);
  assert.equal(c.layers[1].o[1][5], 2);
});

test('mapSceneColors transforma matrices de símbolos', () => {
  const s = base(); s.layers[0].o = [['M', 0, 0, ['0.1']]];
  assert.deepEqual(mapSceneColors(s, (c) => c + 1).layers[0].o[0][3], ['1.2']);
});
