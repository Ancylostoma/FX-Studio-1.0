// Cliente del modo IA. Habla únicamente con el backend propio (/api/…);
// la clave de la API vive en el servidor y nunca se expone al navegador.
import { validateScene } from '../core/pixelScene.js';

export async function aiStatus() {
  try {
    const r = await fetch('api/status', { cache: 'no-store' });
    if (!r.ok) return { available: false, reason: 'El servidor no ofrece el modo IA' };
    const j = await r.json();
    return { available: !!j.ai, model: j.model, reason: j.ai ? '' : 'Falta configurar ANTHROPIC_API_KEY en el servidor' };
  } catch {
    return { available: false, reason: 'Sin servidor de IA (la app funciona en modo offline)' };
  }
}

// body: { prompt, w, h, style } o { base, instruction, w, h }
export async function aiGenerate(body, signal) {
  const r = await fetch('api/pixelscene', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), signal });
  let j = null;
  try { j = await r.json(); } catch { /* respuesta sin cuerpo JSON */ }
  if (!r.ok || !j?.scene) throw new Error(j?.error || `El servidor respondió ${r.status}`);
  // Nunca se confía en la respuesta: se vuelve a validar en el cliente.
  const v = validateScene(j.scene);
  if (!v.ok) throw new Error('La IA devolvió una escena inválida: ' + v.errors.slice(0, 3).join('; '));
  return { scene: j.scene, warnings: j.warnings || [], usage: j.usage || null, model: j.model };
}
