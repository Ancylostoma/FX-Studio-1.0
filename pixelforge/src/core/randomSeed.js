// Generación determinista: la misma semilla produce siempre la misma escena.

export function hashStr(s) {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619);
  return h >>> 0;
}

// Hash entero de una coordenada (para texturas reproducibles sin estado).
export function hash2(x, y, seed) {
  let h = (Math.imul(x, 374761393) + Math.imul(y, 668265263) + Math.imul(seed, 2147483647)) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return (h ^ (h >>> 16)) >>> 0;
}

export class RNG {
  constructor(seed) {
    this.seed = (typeof seed === 'string' ? hashStr(seed) : seed >>> 0) || 1;
    this.s = this.seed;
  }
  next() { // mulberry32
    let t = (this.s += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }
  range(a, b) { return a + this.next() * (b - a); }
  int(a, b) { return Math.floor(this.range(a, b + 1)); }
  chance(p) { return this.next() < p; }
  pick(arr) { return arr[Math.floor(this.next() * arr.length)]; }
  // Sub-generador independiente: cambiar una parte de la escena no altera las demás.
  fork(label) { return new RNG(hashStr(this.seed + ':' + label)); }
}

export function randomSeed() {
  return (Math.floor(Math.random() * 0xffffffff) >>> 0) % 1000000;
}
