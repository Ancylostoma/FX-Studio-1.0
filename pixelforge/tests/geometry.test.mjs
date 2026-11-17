import { test } from 'node:test';
import assert from 'node:assert/strict';
import { rasterRect, rasterLine, rasterEllipse, rasterPolygon, scaleNearest, opaqueBounds } from '../src/core/geometry.js';
import { bayer, ditherBand } from '../src/core/dithering.js';
import { renderScene } from '../src/core/renderer.js';
import { RNG, hash2 } from '../src/core/randomSeed.js';

const collect = (fn) => { const s = new Set(); fn((x, y) => s.add(x + ',' + y)); return s; };

test('rectángulo: cubre exactamente w×h', () => {
  const s = collect((p) => rasterRect(2, 3, 4, 2, p));
  assert.equal(s.size, 8); assert.ok(s.has('2,3') && s.has('5,4') && !s.has('6,4'));
});

test('línea: extremos incluidos, conexa y simétrica', () => {
  const a = collect((p) => rasterLine(0, 0, 7, 3, p)), b = collect((p) => rasterLine(7, 3, 0, 0, p));
  assert.ok(a.has('0,0') && a.has('7,3')); assert.equal(a.size, 8);
  assert.equal(b.size, 8);
  assert.equal(collect((p) => rasterLine(4, 4, 4, 4, p)).size, 1);
});

test('elipse: simétrica, sin píxeles sueltos en los extremos', () => {
  const s = collect((p) => rasterEllipse(10, 10, 4, 4, p));
  for (const k of s) { const [x, y] = k.split(',').map(Number); assert.ok(s.has(20 - x + ',' + y) && s.has(x + ',' + (20 - y))); }
  assert.ok(s.has('10,6') && s.has('9,6') && s.has('11,6'), 'el borde superior tiene más de un píxel');
  assert.ok(!s.has('15,10') && s.has('14,10'));
  assert.equal(collect((p) => rasterEllipse(3, 3, 0, 0, p)).size, 1);
});

test('polígono: triángulo relleno, aristas incluidas', () => {
  const s = collect((p) => rasterPolygon([0, 0, 8, 0, 0, 8], p));
  assert.ok(s.has('0,0') && s.has('8,0') && s.has('0,8') && s.has('2,2') && !s.has('7,7'));
  assert.equal(collect((p) => rasterPolygon([1, 1, 5, 1], p)).size, 5);
});

test('escalado por vecino más cercano: bloques idénticos, sin colores nuevos', () => {
  const src = new Uint8ClampedArray([255, 0, 0, 255, 0, 0, 255, 255, 0, 255, 0, 255, 0, 0, 0, 0]);
  const out = scaleNearest(src, 2, 2, 3);
  assert.equal(out.w, 6); assert.equal(out.h, 6);
  for (let y = 0; y < 6; y++) for (let x = 0; x < 6; x++) {
    const s = ((y / 3 | 0) * 2 + (x / 3 | 0)) * 4, d = (y * 6 + x) * 4;
    assert.deepEqual([...out.data.subarray(d, d + 4)], [...src.subarray(s, s + 4)]);
  }
});

test('opaqueBounds', () => {
  const d = new Uint8ClampedArray(4 * 4 * 4); d[(1 * 4 + 2) * 4 + 3] = 255; d[(2 * 4 + 3) * 4 + 3] = 9;
  assert.deepEqual(opaqueBounds(d, 4, 4), { x: 2, y: 1, w: 2, h: 2 });
  assert.equal(opaqueBounds(new Uint8ClampedArray(16), 2, 2), null);
});

test('dithering ordenado: determinista y dentro de rango', () => {
  const seen = new Set(); for (let y = 0; y < 4; y++) for (let x = 0; x < 4; x++) seen.add(bayer(x, y));
  assert.equal(seen.size, 16);
  assert.equal(ditherBand(0.2, 3, 0, 0, 0), 0); assert.equal(ditherBand(0.6, 3, 0, 0, 0), 1); assert.equal(ditherBand(5, 3, 1, 1, 3), 2);
});

test('semillas reproducibles', () => {
  const a = new RNG(42), b = new RNG(42), c = new RNG(43);
  const seq = (r) => Array.from({ length: 5 }, () => r.next());
  const sa = seq(a); assert.deepEqual(sa, seq(b)); assert.notDeepEqual(sa, seq(c));
  assert.equal(new RNG(1).fork('x').next(), new RNG(1).fork('x').next());
  assert.equal(hash2(3, 4, 5), hash2(3, 4, 5));
});

test('renderizador: paleta por índices, capas, opacidad y transparencia', () => {
  const s = { v: 1, w: 8, h: 8, pal: [[10, 20, 30], [200, 100, 50], [0, 255, 0]], bg: -1, layers: [{ o: [['R', 0, 0, 4, 8, 1]] }, { a: 0.5, o: [['R', 0, 0, 8, 1, 2]] }, { v: 0, o: [['R', 0, 0, 8, 8, 0]] }] };
  const px = (img, x, y) => [...img.data.subarray((y * 8 + x) * 4, (y * 8 + x) * 4 + 4)];
  const img = renderScene(s);
  assert.deepEqual(px(img, 1, 3), [200, 100, 50, 255]);
  assert.deepEqual(px(img, 6, 3), [0, 0, 0, 0]);
  assert.deepEqual(px(img, 1, 0), [100, 178, 25, 255]);
  assert.equal(px(img, 6, 0)[3], 128);
  assert.deepEqual(px(renderScene({ ...s, bg: 0 }), 6, 3), [10, 20, 30, 255]);
  // Cambiar un color de la paleta cambia todos los píxeles que lo referencian.
  s.pal[1] = [1, 2, 3];
  assert.deepEqual(px(renderScene(s), 1, 3), [1, 2, 3, 255]);
});

test('renderizador: luz ambiental, cuantización y sombreado por rampa', () => {
  const s = { v: 1, w: 8, h: 8, pal: [[50, 50, 50], [100, 100, 100], [200, 200, 200]], bg: -1, rmp: [[0, 1, 2]], fx: { amb: [0, 0, 255, 100] }, layers: [{ o: [['R', 0, 0, 8, 8, 1]] }, { nl: 1, o: [['R', 0, 0, 1, 1, 1]] }] };
  let img = renderScene(s);
  assert.deepEqual([...img.data.subarray(4, 8)], [0, 0, 100, 255]);
  assert.deepEqual([...img.data.subarray(0, 4)], [100, 100, 100, 255], 'capa emisiva sin tinte');
  img = renderScene({ ...s, fx: {}, q: [[0, 0, 0], [255, 255, 255]] });
  assert.deepEqual([...img.data.subarray(4, 8)], [0, 0, 0, 255]);
  const sh = renderScene({ ...s, fx: { ld: -1 }, layers: [{ sh: 1, o: [['R', 2, 2, 4, 4, 1]] }] });
  const at = (x, y) => sh.data[(y * 8 + x) * 4];
  assert.equal(at(2, 2), 200); assert.equal(at(5, 5), 50); assert.equal(at(3, 3), 100);
});
