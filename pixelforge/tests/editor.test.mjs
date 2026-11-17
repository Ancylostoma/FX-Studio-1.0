import { test } from 'node:test';
import assert from 'node:assert/strict';
import { History } from '../src/editor/history.js';
import * as LM from '../src/editor/layerManager.js';
import * as CE from '../src/editor/colorEditor.js';
import { paintPixels, pickIndex, strokePixels } from '../src/editor/canvasEditor.js';
import { validateScene } from '../src/core/pixelScene.js';
import { renderScene } from '../src/core/renderer.js';
import { extractJSON, sceneFromModelText, parseRequest, aiConfigured } from '../server/aiAdapter.mjs';

const base = () => ({ v: 1, w: 8, h: 8, pal: [[0, 0, 0], [255, 0, 0], [0, 0, 255]], bg: 0, fx: {}, layers: [{ n: 'a', k: 'sky', o: [['R', 0, 0, 8, 8, 1]] }, { n: 'b', k: 'subj:x', o: [['R', 2, 2, 2, 2, 2]] }, { n: 'luces de b', k: 'subj:x', o: [] }] });

test('historial: deshacer, rehacer y rama nueva', () => {
  const h = new History(3);
  h.push({ n: 1 }); h.push({ n: 2 }); assert.equal(h.push({ n: 2 }), false);
  assert.deepEqual(h.undo(), { n: 1 }); assert.equal(h.undo(), null); assert.deepEqual(h.redo(), { n: 2 });
  h.undo(); h.push({ n: 3 }); assert.equal(h.canRedo, false);
  h.push({ n: 4 }); h.push({ n: 5 }); assert.equal(h.stack.length, 3);
});

test('capas: operaciones puras y válidas', () => {
  const s = base();
  let t = LM.toggleLayer(s, 1); assert.equal(t.layers[1].v, 0); assert.equal(s.layers[1].v, undefined);
  assert.equal(LM.toggleLayer(t, 1).layers[1].v, undefined);
  assert.deepEqual(LM.moveLayer(s, 0, 1).layers.map((l) => l.n), ['b', 'a', 'luces de b']);
  assert.equal(LM.moveLayer(s, 0, -1), s);
  assert.equal(LM.removeLayer(s, 0).layers.length, 2);
  assert.equal(LM.duplicateLayer(s, 1).layers[2].n, 'b copia');
  assert.equal(LM.setLayerOpacity(s, 1, 0.5).layers[1].a, 0.5); assert.equal(LM.setLayerOpacity(s, 1, 1).layers[1].a, undefined);
  assert.equal(LM.setLayerOutline(s, 1, 'sel').layers[1].ol, -2); assert.equal(LM.setLayerOutline(s, 1, 2).layers[1].ol, 2);
  t = LM.toggleElement(s, 'subj:x'); assert.deepEqual(t.layers.map((l) => l.v), [undefined, 0, 0]);
  for (const x of [t, LM.renameLayer(s, 0, '  nuevo  '), LM.setLayerShading(s, 1, true), LM.setLayerLit(s, 1, false)]) assert.equal(validateScene(x).ok, true);
});

test('color y luz: se editan datos, no píxeles', () => {
  const s = base();
  assert.deepEqual(CE.setPaletteColor(s, 1, [1.4, 999, -3]).pal[1], [1, 255, 0]);
  assert.equal(CE.setPaletteColor(s, 9, [1, 1, 1]), s);
  assert.equal(CE.setBackground(s, -1).bg, -1); assert.equal(CE.setBackground(s, 99).bg, 2);
  const q = CE.setQuantPalette(s, 'gb4'); assert.equal(q.q.length, 4); assert.equal(CE.quantKey(q), 'gb4'); assert.equal(CE.setQuantPalette(q, 'auto').q, undefined);
  const n = CE.setAmbient(s, 'night'); assert.equal(CE.ambientKey(n), 'night'); assert.equal(CE.setAmbient(n, 'day').fx.amb, undefined);
  assert.equal(CE.setAmbient(s, 'night', 30).fx.amb[3], 30);
  assert.equal(CE.setDither(s, 9).fx.dth, 3); assert.equal(CE.setLightDirection(s, 5).fx.ld, 1);
  assert.equal(validateScene(CE.setAmbientColor(s, [300, 0, 0], 150)).ok, true);
});

test('retoque: lápiz, borrador y cuentagotas', () => {
  const s = base();
  let t = paintPixels(s, strokePixels(0, 0, 3, 3), 2);
  const l = t.layers.at(-1);
  assert.equal(l.k, 'user'); assert.equal(l.o[0][3][1], '.2......'); assert.equal(validateScene(t).ok, true);
  assert.deepEqual([...renderScene(t).data.subarray(0, 4)], [0, 0, 255, 255]);
  assert.equal(pickIndex(t, 0, 0), 2); assert.equal(pickIndex(t, 7, 7), 1); assert.equal(pickIndex(t, 2, 2), 2); assert.equal(pickIndex(t, 99, 0), -1);
  t = paintPixels(t, [[0, 0], [50, 50]], -1);
  assert.equal(t.layers.at(-1).o[0][3][0], '........'); assert.equal(pickIndex(t, 0, 0), 1);
  assert.equal(paintPixels(s, [[0, 0]], -1), s, 'borrar sin capa de retoques no hace nada');
});

test('adaptador de IA: extrae, sanea y limita la respuesta del modelo', () => {
  assert.deepEqual(extractJSON('```json\n{"a":1}\n```'), { a: 1 }); assert.equal(extractJSON('sin json'), null); assert.equal(extractJSON('{roto'), null);
  const text = 'Aquí tienes: {"v":1,"w":9999,"h":3,"pal":[[0,0,0],[255,255,255]],"bg":0,"layers":[{"n":"x","o":[["R",0,0,4,4,1],["R",0,0,4,4,7],["FETCH","http://evil"]]}]}';
  const { scene, warnings } = sceneFromModelText(text, 32, 32);
  assert.equal(scene.w, 32); assert.equal(scene.h, 32, 'el tamaño lo impone la aplicación');
  assert.deepEqual(scene.layers[0].o, [['R', 0, 0, 4, 4, 1]]); assert.equal(warnings.length, 2);
  assert.equal(sceneFromModelText('Lo siento, no puedo.', 32, 32).scene, null);
});

test('adaptador de IA: valida la petición y no funciona sin clave', () => {
  assert.throws(() => parseRequest({}), /Falta el prompt/);
  assert.throws(() => parseRequest({ base: { v: 2 }, instruction: 'x' }), /no es válida/);
  const r = parseRequest({ prompt: 'x'.repeat(5000), w: 99999, h: -1, style: 'nope' });
  assert.equal(r.prompt.length, 600); assert.equal(r.w, 256); assert.equal(r.h, 8); assert.equal(r.styleName, '16-bit');
  assert.equal(parseRequest({ base: base(), instruction: 'más rojo' }).w, 8);
  const saved = [process.env.ANTHROPIC_API_KEY, process.env.ANTHROPIC_AUTH_TOKEN];
  delete process.env.ANTHROPIC_API_KEY; delete process.env.ANTHROPIC_AUTH_TOKEN;
  assert.equal(aiConfigured(), false);
  if (saved[0]) process.env.ANTHROPIC_API_KEY = saved[0]; if (saved[1]) process.env.ANTHROPIC_AUTH_TOKEN = saved[1];
});
