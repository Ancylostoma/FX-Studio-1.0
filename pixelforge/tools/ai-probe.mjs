// Prueba del modo híbrido contra la API real de Claude.
//
//   node tools/ai-probe.mjs                 prompts de prueba incluidos
//   node tools/ai-probe.mjs "prompt" "otro" prompts propios
//   node tools/ai-probe.mjs --offline       solo el motor offline (sin clave, sin coste)
//
// Para cada prompt compara lo que entiende el motor offline con lo que entiende
// la IA, y deja en docs/probe/ un informe (informe.md), una imagen por prompt
// y modo, y una hoja comparativa (comparativa.png: offline a la izquierda,
// híbrido a la derecha). Requiere ANTHROPIC_API_KEY en .env o en el entorno.
import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parsePrompt, finalize, describeSpec } from '../src/generator/promptParser.js';
import { composeScene } from '../src/generator/sceneComposer.js';
import { renderScene } from '../src/core/renderer.js';
import { scaleNearest } from '../src/core/geometry.js';
import { encodePNG } from './png.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const envFile = path.join(root, '.env');
if (existsSync(envFile)) {
  for (const line of readFileSync(envFile, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m && m[2] && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}
const { interpretPrompt, aiConfigured, interpretModelId } = await import('../server/aiAdapter.mjs');

// Descripciones elegidas para caer FUERA del vocabulario offline: es justo
// lo que el modo híbrido tiene que resolver.
const DEFAULT_PROMPTS = [
  'Un tigre acechando entre los árboles de una selva al anochecer',
  'Una catedral gótica bajo una tormenta',
  'Un hada volando sobre un estanque con nenúfares',
  'Un vaquero a caballo en el desierto al atardecer',
  'Un submarino explorando el fondo del océano',
  'Una bruja en su escoba cruzando la luna llena',
  'Un pingüino sobre un iceberg con aurora boreal',
  'Un samurái meditando bajo un cerezo junto a un templo',
  'Un tren de vapor cruzando un puente entre montañas',
  'Una niña con un paraguas rojo en una ciudad lluviosa',
  "A lighthouse keeper's cottage on a cliff on a stormy night",
  'Dos guerreros luchando frente a un volcán en erupción',
];

// Precios por millón de tokens (entrada, salida, lectura de caché, escritura de caché).
const PRICES = {
  'claude-opus-5-5': [4, 20, 0.2, 5], 'claude-sonnet-5-5': [2, 10, 0.1, 2.5], 'claude-haiku-5-5': [0.1, 0.5, 0.01, 0.125],
  'claude-fable-5-2': [8, 40, 0.2, 10],
};
const cost = (model, u) => {
  const p = PRICES[Object.keys(PRICES).find((k) => model?.startsWith(k))];
  if (!p || !u) return null;
  return (u.input_tokens * p[0] + u.output_tokens * p[1] + (u.cache_read_input_tokens || 0) * p[2] + (u.cache_creation_input_tokens || 0) * p[3]) / 1e6;
};

const args = process.argv.slice(2);
const offlineOnly = args.includes('--offline');
const prompts = args.filter((a) => !a.startsWith('--'));
const list = prompts.length ? prompts : DEFAULT_PROMPTS;
if (!offlineOnly && !aiConfigured()) {
  console.error('Falta ANTHROPIC_API_KEY (en pixelforge/.env o en el entorno). Usa --offline para probar sin clave.');
  process.exit(1);
}

const outDir = path.join(root, 'docs', 'probe');
mkdirSync(outDir, { recursive: true });
const O = { w: 96, h: 96, style: 'bit16', seed: 7, detail: 2 };
const draw = (raw) => renderScene(composeScene(finalize(raw), O));
const K = 3, cell = O.w * K + 8;
const sheet = { w: 2 * cell, h: list.length * cell, data: new Uint8ClampedArray(2 * cell * list.length * cell * 4) };
for (let i = 0; i < sheet.data.length; i += 4) sheet.data.set([40, 36, 56, 255], i);
const blit = (img, col, row) => {
  const big = scaleNearest(img.data, img.w, img.h, K);
  for (let y = 0; y < big.h; y++) sheet.data.set(big.data.subarray(y * big.w * 4, (y + 1) * big.w * 4), ((row * cell + 4 + y) * sheet.w + col * cell + 4) * 4);
  return big;
};

const rows = [];
let total = 0, tokIn = 0, tokOut = 0, failures = 0;
console.log(`Modelo: ${offlineOnly ? '(solo offline)' : interpretModelId()} · ${list.length} prompts\n`);
for (const [n, prompt] of list.entries()) {
  const slug = String(n + 1).padStart(2, '0');
  const off = parsePrompt(prompt);
  const offSpec = describeSpec(off.spec);
  writeFileSync(path.join(outDir, `${slug}-offline.png`), encodePNG(blit(draw(off.raw), 0, n)));
  const row = { n: n + 1, prompt, offSpec, offUnknown: off.spec.unknown };
  if (!offlineOnly) {
    const t0 = Date.now();
    try {
      const r = await interpretPrompt({ prompt });
      const spec = finalize(r.raw);
      row.aiSpec = describeSpec(spec);
      row.adapted = r.raw.unknown;
      row.ms = Date.now() - t0;
      row.usage = r.usage; row.model = r.model;
      row.cost = cost(r.model, r.usage);
      total += row.cost || 0; tokIn += r.usage.input_tokens + (r.usage.cache_read_input_tokens || 0); tokOut += r.usage.output_tokens;
      writeFileSync(path.join(outDir, `${slug}-hibrido.png`), encodePNG(blit(draw(r.raw), 1, n)));
    } catch (e) {
      row.error = e.message; failures++;
    }
  }
  rows.push(row);
  console.log(`${slug}. ${prompt}`);
  console.log(`    offline: ${row.offSpec}${row.offUnknown.length ? `   [no entiende: ${row.offUnknown.join(', ')}]` : ''}`);
  if (!offlineOnly) {
    if (row.error) console.log(`    híbrido: ERROR — ${row.error}`);
    else console.log(`    híbrido: ${row.aiSpec}${row.adapted.length ? `   [adaptado: ${row.adapted.join('; ')}]` : ''}\n             ${row.ms} ms · ${row.usage.output_tokens} tokens de salida${row.cost != null ? ` · $${row.cost.toFixed(4)}` : ''}`);
  }
}
writeFileSync(path.join(outDir, 'comparativa.png'), encodePNG(sheet));

const md = [`# Prueba del modo híbrido`, '', `Modelo: \`${offlineOnly ? 'ninguno (solo offline)' : rows.find((r) => r.model)?.model || interpretModelId()}\` · ${new Date().toISOString().slice(0, 16).replace('T', ' ')} UTC`, ''];
if (!offlineOnly) md.push(`Coste total: **$${total.toFixed(4)}** · tokens de entrada ${tokIn} · de salida ${tokOut} · errores ${failures}`, '');
md.push('![Comparativa: offline a la izquierda, híbrido a la derecha](comparativa.png)', '');
for (const r of rows) {
  md.push(`## ${r.n}. ${r.prompt}`, '', `- **Offline:** \`${r.offSpec}\`${r.offUnknown.length ? ` — no entiende: ${r.offUnknown.join(', ')}` : ''}`);
  if (!offlineOnly) md.push(r.error ? `- **Híbrido:** error — ${r.error}` : `- **Híbrido:** \`${r.aiSpec}\`${r.adapted.length ? ` — adaptado: ${r.adapted.join('; ')}` : ''} (${r.ms} ms, ${r.usage.output_tokens} tokens de salida${r.cost != null ? `, $${r.cost.toFixed(4)}` : ''})`);
  md.push('');
}
writeFileSync(path.join(outDir, 'informe.md'), md.join('\n'));
console.log(`\n${offlineOnly ? '' : `Coste total: $${total.toFixed(4)} · errores: ${failures}\n`}Informe: docs/probe/informe.md · hoja: docs/probe/comparativa.png`);
process.exitCode = failures ? 1 : 0;
