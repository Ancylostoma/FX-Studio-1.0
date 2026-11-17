// Gestión de proyectos guardados localmente (galería).
import * as db from './database.js';

const newId = () => 'p' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
const DRAFT = 'draft';

export async function saveProject(project) {
  const now = Date.now();
  const rec = { ...project, id: project.id || newId(), name: (project.name || 'Sin título').slice(0, 80), created: project.created || now, updated: now };
  await db.put(db.STORES.projects, rec);
  return rec;
}
export async function listProjects() {
  const all = await db.getAll(db.STORES.projects);
  return all.sort((a, b) => (b.updated || 0) - (a.updated || 0));
}
export const loadProject = (id) => db.get(db.STORES.projects, id);
export const deleteProject = (id) => db.del(db.STORES.projects, id);
export async function renameProject(id, name) {
  const p = await loadProject(id);
  if (!p) return null;
  return saveProject({ ...p, name: String(name).trim().slice(0, 80) || p.name });
}
export async function duplicateProject(id) {
  const p = await loadProject(id);
  if (!p) return null;
  const copy = JSON.parse(JSON.stringify(p));
  delete copy.id; delete copy.created;
  copy.name = (p.name + ' (copia)').slice(0, 80);
  return saveProject(copy);
}
// Borrador de la sesión: se restaura al volver a abrir la aplicación.
export const saveDraft = (project) => db.put(db.STORES.kv, project, DRAFT);
export const loadDraft = () => db.get(db.STORES.kv, DRAFT);
export const isPersistent = db.isPersistent;
