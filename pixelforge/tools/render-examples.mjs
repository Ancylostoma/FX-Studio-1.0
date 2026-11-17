// Genera los ejemplos de docs/examples y los iconos de la PWA con el propio
// motor:  node tools/render-examples.mjs
import { writeFileSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parsePrompt } from '../src/generator/promptParser.js';
import { composeScene, dimensions } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { encodePNG } from './png.mjs';
import { animationToGif } from '../src/export/pngExporter.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'docs', 'examples');
mkdirSync(out, { recursive: true });
mkdirSync(path.join(root, 'icons'), { recursive: true });

export const EXAMPLES = [
  ['dragon-castillo', 'Un dragón rojo volando sobre un castillo medieval durante la noche', 'bit16', 7, '1:1', 128],
  ['cyberpunk', 'Una ciudad futurista cyberpunk bajo la lluvia', 'cyber', 7, '16:9', 128],
  ['bosque-encantado', 'Un bosque encantado con una pequeña casa y luciérnagas', 'fantasy', 7, '1:1', 128],
  ['astronauta-marte', 'Un astronauta caminando sobre Marte', 'bit16', 7, '4:3', 128],
  ['paisaje-japones', 'Un paisaje japonés con montañas, un lago y un árbol de cerezo', 'landscape', 7, '3:2', 128],
  ['personaje-rpg', 'Un personaje RPG con armadura dorada y una espada de fuego', 'rpg', 7, '1:1', 48],
  ['velero-atardecer', 'Un velero en el mar al atardecer', 'modern', 3, '16:9', 128],
  ['cabana-nevada', 'Una cabaña en un paisaje nevado con pinos, nevando', 'detailed', 4, '1:1', 96],
  ['isla-isometrica', 'Castillo con un caballero y un dragón', 'iso', 6, '1:1', 96],
  ['cohete-espacio', 'Un cohete en el espacio con planetas', 'bit16', 1, '9:16', 96],
  ['sprite-mago', 'Un mago', 'sprite', 2, '1:1', 32],
  ['sprite-cofre', 'Un cofre', 'sprite', 2, '1:1', 16],
];

const draw = (prompt, style, seed, aspect, res, target) => {
  const scene = composeScene(parsePrompt(prompt).spec, { ...dimensions(res, aspect), style, seed, detail: 2 });
  const img = renderScene(scene);
  return { scene, png: encodePNG(scaleNearest(img.data, img.w, img.h, Math.max(1, Math.floor(target / Math.max(img.w, img.h))))) };
};

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  for (const [name, prompt, style, seed, aspect, res] of EXAMPLES) {
    const { scene, png } = draw(prompt, style, seed, aspect, res, 512);
    writeFileSync(path.join(out, name + '.png'), png);
    console.log(name.padEnd(20), `${scene.w}×${scene.h}`, `${JSON.stringify(scene).length} B de PixelScene`);
  }
  // Ejemplos animados (GIF codificado por src/export/gifEncoder.js).
  for (const [name, prompt, style, anim, res, scale] of [
    ['anim-dragon-lluvia', 'Un dragón rojo volando sobre un castillo de noche con lluvia', 'bit16', 'idle', 96, 3],
    ['anim-caballero-ataque', 'Un caballero con una espada de fuego', 'sprite', 'attack', 48, 4],
    ['anim-caballo', 'Un caballo', 'sprite', 'walk', 48, 4],
    ['anim-bosque', 'Un bosque encantado con una pequeña casa, luciérnagas y una hoguera', 'fantasy', 'idle', 96, 3],
  ]) {
    const scene = composeScene(parsePrompt(prompt).spec, { w: res, h: res, style, seed: 7, detail: 2, anim });
    const gif = animationToGif(scene, { scale });
    writeFileSync(path.join(out, name + '.gif'), gif);
    console.log(name.padEnd(24), `${scene.an.n} fotogramas`, `${JSON.stringify(scene).length} B de PixelScene`, `${gif.length} B de GIF`);
  }
  writeFileSync(path.join(root, 'icons', 'icon-192.png'), draw('Un dragón rojo de noche', 'bit16', 5, '1:1', 32, 192).png);
  writeFileSync(path.join(root, 'icons', 'icon-512.png'), draw('Un dragón rojo de noche', 'bit16', 5, '1:1', 32, 512).png);
}
