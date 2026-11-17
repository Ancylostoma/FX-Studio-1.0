// Benchmark de 12 escenas: motor procedural frente a Ollama (si está disponible).
//
//   node tools/benchmark.mjs                       solo procedural
//   node tools/benchmark.mjs qwen2.5:3b llama3.2:1b  procedural + esos modelos de Ollama
//   OLLAMA_URL=http://127.0.0.1:11434 node tools/benchmark.mjs qwen2.5:3b
//
// Mismos parámetros para todos (128×128, 16-bit, semilla 7). Deja en
// docs/benchmark/: una imagen por escena y motor, comparativa.png (filas =
// escenas, columnas = motores) e informe.md con métricas MEDIDAS:
//  - interpretación: elementos esperados que aparecen en el plan
//  - presencia: elementos esperados que se ven en la imagen (píxeles propios)
//  - control de calidad, colores, tiempo de interpretación y de render.
// No asigna notas de calidad artística: eso requiere mirar las imágenes.
import { writeFileSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parsePrompt, finalize } from '../src/generator/promptParser.js';
import { composeChecked, measure } from '../src/generator/qualityControl.js';
import { composeSceneEx } from '../src/generator/sceneComposer.js';
import { defOf } from '../src/generator/spriteLibrary.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { encodePNG } from './png.mjs';
import * as local from '../server/ollamaAdapter.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'docs', 'benchmark');
mkdirSync(out, { recursive: true });

// [id, descripción, elementos esperados]. Un elemento esperado es un id del
// catálogo o una etiqueta de escena (interior:<sala>, town:<estilo>, biome:<x>,
// feature:<x>, weather:<x>, battle, sky:<x>).
export const BENCH = [
  ['dragon-castillo', 'Un dragón volando sobre un castillo', ['dragon', 'castle']],
  ['bosque-japones', 'Un bosque japonés con un lago y un cerezo', ['cherry', 'feature:lake']],
  ['cyberpunk', 'Una ciudad cyberpunk de noche', ['biome:cyber']],
  ['astronauta-marte', 'Un astronauta sobre Marte', ['astronaut', 'biome:mars']],
  ['caballero-castillo', 'Un caballero medieval frente a un castillo', ['knight', 'castle']],
  ['aldea-tropical', 'Una aldea tropical frente al mar', ['town:tropical']],
  ['biblioteca', 'El interior de una biblioteca fantástica', ['interior:library']],
  ['tren-montanas', 'Un tren futurista atravesando montañas', ['train', 'feature:mountains']],
  ['tigre-selva', 'Un tigre en una selva', ['tiger', 'biome:forest']],
  ['nave-planetas', 'Una nave espacial rodeada de planetas', ['spaceship', 'sky:planet']],
  ['batalla', 'Una batalla fantástica entre criaturas', ['battle', 'creatures:2']],
  ['ciudad-tormenta', 'Una ciudad medieval durante una tormenta', ['town:medieval', 'weather:storm']],
];
const O = { w: 128, h: 128, style: 'bit16', seed: 7, detail: 2 };

function tagsOf(spec) {
  const t = new Set();
  for (const e of [...spec.subjects, ...spec.structures, ...spec.flora]) t.add(e.id);
  if (spec.setting === 'interior') t.add('interior:' + spec.room);
  if (spec.town) t.add('town:' + spec.town.style);
  if (spec.biome) t.add('biome:' + spec.biome);
  for (const f of spec.features) t.add('feature:' + f);
  for (const w of spec.weather) t.add('weather:' + w);
  for (const c of spec.celestial) t.add('sky:' + c);
  if (spec.battle) t.add('battle');
  const fighters = spec.subjects.filter((e) => ['creature', 'animal', 'humanoid'].includes(defOf(e)?.cat)).length;
  if (fighters >= 2) t.add('creatures:2');
  return t;
}

async function runEngine(engine, prompt, model) {
  const t0 = performance.now();
  let raw, info = {};
  if (engine === 'procedural') raw = parsePrompt(prompt).raw;
  else {
    const r = await local.interpret({ model, prompt, temperature: 0, maxTokens: 1200, timeoutMs: 600000, prefs: 'balanced' });
    raw = r.raw; info = r.timings;
  }
  const interpMs = performance.now() - t0;
  const spec = finalize(raw);
  const t1 = performance.now();
  const { scene, qc } = composeChecked(spec, O);
  const renderMs = performance.now() - t1;
  // Presencia en la imagen: entidades esperadas con píxeles propios visibles.
  const { G } = composeSceneEx(spec, O);
  const M = measure(scene, G);
  const visible = new Set([...M.own.entries()].filter(([, m]) => m.visible >= 4).map(([k]) => k.replace(/^(subj|struct):/, '').replace(/\d+$/, '')));
  return { spec, scene, qc, interpMs, renderMs, info, visible };
}

const models = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const engines = [['procedural', null]];
if (models.length) {
  const st = await local.status();
  if (!st.available) console.log(`Ollama no disponible (${st.reason}): solo se ejecuta el motor procedural.`);
  else for (const m of models) { if (st.models.some((x) => x.name === m)) engines.push(['ollama', m]); else console.log(`Modelo no instalado en Ollama: ${m}`); }
}

const K = 3, cell = O.w * K + 8;
const sheet = { w: engines.length * cell, h: BENCH.length * cell, data: new Uint8ClampedArray(engines.length * cell * BENCH.length * cell * 4) };
for (let i = 0; i < sheet.data.length; i += 4) sheet.data.set([36, 32, 52, 255], i);
const rows = [];
for (const [row, [id, prompt, expect]] of BENCH.entries()) {
  const r = { id, prompt, expect, runs: [] };
  for (const [col, [engine, model]] of engines.entries()) {
    const label = engine === 'procedural' ? 'procedural' : model;
    try {
      const x = await runEngine(engine, prompt, model);
      const tags = tagsOf(x.spec);
      const interpreted = expect.filter((t) => tags.has(t));
      // Solo sujetos y estructuras tienen capas propias medibles (la vegetación comparte capa).
      const entities = expect.filter((t) => !t.includes(':') && t !== 'battle' && defOf({ id: t })?.cat !== 'flora');
      const seen = entities.filter((t) => x.visible.has(defOf({ id: t })?.id || t));
      const img = renderScene(x.scene), big = scaleNearest(img.data, img.w, img.h, K);
      for (let y = 0; y < big.h; y++) sheet.data.set(big.data.subarray(y * big.w * 4, (y + 1) * big.w * 4), ((row * cell + 4 + y) * sheet.w + col * cell + 4) * 4);
      writeFileSync(path.join(out, `${String(row + 1).padStart(2, '0')}-${id}-${label.replace(/[^\w.-]/g, '_')}.png`), encodePNG(scaleNearest(img.data, img.w, img.h, 4)));
      r.runs.push({ label, interpreted, missing: expect.filter((t) => !tags.has(t)), seen: `${seen.length}/${entities.length}`, qc: `${x.qc.checks.filter((c) => c.ok).length}/${x.qc.checks.length}`, qcBad: x.qc.checks.filter((c) => !c.ok).map((c) => c.id), colors: x.scene.pal.length, interpMs: Math.round(x.interpMs), renderMs: Math.round(x.renderMs), tps: x.info.tokensPerSecond || null, notes: x.spec.notes });
    } catch (e) { r.runs.push({ label, error: e.message }); }
    const last = r.runs.at(-1);
    console.log(`${String(row + 1).padStart(2)} ${id.padEnd(20)} ${label.padEnd(12)} ${last.error ? 'ERROR ' + last.error : `interpretación ${last.interpreted.length}/${expect.length} · visibles ${last.seen} · QC ${last.qc} · ${last.colors} colores · ${last.interpMs} ms + ${last.renderMs} ms`}`);
  }
  rows.push(r);
}
writeFileSync(path.join(out, 'comparativa.png'), encodePNG(sheet));

const pct = (a, b) => (b ? Math.round((100 * a) / b) + ' %' : '—');
const md = ['# Benchmark de 12 escenas', '', `Parámetros comunes: ${O.w}×${O.h}, estilo 16-bit, semilla ${O.seed}. Columnas de la comparativa: ${engines.map(([e, m]) => e === 'procedural' ? 'procedural' : m).join(' · ')}.`, '', '![Comparativa](comparativa.png)', '', '## Resumen medido', '', '| Motor | Interpretación (elementos esperados) | Elementos visibles en la imagen | Control de calidad | Tiempo medio de interpretación |', '|---|---|---|---|---|'];
for (const [col, [e, m]] of engines.entries()) {
  const runs = rows.map((r) => r.runs[col]).filter((x) => !x.error);
  const exp = rows.reduce((a, r) => a + r.expect.length, 0), got = runs.reduce((a, x) => a + x.interpreted.length, 0);
  const seen = runs.reduce((a, x) => a + +x.seen.split('/')[0], 0), ent = runs.reduce((a, x) => a + +x.seen.split('/')[1], 0);
  const qcOk = runs.reduce((a, x) => a + +x.qc.split('/')[0], 0), qcAll = runs.reduce((a, x) => a + +x.qc.split('/')[1], 0);
  md.push(`| ${e === 'procedural' ? 'Procedural' : 'Ollama · ' + m} | ${got}/${exp} (${pct(got, exp)}) | ${seen}/${ent} (${pct(seen, ent)}) | ${qcOk}/${qcAll} | ${Math.round(runs.reduce((a, x) => a + x.interpMs, 0) / Math.max(1, runs.length))} ms |`);
}
md.push('', '> Aviso: estas 12 descripciones usan palabras que el vocabulario procedural conoce, en parte porque se amplió pensando en ellas. Por eso su interpretación es completa aquí; no indica que entienda cualquier redacción. La comprensión de frases nuevas se mide con «Evaluar este modelo» (panel IA local), que usa sinónimos fuera del vocabulario. «Elementos visibles» solo cuenta sujetos y estructuras: la vegetación comparte capa y no se puede medir por separado.');
if (engines.length === 1) md.push('', '> Solo se ejecutó el motor procedural. Para comparar con Ollama: `node tools/benchmark.mjs <modelo> [modelo…]` con Ollama en marcha.');
md.push('', '## Detalle por escena', '');
for (const r of rows) {
  md.push(`### ${r.prompt}`, '', `Esperado: ${r.expect.join(', ')}`, '');
  for (const x of r.runs) md.push(x.error ? `- **${x.label}:** error — ${x.error}` : `- **${x.label}:** interpretación ${x.interpreted.length}/${r.expect.length}${x.missing.length ? ' (falta: ' + x.missing.join(', ') + ')' : ''} · visibles ${x.seen} · QC ${x.qc}${x.qcBad.length ? ' (no superado: ' + x.qcBad.join(', ') + ')' : ''} · ${x.colors} colores · ${x.interpMs} ms + ${x.renderMs} ms${x.tps ? ` · ${x.tps} tok/s` : ''}${x.notes.length ? ' · ' + x.notes.join(' ') : ''}`);
  md.push('');
}
writeFileSync(path.join(out, 'informe.md'), md.join('\n'));
console.log('\nInforme: docs/benchmark/informe.md · comparativa: docs/benchmark/comparativa.png');
