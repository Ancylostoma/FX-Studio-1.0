// Uso: node tools/render.mjs "prompt" salida.png [res] [estilo] [semilla] [aspecto] [escala]
import { writeFileSync } from 'node:fs';
import { parsePrompt, describeSpec } from '../src/generator/promptParser.js';
import { composeScene, dimensions } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { sceneStats } from '../src/core/pixelScene.js';
import { encodePNG } from './png.mjs';

const [prompt, out = 'out.png', res = '64', style = 'bit16', seed = '7', aspect = '1:1', scale] = process.argv.slice(2);
const { spec } = parsePrompt(prompt);
const scene = composeScene(spec, { ...dimensions(+res, aspect), style, seed: +seed, detail: 2 });
const img = renderScene(scene);
const k = +(scale || Math.max(1, Math.floor(512 / Math.max(scene.w, scene.h))));
writeFileSync(out, encodePNG(scaleNearest(img.data, img.w, img.h, k)));
console.log(describeSpec(spec, { style, seed }), JSON.stringify(sceneStats(scene)), spec.notes.join(' '));
