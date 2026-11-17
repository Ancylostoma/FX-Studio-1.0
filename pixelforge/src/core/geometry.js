// Rasterizadores enteros sin antialiasing. Cada función llama a plot(x, y)
// por cada píxel cubierto; el recorte lo hace quien recibe el píxel.

export function rasterRect(x, y, w, h, plot) {
  for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) plot(x + i, y + j);
}

// Bresenham.
export function rasterLine(x0, y0, x1, y1, plot) {
  let dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx + dy;
  for (;;) {
    plot(x0, y0);
    if (x0 === x1 && y0 === y1) break;
    const e2 = 2 * err;
    if (e2 >= dy) { err += dy; x0 += sx; }
    if (e2 <= dx) { err += dx; y0 += sy; }
  }
}

// Elipse rellena. Se usa radio+0.5 para obtener círculos de pixel art
// (sin "pezones" de un píxel en los extremos).
export function rasterEllipse(cx, cy, rx, ry, plot) {
  if (rx < 0 || ry < 0) return;
  const a = rx + 0.5, b = ry + 0.5;
  for (let dy = -ry; dy <= ry; dy++) {
    const t = 1 - (dy * dy) / (b * b);
    const half = Math.min(rx, Math.floor(a * Math.sqrt(Math.max(0, t))));
    for (let dx = -half; dx <= half; dx++) plot(cx + dx, cy + dy);
  }
}

// Polígono relleno (regla par-impar, muestreo en el centro del píxel) más
// sus aristas, para que las formas delgadas no desaparezcan.
export function rasterPolygon(pts, plot) {
  const n = pts.length >> 1;
  if (n < 2) { if (n === 1) plot(pts[0], pts[1]); return; }
  let minY = Infinity, maxY = -Infinity;
  for (let i = 0; i < n; i++) { minY = Math.min(minY, pts[2 * i + 1]); maxY = Math.max(maxY, pts[2 * i + 1]); }
  const xs = [];
  for (let y = minY; y <= maxY; y++) {
    xs.length = 0;
    const sy = y + 0.5;
    for (let i = 0; i < n; i++) {
      const j = (i + 1) % n;
      const x0 = pts[2 * i], y0 = pts[2 * i + 1], x1 = pts[2 * j], y1 = pts[2 * j + 1];
      if ((y0 <= sy && y1 > sy) || (y1 <= sy && y0 > sy)) xs.push(x0 + ((sy - y0) / (y1 - y0)) * (x1 - x0));
    }
    xs.sort((p, q) => p - q);
    for (let k = 0; k + 1 < xs.length; k += 2) {
      const a = Math.ceil(xs[k] - 0.5), b = Math.floor(xs[k + 1] - 0.5);
      for (let x = a; x <= b; x++) plot(x, y);
    }
  }
  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    rasterLine(pts[2 * i], pts[2 * i + 1], pts[2 * j], pts[2 * j + 1], plot);
  }
}

// Escalado por vecino más cercano de un búfer RGBA (factor entero).
export function scaleNearest(src, w, h, k) {
  k = Math.max(1, Math.floor(k));
  const out = new Uint8ClampedArray(w * k * h * k * 4);
  const W = w * k;
  for (let y = 0; y < h * k; y++) {
    const sy = (y / k) | 0;
    for (let x = 0; x < W; x++) {
      const s = (sy * w + ((x / k) | 0)) * 4, d = (y * W + x) * 4;
      out[d] = src[s]; out[d + 1] = src[s + 1]; out[d + 2] = src[s + 2]; out[d + 3] = src[s + 3];
    }
  }
  return { w: W, h: h * k, data: out };
}

// Caja delimitadora de los píxeles con alfa > 0.
export function opaqueBounds(data, w, h) {
  let x0 = w, y0 = h, x1 = -1, y1 = -1;
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    if (data[(y * w + x) * 4 + 3]) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y; }
  }
  return x1 < 0 ? null : { x: x0, y: y0, w: x1 - x0 + 1, h: y1 - y0 + 1 };
}
