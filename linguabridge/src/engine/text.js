// Segmentación, detección de idioma y limpieza de fluidez.

import { PH_SRC } from './util.js';

const ABBREV = new Set([
  'dr', 'dra', 'drs', 'sr', 'sra', 'srta', 'ud', 'uds', 'etc', 'e.g', 'i.e', 'ej', 'p. ej', 'vs', 'aprox', 'approx', 'fig', 'figs',
  'tab', 'no', 'núm', 'nº', 'vol', 'vols', 'pp', 'p', 'cap', 'ed', 'eds', 'inc', 'ltd', 'co', 'jr', 'st', 'mr', 'mrs', 'ms', 'prof',
  'mín', 'máx', 'cf', 'al', 'col', 'cols', 'lic', 'enf', 'ca', 'esp', 'dpto', 'av', 'gral', 'art', 'sec', 'pág', 'págs', 'ref',
]);

/** Divide un párrafo (sin saltos de párrafo) en oraciones conservando los separadores. */
export function splitSentences(par) {
  const out = [];
  const re = new RegExp(`([.!?…]+)(["”»’)\\]]*)(\\s+)(?=[¿¡"“«(\\[]?(?:[\\p{Lu}\\d]|${PH_SRC}))`, 'gu');
  let last = 0, m;
  while ((m = re.exec(par))) {
    const end = m.index + m[1].length + m[2].length;
    const before = par.slice(last, m.index);
    const lastWord = (before.match(/([\p{L}.]+)$/u)?.[1] || '').toLowerCase();
    if (m[1] === '.' && (ABBREV.has(lastWord) || /^\p{L}$/u.test(lastWord) || /(?:^|\.)\p{L}\.\p{L}$/u.test(lastWord))) continue;
    out.push({ text: par.slice(last, end), sep: m[3] });
    last = end + m[3].length;
  }
  if (last < par.length) out.push({ text: par.slice(last), sep: '' });
  return out;
}

/** Divide el texto en bloques: párrafos y líneas, conservando exactamente los saltos. */
export function splitBlocks(text) {
  const parts = text.split(/(\n+)/);
  return parts.map((p, i) => ({ text: p, isSep: i % 2 === 1 }));
}

const ES_WORDS = /(?<![\p{L}])(?:el|la|los|las|de|del|que|y|en|un|una|por|con|para|es|son|se|su|sus|al|como|más|pero|fue|fueron|este|esta|entre|también|según|sobre|cuando|muy|sin|hay|ha|han)(?![\p{L}])/giu;
const EN_WORDS = /(?<![\p{L}])(?:the|of|and|to|in|is|are|was|were|for|that|with|as|on|by|this|these|be|been|from|at|an|or|which|it|their|have|has|not|but|also|between)(?![\p{L}])/giu;

export function detectLanguage(text) {
  const es = (text.match(ES_WORDS) || []).length + 3 * (text.match(/[ñ¿¡áéíóú]/giu) || []).length;
  const en = (text.match(EN_WORDS) || []).length;
  if (es === 0 && en === 0) return 'es';
  return es >= en ? 'es' : 'en';
}

/** Limpieza tipográfica y de fluidez que no toca el contenido. */
export function tidy(s, lang) {
  let t = s
    .replace(/[ \t]{2,}/g, ' ')
    .replace(/ +([,.;:!?…)\]»”])/g, '$1')
    .replace(/([(\[«“¿¡]) +/g, '$1')
    .replace(/,(?=[\p{L}])/gu, ', ')
    .replace(/,\s*,/g, ',')
    .replace(/\.\s*\./g, '.');
  if (lang === 'es') t = t.replace(/(?<![\p{L}])([dD]e|[aA]) el(?![\p{L}])/gu, (m, p) => (p[0] === 'd' || p[0] === 'D' ? p[0] + 'el' : p[0] + 'l'));
  return t;
}

// Inicial mayúscula de la oración si las reglas la dejaron en minúscula.
export function capitalizeSentence(s) {
  return s.replace(/^(\s*[¿¡"“«(\[]*)(\p{Ll})/u, (m, a, b) => a + b.toUpperCase());
}
