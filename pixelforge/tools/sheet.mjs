// Hoja de contacto: node tools/sheet.mjs salida.png res "prompt|estilo|semilla|aspecto" ...
import { writeFileSync } from 'node:fs';
import { parsePrompt } from '../src/generator/promptParser.js';
import { composeScene, dimensions } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { encodePNG } from './png.mjs';

const [out, res, ...items] = process.argv.slice(2);
const cell = 384, cols = Math.min(3, items.length), rows = Math.ceil(items.length / cols);
const sheet = { w: cols * cell, h: rows * cell, data: new Uint8ClampedArray(cols * cell * rows * cell * 4).fill(40) };
items.forEach((it, n) => {
  const [prompt, style = 'bit16', seed = '7', aspect = '1:1', r = res] = it.split('|');
  const t0 = performance.now();
  const { spec } = parsePrompt(prompt);
  const scene = composeScene(spec, { ...dimensions(+r, aspect), style, seed: +seed, detail: 2 });
  const img = renderScene(scene);
  const k = Math.max(1, Math.floor((cell - 8) / Math.max(img.w, img.h)));
  const big = scaleNearest(img.data, img.w, img.h, k);
  const ox = (n % cols) * cell + ((cell - big.w) >> 1), oy = Math.floor(n / cols) * cell + ((cell - big.h) >> 1);
  for (let y = 0; y < big.h; y++) sheet.data.set(big.data.subarray(y * big.w * 4, (y + 1) * big.w * 4), ((oy + y) * sheet.w + ox) * 4);
  console.log(n, prompt.slice(0, 50), `${(performance.now() - t0).toFixed(0)}ms`, JSON.stringify(scene).length + 'B', spec.notes.join(' '));
});
for (let i = 3; i < sheet.data.length; i += 4) sheet.data[i] = 255;
writeFileSync(out, encodePNG(sheet));
