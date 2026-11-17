// Dithering ordenado (matriz de Bayer 4×4): tramas regulares y controladas,
// nunca ruido aleatorio.

const B4 = [0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5];

export function bayer(x, y) { return B4[((y & 3) << 2) | (x & 3)]; }

// Anchura de la zona tramada entre dos bandas según el nivel (0 = bandas duras).
const ZONE = [0, 0.3, 0.6, 1];

// Elige el índice de banda para la posición continua t ∈ [0, n-1].
export function ditherBand(t, n, x, y, level) {
  if (n <= 1) return 0;
  t = Math.max(0, Math.min(n - 1, t));
  let base = Math.floor(t);
  if (base >= n - 1) return n - 1;
  let f = t - base;
  const z = ZONE[Math.max(0, Math.min(3, level | 0))];
  if (z === 0) return f >= 0.5 ? base + 1 : base;
  f = (f - (0.5 - z / 2)) / z;
  return f > (bayer(x, y) + 0.5) / 16 ? base + 1 : base;
}
