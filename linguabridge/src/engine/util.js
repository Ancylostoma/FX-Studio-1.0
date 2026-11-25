// Utilidades compartidas del motor. Sin dependencias: funciona en Node y en el navegador.

// ---------------- Marcadores de elementos protegidos ----------------
// Cada elemento protegido (cita, cifra, término clínico...) se sustituye por un
// marcador de un solo "token" hecho con caracteres Unicode de uso privado. No son
// letras ni dígitos, así que ninguna regla léxica puede tocarlos ni partirlos.
export const PH_OPEN = '\uE000';
export const PH_CLOSE = '\uE001';
export const PH_BASE = 0xe100;
export const PH_MAX = 0xf8ff - PH_BASE;
export const PH_SRC = '\uE000[\uE100-\uF8FF]\uE001';
export const phRegex = (flags = 'g') => new RegExp(PH_SRC, flags);
export const makePh = (i) => PH_OPEN + String.fromCharCode(PH_BASE + i) + PH_CLOSE;
export const phIndex = (ph) => ph.charCodeAt(1) - PH_BASE;
export const isPh = (s) => new RegExp('^' + PH_SRC + '$').test(s);
export const listPh = (s) => s.match(phRegex()) || [];

// Palabra (letras, apóstrofo, guion interno) o marcador.
export const WORD_SRC = `(?:[\\p{L}][\\p{L}'’\\-]*|${PH_SRC})`;

// ---------------- Azar reproducible ----------------
export function hashString(s) {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
  return h >>> 0;
}
export function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
export const pick = (rng, arr) => arr[Math.floor(rng() * arr.length) % arr.length];

// ---------------- Mayúsculas ----------------
export const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
export const isUpperLetter = (c) => c !== c.toLowerCase() && c === c.toUpperCase();
export function capitalizeFirst(s) {
  const m = s.match(/^([\s¿¡"“«(\[]*)(\p{L})/u);
  if (!m) return s;
  return m[1] + m[2].toUpperCase() + s.slice(m[0].length);
}
export function lowerFirst(s) {
  const m = s.match(/^([\s¿¡"“«(\[]*)(\p{L})/u);
  if (!m) return s;
  return m[1] + m[2].toLowerCase() + s.slice(m[0].length);
}
// Aplica al reemplazo la capitalización del texto original.
export function matchCase(src, repl) {
  const letters = src.replace(/[^\p{L}]/gu, '');
  if (letters.length > 1 && letters === letters.toUpperCase()) return repl.toUpperCase();
  const first = src.match(/\p{L}/u)?.[0];
  if (first && isUpperLetter(first)) return capitalizeFirst(repl);
  return repl;
}

export const countMatches = (s, re) => (s.match(re) || []).length;
export const normSpace = (s) => s.replace(/\s+/g, ' ').trim();
