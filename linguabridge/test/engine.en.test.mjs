import test from 'node:test';
import assert from 'node:assert/strict';
import { paraphraseRules } from '../src/engine/index.js';
import { EN_CLINICO, EN_NURSING } from './fixtures.mjs';

const P = (t, o = {}) => paraphraseRules(t, { lang: 'en', seed: 1, ...o }).result;
const all = (t, o = {}) => [1, 2, 3, 4, 5, 6, 7, 8].map((seed) => paraphraseRules(t, { lang: 'en', ...o, seed }).result);

test('detects English', () => {
  assert.equal(paraphraseRules(EN_CLINICO).lang, 'en');
});

test('level 1: redundancy', () => {
  assert.equal(P('The end result was repeated again in the year 2021.', { level: 1 }), 'The result was repeated in 2021.');
  assert.equal(P('Each and every patient received the the same care.', { level: 1 }), 'Every patient received the same care.');
  assert.equal(P('She said that that plan had had problems.', { level: 1 }), 'She said that that plan had had problems.');
});

test('level 2: wordy expressions', () => {
  assert.equal(P('In order to reduce falls, nurses carried out rounds due to the fact that risk was high.', { level: 3, seed: 4 }),
    'To reduce falls, nurses conducted rounds because risk was high.');
});

test('a/an agreement after substitution', () => {
  const out = all('It was a crucial step.', { level: 5 });
  assert.ok(out.includes('It was an essential step.'), out.join('\n'));
});

test('"improve" is only replaced when transitive', () => {
  for (const o of all('Pain improved after the intervention, which improved the quality of sleep.', { level: 5 })) {
    assert.ok(o.startsWith('Pain improved after'), o);
  }
});

test('voice: passive with agent → active; active → passive at level 5', () => {
  const a = all('The dose was adjusted by the nurse.', { level: 4 });
  assert.ok(a.includes('The nurse adjusted the dose.'), a.join('\n'));
  const b = all('The questionnaire was written by the research team.', { level: 5 });
  assert.ok(b.includes('The research team wrote the questionnaire.'), b.join('\n'));
  const c = all('The researchers analyzed the data.', { level: 5 });
  assert.ok(c.includes('The data were analyzed by the researchers.'), c.join('\n'));
  const d = all('Nurses recorded the pain score.', { level: 5 });
  assert.ok(d.includes('The pain score was recorded by nurses.'), d.join('\n'));
  // "by" that is not an agent
  assert.ok(all('The dose was adjusted by weight.', { level: 5 }).every((o) => o === 'The dose was adjusted by weight.'));
  // no active → passive at level 4
  assert.ok(all('The researchers analyzed the data.', { level: 4 }).every((o) => !o.includes('were analyzed')));
});

test('reordering keeps negation scope safe', () => {
  const a = all('Although the risk was low, nurses increased monitoring.', { level: 5 });
  assert.ok(a.includes('Nurses increased monitoring, although the risk was low.'), a.join('\n'));
  assert.ok(all('The patient did not improve because the dose was low.', { level: 5 }).every((o) => !o.startsWith('Because')));
});

test('nursing mode: person-first language and protected nursing terms', () => {
  const r = paraphraseRules(EN_NURSING, { lang: 'en', mode: 'nursing', level: 3, seed: 2 });
  assert.ok(r.result.includes('Older adults with cognitive impairment'), r.result);
  for (const t of ['nursing assessment', 'Braden Scale', 'Morse Fall Scale', 'nursing diagnosis', '"Risk for falls"', 'NANDA-I', 'hand hygiene', 'pressure injury', 'hospital-acquired infection', 'visual analogue scale', '"nursing care is not only a task but a relationship" (p. 12)', 'Jones (2018)']) {
    assert.ok(r.result.includes(t), `missing ${t}`);
  }
});

test('"significant" (statistical) and clinical terms never replaced', () => {
  for (const o of all('The difference was statistically significant and clinically significant for blood pressure.', { level: 5 })) {
    assert.ok(o.includes('statistically significant') && o.includes('clinically significant') && o.includes('blood pressure'), o);
  }
});
