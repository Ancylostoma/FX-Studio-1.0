// Servidor local de LinguaBridge: sirve la interfaz, expone el motor por HTTP y
// hace de puente con Ollama (el navegador no puede hablar con Ollama directamente
// por CORS). Solo escucha en 127.0.0.1 salvo que se cambie LINGUABRIDGE_HOST.
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync, readFileSync } from 'node:fs';
import { extname, join, normalize, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { paraphrase, createOllamaProvider, createOpenAICompatibleProvider, VERSION } from '../src/engine/index.js';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');

// .env opcional (sin dependencias)
const envFile = join(ROOT, '.env');
if (existsSync(envFile)) {
  for (const line of readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/);
    if (m && !(m[1] in process.env)) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}

const PORT = Number(process.env.LINGUABRIDGE_PORT || 8788);
const HOST = process.env.LINGUABRIDGE_HOST || '127.0.0.1';
const LOOPBACK = new Set(['127.0.0.1', 'localhost', '::1', '[::1]']);

export function makeProvider(model) {
  if (process.env.LINGUABRIDGE_LLM === 'openai') {
    return createOpenAICompatibleProvider({
      baseUrl: process.env.LINGUABRIDGE_OPENAI_URL || 'http://127.0.0.1:1234/v1',
      model: model || process.env.LINGUABRIDGE_OPENAI_MODEL || 'local-model',
      apiKey: process.env.LINGUABRIDGE_OPENAI_KEY || '',
    });
  }
  const url = new URL(process.env.OLLAMA_URL || 'http://127.0.0.1:11434');
  const allowed = (process.env.LINGUABRIDGE_OLLAMA_HOSTS || '').split(',').map((s) => s.trim().toLowerCase()).filter(Boolean);
  if (!LOOPBACK.has(url.hostname) && !allowed.includes(url.host.toLowerCase()))
    throw Object.assign(new Error(`Por seguridad, Ollama debe estar en este equipo. Para otro equipo de la red añade ${url.host} a LINGUABRIDGE_OLLAMA_HOSTS.`), { status: 403 });
  return createOllamaProvider({ baseUrl: url.origin, model: model || process.env.LINGUABRIDGE_OLLAMA_MODEL || 'qwen3.5:4b' });
}

const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.svg': 'image/svg+xml', '.ico': 'image/x-icon' };
const STATIC_DIRS = ['ui', 'src'];

function send(res, status, body, type = 'application/json; charset=utf-8') {
  res.writeHead(status, { 'Content-Type': type, 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
  res.end(typeof body === 'string' || Buffer.isBuffer(body) ? body : JSON.stringify(body));
}
async function readBody(req, limit = 1_000_000) {
  let size = 0; const chunks = [];
  for await (const c of req) { size += c.length; if (size > limit) throw Object.assign(new Error('Texto demasiado largo'), { status: 413 }); chunks.push(c); }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}'); } catch { throw Object.assign(new Error('JSON no válido'), { status: 400 }); }
}

export function createServer() {
  return http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, 'http://x');
      const p = url.pathname;
      if (req.method === 'GET' && p === '/api/health') return send(res, 200, { ok: true, version: VERSION });
      if (req.method === 'GET' && p === '/api/llm/status') {
        const prov = makeProvider(url.searchParams.get('model') || undefined);
        return send(res, 200, { provider: prov.name, model: prov.model, ...(await prov.status()) });
      }
      if (req.method === 'POST' && p === '/api/llm/generate') {
        const b = await readBody(req);
        if (typeof b.system !== 'string' || typeof b.prompt !== 'string') return send(res, 400, { error: 'Faltan system/prompt' });
        const prov = makeProvider(b.model);
        const text = await prov.generate({ system: b.system, prompt: b.prompt, temperature: Number(b.temperature ?? 0.2), seed: b.seed, json: b.json !== false });
        return send(res, 200, { text });
      }
      if (req.method === 'POST' && p === '/api/paraphrase') {
        const b = await readBody(req);
        if (typeof b.text !== 'string') return send(res, 400, { error: 'Falta el texto' });
        const opts = { lang: b.lang, level: b.level, mode: b.mode, seed: b.seed, protectedTerms: b.protectedTerms };
        if (b.engine === 'llm' || b.engine === 'auto') {
          const prov = makeProvider(b.model);
          const st = await prov.status();
          if (st.available) opts.llm = { provider: prov };
          else if (b.engine === 'llm') return send(res, 503, { error: 'El modelo de lenguaje no está disponible: ' + (st.error || '') });
        }
        return send(res, 200, await paraphrase(b.text, opts));
      }
      if (req.method !== 'GET') return send(res, 405, { error: 'Método no permitido' });
      // Estáticos
      let file = p === '/' ? '/ui/index.html' : p;
      file = normalize(decodeURIComponent(file)).replace(/^([/\\])+/, '');
      if (!STATIC_DIRS.some((d) => file === d || file.startsWith(d + '/') || file.startsWith(d + '\\'))) return send(res, 404, { error: 'No encontrado' });
      const abs = resolve(ROOT, file);
      if (!abs.startsWith(ROOT)) return send(res, 403, { error: 'Prohibido' });
      const data = await readFile(abs).catch(() => null);
      if (!data) return send(res, 404, { error: 'No encontrado' });
      return send(res, 200, data, MIME[extname(abs)] || 'application/octet-stream');
    } catch (e) {
      return send(res, e.status || 500, { error: e.message || 'Error interno' });
    }
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  createServer().listen(PORT, HOST, () => {
    console.log(`LinguaBridge AI ${VERSION} · http://${HOST === '0.0.0.0' ? 'localhost' : HOST}:${PORT}`);
    console.log('Modelo de lenguaje opcional: ' + (process.env.LINGUABRIDGE_LLM === 'openai' ? 'servidor compatible con OpenAI' : `Ollama en ${process.env.OLLAMA_URL || 'http://127.0.0.1:11434'}`));
  });
}
