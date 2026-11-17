// Tira de fotogramas para revisar animaciones: node tools/animsheet.mjs salida.png res "prompt|estilo|anim" ...
import { writeFileSync } from 'node:fs';
import { parsePrompt } from '../src/generator/promptParser.js';
import { composeScene } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { validateScene } from '../src/core/pixelScene.js';
import { scaleNearest } from '../src/core/geometry.js';
import { encodePNG } from './png.mjs';

const [out, res, ...items] = process.argv.slice(2);
const cell = 200, sheet = { w: 4 * cell, h: items.length * cell, data: new Uint8ClampedArray(4 * cell * items.length * cell * 4) };
for (let i = 0; i < sheet.data.length; i += 4) { sheet.data[i] = 70; sheet.data[i + 1] = 80; sheet.data[i + 2] = 110; sheet.data[i + 3] = 255; }
items.forEach((it, row) => {
  const [prompt, style = 'sprite', anim = 'walk'] = it.split('|');
  const scene = composeScene(parsePrompt(prompt).spec, { w: +res, h: +res, style, seed: 3, detail: 2, anim });
  const v = validateScene(scene); if (!v.ok) throw new Error(v.errors.join());
  for (let f = 0; f < 4; f++) {
    const img = renderScene(scene, { frame: f }), k = Math.floor((cell - 8) / +res), big = scaleNearest(img.data, img.w, img.h, k);
    for (let y = 0; y < big.h; y++) for (let x = 0; x < big.w; x++) { const s = (y * big.w + x) * 4; if (big.data[s + 3]) sheet.data.set(big.data.subarray(s, s + 4), ((row * cell + 4 + y) * sheet.w + f * cell + 4 + x) * 4); }
  }
  console.log(prompt, anim, JSON.stringify(scene.an), JSON.stringify(scene).length + 'B');
});
writeFileSync(out, encodePNG(sheet));
