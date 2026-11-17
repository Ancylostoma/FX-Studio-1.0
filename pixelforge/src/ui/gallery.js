// Galería local: proyectos guardados en IndexedDB. Las miniaturas se
// renderizan desde los datos de la escena (no se almacenan imágenes).
import { $, h, toast } from './controls.js';
import * as pm from '../storage/projectManager.js';
import { drawScene } from '../core/pixelEngine.js';
import { validateScene } from '../core/pixelScene.js';
import { serializeProject, safeFileName } from '../export/projectExporter.js';
import { downloadText } from '../export/pngExporter.js';

export function initGallery({ onOpen }) {
  const dlg = $('#galleryDlg');
  $('#btnGalleryClose').addEventListener('click', () => dlg.close());
  dlg.addEventListener('click', (e) => { if (e.target === dlg) dlg.close(); });

  async function refresh() {
    const grid = $('#galleryGrid');
    const items = (await pm.listProjects()).filter((p) => validateScene(p.scene).ok);
    $('#galleryHint').textContent = items.length
      ? `${items.length} proyecto(s) guardados en este dispositivo${(await pm.isPersistent()) ? '' : ' (solo durante esta sesión: almacenamiento no disponible)'}.`
      : 'Todavía no hay proyectos. Pulsa «Guardar» para añadir la imagen actual.';
    grid.replaceChildren(...items.map((p) => {
      const cv = h('canvas');
      drawScene(p.scene, cv, Math.max(1, Math.floor(170 / Math.max(p.scene.w, p.scene.h))));
      const open = () => { onOpen(p); dlg.close(); };
      return h('article', { class: 'gcard' },
        h('div', { class: 'thumb', onclick: open, title: 'Abrir' }, cv),
        h('p', { class: 'title', title: p.name }, p.name),
        h('p', { class: 'meta' }, `${p.scene.w}×${p.scene.h} · ${new Date(p.updated).toLocaleDateString('es')}`),
        h('div', { class: 'row' },
          h('button', { class: 'btn', onclick: open }, 'Abrir'),
          h('button', { class: 'btn', onclick: async () => { await pm.duplicateProject(p.id); refresh(); } }, 'Duplicar'),
          h('button', { class: 'btn', onclick: async () => { const n = window.prompt('Nuevo nombre', p.name); if (n) { await pm.renameProject(p.id, n); refresh(); } } }, 'Renombrar'),
          h('button', { class: 'btn', onclick: () => downloadText(serializeProject(p), safeFileName(p.name, 'pfproj.json')) }, 'Exportar'),
          h('button', { class: 'btn danger', onclick: async () => { if (window.confirm(`¿Eliminar «${p.name}»? No se puede deshacer.`)) { await pm.deleteProject(p.id); toast('Proyecto eliminado'); refresh(); } } }, 'Eliminar'),
        ));
    }));
  }
  return { open: async () => { await refresh(); if (!dlg.open) dlg.showModal(); }, refresh };
}
