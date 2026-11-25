// Conservación del significado: para cada texto, cada nivel y varias semillas,
// el resultado debe conservar citas, referencias, cifras, unidades, términos
// clínicos, negaciones y grado de certeza.
import test from 'node:test';
import assert from 'node:assert/strict';
import { paraphraseRules } from '../src/engine/index.js';
import { NEGATION, HEDGES, negCount } from '../src/engine/verify.js';
import { CORPUS } from './fixtures.mjs';

const nums = (s) => (s.match(/\d+(?:[.,]\d+)*/g) || []).sort();
const hedges = (s, lang) => (s.match(HEDGES[lang]) || []).map((h) => h.toLowerCase()).sort();

for (const [lang, text] of CORPUS) {
  for (const mode of ['general', 'nursing']) {
    test(`${lang}/${mode}: invariantes en los 5 niveles × 6 semillas`, () => {
      for (let level = 1; level <= 5; level++) {
        for (let seed = 1; seed <= 6; seed++) {
          const r = paraphraseRules(text, { lang, level, mode, seed });
          const tag = `${lang} L${level} s${seed}`;
          assert.ok(r.integrity.ok, `${tag}: ${JSON.stringify(r.integrity.checks.filter((c) => !c.ok))}`);
          assert.deepEqual(nums(r.result), nums(text), `${tag}: cifras`);
          for (const p of r.protected) assert.ok(r.result.includes(p.text), `${tag}: falta «${p.text}»`);
          assert.equal(negCount(r.result, lang), negCount(text, lang), `${tag}: negaciones`);
          assert.equal(hedges(r.result, lang).length, hedges(text, lang).length, `${tag}: certeza`);
          const refStart = text.search(/^(Referencias|References)$/m);
          if (refStart > 0) assert.ok(r.result.endsWith(text.slice(refStart)), `${tag}: referencias alteradas`);
          assert.ok(!/[-]/.test(r.result), `${tag}: marcadores sin restaurar`);
          const bad = (x) => (x.match(/ {2,}| [,;:]|\s\.(?!\d)/g) || []).length;
          assert.ok(bad(r.result) <= bad(text), `${tag}: espacios`);
        }
      }
    });
  }
}

test('la intensidad aumenta con el nivel', () => {
  for (const [lang, text] of CORPUS) {
    const pct = [1, 2, 3, 4, 5].map((level) => paraphraseRules(text, { lang, level, seed: 3 }).stats.changedPct);
    assert.ok(pct[0] < pct[4], `${lang}: ${pct}`);
    assert.ok(pct[0] <= pct[2] && pct[2] <= pct[4], `${lang}: ${pct}`);
  }
});

test('determinista con la misma semilla; varía con otra', () => {
  const [, t] = CORPUS[0];
  const a = paraphraseRules(t, { level: 5, seed: 11 }).result;
  const b = paraphraseRules(t, { level: 5, seed: 11 }).result;
  assert.equal(a, b);
  const variants = new Set([1, 2, 3, 4, 5, 6, 7, 8].map((seed) => paraphraseRules(t, { level: 5, seed }).result));
  assert.ok(variants.size > 1);
});

test('no inventa: el texto nunca contiene palabras con mayúscula nuevas ni años nuevos', () => {
  for (const [lang, text] of CORPUS) for (let level = 1; level <= 5; level++) {
    const r = paraphraseRules(text, { lang, level, seed: 5 });
    const years = (s) => (s.match(/\b(?:19|20)\d{2}\b/g) || []).sort().join();
    assert.equal(years(r.result), years(text));
    const caps = (s) => new Set((s.match(/(?<=[a-zñáéíóú,;]\s)\p{Lu}\p{Ll}+/gu) || []));
    const orig = new Set([...(text.match(/\p{L}+/gu) || [])].map((w) => w.toLowerCase()));
    for (const c of caps(r.result)) assert.ok(orig.has(c.toLowerCase()), `${lang} L${level}: nombre nuevo «${c}»`);
  }
});
