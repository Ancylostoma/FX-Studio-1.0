// LinguaBridge AI · motor de parafraseo (API pública).
//
//   import { paraphrase, paraphraseRules } from './src/engine/index.js';
//   const r = paraphraseRules(texto, { lang: 'auto', level: 3, mode: 'nursing' });
//   const r2 = await paraphrase(texto, { level: 4, llm: { provider: createOllamaProvider() } });
//
// Flujo: detectar idioma → proteger (citas, cifras, términos) → reglas por
// oración con verificación y reversión → (opcional) modelo de lenguaje por
// párrafo con verificación estricta → restaurar → informe y comparación.

import { protect, restore } from './protect.js';
import { splitBlocks, splitSentences, detectLanguage, tidy, capitalizeSentence } from './text.js';
import { applyEntries, removeDuplicateWords, fixEnglishArticles } from './transforms/lexical.js';
import { reorder, voiceEN, voiceES } from './transforms/syntax.js';
import { checkProtected, checkLLM, finalReport } from './verify.js';
import { diffWords, diffStats } from './diff.js';
import { LEVELS, clampLevel } from './levels.js';
import { buildPrompt, retryNote, parseModelText } from './llm/prompt.js';
import { mulberry32, hashString, phRegex, phIndex, makePh } from './util.js';
import { DO_NOT_USE } from './lexicon/clinical.js';
import * as ES from './lexicon/es.js';
import * as EN from './lexicon/en.js';

export { LEVELS } from './levels.js';
export { detectLanguage } from './text.js';
export { diffWords, diffStats } from './diff.js';
export { protect, restore } from './protect.js';
export * from './llm/providers.js';

export const VERSION = '1.0.0';
export const MODES = Object.freeze({ general: 'General / académico', nursing: 'Enfermería y ciencias de la salud' });

function prepare(text, opts = {}) {
  const input = String(text ?? '').replace(/\r\n?/g, '\n');
  const detected = detectLanguage(input);
  const lang = opts.lang === 'es' || opts.lang === 'en' ? opts.lang : detected;
  const level = clampLevel(opts.level ?? 3);
  const mode = opts.mode === 'nursing' ? 'nursing' : 'general';
  const seed = Number.isFinite(opts.seed) ? opts.seed >>> 0 : (hashString(input) ^ level) >>> 0;
  const prot = protect(input, { lang, mode, customTerms: opts.protectedTerms || [] });
  const warnings = [];
  if (opts.lang && opts.lang !== 'auto' && opts.lang !== detected && input.length > 80)
    warnings.push(lang === 'es' ? 'El texto parece estar en inglés, pero se procesa como español.' : 'The text looks Spanish but is being processed as English.');
  if (mode === 'nursing') {
    const seen = new Set();
    for (const { re, msg } of DO_NOT_USE) { re.lastIndex = 0; if (re.test(input) && !seen.has(msg)) { seen.add(msg); warnings.push('Seguridad clínica: ' + msg); } }
  }
  return { input, lang, level, mode, seed, cfg: LEVELS[level], lex: lang === 'es' ? ES : EN, prot, warnings };
}

const restoreText = (s, spans) => restore(s, spans);
const hasWords = (s) => /\p{L}/u.test(s.replace(phRegex(), ''));

function processSentence(s0, ctx, rng) {
  const { lang, cfg, lex, mode, prot } = ctx;
  let s = s0;
  const changes = [];
  const run = (entries, prob, tier, reason) => {
    const r = applyEntries(s, entries, { rng, prob, tier, reason });
    s = r.text; changes.push(...r.changes);
  };
  if (cfg.redundancy) {
    run(lex.REDUNDANCY, 1, 'redundancy', 'Redundancia');
    const d = removeDuplicateWords(s, lex.DUPLICATE_OK); s = d.text; changes.push(...d.changes);
  }
  if (mode === 'nursing') run(lex.PERSON_FIRST, 1, 'person-first', 'Lenguaje centrado en la persona (APA 7)');
  if (cfg.expressions) run(lex.EXPRESSIONS, cfg.expressions, 'expression', 'Expresión equivalente');
  if (cfg.synonyms) run(lex.SYNONYMS, cfg.synonyms, 'synonym', 'Sinónimo contextual');
  s = tidy(s, lang);
  if (lang === 'en') {
    const changed = new Set(changes.map((c) => c.to.split(/\s+/)[0]?.toLowerCase()).filter(Boolean));
    s = fixEnglishArticles(s, changed);
  }
  const syntaxCtx = { lang, spans: prot.spans, allowActiveToPassive: cfg.activeToPassive };
  if (cfg.voice && rng() < cfg.voice) {
    const v = lang === 'es' ? voiceES(s, syntaxCtx) : voiceEN(s, syntaxCtx);
    if (v) { changes.push({ tier: 'voice', from: s, to: v.text, reason: v.note }); s = v.text; }
  }
  if (cfg.reorder && rng() < cfg.reorder) {
    const r = reorder(s, syntaxCtx);
    if (r) { changes.push({ tier: 'reorder', from: s, to: r.text, reason: r.note }); s = r.text; }
  }
  if (/^\s*[¿¡"“«(\[]*\p{Lu}/u.test(s0)) s = capitalizeSentence(s);
  const check = checkProtected(s0, s, lang);
  if (!check.ok) return { text: s0, changes: [], reverted: check.failed };
  return { text: s, changes, reverted: null };
}

function rulesBlocks(ctx) {
  const rng = mulberry32(ctx.seed);
  const blocks = splitBlocks(ctx.prot.text);
  const out = [];
  const changes = [];
  let reverted = 0;
  blocks.forEach((b, bi) => {
    if (b.isSep || !hasWords(b.text)) { out.push({ ...b, rules: b.text, hasText: false }); return; }
    const sents = splitSentences(b.text);
    let res = '';
    sents.forEach((st, si) => {
      const r = processSentence(st.text, ctx, rng);
      if (r.reverted) reverted++;
      for (const c of r.changes) changes.push({ ...c, block: bi, sentence: si });
      res += r.text + st.sep;
    });
    out.push({ ...b, rules: res, hasText: true });
  });
  return { blocks: out, changes, reverted };
}

function finish(ctx, protectedResult, changes, extra = {}) {
  const { spans } = ctx.prot;
  const result = restoreText(protectedResult, spans);
  const integrity = finalReport(ctx.input, result, spans, ctx.lang);
  const diff = diffWords(ctx.input, result);
  return {
    original: ctx.input, result, lang: ctx.lang, level: ctx.level, levelName: ctx.cfg.name, mode: ctx.mode, seed: ctx.seed,
    engine: 'reglas', protected: spans.map(({ kind, text }) => ({ kind, text })),
    changes: changes.map((c) => ({ ...c, from: restoreText(c.from, spans), to: restoreText(c.to, spans) })),
    warnings: [...ctx.warnings], integrity, diff, stats: diffStats(diff), ...extra,
  };
}

/** Parafraseo solo con reglas (síncrono, sin red). */
export function paraphraseRules(text, opts = {}) {
  const ctx = prepare(text, opts);
  const { blocks, changes, reverted } = rulesBlocks(ctx);
  const r = finish(ctx, blocks.map((b) => b.rules).join(''), changes, { revertedSentences: reverted });
  if (!r.integrity.ok) {
    // Red de seguridad: nunca se entrega un texto que no supera las comprobaciones.
    const safe = finish(ctx, ctx.prot.text, [], { revertedSentences: reverted });
    safe.warnings.push('Las comprobaciones de integridad fallaron; se devuelve el texto original sin cambios.');
    return safe;
  }
  return r;
}

// --- Modelo de lenguaje ---
function toModelMarkers(block) {
  const map = [];
  const text = block.replace(phRegex(), (ph) => { map.push(phIndex(ph)); return `⟦${map.length}⟧`; });
  return { text, map };
}
function fromModelMarkers(text, map) {
  let bad = false;
  const out = text.replace(/⟦\s*(\d+)\s*⟧/g, (m, n) => {
    const idx = map[Number(n) - 1];
    if (idx === undefined) { bad = true; return m; }
    return makePh(idx);
  });
  return { text: out, bad };
}

/**
 * Parafraseo con reglas y, si se indica un proveedor, mejora semántica con un modelo de lenguaje.
 * opts.llm = { provider, enabled = true, temperature = 0.2, retries = 1 }
 */
export async function paraphrase(text, opts = {}) {
  const llm = opts.llm;
  if (!llm?.provider || llm.enabled === false) return paraphraseRules(text, opts);
  const ctx = prepare(text, opts);
  const { blocks, changes, reverted } = rulesBlocks(ctx);
  const info = { used: true, provider: llm.provider.name, model: llm.provider.model, accepted: 0, rejected: 0, errors: [] };
  const finalBlocks = [];
  const llmChanges = [];
  const llmBlocks = new Set();
  for (const [bi, b] of blocks.entries()) {
    if (!b.hasText) { finalBlocks.push(b.rules); continue; }
    const lead = b.text.match(/^\s*/)[0], trail = b.text.match(/\s*$/)[0];
    const core = b.text.trim();
    const { text: modelIn, map } = toModelMarkers(core);
    const { system, prompt } = buildPrompt({ text: modelIn, lang: ctx.lang, level: ctx.level, mode: ctx.mode });
    let accepted = null, lastFail = [];
    for (let attempt = 0; attempt <= (llm.retries ?? 1) && !accepted; attempt++) {
      try {
        const raw = await llm.provider.generate({
          system, prompt: attempt ? prompt + retryNote(ctx.lang, lastFail) : prompt,
          temperature: attempt ? 0 : (llm.temperature ?? 0.2), seed: ctx.seed + attempt, json: true,
        });
        const back = fromModelMarkers(parseModelText(raw), map);
        if (back.bad) { lastFail = ['marcadores desconocidos']; continue; }
        const cand = tidy(back.text, ctx.lang);
        const chk = checkLLM(core, cand, ctx.lang, ctx.level);
        if (chk.ok) accepted = cand; else lastFail = chk.failed;
      } catch (e) { lastFail = ['error: ' + e.message]; info.errors.push(e.message); break; }
    }
    if (accepted) {
      info.accepted++;
      llmBlocks.add(bi);
      llmChanges.push({ tier: 'llm', from: core, to: accepted, reason: 'Reformulación del modelo verificada' });
      finalBlocks.push(lead + accepted + trail);
    } else {
      info.rejected++;
      if (lastFail.length) ctx.warnings.push(`Párrafo con reglas: la propuesta del modelo se descartó (${lastFail.join('; ')}).`);
      finalBlocks.push(b.rules);
    }
  }
  const all = [...changes.filter((c) => !llmBlocks.has(c.block)), ...llmChanges];
  const r = finish(ctx, finalBlocks.join(''), all, { revertedSentences: reverted, llm: info, engine: info.accepted ? 'híbrido (reglas + modelo)' : 'reglas (el modelo no aportó cambios válidos)' });
  if (!r.integrity.ok) {
    const safe = paraphraseRules(text, opts);
    safe.warnings.push('La versión con el modelo no superó las comprobaciones finales; se usa la de reglas.');
    safe.llm = { ...info, accepted: 0 };
    return safe;
  }
  return r;
}
