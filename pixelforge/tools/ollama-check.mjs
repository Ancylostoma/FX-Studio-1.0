// Prueba REAL de integración con Ollama (se ejecuta en el equipo que tiene Ollama).
//
//   node tools/ollama-check.mjs                 modelo por defecto (qwen3.5:4b)
//   node tools/ollama-check.mjs qwen3.5:4b      otro modelo instalado
//   node tools/ollama-check.mjs --url http://127.0.0.1:11434 --out carpeta
//
// Recorre los 12 puntos de la etapa de integración y deja en
// docs/ollama-check/: informe.md, resultados.json, las respuestas en bruto del
// modelo y las imágenes generadas. Todo lo que figura en el informe es lo que
// ha ocurrido: nada se da por bueno sin medirlo.
import { writeFileSync, mkdirSync, readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const arg = (name) => { const i = process.argv.indexOf(name); return i > 0 ? process.argv[i + 1] : null; };
const positional = process.argv.slice(2).filter((a, i, all) => !a.startsWith('--') && !all[i - 1]?.startsWith('--'));
const outDir = path.resolve(arg('--out') || path.join(root, 'docs', 'ollama-check'));
mkdirSync(outDir, { recursive: true });

// 12. Registro de TODAS las conexiones de red de esta prueba (deben ser locales).
const hosts = new Map();
const realFetch = globalThis.fetch;
globalThis.fetch = (input, init) => { const u = new URL(typeof input === 'string' ? input : input.url); hosts.set(u.host, (hosts.get(u.host) || 0) + 1); return realFetch(input, init); };

const local = await import('../server/ollamaAdapter.mjs');
const { PLAN_SCHEMA } = await import('../src/ai/planSchema.js');
const { checkSchema } = await import('../src/ai/schemaCheck.js');
const { parsePrompt, finalize, describeSpec } = await import('../src/generator/promptParser.js');
const { composeChecked } = await import('../src/generator/qualityControl.js');
const { renderScene } = await import('../src/core/renderer.js');
const { validateScene } = await import('../src/core/pixelScene.js');
const { scaleNearest } = await import('../src/core/geometry.js');
const { encodePNG } = await import('./png.mjs');

const url = local.resolveUrl(arg('--url') || undefined);
const model = positional[0] || local.DEFAULT_MODEL();
const results = { fecha: new Date().toISOString(), url, modelo: model, pasos: [], incompatibilidades: [] };
const step = (n, titulo, ok, detalle, extra = {}) => { results.pasos.push({ n, titulo, ok, detalle, ...extra }); console.log(`${ok === true ? '✓' : ok === false ? '✗' : '·'} ${String(n).padStart(2)}. ${titulo}${detalle ? ' — ' + detalle : ''}`); };
const ms = (t0) => Math.round(performance.now() - t0);
const O = { w: 128, h: 128, style: 'bit16', seed: 7, detail: 3 };
const savePng = (name, scene) => { const img = renderScene(scene); const file = path.join(outDir, name); writeFileSync(file, encodePNG(scaleNearest(img.data, img.w, img.h, 4))); return path.relative(outDir, file).replace(/\\/g, '/'); };

async function chat(body, timeoutMs = 300000) {
  const t0 = performance.now();
  const r = await realFetch(url + '/api/chat', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ stream: false, keep_alive: '30m', ...body }), signal: AbortSignal.timeout(timeoutMs) });
  hosts.set(new URL(url).host, (hosts.get(new URL(url).host) || 0) + 1);
  const j = await r.json().catch(() => null);
  if (!r.ok) throw Object.assign(new Error(j?.error || 'HTTP ' + r.status), { status: r.status });
  return { ...j, ms: ms(t0), tps: j.eval_count && j.eval_duration ? +(j.eval_count / (j.eval_duration / 1e9)).toFixed(1) : null };
}
const parseJson = (t) => { try { return JSON.parse(t); } catch { const s = t.indexOf('{'), e = t.lastIndexOf('}'); try { return s >= 0 ? JSON.parse(t.slice(s, e + 1)) : null; } catch { return null; } } };

console.log(`Prueba de integración · Ollama en ${url} · modelo ${model}\n`);

// 1. Disponibilidad
const st = await local.status(url);
step(1, 'Ollama disponible', st.available, st.available ? `versión ${st.version}` : st.reason);
if (!st.available) {
  results.incompatibilidades.push('Ollama no responde: inícialo (aplicación Ollama o "ollama serve") y repite la prueba.');
  finish(); process.exit(1);
}
// 2. Modelo instalado
const m = st.models.find((x) => x.name === model);
step(2, `${model} instalado`, !!m, m ? `${(m.size / 1e9).toFixed(2)} GB · ${m.params || '?'} · ${m.quant || '?'} · capacidades: ${(m.capabilities || ['desconocidas']).join(', ')}` : 'instalados: ' + st.models.map((x) => x.name).join(', '));
if (!m) { finish(); process.exit(1); }
const thinking = (m.capabilities || []).includes('thinking');
results.sistema = { ram: st.system.totalRAM, gpu: st.system.gpu, cpu: st.system.cpu };

// Carga del modelo y uso de GPU
try {
  const w = await local.warmup({ url, model, keepAlive: '30m' });
  results.carga = w;
  step('·', 'Carga del modelo en memoria', true, `${(w.ms / 1000).toFixed(1)} s${w.gpuShare != null ? ` · ${w.gpuShare} % en GPU (ollama ps)` : ''}`);
} catch (e) { step('·', 'Carga del modelo en memoria', false, e.message); }

// 3–4. Prompt en español, respuesta libre
try {
  const r = await chat({ model, messages: [{ role: 'user', content: 'Responde en una sola frase en español: ¿qué es el pixel art?' }], options: { temperature: 0.1, seed: 7, num_ctx: 4096, num_predict: 200 }, ...(thinking ? { think: false } : {}) });
  const txt = String(r.message?.content || '').trim();
  step(3, 'Envío de un prompt en español', true, `${r.ms} ms · ${r.tps ?? '?'} tok/s`);
  step(4, 'Respuesta del modelo', txt.length > 10, `«${txt.slice(0, 160)}»`);
} catch (e) { step(3, 'Envío de un prompt en español', false, e.message); }

// 5–6. JSON estructurado con un esquema compacto (validación estricta de la salida en bruto)
const COLORES = ['rojo', 'naranja', 'amarillo', 'verde', 'azul', 'morado', 'rosa', 'marrón', 'negro', 'blanco', 'gris', 'dorado', 'plateado', 'turquesa'];
const COMPACT = { type: 'object', additionalProperties: false, required: ['sujeto_principal', 'escenario', 'estilo', 'ambiente', 'paleta', 'objetos'], properties: {
  sujeto_principal: { type: 'string' }, escenario: { type: 'string' },
  estilo: { type: 'string', enum: ['fantasia', 'medieval', 'ciencia_ficcion', 'realista', 'cyberpunk', 'japones', 'retro'] },
  ambiente: { type: 'string', enum: ['dia', 'noche', 'atardecer', 'amanecer', 'tormenta', 'interior'] },
  paleta: { type: 'array', items: { type: 'string', enum: COLORES } },
  objetos: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['nombre', 'posicion', 'profundidad', 'importancia'], properties: {
    nombre: { type: 'string' }, posicion: { type: 'string', enum: ['izquierda', 'centro', 'derecha', 'arriba', 'abajo'] },
    profundidad: { type: 'string', enum: ['fondo', 'medio', 'primer_plano'] }, importancia: { type: 'string', enum: ['protagonista', 'secundario', 'ambiental'] } } } },
} };
const DRAGON = 'Un enorme dragón rojo volando sobre un castillo medieval durante una noche de luna llena.';
const compactCases = [
  { prompt: DRAGON, expect: [['drag', 'importancia', ['protagonista']], ['castill', 'profundidad', ['fondo', 'medio']], ['luna', 'profundidad', ['fondo']]] },
  { prompt: 'Algo misterioso brilla en la oscuridad.', expect: [] },
  { prompt: 'Un mercado bullicioso en una ciudad del desierto al mediodía, con camellos, toldos de colores, un mago vendiendo pociones y una mezquita al fondo.', expect: [['mezquita', 'profundidad', ['fondo', 'medio']]] },
];
results.compacto = [];
for (const [i, c] of compactCases.entries()) {
  try {
    const r = await chat({ model, format: COMPACT, messages: [{ role: 'system', content: 'Eres director artístico de pixel art. Describe la composición de la escena en JSON según el esquema. Los nombres de objetos en español.' }, { role: 'user', content: c.prompt }], options: { temperature: 0.1, seed: 7, num_ctx: 4096, num_predict: 800 }, ...(thinking ? { think: false } : {}) });
    const text = String(r.message?.content || '');
    const json = parseJson(text);
    const viol = json ? checkSchema(COMPACT, json) : ['no es JSON'];
    const sem = c.expect.map(([name, field, ok]) => { const o = (json?.objetos || []).find((x) => String(x.nombre).toLowerCase().includes(name)); return { esperado: `${name}: ${field} ∈ {${ok.join(', ')}}`, obtenido: o ? o[field] : 'no aparece', ok: !!o && ok.includes(o[field]) }; });
    writeFileSync(path.join(outDir, `compacto-${i + 1}.json`), text);
    results.compacto.push({ prompt: c.prompt, ms: r.ms, tps: r.tps, cumpleEsquema: viol.length === 0, infracciones: viol, semantica: sem, json });
    step(i === 0 ? 5 : '·', `JSON estructurado (esquema compacto) · «${c.prompt.slice(0, 40)}…»`, true, `${r.ms} ms · ${r.tps ?? '?'} tok/s`);
    step(i === 0 ? 6 : '·', 'La salida en bruto cumple el esquema', viol.length === 0, viol.length ? viol.slice(0, 4).join('; ') : `${json.objetos.length} objetos`);
    if (sem.length) step('·', 'Composición coherente (protagonista, fondo)', sem.every((x) => x.ok), sem.map((x) => `${x.esperado} → ${x.obtenido}`).join(' | '));
  } catch (e) {
    results.compacto.push({ prompt: c.prompt, error: e.message });
    step(i === 0 ? 5 : '·', 'JSON estructurado (esquema compacto)', false, e.message);
    if (/format|schema|grammar/i.test(e.message)) results.incompatibilidades.push('El esquema compacto fue rechazado: ' + e.message);
  }
}

// 7–10. Plan de dirección artística completo → escena → motor → PNG
// [id, descripción, lo que debe aparecer]. Cada esperado admite alternativas
// con "|". Etiquetas: id del catálogo, biome:, feature:, weather:, sky:,
// time:, town, focus:, rel:<a>:<tipo>:<b>.
const SCENES = [
  ['personaje', 'Una guerrera elfa con capa verde y un arco, de pie en un claro del bosque, mirando hacia la izquierda.', ['elf|archer|custom', 'biome:forest', 'facing:left', '!knight']],
  ['paisaje', 'Un valle con un río serpenteante, montañas nevadas al fondo y niebla al amanecer.', ['feature:mountains', 'feature:river', 'weather:fog', 'time:dawn', '!town']],
  ['arquitectura', 'Una catedral gótica con vidrieras iluminadas en la plaza de una ciudad medieval bajo la lluvia.', ['cathedral', 'town', 'weather:rain']],
  ['fantasia-compleja', 'Un enorme dragón rojo volando sobre un castillo medieval durante una noche de luna llena, mientras un mago lanza un hechizo desde la torre y unos murciélagos huyen.', ['dragon', 'castle', 'wizard', 'bat', 'sky:moon', 'time:night', 'focus:dragon', 'rel:wizard:on:tower', '!biome:interior']],
  ['ambigua', 'algo antiguo y olvidado', []],
  ['fuera-del-catalogo', 'Un samurái montado en un oso polar junto a un faro, con una aurora boreal en el cielo.', ['samurai', 'bear', 'lighthouse', 'sky:aurora', 'rel:samurai:on:bear', 'color:bear:white', '!biome:interior']],
];
function tagsOf(spec) {
  const t = new Set(), ents = [...spec.subjects, ...spec.structures, ...spec.flora];
  for (const e of ents) { t.add(e.kit ? 'custom' : e.id); if (e.rel) t.add(`rel:${e.id}:${e.rel.type}:${e.rel.of}`); if (e.facing) t.add('facing:' + e.facing); if (e.color) t.add(`color:${e.id}:${e.color}`); }
  if (spec.biome) t.add('biome:' + spec.biome);
  for (const f of spec.features) t.add('feature:' + f);
  for (const w of spec.weather) t.add('weather:' + w);
  for (const c of spec.celestial) t.add('sky:' + c);
  if (spec.time) t.add('time:' + spec.time);
  if (spec.town) t.add('town');
  if (spec.focus) t.add('focus:' + spec.focus);
  return t;
}
function fidelity(spec, expect, prompt) {
  const t = tagsOf(spec);
  const ok = (x) => (x.startsWith('!') ? !t.has(x.slice(1)) : x.split('|').some((y) => t.has(y)));
  const missing = expect.filter((x) => !ok(x));
  // Inventados: objetos que ni se esperaban ni reconoce el parser en la frase.
  const parsed = parsePrompt(prompt).spec, known = new Set([...parsed.subjects, ...parsed.structures, ...parsed.flora].map((e) => e.id));
  const wanted = new Set(expect.flatMap((x) => x.split('|')));
  const invented = [...spec.subjects, ...spec.structures, ...spec.flora].filter((e) => !e.kit && !known.has(e.id) && !wanted.has(e.id) && !e.clash).map((e) => e.id);
  return { cumplidos: expect.length - missing.length, total: expect.length, faltan: missing, inventados: [...new Set(invented)] };
}
results.escenas = [];
for (const [id, prompt, expect] of SCENES) {
  const row = { id, prompt };
  try {
    const r = await local.interpret({ url, model, prompt, temperature: 0.1, seed: 7, numCtx: 4096, keepAlive: '30m', maxTokens: 1200, timeoutMs: 300000, prefs: 'balanced', debug: true });
    const text = r.modelText?.[0] || '';
    writeFileSync(path.join(outDir, `plan-${id}.json`), r.modelText.join('\n\n--- reintento ---\n\n'));
    const bruto = parseJson(text);
    row.textoModelo = text;
    row.infraccionesEsquema = bruto ? checkSchema(PLAN_SCHEMA, bruto) : ['no es JSON'];
    row.intentos = r.attempts; row.descartados = r.warnings; row.compat = r.compat; row.tiempos = r.timings;
    for (const c of r.compat) if (!results.incompatibilidades.includes(c)) results.incompatibilidades.push(c);
    const spec = finalize(r.raw);
    row.interpretacion = describeSpec(spec); row.notas = spec.notes; row.correcciones = r.corrections || [];
    row.fidelidad = fidelity(spec, expect, prompt);
    row.fidelidadProcedural = fidelity(parsePrompt(prompt).spec, expect, prompt);
    const t1 = performance.now();
    const { scene, qc } = composeChecked(spec, O);
    row.renderMs = ms(t1);
    row.escenaValida = validateScene(scene).ok;
    row.calidad = `${qc.checks.filter((c) => c.ok).length}/${qc.checks.length}`;
    row.png = savePng(`ollama-${id}.png`, scene);
    const proc = composeChecked(parsePrompt(prompt).spec, O);
    row.pngProcedural = savePng(`procedural-${id}.png`, proc.scene);
    row.interpretacionProcedural = describeSpec(parsePrompt(prompt).spec);
    step('·', `Escena «${id}»`, row.escenaValida, `interpretación ${r.timings.totalMs} ms (${r.timings.tokensPerSecond ?? '?'} tok/s) · render ${row.renderMs} ms · esquema ${row.infraccionesEsquema.length ? row.infraccionesEsquema.length + ' infracción(es)' : 'cumplido'} · calidad ${row.calidad} · fidelidad ${row.fidelidad.cumplidos}/${row.fidelidad.total}${row.fidelidad.inventados.length ? ' · inventados: ' + row.fidelidad.inventados.join(', ') : ''}`);
  } catch (e) { row.error = e.message; step('·', `Escena «${id}»`, false, e.message); }
  results.escenas.push(row);
}
const okScenes = results.escenas.filter((r) => !r.error);
const strict = okScenes.filter((r) => !r.infraccionesEsquema.length).length;
step(7, 'Respuesta convertida en escena', okScenes.length === SCENES.length, `${okScenes.length}/${SCENES.length} escenas`);
step(8, 'Escena validada como PixelScene y enviada al motor', okScenes.every((r) => r.escenaValida), `${okScenes.filter((r) => r.escenaValida).length}/${okScenes.length} válidas`);
step(9, 'Imagen producida', okScenes.every((r) => r.png), `${okScenes.length} imágenes`);
step(10, 'Exportación a PNG', okScenes.every((r) => statSync(path.join(outDir, r.png)).size > 100), outDir);
step('·', 'El modelo respeta el esquema completo sin necesitar saneado', strict === okScenes.length, `${strict}/${okScenes.length} respuestas sin infracciones`);
const sum = (k) => okScenes.reduce((a, r) => a + r[k].cumplidos, 0), tot = okScenes.reduce((a, r) => a + r.fidelidad.total, 0);
const inv = okScenes.reduce((a, r) => a + r.fidelidad.inventados.length, 0);
results.fidelidad = { ollama: sum('fidelidad'), procedural: sum('fidelidadProcedural'), total: tot, inventados: inv };
step('·', 'Fidelidad a la descripción (elementos pedidos presentes)', null, `Ollama ${sum('fidelidad')}/${tot} · procedural ${sum('fidelidadProcedural')}/${tot} · objetos inventados por Ollama: ${inv}`);

// Repetibilidad: con temperatura 0,1 y la misma semilla, la misma frase debe dar el mismo plan.
// Con otra semilla puede variar (así se piden variaciones desde la app).
try {
  const base = results.escenas.find((r) => r.id === 'fantasia-compleja');
  const again = async (seed) => (await local.interpret({ url, model, prompt: base.prompt, temperature: 0.1, seed, numCtx: 4096, keepAlive: '30m', maxTokens: 1200, timeoutMs: 300000, prefs: 'balanced', debug: true })).modelText?.[0] || '';
  const same = await again(7), other = await again(8);
  results.repetible = { mismaSemilla: same === base.textoModelo, otraSemillaDistinta: other !== base.textoModelo };
  step('·', 'Repetible: misma frase y misma semilla → mismo plan', results.repetible.mismaSemilla, `semilla 7 dos veces: ${results.repetible.mismaSemilla ? 'idéntico' : 'distinto'} · semilla 8: ${results.repetible.otraSemillaDistinta ? 'plan distinto (variación)' : 'mismo plan'} · temperatura 0,1`);
} catch (e) { step('·', 'Repetible: misma frase y misma semilla → mismo plan', false, e.message); }

// 11. Respaldo procedural sin Ollama (servidor inexistente)
try {
  await local.interpret({ url: 'http://127.0.0.1:9', model, prompt: DRAGON, timeoutMs: 3000 });
  step(11, 'Modo procedural sin Ollama', false, 'se esperaba un error de conexión');
} catch (e) {
  const proc = composeChecked(parsePrompt(DRAGON).spec, O);
  const file = savePng('procedural-sin-ollama.png', proc.scene);
  step(11, 'Modo procedural sin Ollama', e.code === 'unreachable' && validateScene(proc.scene).ok, `Ollama inaccesible (${e.code}) → imagen procedural ${file}`);
}

// 12. Sin servicios de IA remotos: conexiones reales de esta prueba + revisión del código
const remote = [...hosts.keys()].filter((h) => !/^(127\.0\.0\.1|localhost|\[::1\])(:\d+)?$/.test(h));
const scan = [];
const walk = (d) => { for (const f of readdirSync(d)) { const p = path.join(d, f); if (statSync(p).isDirectory()) walk(p); else if (/\.(m?js)$/.test(f)) { const t = readFileSync(p, 'utf8'); if (/fetch\(\s*['"`]https?:\/\//.test(t)) scan.push(path.relative(root, p)); } } };
walk(path.join(root, 'src'));
step(12, 'Sin llamadas a servicios de IA remotos', !remote.length && !scan.length, `conexiones de la prueba: ${[...hosts.entries()].map(([h, n]) => `${h} ×${n}`).join(', ')}${scan.length ? ' · fetch absolutos en: ' + scan.join(', ') : ' · el cliente solo llama a rutas locales'}`);
const cloud = process.env.PIXELFORGE_ENABLE_CLOUD === '1';
step('·', 'Nube desactivada', !cloud, cloud ? 'PIXELFORGE_ENABLE_CLOUD=1 está activo' : 'PIXELFORGE_ENABLE_CLOUD no está activo: Claude no se puede usar');

try { const ps = await local.status(url); results.gpuFinal = (ps.running || []).find((r) => r.name === model) || null; } catch { /* sin estado */ }
finish();

function finish() {
  writeFileSync(path.join(outDir, 'resultados.json'), JSON.stringify(results, null, 2));
  const L = [`# Prueba de integración con Ollama`, '', `Fecha: ${results.fecha} · Servidor: ${results.url} · Modelo: \`${results.modelo}\` · Temperatura 0,1 · Semilla 7`];
  if (results.sistema) L.push(`Equipo: ${(results.sistema.ram / 1e9).toFixed(1)} GB de RAM · ${results.sistema.cpu} · GPU: ${results.sistema.gpu ? results.sistema.gpu.name + (results.sistema.gpu.vram ? ` (${(results.sistema.gpu.vram / 1e9).toFixed(1)} GB)` : '') : 'no detectada con nvidia-smi'}`);
  if (results.carga) L.push(`Carga del modelo: ${(results.carga.ms / 1000).toFixed(1)} s${results.carga.gpuShare != null ? ` · ${results.carga.gpuShare} % en GPU` : ''}`);
  L.push('', '## Pasos', '', '| Paso | Resultado | Detalle |', '|---|---|---|');
  for (const p of results.pasos) L.push(`| ${p.n} ${p.titulo} | ${p.ok === true ? '✓' : p.ok === false ? '✗' : '·'} | ${String(p.detalle || '').replace(/\|/g, '/')} |`);
  L.push('', '## Incompatibilidades detectadas', '', ...(results.incompatibilidades.length ? results.incompatibilidades.map((x) => '- ' + x) : ['- Ninguna detectada en esta ejecución.']));
  if (results.compacto?.length) {
    L.push('', '## Esquema compacto', '');
    for (const c of results.compacto) L.push(`- «${c.prompt}» → ${c.error ? 'error: ' + c.error : `${c.cumpleEsquema ? 'cumple el esquema' : 'NO cumple: ' + c.infracciones.slice(0, 5).join('; ')} · ${c.ms} ms`}${c.semantica?.length ? ' · ' + c.semantica.map((s) => `${s.ok ? '✓' : '✗'} ${s.esperado} → ${s.obtenido}`).join(' · ') : ''}`);
  }
  if (results.escenas?.length) {
    L.push('', '## Escenas (izquierda Ollama, derecha procedural)', '');
    for (const r of results.escenas) {
      L.push(`### ${r.id}: ${r.prompt}`, '');
      if (r.error) { L.push(`Error: ${r.error}`, ''); continue; }
      L.push(`![${r.id} con Ollama](${r.png}) ![${r.id} procedural](${r.pngProcedural})`, '', `- Ollama: \`${r.interpretacion}\``, `- Procedural: \`${r.interpretacionProcedural}\``, `- Fidelidad: Ollama ${r.fidelidad.cumplidos}/${r.fidelidad.total}${r.fidelidad.faltan.length ? ' (falta: ' + r.fidelidad.faltan.join(', ') + ')' : ''}${r.fidelidad.inventados.length ? ' · inventados: ' + r.fidelidad.inventados.join(', ') : ''} · procedural ${r.fidelidadProcedural.cumplidos}/${r.fidelidadProcedural.total}`, ...(r.correcciones.length ? [`- Correcciones automáticas: ${r.correcciones.join('; ')}`] : []), `- Esquema: ${r.infraccionesEsquema.length ? r.infraccionesEsquema.length + ' infracción(es): ' + r.infraccionesEsquema.slice(0, 4).join('; ') : 'cumplido'} · intentos: ${r.intentos} · datos descartados: ${r.descartados.length}`, `- Tiempos: interpretación ${r.tiempos.totalMs} ms (carga ${r.tiempos.loadMs ?? '?'} ms, ${r.tiempos.outputTokens ?? '?'} tokens a ${r.tiempos.tokensPerSecond ?? '?'} tok/s) · render ${r.renderMs} ms · control de calidad ${r.calidad}`, ...(r.notas.length ? [`- Notas: ${r.notas.join(' ')}`] : []), '');
    }
  }
  L.push('', 'Respuestas en bruto del modelo: `compacto-*.json` y `plan-*.json`. Datos completos: `resultados.json`.');
  writeFileSync(path.join(outDir, 'informe.md'), L.join('\n'));
  console.log(`\nInforme: ${path.join(outDir, 'informe.md')}`);
}
