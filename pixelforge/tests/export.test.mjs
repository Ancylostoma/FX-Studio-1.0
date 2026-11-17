import { test } from 'node:test';
import assert from 'node:assert/strict';
import { inflateSync } from 'node:zlib';
import { parsePrompt } from '../src/generator/promptParser.js';
import { composeScene } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { clampScale, MAX_EXPORT } from '../src/core/pixelEngine.js';
import { scaleForTarget, exportImage, collectSprites, buildSpriteSheet, cropImage } from '../src/export/pngExporter.js';
import { sceneToJSON, serializeProject, parseProjectFile, safeFileName, PROJECT_FORMAT } from '../src/export/projectExporter.js';
import { encodePNG } from '../tools/png.mjs';

const scene = composeScene(parsePrompt('Un caballero frente a un castillo de noche').spec, { w: 64, h: 64, style: 'bit16', seed: 5, detail: 2 });

test('factor de escala entero y acotado', () => {
  assert.equal(scaleForTarget(scene, 1024), 16);
  assert.equal(scaleForTarget(scene, 1000), 15);
  assert.equal(scaleForTarget(scene, 10), 1);
  assert.equal(scaleForTarget(scene, 99999), MAX_EXPORT / 64);
  assert.equal(clampScale({ w: 256, h: 256 }, 999), 16);
  assert.equal(clampScale(scene, 2.9), 2); assert.equal(clampScale(scene, NaN), 1);
});

test('64×64 → 1024×1024 conserva exactamente el conjunto de colores', () => {
  const img = renderScene(scene), big = scaleNearest(img.data, 64, 64, 16);
  assert.equal(big.w, 1024); assert.equal(big.h, 1024);
  const colors = (d) => { const s = new Set(); for (let i = 0; i < d.length; i += 4) s.add((d[i] << 16) | (d[i + 1] << 8) | d[i + 2]); return s; };
  assert.deepEqual(colors(big.data), colors(img.data));
});

test('PNG: firma, cabecera y píxeles recuperables', () => {
  const img = renderScene(scene), png = encodePNG(scaleNearest(img.data, 64, 64, 2));
  assert.deepEqual([...png.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
  assert.equal(png.readUInt32BE(16), 128); assert.equal(png.readUInt32BE(20), 128); assert.equal(png[25], 6);
  const idat = png.subarray(png.indexOf('IDAT') + 4, png.indexOf('IEND') - 8);
  const raw = inflateSync(idat);
  assert.equal(raw.length, (128 * 4 + 1) * 128);
  assert.deepEqual([...raw.subarray(1, 5)], [...img.data.subarray(0, 4)]);
});

test('exportación transparente: sin fondo ni cielo', () => {
  const opaque = exportImage(scene), clear = exportImage(scene, { transparent: true });
  assert.equal(opaque.data[3], 255); assert.equal(clear.data[3], 0);
  let n = 0; for (let i = 3; i < clear.data.length; i += 4) if (clear.data[i]) n++;
  assert.ok(n > 500, 'el resto de la escena sigue presente');
});

test('sprites individuales y hoja de sprites', () => {
  const sprites = collectSprites(scene);
  assert.ok(sprites.some((s) => s.name === 'subj-knight') && sprites.some((s) => s.name === 'struct-castle'));
  const k = sprites.find((s) => s.name === 'subj-knight').img;
  assert.ok(k.w < 40 && k.h < 40 && k.w > 4);
  const sheet = buildSpriteSheet(sprites);
  assert.equal(sheet.frames.length, sprites.length);
  const f = sheet.frames[0];
  assert.deepEqual([...cropImage(sheet.img, f).data], [...sprites[0].img.data]);
  assert.equal(buildSpriteSheet([]), null);
});

test('proyecto: ida y vuelta, y rechazo de archivos inválidos', () => {
  const p = { name: 'Castillo', prompt: 'x', opts: { res: 64, aspect: '1:1', style: 'bit16', palette: 'auto', detail: 2, seed: 5 }, raw: { a: 1 }, scene };
  const back = parseProjectFile(serializeProject(p));
  assert.equal(back.ok, true); assert.equal(back.project.name, 'Castillo'); assert.deepEqual(back.project.scene, JSON.parse(sceneToJSON(scene)));
  assert.equal(JSON.parse(serializeProject(p)).format, PROJECT_FORMAT);
  assert.equal(parseProjectFile(sceneToJSON(scene)).ok, true, 'también acepta un PixelScene suelto');
  assert.equal(parseProjectFile('{').ok, false);
  assert.equal(parseProjectFile(JSON.stringify({ format: PROJECT_FORMAT, scene: { v: 1 } })).ok, false);
  assert.ok(!sceneToJSON(scene).includes('\n') && !sceneToJSON(scene).includes(': '));
});

test('nombres de archivo seguros', () => {
  assert.equal(safeFileName('Un dragón / rojo..', 'png'), 'Un-dragon-rojo.png');
  assert.equal(safeFileName('../../etc/passwd', 'json'), 'etc-passwd.json');
  assert.equal(safeFileName('', 'png'), 'pixelforge.png');
});
