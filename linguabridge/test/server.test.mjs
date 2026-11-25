import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from '../server/server.mjs';

test('servidor: interfaz, motor por HTTP y seguridad de rutas', async () => {
  process.env.OLLAMA_URL = 'http://127.0.0.1:9'; // sin Ollama
  const srv = createServer();
  await new Promise((r) => srv.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${srv.address().port}`;
  try {
    const html = await (await fetch(base + '/')).text();
    assert.match(html, /LinguaBridge/);
    assert.equal((await fetch(base + '/src/engine/index.js')).status, 200);
    assert.equal((await fetch(base + '/server/server.mjs')).status, 404);
    assert.equal((await fetch(base + '/ui/../.env')).status, 404);
    const st = await (await fetch(base + '/api/llm/status')).json();
    assert.equal(st.available, false);
    const r = await (await fetch(base + '/api/paraphrase', { method: 'POST', body: JSON.stringify({ text: 'Sin embargo, el dolor no disminuyó (Ruiz, 2020).', level: 2, engine: 'auto', seed: 2 }) })).json();
    assert.equal(r.lang, 'es');
    assert.ok(r.result.includes('(Ruiz, 2020)'));
    assert.equal(r.engine, 'reglas');
    const forced = await fetch(base + '/api/paraphrase', { method: 'POST', body: JSON.stringify({ text: 'Hola.', engine: 'llm' }) });
    assert.equal(forced.status, 503);
    process.env.OLLAMA_URL = 'http://192.168.1.50:11434';
    assert.equal((await fetch(base + '/api/llm/status')).status, 403, 'Ollama remoto no autorizado');
  } finally { srv.close(); delete process.env.OLLAMA_URL; }
});
