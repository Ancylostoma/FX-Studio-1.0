// Transformaciones sintácticas conservadoras: voz activa/pasiva y reordenación
// de oraciones subordinadas. Solo actúan sobre patrones simples y reconocibles
// (una oración, sin comas internas ambiguas, núcleos conocidos). Si hay duda,
// devuelven null y la oración se queda como estaba.

import { PH_SRC, isPh, capitalizeFirst, lowerFirst } from '../util.js';
import { spanOf } from '../protect.js';
import * as ES from '../lexicon/es.js';
import * as EN from '../lexicon/en.js';

const W = `(?:[\\p{L}][\\p{L}'’\\-]*|${PH_SRC})`;
const NEG = { es: /(?<![\p{L}])(?:no|nunca|jamás|ningún|ninguna|ninguno|nadie|nada|ni|tampoco|sin)(?![\p{L}])/iu, en: /(?<![\p{L}])(?:not|no|never|none|nobody|nothing|neither|nor|without|cannot)(?![\p{L}])|n['’]t(?![\p{L}])/iu };
const words = (s) => s.trim().split(/\s+/);

// ¿Puede la primera palabra pasar a minúscula (o quedarse como está) al moverse al interior?
function canMoveInside(seg, lang, spans) {
  const first = words(seg)[0];
  if (isPh(first)) {
    const sp = spanOf(first, spans);
    if (!sp) return false;
    if (sp.kind === 'number' || sp.kind === 'stat') return true;
    // Citas, siglas y URL conservan su mayúscula; un término clínico capitalizado no.
    return sp.kind === 'clinical' || sp.kind === 'quote' ? !/^\p{Lu}/u.test(sp.text) : true;
  }
  const set = lang === 'es' ? ES.LOWERABLE : EN.LOWERABLE;
  return set.has(first.toLowerCase());
}
// ¿Puede la primera palabra abrir la oración? (APA: no empezar con una cifra)
function canStartSentence(seg, spans) {
  const first = words(seg)[0];
  if (!isPh(first)) return true;
  const sp = spanOf(first, spans);
  if (!sp || sp.kind === 'number' || sp.kind === 'stat') return false;
  // Un término protegido en minúscula no puede capitalizarse sin alterarlo.
  return !/^\p{Ll}/u.test(sp.text);
}
// Palabra «real» de un marcador: primera (núcleo en español) o última (núcleo en inglés).
const wordOf = (w, spans, last = false) => {
  if (!isPh(w)) return w;
  const ws = (spanOf(w, spans)?.text || '').split(/\s+/);
  return (last ? ws[ws.length - 1] : ws[0]) || '';
};
function toInside(seg, spans) {
  const first = words(seg)[0];
  return isPh(first) ? seg : lowerFirst(seg);
}
const phText = (w, spans) => (isPh(w) ? spanOf(w, spans)?.text ?? '' : w);

// ======================= REORDENACIÓN =======================
const ES_FRONT = ['Si bien', 'Aunque', 'Dado que', 'Ya que', 'Puesto que', 'A pesar de', 'Pese a', 'Mientras que', 'Según', 'De acuerdo con', 'Cuando', 'Si', 'Tras', 'Antes de', 'Después de', 'Debido a', 'Gracias a', 'En caso de'];
const ES_COMMA = new Set(['si bien', 'aunque', 'dado que', 'ya que', 'puesto que', 'a pesar de', 'pese a', 'mientras que', 'según', 'de acuerdo con']);
const ES_CAUSAL_NOCOMMA = new Set(['debido a', 'gracias a']);
const EN_FRONT = ['Although', 'Though', 'Whereas', 'While', 'Because', 'Since', 'When', 'If', 'Unless', 'After', 'Before', 'Once', 'According to', 'Despite', 'In spite of'];
const EN_COMMA = new Set(['although', 'though', 'whereas', 'while', 'according to', 'despite', 'in spite of']);
const EN_CAUSAL = new Set(['because', 'since']);

export function reorder(sentence, { lang, spans }) {
  const m0 = sentence.match(/^(\s*)([\s\S]*?)(\s*)$/);
  const [lead, s, trail] = [m0[1], m0[2], m0[3]];
  const front = lang === 'es' ? ES_FRONT : EN_FRONT;
  const fr = new RegExp(`^(${front.join('|')})\\s+([^,;:]+),\\s+([^,;:]+?)([.!])$`, 'u');
  let m = s.match(fr);
  if (m) {
    const [, marker, sub, main, punct] = m;
    const mk = marker.toLowerCase();
    if (!canStartSentence(main, spans)) return null;
    // «Because X, Y did not…» → «Y did not… because X» introduce ambigüedad de alcance de la negación.
    const causal = lang === 'es' ? ES_CAUSAL_NOCOMMA.has(mk) : EN_CAUSAL.has(mk);
    if (causal && NEG[lang].test(main)) return null;
    if (words(sub).length < 2 || words(main).length < 3) return null;
    const comma = (lang === 'es' ? ES_COMMA : EN_COMMA).has(mk) ? ',' : '';
    const out = `${capitalizeFirst(main)}${comma} ${mk} ${sub}${punct}`;
    return { text: lead + out + trail, kind: 'reorder', note: `Subordinada «${marker}» al final` };
  }
  const back = lang === 'es'
    ? /^([^,;:]+?),\s+(aunque|ya que|puesto que|dado que)\s+([^,;:]+)([.!])$/u
    : /^([^,;:]+?),?\s+(because|although|unless|whereas)\s+([^,;:]+)([.!])$/u;
  m = s.match(back);
  if (m) {
    const [, main, marker, sub, punct] = m;
    if (!canMoveInside(main, lang, spans)) return null;
    if (NEG[lang].test(main) && /because|ya que|puesto que|dado que/.test(marker)) return null;
    if (words(sub).length < 2 || words(main).length < 3) return null;
    if (lang === 'en' && /\b(?:not only|so)\s*$/i.test(main)) return null;
    const out = `${capitalizeFirst(marker)} ${sub}, ${toInside(main, spans)}${punct}`;
    return { text: lead + out + trail, kind: 'reorder', note: `Subordinada «${marker}» al principio` };
  }
  return null;
}

// ======================= VOZ (INGLÉS) =======================
const EN_DET = '(?:the|a|an|this|these|those|each|all|our|their|its|both)';
const EN_SUBJ_PRON = { it: 'it', they: 'them', he: 'him', she: 'her', we: 'us', i: 'me' };
const EN_AGENT_PRON = { them: 'They', us: 'We', him: 'He', her: 'She', me: 'I' };
const EN_BLOCK = /(?<![\p{L}])(?:that|which|who|whom|whose|was|were|is|are|been|being|by|to|not)(?![\p{L}])/iu;
const EN_PREP = /(?<![\p{L}])(?:in|on|at|from|with|for|to|into|during|after|before|between|among|by|via|per|through|under|over|without|within)(?![\p{L}])/iu;

function enAgentOk(agent, spans) {
  const ws = words(agent);
  if (ws.length === 1 && isPh(ws[0])) {
    const sp = spanOf(ws[0], spans);
    return sp && (sp.kind === 'acronym' || (sp.kind === 'citation' && !sp.text.startsWith('(')));
  }
  if (EN_PREP.test(agent) && !/^\S+(?:\s+\S+)?\s+of\s+/i.test(agent)) return false;
  return EN.AGENT_NOUNS.has(wordOf(ws[ws.length - 1], spans, true).toLowerCase());
}
function enIsPlural(np, spans) {
  const ws = words(np.replace(/\s+of\s+.*$/i, ''));
  let head = ws[ws.length - 1];
  if (isPh(head)) head = phText(head, spans).split(/\s+/).pop() || '';
  if (/\band\b/i.test(np)) return true;
  if (EN.PLURAL_INVARIANT.has(head.toLowerCase())) return true;
  if (/^(?:these|those|all|both)$/i.test(ws[0])) return true;
  if (/^(?:this|each|a|an)$/i.test(ws[0])) return false;
  return /s$/i.test(head) && !EN.SINGULAR_S.test(head);
}

export function voiceEN(sentence, { spans, allowActiveToPassive }) {
  const m0 = sentence.match(/^(\s*)([\s\S]*?)(\s*)$/);
  const [lead, s, trail] = [m0[1], m0[2], m0[3]];
  // Pasiva con agente → activa: «The dose was adjusted by the nurse.» → «The nurse adjusted the dose.»
  const pas = new RegExp(`^(${EN_DET}\\s+(?:${W}\\s+){0,6}?${W}|It|They|He|She|We)\\s+(was|were)\\s+(?:(\\p{L}+ly)\\s+)?(\\p{L}+)\\s+by\\s+(${EN_DET}\\s+(?:${W}\\s+){0,3}?${W}|${PH_SRC}|them|us|him|her|me)([.!])$`, 'iu');
  let m = s.match(pas);
  if (m) {
    const [, subj, , adv, pp, agent, punct] = m;
    const ppl = pp.toLowerCase();
    const past = EN.PARTICIPLE_TO_PAST[ppl] || (/ed$/.test(ppl) ? ppl : null);
    if (!past) return null;
    if (EN_BLOCK.test(subj.replace(/^(?:It|They|He|She|We)$/i, ''))) return null;
    const agentPron = EN_AGENT_PRON[agent.toLowerCase()];
    if (!agentPron && !enAgentOk(agent, spans)) return null;
    const obj = EN_SUBJ_PRON[subj.toLowerCase()] ?? (canMoveInside(subj, 'en', spans) ? toInside(subj, spans) : null);
    if (!obj) return null;
    const newSubj = agentPron || capitalizeFirst(agent);
    const out = `${newSubj} ${adv ? adv + ' ' : ''}${past} ${obj}${punct}`;
    return { text: lead + out + trail, kind: 'voice', note: 'Pasiva con agente → activa' };
  }
  if (!allowActiveToPassive) return null;
  // Activa con agente genérico → pasiva (estilo de métodos): «The researchers collected the data.»
  const act = new RegExp(`^((?:The\\s+)?\\p{L}+(?:\\s+\\p{L}+)?)\\s+(\\p{L}+ed|${Object.keys(EN.PAST_TO_PARTICIPLE).join('|')})\\s+(${EN_DET}\\s+(?:${W}\\s+){0,4}?${W})([.!])$`, 'iu');
  m = s.match(act);
  if (m) {
    const [, agent, verb, obj, punct] = m;
    if (!EN.GENERIC_AGENTS.test(agent)) return null;
    const vl = verb.toLowerCase();
    if (!EN.PASSIVIZABLE.has(vl)) return null;
    if (EN_BLOCK.test(obj) || (EN_PREP.test(obj) && !/\sof\s/i.test(obj))) return null;
    if (!canStartSentence(obj, spans)) return null;
    const pp = EN.PAST_TO_PARTICIPLE[vl] || vl;
    const aux = enIsPlural(obj, spans) ? 'were' : 'was';
    const out = `${capitalizeFirst(obj)} ${aux} ${pp} by ${lowerFirst(agent)}${punct}`;
    return { text: lead + out + trail, kind: 'voice', note: 'Activa → pasiva (agente genérico conservado)' };
  }
  return null;
}

// ======================= VOZ (ESPAÑOL) =======================
const ES_DET = '(?:El|La|Los|Las|Un|Una|Unos|Unas|Este|Esta|Estos|Estas|Dicho|Dicha|Dichos|Dichas|Su|Sus|Nuestro|Nuestra|Nuestros|Nuestras|Cada|Todos|Todas|Ambos|Ambas)';
const ES_DET_LOWER = ES_DET.toLowerCase();
const ES_BLOCK = /(?<![\p{L}])(?:que|cual|cuales|quien|quienes|cuyo|cuya|fue|fueron|es|son|se|no|y|o)(?![\p{L}])/iu;
// Participios que suelen funcionar como adjetivos («la presión fue elevada» = estaba alta).
const ES_ADJ_PART = new Set(['elevado', 'prolongado', 'limitado', 'adecuado', 'apropiado', 'indicado', 'complicado', 'moderado', 'cansado',
  'preocupado', 'agitado', 'desorientado', 'orientado', 'pasado', 'conocido', 'querido', 'parecido', 'decidido', 'aburrido', 'dolorido',
  'dormido', 'reducido', 'aumentado', 'alterado', 'disminuido', 'relacionado', 'asociado', 'considerado', 'llamado', 'denominado',
  'situado', 'ubicado', 'destinado', 'basado', 'centrado', 'dedicado', 'encargado', 'interesado', 'capacitado', 'obligado', 'acostumbrado']);

function esPreterite(part) {
  const p = part.toLowerCase();
  const key = p.replace(/[oa]s?$/u, 'o');
  if (key in ES.PARTICIPLE_IRREGULAR) return ES.PARTICIPLE_IRREGULAR[key];
  if (/ado$/.test(key)) return [key.replace(/ado$/, 'ó'), key.replace(/ado$/, 'aron')];
  if (/ido$/.test(key) && !/[aeo]ído$|uido$/.test(key)) return [key.replace(/ido$/, 'ió'), key.replace(/ido$/, 'ieron')];
  return null;
}
const esHead = (np) => words(np)[1] || '';
function esAgentPlural(agent, spans) {
  const ws = words(agent);
  if (ws.length === 1 && isPh(ws[0])) {
    const sp = spanOf(ws[0], spans);
    if (!sp) return null;
    if (sp.kind === 'acronym') return false;
    if (sp.kind === 'citation' && !sp.text.startsWith('(')) return /\set al\.|\sy\scol|\s(?:y|e|and)\s|&/.test(sp.text);
    return null;
  }
  return ES.PLURAL_DET.has(ws[0].toLowerCase());
}
function esAgentOk(agent, spans) {
  const ws = words(agent);
  if (ws.length === 1 && isPh(ws[0])) return esAgentPlural(agent, spans) !== null;
  return ES.DETERMINERS.has(ws[0].toLowerCase()) && ES.AGENT_NOUNS.has(wordOf(ws[1] || '', spans).toLowerCase());
}
function esPersonalA(np, spans) {
  if (!ES.PERSON_NOUNS.has(wordOf(esHead(np), spans).toLowerCase())) return np;
  return /^el\s/iu.test(np) ? 'al ' + np.replace(/^el\s+/iu, '') : 'a ' + np;
}

export function voiceES(sentence, { spans }) {
  const m0 = sentence.match(/^(\s*)([\s\S]*?)(\s*)$/);
  const [lead, s, trail] = [m0[1], m0[2], m0[3]];
  const PART = '(\\p{L}+[oa]s?)'; // esPreterite() decide si es un participio conocido
  // Pasiva perifrástica con agente → activa
  const pas = new RegExp(`^(${ES_DET}\\s+(?:${W}\\s+){0,6}?${W})\\s+(fue|fueron)\\s+(?:(\\p{L}+mente)\\s+)?${PART}\\s+por\\s+((?:${ES_DET_LOWER})\\s+(?:${W}\\s+){0,4}?${W}|${PH_SRC})([.!])$`, 'u');
  let m = s.match(pas);
  if (m) {
    const [, subj, , adv, part, agent, punct] = m;
    if (ES_BLOCK.test(subj) || !esAgentOk(agent, spans)) return null;
    if (ES_ADJ_PART.has(part.toLowerCase().replace(/[oa]s?$/, 'o'))) return null;
    const pret = esPreterite(part);
    const plural = esAgentPlural(agent, spans);
    if (!pret || plural === null) return null;
    if (!canMoveInside(subj, 'es', spans)) return null;
    const obj = esPersonalA(toInside(subj, spans), spans);
    const out = `${capitalizeFirst(agent)} ${pret[plural ? 1 : 0]}${adv ? ' ' + adv : ''} ${obj}${punct}`;
    return { text: lead + out + trail, kind: 'voice', note: 'Pasiva perifrástica con agente → activa' };
  }
  // Pasiva perifrástica sin agente → pasiva refleja («La medicación fue administrada…» → «La medicación se administró…»)
  const ref = new RegExp(`^(${ES_DET}\\s+(?:${W}\\s+){0,6}?${W})\\s+(fue|fueron)\\s+${PART}((?:\\s+[^.!?]*)?)([.!])$`, 'u');
  m = s.match(ref);
  if (m) {
    const [, subj, aux, part, tail, punct] = m;
    if (ES_BLOCK.test(subj)) return null;
    // Un complemento agente («por el comité», «por García (2020)») impide la refleja.
    if (/(?<![\p{L}])por\s+(?:el|la|los|las|un|una|unos|unas|este|esta|estos|estas|su|sus)(?![\p{L}])/iu.test(tail)) return null;
    const porPh = tail.match(new RegExp(`(?<![\\p{L}])por\\s+(${PH_SRC})`, 'u'));
    if (porPh && ['citation', 'acronym'].includes(spanOf(porPh[1], spans)?.kind)) return null;
    if (ES.PERSON_NOUNS.has(wordOf(esHead(subj), spans).toLowerCase())) return null; // «el paciente se trasladó» sería ambiguo
    if (ES_ADJ_PART.has(part.toLowerCase().replace(/[oa]s?$/, 'o'))) return null;
    const pret = esPreterite(part);
    if (!pret) return null;
    const out = `${subj} se ${pret[aux === 'fueron' ? 1 : 0]}${tail}${punct}`;
    return { text: lead + out + trail, kind: 'voice', note: 'Pasiva perifrástica → pasiva refleja' };
  }
  return null;
}
