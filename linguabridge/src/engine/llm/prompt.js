// Instrucciones para el modelo de lenguaje. El modelo recibe el texto con los
// elementos protegidos ya sustituidos por marcadores ⟦n⟧, así que nunca ve (ni
// puede alterar) citas, cifras ni términos clínicos.

const LEVEL_ES = {
  1: 'Cambia lo mínimo: elimina redundancias y corrige la fluidez. Mantén las palabras y el orden.',
  2: 'Sustituye algunas expresiones y conectores por equivalentes. Mantén la estructura de cada oración.',
  3: 'Usa expresiones y vocabulario equivalentes y precisos. Puedes ajustar el orden dentro de cada oración.',
  4: 'Reformula con libertad moderada: reorganiza oraciones y alterna voz activa y pasiva cuando mejore la claridad.',
  5: 'Reformula a fondo cada oración con otra estructura, manteniendo exactamente el mismo contenido.',
};
const LEVEL_EN = {
  1: 'Change as little as possible: remove redundancy and fix flow. Keep the words and the order.',
  2: 'Replace some expressions and connectors with equivalents. Keep the structure of each sentence.',
  3: 'Use equivalent, precise expressions and vocabulary. You may adjust word order within each sentence.',
  4: 'Rephrase with moderate freedom: reorganize sentences and alternate active and passive voice where it improves clarity.',
  5: 'Thoroughly rephrase every sentence with a different structure while keeping exactly the same content.',
};

export function buildPrompt({ text, lang, level, mode }) {
  if (lang === 'en') {
    const system = [
      'You are an academic editor. You rephrase English text to improve clarity, flow and naturalness without changing its meaning.',
      'Mandatory rules:',
      '1. Keep every marker ⟦n⟧ exactly as written, once each. Markers stand for citations, figures, units, clinical terms and references. Do not translate, split, remove, reorder inside, or invent markers.',
      '2. Do not add information, data, numbers, examples, authors, citations or references that are not in the original.',
      '3. Do not remove information. Keep every negation and the same degree of certainty (may, might, suggests, only, all...).',
      '4. Write in English. Do not translate.',
      '5. Reply only with JSON: {"text": "..."}',
      'Intensity: ' + LEVEL_EN[level],
      mode === 'nursing' ? 'Register: nursing and health sciences. Use person-first, bias-free language (APA 7). Keep NANDA-I/NIC/NOC labels, routes, doses and frequencies. Never replace technical terms with lay synonyms.' : '',
    ].filter(Boolean).join('\n');
    return { system, prompt: 'Text:\n' + text };
  }
  const system = [
    'Eres un editor académico. Reformulas textos en español para mejorar su claridad, fluidez y naturalidad sin cambiar el significado.',
    'Reglas obligatorias:',
    '1. Conserva cada marcador ⟦n⟧ exactamente como está, una vez cada uno. Los marcadores representan citas, cifras, unidades, términos clínicos y referencias. No los traduzcas, dividas, elimines ni inventes.',
    '2. No añadas información, datos, cifras, ejemplos, autores, citas ni referencias que no estén en el original.',
    '3. No elimines información. Conserva todas las negaciones y el mismo grado de certeza (puede, podría, sugiere, solo, todos...).',
    '4. Escribe en español. No traduzcas.',
    '5. Responde solo con JSON: {"text": "..."}',
    'Intensidad: ' + LEVEL_ES[level],
    mode === 'nursing' ? 'Registro: enfermería y ciencias de la salud. Usa lenguaje centrado en la persona y libre de sesgos (APA 7). Conserva las etiquetas NANDA-I/NIC/NOC, las vías, dosis y frecuencias. No sustituyas términos técnicos por sinónimos coloquiales.' : '',
  ].filter(Boolean).join('\n');
  return { system, prompt: 'Texto:\n' + text };
}

export function retryNote(lang, failed) {
  return lang === 'en'
    ? `\n\nYour previous answer was rejected (${failed.join('; ')}). Follow the rules strictly and keep every marker.`
    : `\n\nTu respuesta anterior se rechazó (${failed.join('; ')}). Cumple las reglas estrictamente y conserva todos los marcadores.`;
}

/** Extrae el texto de la respuesta del modelo (JSON, o texto plano como último recurso). */
export function parseModelText(raw) {
  let s = String(raw ?? '').replace(/<think>[\s\S]*?<\/think>/g, '').trim();
  s = s.replace(/^```(?:json)?\s*|\s*```$/g, '').trim();
  try { const j = JSON.parse(s); if (typeof j?.text === 'string') return j.text.trim(); } catch { /* no es JSON */ }
  const m = s.match(/\{[\s\S]*"text"\s*:\s*"([\s\S]*)"\s*\}/);
  if (m) { try { return JSON.parse(`"${m[1]}"`).trim(); } catch { /* sigue */ } }
  return s.replace(/^(?:texto reformulado|reformulación|rephrased text|text)\s*:\s*/i, '').trim();
}
