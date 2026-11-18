// Servidor local sin dependencias propias: sirve la aplicación estática y,
// si hay clave configurada, expone el adaptador de IA.
//   node server/server.mjs      (o: npm start)
import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { generateScene, interpretPrompt, aiConfigured, modelId, interpretModelId, AdapterError } from './aiAdapter.mjs';
import * as local from './ollamaAdapter.mjs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

// Carga .env sin dependencias (solo claves no definidas ya en el entorno).
const envFile = path.join(ROOT, '.env');
if (existsSync(envFile)) {
  for (const line of readFileSync(envFile, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m && m[2] && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}

const PORT = +process.env.PORT || 8787;
const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8', '.png': 'image/png', '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.webmanifest': 'application/manifest+json' };
// Solo se sirve lo que la aplicación necesita; nunca .env, server/ ni node_modules/.
const PUBLIC = [/^\/index\.html$/, /^\/styles\.css$/, /^\/manifest\.json$/, /^\/sw\.js$/, /^\/src\/[\w./-]+\.js$/, /^\/icons\/[\w.-]+\.png$/, /^\/docs\/examples\/[\w.-]+\.png$/];
const HEADERS = {
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'Content-Security-Policy': "default-src 'self'; img-src 'self' blob: data:; style-src 'self' 'unsafe-inline'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
};

// Límite simple por IP para el endpoint de IA (cada llamada cuesta dinero).
const hits = new Map();
const RATE = { max: 12, windowMs: 60_000 };
function limited(ip) {
  const now = Date.now(), list = (hits.get(ip) || []).filter((t) => now - t < RATE.windowMs);
  list.push(now); hits.set(ip, list);
  if (hits.size > 5000) hits.clear();
  return list.length > RATE.max;
}

const json = (res, status, body) => { res.writeHead(status, { ...HEADERS, 'Content-Type': MIME['.json'], 'Cache-Control': 'no-store' }); res.end(JSON.stringify(body)); };

function readBody(req, max = 512 * 1024) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => { size += c.length; if (size > max) { reject(new AdapterError('Cuerpo demasiado grande', 413)); req.destroy(); } else chunks.push(c); });
    req.on('end', () => { try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}')); } catch { reject(new AdapterError('JSON inválido', 400)); } });
    req.on('error', reject);
  });
}

const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    let p = decodeURIComponent(url.pathname);
    if (p === '/api/status' && req.method === 'GET') return json(res, 200, { ai: aiConfigured(), model: aiConfigured() ? modelId() : null, interpretModel: aiConfigured() ? interpretModelId() : null, local: true });
    // IA local (Ollama): sin claves ni servicios externos.
    if (p.startsWith('/api/local/')) {
      const origin = req.headers.origin;
      if (origin && new URL(origin).host !== req.headers.host) return json(res, 403, { error: 'Origen no permitido' });
      if (p === '/api/local/status' && req.method === 'GET') return json(res, 200, await local.status(url.searchParams.get('url')));
      if (req.method !== 'POST') return json(res, 405, { error: 'Método no permitido' });
      if (!/^application\/json/.test(req.headers['content-type'] || '')) return json(res, 415, { error: 'Se esperaba application/json' });
      const body = await readBody(req);
      const ctl = new AbortController(); req.on('close', () => { if (!res.writableEnded) ctl.abort(); });
      if (p === '/api/local/interpret') return json(res, 200, await local.interpret(body, ctl.signal));
      if (p === '/api/local/evaluate') return json(res, 200, await local.evaluateModel(body, ctl.signal));
      if (p === '/api/local/warmup') return json(res, 200, await local.warmup(body, ctl.signal));
      return json(res, 404, { error: 'No encontrado' });
    }
    if (p === '/api/pixelscene' || p === '/api/interpret') {
      if (req.method !== 'POST') return json(res, 405, { error: 'Método no permitido' });
      // Solo peticiones del mismo origen (evita que otras webs gasten tu clave).
      const origin = req.headers.origin;
      if (origin && new URL(origin).host !== req.headers.host) return json(res, 403, { error: 'Origen no permitido' });
      if (!/^application\/json/.test(req.headers['content-type'] || '')) return json(res, 415, { error: 'Se esperaba application/json' });
      if (limited(req.socket.remoteAddress || '?')) return json(res, 429, { error: 'Demasiadas peticiones; espera un minuto' });
      const out = await (p === '/api/interpret' ? interpretPrompt : generateScene)(await readBody(req));
      return json(res, 200, out);
    }
    if (req.method !== 'GET' && req.method !== 'HEAD') return json(res, 405, { error: 'Método no permitido' });
    if (p === '/') p = '/index.html';
    if (!PUBLIC.some((re) => re.test(p)) || p.includes('..')) return json(res, 404, { error: 'No encontrado' });
    const file = path.join(ROOT, p);
    if (!file.startsWith(ROOT + path.sep)) return json(res, 404, { error: 'No encontrado' });
    const st = await stat(file).catch(() => null);
    if (!st?.isFile()) return json(res, 404, { error: 'No encontrado' });
    res.writeHead(200, { ...HEADERS, 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
    res.end(req.method === 'HEAD' ? undefined : await readFile(file));
  } catch (e) {
    if (e instanceof AdapterError) return json(res, e.status, { error: e.message });
    if (e instanceof local.LocalError) return json(res, e.status, { error: e.message, code: e.code });
    console.error(e);
    json(res, 500, { error: 'Error interno' });
  }
});

// Solo este equipo por defecto. Para abrirlo a la red local (p. ej. el móvil)
// hay que pedirlo expresamente con PIXELFORGE_HOST=0.0.0.0 en .env.
const HOST = process.env.PIXELFORGE_HOST || '127.0.0.1';
server.on('error', (e) => {
  if (e.code === 'EADDRINUSE') { console.error(`El puerto ${PORT} ya está en uso: probablemente PixelForge ya está abierto. Abre http://localhost:${PORT} (no se inicia un segundo servidor).`); process.exit(2); }
  throw e;
});
server.listen(PORT, HOST, () => {
  console.log(`PixelForge Studio → http://localhost:${PORT}${HOST !== '127.0.0.1' ? ` (también en la red local: ${HOST})` : ''}`);
  local.status().then((s) => console.log(s.available ? `Ollama ${s.version} detectado en ${s.url} · ${s.models.length} modelo(s)${s.recommended ? ' · recomendado: ' + s.recommended : ''}` : `Ollama no detectado (${s.reason}). El motor procedural funciona igualmente.`)).catch(() => {});
  if (aiConfigured()) console.log(`Claude (opcional) configurado: ${modelId()}`);
});
