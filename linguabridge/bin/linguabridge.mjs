#!/usr/bin/env node
// Uso:
//   node bin/linguabridge.mjs [opciones] archivo.txt     (o texto por la entrada estándar)
//   --lang es|en|auto   --level 1..5   --mode general|nursing   --seed N
//   --ollama [modelo]   usa Ollama si está disponible
//   --protect "término1;término2"   --json   --report   --demo
import { readFileSync } from 'node:fs';
import { paraphrase, createOllamaProvider, LEVELS } from '../src/engine/index.js';

const args = process.argv.slice(2);
const opt = { lang: 'auto', level: 3, mode: 'general' };
let file = null, json = false, report = false, ollama = null, demo = false;
for (let i = 0; i < args.length; i++) {
  const a = args[i];
  if (a === '--lang') opt.lang = args[++i];
  else if (a === '--level') opt.level = Number(args[++i]);
  else if (a === '--mode') opt.mode = args[++i];
  else if (a === '--seed') opt.seed = Number(args[++i]);
  else if (a === '--protect') opt.protectedTerms = args[++i].split(';');
  else if (a === '--ollama') ollama = args[i + 1] && !args[i + 1].startsWith('--') && !args[i + 1].includes('.') ? args[++i] : (process.env.LINGUABRIDGE_OLLAMA_MODEL || 'qwen3.5:4b');
  else if (a === '--json') json = true;
  else if (a === '--report') report = true;
  else if (a === '--demo') demo = true;
  else if (a === '--help' || a === '-h') { console.log(readFileSync(new URL(import.meta.url)).toString().split('\n').slice(1, 7).join('\n').replace(/^\/\/ ?/gm, '')); process.exit(0); }
  else file = a;
}

const DEMO = 'Con el objetivo de evaluar la adherencia, se llevó a cabo un estudio en el año 2022 con 120 pacientes diabéticos (García et al., 2021). La insulina fue administrada cada 24 horas por vía subcutánea. Sin embargo, los resultados no fueron estadísticamente significativos (p = 0,07).';
const text = demo ? DEMO : file ? readFileSync(file, 'utf8') : readFileSync(0, 'utf8');

if (ollama) {
  const provider = createOllamaProvider({ baseUrl: process.env.OLLAMA_URL || 'http://127.0.0.1:11434', model: ollama });
  const st = await provider.status();
  if (st.available) opt.llm = { provider };
  else console.error(`[aviso] Ollama no disponible (${st.error}); se usan solo reglas.`);
}

const r = await paraphrase(text, opt);
if (json) { console.log(JSON.stringify(r, null, 2)); process.exit(r.integrity.ok ? 0 : 2); }
console.log(r.result);
if (report || demo) {
  console.error(`\n--- ${r.lang.toUpperCase()} · nivel ${r.level} (${LEVELS[r.level].name}) · ${r.mode} · motor: ${r.engine} · ${r.stats.changedPct} % de palabras cambiadas`);
  for (const c of r.integrity.checks) console.error(`${c.ok ? '✔' : '✘'} ${c.name}${c.detail ? ' — ' + c.detail : ''}`);
  for (const w of r.warnings) console.error('! ' + w);
  for (const c of r.changes) console.error(`· [${c.tier}] «${c.from.trim()}» → «${c.to.trim()}»`);
}
process.exit(r.integrity.ok ? 0 : 2);
