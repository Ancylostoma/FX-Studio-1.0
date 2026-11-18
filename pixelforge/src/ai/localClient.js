// Cliente del modo IA local. Habla con el servidor de PixelForge, que a su vez
// habla con Ollama en el propio equipo. Todo se valida de nuevo aquí.
import { sanitizePlan, rawToPlan } from './planSchema.js';

async function req(path, body, signal) {
  const r = await fetch(path, body ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), signal } : { cache: 'no-store', signal });
  let j = null;
  try { j = await r.json(); } catch { /* sin cuerpo JSON */ }
  if (!r.ok) { const e = new Error(j?.error || `El servidor respondió ${r.status}`); e.code = j?.code || 'http_' + r.status; throw e; }
  return j;
}

export async function localStatus(url) {
  try { return await req('api/local/status' + (url ? '?url=' + encodeURIComponent(url) : '')); }
  catch (e) { return { available: false, reason: e.code?.startsWith('http_') ? 'El servidor de PixelForge no está en marcha (abre la app con INICIAR.bat o npm start)' : e.message, code: e.code || 'no_server' }; }
}

export async function localInterpret(body, signal) {
  const j = await req('api/local/interpret', body, signal);
  // Doble saneado: el plan vuelve al formato del modelo y se valida otra vez.
  const again = sanitizePlan({ ...rawToPlan(j.raw), unsupported: j.raw?.unknown || [] });
  if (!again) throw new Error('El modelo local devolvió un plan inválido');
  return { raw: again.raw, warnings: [...(j.warnings || []), ...again.warnings], timings: j.timings, model: j.model, compat: j.compat || [] };
}

export const localEvaluate = (body, signal) => req('api/local/evaluate', body, signal);
export const localWarmup = (body, signal) => req('api/local/warmup', body, signal);
