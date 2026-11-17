// Orquestación de la aplicación: estado, generación, edición y vistas.
import { $, h, toast, initControls, readOptions, writeOptions, fillSelect } from './controls.js';
import { initGallery } from './gallery.js';
import { parsePrompt, finalize, describeSpec, explainSpec, applyEdit } from '../generator/promptParser.js';
import { composeScene, editScene, dimensions } from '../generator/sceneComposer.js';
import { render, imageToCanvas, drawScene, clampScale } from '../core/pixelEngine.js';
import { parseScene, sceneStats, validateScene } from '../core/pixelScene.js';
import { toHex, fromHex } from '../core/paletteManager.js';
import { AMBIENTS } from '../core/lighting.js';
import { randomSeed } from '../core/randomSeed.js';
import { History } from '../editor/history.js';
import * as LM from '../editor/layerManager.js';
import * as CE from '../editor/colorEditor.js';
import { paintPixels, pickIndex, strokePixels } from '../editor/canvasEditor.js';
import { exportImage, scaleForTarget, collectSprites, buildSpriteSheet, imageToBlob, downloadBlob, downloadText } from '../export/pngExporter.js';
import { sceneToJSON, serializeProject, parseProjectFile, safeFileName } from '../export/projectExporter.js';
import * as pm from '../storage/projectManager.js';
import { aiStatus, aiGenerate } from '../ai/aiClient.js';

const state = {
  project: null,            // { id?, name, prompt, opts, raw, scene }
  history: new History(),
  zoom: 'fit', grid: false, tool: 'move', color: 0,
  openLayer: -1, ai: { available: false },
};
const scene = () => state.project?.scene;
const composeOpts = (o) => ({ ...dimensions(o.res, o.aspect), style: o.style, detail: o.detail, seed: o.seed, palette: o.palette });

// ---------- Estado e historial ----------
function commit(newScene, { raw, record = true } = {}) {
  const v = validateScene(newScene);
  if (!v.ok) { toast('Cambio rechazado: ' + v.errors[0], 'error'); return false; }
  state.project.scene = newScene;
  if (raw !== undefined) state.project.raw = raw;
  if (record) state.history.push({ scene: newScene, raw: state.project.raw });
  state.color = Math.min(state.color, newScene.pal.length - 1);
  refresh();
  saveDraftSoon();
  return true;
}
function restore(snap) {
  if (!snap) return;
  state.project.scene = snap.scene; state.project.raw = snap.raw;
  state.color = Math.min(state.color, snap.scene.pal.length - 1);
  refresh(); saveDraftSoon();
}
let draftTimer = 0;
function saveDraftSoon() { clearTimeout(draftTimer); draftTimer = setTimeout(() => pm.saveDraft(state.project).catch(() => {}), 600); }

function setProject(p, { resetHistory = true } = {}) {
  state.project = p;
  if (resetHistory) state.history.reset({ scene: p.scene, raw: p.raw });
  state.openLayer = -1; state.color = Math.min(state.color, p.scene.pal.length - 1);
  $('#prompt').value = p.prompt || '';
  writeOptions(p.opts);
  refresh(); saveDraftSoon();
}

// ---------- Previsualización ----------
function currentZoom() {
  const s = scene(), wrap = $('#canvasWrap');
  if (state.zoom !== 'fit') return clampScale(s, +state.zoom);
  const z = Math.floor(Math.min((wrap.clientWidth - 34) / s.w, (wrap.clientHeight - 34) / s.h));
  return clampScale(s, Math.max(1, z));
}
function drawPreview() {
  const s = scene();
  if (!s) return;
  const z = currentZoom();
  imageToCanvas(render(s), $('#view'), z);
  const g = $('#grid');
  g.width = s.w * z; g.height = s.h * z;
  if (state.grid && z >= 4) {
    const ctx = g.getContext('2d');
    ctx.strokeStyle = 'rgba(255,255,255,.14)'; ctx.lineWidth = 1; ctx.beginPath();
    for (let x = 1; x < s.w; x++) { ctx.moveTo(x * z + 0.5, 0); ctx.lineTo(x * z + 0.5, g.height); }
    for (let y = 1; y < s.h; y++) { ctx.moveTo(0, y * z + 0.5); ctx.lineTo(g.width, y * z + 0.5); }
    ctx.stroke();
  }
  const st = sceneStats(s);
  $('#info').textContent = `${s.w}×${s.h} · ${st.colors} colores · ${st.layers} capas · ${st.ops} ops · ${(st.bytes / 1024).toFixed(1)} KB · zoom ${z}×`;
}

function refresh() {
  drawPreview();
  renderLayers(); renderPalette(); renderLight(); renderExport(); renderJson();
  $('#btnUndo').disabled = !state.history.canUndo;
  $('#btnRedo').disabled = !state.history.canRedo;
}

// ---------- Generación ----------
function showInterpretation(spec, opts) {
  $('#interp').hidden = false;
  $('#interpText').textContent = explainSpec(spec);
  $('#interpCode').textContent = describeSpec(spec, { style: opts.style, seed: opts.seed });
  $('#interpNotes').textContent = spec.notes.join(' ');
}
async function busy(fn) {
  document.body.classList.add('busy');
  try { await fn(); } catch (e) { console.error(e); toast(e.message || String(e), 'error'); } finally { document.body.classList.remove('busy'); }
}
const projectName = (prompt) => (prompt.trim().slice(0, 48) || 'Sin título');

function generate({ newSeed }) {
  return busy(async () => {
    const prompt = $('#prompt').value.trim();
    if (!prompt) { toast('Escribe primero una descripción'); return; }
    const opts = readOptions();
    if (newSeed || !$('#optLock').checked && !state.seedTyped) opts.seed = randomSeed() || 1;
    state.seedTyped = false;
    $('#optSeed').value = opts.seed;
    let sceneOut, raw = null;
    if (opts.mode === 'ai') {
      if (!state.ai.available) { toast('Modo IA no disponible: ' + state.ai.reason, 'error'); return; }
      toast('Pidiendo la escena a la IA…');
      const d = dimensions(opts.res, opts.aspect);
      const out = await aiGenerate({ prompt, w: d.w, h: d.h, style: opts.style });
      sceneOut = CE.setQuantPalette(out.scene, opts.palette);
      $('#interp').hidden = false;
      $('#interpText').textContent = `Escena interpretada por la IA (${out.model || 'Claude'}).`;
      $('#interpCode').textContent = out.usage ? `tokens: entrada ${out.usage.input_tokens} · salida ${out.usage.output_tokens}` : '';
      $('#interpNotes').textContent = out.warnings.length ? `${out.warnings.length} operación(es) descartadas al validar.` : '';
    } else {
      const parsed = parsePrompt(prompt);
      raw = parsed.raw;
      const style = parsed.raw.style && !state.styleTouched ? parsed.raw.style : opts.style;
      if (style !== opts.style) { opts.style = style; $('#optStyle').value = style; }
      sceneOut = composeScene(parsed.spec, composeOpts(opts));
      showInterpretation(parsed.spec, opts);
    }
    setProject({ name: projectName(prompt), prompt, opts, raw, scene: sceneOut });
    $('#variations').hidden = true;
    setView('stage');
  });
}

function vary() {
  return busy(async () => {
    const p = state.project;
    if (!p?.raw) { toast(p ? 'Las variaciones por semilla solo existen en modo offline; usa REGENERAR para pedir otra versión a la IA.' : 'Genera primero una imagen'); return; }
    const spec = finalize(p.raw);
    const box = $('#variations');
    const items = [1, 2, 3, 4].map(() => {
      const opts = { ...p.opts, seed: randomSeed() || 1 };
      const sc = composeScene(spec, composeOpts(opts));
      const cv = h('canvas');
      drawScene(sc, cv, Math.max(1, Math.floor(128 / Math.max(sc.w, sc.h))));
      return h('button', { title: 'Semilla ' + opts.seed, onclick: () => { setProject({ ...p, id: undefined, opts, scene: sc }); toast('Variación aplicada (semilla ' + opts.seed + ')'); } }, cv);
    });
    box.replaceChildren(h('span', {}, 'Variaciones:'), ...items, h('button', { class: 'chip', onclick: () => { box.hidden = true; drawPreview(); } }, 'cerrar'));
    box.hidden = false;
    drawPreview();
    setView('stage');
  });
}

function editByInstruction() {
  return busy(async () => {
    const text = $('#editText').value.trim(), p = state.project;
    if (!p) { toast('Genera primero una imagen'); return; }
    if (!text) { toast('Escribe qué quieres cambiar'); return; }
    if (p.raw && readOptions().mode !== 'ai') {
      const { raw, done } = applyEdit(p.raw, text);
      const res = editScene(p.scene, p.raw, raw, composeOpts(p.opts));
      if (!res.changed.length) { toast('No encontré nada que cambiar en esa instrucción'); return; }
      commit(res.scene, { raw });
      showInterpretation(finalize(raw), p.opts);
      const kept = new Set(res.scene.layers.map((l) => l.k)).size - res.changed.length;
      toast(res.rebuilt ? 'La escena se regeneró completa.' : `${done.join(' · ')} — regeneradas ${res.changed.length} parte(s), conservadas ${Math.max(0, kept)}.`);
    } else if (state.ai.available) {
      toast('Pidiendo el cambio a la IA…');
      const out = await aiGenerate({ base: p.scene, instruction: text, w: p.scene.w, h: p.scene.h });
      commit(out.scene, { raw: null });
    } else toast('Esta escena no tiene especificación offline y el modo IA no está disponible.', 'error');
    $('#editText').value = '';
    setView('stage');
  });
}

// ---------- Panel: capas ----------
const KIND_NAMES = { sky: 'cielo', stars: 'estrellas', celestial: 'astros', clouds: 'nubes', far: 'lejanía', ground: 'suelo', water: 'agua', props: 'vegetación', front: 'primer plano', weather: 'clima y partículas', user: 'retoques' };
function renderLayers() {
  const s = scene();
  const groups = LM.elementGroups(s);
  $('#elements').replaceChildren(...[...groups].map(([key, idx]) => {
    const on = idx.some((i) => s.layers[i].v !== 0);
    const label = KIND_NAMES[key] || s.layers[idx[idx.length - 1]].n?.replace(/^luces de /, '') || key;
    return h('button', { class: 'chip ' + (on ? 'on' : 'off'), title: 'Activar o desactivar este elemento', onclick: () => commit(LM.toggleElement(s, key)) }, label);
  }));
  const list = $('#layerList');
  const rows = [];
  for (let i = s.layers.length - 1; i >= 0; i--) {
    const l = s.layers[i], open = state.openLayer === i;
    const mini = (txt, title, fn, dis) => h('button', { class: 'mini', title, disabled: dis, onclick: (e) => { e.stopPropagation(); fn(); } }, txt);
    const li = h('li', { class: l.v === 0 ? 'hidden' : '' },
      h('div', { class: 'head' },
        mini(l.v === 0 ? '○' : '●', 'Mostrar u ocultar', () => commit(LM.toggleLayer(s, i))),
        h('span', { class: 'name', title: l.n, onclick: () => { state.openLayer = open ? -1 : i; renderLayers(); } }, l.n || 'capa'),
        mini('▲', 'Traer adelante', () => { if (state.openLayer === i) state.openLayer = i + 1; commit(LM.moveLayer(s, i, 1)); }, i === s.layers.length - 1),
        mini('▼', 'Enviar atrás', () => { if (state.openLayer === i) state.openLayer = i - 1; commit(LM.moveLayer(s, i, -1)); }, i === 0),
      ));
    if (open) {
      const olv = l.ol === undefined ? 'none' : l.ol === -2 ? 'sel' : 'col';
      const olSel = h('select', { onchange: (e) => commit(LM.setLayerOutline(s, i, e.target.value === 'col' ? state.color : e.target.value)) });
      fillSelect(olSel, [['none', 'Sin contorno'], ['sel', 'Selectivo'], ['col', 'Color seleccionado']], olv);
      li.append(h('div', { class: 'body' },
        h('label', {}, `Opacidad ${Math.round((l.a ?? 1) * 100)}%`, h('input', { type: 'range', min: 0, max: 100, value: Math.round((l.a ?? 1) * 100), onchange: (e) => commit(LM.setLayerOpacity(s, i, e.target.value / 100)) })),
        h('label', {}, 'Contorno', olSel),
        h('div', { class: 'row wrap' },
          h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: !!l.sh, onchange: (e) => commit(LM.setLayerShading(s, i, e.target.checked)) }), ' sombreado'),
          h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: !l.nl, onchange: (e) => commit(LM.setLayerLit(s, i, e.target.checked)) }), ' recibe luz')),
        h('div', { class: 'row wrap' },
          h('button', { class: 'btn', onclick: () => { const n = window.prompt('Nombre de la capa', l.n); if (n) commit(LM.renameLayer(s, i, n)); } }, 'Renombrar'),
          h('button', { class: 'btn', onclick: () => commit(LM.duplicateLayer(s, i)) }, 'Duplicar'),
          h('button', { class: 'btn', onclick: () => exportLayer(i) }, 'PNG'),
          h('button', { class: 'btn danger', onclick: () => { state.openLayer = -1; commit(LM.removeLayer(s, i)); } }, 'Eliminar')),
        h('p', { class: 'hint' }, `${l.o.length} operación(es)`)));
    }
    rows.push(li);
  }
  list.replaceChildren(...rows);
}

// ---------- Panel: paleta ----------
function renderPalette() {
  const s = scene();
  $('#palCount').textContent = `(${s.pal.length} colores)`;
  $('#swatches').replaceChildren(...s.pal.map((c, i) => h('button', {
    class: (i === state.color ? 'on ' : '') + (i === s.bg ? 'bg' : ''), style: { background: toHex(c) }, title: `#${i} ${toHex(c)}${i === s.bg ? ' (fondo)' : ''}`,
    onclick: () => { state.color = i; renderPalette(); },
  })));
  const c = s.pal[state.color] || [0, 0, 0];
  $('#colorInput').value = toHex(c);
  $('#colorLabel').textContent = `índice ${state.color} · ${toHex(c)}`;
  $('#curColor').style.background = toHex(c);
  $('#optPalette2').value = CE.quantKey(s);
  $('#optPalette').value = CE.quantKey(s);
}

// ---------- Panel: luz ----------
function renderLight() {
  const s = scene(), amb = s.fx?.amb;
  $('#lightDir').value = String(s.fx?.ld ?? -1);
  $('#ambPreset').value = CE.ambientKey(s);
  $('#ambInt').value = amb ? amb[3] : 0;
  $('#ambOut').textContent = (amb ? amb[3] : 0) + '%';
  $('#ambColor').value = toHex(amb ? amb.slice(0, 3) : [255, 255, 255]);
  $('#dthRange').value = s.fx?.dth ?? 2;
  $('#dthOut').textContent = ['sin trama', 'suave', 'medio', 'intenso'][s.fx?.dth ?? 2];
}

// ---------- Panel: exportación ----------
function renderExport() {
  const s = scene(), sel = $('#expScale'), prev = sel.value;
  const seen = new Set();
  const opts = [[1, `Original · ${s.w}×${s.h}`]];
  for (const t of [256, 512, 1024, 2048, 4096]) { const k = scaleForTarget(s, t); if (k > 1 && !seen.has(k)) { seen.add(k); opts.push([k, `×${k} · ${s.w * k}×${s.h * k}`]); } }
  fillSelect(sel, opts, opts.some(([k]) => String(k) === prev) ? prev : opts[Math.min(3, opts.length - 1)][0]);
}
async function exportPng() {
  const s = scene(), k = +$('#expScale').value || 1, transparent = $('#expTransparent').checked;
  const blob = await imageToBlob(exportImage(s, { transparent }), k);
  downloadBlob(blob, safeFileName(`${state.project.name}-${s.w * k}x${s.h * k}`, 'png'));
  toast(`PNG exportado a ${s.w * k}×${s.h * k}`);
}
async function exportLayer(i) {
  const s = scene(), k = +$('#expScale').value || 1;
  downloadBlob(await imageToBlob(render(s, { transparent: true, only: [i], showHidden: true }), k), safeFileName(`${state.project.name}-${s.layers[i].n}`, 'png'));
}
async function exportSheet() {
  const sheet = buildSpriteSheet(collectSprites(scene()));
  if (!sheet) { toast('Esta escena no contiene sprites individuales'); return; }
  const k = Math.min(+$('#expScale').value || 1, Math.max(1, Math.floor(4096 / Math.max(sheet.img.w, sheet.img.h))));
  downloadBlob(await imageToBlob(sheet.img, k), safeFileName(state.project.name + '-sprites', 'png'));
  downloadText(JSON.stringify({ scale: k, frames: sheet.frames }), safeFileName(state.project.name + '-sprites', 'json'));
  toast(`Hoja con ${sheet.frames.length} sprite(s) y su índice JSON`);
}
async function importFile(file) {
  if (!file) return;
  if (file.size > 2_000_000) { toast('Archivo demasiado grande', 'error'); return; }
  const r = parseProjectFile(await file.text());
  if (!r.ok) { toast('No se pudo importar: ' + r.errors[0], 'error'); return; }
  setProject(r.project);
  $('#interp').hidden = true;
  toast('Proyecto importado');
}

// ---------- Panel: JSON ----------
function renderJson() {
  const s = scene(), txt = sceneToJSON(s);
  if (document.activeElement !== $('#jsonText')) $('#jsonText').value = txt;
  $('#jsonStats').textContent = `(${txt.length.toLocaleString('es')} caracteres)`;
}

// ---------- Lienzo: herramientas ----------
function pixelAt(e) {
  const cv = $('#view'), r = cv.getBoundingClientRect(), s = scene();
  return [Math.floor(((e.clientX - r.left) / r.width) * s.w), Math.floor(((e.clientY - r.top) / r.height) * s.h)];
}
function initCanvasTools() {
  const cv = $('#view');
  let last = null, working = null;
  const apply = (x, y) => {
    const pts = last ? strokePixels(last[0], last[1], x, y) : [[x, y]];
    working = paintPixels(working, pts, state.tool === 'eraser' ? -1 : state.color);
    last = [x, y];
    imageToCanvas(render(working), cv, currentZoom());
  };
  cv.addEventListener('pointerdown', (e) => {
    const [x, y] = pixelAt(e);
    if (state.tool === 'picker') { const c = pickIndex(scene(), x, y); if (c >= 0) { state.color = c; renderPalette(); toast('Color ' + c + ' seleccionado'); } return; }
    if (state.tool === 'move') return;
    e.preventDefault(); cv.setPointerCapture(e.pointerId);
    working = scene(); last = null; apply(x, y);
  });
  cv.addEventListener('pointermove', (e) => {
    const [x, y] = pixelAt(e);
    $('#cursorInfo').textContent = `x ${x} · y ${y}`;
    if (working && (!last || last[0] !== x || last[1] !== y)) apply(x, y);
  });
  const end = () => { if (working) { const w = working; working = null; last = null; if (w !== scene()) commit(w); } };
  cv.addEventListener('pointerup', end);
  cv.addEventListener('pointercancel', end);
  cv.addEventListener('pointerleave', () => { $('#cursorInfo').textContent = ''; });
}

// ---------- Vistas ----------
function setView(v) {
  $('#app').dataset.view = v;
  document.querySelectorAll('#mobileTabs button').forEach((b) => b.classList.toggle('on', b.dataset.view === v));
  requestAnimationFrame(drawPreview);
}

function bind() {
  $('#btnGenerate').addEventListener('click', () => generate({ newSeed: false }));
  $('#btnRegen').addEventListener('click', () => generate({ newSeed: true }));
  $('#btnVary').addEventListener('click', vary);
  $('#btnEdit').addEventListener('click', editByInstruction);
  $('#editText').addEventListener('keydown', (e) => { if (e.key === 'Enter') editByInstruction(); });
  $('#prompt').addEventListener('keydown', (e) => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) generate({ newSeed: false }); });
  $('#btnDice').addEventListener('click', () => { $('#optSeed').value = randomSeed() || 1; state.seedTyped = true; });
  $('#optSeed').addEventListener('input', () => { state.seedTyped = true; });
  $('#optSeed').addEventListener('keydown', (e) => { if (e.key === 'Enter') generate({ newSeed: false }); });
  $('#optStyle').addEventListener('change', () => { state.styleTouched = true; });
  const setPal = (e) => { if (scene()) { state.project.opts.palette = e.target.value; commit(CE.setQuantPalette(scene(), e.target.value)); } };
  $('#optPalette').addEventListener('change', setPal);
  $('#optPalette2').addEventListener('change', setPal);

  document.querySelectorAll('#mobileTabs button').forEach((b) => b.addEventListener('click', () => setView(b.dataset.view)));
  document.querySelectorAll('#editTabs button').forEach((b) => b.addEventListener('click', () => {
    document.querySelectorAll('#editTabs button').forEach((x) => x.classList.toggle('on', x === b));
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('on', t.id === 'tab-' + b.dataset.tab));
  }));
  document.querySelectorAll('[data-tool]').forEach((b) => b.addEventListener('click', () => {
    state.tool = b.dataset.tool;
    document.querySelectorAll('[data-tool]').forEach((x) => x.classList.toggle('on', x === b));
    $('#canvasBox').classList.toggle('draw', state.tool !== 'move');
    $('#view').style.touchAction = state.tool === 'move' ? 'auto' : 'none';
  }));
  $('#view').style.touchAction = 'auto';

  $('#btnUndo').addEventListener('click', () => restore(state.history.undo()));
  $('#btnRedo').addEventListener('click', () => restore(state.history.redo()));
  document.addEventListener('keydown', (e) => {
    if (!(e.ctrlKey || e.metaKey) || /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement?.tagName)) return;
    const k = e.key.toLowerCase();
    if (k === 'z' && !e.shiftKey) { e.preventDefault(); restore(state.history.undo()); }
    else if (k === 'y' || (k === 'z' && e.shiftKey)) { e.preventDefault(); restore(state.history.redo()); }
  });

  const ZOOMS = [1, 2, 3, 4, 6, 8, 12, 16, 24];
  const setZoom = (z) => { state.zoom = z; $('#zoomSel').value = String(z); drawPreview(); };
  $('#zoomSel').addEventListener('change', (e) => setZoom(e.target.value === 'fit' ? 'fit' : +e.target.value));
  $('#btnZoomIn').addEventListener('click', () => setZoom(ZOOMS.find((z) => z > currentZoom()) || 24));
  $('#btnZoomOut').addEventListener('click', () => setZoom([...ZOOMS].reverse().find((z) => z < currentZoom()) || 1));
  $('#optGrid').addEventListener('change', (e) => { state.grid = e.target.checked; drawPreview(); });
  new ResizeObserver(() => { if (state.zoom === 'fit' && scene()) drawPreview(); }).observe($('#canvasWrap'));

  $('#colorInput').addEventListener('change', (e) => commit(CE.setPaletteColor(scene(), state.color, fromHex(e.target.value))));
  $('#btnBgColor').addEventListener('click', () => commit(CE.setBackground(scene(), state.color)));
  $('#btnBgNone').addEventListener('click', () => commit(CE.setBackground(scene(), -1)));

  $('#lightDir').addEventListener('change', (e) => commit(CE.setLightDirection(scene(), +e.target.value)));
  $('#ambPreset').addEventListener('change', (e) => { if (e.target.value !== 'custom') commit(CE.setAmbient(scene(), e.target.value)); });
  const ambCustom = () => { const key = $('#ambPreset').value, base = AMBIENTS[key]?.amb; commit(CE.setAmbientColor(scene(), key === 'custom' || !base ? fromHex($('#ambColor').value) : base, +$('#ambInt').value)); };
  $('#ambInt').addEventListener('change', ambCustom);
  $('#ambColor').addEventListener('change', () => { $('#ambPreset').value = 'custom'; if (+$('#ambInt').value === 0) $('#ambInt').value = 50; ambCustom(); });
  $('#dthRange').addEventListener('change', (e) => commit(CE.setDither(scene(), +e.target.value)));

  $('#btnPng').addEventListener('click', () => busy(exportPng));
  $('#btnSheet').addEventListener('click', () => busy(exportSheet));
  $('#btnJson').addEventListener('click', () => downloadText(sceneToJSON(scene()), safeFileName(state.project.name, 'pixelscene.json')));
  $('#btnProject').addEventListener('click', () => downloadText(serializeProject(state.project), safeFileName(state.project.name, 'pfproj.json')));
  $('#btnImport').addEventListener('click', () => $('#fileImport').click());
  $('#fileImport').addEventListener('change', (e) => { busy(() => importFile(e.target.files[0])); e.target.value = ''; });

  $('#btnJsonApply').addEventListener('click', () => {
    const r = parseScene($('#jsonText').value);
    $('#jsonMsg').textContent = r.ok ? 'Escena válida aplicada.' : 'Errores: ' + r.errors.slice(0, 4).join(' · ');
    if (r.ok) commit(r.scene, { raw: null });
  });
  $('#btnJsonCopy').addEventListener('click', async () => { try { await navigator.clipboard.writeText(sceneToJSON(scene())); toast('JSON copiado'); } catch { toast('No se pudo copiar', 'error'); } });

  const gallery = initGallery({ onOpen: (p) => { setProject(p); $('#interp').hidden = true; setView('stage'); } });
  $('#btnGallery').addEventListener('click', () => gallery.open());
  $('#btnSave').addEventListener('click', () => busy(async () => {
    const p = state.project;
    if (!p) return;
    if (!p.id) { const n = window.prompt('Nombre del proyecto', p.name); if (n === null) return; p.name = n.trim() || p.name; }
    const rec = await pm.saveProject(p);
    state.project.id = rec.id; state.project.created = rec.created;
    toast((await pm.isPersistent()) ? 'Guardado en la galería local' : 'Guardado solo para esta sesión (almacenamiento no disponible)');
  }));
}

async function start() {
  initControls();
  $('#optSeed').value = randomSeed() || 1;
  bind();
  initCanvasTools();
  let draft = null;
  try { draft = await pm.loadDraft(); } catch { /* sin almacenamiento */ }
  if (draft?.scene && validateScene(draft.scene).ok) setProject(draft);
  else { $('#prompt').value = 'Un dragón rojo volando sobre un castillo medieval durante la noche'; $('#optSeed').value = 7; state.seedTyped = true; await generate({ newSeed: false }); setView('create'); }
  aiStatus().then((st) => {
    state.ai = st;
    const badge = $('#aiBadge');
    badge.textContent = st.available ? 'IA disponible' : 'Offline';
    badge.title = st.available ? `Modelo: ${st.model}` : st.reason;
    badge.classList.toggle('on', st.available);
    const opt = $('#optMode').querySelector('[value=ai]');
    opt.disabled = !st.available;
    if (!st.available) opt.textContent = 'IA (no configurada)';
  });
  if ('serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('sw.js').catch(() => {});
  window.pixelforge = { state };   // punto de inspección para pruebas y depuración
}

start().catch((e) => { console.error(e); toast('Error al iniciar: ' + e.message, 'error'); });
