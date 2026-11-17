// Adaptador de IA local con Ollama. Habla con la interfaz HTTP local de
// Ollama (por defecto http://127.0.0.1:11434): no usa claves ni servicios en
// la nube. El modelo devuelve un plan de dirección artística (datos, nunca
// código) que se sanea y valida antes de llegar al motor gráfico.
import os from 'node:os';
import { execFile } from 'node:child_process';
import { PLAN_SCHEMA, buildLocalMessages, sanitizePlan } from '../src/ai/planSchema.js';
import { finalize, parsePrompt } from '../src/generator/promptParser.js';

export const DEFAULT_URL = 'http://127.0.0.1:11434';
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
const FAMILY = [[/qwen3/i, 3.2], [/qwen2\.5|qwen2/i, 3], [/gemma3|gemma-3/i, 2.6], [/llama3\.[123]|llama-3/i, 2.2], [/mistral|ministral/i, 2.2], [/phi-?4/i, 2.3], [/phi/i, 1.8], [/granite/i, 1.8], [/smollm/i, 1.2]];
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
  if (vram >= 10 || (sys.gpu?.unified && gb >= 32)) return { model: 'qwen2.5:14b', approxGB: 9, why: 'GPU con memoria suficiente' };
  if (gb >= 16 || vram >= 6) return { model: 'qwen2.5:7b', approxGB: 4.7, why: '16 GB de RAM o más' };
  if (gb >= 8) return { model: 'qwen2.5:3b', approxGB: 1.9, why: '8–16 GB de RAM' };
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
  const recommended = models.find((m) => m.fits && (!m.capabilities || m.capabilities.includes('completion')))?.name || null;
  return { available: true, url: base, version, models, recommended, running: (ps.models || []).map((m) => ({ name: m.name, sizeVram: m.size_vram, size: m.size })), system: sys, suggestion: recommended ? null : suggestPull(sys) };
}

const versionAtLeast = (v, [a, b]) => { const m = String(v).match(/(\d+)\.(\d+)/); return m ? +m[1] > a || (+m[1] === a && +m[2] >= b) : false; };

// ---------------- Interpretación ----------------
// body: { prompt | raw+instruction, model, temperature, maxTokens, timeoutMs, prefs, url }
export async function interpret(body, signal) {
  const base = resolveUrl(body?.url);
  const model = typeof body?.model === 'string' && /^[\w.:\-\/]{1,100}$/.test(body.model) ? body.model : null;
  if (!model) throw new LocalError('Elige un modelo de Ollama', 400, 'no_model');
  const prompt = String(body?.prompt || '').trim().slice(0, 600);
  const instruction = String(body?.instruction || '').trim().slice(0, 300);
  const raw = body?.raw && typeof body.raw === 'object' ? body.raw : null;
  if (!prompt && !(raw && instruction)) throw new LocalError('Falta la descripción', 400, 'no_prompt');
  const temperature = Math.max(0, Math.min(1.5, Number.isFinite(+body?.temperature) ? +body.temperature : 0.2));
  const maxTokens = Math.max(200, Math.min(4096, +body?.maxTokens || 1200));
  const timeoutMs = Math.max(1000, Math.min(600000, +body?.timeoutMs || 120000));
  const prefs = ['strict', 'balanced', 'creative'].includes(body?.prefs) ? body.prefs : 'balanced';
  const ver = (await call(base, '/api/version', { timeoutMs: 2000 }))?.version || '0';
  // Salida estructurada: esquema JSON desde Ollama 0.5; antes, solo "json".
  const format = versionAtLeast(ver, [0, 5]) ? PLAN_SCHEMA : 'json';
  const caps = (await call(base, '/api/show', { method: 'POST', body: { model }, timeoutMs: 8000 }).catch((e) => { if (e.code === 'model_missing') throw e; return null; }))?.capabilities || [];
  const messages = buildLocalMessages({ prompt, raw: raw ? sanitizeEditRaw(raw) : null, instruction, prefs });
  const req = { model, messages, stream: false, format, keep_alive: '15m', options: { temperature, num_predict: maxTokens, num_ctx: 8192 } };
  if (caps.includes('thinking')) req.think = false;          // solo si el modelo lo admite
  const t0 = Date.now();
  let res = await call(base, '/api/chat', { method: 'POST', body: req, timeoutMs, signal });
  let out = parseOut(res?.message?.content);
  let attempts = 1;
  if (!out) {   // un reintento devolviendo el error al modelo
    attempts++;
    req.messages = [...messages, { role: 'assistant', content: String(res?.message?.content || '').slice(0, 2000) }, { role: 'user', content: 'Eso no es un JSON válido con el formato pedido. Devuelve solo el JSON.' }];
    res = await call(base, '/api/chat', { method: 'POST', body: req, timeoutMs, signal });
    out = parseOut(res?.message?.content);
  }
  if (!out) throw new LocalError('El modelo local no devolvió un plan válido', 502, 'invalid_output');
  const ns = (v) => (v ? Math.round(v / 1e6) : null);
  return {
    raw: out.raw, warnings: out.warnings, model, attempts,
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
