import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parsePrompt, finalize, emptyRaw, applyEdit } from '../src/generator/promptParser.js';
import { composeScene, editScene, dimensions, RESOLUTIONS, ASPECTS } from '../src/generator/sceneComposer.js';
import { validateScene, layerSignature, LIMITS } from '../src/core/pixelScene.js';
import { renderScene } from '../src/core/renderer.js';
import { LIB } from '../src/generator/spriteLibrary.js';
import { STYLES, BIOMES } from '../src/generator/templates.js';
import { EXAMPLES } from '../tools/render-examples.mjs';

const make = (prompt, o = {}) => composeScene(parsePrompt(prompt).spec, { w: 64, h: 64, style: 'bit16', seed: 7, detail: 2, ...o });
const P = 'Un dragón rojo volando sobre un castillo medieval durante la noche';

test('dimensiones por resolución y proporción', () => {
  assert.deepEqual(dimensions(64, '1:1'), { w: 64, h: 64 });
  assert.deepEqual(dimensions(64, '16:9'), { w: 64, h: 36 });
  assert.deepEqual(dimensions(256, '9:16'), { w: 144, h: 256 });
  for (const r of RESOLUTIONS) for (const a of Object.keys(ASPECTS)) { const d = dimensions(r, a); assert.ok(d.w >= LIMITS.minDim && d.h >= LIMITS.minDim && Math.max(d.w, d.h) <= 256); }
});

test('toda escena generada es un PixelScene válido y compacto', () => {
  for (const [, prompt, style, seed, aspect, res] of EXAMPLES) {
    const s = composeScene(parsePrompt(prompt).spec, { ...dimensions(res, aspect), style, seed, detail: 2 });
    assert.deepEqual(validateScene(s).errors, [], prompt);
    assert.ok(JSON.stringify(s).length < 16000, prompt + ': ' + JSON.stringify(s).length);
    const img = renderScene(s);
    assert.equal(img.data.length, s.w * s.h * 4);
  }
});

test('determinismo: misma semilla → misma escena; otra semilla → otra', () => {
  assert.equal(JSON.stringify(make(P)), JSON.stringify(make(P)));
  assert.notEqual(JSON.stringify(make(P)), JSON.stringify(make(P, { seed: 8 })));
});

test('la escena contiene las capas esperadas y usa sprites reutilizables', () => {
  const s = make(P, { w: 128, h: 128 });
  const keys = new Set(s.layers.map((l) => l.k));
  for (const k of ['sky', 'stars', 'celestial', 'ground', 'struct:castle', 'subj:dragon']) assert.ok(keys.has(k), k);
  assert.ok(Object.keys(s.sprites).length > 0);
  assert.ok(s.layers.some((l) => l.o.some((op) => op[0] === 'S')));
  assert.ok(s.fx.amb, 'la noche se expresa como luz ambiental');
});

test('todas las combinaciones de estilo, resolución y bioma renderizan', () => {
  for (const style of Object.keys(STYLES)) for (const res of [16, 48, 256]) {
    const s = make(P, { ...dimensions(res, res === 48 ? '16:9' : '1:1'), style, detail: res === 256 ? 4 : 1 });
    assert.equal(validateScene(s).ok, true, `${style}@${res}`);
    renderScene(s);
  }
  for (const biome of Object.keys(BIOMES)) {
    const raw = emptyRaw(); raw.biome = biome; raw.weather = ['rain', 'snow', 'fog', 'storm']; raw.celestial = ['aurora', 'rainbow', 'comet', 'planet']; raw.fx = ['fireflies', 'sparkles']; raw.features = ['lake', 'mountains'];
    renderScene(composeScene(finalize(raw), { w: 96, h: 54, style: 'detailed', seed: 3, detail: 3 }));
  }
});

test('todos los sprites de la biblioteca se construyen a cualquier tamaño', () => {
  for (const id of Object.keys(LIB)) for (const res of [16, 64]) {
    const raw = emptyRaw(); raw.subjects.push({ id, color: 'blue', color2: 'gold', n: 1, size: 0, fly: null, a: { weaponFx: 'fire', fire: true } });
    const s = composeScene(finalize(raw), { w: res, h: res, style: 'sprite', seed: 1, detail: 2 });
    assert.equal(s.bg, -1, 'los sprites tienen fondo transparente');
    const img = renderScene(s);
    let opaque = 0; for (let i = 3; i < img.data.length; i += 4) if (img.data[i]) opaque++;
    assert.ok(opaque > res, `${id}@${res} dibuja algo`);
  }
});

test('edición parcial: solo cambian las partes afectadas', () => {
  const o = { w: 96, h: 96, style: 'bit16', seed: 7, detail: 2 };
  const { raw, spec } = parsePrompt('Un castillo en una pradera de día');
  const scene = composeScene(spec, o);
  // El usuario oculta a mano una capa: debe sobrevivir a la edición.
  const gi = scene.layers.findIndex((l) => l.k === 'ground'); scene.layers[gi].v = 0;
  const r2 = applyEdit(raw, 'Conserva el castillo, cambia el cielo a púrpura y agrega una luna').raw;
  const { scene: out, changed, rebuilt } = editScene(scene, raw, r2, o);
  assert.equal(rebuilt, false); assert.equal(validateScene(out).ok, true);
  assert.ok(changed.includes('sky') && changed.includes('celestial'));
  assert.ok(!changed.includes('struct:castle') && !changed.includes('ground') && !changed.includes('props'));
  const sig = (S, k) => S.layers.filter((l) => l.k === k).map((l) => layerSignature(S, l)).join();
  assert.equal(sig(out, 'struct:castle'), sig(scene, 'struct:castle'));
  assert.notEqual(sig(out, 'sky'), sig(scene, 'sky'));
  assert.equal(out.layers.find((l) => l.k === 'ground').v, 0);
  assert.ok(out.layers.some((l) => l.n === 'luna'));
});

test('lo que vuela no tapa a otros personajes; de noche lleva borde de luna (5.ª prueba real)', async () => {
  const { composeSceneEx } = await import('../src/generator/sceneComposer.js');
  const prompt = 'Un enorme dragón rojo volando sobre un castillo medieval durante una noche de luna llena, mientras un mago lanza un hechizo desde la torre y unos murciélagos huyen.';
  for (const seed of [1, 7, 42, 99]) {
    const { G, scene } = composeSceneEx(parsePrompt(prompt).spec, { w: 128, h: 128, style: 'bit16', seed, detail: 3 });
    const box = (id) => G.subs.find((b) => b.e.id === id);
    const d = box('dragon'), wz = box('wizard');
    const ox = Math.max(0, Math.min(d.x + d.w, wz.x + wz.w) - Math.max(d.x, wz.x)), oy = Math.max(0, Math.min(d.y + d.h, wz.y + wz.h) - Math.max(d.y, wz.y));
    assert.ok((ox * oy) / (wz.w * wz.h) < 0.15, `semilla ${seed}: el dragón tapa ${Math.round((100 * ox * oy) / (wz.w * wz.h))} % del mago`);
    assert.ok(scene.layers.filter((l) => l.k?.startsWith('subj:bat') && l.ol !== undefined).every((l) => l.ol >= 0), 'murciélagos con borde claro');
  }
  // Un jinete sobre un dragón no se separa de su montura.
  const { G } = composeSceneEx(parsePrompt('Un caballero montado en un dragón volando de noche').spec, { w: 128, h: 128, style: 'bit16', seed: 7, detail: 3 });
  const k = G.subs.find((b) => b.e.id === 'knight'), dr = G.subs.find((b) => b.e.id === 'dragon');
  assert.ok(Math.abs(k.x + k.w / 2 - (dr.x + dr.w / 2)) < dr.w * 0.6, 'el jinete sigue encima del dragón');
});
