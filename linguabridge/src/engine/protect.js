// Protección de elementos que no deben reformularse: referencias, citas APA 7,
// citas textuales, URL/DOI, estadísticos, cifras con unidades, terminología
// clínica, fármacos, siglas y términos del usuario.
//
// Todos los detectores se ejecutan sobre el texto ORIGINAL y devuelven tramos
// [inicio, fin). Se resuelven los solapamientos (prioridad y longitud) y cada
// tramo aceptado se sustituye por un marcador de un único carácter-token.

import { CLINICAL_ES, CLINICAL_EN, NURSING_ES, NURSING_EN, DRUG_SUFFIX_SRC } from './lexicon/clinical.js';
import { makePh, phRegex, phIndex, escapeRe, PH_MAX } from './util.js';

const L = '\\p{L}';
const NB = `(?<![${L}\\d])`; // sin letra/dígito antes
const NA = `(?![${L}\\d])`; // sin letra/dígito después
const YEAR = '(?:1[6-9]|20)\\d{2}[a-z]?';
const NODATE = '(?:n\\.\\s?d\\.|s\\.\\s?f\\.|in press|en prensa)';

// Palabras con mayúscula que abren oraciones y NO forman parte del autor citado.
const NOT_NAME = '(?!(?:Según|According|In|En|The|El|La|Los|Las|As|Como|De|For|Por|Para|Although|Aunque|While|When|Cuando|Si|If|And|Y|But|Pero|Also|Además|This|These|Este|Esta|Estos|Estas|Since|Desde|Following|Tras|After|Before|Antes|Después|However|Sin|No|A|An|Al|Del|Con|With|From|Hence|Thus|Asimismo)\\b)';
const NAME = `${NOT_NAME}[A-ZÁÉÍÓÚÑÜ][\\p{L}'’\\-]+`;
const NAMESEQ = `${NAME}(?:,?\\s(?:(?:de|del|de\\sla|van|von|da|di|le|of|for|the|and|y|e|&)\\s)?${NAME}){0,6}`;

const UNITS = [
  '%', '‰', '°C', 'ºC', '°F', '°', 'mg/kg/día', 'mg/kg/day', 'mg/kg/h', 'mg/kg', 'mcg/kg/min', 'µg/kg/min', 'mcg/kg', 'µg/kg',
  'mg/dL', 'mg/dl', 'g/dL', 'g/dl', 'g/L', 'g/l', 'mmol/L', 'mmol/l', 'mEq/L', 'mEq/l', 'mOsm/kg', 'UI/kg', 'IU/kg', 'U/kg',
  'UI/h', 'IU/h', 'U/h', 'mL/h', 'ml/h', 'mL/kg/h', 'ml/kg/h', 'mL/min', 'ml/min', 'L/min', 'l/min', 'lpm', 'rpm', 'bpm',
  'mmHg', 'cmH2O', 'cm H2O', 'kPa', 'kcal', 'kg/m2', 'kg/m²', 'mg', 'mcg', 'µg', 'ug', 'ng', 'kg', 'g', 'mL', 'ml', 'cc',
  'dL', 'dl', 'L', 'l', 'mUI', 'UI', 'IU', 'U', 'mm', 'cm', 'km', 'm', 'h', 'hrs', 'hr', 'horas', 'hora', 'hours', 'hour',
  'minutos', 'minuto', 'minutes', 'minute', 'min', 'segundos', 'seconds', 'seg', 's', 'días', 'día', 'days', 'day', 'd',
  'semanas', 'semana', 'weeks', 'week', 'meses', 'mes', 'months', 'month', 'años', 'año', 'years', 'year', 'veces', 'vez',
  'times', 'gotas', 'drops', 'comprimidos', 'comprimido', 'tablets', 'tablet', 'puffs', 'puff', 'dosis', 'doses', 'dose',
  'ampollas', 'ampolla', 'viales', 'vial', 'vials', 'cápsulas', 'capsules', 'litros', 'liters', 'litres', 'gramos', 'grams',
  'miligramos', 'milligrams', 'mililitros', 'millilitres', 'milliliters', 'puntos', 'points', 'unidades', 'units',
].sort((a, b) => b.length - a.length).map(escapeRe).join('|');

const NUM = `[-−+±]?(?:\\d+(?:[ \\u00A0\\u202F]\\d{3}(?!\\d))*(?:[.,]\\d+)*|[.,]\\d+)(?::\\d{2})?(?:(?:st|nd|rd|th|\\.?º|\\.?ª)${NA})?`;
const RANGE_SEP = '(?:\\s?[-–—/×x]\\s?|\\s(?:to|a|al|y|and|-)\\s)';
const NUMBER_SRC = `(?<![${L}\\d.,])${NUM}(?:${RANGE_SEP}${NUM})?(?:\\s?(?:${UNITS})(?![${L}\\d]))?`;

const NUMBER_WORDS = [
  'dos', 'tres', 'cuatro', 'cinco', 'seis', 'siete', 'ocho', 'nueve', 'diez', 'once', 'doce', 'quince', 'veinte', 'treinta',
  'cuarenta', 'cincuenta', 'cien', 'ciento', 'cientos', 'mil', 'millón', 'millones', 'doble', 'triple', 'mitad',
  'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve', 'fifteen', 'twenty', 'thirty',
  'forty', 'fifty', 'hundred', 'hundreds', 'thousand', 'thousands', 'million', 'millions', 'twice', 'thrice', 'half', 'double',
  'triple', 'dozen', 'docena', 'primero', 'primera', 'primer', 'segundo', 'segunda', 'tercero', 'tercera', 'tercer', 'cuarto',
  'cuarta', 'quinto', 'quinta', 'first', 'second', 'third', 'fourth', 'fifth', 'diaria', 'diario', 'diariamente', 'daily',
  'semanal', 'weekly', 'mensual', 'monthly', 'anual', 'annual', 'annually', 'bianual',
];

function termsRegex(terms) {
  const alt = [...new Set(terms)].sort((a, b) => b.length - a.length).map((t) => escapeRe(t).replace(/\s+/g, '\\s+')).join('|');
  return new RegExp(`${NB}(?:${alt})${NA}`, 'giu');
}
const cache = new Map();
function cached(key, build) { if (!cache.has(key)) cache.set(key, build()); return cache.get(key); }

// Cada detector: [tipo, prioridad (menor = más importante), regex | función(texto)→tramos]
function detectors(lang, mode, customTerms) {
  const clinical = cached(`clin:${lang}:${mode}`, () => {
    const base = lang === 'es' ? CLINICAL_ES : CLINICAL_EN;
    const extra = mode === 'nursing' ? (lang === 'es' ? NURSING_ES : NURSING_EN) : [];
    // Los términos del otro idioma también se protegen (textos mixtos, préstamos como «odds ratio»).
    const other = lang === 'es' ? CLINICAL_EN : CLINICAL_ES;
    return termsRegex([...base, ...extra, ...other.filter((t) => t.includes(' ') || t.length > 7)]);
  });
  const list = [
    ['references', 0, findReferenceSection],
    // Entrada de referencia APA: «Apellido, A. A., ... (2020). Título...» o autor institucional «Organización Mundial de la Salud. (2021). ...»
    ['reference', 0, /^[ \t]*\p{Lu}[\p{L}'’\-]+(?:[ \-]\p{Lu}[\p{L}'’\-]+)*,\s(?:\p{Lu}\.[ \-]?){1,3}[^\n]*?\((?:(?:1[6-9]|20)\d{2}[a-z]?|n\.\s?d\.|s\.\s?f\.)(?:,[^)\n]{0,40})?\)\.\s[^\n]+$/gmu],
    ['reference', 0, /^[ \t]*\p{Lu}[\p{L}'’\-]*(?:\s(?:\p{Lu}[\p{L}'’\-]*|de|del|la|las|los|y|e|of|the|and|for|para|on|en|&))*\.\s\((?:(?:1[6-9]|20)\d{2}[a-z]?|n\.\s?d\.|s\.\s?f\.)(?:,[^)\n]{0,40})?\)\.\s[^\n]+$/gmu],
    ['reference', 0, /^[ \t]*[^\n]*\b(?:doi\.org\/|doi:\s?10\.)[^\n]*$/gimu],
    ['quote', 1, /“[^”\n]{1,1500}”|«[^»\n]{1,1500}»|"[^"\n]{1,1500}"|‘[^’\n]{1,400}’(?![\p{L}])/gu],
    ['url', 1, /\bhttps?:\/\/[^\s<>"]+[^\s<>".,;:)]|\bdoi:\s?10\.\d{4,9}\/[^\s]+[^\s.,;:)]|\b10\.\d{4,9}\/[^\s]+[^\s.,;:)]|\bwww\.[^\s]+[^\s.,;:)]|[\w.+-]+@[\w-]+\.[\w.]+\w/giu],
    ['citation', 2, new RegExp(`\\((?=[^()\\n]*(?:${NB}${YEAR}${NA}|${NODATE}))[^()\\n]{2,400}\\)`, 'giu')],
    ['citation', 2, new RegExp(`${NAMESEQ}(?:\\set\\sal\\.|\\sy\\scol(?:s|aboradores)?\\.?)?\\s\\((?:${YEAR}|${NODATE})(?:[,;]\\s*(?:pp?\\.|párr\\.|para\\.)\\s*\\d+(?:[-–]\\d+)?)?\\)`, 'gu')],
    ['stat', 3, /(?<![\p{L}\d])(?:p|n|N|M|SD|DE|DT|Mdn|Me|OR|RR|HR|IRR|aOR|ORa|r|rs|R2|R²|t|F|z|U|d|g|β|B|SE|EE|df|gl|χ2|χ²|X2|η2|η²|ICC|AUC|κ|α|ρ)\s*(?:\([^()\n]{1,15}\)\s*)?[=<>≤≥]\s*[-−–]?(?:\d+(?:[.,]\d+)?|[.,]\d+)/gu],
    ['stat', 3, /(?<![\p{L}\d])(?:(?:IC|CI)\s?(?:del\s)?\d{2}\s?%|\d{2}\s?%\s?(?:IC|CI))\s*[:=,]?\s*[[(]?\s*[-−]?\d+(?:[.,]\d+)?\s*(?:[-–;,]|to|a|al)\s*[-−]?\d+(?:[.,]\d+)?\s*[\])]?/gu],
    ['clinical', 3, clinical],
    ['clinical', 3, new RegExp(`${NB}\\p{L}{2,}${DRUG_SUFFIX_SRC}(?:s|es)?${NA}`, 'giu')],
    ['clinical', 3, /(?<![\p{L}\d])(?:SpO2|SaO2|PaO2|PaCO2|FiO2|EtCO2|HbA1c|pH|q\d{1,2}h|c\/\d{1,2}\s?h|b\.i\.d\.|t\.i\.d\.|q\.i\.d\.|p\.r\.n\.|PRN|NANDA-I|NANDA|NIC|NOC|SBAR|ISBAR)(?![\p{L}\d])/gu],
    ['acronym', 3, /(?<![\p{L}\d])\p{Lu}[\p{Lu}\d]*\p{Lu}[\p{Lu}\d]*(?:[-/][\p{Lu}\d]+)*(?:s)?(?![\p{L}\d])|(?<![\p{L}\d])\p{Lu}\d+(?![\p{L}\d])/gu],
    ['number', 4, new RegExp(NUMBER_SRC, 'gu')],
    ['number', 5, cached('numwords', () => termsRegex(NUMBER_WORDS))],
    // Red de seguridad: ningún dígito queda sin proteger aunque pierda un solapamiento.
    ['number', 6, /(?<![\p{L}\d])\d+(?:[.,]\d+)*|\d+/gu],
  ];
  if (customTerms?.length) list.unshift(['custom', 0.5, termsRegex(customTerms.filter((t) => t && t.trim()).map((t) => t.trim()))]);
  return list;
}

function findReferenceSection(text) {
  const re = /^[ \t]*(?:#+\s*)?(?:Referencias(?: bibliográficas)?|Bibliografía|Bibliografía consultada|Literatura citada|References|Reference list|Works cited|Bibliography)[ \t]*:?[ \t]*$/gimu;
  const m = re.exec(text);
  return m ? [{ start: m.index, end: text.length }] : [];
}

function runDetector(text, det) {
  if (typeof det === 'function') return det(text);
  const out = [];
  det.lastIndex = 0;
  let m;
  while ((m = det.exec(text))) {
    if (m[0].length === 0) { det.lastIndex++; continue; }
    let s = m.index, e = m.index + m[0].length;
    while (e > s && /\s/.test(text[e - 1])) e--; // sin espacios finales
    while (s < e && /[ \t]/.test(text[s])) s++;
    if (e > s) out.push({ start: s, end: e });
  }
  return out;
}

/**
 * Protege el texto. Devuelve { text, spans } donde spans[i] = { id, kind, text }.
 * El texto devuelto lleva marcadores en lugar de los elementos protegidos.
 */
export function protect(text, { lang = 'es', mode = 'general', customTerms = [] } = {}) {
  const cands = [];
  for (const [kind, prio, det] of detectors(lang, mode, customTerms)) {
    for (const r of runDetector(text, det)) cands.push({ ...r, kind, prio });
  }
  cands.sort((a, b) => a.prio - b.prio || (b.end - b.start) - (a.end - a.start) || a.start - b.start);
  const accepted = [];
  for (const c of cands) {
    if (accepted.some((a) => c.start < a.end && a.start < c.end)) continue;
    accepted.push(c);
  }
  accepted.sort((a, b) => a.start - b.start);
  if (accepted.length > PH_MAX) accepted.length = PH_MAX;
  const spans = [];
  let out = '', pos = 0;
  for (const a of accepted) {
    out += text.slice(pos, a.start);
    const id = spans.length;
    spans.push({ id, kind: a.kind, text: text.slice(a.start, a.end) });
    out += makePh(id);
    pos = a.end;
  }
  out += text.slice(pos);
  return { text: out, spans };
}

export function restore(text, spans) {
  return text.replace(phRegex(), (ph) => spans[phIndex(ph)]?.text ?? ph);
}

export const spanOf = (ph, spans) => spans[phIndex(ph)];
