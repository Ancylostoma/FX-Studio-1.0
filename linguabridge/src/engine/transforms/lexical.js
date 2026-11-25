// Sustitución contextual en UNA sola pasada: todas las formas de un nivel se
// compilan en una alternancia (de la más larga a la más corta) y cada tramo del
// texto se procesa una vez. Así «sin embargo → no obstante» y «no obstante →
// sin embargo» no se anulan entre sí, y lo ya sustituido no se vuelve a tocar.

import { escapeRe, matchCase, pick, normSpace } from '../util.js';

const compiled = new WeakMap();

function compile(entries) {
  if (compiled.has(entries)) return compiled.get(entries);
  const byForm = new Map();
  for (const e of entries) {
    const froms = Array.isArray(e.from) ? e.from : [e.from];
    for (const f of froms) {
      const key = normSpace(f).toLowerCase();
      if (!byForm.has(key)) byForm.set(key, []);
      byForm.get(key).push({ ...e, to: Array.isArray(e.to) ? e.to : [e.to] });
    }
  }
  const alt = [...byForm.keys()].sort((a, b) => b.length - a.length)
    .map((k) => escapeRe(k).replace(/ /g, '\\s+')).join('|');
  const re = new RegExp(`(?<![\\p{L}\\d'’-])(?:${alt})(?![\\p{L}\\d'’-])`, 'giu');
  const res = { re, byForm };
  compiled.set(entries, res);
  return res;
}

const atSentenceStart = (before) => /^[\s¿¡"“«(\[]*$/u.test(before);

/**
 * Aplica un conjunto de entradas a una oración.
 * @returns {{ text: string, changes: Array<{from,to,tier}> }}
 */
export function applyEntries(text, entries, { rng, prob = 1, tier = 'synonym', reason } = {}) {
  if (!entries?.length) return { text, changes: [] };
  const { re, byForm } = compile(entries);
  const changes = [];
  re.lastIndex = 0;
  const out = text.replace(re, (m, offset, str) => {
    const cands = byForm.get(normSpace(m).toLowerCase());
    if (!cands) return m;
    const before = str.slice(0, offset), after = str.slice(offset + m.length);
    const ok = cands.filter((e) =>
      (!e.start || atSentenceStart(before)) &&
      (!e.after || e.after.test(before)) && (!e.notAfter || !e.notAfter.test(before)) &&
      (!e.before || e.before.test(after)) && (!e.notBefore || !e.notBefore.test(after)));
    if (!ok.length) return m;
    if (prob < 1 && rng() >= prob) return m;
    const e = ok[0];
    let to = pick(rng, e.to);
    if (to === '' && e.raw) { changes.push({ from: m, to: '', tier, reason: e.reason || reason }); return ''; }
    to = matchCase(m, to);
    changes.push({ from: m, to, tier, reason: e.reason || reason });
    return to;
  });
  return { text: out, changes };
}

// Palabra repetida por error: «de de», «the the». Respeta las repeticiones válidas.
export function removeDuplicateWords(text, okSet) {
  const changes = [];
  const out = text.replace(/(?<![\p{L}])(\p{L}+)(\s+)\1(?![\p{L}])/giu, (m, w) => {
    if (okSet.has(w.toLowerCase())) return m;
    changes.push({ from: m, to: w, tier: 'redundancy', reason: 'Palabra repetida' });
    return w;
  });
  return { text: out, changes };
}

// Artículo inglés a/an tras sustituir la palabra siguiente.
const AN_EXCEPT = /^(?:uni|use|usu|uti|eu|one|once|ur[aeiou]|u[bcfgklmnprstvz][aeiou])/i;
const A_EXCEPT = /^(?:hour|honest|honor|honour|heir|herb)/i;
export function fixEnglishArticles(text, changedWords) {
  if (!changedWords.size) return text;
  return text.replace(/(?<![\p{L}])(a|an|A|An)(\s+)(\p{L}+)/gu, (m, art, sp, word) => {
    if (!changedWords.has(word.toLowerCase())) return m;
    const vowel = (/^[aeiou]/i.test(word) && !AN_EXCEPT.test(word)) || A_EXCEPT.test(word);
    const want = vowel ? 'an' : 'a';
    return (art[0] === 'A' ? want[0].toUpperCase() + want.slice(1) : want) + sp + word;
  });
}
