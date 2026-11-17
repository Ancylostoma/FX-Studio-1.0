// Serialización de escenas y proyectos (sin dependencias del navegador).
import { validateScene, LIMITS } from '../core/pixelScene.js';

export const PROJECT_FORMAT = 'pixelforge-project';

// JSON PixelScene compacto (una sola línea, sin espacios).
export function sceneToJSON(scene) { return JSON.stringify(scene); }

export function serializeProject(p) {
  return JSON.stringify({ format: PROJECT_FORMAT, version: 1, name: p.name, prompt: p.prompt, opts: p.opts, raw: p.raw ?? null, scene: p.scene, created: p.created, updated: p.updated });
}

// Acepta tanto un proyecto completo como un PixelScene suelto.
export function parseProjectFile(text) {
  if (typeof text !== 'string' || text.length > LIMITS.maxJson) return { ok: false, errors: ['Archivo vacío o demasiado grande'] };
  let data;
  try { data = JSON.parse(text); } catch (e) { return { ok: false, errors: ['JSON inválido: ' + e.message] }; }
  const isProject = data && data.format === PROJECT_FORMAT;
  const scene = isProject ? data.scene : data;
  const v = validateScene(scene);
  if (!v.ok) return v;
  const str = (s, n) => (typeof s === 'string' ? s.slice(0, n) : '');
  const opts = isProject && data.opts && typeof data.opts === 'object' && !Array.isArray(data.opts) ? data.opts : {};
  return {
    ok: true, errors: [],
    project: {
      name: str(data.name, 80) || 'Proyecto importado',
      prompt: isProject ? str(data.prompt, 600) : '',
      opts: { res: +opts.res || Math.max(scene.w, scene.h), aspect: str(opts.aspect, 8) || '1:1', style: str(opts.style, 20) || 'bit16', palette: str(opts.palette, 20) || 'auto', detail: +opts.detail || 2, seed: (opts.seed >>> 0) || 1, mode: 'offline' },
      raw: isProject && data.raw && typeof data.raw === 'object' ? data.raw : null,
      scene,
    },
  };
}

export function safeFileName(name, ext) {
  const base = String(name || 'pixelforge').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60) || 'pixelforge';
  return base + '.' + ext;
}
