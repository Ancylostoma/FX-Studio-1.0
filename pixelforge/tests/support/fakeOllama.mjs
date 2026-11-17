// Simulador del contrato HTTP de Ollama para pruebas. NO es un modelo: sus
// respuestas son fijas por palabras clave. Valida las peticiones como lo
// haría Ollama (modelo inexistente → 404, "think" en un modelo que no lo
// admite → 400) y registra todo lo recibido.
import { createServer } from 'node:http';

export const FAKE_MODELS = [
  { name: 'qwen2.5:3b', size: 1_929_903_008, digest: 'a1', details: { family: 'qwen2', parameter_size: '3.1B', quantization_level: 'Q4_K_M' }, capabilities: ['completion', 'tools'] },
  { name: 'qwen3:1.7b', size: 1_100_000_000, digest: 'b2', details: { family: 'qwen3', parameter_size: '1.7B', quantization_level: 'Q4_K_M' }, capabilities: ['completion', 'thinking'] },
  { name: 'llama3.2:1b', size: 770_928_480, digest: 'c3', details: { family: 'llama', parameter_size: '1.2B', quantization_level: 'Q4_0' }, capabilities: ['completion'] },
  { name: 'nomic-embed-text', size: 274_000_000, digest: 'd4', details: { family: 'nomic-bert', parameter_size: '137M' }, capabilities: ['embedding'] },
];

// Respuesta "de director artístico" simulada según palabras clave.
export function fakePlan(text) {
  const t = text.toLowerCase();
  if (t.includes('plan actual')) {               // edición
    const cur = JSON.parse(text.slice(text.indexOf('{'), text.lastIndexOf('}') + 1));
    if (t.includes('púrpura') || t.includes('purpura')) cur.skyColor = 'purple';
    if (t.includes('luna')) cur.sky = [...new Set([...(cur.sky || []), 'moon'])];
    return cur;
  }
  if (t.includes('hada')) return { subjects: [{ id: 'custom', name: 'hada', kit: { base: 'humanoid', body: 'pink', robe: true, wings: 'fairy', glow: true }, size: -1, fly: true, relation: { type: 'on', to: 'pond' } }], props: [{ id: 'flower', n: 3, side: 'left' }], features: ['lake'], biome: 'forest', time: 'night', effects: ['fireflies'], palette: 'fantasy', focus: 'hada', unsupported: ['nenúfares → flores'] };
  if ((t.includes('caballero') && t.includes('dragón')) || t.includes('paladín')) return { subjects: [{ id: 'knight', armor: 'gold', weapon: 'sword', action: 'attack', facing: 'right' }, { id: 'dragon', color: 'red', fly: true, breathesFire: true, action: 'attack' }], structures: [{ id: 'castle', depth: 'back' }], biome: 'meadow', time: 'sunset', battle: true, palette: 'medieval', focus: 'knight' };
  if (t.includes('tigre') || t.includes('felino')) return { subjects: [{ id: 'tiger', action: 'walk' }], features: ['waterfall'], biome: 'forest', palette: 'tropical', focus: 'tiger' };
  if (t.includes('biblioteca') || t.includes('archimago')) return { subjects: [{ id: 'wizard', action: 'cast' }], props: [{ id: 'candle' }], setting: 'interior', room: 'library', palette: 'fantasy', focus: 'wizard' };
  if (t.includes('catedral')) return { subjects: [{ id: 'custom', name: 'gárgola', kit: { base: 'humanoid', body: 'stone', skin: 'stone', wings: 'bat', horns: true } }], structures: [{ id: 'cathedral' }], biome: 'town', town: { style: 'medieval', size: 2 }, weather: ['storm'], time: 'night', palette: 'dark', focus: 'cathedral' };
  return { subjects: [{ id: 'character' }], biome: 'meadow', time: 'day' };
}

export function startFakeOllama({ version = '0.12.3', models = FAKE_MODELS, invalidFirst = false, delayMs = 0, port = 0 } = {}) {
  const log = [];
  let bad = invalidFirst;
  const send = (res, code, obj) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(obj)); };
  const server = createServer((req, res) => {
    let body = ''; req.on('data', (c) => (body += c)); req.on('end', () => setTimeout(() => {
      const b = body ? JSON.parse(body) : null; log.push({ method: req.method, path: req.url, body: b });
      if (req.url === '/api/version') return send(res, 200, { version });
      if (req.url === '/api/tags') return send(res, 200, { models: models.map(({ capabilities, ...m }) => m) });
      if (req.url === '/api/ps') return send(res, 200, { models: [] });
      const m = b && models.find((x) => x.name === b.model);
      if (req.url === '/api/show') return m ? send(res, 200, { capabilities: m.capabilities, details: m.details }) : send(res, 404, { error: `model "${b?.model}" not found` });
      if (req.url === '/api/chat') {
        if (!m) return send(res, 404, { error: `model "${b?.model}" not found, try pulling it first` });
        if (b.think !== undefined && !m.capabilities.includes('thinking')) return send(res, 400, { error: `"${m.name}" does not support thinking` });
        const user = [...b.messages].reverse().find((x) => x.role === 'user' && !x.content.startsWith('Eso no es')).content;
        const content = bad ? 'Claro, aquí tienes la escena: un dragón…' : JSON.stringify(fakePlan(user));
        bad = false;
        return send(res, 200, { model: m.name, message: { role: 'assistant', content }, done: true, load_duration: 120e6, prompt_eval_count: 900, eval_count: 140, eval_duration: 4e9 });
      }
      send(res, 404, { error: 'not found' });
    }, req.url === '/api/chat' ? delayMs : 0));
  });
  return new Promise((resolve) => server.listen(port, '127.0.0.1', () => resolve({ url: `http://127.0.0.1:${server.address().port}`, log, close: () => new Promise((r) => { server.closeAllConnections?.(); server.close(r); }) })));
}
