// Iluminación sobre datos indexados: luz ambiental (tinte multiplicativo de
// la paleta), sombreado direccional por rampas y contornos selectivos.

export const EMPTY = 255;

// Preajustes de luz ambiental [r,g,b,intensidad%].
export const AMBIENTS = {
  day: { name: 'Día', amb: null },
  dawn: { name: 'Amanecer', amb: [255, 190, 200, 35] },
  sunset: { name: 'Atardecer', amb: [255, 150, 110, 55] },
  night: { name: 'Noche', amb: [70, 90, 175, 78] },
  storm: { name: 'Tormenta', amb: [140, 150, 185, 55] },
};

export function litColor(c, amb) {
  if (!amb || !amb[3]) return c;
  const k = amb[3] / 100;
  return [0, 1, 2].map((i) => Math.round(c[i] * (1 - k + (k * amb[i]) / 255)));
}

export function fadeColor(c, to, pct) {
  const k = pct / 100;
  return [0, 1, 2].map((i) => Math.round(c[i] + (to[i] - c[i]) * k));
}

// Tabla índice → (rampa, posición).
export function rampTable(scene) {
  const n = scene.pal.length;
  const id = new Int16Array(n).fill(-1), pos = new Int16Array(n);
  (scene.rmp || []).forEach((r, ri) => r.forEach((c, p) => { if (id[c] < 0) { id[c] = ri; pos[c] = p; } }));
  return { id, pos, ramps: scene.rmp || [] };
}

export function shiftRamp(c, delta, rt) {
  const r = rt.id[c];
  if (r < 0) return c;
  const ramp = rt.ramps[r];
  return ramp[Math.max(0, Math.min(ramp.length - 1, rt.pos[c] + delta))];
}

// Sombreado de borde: los píxeles del contorno que miran a la luz suben un
// tono en su rampa y los opuestos bajan uno. ld = -1 luz desde la izquierda.
export function shadeLayer(buf, w, h, rt, ld) {
  const src = buf.slice();
  const empty = (x, y) => x < 0 || y < 0 || x >= w || y >= h || src[y * w + x] === EMPTY;
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const c = src[y * w + x];
    if (c === EMPTY || rt.id[c] < 0) continue;
    if (empty(x, y - 1) || empty(x + ld, y)) buf[y * w + x] = shiftRamp(c, 1, rt);
    else if (empty(x, y + 1) || empty(x - ld, y)) buf[y * w + x] = shiftRamp(c, -1, rt);
  }
}

// Contorno exterior. ol >= 0: color fijo. ol = -2: contorno selectivo (toma el
// tono más oscuro de la rampa del píxel vecino y se omite en el lado iluminado).
export function outlineLayer(buf, w, h, ol, rt, ld) {
  const src = buf.slice();
  const at = (x, y) => (x < 0 || y < 0 || x >= w || y >= h ? EMPTY : src[y * w + x]);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    if (src[y * w + x] !== EMPTY) continue;
    if (ol >= 0) {
      if (at(x - 1, y) !== EMPTY || at(x + 1, y) !== EMPTY || at(x, y - 1) !== EMPTY || at(x, y + 1) !== EMPTY) buf[y * w + x] = ol;
    } else {
      // Lado en sombra: el vecino opaco está hacia la luz (arriba o en x+ld).
      const n = at(x, y - 1) !== EMPTY ? at(x, y - 1) : at(x + ld, y) !== EMPTY ? at(x + ld, y) : at(x - ld, y) !== EMPTY ? at(x - ld, y) : EMPTY;
      if (n !== EMPTY) buf[y * w + x] = shiftRamp(n, -9, rt);
    }
  }
}
