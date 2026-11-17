// Hoja con todos los sprites de la biblioteca: node tools/libsheet.mjs salida.png [res] [estilo] [desde] [hasta]
import { writeFileSync } from 'node:fs';
import { emptyRaw, finalize } from '../src/generator/promptParser.js';
import { composeScene } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { LIB } from '../src/generator/spriteLibrary.js';
import { encodePNG } from './png.mjs';

const [out, res = '48', style = 'sprite', from = '0', to = '999'] = process.argv.slice(2);
const ids = Object.keys(LIB).slice(+from, +to);
const cell = 160, cols = 8, rows = Math.ceil(ids.length / cols);
const sheet = { w: cols * cell, h: rows * cell, data: new Uint8ClampedArray(cols * cell * rows * cell * 4) };
for (let i = 0; i < sheet.data.length; i += 4) { sheet.data[i] = 70; sheet.data[i + 1] = 80; sheet.data[i + 2] = 110; sheet.data[i + 3] = 255; }
ids.forEach((id, n) => {
  const raw = emptyRaw();
  raw.subjects.push({ id, color: null, color2: null, n: 1, size: 0, fly: null, a: {} });
  const img = renderScene(composeScene(finalize(raw), { w: +res, h: +res, style, seed: 3, detail: 2 }));
  const k = Math.floor((cell - 8) / +res), big = scaleNearest(img.data, img.w, img.h, k);
  const ox = (n % cols) * cell + 4, oy = Math.floor(n / cols) * cell + 4;
  for (let y = 0; y < big.h; y++) for (let x = 0; x < big.w; x++) { const s = (y * big.w + x) * 4; if (big.data[s + 3]) sheet.data.set(big.data.subarray(s, s + 4), ((oy + y) * sheet.w + ox + x) * 4); }
});
writeFileSync(out, encodePNG(sheet));
console.log(ids.join(' '));
