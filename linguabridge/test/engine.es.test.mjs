import test from 'node:test';
import assert from 'node:assert/strict';
import { paraphraseRules } from '../src/engine/index.js';
import { ES_CLINICO, ES_ENFERMERIA } from './fixtures.mjs';

const P = (t, o = {}) => paraphraseRules(t, { lang: 'es', seed: 1, ...o }).result;
const all = (t, o = {}) => [1, 2, 3, 4, 5, 6, 7, 8].map((seed) => paraphraseRules(t, { lang: 'es', ...o, seed }).result);

test('detecta el español', () => {
  assert.equal(paraphraseRules(ES_CLINICO).lang, 'es');
});

test('nivel 1: redundancias y palabras repetidas', () => {
  assert.equal(P('El paciente volvió a repetir la prueba en el año 2021.', { level: 1 }), 'El paciente repitió la prueba en 2021.');
  assert.equal(P('Hace cinco años atrás se implantó el protocolo de de seguridad.', { level: 1 }), 'Hace cinco años se implantó el protocolo de seguridad.');
  assert.equal(P('El resultado final fue más óptimo de lo esperado.', { level: 1 }), 'El resultado fue óptimo de lo esperado.');
  assert.equal(P('Con el objetivo de evaluar el dolor.', { level: 1 }), 'Con el objetivo de evaluar el dolor.', 'el nivel 1 no cambia expresiones');
});

test('nivel 2: expresiones con flexión correcta', () => {
  const out = all('Las enfermeras llevaron a cabo la intervención debido a que el riesgo era alto.', { level: 2 });
  assert.ok(out.includes('Las enfermeras realizaron la intervención porque el riesgo era alto.') || out.includes('Las enfermeras realizaron la intervención ya que el riesgo era alto.'), out.join('\n'));
  assert.ok(out.every((o) => !/realizó/.test(o)), 'concordancia de número');
});

test('conectores al inicio y no a mitad de oración', () => {
  const out = all('Sin embargo, el dolor persistió.', { level: 2 });
  assert.ok(out.some((o) => /^(No obstante|Ahora bien), el dolor persistió\.$/.test(o)), out.join('\n'));
  assert.ok(all('El dolor persistió sin embargo.', { level: 5 }).every((o) => o.includes('sin embargo')));
});

test('sinónimos conservan género y número; «objetivo» adjetivo no cambia', () => {
  const out = all('Se utilizaron escalas adecuadas y el objetivo principal fue claro. Era un dato objetivo.', { level: 5 });
  assert.ok(out.some((o) => o.includes('emplearon escalas apropiadas')), out.join('\n'));
  assert.ok(out.every((o) => o.includes('un dato objetivo')));
  assert.ok(out.every((o) => !/escalas apropiados|escalas adecuados/.test(o)));
});

test('verbos intransitivos no se convierten en transitivos', () => {
  for (const o of all('La presión arterial disminuyó y el dolor aumentó tras la cirugía.', { level: 5 })) {
    assert.ok(o.includes('disminuyó') && o.includes('aumentó'), o);
  }
});

test('voz: pasiva perifrástica → refleja y con agente → activa (con «a» personal)', () => {
  const r1 = all('La medicación fue administrada cada 8 horas.', { level: 5 });
  assert.ok(r1.includes('La medicación se administró cada 8 horas.'), r1.join('\n'));
  const r2 = all('El protocolo fue aprobado por el comité de ética.', { level: 5 });
  assert.ok(r2.includes('El comité de ética aprobó el protocolo.'), r2.join('\n'));
  const r3 = all('El paciente fue evaluado por la enfermera.', { level: 5 });
  assert.ok(r3.includes('La enfermera evaluó al paciente.'), r3.join('\n'));
  const r4 = all('Los pacientes fueron entrevistados por las investigadoras.', { level: 5 });
  assert.ok(r4.includes('Las investigadoras entrevistaron a los pacientes.'), r4.join('\n'));
  const r5 = all('La tensión fue medida por el equipo.', { level: 5 });
  assert.ok(r5.includes('El equipo midió la tensión.'), r5.join('\n'));
});

test('voz: casos que NO deben transformarse', () => {
  // persona como sujeto de refleja: «el paciente se trasladó» sería ambiguo
  assert.ok(all('El paciente fue trasladado a la planta.', { level: 5 }).every((o) => o.includes('fue trasladado')));
  // participio adjetival
  assert.ok(all('La presión fue elevada durante el ingreso.', { level: 5 }).every((o) => o.includes('fue elevada')));
  // «por» que no es agente
  assert.ok(all('La medicación fue administrada por la vía periférica.', { level: 5 }).every((o) => !/La vía periférica administró/.test(o)));
});

test('reordenación de subordinadas conserva el contenido', () => {
  const out = all('Aunque el riesgo era bajo, las enfermeras reforzaron la vigilancia.', { level: 5 });
  assert.ok(out.includes('Las enfermeras reforzaron la vigilancia, aunque el riesgo era bajo.'), out.join('\n'));
  const out2 = all('Cuando el dolor supera 4 puntos, se administra analgesia.', { level: 5 });
  assert.ok(out2.every((o) => !/^\d/.test(o)), 'no empezar con cifra');
});

test('modo enfermería: lenguaje centrado en la persona y avisos de seguridad', () => {
  const r = paraphraseRules(ES_ENFERMERIA, { lang: 'es', mode: 'nursing', level: 3, seed: 2 });
  assert.ok(r.result.includes('Las personas mayores con deterioro cognitivo'), r.result);
  assert.ok(r.warnings.some((w) => /Cero final/.test(w)));
  assert.ok(r.warnings.some((w) => /«U»/.test(w)));
  for (const t of ['valoración de enfermería', 'escala de Braden', 'escala de Morse', 'diagnóstico de enfermería', '«Riesgo de caídas»', 'NANDA-I', 'higiene de manos', 'lesión por presión', 'escala EVA', '1.0 mg', '10 U']) {
    assert.ok(r.result.includes(t), `falta ${t}`);
  }
  const g = paraphraseRules(ES_ENFERMERIA, { lang: 'es', mode: 'general', level: 3, seed: 2 });
  assert.ok(g.result.includes('Los ancianos'), 'el modo general no aplica lenguaje centrado en la persona');
});

test('las citas textuales no se tocan', () => {
  const t = 'La autora afirma: «Es importante destacar que el cuidado debe llevarse a cabo con respeto» (Ruiz, 2020, p. 3).';
  for (const o of all(t, { level: 5 })) assert.ok(o.includes('«Es importante destacar que el cuidado debe llevarse a cabo con respeto» (Ruiz, 2020, p. 3)'));
});

test('informe de cambios y comparación', () => {
  const r = paraphraseRules(ES_CLINICO, { lang: 'es', level: 4, seed: 1 });
  assert.ok(r.changes.length > 3);
  assert.ok(r.changes.every((c) => typeof c.from === 'string' && typeof c.to === 'string' && c.reason));
  const rebuiltOrig = r.diff.filter((o) => o.type !== 'insert').map((o) => o.text).join('');
  const rebuiltRes = r.diff.filter((o) => o.type !== 'delete').map((o) => o.text).join('');
  assert.equal(rebuiltOrig, r.original);
  assert.equal(rebuiltRes, r.result);
});
