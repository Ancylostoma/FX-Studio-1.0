import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parsePrompt, emptyRaw, finalize, applyEdit } from '../src/generator/promptParser.js';
import { composeScene, editScene } from '../src/generator/sceneComposer.js';
import { validateScene, sceneStats } from '../src/core/pixelScene.js';
import { compactPalette } from '../src/core/paletteManager.js';
import { renderScene } from '../src/core/renderer.js';
import { LIB } from '../src/generator/spriteLibrary.js';
import { ANIMS } from '../src/generator/templates.js';
import { encodeGIF, buildPalette } from '../src/export/gifEncoder.js';
import { exportFrames, buildFrameStrip, animationToGif } from '../src/export/pngExporter.js';

const O = { w: 64, h: 64, style: 'bit16', seed: 7, detail: 2 };
const make = (prompt, o = {}) => composeScene(parsePrompt(prompt).spec, { ...O, ...o });
const same = (a, b) => a.data.length === b.data.length && a.data.every((v, i) => v === b.data[i]);
// GIF solo tiene transparencia de 1 bit: alfa < 128 se vuelve transparente y el resto, opaco.
const gifView = (img) => { const d = img.data.slice(); for (let i = 0; i < d.length; i += 4) { if (d[i + 3] < 128) d.fill(0, i, i + 4); else d[i + 3] = 255; } return { data: d }; };

// Decodificador GIF mínimo e independiente del codificador, solo para las pruebas.
function decodeGIF(buf) {
  let p = 6; const w = buf[p] | (buf[p + 1] << 8), h = buf[p + 2] | (buf[p + 3] << 8); const flags = buf[p + 4]; p += 7;
  const pal = buf.subarray(p, p + 3 * (1 << ((flags & 7) + 1))); p += pal.length;
  const frames = []; let gce = null, loop = false;
  for (;;) {
    const b = buf[p++];
    if (b === 0x3b) break;
    if (b === 0x21) { const label = buf[p++]; if (label === 0xf9) gce = { transparent: buf[p + 1] & 1, delay: buf[p + 2] | (buf[p + 3] << 8), disposal: (buf[p + 1] >> 2) & 7 }; if (label === 0xff) loop = true; while (buf[p]) p += buf[p] + 1; p++; continue; }
    assert.equal(b, 0x2c); p += 9;
    const min = buf[p++]; const bytes = [];
    while (buf[p]) { bytes.push(...buf.subarray(p + 1, p + 1 + buf[p])); p += buf[p] + 1; } p++;
    const clear = 1 << min, eoi = clear + 1; let size = min + 1, dict = [], prev = null, acc = 0, bits = 0, bi = 0; const out = [];
    const reset = () => { dict = []; for (let i = 0; i < clear; i++) dict[i] = [i]; dict[clear] = []; dict[eoi] = null; size = min + 1; prev = null; };
    reset();
    for (;;) {
      while (bits < size) { acc |= bytes[bi++] << bits; bits += 8; }
      const code = acc & ((1 << size) - 1); acc >>>= size; bits -= size;
      if (code === clear) { reset(); continue; }
      if (code === eoi) break;
      const entry = code < dict.length ? dict[code] : [...prev, prev[0]];
      out.push(...entry);
      if (prev) { dict.push([...prev, entry[0]]); if (dict.length === 1 << size && size < 12) size++; }
      prev = entry;
    }
    const data = new Uint8ClampedArray(w * h * 4);
    out.forEach((c, i) => { if (!(gce.transparent && c === 0)) { data.set(pal.subarray(c * 3, c * 3 + 3), i * 4); data[i * 4 + 3] = 255; } });
    frames.push({ w, h, data, ...gce });
  }
  return { w, h, frames, loop };
}

test('formato: an, fo y mv se validan', () => {
  const s = { v: 1, w: 8, h: 8, pal: [[0, 0, 0], [255, 0, 0]], bg: -1, an: { n: 2, fps: 4 }, layers: [{ o: [['R', 0, 0, 1, 1, 1]], fo: [[['R', 3, 0, 1, 1, 1]]] }, { o: [['R', 0, 4, 1, 1, 1]], mv: [[0, 0], [2, 1]] }] };
  assert.deepEqual(validateScene(s).errors, []);
  const px = (f, x, y) => renderScene(s, { frame: f }).data[(y * 8 + x) * 4 + 3];
  assert.deepEqual([px(0, 0, 0), px(0, 3, 0), px(1, 0, 0), px(1, 3, 0)], [255, 0, 0, 255], 'fo sustituye a o por fotograma');
  assert.deepEqual([px(0, 0, 4), px(1, 0, 4), px(1, 2, 5)], [255, 0, 255], 'mv desplaza la capa');
  assert.equal(px(2, 0, 0), 255, 'los fotogramas se repiten en ciclo');
  assert.equal(sceneStats(s).frames, 2); assert.equal(sceneStats(s).ops, 3);
  for (const bad of [{ an: { n: 1, fps: 4 } }, { an: { n: 99, fps: 4 } }, { an: { n: 2, fps: 0 } }]) assert.equal(validateScene({ ...s, ...bad }).ok, false);
  const l = (x) => validateScene({ ...s, layers: [{ o: [], ...x }] }).ok;
  assert.equal(l({ fo: [] }), false); assert.equal(l({ fo: [[['R', 0, 0, 1, 1, 9]]] }), false); assert.equal(l({ mv: [[0, 0], [999, 0]] }), false); assert.equal(l({ mv: 'x' }), false);
  const c = compactPalette({ ...s, pal: [[9, 9, 9], [0, 0, 0], [255, 0, 0]], layers: [{ o: [['R', 0, 0, 1, 1, 2]], fo: [[['R', 1, 0, 1, 1, 2]]] }] });
  assert.equal(c.layers[0].fo[0][0][5], 0, 'la paleta se reindexa también en los fotogramas');
});

test('sin animación la salida es idéntica a la de siempre', () => {
  const p = 'Un dragón rojo volando sobre un castillo de noche con lluvia';
  assert.equal(JSON.stringify(make(p)), JSON.stringify(make(p, { anim: 'none' })));
  assert.equal(make(p).an, undefined); assert.ok(make(p).layers.every((l) => !l.fo && !l.mv));
});

test('escena animada: válida, compacta y solo duplica lo que se mueve', () => {
  const p = 'Un dragón rojo volando sobre un castillo de noche con lluvia';
  const st = make(p), an = make(p, { anim: 'idle' });
  assert.deepEqual(validateScene(an).errors, []); assert.deepEqual(an.an, { n: 4, fps: 4 });
  const moving = an.layers.filter((l) => l.fo || l.mv).map((l) => l.n);
  for (const n of ['centelleo', 'dragón', 'lluvia']) assert.ok(moving.includes(n), n);
  for (const n of ['cielo', 'estrellas', 'suelo', 'castillo', 'luna']) assert.ok(!moving.includes(n), n + ' es estático');
  assert.ok(JSON.stringify(an).length < JSON.stringify(st).length * 1.6, 'cuatro fotogramas cuestan menos del 60 % extra');
  const fr = exportFrames(an);
  assert.equal(fr.length, 4); assert.ok(!same(fr[0], fr[1]) && !same(fr[1], fr[2]));
  assert.equal(JSON.stringify(an), JSON.stringify(make(p, { anim: 'idle' })), 'determinista');
});

test('ambiente: lluvia, nieve, agua y luciérnagas se animan', () => {
  for (const [p, layer] of [['castillo con lluvia', 'lluvia'], ['pinos nevando', 'nevada'], ['un velero en el mar', 'mar'], ['bosque con luciérnagas', 'luciérnagas'], ['pradera con tormenta', 'relámpago']]) {
    const s = make(p, { anim: 'idle' });
    assert.ok(s.layers.find((l) => l.n === layer)?.fo, `${p}: ${layer}`);
    assert.equal(validateScene(s).ok, true);
  }
  const boat = make('un velero en el mar', { anim: 'idle' }).layers.find((l) => l.n === 'barco');
  assert.ok(boat.mv, 'lo que flota se mece');
});

test('todos los sprites admiten todas las animaciones', () => {
  const moves = { knight: ['walk', 'attack', 'idle'], horse: ['walk'], dragon: ['idle'], bird: ['idle'], bat: ['idle'], butterfly: ['idle'], slime: ['idle'], wizard: ['attack'], rocket: ['idle'], campfire: ['idle'] };
  for (const id of Object.keys(LIB)) for (const anim of Object.keys(ANIMS)) {
    const raw = emptyRaw(); raw.subjects.push({ id, color: null, color2: null, n: 1, size: 0, fly: null, a: { weaponFx: 'fire' } });
    const s = composeScene(finalize(raw), { w: 48, h: 48, style: 'sprite', seed: 1, detail: 2, anim });
    assert.deepEqual(validateScene(s).errors, [], `${id}/${anim}`);
    const fr = exportFrames(s);
    if (moves[id]?.includes(anim)) assert.ok(fr.length === 4 && fr.some((f) => !same(f, fr[0])), `${id} se mueve en ${anim}`);
  }
});

test('el prompt puede sugerir la animación', () => {
  assert.equal(parsePrompt('un caballero caminando por el bosque').raw.anim, 'walk');
  assert.equal(parsePrompt('a knight attacking a dragon').raw.anim, 'attack');
  assert.equal(parsePrompt('un caballero').raw.anim, null);
});

test('la edición parcial conserva la animación', () => {
  const o = { ...O, anim: 'idle' };
  const { raw, spec } = parsePrompt('Un dragón volando sobre un castillo de noche');
  const { scene, changed } = editScene(composeScene(spec, o), raw, applyEdit(raw, 'agrega lluvia').raw, o);
  assert.equal(validateScene(scene).ok, true); assert.deepEqual(scene.an, { n: 4, fps: 4 });
  assert.ok(changed.includes('weather') && !changed.includes('subj:dragon'));
  assert.ok(scene.layers.find((l) => l.n === 'dragón').fo && scene.layers.find((l) => l.n === 'lluvia').fo);
});

test('GIF: cabecera, bucle, tiempos y píxeles idénticos al original', () => {
  const s = make('un caballero con espada de fuego', { w: 48, h: 48, style: 'sprite', anim: 'attack' });
  const src = exportFrames(s), gif = animationToGif(s);
  assert.equal(String.fromCharCode(...gif.subarray(0, 6)), 'GIF89a'); assert.equal(gif.at(-1), 0x3b);
  const dec = decodeGIF(gif);
  assert.equal(dec.w, 48); assert.equal(dec.frames.length, 4); assert.equal(dec.loop, true);
  assert.equal(dec.frames[0].delay, Math.round(100 / s.an.fps)); assert.equal(dec.frames[0].disposal, 2); assert.equal(dec.frames[0].transparent, 1);
  dec.frames.forEach((f, i) => assert.ok(same(f, gifView(src[i])), 'fotograma ' + i));
  const big = decodeGIF(animationToGif(s, { scale: 3 }));
  assert.equal(big.w, 144);
  assert.deepEqual([...big.frames[2].data.subarray((70 * 144 + 70) * 4, (70 * 144 + 70) * 4 + 4)], [...src[2].data.subarray((23 * 48 + 23) * 4, (23 * 48 + 23) * 4 + 4)]);
});

test('GIF: escenas grandes (reinicio del diccionario LZW) y más de 255 colores', () => {
  const s = make('bosque encantado con casa, luciérnagas y nieve', { w: 256, h: 256, style: 'detailed', detail: 4, anim: 'idle' });
  const src = exportFrames(s), dec = decodeGIF(encodeGIF(src, { fps: 4 }));
  assert.ok(buildPalette(src).mask === 0xff, 'la escena cabe en 255 colores sin pérdida');
  dec.frames.forEach((f, i) => assert.ok(same(f, gifView(src[i])), 'fotograma ' + i));
  const noisy = { w: 64, h: 64, data: new Uint8ClampedArray(64 * 64 * 4) };
  for (let i = 0; i < 4096; i++) noisy.data.set([i & 255, (i >> 4) & 255, (i * 7) & 255, 255], i * 4);
  assert.ok(buildPalette([noisy]).colors.length <= 255);
  assert.equal(decodeGIF(encodeGIF([noisy])).frames.length, 1);
  assert.throws(() => encodeGIF([]), /Sin fotogramas/);
  assert.throws(() => encodeGIF([noisy, { w: 8, h: 8, data: new Uint8ClampedArray(256) }]), /inválido/);
});

test('tira de fotogramas', () => {
  const fr = exportFrames(make('un caballo', { w: 32, h: 32, style: 'sprite', anim: 'walk' })), strip = buildFrameStrip(fr);
  assert.equal(strip.w, 128); assert.equal(strip.h, 32);
  for (let n = 0; n < 4; n++) assert.deepEqual([...strip.data.subarray((20 * 128 + n * 32 + 16) * 4, (20 * 128 + n * 32 + 16) * 4 + 4)], [...fr[n].data.subarray((20 * 32 + 16) * 4, (20 * 32 + 16) * 4 + 4)]);
  assert.equal(exportFrames(make('un caballo')).length, 1);
});
