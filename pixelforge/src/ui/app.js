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
import { exportImage, exportFrames, buildFrameStrip, animationToGif, scaleForTarget, collectSprites, buildSpriteSheet, imageToBlob, downloadBlob, downloadText } from '../export/pngExporter.js';
import { sceneToJSON, serializeProject, parseProjectFile, safeFileName } from '../export/projectExporter.js';
import * as pm from '../storage/projectManager.js';
import { aiStatus, aiGenerate, aiInterpret } from '../ai/aiClient.js';
import { THEMES } from '../generator/templates.js';
import { localStatus, localInterpret, localEvaluate, localWarmup } from '../ai/localClient.js';
import { mergeWithParser } from '../ai/planSchema.js';
import { composeChecked } from '../generator/qualityControl.js';
import { autoResolution, applyTheme } from '../generator/environments.js';

const state = {
  project: null,            // { id?, name, prompt, opts, raw, scene }
  history: new History(),
  zoom: 'fit', grid: false, tool: 'move', color: 0,
  openLayer: -1, ai: { available: false }, local: { available: false }, localCfg: null,
  frame: 0, playing: true,
};
const scene = () => state.project?.scene;
const composeOpts = (o) => ({ ...dimensions(o.res === 'auto' ? 128 : o.res, o.aspect), style: o.style, detail: o.detail, seed: o.seed, palette: o.palette, anim: o.anim });

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
  renderVersions();
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
  const n = s.an?.n || 1;
  state.frame %= n;
  imageToCanvas(render(s, { frame: state.frame }), $('#view'), z);
  $('#animBar').hidden = n < 2; $('#animExport').hidden = n < 2;
  $('#frameInfo').textContent = n > 1 ? `${state.frame + 1}/${n} · ${s.an.fps} fps` : '';
  $('#btnPlay').textContent = state.playing ? '⏸' : '▶';
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
  $('#info').textContent = `${s.w}×${s.h} · ${st.frames > 1 ? st.frames + ' fotogramas · ' : ''}${st.colors} colores · ${st.layers} capas · ${st.ops} ops · ${(st.bytes / 1024).toFixed(1)} KB · zoom ${z}×`;
}

function refresh() {
  drawPreview();
  renderLayers(); renderPalette(); renderLight(); renderExport(); renderJson();
  $('#btnUndo').disabled = !state.history.canUndo;
  $('#btnRedo').disabled = !state.history.canRedo;
}

// ---------- Generación ----------
function showInterpretation(spec, opts, ai) {
  $('#interp').hidden = false;
  $('#interpText').textContent = explainSpec(spec) + (ai ? ` · interpretado por IA${ai.usage ? ` (${ai.usage.input_tokens + ai.usage.cache_read_input_tokens} tokens de entrada, ${ai.usage.output_tokens} de salida)` : ''}` : '');
  $('#interpCode').textContent = describeSpec(spec, { style: opts.style, seed: opts.seed });
  $('#interpNotes').textContent = spec.notes.join(' ');
}
async function busy(fn) {
  document.body.classList.add('busy');
  try { await fn(); } catch (e) { console.error(e); toast(e.message || String(e), 'error'); } finally { document.body.classList.remove('busy'); }
}
const projectName = (prompt) => (prompt.trim().slice(0, 48) || 'Sin título');

// ---------- Motores de interpretación ----------
// procedural: reglas locales · local: Ollama · auto: Ollama con recurso al
// procedural · claude / claude-full: nube opcional (solo si hay clave).
const ENGINE_NAMES = { procedural: 'Procedural', local: 'IA local', auto: 'Híbrido', claude: 'Claude', 'claude-full': 'Claude (dibujo)' };
function localSettings() { return state.localCfg; }
async function interpretWith(engine, input) {
  const t0 = performance.now();
  const procedural = () => (input.prompt ? { raw: parsePrompt(input.prompt).raw } : { raw: applyEdit(input.raw, input.instruction).raw });
  const cfg = localSettings();
  const localBody = () => ({ ...input, numCtx: cfg.numCtx, keepAlive: cfg.keepAlive, url: cfg.url || undefined, model: cfg.model, temperature: cfg.temperature, seed: input.seed, maxTokens: cfg.maxTokens, timeoutMs: cfg.timeout * 1000, prefs: cfg.prefs });
  if (engine === 'local') {
    if (!state.local.available) throw new Error('Ollama no está disponible. ' + (state.local.reason || '') + ' Cambia el motor a «Procedural» o «Híbrido automático».');
    if (!cfg.model) throw new Error('Elige un modelo de Ollama en el panel «IA local».');
    toast(`Interpretando con ${cfg.model}…`);
    const out = await localInterpret(localBody());
    return { raw: out.raw, source: 'local', info: { corrections: out.corrections, compat: out.compat, model: out.model, ms: performance.now() - t0, timings: out.timings, warnings: out.warnings } };
  }
  if (engine === 'auto') {
    if (state.local.available && cfg.model) {
      try {
        toast(`Interpretando con ${cfg.model}…`);
        const out = await localInterpret(localBody());
        // Fusión: lo que el parser reconoce con certeza y el modelo omitió se añade.
        const merged = input.prompt ? mergeWithParser(out.raw, parsePrompt(input.prompt).raw) : { raw: out.raw, added: [] };
        return { raw: merged.raw, source: 'local', info: { corrections: out.corrections, compat: out.compat, model: out.model, ms: performance.now() - t0, timings: out.timings, warnings: out.warnings, added: merged.added } };
      } catch (e) {
        const r = procedural();
        return { ...r, source: 'procedural', info: { ms: performance.now() - t0, fallback: e.message, tried: true } };
      }
    }
    return { ...procedural(), source: 'procedural', info: { ms: performance.now() - t0, fallback: state.local.available ? 'no hay modelo seleccionado' : 'Ollama no disponible' } };
  }
  if (engine === 'claude') {
    if (!state.ai.available) throw new Error('Claude no está configurado.');
    const { seed: _seed, ...cloudInput } = input;   // la semilla solo la usa Ollama
    const out = await aiInterpret(cloudInput);
    return { raw: out.raw, source: 'claude', info: { model: out.model, ms: performance.now() - t0, usage: out.usage } };
  }
  return { ...procedural(), source: 'procedural', info: { ms: performance.now() - t0 } };
}

function showEngineInfo(res, renderMs, scene, qc, opts) {
  const i = res.info || {};
  const parts = [ENGINE_NAMES[res.source] + (i.model ? ` (${i.model})` : '')];
  if (res.source !== 'procedural' || i.ms > 20) parts.push(`interpretación ${(i.ms / 1000).toFixed(i.ms < 1000 ? 2 : 1)} s`);
  if (i.timings?.tokensPerSecond) parts.push(`${i.timings.tokensPerSecond} tok/s`);
  parts.push(`render ${Math.round(renderMs)} ms`, `${scene.w}×${scene.h}`, `paleta ${THEMES[res.spec?.theme]?.name || res.spec?.theme || 'automática'}`, `semilla ${opts.seed}`, `${scene.pal.length} colores`);
  if (qc) parts.push(`calidad ${qc.checks.filter((c) => c.ok).length}/${qc.checks.length}`);
  $('#perf').textContent = parts.join(' · ');
  const notes = [];
  if (i.fallback) notes.push(`IA local no usada (${i.fallback}): se ha usado el motor procedural.`);
  if (i.added?.length) notes.push('Añadido por el intérprete procedural: ' + i.added.join(', ') + '.');
  if (i.warnings?.length) notes.push(i.warnings.length + ' dato(s) del modelo descartados al validar.');
  if (i.compat?.length) notes.push('Compatibilidad: ' + i.compat.join('; ') + '.');
  if (i.corrections?.length) notes.push('Ajustado a tu descripción: ' + i.corrections.join('; ') + '.');
  return notes;
}
function showQC(qc) {
  const box = $('#qcBox');
  if (!qc) { box.hidden = true; return; }
  box.hidden = false;
  const bad = qc.checks.filter((c) => !c.ok).length, fixed = qc.checks.filter((c) => c.fixed).length;
  $('#qcSummary').textContent = `Control de calidad: ${qc.checks.length - bad}/${qc.checks.length} correcto${fixed ? `, ${fixed} corregido(s)` : ''}${bad ? `, ${bad} pendiente(s)` : ''} · ${qc.passes} pasada(s)`;
  $('#qcList').replaceChildren(...qc.checks.map((c) => h('li', { class: (c.ok ? 'ok' : 'bad') + (c.fixed ? ' fixed' : '') }, `${c.label}: ${c.detail}`)));
}
function composeFor(spec, opts) {
  const res = opts.res === 'auto' ? autoResolution(spec) : opts.res;
  const t0 = performance.now();
  const out = composeChecked(spec, { ...composeOpts({ ...opts, res }) });
  return { ...out, renderMs: performance.now() - t0, res };
}
function pushVersion(label) {
  const p = state.project;
  if (!p) return;
  p.versions = [...(p.versions || []), { t: Date.now(), label: label.slice(0, 60), scene: p.scene, raw: p.raw }].slice(-12);
  renderVersions();
}
function renderVersions() {
  const v = state.project?.versions || [];
  fillSelect($('#versionSel'), v.map((x, i) => [i, `${new Date(x.t).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })} · ${x.label}`]).reverse(), v.length - 1);
  $('#btnVersion').disabled = v.length < 2;
}

function generate({ newSeed }) {
  return busy(async () => {
    const prompt = $('#prompt').value.trim();
    if (!prompt) { toast('Escribe primero una descripción'); return; }
    const opts = readOptions();
    if (newSeed || !$('#optLock').checked && !state.seedTyped) opts.seed = randomSeed() || 1;
    state.seedTyped = false;
    $('#optSeed').value = opts.seed;
    let sceneOut, raw = null, qc = null;
    if (opts.mode === 'claude-full') {
      if (!state.ai.available) { toast('Claude no está configurado', 'error'); return; }
      toast('Pidiendo la escena a la IA…');
      const d = dimensions(opts.res === 'auto' ? 128 : opts.res, opts.aspect);
      const out = await aiGenerate({ prompt, w: d.w, h: d.h, style: opts.style });
      sceneOut = CE.setQuantPalette(out.scene, opts.palette);
      $('#interp').hidden = false; showQC(null);
      $('#interpText').textContent = `Escena dibujada por la IA (${out.model || 'Claude'}).`;
      $('#interpCode').textContent = out.usage ? `tokens: entrada ${out.usage.input_tokens} · salida ${out.usage.output_tokens}` : '';
      $('#interpNotes').textContent = out.warnings.length ? `${out.warnings.length} operación(es) descartadas al validar.` : '';
    } else {
      const res = await interpretWith(opts.mode, { prompt, seed: opts.seed });
      raw = res.raw;
      if (raw.anim && !state.animTouched && opts.anim === 'none') { opts.anim = raw.anim; $('#optAnim').value = opts.anim; }
      if (raw.style && !state.styleTouched && raw.style !== opts.style) { opts.style = raw.style; $('#optStyle').value = raw.style; }
      const spec = finalize(raw);
      const c = composeFor(spec, opts);
      sceneOut = c.scene; qc = c.qc;
      showInterpretation(spec, opts);
      const notes = showEngineInfo({ ...res, spec }, c.renderMs, sceneOut, qc, opts);
      $('#interpNotes').textContent = [...notes, ...spec.notes].join(' ');
      showQC(qc);
      if (res.info?.tried && opts.mode === 'auto') toast('Ollama no respondió: imagen generada con el motor procedural.');
    }
    setProject({ name: projectName(prompt), prompt, opts, raw, scene: sceneOut, qc });
    pushVersion('Generada');
    $('#variations').hidden = true;
    setView('stage');
  });
}

function vary() {
  return busy(async () => {
    const p = state.project;
    if (!p?.raw) { toast(p ? 'Esta escena no tiene plan editable; usa REGENERAR.' : 'Genera primero una imagen'); return; }
    const spec = finalize(p.raw);
    const box = $('#variations');
    // Las variaciones reutilizan el plan ya interpretado: no vuelven a llamar a la IA.
    const items = [1, 2, 3, 4].map(() => {
      const opts = { ...p.opts, seed: randomSeed() || 1 };
      const { scene: sc } = composeFor(spec, opts);
      const cv = h('canvas');
      drawScene(sc, cv, Math.max(1, Math.floor(128 / Math.max(sc.w, sc.h))));
      return h('button', { title: 'Semilla ' + opts.seed, onclick: () => { setProject({ ...p, id: undefined, opts, scene: sc }); pushVersion('Variación ' + opts.seed); toast('Variación aplicada (semilla ' + opts.seed + ')'); } }, cv);
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
    const mode = readOptions().mode;
    if (p.raw && mode !== 'claude-full') {
      const res = await interpretWith(mode, { raw: p.raw, instruction: text, seed: p.opts?.seed });
      const raw = res.raw;
      const edit = editScene(p.scene, p.raw, raw, composeOpts({ ...p.opts, res: p.scene.w === p.scene.h ? p.scene.w : Math.max(p.scene.w, p.scene.h) }));
      if (!edit.changed.length) { toast('No encontré nada que cambiar en esa instrucción'); return; }
      commit(edit.scene, { raw });
      pushVersion(text);
      const spec = finalize(raw);
      showInterpretation(spec, p.opts);
      const notes = showEngineInfo({ ...res, spec }, 0, edit.scene, null, p.opts);
      $('#interpNotes').textContent = [...notes, ...spec.notes].join(' ');
      const kept = new Set(edit.scene.layers.map((l) => l.k)).size - edit.changed.length;
      toast(edit.rebuilt ? 'La escena se regeneró completa.' : `Regeneradas ${edit.changed.length} parte(s), conservadas ${Math.max(0, kept)}.`);
    } else if (state.ai.available) {
      toast('Pidiendo el cambio a la IA…');
      const out = await aiGenerate({ base: p.scene, instruction: text, w: p.scene.w, h: p.scene.h });
      commit(out.scene, { raw: null });
      pushVersion(text);
    } else toast('Esta escena no tiene plan editable y no hay IA que pueda modificarla.', 'error');
    $('#editText').value = '';
    setView('stage');
  });
}

// ---------- IA local: estado y ajustes ----------
// Valores iniciales pensados para un portátil con 16 GB de RAM y GPU de 6 GB (RTX 4050).
const LOCAL_DEFAULTS = { v: 2, url: '', model: '', temperature: 0.1, maxTokens: 1200, timeout: 120, prefs: 'balanced', numCtx: 4096, keepAlive: '30m' };
function loadLocalCfg() {
  try {
    const saved = JSON.parse(localStorage.getItem('pf.local') || '{}');
    // Ajustes guardados con la versión anterior: la temperatura por defecto pasa de 0,25 a 0,1.
    if (!saved.v) { if (saved.temperature === 0.25) delete saved.temperature; saved.v = 2; }
    return { ...LOCAL_DEFAULTS, ...saved };
  } catch { return { ...LOCAL_DEFAULTS }; }
}
function saveLocalCfg() { try { localStorage.setItem('pf.local', JSON.stringify(state.localCfg)); } catch { /* sin almacenamiento */ } }
const gb = (b) => (b / 1e9).toFixed(1) + ' GB';
// GPU: la tarjeta detectada por el sistema y, si el modelo está cargado, qué
// parte está en la VRAM según Ollama (lo mismo que muestra "ollama ps").
function showGpu(st) {
  const gpu = st.system?.gpu, run = (st.running || []).find((r) => r.name === state.localCfg.model);
  const parts = [gpu ? `GPU detectada: ${gpu.name}${gpu.vram ? ' (' + gb(gpu.vram) + ')' : ''}` : 'GPU: no verificable desde aquí (Ollama puede usarla igualmente)'];
  parts.push(run ? `Modelo cargado: ${run.gpuShare != null ? run.gpuShare + ' % en GPU' : 'sí'}` : 'Modelo no cargado en memoria');
  $('#ollamaGpu').textContent = parts.join(' · ');
}
// Precarga: Ollama mantiene el modelo en memoria según "Mantener en memoria",
// así las generaciones siguientes no vuelven a cargarlo.
async function warmModel(loud) {
  const cfg = state.localCfg;
  if (!state.local.available || !cfg.model) { if (loud) toast('Primero conecta Ollama y elige un modelo', 'error'); return; }
  $('#ollamaGpu').textContent = `Cargando ${cfg.model} en memoria…`;
  try {
    const r = await localWarmup({ url: cfg.url || undefined, model: cfg.model, keepAlive: cfg.keepAlive });
    $('#ollamaGpu').textContent = `${cfg.model} listo en ${(r.ms / 1000).toFixed(1)} s${r.gpuShare != null ? ` · ${r.gpuShare} % en GPU` : ''}`;
    if (loud) toast(`${cfg.model} cargado${r.gpuShare != null ? ` (${r.gpuShare} % en GPU)` : ''}`);
  } catch (e) { $('#ollamaGpu').textContent = 'No se pudo cargar el modelo: ' + e.message; if (loud) toast(e.message, 'error'); }
}
async function refreshLocal() {
  const cfg = state.localCfg;
  $('#ollamaState').textContent = 'comprobando…'; $('#ollamaState').classList.remove('on');
  const st = await localStatus(cfg.url);
  state.local = st;
  const sel = $('#ollamaModel');
  if (st.available) {
    const models = st.models || [];
    if (!cfg.model || !models.some((m) => m.name === cfg.model)) { cfg.model = (models.some((m) => m.name === st.defaultModel) && st.defaultModel) || st.recommended || models[0]?.name || ''; saveLocalCfg(); }
    showGpu(st);
    fillSelect(sel, models.length ? models.map((m) => [m.name, `${m.name}${m.params ? ' · ' + m.params : ''} · ${gb(m.size)}${m.fits ? '' : ' · ⚠ memoria justa'}${m.name === st.recommended ? ' · recomendado' : ''}`]) : [['', 'No hay modelos instalados']], cfg.model);
    $('#ollamaState').textContent = `conectado (v${st.version})${cfg.model ? ' · ' + cfg.model : ''}`; $('#ollamaState').classList.add('on');
    const m = models.find((x) => x.name === cfg.model), sys = st.system;
    const mem = sys ? `Equipo: ${gb(sys.totalRAM)} de RAM${sys.gpu ? ', GPU ' + sys.gpu.name + (sys.gpu.vram ? ' (' + gb(sys.gpu.vram) + ')' : '') : ', sin GPU detectada'}.` : '';
    $('#ollamaRec').textContent = [mem, m ? `«${m.name}» necesita unos ${gb(m.needBytes)} de memoria${m.fits ? '' : ': puede ir lento o fallar en este equipo'}.` : '', st.suggestion ? `Sugerencia: «ollama pull ${st.suggestion.model}» (≈${st.suggestion.approxGB} GB, ${st.suggestion.why}). No se descarga nada sin que tú lo ejecutes.` : ''].filter(Boolean).join(' ');
    $('#ollamaHelp').hidden = !!models.length;
    if (!models.length) $('#ollamaHelp').replaceChildren('Ollama está en marcha pero no tiene modelos. Abre una terminal y ejecuta ', h('code', {}, `ollama pull ${st.suggestion?.model || 'qwen2.5:3b'}`), ' (descarga de varios GB; solo cuando tú quieras).');
  } else {
    fillSelect(sel, [['', '—']], '');
    $('#ollamaState').textContent = 'no detectado';
    $('#ollamaRec').textContent = st.system ? `Equipo: ${gb(st.system.totalRAM)} de RAM. Modelo sugerido al instalar: ${st.suggestion?.model} (≈${st.suggestion?.approxGB} GB).` : '';
    $('#ollamaHelp').hidden = false;
    $('#ollamaHelp').replaceChildren(h('b', {}, 'La IA local es opcional; el motor procedural funciona sin ella. '), 'Para activarla: instala Ollama desde ollama.com, ejecuta ', h('code', {}, 'ollama pull ' + (st.suggestion?.model || 'qwen2.5:3b')), ' y pulsa ⟳. Detalle: ' + (st.reason || 'sin respuesta') + '.');
  }
  const opt = $('#optMode').querySelector('[value=local]');
  opt.disabled = !st.available || !cfg.model;
  opt.textContent = 'IA local · Ollama' + (st.available ? (cfg.model ? '' : ' (sin modelo)') : ' (no detectado)');
  if ($('#optMode').value === 'local' && opt.disabled) $('#optMode').value = 'auto';
  updateBadge();
}
function bindLocal() {
  const cfg = state.localCfg;
  $('#ollamaUrl').value = cfg.url; $('#ollamaTemp').value = cfg.temperature; $('#ollamaTempOut').textContent = cfg.temperature;
  $('#ollamaTokens').value = cfg.maxTokens; $('#ollamaTimeout').value = cfg.timeout; $('#ollamaPrefs').value = cfg.prefs;
  $('#ollamaCtx').value = String(cfg.numCtx); $('#ollamaKeep').value = cfg.keepAlive;
  $('#ollamaCtx').addEventListener('change', (e) => upd('numCtx', +e.target.value));
  $('#ollamaKeep').addEventListener('change', (e) => upd('keepAlive', e.target.value));
  $('#btnOllamaCheck').addEventListener('click', async () => { await refreshLocal(); toast(state.local.available ? `Ollama conectado (v${state.local.version}) · ${state.local.models.length} modelo(s)` : 'Ollama no responde: ' + (state.local.reason || ''), state.local.available ? '' : 'error'); });
  $('#btnOllamaWarm').addEventListener('click', () => warmModel(true));
  const upd = (k, v) => { cfg[k] = v; saveLocalCfg(); };
  $('#ollamaUrl').addEventListener('change', (e) => { upd('url', e.target.value.trim()); refreshLocal(); });
  $('#ollamaModel').addEventListener('change', (e) => { upd('model', e.target.value); refreshLocal().then(() => warmModel(false)); });
  $('#ollamaTemp').addEventListener('input', (e) => { upd('temperature', +e.target.value); $('#ollamaTempOut').textContent = e.target.value; });
  $('#ollamaTokens').addEventListener('change', (e) => upd('maxTokens', Math.max(200, Math.min(4096, +e.target.value || 1200))));
  $('#ollamaTimeout').addEventListener('change', (e) => upd('timeout', Math.max(5, Math.min(600, +e.target.value || 120))));
  $('#ollamaPrefs').addEventListener('change', (e) => upd('prefs', e.target.value));
  $('#btnOllamaRefresh').addEventListener('click', refreshLocal);
  $('#btnOllamaEval').addEventListener('click', () => busy(async () => {
    if (!state.local.available || !cfg.model) { toast('Primero conecta Ollama y elige un modelo', 'error'); return; }
    $('#ollamaEvalOut').textContent = `Evaluando ${cfg.model} con 3 descripciones de prueba… (puede tardar varios minutos sin GPU)`;
    const r = await localEvaluate({ url: cfg.url || undefined, model: cfg.model, maxTokens: cfg.maxTokens, timeoutMs: cfg.timeout * 1000, prefs: 'strict' });
    const pct = (v) => Math.round(v * 100) + ' %';
    $('#ollamaEvalOut').replaceChildren(h('p', {}, `${r.model}: JSON válido ${pct(r.validJson)} · elementos esperados encontrados ${pct(r.recall)} (motor procedural: ${pct(r.baselineRecall)}) · ${(r.avgMs / 1000).toFixed(1)} s por descripción.`),
      ...r.results.map((x) => h('p', {}, `${x.ok ? '✓' : '✗'} «${x.prompt.slice(0, 50)}…» ${x.ok ? `${pct(x.recall)}${x.missing.length ? ', falta: ' + x.missing.join(', ') : ''}, ${(x.ms / 1000).toFixed(1)} s${x.tokensPerSecond ? ', ' + x.tokensPerSecond + ' tok/s' : ''}` : x.error}`)));
  }));
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
async function exportGif() {
  const s = scene(), k = Math.min(+$('#expScale').value || 1, Math.max(1, Math.floor(1024 / Math.max(s.w, s.h))));
  const gif = animationToGif(s, { scale: k, transparent: $('#expTransparent').checked });
  downloadBlob(new Blob([gif], { type: 'image/gif' }), safeFileName(`${state.project.name}-${s.w * k}x${s.h * k}`, 'gif'));
  toast(`GIF de ${s.an.n} fotogramas a ${s.w * k}×${s.h * k} (${(gif.length / 1024).toFixed(0)} KB)`);
}
async function exportStrip() {
  const s = scene(), frames = exportFrames(s, { transparent: $('#expTransparent').checked }), strip = buildFrameStrip(frames);
  const k = Math.min(+$('#expScale').value || 1, Math.max(1, Math.floor(4096 / strip.w)));
  downloadBlob(await imageToBlob(strip, k), safeFileName(state.project.name + '-fotogramas', 'png'));
  downloadText(JSON.stringify({ frames: frames.length, fps: s.an.fps, frameWidth: s.w * k, frameHeight: s.h * k, loop: true }), safeFileName(state.project.name + '-fotogramas', 'json'));
  toast(`Tira de ${frames.length} fotogramas de ${s.w * k}×${s.h * k}`);
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
    imageToCanvas(render(working, { frame: state.frame }), cv, currentZoom());
  };
  cv.addEventListener('pointerdown', (e) => {
    const [x, y] = pixelAt(e);
    if (state.tool === 'picker') { const c = pickIndex(scene(), x, y); if (c >= 0) { state.color = c; renderPalette(); toast('Color ' + c + ' seleccionado'); } return; }
    if (state.tool === 'move') return;
    e.preventDefault(); cv.setPointerCapture(e.pointerId);
    working = scene(); last = null; state.painting = true; apply(x, y);
  });
  cv.addEventListener('pointermove', (e) => {
    const [x, y] = pixelAt(e);
    $('#cursorInfo').textContent = `x ${x} · y ${y}`;
    if (working && (!last || last[0] !== x || last[1] !== y)) apply(x, y);
  });
  const end = () => { state.painting = false; if (working) { const w = working; working = null; last = null; if (w !== scene()) commit(w); } };
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
  $('#btnVersion').addEventListener('click', () => { const v = state.project?.versions?.[+$('#versionSel').value]; if (v) { commit(v.scene, { raw: v.raw }); toast('Versión recuperada: ' + v.label); } });
  // Variante de color: gira el tono de los colores saturados sin tocar la geometría.
  $('#btnVariant').addEventListener('click', () => { if (scene()) { commit(CE.colorVariant(scene(), randomSeed())); pushVersion('Variante de color'); } });
  $('#themeSel').addEventListener('change', (e) => { if (scene() && e.target.value) { commit(applyTheme(scene(), e.target.value)); pushVersion('Paleta ' + e.target.value); e.target.value = ''; } });
  $('#btnGif').addEventListener('click', () => busy(exportGif));
  $('#btnStrip').addEventListener('click', () => busy(exportStrip));
  $('#btnPlay').addEventListener('click', () => { state.playing = !state.playing; drawPreview(); });
  $('#btnFrame').addEventListener('click', () => { state.playing = false; state.frame++; drawPreview(); });
  $('#optAnim').addEventListener('change', () => { state.animTouched = true; });
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

function updateBadge() {
  const b = $('#aiBadge'), on = state.local.available && state.localCfg.model;
  b.textContent = on ? 'IA local: ' + state.localCfg.model : state.ai.available ? 'Claude disponible' : 'Procedural';
  b.title = on ? 'Ollama en ' + (state.local.url || '') : state.local.reason || '';
  b.classList.toggle('on', !!on);
}

async function start() {
  initControls();
  state.localCfg = loadLocalCfg();
  bindLocal();
  $('#optSeed').value = randomSeed() || 1;
  bind();
  initCanvasTools();
  // Reproducción: avanza un fotograma según los fps de la escena (se pausa al dibujar o con la pestaña oculta).
  let lastTick = 0;
  const tick = (t) => {
    const s = scene();
    if (s?.an && state.playing && !state.painting && !document.hidden && t - lastTick >= 1000 / s.an.fps) { lastTick = t; state.frame = (state.frame + 1) % s.an.n; drawPreview(); }
    requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
  let draft = null;
  try { draft = await pm.loadDraft(); } catch { /* sin almacenamiento */ }
  if (draft?.scene && validateScene(draft.scene).ok) setProject(draft);
  else { $('#prompt').value = 'Un dragón rojo volando sobre un castillo medieval durante la noche'; $('#optSeed').value = 7; state.seedTyped = true; await generate({ newSeed: false }); setView('create'); }
  // Claude es opcional: sus motores solo aparecen si el servidor tiene clave.
  aiStatus().then((st) => {
    state.ai = st;
    if (st.available) for (const [v, t] of [['claude', 'Claude interpreta (nube)'], ['claude-full', 'Claude dibuja (nube)']]) $('#optMode').append(h('option', { value: v }, t));
    updateBadge();
  });
  // Al abrir: si hay Ollama y modelo, se precarga una vez (queda en memoria según «Mantener en memoria»).
  refreshLocal().then(() => { updateBadge(); if (state.local.available && state.localCfg.model && ['auto', 'local'].includes($('#optMode').value)) warmModel(false); });
  if ('serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('sw.js').catch(() => {});
  window.pixelforge = { state, refreshLocal };   // punto de inspección para pruebas y depuración
}

start().catch((e) => { console.error(e); toast('Error al iniciar: ' + e.message, 'error'); });
