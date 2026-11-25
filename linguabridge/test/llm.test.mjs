// Ruta con modelo de lenguaje: se prueba con proveedores simulados y con un
// servidor HTTP que imita la API de Ollama (en este entorno no hay Ollama real).
import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { paraphrase, createOllamaProvider } from '../src/engine/index.js';
import { parseModelText } from '../src/engine/llm/prompt.js';
import { ES_CLINICO, EN_CLINICO } from './fixtures.mjs';

const inputOf = (prompt) => prompt.replace(/^(Texto|Text):\n/, '').split('\n\n')[0];
const fake = (fn, name = 'fake') => {
  const calls = [];
  return { name, model: 'fake-1', calls, status: async () => ({ available: true }), generate: async (req) => { calls.push(req); return fn(inputOf(req.prompt), req, calls.length); } };
};
const good = (s) => JSON.stringify({ text: s.replace('Sin embargo,', 'No obstante,').replace('However,', 'Nevertheless,').replace(/Con el objetivo de/, 'Para').replace(/In order to/, 'To') });

test('acepta una reformulación válida y conserva todo lo protegido', async () => {
  for (const [lang, t] of [['es', ES_CLINICO], ['en', EN_CLINICO]]) {
    const p = fake(good);
    const r = await paraphrase(t, { lang, level: 3, llm: { provider: p } });
    assert.ok(r.llm.accepted >= 1, JSON.stringify(r.warnings));
    assert.ok(r.integrity.ok);
    assert.match(r.engine, /híbrido/);
    for (const s of r.protected) assert.ok(r.result.includes(s.text), s.text);
    // el modelo nunca ve el texto protegido: solo marcadores
    for (const c of p.calls) {
      assert.ok(!/García et al|Smith et al|0,07|\.07|mmHg|doi/.test(c.prompt), 'filtró datos protegidos al modelo');
      assert.match(c.prompt, /⟦\d+⟧/);
    }
    assert.equal(p.calls.filter((c) => /Referencias|References/.test(c.prompt)).length, 0, 'la lista de referencias no se envía');
  }
});

const BAD = {
  'pierde un marcador': (s) => JSON.stringify({ text: s.replace(/⟦1⟧/, '') }),
  'inventa una cifra': (s) => JSON.stringify({ text: s + ' El 45 % mejoró.' }),
  'inventa una cita': (s) => JSON.stringify({ text: s.replace(/\.$/, ' (Pérez, 2019).') }),
  'quita una negación': (s) => JSON.stringify({ text: s.replace(/\bno\b/, '').replace(/\bnot\b/, '') }),
  'añade un autor': (s) => JSON.stringify({ text: s.replace(/\.\s/, ', según Gómez. ') }),
  'cambia la certeza': (s) => JSON.stringify({ text: s.replace(/podría/, 'va a').replace(/\bmay\b/, 'will') }),
  'marcador inexistente': (s) => JSON.stringify({ text: s + ' ⟦99⟧' }),
  'texto vacío': () => JSON.stringify({ text: '' }),
};
for (const [name, fn] of Object.entries(BAD)) {
  test(`rechaza al modelo si ${name} y vuelve a las reglas`, async () => {
    for (const [lang, t] of [['es', ES_CLINICO], ['en', EN_CLINICO]]) {
      const r = await paraphrase(t, { lang, level: 4, seed: 3, llm: { provider: fake(fn) } });
      assert.equal(r.llm.accepted, 0, `${lang}: ${r.result}`);
      assert.ok(r.integrity.ok);
      assert.ok(r.warnings.some((w) => /descartó/.test(w)));
      assert.ok(!/Pérez, 2019|Gómez|45 %/.test(r.result));
    }
  });
}

test('reintenta una vez con indicaciones y acepta si se corrige', async () => {
  const p = fake((s, req, n) => (n % 2 === 1 ? JSON.stringify({ text: s.replace(/⟦1⟧/, '') }) : good(s)));
  const r = await paraphrase(ES_CLINICO, { lang: 'es', level: 3, llm: { provider: p } });
  assert.equal(r.llm.accepted, 1);
  assert.match(p.calls[1].prompt, /se rechazó/);
  assert.equal(p.calls[1].temperature, 0);
});

test('si el proveedor falla, el resultado es el de reglas', async () => {
  const p = { name: 'x', model: 'x', status: async () => ({ available: true }), generate: async () => { throw new Error('conexión rechazada'); } };
  const r = await paraphrase(EN_CLINICO, { lang: 'en', level: 3, llm: { provider: p } });
  assert.equal(r.llm.accepted, 0);
  assert.ok(r.llm.errors.length);
  assert.ok(r.integrity.ok);
});

test('analiza respuestas con <think>, bloques de código o texto plano', () => {
  assert.equal(parseModelText('<think>razono…</think>{"text":"Hola ⟦1⟧."}'), 'Hola ⟦1⟧.');
  assert.equal(parseModelText('```json\n{"text":"Hi."}\n```'), 'Hi.');
  assert.equal(parseModelText('Texto reformulado: Hola.'), 'Hola.');
});

test('proveedor Ollama contra un servidor que imita su API', async () => {
  const seen = [];
  const srv = http.createServer(async (req, res) => {
    let body = ''; for await (const c of req) body += c;
    seen.push({ url: req.url, body: body ? JSON.parse(body) : null });
    res.setHeader('Content-Type', 'application/json');
    if (req.url === '/api/version') return res.end(JSON.stringify({ version: '0.9.0' }));
    if (req.url === '/api/tags') return res.end(JSON.stringify({ models: [{ name: 'qwen3.5:4b' }] }));
    if (req.url === '/api/chat') {
      const b = JSON.parse(body);
      return res.end(JSON.stringify({ message: { role: 'assistant', content: good(inputOf(b.messages[1].content)) } }));
    }
    res.statusCode = 404; res.end('{}');
  });
  await new Promise((r) => srv.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${srv.address().port}`;
  try {
    const prov = createOllamaProvider({ baseUrl: base, model: 'qwen3.5:4b' });
    const st = await prov.status();
    assert.equal(st.available, true);
    assert.equal(st.modelInstalled, true);
    const r = await paraphrase(ES_CLINICO, { lang: 'es', level: 3, seed: 9, llm: { provider: prov } });
    assert.ok(r.llm.accepted >= 1);
    const chat = seen.find((x) => x.url === '/api/chat').body;
    assert.equal(chat.stream, false);
    assert.equal(chat.options.seed, 9);
    assert.ok(chat.format && chat.format.required.includes('text'));
    assert.equal(chat.model, 'qwen3.5:4b');
    const off = await createOllamaProvider({ baseUrl: 'http://127.0.0.1:9' }).status();
    assert.equal(off.available, false);
  } finally { srv.close(); }
});
