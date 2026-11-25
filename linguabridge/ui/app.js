// Interfaz de LinguaBridge. El motor de reglas corre en el navegador; el modelo
// de lenguaje (opcional) se usa a través del servidor local (/api/llm).
import { paraphrase, LEVELS, createHttpProvider } from '/src/engine/index.js';

const $ = (id) => document.getElementById(id);
const esc = (s) => s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const KIND = { references: 'Lista de referencias', reference: 'Referencia', citation: 'Cita APA', quote: 'Cita textual', url: 'URL / DOI', stat: 'Estadístico', number: 'Cifra / unidad', clinical: 'Término clínico', acronym: 'Sigla', custom: 'Término del usuario' };
const TIER = { redundancy: 'Redundancia', 'person-first': 'Lenguaje centrado en la persona', expression: 'Expresión', synonym: 'Sinónimo', voice: 'Voz', reorder: 'Orden', llm: 'Modelo' };

const SAMPLE = `Con el objetivo de evaluar la adherencia terapéutica, se llevó a cabo un estudio transversal en el año 2022 con 120 pacientes diabéticos (García et al., 2021). La insulina basal fue administrada cada 24 horas por vía subcutánea. El protocolo fue aprobado por el comité de ética. Sin embargo, los resultados no fueron estadísticamente significativos (p = 0,07). Aunque la presión arterial disminuyó 12 mmHg, la frecuencia cardiaca se mantuvo estable.

Referencias
García, A., Pérez, B., & Ruiz, C. (2021). Adherencia terapéutica en diabetes mellitus tipo 2. Revista de Enfermería Clínica, 12(3), 45–52. https://doi.org/10.1234/rec.2021.045`;

const store = {
  get(k) { try { return localStorage.getItem('lb:' + k); } catch { return null; } },
  set(k, v) { try { localStorage.setItem('lb:' + k, v); } catch { /* sin almacenamiento */ } },
};
for (const id of ['lang', 'mode', 'engine', 'level', 'model']) { const v = store.get(id); if (v !== null) $(id).value = v; }

let seed = null, llmOk = false, last = null;

function showLevel() {
  const l = LEVELS[$('level').value];
  $('level-name').textContent = `${$('level').value} · ${l.name}`;
  $('level-desc').textContent = l.description;
}
function countWords() { $('src-count').textContent = `${($('src').value.match(/[\p{L}\p{N}]+/gu) || []).length} palabras`; }

async function checkLLM() {
  const chip = $('llm-status');
  try {
    const st = await createHttpProvider({ endpoint: '/api/llm', model: $('model').value || undefined }).status();
    llmOk = !!st.available;
    chip.textContent = llmOk ? `Modelo local: ${st.model}${st.modelInstalled === false ? ' (no instalado)' : ''}` : 'Ollama no disponible · solo reglas';
    chip.classList.toggle('on', llmOk);
  } catch { llmOk = false; chip.textContent = 'Sin servidor · solo reglas'; }
}

function renderDiff(ops) {
  let orig = '', res = '', inline = '';
  for (const o of ops) {
    const t = esc(o.text);
    if (o.type === 'equal') { orig += t; res += t; inline += t; }
    else if (o.type === 'delete') { orig += `<del>${t}</del>`; inline += `<del>${t}</del>`; }
    else { res += `<ins>${t}</ins>`; inline += `<ins>${t}</ins>`; }
  }
  $('side-orig').innerHTML = orig; $('side-res').innerHTML = res; $('inline').innerHTML = inline;
}

function renderReport(r) {
  const groups = {};
  for (const p of r.protected) (groups[p.kind] ||= []).push(p.text);
  const prot = Object.entries(groups).map(([k, arr]) => `<h3>${KIND[k] || k} (${arr.length})</h3><div class="prot">${arr.map((t) => `<span title="${esc(t)}">${esc(t.length > 80 ? t.slice(0, 80) + '…' : t)}</span>`).join('')}</div>`).join('');
  const changes = r.changes.map((c) => `<li><small>${TIER[c.tier] || c.tier}</small> «${esc(c.from.trim())}» → «${esc(c.to.trim())}»</li>`).join('');
  $('report').innerHTML = `
    <h3>Comprobaciones</h3>
    <ul class="checks">${r.integrity.checks.map((c) => `<li class="${c.ok ? 'ok' : 'bad'}">${esc(c.name)}${c.detail ? ` <span class="hint">— ${esc(c.detail)}</span>` : ''}</li>`).join('')}</ul>
    ${r.warnings.map((w) => `<div class="warn">${esc(w)}</div>`).join('')}
    <h3>Cambios aplicados (${r.changes.length})</h3>
    ${changes ? `<ol class="changes">${changes}</ol>` : '<p class="hint">Ninguno.</p>'}
    ${r.revertedSentences ? `<p class="hint">${r.revertedSentences} oración(es) se dejaron como estaban porque la reformulación no superó las comprobaciones.</p>` : ''}
    <h3>Elementos protegidos</h3>${prot || '<p class="hint">Ninguno.</p>'}`;
}

async function run(newVariant = false) {
  const text = $('src').value;
  if (!text.trim()) { $('summary').textContent = 'Escribe o pega un texto.'; return; }
  if (newVariant) seed = (seed ?? 1) + 1; else seed = null;
  $('busy').hidden = false; $('run').disabled = true;
  try {
    const engine = $('engine').value;
    const opts = {
      lang: $('lang').value, level: Number($('level').value), mode: $('mode').value,
      protectedTerms: $('terms').value.split(';').map((s) => s.trim()).filter(Boolean),
      ...(seed != null ? { seed } : {}),
    };
    if (engine === 'llm' || (engine === 'auto' && llmOk)) opts.llm = { provider: createHttpProvider({ endpoint: '/api/llm', model: $('model').value || undefined }) };
    if (engine === 'llm' && !llmOk) throw new Error('El modelo de lenguaje no está disponible. Inicia Ollama o elige «Solo reglas».');
    const r = await paraphrase(text, opts);
    last = r; seed = r.seed;
    renderDiff(r.diff);
    $('clean').textContent = r.result;
    renderReport(r);
    const ok = r.integrity.ok ? 'todas las comprobaciones superadas' : 'revisar comprobaciones';
    $('summary').textContent = `${r.lang === 'es' ? 'Español' : 'Inglés'} · nivel ${r.level} (${r.levelName}) · ${r.mode === 'nursing' ? 'enfermería' : 'general'} · motor: ${r.engine} · ${r.stats.changedPct} % de palabras cambiadas · ${r.protected.length} elementos protegidos · ${ok}${r.warnings.length ? ` · ${r.warnings.length} aviso(s)` : ''}`;
    $('variant').disabled = false;
  } catch (e) {
    $('summary').textContent = 'Error: ' + e.message;
  } finally { $('busy').hidden = true; $('run').disabled = false; }
}

document.querySelectorAll('.tabs button').forEach((b) => b.addEventListener('click', () => {
  document.querySelectorAll('.tabs button').forEach((x) => x.setAttribute('aria-selected', String(x === b)));
  document.querySelectorAll('[data-view]').forEach((v) => { v.hidden = v.dataset.view !== b.dataset.tab; });
}));
for (const id of ['lang', 'mode', 'engine', 'level', 'model']) $(id).addEventListener('change', () => store.set(id, $(id).value));
$('level').addEventListener('input', showLevel);
$('model').addEventListener('change', checkLLM);
$('src').addEventListener('input', countWords);
$('sample').addEventListener('click', () => { $('src').value = SAMPLE; countWords(); });
$('run').addEventListener('click', () => run(false));
$('variant').addEventListener('click', () => run(true));
$('copy').addEventListener('click', async () => { if (last) { try { await navigator.clipboard.writeText(last.result); $('copy').textContent = 'Copiado'; setTimeout(() => { $('copy').textContent = 'Copiar texto'; }, 1500); } catch { /* sin portapapeles */ } } });
showLevel(); countWords(); checkLLM();
