// Adaptador de IA local con Ollama. Habla con la interfaz HTTP local de
// Ollama (por defecto http://127.0.0.1:11434): no usa claves ni servicios en
// la nube. El modelo devuelve un plan de dirección artística (datos, nunca
// código) que se sanea y valida antes de llegar al motor gráfico.
import os from 'node:os';
import { execFile } from 'node:child_process';
import { PLAN_SCHEMA, buildLocalMessages, sanitizePlan, applyCertainties } from '../src/ai/planSchema.js';
import { finalize, parsePrompt } from '../src/generator/promptParser.js';

export const DEFAULT_URL = 'http://127.0.0.1:11434';
// Modelo por defecto (configurable con PIXELFORGE_OLLAMA_MODEL en .env).
export const DEFAULT_MODEL = () => process.env.PIXELFORGE_OLLAMA_MODEL || 'qwen3.5:4b';
// Ajustes por defecto pensados para un portátil con 16 GB de RAM y GPU de 6 GB.
// Temperatura baja y semilla fija: la misma frase con la misma semilla da el mismo plan
// (en pruebas reales, con 0.25 y sin semilla, el mismo prompt salió una vez al aire libre
// y otra dentro de una mazmorra). Para variar, se cambia la semilla, no el azar del modelo.
export const DEFAULTS = Object.freeze({ temperature: 0.1, seed: 7, maxTokens: 1200, numCtx: 4096, keepAlive: '30m', timeoutMs: 120000 });
const KEEP_ALIVE_RE = /^(-1|0|\d{1,4}[smh])$/;
export class LocalError extends Error { constructor(message, status = 502, code = 'error') { super(message); this.status = status; this.code = code; } }

// ---------------- Seguridad: a qué servidores se permite hablar ----------------
// Siempre el propio equipo. Otro equipo de la red local solo si se autoriza
// expresamente con PIXELFORGE_OLLAMA_HOSTS (lista "host:puerto" separada por comas).
const LOOPBACK = new Set(['127.0.0.1', 'localhost', '[::1]', '::1']);
export function resolveUrl(input) {
  const raw = (input || process.env.OLLAMA_URL || DEFAULT_URL).trim();
  let u;
  try { u = new URL(/^https?:\/\//.test(raw) ? raw : 'http://' + raw); } catch { throw new LocalError('Dirección de Ollama no válida', 400, 'bad_url'); }
  if (!/^https?:$/.test(u.protocol) || u.username || u.password) throw new LocalError('Dirección de Ollama no válida', 400, 'bad_url');
  const allowed = (process.env.PIXELFORGE_OLLAMA_HOSTS || '').split(',').map((s) => s.trim().toLowerCase()).filter(Boolean);
  const hostPort = `${u.hostname}:${u.port || (u.protocol === 'https:' ? 443 : 80)}`.toLowerCase();
  if (!LOOPBACK.has(u.hostname) && !allowed.includes(hostPort)) throw new LocalError(`Por seguridad solo se permite Ollama en este equipo. Para usar otro equipo de la red, añádelo a PIXELFORGE_OLLAMA_HOSTS (${hostPort}).`, 403, 'host_not_allowed');
  return `${u.protocol}//${u.host}`;
}

async function call(base, path, { method = 'GET', body, timeoutMs = 4000, signal } = {}) {
  const ctl = new AbortController(), t = setTimeout(() => ctl.abort(), timeoutMs);
  signal?.addEventListener('abort', () => ctl.abort(), { once: true });
  try {
    const r = await fetch(base + path, { method, headers: body ? { 'Content-Type': 'application/json' } : undefined, body: body ? JSON.stringify(body) : undefined, signal: ctl.signal });
    const text = await r.text();
    let json = null; try { json = JSON.parse(text); } catch { /* respuesta no JSON */ }
    if (!r.ok) {
      const msg = json?.error || text.slice(0, 200);
      if (r.status === 404 && /model/i.test(msg)) throw new LocalError('El modelo no está instalado en Ollama', 404, 'model_missing');
      throw new LocalError('Ollama respondió con un error: ' + msg, 502, 'ollama_error');
    }
    return json;
  } catch (e) {
    if (e instanceof LocalError) throw e;
    if (e.name === 'AbortError') throw new LocalError(signal?.aborted ? 'Cancelado' : 'Ollama tardó demasiado en responder', 504, 'timeout');
    throw new LocalError('No se pudo conectar con Ollama. ¿Está instalado y en marcha?', 503, 'unreachable');
  } finally { clearTimeout(t); }
}

// ---------------- Sistema: memoria y GPU ----------------
let sysCache = null;
export async function systemInfo() {
  if (sysCache) return { ...sysCache, freeRAM: os.freemem() };
  const info = { platform: process.platform, arch: process.arch, cpus: os.cpus().length, cpu: os.cpus()[0]?.model || '', totalRAM: os.totalmem(), freeRAM: os.freemem(), gpu: null };
  if (process.platform === 'darwin' && process.arch === 'arm64') info.gpu = { name: 'Apple Silicon (memoria unificada)', vram: null, unified: true };
  else info.gpu = await new Promise((res) => {
    execFile('nvidia-smi', ['--query-gpu=name,memory.total', '--format=csv,noheader,nounits'], { timeout: 2500, windowsHide: true }, (err, out) => {
      if (err || !out) return res(null);
      const [name, mem] = out.trim().split('\n')[0].split(',').map((s) => s.trim());
      res({ name, vram: (+mem || 0) * 1024 * 1024 });
    });
  });
  sysCache = info;
  return info;
}

// ---------------- Modelos ----------------
// Calidad esperada por familia para esta tarea (JSON estructurado, catálogo
// cerrado, español). Es una orientación, no una medida: la medida real la da
// evaluateModel().
const FAMILY = [[/qwen3\.5/i, 3.4], [/qwen3/i, 3.2], [/qwen2\.5|qwen2/i, 3], [/gemma3|gemma-3/i, 2.6], [/llama3\.[123]|llama-3/i, 2.2], [/mistral|ministral/i, 2.2], [/phi-?4/i, 2.3], [/phi/i, 1.8], [/granite/i, 1.8], [/smollm/i, 1.2]];
const EMBED = /embed|bge|minilm|nomic-embed/i;
const paramsOf = (m) => { const v = parseFloat(String(m.details?.parameter_size || m.name.match(/(\d+(?:\.\d+)?)b/i)?.[1] || '0')); return /m$/i.test(m.details?.parameter_size || '') ? v / 1000 : v; };

export function scoreModel(m, sys) {
  const fam = FAMILY.find(([re]) => re.test(m.name + ' ' + (m.details?.family || '')))?.[1] ?? 1.5;
  const p = paramsOf(m);
  const mem = sys.gpu?.vram && !sys.gpu.unified ? Math.max(sys.gpu.vram, sys.totalRAM * 0.5) : sys.totalRAM;
  const need = (m.size || 0) * 1.25 + 0.6e9;                      // pesos + contexto aproximado
  const fits = need < mem * 0.7;
  const sizeScore = p <= 0 ? 0.5 : p < 1 ? 0.3 : p < 2 ? 0.75 : p <= 9 ? 1 : p <= 15 ? 0.95 : 0.85;
  const speed = sys.gpu?.vram || sys.gpu?.unified ? 1 : p <= 4 ? 1 : p <= 8 ? 0.8 : 0.55;
  return { fits, needBytes: Math.round(need), score: fits ? fam * sizeScore * speed : 0 };
}

// Sugerencia de descarga cuando no hay modelos adecuados (no se descarga nada).
export function suggestPull(sys) {
  const gb = sys.totalRAM / 1e9, vram = (sys.gpu?.vram || 0) / 1e9;
  if (vram >= 10 || (sys.gpu?.unified && gb >= 32)) return { model: 'qwen2.5:14b', approxGB: 9, why: 'GPU con 10 GB o más' };
  if (gb >= 24 && !vram) return { model: 'qwen2.5:7b', approxGB: 4.7, why: '24 GB de RAM o más sin GPU dedicada' };
  if (gb >= 8 || vram >= 4) return { model: 'qwen3.5:4b', approxGB: 3.4, why: '8–16 GB de RAM o GPU de 4–8 GB (cabe entero en la VRAM)' };
  return { model: 'qwen2.5:1.5b', approxGB: 1, why: 'menos de 8 GB de RAM' };
}

const showCache = new Map();
export async function status(urlInput) {
  const base = resolveUrl(urlInput), sys = await systemInfo();
  let version = null;
  try { version = (await call(base, '/api/version', { timeoutMs: 1500 }))?.version || 'desconocida'; }
  catch (e) { return { available: false, url: base, reason: e.message, code: e.code, system: sys, suggestion: suggestPull(sys) }; }
  const tags = await call(base, '/api/tags').catch(() => ({ models: [] }));
  const ps = await call(base, '/api/ps').catch(() => ({ models: [] }));
  const models = [];
  for (const m of (tags.models || []).filter((x) => !EMBED.test(x.name)).slice(0, 30)) {
    let caps = showCache.get(m.digest);
    if (!caps) { caps = (await call(base, '/api/show', { method: 'POST', body: { model: m.name }, timeoutMs: 4000 }).catch(() => null))?.capabilities || null; showCache.set(m.digest, caps); }
    const sc = scoreModel(m, sys);
    models.push({ name: m.name, size: m.size, family: m.details?.family || null, params: m.details?.parameter_size || null, quant: m.details?.quantization_level || null, capabilities: caps, loaded: (ps.models || []).some((x) => x.name === m.name), ...sc });
  }
  models.sort((a, b) => b.score - a.score);
  // Si el modelo por defecto está instalado y cabe, es el recomendado.
  const def = models.find((m) => m.name === DEFAULT_MODEL() && m.fits);
  const recommended = def?.name || models.find((m) => m.fits && (!m.capabilities || m.capabilities.includes('completion')))?.name || null;
  // Uso de GPU real según Ollama: fracción del modelo cargada en VRAM (lo que "ollama ps" muestra como "100% GPU").
  const running = (ps.models || []).map((m) => ({ name: m.name, size: m.size, sizeVram: m.size_vram, gpuShare: m.size ? Math.round((100 * (m.size_vram || 0)) / m.size) : null, expiresAt: m.expires_at || null }));
  return { available: true, url: base, version, models, recommended, defaultModel: DEFAULT_MODEL(), running, system: sys, suggestion: recommended ? null : suggestPull(sys) };
}

// Precarga el modelo en memoria (Ollama lo carga con una petición vacía) para
// que la primera generación no pague el tiempo de carga.
export async function warmup(body, signal) {
  const base = resolveUrl(body?.url);
  const model = typeof body?.model === 'string' && /^[\w.:\-\/]{1,100}$/.test(body.model) ? body.model : null;
  if (!model) throw new LocalError('Elige un modelo de Ollama', 400, 'no_model');
  const keepAlive = KEEP_ALIVE_RE.test(String(body?.keepAlive)) ? (body.keepAlive === '-1' ? -1 : body.keepAlive) : DEFAULTS.keepAlive;
  const t0 = Date.now();
  const r = await call(base, '/api/generate', { method: 'POST', body: { model, prompt: '', keep_alive: keepAlive, stream: false }, timeoutMs: 180000, signal });
  const ps = await call(base, '/api/ps').catch(() => ({ models: [] }));
  const m = (ps.models || []).find((x) => x.name === model);
  return { model, ms: Date.now() - t0, loadMs: r?.load_duration ? Math.round(r.load_duration / 1e6) : null, gpuShare: m?.size ? Math.round((100 * (m.size_vram || 0)) / m.size) : null, sizeVram: m?.size_vram ?? null };
}

const versionAtLeast = (v, [a, b]) => { const m = String(v).match(/(\d+)\.(\d+)/); return m ? +m[1] > a || (+m[1] === a && +m[2] >= b) : false; };

// ---------------- Interpretación ----------------
// body: { prompt | raw+instruction, model, temperature, seed, maxTokens, timeoutMs, prefs, url }
export async function interpret(body, signal) {
  const base = resolveUrl(body?.url);
  const model = typeof body?.model === 'string' && /^[\w.:\-\/]{1,100}$/.test(body.model) ? body.model : null;
  if (!model) throw new LocalError('Elige un modelo de Ollama', 400, 'no_model');
  const prompt = String(body?.prompt || '').trim().slice(0, 600);
  const instruction = String(body?.instruction || '').trim().slice(0, 300);
  const raw = body?.raw && typeof body.raw === 'object' ? body.raw : null;
  if (!prompt && !(raw && instruction)) throw new LocalError('Falta la descripción', 400, 'no_prompt');
  const temperature = Math.max(0, Math.min(1.5, Number.isFinite(+body?.temperature) ? +body.temperature : DEFAULTS.temperature));
  const seed = Number.isInteger(+body?.seed) && body?.seed !== null && body?.seed !== '' ? Math.abs(+body.seed) % 2147483647 : DEFAULTS.seed;
  const maxTokens = Math.max(200, Math.min(4096, +body?.maxTokens || DEFAULTS.maxTokens));
  const timeoutMs = Math.max(1000, Math.min(600000, +body?.timeoutMs || DEFAULTS.timeoutMs));
  let numCtx = Math.max(2048, Math.min(32768, +body?.numCtx || DEFAULTS.numCtx));
  const keepAlive = KEEP_ALIVE_RE.test(String(body?.keepAlive)) ? (body.keepAlive === '-1' ? -1 : body.keepAlive) : DEFAULTS.keepAlive;
  const prefs = ['strict', 'balanced', 'creative'].includes(body?.prefs) ? body.prefs : 'balanced';
  const ver = (await call(base, '/api/version', { timeoutMs: 2000 }))?.version || '0';
  // Salida estructurada: esquema JSON desde Ollama 0.5; antes, solo "json".
  const format = versionAtLeast(ver, [0, 5]) ? PLAN_SCHEMA : 'json';
  const caps = (await call(base, '/api/show', { method: 'POST', body: { model }, timeoutMs: 8000 }).catch((e) => { if (e.code === 'model_missing') throw e; return null; }))?.capabilities || [];
  const messages = buildLocalMessages({ prompt, raw: raw ? sanitizeEditRaw(raw) : null, instruction, prefs });
  // Contexto: si el prompt (≈3 caracteres por token, estimación prudente) más
  // la respuesta no caben, Ollama recortaría el principio (las instrucciones)
  // sin avisar. En ese caso se amplía lo justo y se informa.
  const compat = [];
  const estPrompt = Math.ceil(messages.reduce((a, m) => a + m.content.length, 0) / 3);
  if (estPrompt + maxTokens > numCtx) { const need = Math.ceil((estPrompt + maxTokens) / 1024) * 1024; compat.push(`Contexto ampliado de ${numCtx} a ${need} tokens para que quepa el prompt`); numCtx = need; }
  const req = { model, messages, stream: false, format, keep_alive: keepAlive, options: { temperature, seed, num_predict: maxTokens, num_ctx: numCtx } };
  if (format === 'json') compat.push(`Ollama ${ver} no admite esquemas: se usa format "json"`);
  if (caps.includes('thinking')) req.think = false;          // solo si el modelo lo admite
  const t0 = Date.now();
  let res;
  try { res = await call(base, '/api/chat', { method: 'POST', body: req, timeoutMs, signal }); }
  catch (e) {
    // Incompatibilidades reales de una versión concreta: se reintenta sin la opción y se registra.
    if (e.code !== 'ollama_error') throw e;
    if (req.think !== undefined && /think/i.test(e.message)) { delete req.think; compat.push('El modelo rechazó la opción think: se envía sin ella'); }
    else if (typeof req.format === 'object' && /format|schema|grammar/i.test(e.message)) { req.format = 'json'; compat.push('Ollama rechazó el esquema JSON: se usa format "json" (' + e.message.slice(0, 80) + ')'); }
    else throw e;
    res = await call(base, '/api/chat', { method: 'POST', body: req, timeoutMs, signal });
  }
  let out = parseOut(res?.message?.content);
  let attempts = 1;
  const firstText = res?.message?.content;
  if (!out) {   // un reintento devolviendo el error al modelo
    attempts++;
    req.messages = [...messages, { role: 'assistant', content: String(res?.message?.content || '').slice(0, 2000) }, { role: 'user', content: 'Eso no es un JSON válido con el formato pedido. Devuelve solo el JSON.' }];
    res = await call(base, '/api/chat', { method: 'POST', body: req, timeoutMs, signal });
    out = parseOut(res?.message?.content);
  }
  if (!out) throw new LocalError('El modelo local no devolvió un plan válido', 502, 'invalid_output');
  const ns = (v) => (v ? Math.round(v / 1e6) : null);
  if (res.prompt_eval_count && res.prompt_eval_count >= numCtx - 8) compat.push(`El prompt ocupó todo el contexto (${res.prompt_eval_count} tokens): puede haberse recortado`);
  return {
    ...(() => { if (!prompt) return { raw: out.raw, corrections: [] }; const f = applyCertainties(out.raw, prompt, prefs, parsePrompt); return { raw: f.raw, corrections: f.fixes }; })(),
    warnings: out.warnings, model, attempts, compat, numCtx, temperature, seed,
    ...(body?.debug ? { modelText: [firstText, attempts > 1 ? res?.message?.content : null].filter(Boolean) } : {}),
    timings: { totalMs: Date.now() - t0, loadMs: ns(res.load_duration), promptTokens: res.prompt_eval_count || null, outputTokens: res.eval_count || null, tokensPerSecond: res.eval_count && res.eval_duration ? +(res.eval_count / (res.eval_duration / 1e9)).toFixed(1) : null },
  };
}
function parseOut(text) {
  if (typeof text !== 'string') return null;
  const s = text.indexOf('{'), e = text.lastIndexOf('}');
  if (s < 0 || e <= s) return null;
  let o; try { o = JSON.parse(text.slice(s, e + 1)); } catch { return null; }
  const r = sanitizePlan(o);
  return r && (r.raw.subjects.length + r.raw.structures.length + r.raw.flora.length || r.raw.biome || r.raw.setting) ? r : null;
}
// El "raw" de una edición viene del navegador: se reconstruye pasando por el
// mismo saneado que la salida del modelo (nunca se reenvía texto arbitrario).
function sanitizeEditRaw(raw) {
  const plan = { subjects: [], structures: [], props: [] };
  const conv = (x) => (x && typeof x === 'object' ? { id: x.kit ? 'custom' : x.id, name: x.name, kit: x.kit, color: x.color, n: x.n, size: x.size, side: x.side, depth: x.depth, action: x.action, ...(x.rel ? { relation: { type: x.rel.type, to: x.rel.of } } : {}) } : null);
  for (const [a, b] of [['subjects', 'subjects'], ['structures', 'structures'], ['flora', 'props']]) plan[b] = (Array.isArray(raw[a]) ? raw[a] : []).map(conv).filter(Boolean);
  Object.assign(plan, { biome: raw.biome, time: raw.time, weather: raw.weather, sky: raw.celestial, effects: raw.fx, palette: raw.theme, focus: raw.focus, battle: raw.battle, setting: raw.setting === 'interior' ? 'interior' : undefined, room: raw.room, town: raw.town?.style ? { style: raw.town.style, size: raw.town.size } : undefined, features: raw.features });
  return sanitizePlan(plan)?.raw || null;
}

// ---------------- Evaluación real de un modelo ----------------
// Tres descripciones redactadas con sinónimos que el motor procedural NO
// conoce: miden si el modelo entiende lo que el parser no entiende. Se mide
// JSON válido, elementos esperados presentes, tiempo y velocidad.
export const EVAL_SET = [
  { prompt: 'Un paladín de armadura dorada combatiendo a una bestia alada que escupe fuego delante de una fortaleza al ocaso', expect: ['knight', 'dragon', 'castle'] },
  { prompt: 'Un felino rayado acechando entre la espesura junto a un salto de agua', expect: ['tiger', 'waterfall'] },
  { prompt: 'Un archimago estudiando grimorios a la luz de las velas en una vieja sala de lectura', expect: ['interior', 'wizard', 'candle'] },
];
export async function evaluateModel(body, signal) {
  const results = [];
  for (const t of EVAL_SET) {
    const t0 = Date.now();
    try {
      const r = await interpret({ ...body, prompt: t.prompt, temperature: 0 }, signal);
      const spec = finalize(r.raw);
      const have = new Set([...spec.subjects, ...spec.structures, ...spec.flora].map((e) => e.id).concat(spec.features, spec.setting === 'interior' ? ['interior'] : []));
      const found = t.expect.filter((x) => have.has(x));
      results.push({ prompt: t.prompt, ok: true, recall: found.length / t.expect.length, missing: t.expect.filter((x) => !have.has(x)), ms: Date.now() - t0, tokensPerSecond: r.timings.tokensPerSecond, attempts: r.attempts });
    } catch (e) {
      if (signal?.aborted) throw e;
      results.push({ prompt: t.prompt, ok: false, error: e.message, ms: Date.now() - t0 });
    }
  }
  const ok = results.filter((r) => r.ok);
  return {
    model: body.model, results,
    validJson: ok.length / results.length,
    recall: ok.length ? ok.reduce((a, r) => a + r.recall, 0) / ok.length : 0,
    avgMs: Math.round(results.reduce((a, r) => a + r.ms, 0) / results.length),
    baselineRecall: EVAL_SET.reduce((a, t) => { const s = parsePrompt(t.prompt).spec; const have = new Set([...s.subjects, ...s.structures, ...s.flora].map((e) => e.id).concat(s.features, s.setting === 'interior' ? ['interior'] : [])); return a + t.expect.filter((x) => have.has(x)).length / t.expect.length; }, 0) / EVAL_SET.length,
  };
}
