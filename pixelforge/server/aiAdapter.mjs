// Adaptador de IA: descripción → PixelScene v1 mediante Claude.
// El modelo solo produce datos estructurados; jamás una imagen. Toda salida
// se sanea y valida antes de devolverla.
import { SYSTEM_PROMPT, buildUserMessage } from '../src/ai/pixelSceneSpec.js';
import { sanitizeScene, validateScene, LIMITS } from '../src/core/pixelScene.js';
import { STYLES } from '../src/generator/templates.js';
import { SPEC_SCHEMA, INTERPRET_SYSTEM, buildInterpretMessage, sanitizeRaw } from '../src/ai/specSchema.js';

export const DEFAULT_MODEL = 'claude-opus-5-5';
export const aiConfigured = () => !!(process.env.ANTHROPIC_API_KEY || process.env.ANTHROPIC_AUTH_TOKEN);
export const modelId = () => process.env.PIXELFORGE_MODEL || DEFAULT_MODEL;
// Interpretar es una tarea corta: puede usar un modelo distinto si se configura.
export const interpretModelId = () => process.env.PIXELFORGE_INTERPRET_MODEL || modelId();

export class AdapterError extends Error {
  constructor(message, status = 502) { super(message); this.status = status; }
}

// Extrae el primer objeto JSON del texto (tolera vallas de código accidentales).
export function extractJSON(text) {
  const start = text.indexOf('{'), end = text.lastIndexOf('}');
  if (start < 0 || end <= start) return null;
  try { return JSON.parse(text.slice(start, end + 1)); } catch { return null; }
}

// Convierte el texto del modelo en una escena válida con el tamaño pedido.
export function sceneFromModelText(text, w, h) {
  const raw = extractJSON(String(text).slice(0, LIMITS.maxJson));
  if (!raw) return { scene: null, warnings: ['La respuesta no contiene JSON'] };
  raw.w = w; raw.h = h;   // el tamaño lo decide la aplicación, no el modelo
  return sanitizeScene(raw);
}

export function parseRequest(body) {
  const int = (v, d) => (Number.isInteger(v) ? v : d);
  const w = Math.max(LIMITS.minDim, Math.min(LIMITS.maxDim, int(body?.w, 64)));
  const h = Math.max(LIMITS.minDim, Math.min(LIMITS.maxDim, int(body?.h, 64)));
  const styleName = STYLES[body?.style]?.name || STYLES.bit16.name;
  if (body?.base) {
    const v = validateScene(body.base);
    if (!v.ok) throw new AdapterError('La escena base no es válida', 400);
    const instruction = String(body.instruction || '').trim().slice(0, 600);
    if (!instruction) throw new AdapterError('Falta la instrucción de edición', 400);
    return { w: body.base.w, h: body.base.h, styleName, base: body.base, instruction };
  }
  const prompt = String(body?.prompt || '').trim().slice(0, 600);
  if (!prompt) throw new AdapterError('Falta el prompt', 400);
  return { w, h, styleName, prompt };
}

let clientPromise = null;
async function getClient() {
  clientPromise ||= import('@anthropic-ai/sdk').then((m) => ({ Anthropic: m.default, client: new m.default() }));
  try { return await clientPromise; } catch { clientPromise = null; throw new AdapterError('Falta la dependencia @anthropic-ai/sdk: ejecuta "npm install"', 503); }
}

function mapError(e, Anthropic) {
  if (e instanceof Anthropic.AuthenticationError) return new AdapterError('La clave de la API no es válida', 503);
  if (e instanceof Anthropic.RateLimitError) return new AdapterError('Límite de peticiones de la API alcanzado; inténtalo en unos segundos', 429);
  if (e instanceof Anthropic.BadRequestError) return new AdapterError('La API rechazó la petición: ' + e.message, 502);
  if (e instanceof Anthropic.APIConnectionError) return new AdapterError('No se pudo conectar con la API', 502);
  if (e instanceof Anthropic.APIError) return new AdapterError(`Error de la API (${e.status})`, 502);
  return e;
}

export function parseInterpretRequest(body) {
  if (body?.raw !== undefined) {
    if (!body.raw || typeof body.raw !== 'object' || Array.isArray(body.raw)) throw new AdapterError('La especificación actual no es válida', 400);
    const instruction = String(body.instruction || '').trim().slice(0, 600);
    if (!instruction) throw new AdapterError('Falta la instrucción de edición', 400);
    // Se reconstruye desde cero: nunca se reenvía al modelo texto arbitrario del cliente.
    return { raw: sanitizeRaw(rawEcho(body.raw)) || {}, instruction };
  }
  const prompt = String(body?.prompt || '').trim().slice(0, 600);
  if (!prompt) throw new AdapterError('Falta el prompt', 400);
  return { prompt };
}
// El cliente envía un "raw" del parser; se normaliza a identificadores del catálogo.
function rawEcho(raw) {
  const flat = (x) => ({ ...x, armor: x?.a?.armor, weapon: x?.a?.weapon, weaponFx: x?.a?.weaponFx, shield: x?.a?.shield, cape: !!x?.a?.cape, wizardHat: x?.a?.hat === 'wizard', breathesFire: x?.a?.fire });
  const l = (v) => (Array.isArray(v) ? v.map(flat) : []);
  return { ...raw, subjects: l(raw.subjects), structures: l(raw.structures), flora: l(raw.flora), skyColor: raw.sky?.color, crescentMoon: raw.moonPhase === 'crescent', exclude: raw.no, unsupported: [] };
}

// Modo híbrido: prompt → especificación del catálogo (el dibujo es local).
export async function interpretPrompt(body) {
  if (!aiConfigured()) throw new AdapterError('El modo IA no está configurado (falta ANTHROPIC_API_KEY)', 503);
  const req = parseInterpretRequest(body);
  const { Anthropic, client } = await getClient();
  let msg;
  try {
    msg = await client.beta.messages.create({
      model: interpretModelId(),
      max_tokens: 8000,
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
      thinking: { type: 'adaptive' },
      output_config: { effort: 'low', format: { type: 'json_schema', schema: SPEC_SCHEMA } },
      system: [{ type: 'text', text: INTERPRET_SYSTEM, cache_control: { type: 'ephemeral' } }],
      messages: [{ role: 'user', content: buildInterpretMessage(req) }],
    });
  } catch (e) { throw mapError(e, Anthropic); }
  if (msg.stop_reason === 'refusal') throw new AdapterError('El modelo declinó esta petición', 422);
  if (msg.stop_reason === 'max_tokens') throw new AdapterError('La interpretación quedó incompleta', 502);
  const raw = sanitizeRaw(extractJSON(msg.content.filter((b) => b.type === 'text').map((b) => b.text).join('')));
  if (!raw) throw new AdapterError('La IA no devolvió una especificación válida', 502);
  return { raw, usage: { input_tokens: msg.usage?.input_tokens || 0, output_tokens: msg.usage?.output_tokens || 0, cache_read_input_tokens: msg.usage?.cache_read_input_tokens || 0 }, model: msg.model };
}

export async function generateScene(body) {
  if (!aiConfigured()) throw new AdapterError('El modo IA no está configurado (falta ANTHROPIC_API_KEY)', 503);
  const req = parseRequest(body);
  const { Anthropic, client } = await getClient();
  const messages = [{ role: 'user', content: buildUserMessage(req) }];
  const usage = { input_tokens: 0, output_tokens: 0 };
  let warnings = [];
  for (let attempt = 0; attempt < 2; attempt++) {
    let msg;
    try {
      // Streaming + finalMessage(): evita los límites de tiempo HTTP con salidas largas.
      msg = await client.beta.messages.stream({
        model: modelId(),
        max_tokens: 32000,
        betas: ['server-side-fallback-2026-07-01'],
        fallbacks: 'default',            // si el modelo declina, reintenta en el recomendado
        thinking: { type: 'adaptive' },
        output_config: { effort: 'medium' },
        system: [{ type: 'text', text: SYSTEM_PROMPT, cache_control: { type: 'ephemeral' } }],
        messages,
      }).finalMessage();
    } catch (e) { throw mapError(e, Anthropic); }
    usage.input_tokens += msg.usage?.input_tokens || 0;
    usage.output_tokens += msg.usage?.output_tokens || 0;
    if (msg.stop_reason === 'refusal') throw new AdapterError('El modelo declinó esta petición', 422);
    if (msg.stop_reason === 'max_tokens') throw new AdapterError('La escena resultó demasiado grande; prueba con menos detalle o menor resolución', 502);
    const text = msg.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
    const out = sceneFromModelText(text, req.w, req.h);
    if (out.scene && out.scene.layers.length) return { scene: out.scene, warnings: out.warnings.slice(0, 10), usage, model: msg.model };
    warnings = out.warnings;
    // Un único reintento, devolviendo al modelo los errores de validación.
    messages.push({ role: 'assistant', content: msg.content }, { role: 'user', content: 'Tu respuesta no es un PixelScene v1 válido: ' + warnings.slice(0, 8).join('; ') + '. Devuelve únicamente el JSON corregido.' });
  }
  throw new AdapterError('La IA no produjo una escena válida: ' + warnings.slice(0, 3).join('; '), 502);
}
