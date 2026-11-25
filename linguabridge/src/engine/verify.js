// Comprobaciones de integridad y conservación del significado.
// Se aplican a cada oración reformulada por reglas (si falla, se conserva la
// original) y a cada párrafo del modelo de lenguaje (si falla, se descarta).

import { listPh, countMatches } from './util.js';

export const NEGATION = {
  es: /(?<![\p{L}])(?:no|nunca|jamás|ningún|ninguna|ninguno|ningunos|ningunas|nadie|nada|ni|tampoco|sin)(?![\p{L}])/giu,
  en: /(?<![\p{L}])(?:not|no|never|none|nobody|nothing|neither|nor|without|cannot|nowhere)(?![\p{L}])|n['’]t(?![\p{L}])/giu,
};
// Conectores que contienen «sin»/«no» pero no niegan nada.
const NOT_NEGATION = /(?<![\p{L}])(?:sin embargo|no obstante|no solo|not only)(?![\p{L}])/giu;
export const negCount = (s, lang) => countMatches(s.replace(NOT_NEGATION, ' '), NEGATION[lang]);
// Marcadores de certeza: cambiarlos altera la fuerza de una afirmación.
export const HEDGES = {
  es: /(?<![\p{L}])(?:podría|podrían|pudiera|pudieran|posiblemente|probablemente|quizá|quizás|tal vez|sugiere|sugieren|sugirió|sugirieron|parece|parecen|aparentemente|presuntamente|siempre|todos|todas|ningún|solo|únicamente|exclusivamente|solamente)(?![\p{L}])/giu,
  en: /(?<![\p{L}])(?:may|might|could|possibly|probably|likely|unlikely|perhaps|suggests?|suggested|appears?|appeared|seems?|seemed|apparently|always|all|only|solely|exclusively)(?![\p{L}])/giu,
};
// Normaliza sinónimos de "solo" para no contar como cambio de certeza lo que no lo es.
const HEDGE_CANON = { únicamente: 'solo', exclusivamente: 'solo', solamente: 'solo', solely: 'only', exclusively: 'only', suggests: 'suggest', appears: 'appear', seems: 'seem' };

function multiset(arr) { const m = new Map(); for (const x of arr) m.set(x, (m.get(x) || 0) + 1); return m; }
function sameMultiset(a, b) {
  const ma = multiset(a), mb = multiset(b);
  if (ma.size !== mb.size) return false;
  for (const [k, v] of ma) if (mb.get(k) !== v) return false;
  return true;
}
const hedgeList = (s, lang) => (s.match(HEDGES[lang]) || []).map((h) => { const l = h.toLowerCase(); return HEDGE_CANON[l] || l; });

const STOP = new Set(('el la los las un una unos unas de del al a en y o u que se su sus por para con sin es son fue fueron ha han ' +
  'como más muy este esta estos estas ese esa entre sobre también lo le les ' +
  'the a an of and or to in on at by for with from is are was were be been being this that these those it its as their ' +
  'which who also than then into such not no').split(' '));
const stem = (w) => w.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/(?:mente|ciones|cion|ados|adas|idos|idas|ado|ada|ido|ida|ing|ed|es|s|ly)$/u, '').slice(0, 6);
export function contentStems(s) {
  return (s.match(/\p{L}{3,}/gu) || []).filter((w) => !STOP.has(w.toLowerCase())).map(stem);
}

/**
 * Invariantes con marcadores aún puestos (antes de restaurar).
 * @returns {{ok:boolean, failed:string[]}}
 */
export function checkProtected(orig, out, lang) {
  const failed = [];
  if (!sameMultiset(listPh(orig), listPh(out))) failed.push('elementos protegidos');
  if (negCount(orig, lang) !== negCount(out, lang)) failed.push('negaciones');
  if (!sameMultiset(hedgeList(orig, lang), hedgeList(out, lang))) failed.push('grado de certeza');
  // Con todo lo numérico protegido no puede aparecer ninguna cifra nueva.
  if (/\d/.test(out.replace(/\uE000[\uE100-\uF8FF]\uE001/g, '')) && !/\d/.test(orig.replace(/\uE000[\uE100-\uF8FF]\uE001/g, ''))) failed.push('cifras nuevas');
  return { ok: failed.length === 0, failed };
}

/** Comprobaciones adicionales para la salida de un modelo de lenguaje. */
export function checkLLM(orig, out, lang, level) {
  const base = checkProtected(orig, out, lang);
  const failed = [...base.failed];
  const strip = (s) => s.replace(/\uE000[\uE100-\uF8FF]\uE001/g, ' ');
  const o = strip(orig), r = strip(out);
  if (/\d/.test(r)) failed.push('cifras fuera de los marcadores');
  if (/\(\s*[^()]*\b(?:1[6-9]|20)\d{2}\b[^()]*\)|\bet al\.|\bdoi\b|https?:\/\//i.test(r)) failed.push('citas o referencias nuevas');
  // Nombres propios nuevos (autores, fármacos, instituciones) a mitad de oración.
  const caps = (s) => new Set((s.match(/(?<=[\p{Ll},;:]\s)\p{Lu}[\p{Ll}]{2,}/gu) || []));
  const origCaps = new Set([...caps(o)].map((x) => x.toLowerCase()));
  const origWords = new Set((o.match(/\p{L}+/gu) || []).map((x) => x.toLowerCase()));
  const newCaps = [...caps(r)].filter((x) => !origCaps.has(x.toLowerCase()) && !origWords.has(x.toLowerCase()));
  if (newCaps.length) failed.push('nombres propios nuevos: ' + newCaps.slice(0, 3).join(', '));
  const ratio = r.length / Math.max(1, o.length);
  if (ratio < 0.55 || ratio > 1.6) failed.push('longitud muy distinta');
  const so = new Set(contentStems(o)), sr = new Set(contentStems(r));
  if (so.size >= 4) {
    const recall = [...so].filter((x) => sr.has(x)).length / so.size;
    const minRecall = [0, 0.8, 0.65, 0.5, 0.4, 0.3][level] ?? 0.3;
    if (recall < minRecall) failed.push(`demasiado contenido perdido (${Math.round(recall * 100)} %)`);
    const novel = [...sr].filter((x) => !so.has(x)).length / Math.max(1, sr.size);
    const maxNovel = [0, 0.25, 0.4, 0.55, 0.65, 0.75][level] ?? 0.75;
    if (novel > maxNovel) failed.push(`demasiado contenido nuevo (${Math.round(novel * 100)} %)`);
  }
  if (/[\uE000\uE001]|⟦|⟧/.test(out.replace(/\uE000[\uE100-\uF8FF]\uE001/g, ''))) failed.push('marcadores dañados');
  return { ok: failed.length === 0, failed };
}

/**
 * Informe final sobre el texto ya restaurado.
 */
export function finalReport(original, result, spans, lang) {
  const checks = [];
  const add = (name, ok, detail = '') => checks.push({ name, ok, detail });
  const missing = [];
  const need = multiset(spans.map((s) => s.text));
  for (const [t, n] of need) {
    let c = 0, i = -1;
    while ((i = result.indexOf(t, i + 1)) !== -1) c++;
    if (c < n) missing.push(t);
  }
  add('Elementos protegidos intactos', missing.length === 0, missing.length ? 'Faltan: ' + missing.slice(0, 5).join(' | ') : `${spans.length} elementos`);
  const nums = (s) => s.match(/\d+(?:[.,]\d+)*/g) || [];
  add('Cifras idénticas', sameMultiset(nums(original), nums(result)), `${nums(original).length} cifras`);
  const cit = (s) => spans.filter((x) => x.kind === 'citation').map((x) => x.text).filter((t) => s.includes(t));
  const citations = spans.filter((x) => x.kind === 'citation');
  add('Citas APA conservadas', cit(result).length === citations.length, `${citations.length} citas`);
  const refs = spans.filter((x) => x.kind === 'references' || x.kind === 'reference');
  add('Referencias sin cambios', refs.every((r) => result.includes(r.text)), refs.length ? `${refs.length} bloque(s)` : 'sin lista de referencias');
  const clin = spans.filter((x) => x.kind === 'clinical');
  add('Terminología clínica conservada', clin.every((r) => result.includes(r.text)), `${clin.length} términos`);
  add('Negaciones conservadas', negCount(original, lang) === negCount(result, lang), `${negCount(original, lang)} negaciones`);
  add('Grado de certeza conservado', sameMultiset(hedgeList(original, lang), hedgeList(result, lang)));
  return { ok: checks.every((c) => c.ok), checks };
}
