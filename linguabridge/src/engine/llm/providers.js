// Proveedores de modelos de lenguaje. Interfaz común:
//   { name, model, status(): Promise<{available, models?, version?, error?}>,
//     generate({system, prompt, temperature, seed, json}): Promise<string> }
// Se inyectan en el motor; el motor no sabe de HTTP ni de claves.

const TIMEOUT = 120000;

async function fetchJSON(fetchImpl, url, { method = 'GET', body, headers = {}, timeoutMs = TIMEOUT } = {}) {
  const ctl = new AbortController();
  const t = setTimeout(() => ctl.abort(), timeoutMs);
  try {
    const r = await fetchImpl(url, {
      method, signal: ctl.signal,
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...headers },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await r.text();
    let json = null;
    try { json = JSON.parse(text); } catch { /* no JSON */ }
    if (!r.ok) throw new Error(json?.error?.message || json?.error || `HTTP ${r.status}: ${text.slice(0, 200)}`);
    return json;
  } catch (e) {
    if (e.name === 'AbortError') throw new Error('El modelo tardó demasiado en responder');
    throw e;
  } finally { clearTimeout(t); }
}

const RESPONSE_SCHEMA = { type: 'object', properties: { text: { type: 'string' } }, required: ['text'] };

/** Ollama local (http://127.0.0.1:11434). */
export function createOllamaProvider({
  baseUrl = 'http://127.0.0.1:11434', model = 'qwen3.5:4b', fetchImpl = globalThis.fetch,
  numCtx = 4096, keepAlive = '30m', timeoutMs = TIMEOUT,
} = {}) {
  const base = baseUrl.replace(/\/+$/, '');
  return {
    name: 'ollama', model,
    async status() {
      try {
        const [v, tags] = await Promise.all([
          fetchJSON(fetchImpl, base + '/api/version', { timeoutMs: 3000 }),
          fetchJSON(fetchImpl, base + '/api/tags', { timeoutMs: 3000 }),
        ]);
        const models = (tags?.models || []).map((m) => m.name);
        return { available: true, version: v?.version, models, modelInstalled: models.some((n) => n === model || n.split(':')[0] === model) };
      } catch (e) { return { available: false, error: e.message }; }
    },
    async generate({ system, prompt, temperature = 0.2, seed, json = true }) {
      const res = await fetchJSON(fetchImpl, base + '/api/chat', {
        method: 'POST', timeoutMs,
        body: {
          model, stream: false, think: false, keep_alive: keepAlive,
          ...(json ? { format: RESPONSE_SCHEMA } : {}),
          options: { temperature, ...(seed != null ? { seed } : {}), num_ctx: numCtx },
          messages: [{ role: 'system', content: system }, { role: 'user', content: prompt }],
        },
      });
      return res?.message?.content ?? '';
    },
  };
}

/** Cualquier servidor compatible con /v1/chat/completions (LM Studio, llama.cpp, vLLM...). */
export function createOpenAICompatibleProvider({ baseUrl, model, apiKey = '', fetchImpl = globalThis.fetch, timeoutMs = TIMEOUT } = {}) {
  const base = String(baseUrl || '').replace(/\/+$/, '');
  const headers = apiKey ? { Authorization: `Bearer ${apiKey}` } : {};
  return {
    name: 'openai-compatible', model,
    async status() {
      try {
        const r = await fetchJSON(fetchImpl, base + '/models', { headers, timeoutMs: 3000 });
        return { available: true, models: (r?.data || []).map((m) => m.id) };
      } catch (e) { return { available: false, error: e.message }; }
    },
    async generate({ system, prompt, temperature = 0.2, seed }) {
      const res = await fetchJSON(fetchImpl, base + '/chat/completions', {
        method: 'POST', headers, timeoutMs,
        body: { model, temperature, ...(seed != null ? { seed } : {}), messages: [{ role: 'system', content: system }, { role: 'user', content: prompt }] },
      });
      return res?.choices?.[0]?.message?.content ?? '';
    },
  };
}

/** Desde el navegador: delega en el servidor local de LinguaBridge. */
export function createHttpProvider({ endpoint = '/api/llm', fetchImpl = globalThis.fetch.bind(globalThis), model } = {}) {
  return {
    name: 'linguabridge-server', model,
    async status() {
      try { return await fetchJSON(fetchImpl, endpoint + '/status', { timeoutMs: 5000 }); } catch (e) { return { available: false, error: e.message }; }
    },
    async generate(req) {
      const r = await fetchJSON(fetchImpl, endpoint + '/generate', { method: 'POST', body: { ...req, model } });
      return r?.text ?? '';
    },
  };
}
