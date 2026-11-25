// Cinco niveles de intensidad. Cada nivel añade técnicas y sube la probabilidad
// de aplicarlas; ninguno relaja las protecciones ni las comprobaciones.

export const LEVELS = Object.freeze({
  1: { name: 'Mínima', description: 'Solo redundancias, palabras repetidas y fluidez tipográfica.', redundancy: true, expressions: 0, synonyms: 0, voice: 0, reorder: 0, activeToPassive: false },
  2: { name: 'Ligera', description: 'Además, expresiones y conectores equivalentes.', redundancy: true, expressions: 0.6, synonyms: 0, voice: 0, reorder: 0, activeToPassive: false },
  3: { name: 'Moderada', description: 'Además, sinónimos contextuales de una palabra.', redundancy: true, expressions: 1, synonyms: 0.5, voice: 0, reorder: 0, activeToPassive: false },
  4: { name: 'Alta', description: 'Además, reorganiza oraciones y alterna la voz pasiva/activa.', redundancy: true, expressions: 1, synonyms: 0.7, voice: 0.7, reorder: 0.5, activeToPassive: false },
  5: { name: 'Máxima', description: 'Todas las técnicas con la máxima frecuencia, incluida activa → pasiva en métodos.', redundancy: true, expressions: 1, synonyms: 0.9, voice: 0.9, reorder: 0.8, activeToPassive: true },
});

export const clampLevel = (l) => Math.min(5, Math.max(1, Math.round(Number(l) || 3)));
