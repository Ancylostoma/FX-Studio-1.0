// Sprites ampliados: vehículos, arquitectura especial, criaturas, mobiliario
// de interiores y efectos. Mismo contrato que spriteBase.js:
// build(p, k) dibuja en coordenadas unitarias mirando a la derecha.
import { reg, lightOrDark, bricks, poseOf, flapper, WARM, FIRE1, FIRE2 } from './spriteBase.js';

// ---------------- Vehículos ----------------
reg('train', 'tren', 'vehicle', 4.2, 0.17, (p, k) => {
  const C = k.C, fut = k.a.style === 'futurist' || k.a.future;
  const body = C.tint(k.a.color, fut ? 'white' : 'red'), loco = C.tint(k.a.color2, fut ? 'white' : 'black'), m = C.mat('metal'), d = C.dark;
  const g = C.mat('glass'), stripe = C.tint(null, fut ? 'cyan' : 'gold');
  const cars = 3, cw = 0.27, gap = 0.015;
  for (let i = 0; i < cars; i++) {           // vagones (detrás de la locomotora, a la izquierda)
    const u = i * (cw + gap);
    p.rect(u, 0.18, cw, 0.62, body[2]); p.rect(u, 0.18, cw, 0.08, body[3]); p.rect(u, 0.72, cw, 0.08, body[1]);
    for (let j = 0; j < 4; j++) (k.night ? k.lit : p).rect(u + 0.025 + j * 0.062, 0.32, 0.045, 0.18, k.night ? C.rgb(WARM) : g[3]);
    p.rect(u, 0.58, cw, 0.04, stripe[2]);
    if (!fut) for (const w of [0.05, 0.2]) { p.ell(u + w, 0.86, 0.025, 0.13, d); p.ell(u + w, 0.86, 0.012, 0.06, m[2]); }
  }
  const lu = cars * (cw + gap);
  if (fut) {                                 // morro aerodinámico
    p.poly([[lu, 0.18], [lu + 0.12, 0.18], [1, 0.62], [1, 0.8], [lu, 0.8]], loco[2]);
    p.poly([[lu + 0.1, 0.24], [lu + 0.14, 0.24], [0.94, 0.56], [lu + 0.12, 0.5]], g[1]);
    p.rect(lu, 0.58, 1 - lu, 0.04, stripe[2]); p.rect(0, 0.8, 1, 0.08, m[1]);
    k.lit.rect(0.985, 0.66, 0.015, 0.06, C.rgb([255, 250, 210]));
  } else {
    p.rect(lu, 0.34, 0.2, 0.46, loco[2]); p.rect(lu + 0.12, 0.08, 0.12, 0.72, loco[1]); p.rect(lu + 0.13, 0.16, 0.08, 0.16, g[2]);
    p.rect(lu + 0.03, 0.12, 0.04, 0.22, loco[1]); p.rect(lu + 0.02, 0.1, 0.06, 0.05, loco[3]);
    p.poly([[lu + 0.24, 0.66], [1, 0.8], [lu + 0.24, 0.8]], m[2]);
    for (const w of [0.04, 0.12, 0.2]) { p.ell(lu + w, 0.84, 0.035, 0.15, d); p.ell(lu + w, 0.84, 0.015, 0.07, C.col('red')[2]); }
    const s = C.mat('cloud'); for (let i = 0; i < 3; i++) p.ell(lu + 0.05 - i * 0.05, 0.02 - i * 0.08, 0.035 + i * 0.012, 0.09 + i * 0.03, s[3 - (i & 1)]);
  }
});

reg('spaceship', 'nave espacial', 'vehicle', 1.9, 0.2, (p, k) => {
  const C = k.C, b = C.tint(k.a.color, 'silver'), a = C.tint(k.a.color2, 'red'), g = C.mat('glass'), L = k.lit;
  p.poly([[0.32, 0.46], [0.12, 0.04], [0.24, 0.04], [0.52, 0.42]], a[1]);            // ala superior
  p.poly([[0.06, 0.38], [0.7, 0.34], [0.98, 0.52], [0.7, 0.68], [0.06, 0.64]], b[2]); // fuselaje
  p.poly([[0.06, 0.38], [0.7, 0.34], [0.98, 0.52], [0.06, 0.48]], b[3]);
  p.poly([[0.06, 0.58], [0.7, 0.68], [0.06, 0.64]], b[1]);
  p.poly([[0.56, 0.38], [0.78, 0.4], [0.86, 0.5], [0.6, 0.48]], g[2]); p.line(0.6, 0.41, 0.76, 0.42, g[4]);
  p.poly([[0.3, 0.6], [0.1, 0.98], [0.24, 0.98], [0.52, 0.62]], a[2]);               // ala inferior
  p.rect(0.2, 0.47, 0.36, 0.04, a[2]);
  p.rect(0.0, 0.42, 0.08, 0.18, C.mat('darkstone')[2]);
  const f = k.pose && k.pose.f % 2 ? 0.06 : 0;
  L.poly([[0.0, 0.44], [-0.16 - f, 0.51], [0.0, 0.58]], C.rgb([120, 220, 255])); L.poly([[0.0, 0.47], [-0.08 - f / 2, 0.51], [0.0, 0.55]], C.rgb([235, 250, 255]));
}, { air: 1 });

reg('submarine', 'submarino', 'vehicle', 2.3, 0.16, (p, k) => {
  const C = k.C, b = C.tint(k.a.color, 'yellow'), m = C.mat('metal'), g = C.mat('glass');
  p.ell(0.5, 0.62, 0.46, 0.26, b[2]); p.ell(0.46, 0.52, 0.36, 0.1, b[3], 1); p.ell(0.5, 0.76, 0.4, 0.08, b[1], 1);
  p.rect(0.4, 0.24, 0.2, 0.18, b[2]); p.rect(0.4, 0.24, 0.05, 0.18, b[3]); p.rect(0.48, 0.06, 0.03, 0.2, m[1]); p.rect(0.48, 0.06, 0.1, 0.03, m[1]);
  for (const u of [0.32, 0.5, 0.68]) { p.ell(u, 0.6, 0.05, 0.08, m[1]); (k.night ? k.lit : p).ell(u, 0.6, 0.03, 0.05, k.night ? C.rgb(WARM) : g[3]); }
  p.poly([[0.06, 0.62], [0.0, 0.42], [0.08, 0.5]], m[2]); p.poly([[0.06, 0.62], [0.0, 0.82], [0.08, 0.74]], m[1]);
}, { float: 1 });

// ---------------- Arquitectura especial ----------------
reg('bridge', 'puente', 'structure', 4.5, 0.2, (p, k) => {
  const C = k.C, modern = k.a.future || k.a.style === 'futurist';
  if (modern) {                               // puente de celosía metálica
    const m = C.tint(k.a.color, 'red'), d = C.mat('darkstone');
    p.rect(0, 0.3, 1, 0.08, m[2]); p.rect(0, 0.3, 1, 0.02, m[3]);
    for (let i = 0; i <= 8; i++) { const u = i / 8; p.line(u, 0.0, u, 0.3, m[1]); if (i < 8) { p.line(u, 0.0, u + 1 / 8, 0.3, m[2]); p.line(u, 0.3, u + 1 / 8, 0.0, m[2]); } }
    p.rect(0, 0.0, 1, 0.03, m[2]);
    for (const u of [0.24, 0.74]) { p.rect(u, 0.38, 0.05, 0.62, d[2]); p.rect(u, 0.38, 0.015, 0.62, d[3]); }
    return;
  }
  const s = C.tint(k.a.color, 'stone');
  p.rect(0, 0.18, 1, 0.82, s[2]); p.rect(0, 0.18, 1, 0.04, s[3]); p.rect(0, 0.22, 1, 0.03, s[0]);
  for (let i = 0; i < 12; i++) p.rect(i / 12, 0.1, 1 / 24, 0.08, s[2]);       // pretil
  for (let i = 0; i < 3; i++) { const cx = (i + 0.5) / 3; p.ell(cx, 1.0, 0.12, 0.56, -1); p.ell(cx, 0.99, 0.12, 0.56, s[0], 2); }
  for (let i = 0; i < 3; i++) { const cx = (i + 0.5) / 3; p.ell(cx, 1.0, 0.115, 0.55, -1); }
  bricks(p, k, s);
}, { span: 1, top: 0.2 });

reg('wall', 'muralla', 'structure', 3, 0.22, (p, k) => {
  const C = k.C, s = C.tint(k.a.color, 'stone');
  p.rect(0, 0.2, 1, 0.8, s[2]); p.rect(0, 0.2, 1, 0.05, s[3]);
  const n = 10; for (let i = 0; i < n; i++) p.rect((i * 2) / (2 * n - 1), 0.06, 1 / (2 * n - 1), 0.14, s[2]);
  for (const u of [0.0, 0.88]) { p.rect(u, -0.02, 0.12, 1.02, s[1]); p.rect(u, -0.02, 0.04, 1.02, s[3]); for (let i = 0; i < 3; i++) p.rect(u + i * 0.045, -0.1, 0.03, 0.08, s[1]); lightOrDark(k, u + 0.045, 0.3, 0.03, 0.08, p); }
  bricks(p, k, s);
});

reg('fountain', 'fuente', 'structure', 1.3, 0.18, (p, k) => {
  const C = k.C, s = C.mat('stone'), w = C.mat('water');
  p.ell(0.5, 0.8, 0.48, 0.16, s[2]); p.ell(0.5, 0.76, 0.42, 0.11, w[3]); p.rect(0.44, 0.36, 0.12, 0.42, s[2]); p.ell(0.5, 0.36, 0.22, 0.06, s[3]);
  const f = k.pose ? k.pose.f % 2 : 0;
  for (const d of [-1, 1]) p.line(0.5, 0.22, 0.5 + d * (0.24 + f * 0.02), 0.7, w[4]);
  p.rect(0.48, 0.06, 0.04, 0.3, w[4]);
});

reg('well', 'pozo', 'structure', 0.9, 0.18, (p, k) => {
  const C = k.C, s = C.mat('stone'), w = C.mat('wood'), r = C.mat('roof');
  p.rect(0.12, 0.6, 0.76, 0.4, s[2]); p.rect(0.12, 0.6, 0.76, 0.06, s[3]); bricks(p, k, s);
  p.rect(0.16, 0.2, 0.06, 0.42, w[1]); p.rect(0.78, 0.2, 0.06, 0.42, w[1]);
  p.poly([[0.04, 0.24], [0.5, 0.0], [0.96, 0.24]], r[2]); p.line(0.5, 0.26, 0.5, 0.56, C.dark); p.rect(0.44, 0.54, 0.12, 0.08, w[2]);
});

// ---------------- Criaturas ----------------
reg('penguin', 'pingüino', 'animal', 0.75, 0.12, (p, k) => {
  const C = k.C, b = C.tint(k.a.color, 'black'), w = C.mat('white'), o = C.col('orange'), ps = poseOf(k), t = ps.on ? [0, 0.03, 0, -0.03][ps.f] : 0;
  p.ell(0.5, 0.56, 0.36, 0.42, b[2]); p.ell(0.56, 0.62, 0.24, 0.32, w[3]); p.ell(0.52, 0.2, 0.26, 0.2, b[2]);
  p.px(0.62, 0.16, w[4]); p.px(0.63, 0.17, C.dark); p.poly([[0.74, 0.2], [0.92, 0.25], [0.74, 0.29]], o[2]);
  p.poly([[0.18 + t, 0.4], [0.06 + t, 0.74], [0.2, 0.66]], b[1]);
  p.rect(0.34, 0.94, 0.14, 0.06, o[2]); p.rect(0.56, 0.94, 0.14, 0.06, o[1]);
});

reg('spider', 'araña gigante', 'creature', 1.6, 0.16, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'black'), ps = poseOf(k);
  for (let i = 0; i < 4; i++) {
    const u = 0.36 + i * 0.09, lift = ps.on && (i + ps.f) % 2 ? 0.06 : 0;
    p.line(u, 0.5, u - 0.18, 0.22 - lift, c[1]); p.line(u - 0.18, 0.22 - lift, u - 0.3, 1.0, c[1]);
    p.line(u + 0.02, 0.52, u + 0.2, 0.26 - lift, c[2]); p.line(u + 0.2, 0.26 - lift, u + 0.3, 1.0, c[2]);
  }
  p.ell(0.36, 0.52, 0.22, 0.2, c[2]); p.ell(0.32, 0.46, 0.12, 0.08, c[3], 1); p.ell(0.66, 0.56, 0.12, 0.12, c[2]);
  for (const [u, v] of [[0.72, 0.52], [0.76, 0.56], [0.7, 0.58]]) k.lit.px(u, v, C.rgb([255, 60, 60]));
});

reg('griffin', 'grifo', 'creature', 1.45, 0.3, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'gold'), w = C.tint(k.a.color2, 'brown'), head = C.mat('white'), W = flapper(k, 0.42);
  p.poly(W([[0.5, 0.42], [0.6, 0.02], [0.8, 0.0], [0.7, 0.3]]), w[1]);
  p.ell(0.42, 0.56, 0.24, 0.15, c[2]); p.ell(0.4, 0.5, 0.16, 0.06, c[3], 1);
  for (const u of [0.24, 0.32, 0.52, 0.6]) p.rect(u, 0.64, 0.05, 0.34, u < 0.5 ? c[1] : C.col('yellow')[1]);
  p.line(0.18, 0.54, 0.04, 0.4, c[1]); p.ell(0.03, 0.38, 0.03, 0.05, w[1]);
  p.ell(0.72, 0.36, 0.1, 0.14, head[3]); p.poly([[0.8, 0.32], [0.94, 0.4], [0.8, 0.44]], C.col('yellow')[2]); p.px(0.76, 0.32, C.dark);
  p.poly(W([[0.46, 0.44], [0.34, 0.02], [0.16, 0.04], [0.24, 0.2], [0.18, 0.3], [0.36, 0.42]]), w[2]);
}, { air: 1 });

// ---------------- Mobiliario de interiores ----------------
const BOOKS = ['red', 'blue', 'green', 'gold', 'purple', 'brown', 'orange'];
reg('bookshelf', 'estantería', 'flora', 0.62, 0.48, (p, k) => {
  const C = k.C, w = C.tint(k.a.color, 'wood'), rows = 5;
  p.rect(0, 0, 1, 1, w[1]); p.rect(0, 0, 1, 0.04, w[3]); p.rect(0, 0, 0.07, 1, w[2]); p.rect(0.93, 0, 0.07, 1, w[0]);
  for (let r = 0; r < rows; r++) {
    const v = 0.06 + r * 0.186, h = 0.15;
    p.rect(0.07, v, 0.86, h, w[0]);
    let u = 0.08;
    for (let i = 0; u < 0.9 && i < 12; i++) {
      const bw = 0.06 + ((i * 7 + r * 3) % 3) * 0.02, bh = h * (0.7 + ((i + r) % 3) * 0.1), c = C.col(BOOKS[(i * 3 + r * 5) % BOOKS.length]);
      if ((i + r * 2) % 7 === 6) { p.poly([[u, v + h], [u + bw, v + h], [u + bw * 2, v + h - bh * 0.9], [u + bw * 1.4, v + h - bh]], c[2]); u += bw * 2.2; continue; }
      p.rect(u, v + h - bh, bw, bh, c[2]); p.rect(u, v + h - bh, bw * 0.3, bh, c[3]); u += bw + 0.01;
    }
    p.rect(0.07, v + h, 0.86, 0.035, w[2]);
  }
});
reg('table', 'mesa', 'flora', 1.5, 0.13, (p, k) => {
  const C = k.C, w = C.tint(k.a.color, 'wood');
  p.rect(0, 0.2, 1, 0.14, w[2]); p.rect(0, 0.2, 1, 0.04, w[3]); p.rect(0.08, 0.34, 0.08, 0.66, w[1]); p.rect(0.84, 0.34, 0.08, 0.66, w[0]);
  const b = C.col('red'); p.rect(0.2, 0.08, 0.28, 0.12, b[2]); p.rect(0.2, 0.08, 0.28, 0.03, C.mat('cream')[4]);
  p.rect(0.66, 0.0, 0.05, 0.2, C.mat('cream')[3]); k.lit.ell(0.685, -0.06, 0.03, 0.06, C.rgb(FIRE2));
  if (k.night || k.interior) k.glow.push(['E', p.X(0.685), p.Y(0.0), Math.round(p.w * 0.5), Math.round(p.h * 0.8), C.rgb(WARM)]);
});
reg('chair', 'silla', 'flora', 0.6, 0.12, (p, k) => {
  const w = k.C.tint(k.a.color, 'wood');
  p.rect(0.1, 0.0, 0.14, 1, w[1]); p.rect(0.1, 0.5, 0.8, 0.12, w[2]); p.rect(0.76, 0.5, 0.14, 0.5, w[0]); p.rect(0.1, 0.1, 0.14, 0.4, w[2]);
});
reg('candle', 'vela', 'flora', 0.4, 0.05, (p, k) => {
  const C = k.C; p.rect(0.3, 0.4, 0.4, 0.6, C.mat('cream')[3]); p.rect(0.3, 0.4, 0.12, 0.6, C.mat('cream')[4]);
  k.lit.ell(0.5, 0.2, 0.18, 0.22, C.rgb(FIRE1)); k.lit.ell(0.5, 0.24, 0.08, 0.12, C.rgb(FIRE2));
  k.glow.push(['E', p.X(0.5), p.Y(0.2), Math.round(p.h * 1.4), Math.round(p.h * 1.4), C.rgb(WARM)]);
});
reg('fireplace', 'chimenea', 'flora', 1.2, 0.3, (p, k) => {
  const C = k.C, s = C.tint(k.a.color, 'stone'), L = k.lit, m = (pts) => (k.pose && k.pose.f % 2 ? pts.map(([u, v]) => [1 - u, v]) : pts);
  p.rect(0, 0.1, 1, 0.9, s[2]); p.rect(0, 0.1, 1, 0.08, s[3]); p.rect(-0.04, 0.04, 1.08, 0.08, s[1]); bricks(p, k, s);
  p.ell(0.5, 0.56, 0.3, 0.16, C.dark); p.rect(0.2, 0.56, 0.6, 0.44, C.dark);
  L.poly(m([[0.3, 0.98], [0.32, 0.72], [0.42, 0.78], [0.5, 0.52], [0.58, 0.74], [0.68, 0.66], [0.7, 0.98]]), C.rgb(FIRE1));
  L.poly(m([[0.38, 0.98], [0.44, 0.78], [0.5, 0.66], [0.58, 0.82], [0.62, 0.98]]), C.rgb(FIRE2));
  k.glow.push(['E', p.X(0.5), p.Y(0.8), Math.round(p.w * 1.2), Math.round(p.h * 0.7), C.rgb(FIRE1)]);
});
reg('barrel', 'barril', 'flora', 0.75, 0.1, (p, k) => {
  const w = k.C.tint(k.a.color, 'wood'), m = k.C.mat('metal');
  p.ell(0.5, 0.5, 0.48, 0.5, w[2]); p.rect(0.02, 0.0, 0.96, 1, w[2], 1); p.rect(0.02, 0, 0.2, 1, w[3], 1); p.rect(0.76, 0, 0.22, 1, w[1], 1);
  for (const v of [0.16, 0.8]) p.rect(0.0, v, 1, 0.07, m[1], 1);
});
reg('throne', 'trono', 'flora', 0.75, 0.3, (p, k) => {
  const C = k.C, g = C.tint(k.a.color2, 'gold'), r = C.tint(k.a.color, 'red');
  p.rect(0.12, 0.0, 0.76, 0.6, g[2]); p.poly([[0.12, 0.0], [0.5, -0.12], [0.88, 0.0]], g[3]); p.rect(0.2, 0.08, 0.6, 0.5, r[2]);
  p.rect(0.04, 0.56, 0.92, 0.14, g[2]); p.rect(0.04, 0.7, 0.14, 0.3, g[1]); p.rect(0.82, 0.7, 0.14, 0.3, g[1]); p.rect(0.18, 0.62, 0.64, 0.08, r[3]);
  p.px(0.5, -0.04, C.col('red')[3]);
});
reg('banner', 'estandarte', 'flora', 0.4, 0.3, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), g = C.mat('gold');
  p.rect(0, 0, 1, 0.06, C.mat('wood')[1]); p.poly([[0.06, 0.06], [0.94, 0.06], [0.94, 1], [0.5, 0.84], [0.06, 1]], c[2]); p.rect(0.06, 0.06, 0.22, 0.86, c[3], 1);
  p.ell(0.5, 0.4, 0.2, 0.1, g[3]); p.poly([[0.5, 0.26], [0.62, 0.42], [0.5, 0.56], [0.38, 0.42]], g[2]);
});
reg('torch', 'antorcha', 'flora', 0.35, 0.12, (p, k) => {
  const C = k.C, m = (pts) => (k.pose && k.pose.f % 2 ? pts.map(([u, v]) => [1 - u, v]) : pts);
  p.rect(0.4, 0.36, 0.2, 0.64, C.mat('wood')[1]); p.rect(0.3, 0.34, 0.4, 0.08, C.mat('metal')[2]);
  k.lit.poly(m([[0.2, 0.36], [0.3, 0.12], [0.46, 0.2], [0.52, -0.06], [0.68, 0.16], [0.8, 0.36]]), C.rgb(FIRE1)); k.lit.ell(0.5, 0.26, 0.12, 0.1, C.rgb(FIRE2));
  k.glow.push(['E', p.X(0.5), p.Y(0.2), Math.round(p.h * 1.5), Math.round(p.h * 1.5), C.rgb(FIRE1)]);
});
reg('globe', 'globo terráqueo', 'flora', 0.7, 0.12, (p, k) => {
  const C = k.C, w = C.mat('wood'), g = C.mat('gold'), sea = C.col('blue'), land = C.col('green');
  p.rect(0.42, 0.7, 0.16, 0.22, w[1]); p.rect(0.2, 0.92, 0.6, 0.08, w[2]);
  p.ell(0.5, 0.38, 0.38, 0.34, sea[2]); p.ell(0.38, 0.3, 0.14, 0.12, land[2]); p.ell(0.6, 0.48, 0.12, 0.1, land[2]); p.ell(0.38, 0.26, 0.14, 0.06, sea[4], 1);
  p.line(0.1, 0.1, 0.9, 0.7, g[3]);
});
reg('rug', 'alfombra', 'flora', 4, 0.04, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), g = C.mat('gold');
  p.poly([[0.08, 0], [0.92, 0], [1, 1], [0, 1]], c[2]); p.poly([[0.16, 0.25], [0.84, 0.25], [0.9, 0.75], [0.1, 0.75]], c[1]); p.poly([[0.4, 0.4], [0.6, 0.4], [0.62, 0.6], [0.38, 0.6]], g[2]);
});
reg('cauldron', 'caldero', 'flora', 1, 0.1, (p, k) => {
  const C = k.C, m = C.mat('black'), g = C.tint(k.a.color, 'green');
  p.ell(0.5, 0.56, 0.44, 0.38, m[2]); p.ell(0.4, 0.46, 0.18, 0.14, m[3], 1); p.ell(0.5, 0.24, 0.42, 0.08, m[1]); k.lit.ell(0.5, 0.24, 0.36, 0.05, C.rgb([140, 255, 120]));
  for (const [u, v] of [[0.4, 0.06], [0.58, 0.0]]) k.lit.ell(u, v, 0.05, 0.05, C.rgb([170, 255, 150]));
  void g;
});

// ---------------- Efectos ----------------
reg('explosion', 'explosión', 'effect', 1.1, 0.22, (p, k) => {
  const C = k.C, f = k.pose ? k.pose.f % 4 : 1, s = [0.7, 1, 0.85, 0.6][f], L = k.lit;
  const star = (r, c) => { const pts = []; for (let i = 0; i < 16; i++) { const an = (i / 16) * Math.PI * 2, rr = r * (i % 2 ? 0.55 : 1); pts.push([0.5 + Math.cos(an) * rr * 0.5, 0.5 + Math.sin(an) * rr * 0.5]); } L.poly(pts, c); };
  star(s, C.rgb([220, 60, 30])); star(s * 0.75, C.rgb(FIRE1)); star(s * 0.45, C.rgb(FIRE2)); L.ell(0.5, 0.5, 0.06 * s, 0.06 * s, C.rgb([255, 255, 230]));
  k.glow.push(['E', p.X(0.5), p.Y(0.5), Math.round(p.w * 0.6), Math.round(p.h * 0.6), C.rgb(FIRE1)]);
}, { air: 1, effect: 1 });
reg('fireball', 'bola de fuego', 'effect', 1.8, 0.08, (p, k) => {
  const C = k.C, L = k.lit, j = k.pose && k.pose.f % 2 ? 0.05 : 0;
  L.poly([[0.0, 0.5], [0.4, 0.15 + j], [0.62, 0.2], [0.62, 0.8], [0.4, 0.85 - j]], C.rgb([220, 60, 30])); L.ell(0.72, 0.5, 0.26, 0.4, C.rgb(FIRE1)); L.ell(0.76, 0.5, 0.14, 0.24, C.rgb(FIRE2));
  k.glow.push(['E', p.X(0.7), p.Y(0.5), Math.round(p.w * 0.4), Math.round(p.h * 0.8), C.rgb(FIRE1)]);
}, { air: 1, effect: 1 });
reg('magic', 'magia', 'effect', 1, 0.14, (p, k) => {
  const C = k.C, c = k.a.color ? C.col(k.a.color) : null, col = c ? c[4] : C.rgb([170, 140, 255]), core = C.rgb([235, 225, 255]), L = k.lit, f = k.pose ? k.pose.f % 4 : 0;
  L.ell(0.5, 0.5, 0.22, 0.22, col); L.ell(0.5, 0.5, 0.11, 0.11, core);
  for (let i = 0; i < 6; i++) { const an = ((i + f * 0.5) / 6) * Math.PI * 2; L.px(0.5 + Math.cos(an) * 0.42, 0.5 + Math.sin(an) * 0.42, i % 2 ? core : col); L.line(0.5 + Math.cos(an) * 0.24, 0.5 + Math.sin(an) * 0.24, 0.5 + Math.cos(an) * 0.34, 0.5 + Math.sin(an) * 0.34, col); }
  k.glow.push(['E', p.X(0.5), p.Y(0.5), Math.round(p.w * 0.55), Math.round(p.h * 0.55), col]);
}, { air: 1, effect: 1 });
reg('smoke', 'humo', 'effect', 0.7, 0.3, (p, k) => {
  const C = k.C, s = C.tint(k.a.color, 'ash'), f = k.pose ? k.pose.f % 4 : 0;
  for (let i = 0; i < 5; i++) { const v = 0.9 - i * 0.2 - f * 0.05, u = 0.5 + Math.sin(i * 1.7 + f) * 0.14; p.ell(u, v, 0.2 + i * 0.05, 0.12 + i * 0.03, s[i % 2 ? 2 : 3]); }
}, { effect: 1, alpha: 0.75 });
reg('lightningbolt', 'rayo', 'effect', 0.5, 0.5, (p, k) => {
  const C = k.C, L = k.lit, c = C.rgb([235, 245, 255]);
  const pts = [[0.6, 0], [0.3, 0.42], [0.55, 0.42], [0.25, 1], [0.8, 0.34], [0.52, 0.34], [0.78, 0]];
  L.poly(pts, C.rgb([150, 190, 255])); L.line(0.6, 0.04, 0.38, 0.4, c); L.line(0.5, 0.44, 0.3, 0.92, c);
  k.glow.push(['E', p.X(0.5), p.Y(0.5), Math.round(p.w * 0.7), Math.round(p.h * 0.4), C.rgb([150, 190, 255])]);
}, { air: 1, effect: 1 });
