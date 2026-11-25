import test from 'node:test';
import assert from 'node:assert/strict';
import { protect, restore } from '../src/engine/protect.js';
import { ES_CLINICO, EN_NURSING } from './fixtures.mjs';

const kinds = (text, opts) => protect(text, opts).spans.map((s) => `${s.kind}:${s.text}`);

test('restaurar devuelve exactamente el original', () => {
  for (const t of [ES_CLINICO, EN_NURSING]) {
    const p = protect(t, { lang: 'es' });
    assert.equal(restore(p.text, p.spans), t);
  }
});

test('citas APA 7 parentéticas y narrativas', () => {
  const k = kinds('Como afirman García y López (2020), el cuidado mejora (Pérez et al., 2019; Ruiz, 2018, p. 45). Smith (n.d.) y la OMS (2021) coinciden.', { lang: 'es' });
  assert.ok(k.includes('citation:García y López (2020)'));
  assert.ok(k.includes('citation:(Pérez et al., 2019; Ruiz, 2018, p. 45)'));
  assert.ok(k.includes('citation:Smith (n.d.)'));
  assert.ok(k.some((x) => x.includes('(2021)')));
  assert.ok(!k.some((x) => x.startsWith('citation:Como')), 'no debe tragarse «Como»');
});

test('lista de referencias completa y entradas sueltas', () => {
  const p = protect(ES_CLINICO, { lang: 'es' });
  const refs = p.spans.find((s) => s.kind === 'references');
  assert.ok(refs && refs.text.startsWith('Referencias') && refs.text.includes('Editorial Salud.'));
  const k = kinds('Texto previo.\nSmith, J. A., & Lee, K. (2021). Title of work. Journal, 3(2), 1–9.\nOtra línea.', { lang: 'en' });
  assert.ok(k.some((x) => x.startsWith('reference:Smith, J. A.')));
  // Una oración normal con cita narrativa no es una referencia
  const k2 = kinds('Los resultados coinciden con García (2020). Además, se observó mejoría.', { lang: 'es' });
  assert.ok(!k2.some((x) => x.startsWith('reference')));
});

test('cifras, unidades, rangos y estadísticos', () => {
  const k = kinds('Se administraron 0,5 mg/kg cada 8 h; la PA bajó de 150 a 130 mmHg (p < .001; IC 95 %: 1,2-3,4; OR = 2.3). Peso: 72,5 kg, 38,5 °C y 95 % de SpO2.', { lang: 'es' });
  for (const want of ['number:0,5 mg/kg', 'number:8 h', 'number:150 a 130 mmHg', 'stat:p < .001', 'stat:OR = 2.3', 'number:72,5 kg', 'number:38,5 °C', 'number:95 %', 'clinical:SpO2']) {
    assert.ok(k.includes(want), `falta ${want}: ${k.join(' | ')}`);
  }
  assert.ok(k.some((x) => x.startsWith('stat:IC 95 %')));
});

test('terminología clínica, fármacos por sufijo, siglas y citas textuales', () => {
  const k = kinds('El paciente con EPOC recibió enoxaparina y omeprazol; la presión arterial y la saturación de oxígeno se registraron. «El cuidado es relación».', { lang: 'es' });
  for (const want of ['acronym:EPOC', 'clinical:enoxaparina', 'clinical:omeprazol', 'clinical:presión arterial', 'clinical:saturación de oxígeno', 'quote:«El cuidado es relación»']) {
    assert.ok(k.includes(want), `falta ${want}`);
  }
});

test('modo enfermería protege el proceso enfermero y términos del usuario', () => {
  const g = kinds('La valoración y la planificación del plan de cuidados.', { lang: 'es', mode: 'general' });
  const n = kinds('La valoración y la planificación del plan de cuidados.', { lang: 'es', mode: 'nursing' });
  assert.ok(!g.includes('clinical:valoración'));
  assert.ok(n.includes('clinical:valoración') && n.includes('clinical:plan de cuidados'));
  const c = kinds('Aplicamos el método Kangaroo Plus en la unidad.', { lang: 'es', customTerms: ['método Kangaroo Plus'] });
  assert.ok(c.includes('custom:método Kangaroo Plus'));
});
