// Biblioteca de sprites procedurales. Cada constructor dibuja con una Pluma
// en coordenadas unitarias (0..1) mirando a la derecha, por lo que escala a
// cualquier resolución. Para añadir un objeto nuevo basta registrar aquí su
// constructor y darle nombre en vocabulary.js.
//
// build(p, k):  p = Pluma de la capa principal
//               k = { C: kit de color, a: atributos del prompt, lit: Pluma de
//                     la capa de luces (emisiva), night, rng }
import { Pen } from './proceduralGenerator.js';

const sub = (p, u, v, s) => new Pen(p.o, p.x + Math.round(u * p.w), p.y + Math.round(v * p.h), Math.round(p.w * s), Math.round(p.h * s), p.flip);
const subLit = (k, p2) => ({ ...k, lit: p2.on(k.lit.o) });
const WARM = [255, 222, 120], FIRE1 = [255, 130, 40], FIRE2 = [255, 226, 110];

// ---------------- Humanoides ----------------
const HUM = {
  character: { body: 'cloth', head: 'hair', legs: 'brown', boots: 'bark' },
  knight: { body: 'silver', head: 'helm', legs: 'metal', boots: 'darkstone', weapon: 'sword', shield: 1, armor: 1 },
  wizard: { body: 'purple', head: 'wizard', robe: 1, weapon: 'staff', tall: 1 },
  astronaut: { body: 'white', head: 'astro', legs: 'white', boots: 'metal', bulky: 1, pack: 1 },
  robot: { body: 'metal', head: 'robot', legs: 'metal', boots: 'darkstone' },
  samurai: { body: 'red', head: 'kabuto', legs: 'black', boots: 'black', weapon: 'sword', armor: 1 },
  ninja: { body: 'black', head: 'ninja', legs: 'black', boots: 'black', weapon: 'sword' },
  pirate: { body: 'red', head: 'pirate', legs: 'brown', boots: 'black', weapon: 'sword', tall: 1 },
  king: { body: 'red', head: 'crown', robe: 1, cape: 'purple', tall: 1 },
  princess: { body: 'pink', head: 'crown', robe: 1, hair: 'gold', long: 1, tall: 1 },
  skeleton: { body: 'bone', skin: 'bone', head: 'skull', legs: 'bone', boots: 'bone', thin: 1 },
  alien: { body: 'silver', skin: 'green', head: 'alien', legs: 'silver', boots: 'metal' },
  zombie: { body: 'brown', skin: 'leaf', head: 'hair', legs: 'darkstone', boots: 'black', hair: 'black' },
  elf: { body: 'green', head: 'hair', legs: 'brown', boots: 'bark', hair: 'gold', ears: 1 },
};

function humanoid(kind) {
  return (p0, k0) => {
    const o = HUM[kind], C = k0.C, a = k0.a;
    const p = o.tall || a.hat ? sub(p0, 0.09, 0.18, 0.82) : p0;
    const k = p === p0 ? k0 : subLit(k0, p);
    const armored = o.armor || !!a.armor;
    const body = C.tint(a.armor || a.color, armored && !a.armor && !a.color ? o.body : o.body);
    const skin = C.mat(o.skin || 'skin'), legs = armored && a.armor ? body : C.mat(o.legs || 'brown'), boots = C.mat(o.boots || 'bark');
    const dark = C.dark, t = o.thin ? 0.05 : 0, bk = o.bulky ? 0.03 : 0;
    const head = a.hat === 'wizard' ? 'wizard' : (kind === 'character' && a.armor && a.helmet) ? 'helm' : o.head;
    if (o.cape || a.cape) { const cp = C.tint(a.cape !== true ? a.cape : null, o.cape || 'red'); p.poly([[0.3, 0.36], [0.7, 0.36], [0.86, 0.92], [0.14, 0.92]], cp[1]); p.poly([[0.3, 0.36], [0.4, 0.36], [0.3, 0.92], [0.14, 0.92]], cp[2]); }
    if (o.pack) { const w = C.mat('white'); p.rect(0.1, 0.3, 0.2, 0.36, w[1]); p.rect(0.1, 0.3, 0.2, 0.04, w[2]); }
    if (o.long) { const h = C.mat(o.hair); p.rect(0.24, 0.14, 0.52, 0.34, h[1]); }
    // Piernas y botas
    if (!o.robe) {
      p.rect(0.3 + t - bk, 0.64, 0.18 - t + bk, 0.3, legs[2]); p.rect(0.52, 0.64, 0.18 - t + bk, 0.3, legs[1]);
      p.rect(0.26 + t - bk, 0.9, 0.22 - t + bk, 0.1, boots[2]); p.rect(0.52, 0.9, 0.23 - t + bk, 0.1, boots[1]);
    }
    // Torso
    if (o.robe) {
      p.poly([[0.27, 0.36], [0.73, 0.36], [0.86, 1], [0.14, 1]], body[2]);
      p.poly([[0.58, 0.36], [0.73, 0.36], [0.86, 1], [0.62, 1]], body[1]);
      p.poly([[0.27, 0.36], [0.36, 0.36], [0.26, 1], [0.14, 1]], body[3]);
      p.rect(0.14, 0.95, 0.72, 0.05, body[0]);
    } else {
      p.rect(0.27 + t - bk, 0.36, 0.46 - 2 * t + 2 * bk, 0.3, body[2]);
      p.rect(0.27 + t - bk, 0.36, 0.11, 0.3, body[3]); p.rect(0.62 - t + bk, 0.36, 0.11, 0.3, body[1]);
    }
    const belt = C.mat(armored ? 'brown' : 'bark');
    if (!o.thin && kind !== 'robot' && !o.bulky) p.rect(0.27, 0.6, 0.46, 0.05, belt[1]);
    if (o.thin) { for (const v of [0.4, 0.47, 0.54]) p.rect(0.34, v, 0.32, 0.03, dark); }
    if (o.bulky) { const m = C.mat('metal'); p.rect(0.4, 0.44, 0.2, 0.12, m[2]); k.lit.px(0.44, 0.48, C.rgb([240, 70, 70])); k.lit.px(0.53, 0.48, C.rgb([80, 200, 255])); }
    if (kind === 'robot') { p.rect(0.36, 0.42, 0.28, 0.14, body[0]); k.lit.rect(0.4, 0.46, 0.2, 0.05, C.rgb([90, 240, 220])); }
    // Brazos y manos
    const armW = 0.14 - t + bk;
    p.rect(0.13 + t - bk, 0.38, armW, 0.26, body[3]); p.rect(0.73, 0.38, armW, 0.26, body[1]);
    p.rect(0.13 + t - bk, 0.6, armW, 0.07, o.bulky ? boots[2] : skin[2]); p.rect(0.73, 0.6, armW, 0.07, o.bulky ? boots[1] : skin[1]);
    if (armored) { p.ell(0.2, 0.39, 0.09, 0.045, body[4]); p.ell(0.8, 0.39, 0.09, 0.045, body[2]); p.rect(0.4, 0.4, 0.2, 0.03, body[4]); p.rect(0.47, 0.43, 0.06, 0.15, body[1]); }
    // Cabeza
    const eyes = () => { const eh = p.h >= 36 ? 0.05 : 0.01; p.rect(0.47, 0.21, 0.04, eh, dark); p.rect(0.64, 0.21, 0.04, eh, dark); };
    if (head === 'helm') {
      const m = body; p.ell(0.5, 0.19, 0.24, 0.17, m[2]); p.ell(0.4, 0.12, 0.1, 0.06, m[4], 1); p.rect(0.4, 0.19, 0.36, 0.05, dark); p.rect(0.56, 0.19, 0.04, 0.12, dark);
      const pl = C.tint(a.color2, 'red'); p.poly([[0.52, 0.03], [0.3, -0.06], [0.16, 0.1], [0.36, 0.06]], pl[2]);
    } else if (head === 'astro') {
      const w = body, v = C.hsl(215, 55, 30); p.ell(0.5, 0.2, 0.28, 0.19, w[3]); p.ell(0.42, 0.1, 0.12, 0.05, w[4], 1);
      p.ell(0.56, 0.21, 0.18, 0.12, v[1]); p.ell(0.6, 0.24, 0.12, 0.07, v[2], 1); p.rect(0.56, 0.14, 0.12, 0.03, v[4]);
    } else if (head === 'robot') {
      p.rect(0.28, 0.04, 0.44, 0.28, body[2]); p.rect(0.28, 0.04, 0.1, 0.28, body[3]); p.rect(0.62, 0.04, 0.1, 0.28, body[1]);
      p.rect(0.48, -0.06, 0.04, 0.1, body[1]); k.lit.px(0.49, -0.08, C.rgb([255, 80, 80]));
      p.rect(0.36, 0.13, 0.32, 0.09, dark); k.lit.rect(0.4, 0.15, 0.08, 0.05, C.rgb([90, 240, 255])); k.lit.rect(0.56, 0.15, 0.08, 0.05, C.rgb([90, 240, 255]));
    } else if (head === 'skull') {
      p.ell(0.5, 0.18, 0.22, 0.15, skin[3]); p.rect(0.38, 0.28, 0.24, 0.07, skin[2]); p.rect(0.38, 0.17, 0.09, 0.07, dark); p.rect(0.56, 0.17, 0.09, 0.07, dark); p.px(0.5, 0.26, dark);
    } else if (head === 'alien') {
      p.ell(0.5, 0.17, 0.27, 0.18, skin[2]); p.ell(0.42, 0.1, 0.12, 0.06, skin[3], 1); p.ell(0.4, 0.2, 0.07, 0.05, dark); p.ell(0.64, 0.2, 0.07, 0.05, dark);
    } else {
      p.ell(0.5, 0.2, 0.22, 0.16, skin[2]); p.ell(0.56, 0.24, 0.14, 0.1, skin[3], 1);
      if (o.ears) { p.poly([[0.28, 0.2], [0.16, 0.1], [0.3, 0.14]], skin[2]); }
      if (head === 'ninja') { p.ell(0.5, 0.2, 0.23, 0.17, body[2]); p.rect(0.36, 0.17, 0.4, 0.07, skin[2]); eyes(); p.poly([[0.28, 0.16], [0.08, 0.2], [0.1, 0.26], [0.28, 0.22]], C.col('red')[2]); }
      else {
        eyes();
        if (head === 'hair' || head === 'crown') { const h = C.mat(a.hair || o.hair || 'brown'); p.ell(0.47, 0.1, 0.25, 0.08, h[2]); p.rect(0.26, 0.1, 0.1, 0.14, h[1]); p.ell(0.4, 0.06, 0.1, 0.03, h[3], 1); }
        if (head === 'crown') { const g = C.mat('gold'); p.rect(0.3, 0.02, 0.4, 0.07, g[2]); for (const u of [0.3, 0.46, 0.62]) p.poly([[u, 0.03], [u + 0.04, -0.06], [u + 0.08, 0.03]], g[3]); p.px(0.5, 0.05, C.rgb([230, 60, 80])); }
        if (head === 'wizard') {
          const h = body, w = C.mat('white');
          p.poly([[0.34, 0.26], [0.72, 0.26], [0.56, 0.52]], w[2]); p.poly([[0.34, 0.26], [0.5, 0.26], [0.5, 0.46]], w[3]);
          p.poly([[0.24, 0.12], [0.76, 0.12], [0.62, -0.02], [0.5, -0.22], [0.42, -0.02]], h[2]); p.poly([[0.5, -0.22], [0.62, -0.02], [0.76, 0.12], [0.58, 0.12]], h[1]);
          p.rect(0.12, 0.09, 0.76, 0.05, h[1]); p.rect(0.3, 0.05, 0.4, 0.04, C.mat('gold')[2]);
        }
        if (head === 'kabuto') { const g = C.mat('gold'); p.ell(0.5, 0.13, 0.26, 0.11, body[1]); p.rect(0.2, 0.14, 0.6, 0.05, body[0]); p.poly([[0.4, 0.06], [0.26, -0.1], [0.34, 0.08]], g[2]); p.poly([[0.6, 0.06], [0.74, -0.1], [0.66, 0.08]], g[3]); }
        if (head === 'pirate') { p.poly([[0.16, 0.12], [0.84, 0.12], [0.7, -0.02], [0.5, -0.1], [0.3, -0.02]], dark); p.px(0.5, 0.03, C.mat('bone')[3]); p.rect(0.56, 0.19, 0.12, 0.05, dark); }
      }
    }
    // Armas y escudo
    const weapon = a.weapon || o.weapon;
    if (weapon === 'sword') sword(new Pen(p.o, p.x, p.y, p.w, p.h, p.flip), k, 0.86, 0.6, 0.52);
    if (weapon === 'staff') { const w = C.mat('wood'); p.rect(0.84, 0.08, 0.05, 0.92, w[2]); p.ell(0.86, 0.06, 0.07, 0.05, C.mat('gold')[2]); k.lit.ell(0.86, 0.06, 0.05, 0.035, C.rgb(a.weaponFx === 'fire' ? FIRE2 : [150, 230, 255])); }
    if (a.shield || (o.shield && !a.noShield)) { const s = C.tint(a.color2, 'blue'), g = C.mat('gold'); p.poly([[0.0, 0.4], [0.32, 0.4], [0.32, 0.6], [0.16, 0.76], [0.0, 0.6]], g[2]); p.poly([[0.04, 0.44], [0.28, 0.44], [0.28, 0.59], [0.16, 0.7], [0.04, 0.59]], s[2]); p.poly([[0.04, 0.44], [0.16, 0.44], [0.16, 0.7], [0.04, 0.59]], s[3]); }
  };
}

// Espada vertical con empuñadura en (u,v) y longitud len (reutilizada por el icono).
function sword(p, k, u, v, len, bw = 0.08) {
  const C = k.C, s = C.mat('silver'), g = C.mat('gold'), w = C.mat('wood');
  p.rect(u - bw / 2, v - len, bw, len, s[2]); p.rect(u - bw / 2, v - len, bw / 2, len, s[4]);
  p.poly([[u - bw / 2, v - len], [u, v - len - 0.06], [u + bw / 2, v - len]], s[3]);
  p.rect(u - bw * 1.4, v, bw * 2.8, 0.035, g[2]); p.rect(u - bw * 0.3, v + 0.035, bw * 0.6, 0.08, w[1]); p.rect(u - bw * 0.45, v + 0.11, bw * 0.9, 0.03, g[3]);
  if (k.a.weaponFx === 'fire' || k.a.weaponFx === 'ice') {
    const ice = k.a.weaponFx === 'ice', c1 = C.rgb(ice ? [90, 190, 255] : FIRE1), c2 = C.rgb(ice ? [220, 245, 255] : FIRE2), L = k.lit;
    L.poly([[u - 0.09, v - 0.02], [u - 0.14, v - len * 0.45], [u - 0.07, v - len * 0.7], [u - 0.1, v - len * 0.95], [u, v - len - 0.2], [u + 0.06, v - len * 0.9], [u + 0.13, v - len * 0.72], [u + 0.08, v - len * 0.4], [u + 0.1, v - 0.02]], c1);
    L.poly([[u - 0.04, v - 0.02], [u - 0.05, v - len * 0.6], [u, v - len - 0.06], [u + 0.05, v - len * 0.6], [u + 0.04, v - 0.02]], c2);
  }
}

// ---------------- Cuadrúpedos ----------------
const QUAD = {
  cat: { col: 'orange', neck: 0, head: 0.12, ears: 'pointy', tail: 'cat', leg: 0.26 },
  dog: { col: 'brown', neck: 0.2, head: 0.11, snout: 0.07, ears: 'floppy', tail: 'short', leg: 0.3 },
  wolf: { col: 'gray', neck: 0.25, head: 0.11, snout: 0.08, ears: 'pointy', tail: 'bushy', leg: 0.34 },
  fox: { col: 'orange', neck: 0.15, head: 0.1, snout: 0.08, ears: 'pointy', tail: 'bushy', tip: 1, leg: 0.28 },
  horse: { col: 'brown', neck: 1, head: 0.09, snout: 0.08, ears: 'pointy', tail: 'horse', mane: 'black', leg: 0.46, slim: 1 },
  unicorn: { col: 'white', neck: 1, head: 0.09, snout: 0.08, ears: 'pointy', tail: 'horse', mane: 'pink', leg: 0.46, slim: 1, horn: 1 },
  cow: { col: 'white', neck: 0.2, head: 0.11, snout: 0.07, ears: 'floppy', tail: 'thin', spots: 'black', leg: 0.34, horns: 1 },
  pig: { col: 'pink', neck: 0, head: 0.12, snout: 0.05, ears: 'pointy', tail: 'thin', leg: 0.2, fat: 1 },
  sheep: { col: 'cream', neck: 0.1, head: 0.09, snout: 0.04, ears: 'floppy', tail: 'short', leg: 0.26, wool: 1, face: 'black' },
  bear: { col: 'brown', neck: 0.1, head: 0.13, snout: 0.06, ears: 'round', tail: 'none', leg: 0.3, fat: 1 },
  deer: { col: 'wood', neck: 0.9, head: 0.08, snout: 0.06, ears: 'pointy', tail: 'short', leg: 0.46, slim: 1, antlers: 1 },
  rabbit: { col: 'white', neck: 0, head: 0.12, ears: 'long', tail: 'short', leg: 0.18 },
  lion: { col: 'gold', neck: 0.2, head: 0.12, snout: 0.06, ears: 'round', tail: 'thin', leg: 0.32, lion: 1 },
};

function quadruped(kind) {
  return (p, k) => {
    const o = QUAD[kind], C = k.C, c = C.tint(k.a.color, o.col), dark = C.dark;
    const by = 1 - o.leg - 0.16, ry = o.fat ? 0.2 : o.slim ? 0.13 : 0.16, rx = 0.27;
    const hx = 0.76 + o.neck * 0.06, hy = by - 0.1 - o.neck * 0.28, hr = o.head, hv = hr * 1.4;
    const m = o.mane ? C.mat(o.mane) : c;
    // Cola
    if (o.tail === 'cat') p.poly([[0.2, by], [0.08, by - 0.06], [0.03, by - 0.34], [0.09, by - 0.36], [0.12, by - 0.12], [0.22, by - 0.08]], c[2]);
    if (o.tail === 'bushy') { p.ell(0.1, by - 0.04, 0.11, 0.1, c[1]); p.ell(0.1, by - 0.07, 0.08, 0.06, c[2], 1); if (o.tip) p.ell(0.04, by - 0.06, 0.05, 0.06, C.mat('white')[3], 1); }
    if (o.tail === 'short') p.ell(0.15, by - 0.06, 0.05, 0.06, o.wool ? C.mat('white')[3] : c[3]);
    if (o.tail === 'thin') { p.line(0.16, by - 0.04, 0.06, by + 0.14, c[1]); p.px(0.05, by + 0.16, dark); }
    if (o.tail === 'horse') p.poly([[0.17, by - 0.08], [0.06, by - 0.02], [0.03, by + 0.3], [0.1, by + 0.28], [0.14, by + 0.06]], m[2]);
    // Patas lejanas, cuerpo, patas cercanas
    const lw = o.slim ? 0.06 : 0.08, ly = by + ry * 0.4, lh = 1 - ly;
    p.rect(0.26, ly, lw, lh, c[0]); p.rect(0.56, ly, lw, lh, c[0]);
    p.ell(0.42, by, rx, ry, c[2]); p.ell(0.42, by + ry * 0.45, rx * 0.85, ry * 0.5, c[1], 1); p.ell(0.38, by - ry * 0.45, rx * 0.7, ry * 0.4, c[3], 1);
    p.rect(0.2, ly, lw, lh, c[2]); p.rect(0.62, ly, lw, lh, c[2]);
    const hoof = o.slim || kind === 'cow' || kind === 'pig' ? dark : c[3];
    for (const u of [0.2, 0.62, 0.26, 0.56]) p.rect(u, 0.95, lw, 0.05, hoof);
    if (o.spots) { const s = C.mat(o.spots); p.ell(0.34, by - 0.02, 0.08, 0.07, s[2], 1); p.ell(0.52, by + 0.04, 0.06, 0.06, s[2], 1); }
    if (o.wool) { const w = C.mat('white'); for (const [u, v] of [[0.26, -0.04], [0.4, -0.1], [0.56, -0.05], [0.32, 0.08], [0.5, 0.08], [0.62, 0.02], [0.2, 0.04]]) p.ell(u, by + v, 0.1, 0.1, w[3]); p.ell(0.42, by + 0.1, 0.24, 0.06, w[1], 1); }
    // Cuello y cabeza
    p.poly([[0.56, by - ry * 0.7], [hx - hr * 0.6, hy], [hx + hr * 0.5, hy + hv * 0.6], [0.68, by + ry * 0.3]], c[2]);
    if (o.mane && o.neck > 0.5) p.poly([[0.54, by - ry * 0.8], [hx - hr * 1.3, hy - hv * 0.6], [hx - hr * 0.5, hy - hv * 0.9], [hx - hr * 0.3, hy], [0.6, by - ry * 0.5]], m[2]);
    if (o.lion) { const mn = C.mat('brown'); p.ell(hx - 0.02, hy, hr * 1.6, hv * 1.5, mn[2]); p.ell(hx - 0.05, hy - 0.03, hr * 1.1, hv, mn[3], 1); }
    const face = o.face ? C.mat(o.face) : c;
    if (o.ears === 'pointy') { p.poly([[hx - hr * 0.9, hy - hv * 0.5], [hx - hr * 0.7, hy - hv * 1.7], [hx - hr * 0.1, hy - hv * 0.8]], face[1]); p.poly([[hx, hy - hv * 0.7], [hx + hr * 0.3, hy - hv * 1.7], [hx + hr * 0.8, hy - hv * 0.5]], face[2]); }
    if (o.ears === 'long') { p.rect(hx - hr * 0.7, hy - hv * 2.6, hr * 0.5, hv * 2, c[1]); p.rect(hx + hr * 0.1, hy - hv * 2.8, hr * 0.5, hv * 2.2, c[2]); }
    if (o.ears === 'round') { p.ell(hx - hr * 0.7, hy - hv * 0.9, hr * 0.4, hv * 0.4, face[1]); p.ell(hx + hr * 0.5, hy - hv * 0.9, hr * 0.4, hv * 0.4, face[2]); }
    if (o.antlers) { const b = C.mat('bone'); for (const s of [-0.5, 0.4]) { p.line(hx + hr * s, hy - hv, hx + hr * (s - 0.6), hy - hv * 2.8, b[2]); p.line(hx + hr * (s - 0.3), hy - hv * 1.9, hx + hr * (s + 0.7), hy - hv * 2.6, b[2]); } }
    if (o.horns) { const b = C.mat('bone'); p.line(hx - hr * 0.5, hy - hv, hx - hr * 0.9, hy - hv * 1.6, b[3]); p.line(hx + hr * 0.4, hy - hv, hx + hr * 0.8, hy - hv * 1.6, b[3]); }
    p.ell(hx, hy, hr, hv, face[2]); p.ell(hx - hr * 0.2, hy - hv * 0.4, hr * 0.6, hv * 0.4, face[3], 1);
    if (o.snout) { p.ell(hx + hr * 0.9, hy + hv * 0.3, o.snout, o.snout * 1.2, kind === 'pig' ? c[3] : face[kind === 'fox' || kind === 'dog' ? 3 : 2]); p.px(hx + hr * 0.9 + o.snout, hy + hv * 0.2, dark); }
    else p.px(hx + hr * 0.8, hy + hv * 0.3, C.col('pink')[2]);
    if (o.ears === 'floppy') p.ell(hx - hr * 0.6, hy + hv * 0.1, hr * 0.35, hv * 0.7, face[0]);
    p.px(hx + hr * 0.35, hy - hv * 0.15, o.face ? C.mat('white')[4] : dark);
    if (o.horn) { const g = C.mat('gold'); p.poly([[hx + hr * 0.3, hy - hv * 0.8], [hx + hr * 1.5, hy - hv * 2.6], [hx + hr * 0.8, hy - hv * 0.6]], g[3]); }
  };
}

// ---------------- Registro ----------------
// cat: categoría; ar: ancho/alto; sz: altura relativa al lienzo en una escena;
// air: vuela por defecto; float: flota sobre el agua.
export const LIB = {};
const reg = (id, name, cat, ar, sz, build, extra = {}) => { LIB[id] = { id, name, cat, ar, sz, build, ...extra }; };

for (const [id, name] of [['character', 'personaje'], ['knight', 'caballero'], ['wizard', 'mago'], ['astronaut', 'astronauta'], ['robot', 'robot'], ['samurai', 'samurái'], ['ninja', 'ninja'], ['pirate', 'pirata'], ['king', 'rey'], ['princess', 'princesa'], ['skeleton', 'esqueleto'], ['alien', 'alienígena'], ['zombie', 'zombi'], ['elf', 'elfo']]) reg(id, name, 'humanoid', 0.6, 0.3, humanoid(id));
for (const [id, name, sz] of [['cat', 'gato', 0.13], ['dog', 'perro', 0.15], ['wolf', 'lobo', 0.17], ['fox', 'zorro', 0.14], ['horse', 'caballo', 0.27], ['unicorn', 'unicornio', 0.27], ['cow', 'vaca', 0.2], ['pig', 'cerdo', 0.12], ['sheep', 'oveja', 0.14], ['bear', 'oso', 0.2], ['deer', 'ciervo', 0.25], ['rabbit', 'conejo', 0.09], ['lion', 'león', 0.18]]) reg(id, name, 'animal', 1.4, sz, quadruped(id));

reg('dragon', 'dragón', 'creature', 1.45, 0.4, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), b = C.tint(k.a.color2, 'gold'), bone = C.mat('bone'), dark = C.dark;
  // Ala lejana
  p.poly([[0.5, 0.5], [0.58, 0.14], [0.76, 0.02], [0.93, 0.12], [0.85, 0.2], [0.88, 0.33], [0.76, 0.3], [0.72, 0.43], [0.62, 0.4]], c[0]);
  // Cola con punta
  p.poly([[0.32, 0.5], [0.18, 0.5], [0.08, 0.42], [0.03, 0.3], [0.07, 0.46], [0.15, 0.6], [0.32, 0.66]], c[2]);
  p.poly([[0.03, 0.32], [0.0, 0.2], [0.09, 0.27], [0.08, 0.36]], c[1]);
  p.poly([[0.3, 0.6], [0.4, 0.6], [0.38, 0.8], [0.45, 0.85], [0.31, 0.85], [0.27, 0.74]], c[1]);
  // Cuerpo, vientre, cuello
  p.ell(0.42, 0.56, 0.17, 0.12, c[2]); p.ell(0.38, 0.5, 0.12, 0.05, c[3], 1); p.ell(0.45, 0.64, 0.13, 0.05, b[2], 1);
  p.poly([[0.5, 0.46], [0.62, 0.38], [0.71, 0.24], [0.8, 0.28], [0.72, 0.44], [0.6, 0.62]], c[2]);
  p.poly([[0.6, 0.62], [0.72, 0.44], [0.79, 0.31], [0.75, 0.46], [0.65, 0.62]], b[2]);
  for (const [u, v] of [[0.3, 0.44], [0.4, 0.42], [0.5, 0.42], [0.59, 0.36], [0.66, 0.27]]) p.poly([[u - 0.03, v + 0.02], [u, v - 0.06], [u + 0.03, v + 0.02]], c[1]);
  // Cabeza
  p.poly([[0.77, 0.2], [0.65, 0.07], [0.74, 0.19]], bone[2]); p.poly([[0.82, 0.18], [0.73, 0.03], [0.79, 0.18]], bone[3]);
  p.ell(0.81, 0.24, 0.07, 0.07, c[2]); p.poly([[0.8, 0.19], [0.95, 0.23], [0.98, 0.3], [0.8, 0.31]], c[2]); p.poly([[0.8, 0.31], [0.95, 0.33], [0.84, 0.38]], c[1]);
  p.ell(0.8, 0.21, 0.04, 0.03, c[3], 1); p.px(0.95, 0.25, dark); k.lit.px(0.84, 0.23, C.rgb([255, 230, 90]));
  if (p.w >= 40) { p.px(0.9, 0.32, bone[4]); p.px(0.94, 0.32, bone[4]); }
  p.poly([[0.5, 0.62], [0.58, 0.6], [0.6, 0.76], [0.67, 0.81], [0.54, 0.82], [0.5, 0.74]], c[2]);
  // Ala cercana: membrana, dedos
  p.poly([[0.46, 0.48], [0.38, 0.14], [0.2, 0.0], [0.02, 0.05], [0.11, 0.13], [0.06, 0.25], [0.18, 0.25], [0.2, 0.38], [0.3, 0.35], [0.34, 0.48]], c[1]);
  p.poly([[0.38, 0.14], [0.2, 0.0], [0.02, 0.05], [0.11, 0.13], [0.3, 0.14]], c[2]);
  p.line(0.46, 0.48, 0.38, 0.14, c[3]); p.line(0.38, 0.14, 0.2, 0.0, c[3]); p.line(0.38, 0.14, 0.18, 0.25, c[3]); p.line(0.38, 0.14, 0.3, 0.35, c[3]); p.line(0.2, 0.0, 0.02, 0.05, c[3]);
  if (k.a.fire) { const L = k.lit; L.poly([[0.97, 0.27], [1.2, 0.2], [1.32, 0.3], [1.22, 0.44], [0.97, 0.33]], C.rgb(FIRE1)); L.poly([[0.97, 0.28], [1.14, 0.26], [1.2, 0.31], [1.12, 0.37], [0.97, 0.32]], C.rgb(FIRE2)); }
}, { air: 1 });

reg('bird', 'pájaro', 'animal', 1.5, 0.08, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, k.night ? 'black' : 'white');
  if (p.w < 9) { p.line(0, 0.2, 0.5, 0.7, c[1]); p.line(0.5, 0.7, 1, 0.2, c[1]); return; }
  p.poly([[0.3, 0.5], [0.1, 0.05], [0.5, 0.3], [0.56, 0.5]], c[1]); p.ell(0.5, 0.58, 0.24, 0.16, c[2]); p.poly([[0.26, 0.56], [0.02, 0.6], [0.08, 0.74], [0.3, 0.68]], c[1]);
  p.ell(0.76, 0.46, 0.11, 0.13, c[3]); p.poly([[0.86, 0.42], [1, 0.5], [0.86, 0.54]], C.col('orange')[2]); p.px(0.79, 0.43, C.dark);
  p.poly([[0.4, 0.55], [0.3, 0.1], [0.66, 0.4]], c[3]);
}, { air: 1 });

reg('bat', 'murciélago', 'animal', 1.9, 0.08, (p, k) => {
  const c = k.C.tint(k.a.color, 'black');
  p.poly([[0.42, 0.4], [0.2, 0.1], [0.0, 0.3], [0.08, 0.5], [0.18, 0.4], [0.26, 0.62], [0.36, 0.5]], c[2]);
  p.poly([[0.58, 0.4], [0.8, 0.1], [1.0, 0.3], [0.92, 0.5], [0.82, 0.4], [0.74, 0.62], [0.64, 0.5]], c[2]);
  p.ell(0.5, 0.5, 0.09, 0.22, c[3]); p.poly([[0.42, 0.3], [0.44, 0.08], [0.5, 0.28]], c[3]); p.poly([[0.5, 0.28], [0.56, 0.08], [0.58, 0.3]], c[3]);
  k.lit.px(0.46, 0.42, k.C.rgb([255, 90, 90])); k.lit.px(0.54, 0.42, k.C.rgb([255, 90, 90]));
}, { air: 1 });

reg('butterfly', 'mariposa', 'animal', 1.2, 0.06, (p, k) => {
  const c = k.C.tint(k.a.color, 'orange');
  p.ell(0.26, 0.32, 0.22, 0.26, c[2]); p.ell(0.74, 0.32, 0.22, 0.26, c[2]); p.ell(0.3, 0.72, 0.16, 0.18, c[3]); p.ell(0.7, 0.72, 0.16, 0.18, c[3]);
  p.ell(0.26, 0.32, 0.1, 0.12, c[4]); p.ell(0.74, 0.32, 0.1, 0.12, c[4]); p.rect(0.47, 0.14, 0.06, 0.76, k.C.dark);
}, { air: 1 });

reg('fish', 'pez', 'animal', 1.6, 0.12, (p, k) => fishy(p, k, 'orange', 0));
reg('shark', 'tiburón', 'animal', 2.2, 0.16, (p, k) => fishy(p, k, 'gray', 1));
reg('whale', 'ballena', 'animal', 2, 0.24, (p, k) => fishy(p, k, 'blue', 2));
function fishy(p, k, col, kind) {
  const C = k.C, c = C.tint(k.a.color, col), w = C.mat('white');
  p.poly([[0.24, 0.5], [0.02, 0.14], [0.1, 0.5], [0.02, 0.86]], c[1]);
  p.ell(0.55, 0.5, 0.36, kind === 2 ? 0.34 : 0.28, c[2]); p.ell(0.5, 0.36, 0.26, 0.12, c[3], 1); p.ell(0.56, 0.68, 0.3, 0.14, kind ? w[3] : c[4], 1);
  if (kind === 1) { p.poly([[0.44, 0.26], [0.56, -0.08], [0.66, 0.26]], c[1]); p.poly([[0.8, 0.62], [0.9, 0.58], [0.84, 0.66]], w[4]); }
  else p.poly([[0.46, 0.26], [0.5, 0.1], [0.66, 0.24]], c[1]);
  p.poly([[0.5, 0.62], [0.42, 0.86], [0.6, 0.7]], c[1]);
  p.px(0.78, 0.42, C.dark);
  if (kind === 2) { const s = C.rgb([210, 235, 255]); p.line(0.6, 0.14, 0.6, -0.1, s); p.line(0.6, -0.1, 0.52, -0.2, s); p.line(0.6, -0.1, 0.68, -0.2, s); }
}

reg('slime', 'slime', 'creature', 1.25, 0.14, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'green');
  p.ell(0.5, 0.62, 0.48, 0.4, c[2]); p.rect(0, 0.86, 1, 0.3, -1); p.rect(0.06, 0.82, 0.88, 0.05, c[1]);
  p.ell(0.5, 0.74, 0.4, 0.12, c[1], 1); p.ell(0.34, 0.42, 0.14, 0.1, c[4], 1); p.rect(0.34, 0.56, 0.07, 0.14, C.dark); p.rect(0.6, 0.56, 0.07, 0.14, C.dark);
});

reg('ghost', 'fantasma', 'creature', 0.8, 0.22, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'white');
  p.ell(0.5, 0.36, 0.42, 0.34, c[3]); p.rect(0.08, 0.36, 0.84, 0.52, c[3]);
  for (const u of [0.08, 0.36, 0.64]) p.poly([[u, 0.88], [u + 0.14, 1], [u + 0.28, 0.88]], c[3]);
  p.rect(0.64, 0.3, 0.28, 0.58, c[2], 1); p.ell(0.34, 0.4, 0.07, 0.09, C.dark); p.ell(0.6, 0.4, 0.07, 0.09, C.dark); p.ell(0.47, 0.6, 0.05, 0.05, C.dark);
}, { air: 1, alpha: 0.88 });

// ---------------- Estructuras ----------------
// Aparejo de piedra: patrón en mosaico (B) sobre lo ya dibujado; en tamaños
// pequeños se reduce a unas pocas motas para no generar ruido.
function bricks(p, k, s) {
  if (k.C.style.flat) return;
  if (!k.sprites || p.w < 44) { p.tex(s[1], 4, 11); return; }
  const tw = Math.max(5, Math.round(p.w / 11)), th = Math.max(3, Math.round(tw * 0.55));
  const id = 'brick' + tw;
  k.sprites[id] = { w: tw, h: th * 2, o: [['R', 0, th - 1, tw, 1, s[1]], ['R', 0, 2 * th - 1, tw, 1, s[1]], ['R', 0, 0, 1, th - 1, s[1]], ['R', tw >> 1, th, 1, th - 1, s[1]]] };
  p.o.push(['B', p.x, p.y, p.w + 1, p.h + 1, id, 1]);
  p.o.push(['T', p.x, p.y, p.w + 1, p.h + 1, s[2], 55, 3, 1]);
}
function lightOrDark(k, u, v, w, h, p) { if (k.night) k.lit.rect(u, v, w, h, k.C.rgb(WARM)); else p.rect(u, v, w, h, k.C.dark); }

reg('castle', 'castillo', 'structure', 1.3, 0.5, (p, k) => {
  const C = k.C, s = C.mat('stone'), rf = C.tint(k.a.color, 'roof'), dark = C.dark, fl = C.tint(k.a.color2 || k.a.color, 'red');
  const crenel = (u, v, w, n, c) => { const st = w / (2 * n - 1); for (let i = 0; i < n; i++) p.rect(u + i * 2 * st, v, st, 0.045, c); };
  const towers = [[0.4, 0.2, 0.3], [0.04, 0.2, 0.42], [0.76, 0.2, 0.42]];
  const stone = ([u, w, top]) => {
    p.rect(u, top, w, 1 - top, s[2]); p.rect(u, top, w * 0.3, 1 - top, s[3]); p.rect(u + w * 0.72, top, w * 0.28, 1 - top, s[1]);
    p.rect(u - 0.015, top - 0.03, w + 0.03, 0.05, s[2]); p.rect(u - 0.015, top + 0.02, w + 0.03, 0.015, s[0]);
  };
  stone(towers[0]);
  p.rect(0.14, 0.55, 0.72, 0.45, s[2]); p.rect(0.14, 0.55, 0.72, 0.03, s[3]); crenel(0.14, 0.51, 0.72, 9, s[2]);
  p.rect(0.14, 0.96, 0.72, 0.04, s[1]);
  stone(towers[1]); stone(towers[2]);
  bricks(p, k, s);
  for (const [u, w, top] of towers) {
    p.poly([[u - 0.03, top - 0.03], [u + w / 2, top - 0.28], [u + w + 0.03, top - 0.03]], rf[2]); p.poly([[u + w / 2, top - 0.28], [u + w + 0.03, top - 0.03], [u + w * 0.55, top - 0.03]], rf[1]);
    p.line(u - 0.03, top - 0.03, u + w / 2, top - 0.28, rf[3]);
    p.line(u + w / 2, top - 0.28, u + w / 2, top - 0.4, dark); p.poly([[u + w / 2, top - 0.4], [u + w / 2 + 0.07, top - 0.37], [u + w / 2, top - 0.33]], fl[2]);
    lightOrDark(k, u + w * 0.38, top + 0.1, w * 0.24, 0.07, p);
  }
  p.ell(0.5, 0.78, 0.07, 0.06, dark); p.rect(0.43, 0.78, 0.15, 0.22, dark);
  const w = C.mat('wood'); p.rect(0.45, 0.8, 0.11, 0.2, w[1]); p.rect(0.5, 0.8, 0.01, 0.2, dark);
  lightOrDark(k, 0.29, 0.68, 0.05, 0.08, p); lightOrDark(k, 0.66, 0.68, 0.05, 0.08, p);
});

reg('house', 'casa', 'structure', 1.2, 0.3, (p, k) => {
  const C = k.C, wl = C.tint(k.a.color, 'cream'), rf = C.tint(k.a.color2, 'roof'), w = C.mat('wood'), s = C.mat('stone');
  p.rect(0.68, 0.1, 0.11, 0.3, s[2]); p.rect(0.66, 0.08, 0.15, 0.05, s[1]);
  p.rect(0.14, 0.48, 0.72, 0.52, wl[2]); p.rect(0.14, 0.48, 0.72, 0.06, wl[0]); p.rect(0.72, 0.48, 0.14, 0.52, wl[1]); p.rect(0.14, 0.94, 0.72, 0.06, s[1]);
  p.poly([[0.02, 0.5], [0.5, 0.04], [0.98, 0.5]], rf[2]); p.poly([[0.5, 0.04], [0.98, 0.5], [0.62, 0.5]], rf[1]); p.line(0.02, 0.5, 0.5, 0.04, rf[3]); p.rect(0.02, 0.48, 0.96, 0.04, rf[0]);
  p.rect(0.43, 0.66, 0.15, 0.34, w[1]); p.rect(0.43, 0.66, 0.15, 0.03, w[0]); p.px(0.55, 0.84, C.mat('gold')[3]);
  for (const u of [0.22, 0.66]) { p.rect(u - 0.015, 0.615, 0.15, 0.19, w[0]); lightOrDark(k, u, 0.63, 0.12, 0.16, p); }
  lightOrDark(k, 0.46, 0.26, 0.08, 0.1, p);
  if (k.night || k.a.smoke) { /* humo de la chimenea en la capa principal */ }
});

reg('tower', 'torre', 'structure', 0.5, 0.52, (p, k) => {
  const C = k.C, s = C.mat('stone'), rf = C.tint(k.a.color, 'purple');
  p.poly([[0.24, 0.3], [0.76, 0.3], [0.84, 1], [0.16, 1]], s[2]); p.poly([[0.24, 0.3], [0.4, 0.3], [0.36, 1], [0.16, 1]], s[3]); p.poly([[0.62, 0.3], [0.76, 0.3], [0.84, 1], [0.66, 1]], s[1]);
  p.rect(0.14, 0.26, 0.72, 0.06, s[1]); p.poly([[0.1, 0.27], [0.5, 0], [0.9, 0.27]], rf[2]); p.poly([[0.5, 0], [0.9, 0.27], [0.56, 0.27]], rf[1]);
  lightOrDark(k, 0.42, 0.4, 0.16, 0.08, p); lightOrDark(k, 0.42, 0.62, 0.16, 0.08, p);
  bricks(p, k, s);
  p.rect(0.4, 0.86, 0.2, 0.14, C.mat('wood')[1]);
});

reg('lighthouse', 'faro', 'structure', 0.42, 0.52, (p, k) => {
  const C = k.C, w = C.mat('white'), r = C.tint(k.a.color, 'red'), m = C.mat('darkstone');
  p.poly([[0.3, 0.26], [0.7, 0.26], [0.84, 1], [0.16, 1]], w[3]);
  for (const v of [0.34, 0.6, 0.86]) p.rect(0, v, 1, 0.12, r[2], 1);
  p.shade(0.6, 0.26, 0.3, 0.74, -1);
  p.rect(0.22, 0.22, 0.56, 0.05, m[2]); p.rect(0.32, 0.1, 0.36, 0.12, m[1]); p.poly([[0.26, 0.1], [0.5, 0], [0.74, 0.1]], r[2]);
  k.lit.rect(0.36, 0.12, 0.28, 0.09, C.rgb([255, 240, 160]));
  if (k.night) k.glow.push(['P', [p.X(0.5), p.Y(0.16), p.X(k.beamDir * 4 + 0.5), p.Y(-0.12), p.X(k.beamDir * 4 + 0.5), p.Y(0.5)], C.rgb([255, 240, 160])]);
});

reg('temple', 'templo', 'structure', 1.05, 0.46, (p, k) => {
  const C = k.C, r = C.tint(k.a.color, 'red'), rf = C.mat('darkstone'), g = C.mat('gold');
  const tier = (u, v, w, h) => {
    p.rect(u + w * 0.14, v + h * 0.4, w * 0.72, h * 0.6, r[2]); p.rect(u + w * 0.64, v + h * 0.4, w * 0.22, h * 0.6, r[1]);
    lightOrDark(k, u + w * 0.42, v + h * 0.55, w * 0.16, h * 0.35, p);
    p.poly([[u - 0.03, v + h * 0.34], [u + w * 0.1, v + h * 0.4], [u + w * 0.3, v], [u + w * 0.7, v], [u + w * 0.9, v + h * 0.4], [u + w + 0.03, v + h * 0.34], [u + w, v + h * 0.5], [u, v + h * 0.5]], rf[2]);
    p.rect(u, v + h * 0.44, w, h * 0.07, rf[0]);
  };
  tier(0.08, 0.66, 0.84, 0.34); tier(0.2, 0.36, 0.6, 0.32); tier(0.3, 0.1, 0.4, 0.28);
  p.rect(0.49, 0, 0.03, 0.12, g[2]);
});

reg('torii', 'torii', 'structure', 1.25, 0.3, (p, k) => {
  const C = k.C, r = C.tint(k.a.color, 'red'), d = C.mat('black');
  p.rect(0.2, 0.16, 0.09, 0.84, r[2]); p.rect(0.71, 0.16, 0.09, 0.84, r[2]); p.rect(0.26, 0.16, 0.03, 0.84, r[1]); p.rect(0.77, 0.16, 0.03, 0.84, r[1]);
  p.rect(0.12, 0.3, 0.76, 0.07, r[2]); p.rect(0.47, 0.18, 0.06, 0.12, r[1]);
  p.poly([[0.0, 0.04], [0.14, 0.1], [0.86, 0.1], [1.0, 0.04], [0.98, 0.18], [0.02, 0.18]], r[3]); p.poly([[0.0, 0.02], [0.14, 0.07], [0.86, 0.07], [1.0, 0.02], [1, 0.08], [0.86, 0.12], [0.14, 0.12], [0, 0.08]], d[2]);
  p.rect(0.18, 0.94, 0.13, 0.06, d[2]); p.rect(0.69, 0.94, 0.13, 0.06, d[2]);
});

reg('pyramid', 'pirámide', 'structure', 1.7, 0.34, (p, k) => {
  const C = k.C, s = C.tint(k.a.color, 'sand');
  p.poly([[0, 1], [0.5, 0], [1, 1]], s[2]); p.poly([[0.5, 0], [1, 1], [0.62, 1]], s[1]); p.line(0, 1, 0.5, 0, s[4]);
  if (!C.style.flat) for (let v = 0.2; v < 1; v += 0.14) p.rect(0, v, 1, 0.012, s[1], 1);
});

reg('windmill', 'molino', 'structure', 0.9, 0.44, (p, k) => {
  const C = k.C, w = C.tint(k.a.color, 'cream'), rf = C.mat('roof'), wd = C.mat('wood');
  p.poly([[0.36, 0.38], [0.64, 0.38], [0.74, 1], [0.26, 1]], w[2]); p.poly([[0.56, 0.38], [0.64, 0.38], [0.74, 1], [0.6, 1]], w[1]);
  p.poly([[0.32, 0.4], [0.5, 0.22], [0.68, 0.4]], rf[2]); p.rect(0.44, 0.82, 0.12, 0.18, wd[1]); lightOrDark(k, 0.46, 0.56, 0.08, 0.1, p);
  for (const [du, dv] of [[0.42, -0.3], [0.3, 0.42], [-0.42, 0.3], [-0.3, -0.42]]) {
    p.line(0.5, 0.34, 0.5 + du, 0.34 + dv, wd[0]);
    p.poly([[0.5 + du * 0.3, 0.34 + dv * 0.3], [0.5 + du, 0.34 + dv], [0.5 + du - dv * 0.25, 0.34 + dv + du * 0.25], [0.5 + du * 0.3 - dv * 0.25, 0.34 + dv * 0.3 + du * 0.25]], wd[3]);
  }
  p.ell(0.5, 0.34, 0.03, 0.03, wd[0]);
});

reg('tent', 'tienda de campaña', 'structure', 1.4, 0.18, (p, k) => {
  const c = k.C.tint(k.a.color, 'orange');
  p.poly([[0, 1], [0.5, 0], [1, 1]], c[2]); p.poly([[0.5, 0], [1, 1], [0.66, 1]], c[1]); p.poly([[0.5, 0.3], [0.62, 1], [0.38, 1]], k.C.dark);
  if (k.night) k.lit.poly([[0.5, 0.5], [0.57, 1], [0.43, 1]], k.C.rgb(WARM));
});

reg('skyscraper', 'rascacielos', 'structure', 0.4, 0.74, (p, k) => {
  const C = k.C, b = C.tint(k.a.color, 'concrete'), g = C.hsl(205, 40, 40);
  p.rect(0.1, 0.08, 0.8, 0.92, b[2]); p.rect(0.1, 0.08, 0.2, 0.92, b[3]); p.rect(0.7, 0.08, 0.2, 0.92, b[1]); p.rect(0.3, 0, 0.4, 0.08, b[1]); p.rect(0.48, -0.1, 0.04, 0.1, b[0]);
  const cols = [0.18, 0.34, 0.5, 0.66];
  for (let v = 0.13; v < 0.92; v += 0.065) for (const u of cols) { if (k.night && k.rng.chance(0.55)) k.lit.rect(u, v, 0.1, 0.035, C.rgb(WARM)); else p.rect(u, v, 0.1, 0.035, g[k.night ? 0 : 3]); }
  k.lit.px(0.5, -0.1, C.rgb([255, 80, 80]));
});

// ---------------- Flora y atrezo ----------------
function canopy(p, c, rng, blobs) {
  for (const [u, v, ru, rv] of blobs) p.ell(u, v, ru, rv, c[1]);
  for (const [u, v, ru, rv] of blobs) p.ell(u - 0.03, v - 0.04, ru * 0.86, rv * 0.82, c[2], 1);
  for (const [u, v, ru, rv] of blobs) p.ell(u - 0.08, v - 0.1, ru * 0.5, rv * 0.42, c[3], 1);
}
reg('tree', 'árbol', 'flora', 0.85, 0.34, (p, k) => {
  const C = k.C, l = C.tint(k.a.color, 'leaf'), b = C.mat('bark');
  p.poly([[0.42, 0.55], [0.58, 0.55], [0.62, 1], [0.38, 1]], b[2]); p.poly([[0.52, 0.55], [0.58, 0.55], [0.62, 1], [0.54, 1]], b[1]); p.line(0.5, 0.62, 0.66, 0.5, b[1]);
  canopy(p, l, k.rng, [[0.3, 0.42, 0.26, 0.2], [0.7, 0.42, 0.26, 0.2], [0.5, 0.24, 0.3, 0.22], [0.5, 0.46, 0.3, 0.16]]);
  if (!C.style.flat) p.o.push(['T', p.x, p.y, p.w + 1, Math.round(p.h * 0.45), l[4], 3, 91, 1]);
});
reg('cherry', 'cerezo', 'flora', 1, 0.36, (p, k) => {
  const C = k.C, l = C.tint(k.a.color, 'cherry'), b = C.mat('bark');
  p.poly([[0.44, 0.5], [0.56, 0.5], [0.6, 1], [0.4, 1]], b[2]); p.poly([[0.5, 0.5], [0.56, 0.5], [0.6, 1], [0.52, 1]], b[1]);
  p.line(0.48, 0.62, 0.26, 0.44, b[2]); p.line(0.54, 0.6, 0.76, 0.42, b[1]);
  canopy(p, l, k.rng, [[0.24, 0.4, 0.22, 0.17], [0.76, 0.4, 0.22, 0.17], [0.5, 0.22, 0.3, 0.2], [0.42, 0.42, 0.24, 0.14], [0.64, 0.3, 0.2, 0.16]]);
  if (!C.style.flat) { p.o.push(['T', p.x, p.y, p.w + 1, Math.round(p.h * 0.58), l[4], 9, 31, 1]); p.o.push(['T', p.x, p.y + Math.round(p.h * 0.55), p.w + 1, Math.round(p.h * 0.45), l[3], 3, 47, 2]); }
});
reg('pine', 'pino', 'flora', 0.6, 0.38, (p, k) => {
  const C = k.C, l = C.tint(k.a.color, 'pine'), b = C.mat('bark'), s = k.a.snowy ? C.mat('snow') : null;
  p.rect(0.44, 0.84, 0.12, 0.16, b[1]);
  for (const [v0, v1, hw] of [[0.5, 0.9, 0.5], [0.26, 0.66, 0.4], [0.0, 0.4, 0.28]]) {
    p.poly([[0.5 - hw, v1], [0.5, v0], [0.5 + hw, v1]], l[2]); p.poly([[0.5, v0], [0.5 + hw, v1], [0.56, v1]], l[1]); p.line(0.5 - hw, v1, 0.5, v0, l[3]);
    if (s) { p.poly([[0.5, v0], [0.5 - hw * 0.5, v0 + (v1 - v0) * 0.5], [0.5 - hw * 0.1, v0 + (v1 - v0) * 0.36], [0.5 + hw * 0.16, v0 + (v1 - v0) * 0.52], [0.5 + hw * 0.4, v0 + (v1 - v0) * 0.4]], s[3]); }
  }
});
reg('palm', 'palmera', 'flora', 0.8, 0.38, (p, k) => {
  const C = k.C, l = C.tint(k.a.color, 'leaf'), b = C.mat('wood');
  p.poly([[0.4, 1], [0.44, 0.6], [0.52, 0.26], [0.6, 0.26], [0.54, 0.62], [0.52, 1]], b[2]); p.poly([[0.48, 1], [0.5, 0.62], [0.57, 0.26], [0.6, 0.26], [0.54, 0.62], [0.52, 1]], b[1]);
  for (const [u, v, c] of [[0.04, 0.34, 1], [1.0, 0.36, 1], [0.1, 0.08, 2], [0.96, 0.1, 2], [0.5, -0.04, 3], [0.26, 0.46, 1], [0.84, 0.48, 1]]) {
    const mu = (0.56 + u) / 2, mv = Math.min(0.26, v) - 0.1;
    p.poly([[0.56, 0.26], [mu, mv], [u, v], [mu, mv + 0.1]], l[c]);
  }
  p.ell(0.52, 0.3, 0.04, 0.03, b[0]); p.ell(0.6, 0.31, 0.04, 0.03, b[0]);
});
reg('deadtree', 'árbol seco', 'flora', 0.8, 0.3, (p, k) => {
  const b = k.C.mat('bark');
  p.poly([[0.44, 0.4], [0.54, 0.4], [0.6, 1], [0.38, 1]], b[2]); p.poly([[0.5, 0.4], [0.54, 0.4], [0.6, 1], [0.52, 1]], b[1]);
  for (const [a, b2, c, d] of [[0.48, 0.44, 0.2, 0.14], [0.2, 0.14, 0.1, 0.2], [0.2, 0.14, 0.24, 0], [0.52, 0.5, 0.82, 0.2], [0.82, 0.2, 0.94, 0.26], [0.82, 0.2, 0.78, 0.04], [0.5, 0.42, 0.52, 0.12]]) p.line(a, b2, c, d, b[2]);
});
reg('cactus', 'cactus', 'flora', 0.6, 0.2, (p, k) => {
  const c = k.C.tint(k.a.color, 'green');
  p.rect(0.38, 0.1, 0.24, 0.9, c[2]); p.ell(0.5, 0.1, 0.12, 0.08, c[2]); p.rect(0.38, 0.1, 0.08, 0.9, c[3]); p.rect(0.56, 0.1, 0.06, 0.9, c[1]);
  p.rect(0.1, 0.5, 0.28, 0.12, c[2]); p.rect(0.1, 0.28, 0.13, 0.3, c[2]); p.rect(0.62, 0.62, 0.28, 0.12, c[1]); p.rect(0.78, 0.4, 0.12, 0.3, c[1]);
});
reg('bush', 'arbusto', 'flora', 1.6, 0.07, (p, k) => { const l = k.C.tint(k.a.color, 'leaf'); canopy(p, l, k.rng, [[0.28, 0.62, 0.26, 0.36], [0.72, 0.62, 0.26, 0.36], [0.5, 0.46, 0.3, 0.42]]); p.rect(0, 0.98, 1, 0.3, -1); });
reg('flower', 'flor', 'flora', 0.6, 0.055, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, k.rng.pick(['red', 'yellow', 'pink', 'white', 'purple'])), g = C.mat('leaf');
  p.rect(0.45, 0.4, 0.12, 0.6, g[2]);
  if (p.h < 6) { p.px(0.45, 0.2, c[3]); return; }
  p.ell(0.5, 0.26, 0.3, 0.2, c[3]); p.px(0.46, 0.24, C.col('yellow')[3]); p.px(0.2, 0.7, g[3]);
});
reg('rock', 'roca', 'flora', 1.5, 0.07, (p, k) => {
  const s = k.C.mat(k.a.mat || 'stone');
  p.poly([[0, 1], [0.12, 0.4], [0.4, 0.04], [0.76, 0.2], [1, 1]], s[2]); p.poly([[0.4, 0.04], [0.76, 0.2], [1, 1], [0.56, 1]], s[1]); p.line(0.12, 0.4, 0.4, 0.04, s[4]);
});
reg('crater', 'cráter', 'flora', 3.2, 0.035, (p, k) => { const s = k.C.mat(k.a.mat || 'moon'); p.ell(0.5, 0.5, 0.48, 0.42, s[3]); p.ell(0.5, 0.56, 0.4, 0.3, s[0]); p.ell(0.5, 0.7, 0.32, 0.16, s[1]); });
reg('mushroom', 'seta', 'flora', 1, 0.07, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), w = C.mat('cream');
  p.rect(0.38, 0.5, 0.24, 0.5, w[2]); p.ell(0.5, 0.48, 0.48, 0.42, c[2]); p.rect(0, 0.52, 1, 0.5, -1, 0); p.rect(0.38, 0.52, 0.24, 0.48, w[2]); p.rect(0.54, 0.52, 0.08, 0.48, w[1]);
  p.px(0.3, 0.26, w[4]); p.px(0.62, 0.18, w[4]); if (p.w > 8) p.px(0.76, 0.36, w[4]);
});
reg('campfire', 'hoguera', 'flora', 1.1, 0.12, (p, k) => {
  const C = k.C, w = C.mat('wood'), L = k.lit;
  p.poly([[0.06, 0.92], [0.14, 0.8], [0.92, 0.92], [0.86, 1]], w[1]); p.poly([[0.94, 0.92], [0.86, 0.8], [0.08, 0.92], [0.14, 1]], w[2]);
  L.poly([[0.24, 0.9], [0.18, 0.56], [0.34, 0.66], [0.44, 0.1], [0.62, 0.52], [0.76, 0.4], [0.78, 0.9]], C.rgb(FIRE1));
  L.poly([[0.38, 0.9], [0.36, 0.62], [0.48, 0.4], [0.62, 0.66], [0.62, 0.9]], C.rgb(FIRE2));
  k.glow.push(['E', p.X(0.5), p.Y(0.6), Math.round(p.w * 1.3), Math.round(p.h * 1.1), C.rgb(FIRE1)]);
});
reg('crystal', 'cristal', 'flora', 0.9, 0.14, (p, k) => {
  const c = k.C.tint(k.a.color, 'cyan');
  p.poly([[0.36, 1], [0.34, 0.3], [0.5, 0], [0.66, 0.3], [0.64, 1]], c[2]); p.poly([[0.5, 0], [0.66, 0.3], [0.64, 1], [0.5, 1]], c[1]); p.line(0.36, 0.98, 0.34, 0.3, c[4]);
  p.poly([[0.06, 1], [0.1, 0.56], [0.22, 0.44], [0.32, 0.6], [0.32, 1]], c[3]); p.poly([[0.68, 1], [0.7, 0.66], [0.82, 0.52], [0.94, 0.7], [0.92, 1]], c[1]);
  k.lit.px(0.44, 0.3, k.C.rgb([240, 255, 255]));
});
reg('lamp', 'farola', 'flora', 0.34, 0.3, (p, k) => {
  const C = k.C, m = C.mat('darkstone');
  p.rect(0.42, 0.12, 0.16, 0.88, m[2]); p.rect(0.3, 0.94, 0.4, 0.06, m[1]); p.rect(0.2, 0.06, 0.6, 0.08, m[1]);
  const c = C.rgb(k.neon ? [110, 240, 255] : WARM);
  if (k.night) { k.lit.rect(0.26, 0.14, 0.48, 0.07, c); k.glow.push(['E', p.X(0.5), p.Y(0.18), Math.round(p.h * 0.22), Math.round(p.h * 0.22), c]); } else p.rect(0.26, 0.14, 0.48, 0.07, C.mat('cream')[3]);
});

// ---------------- Vehículos ----------------
reg('car', 'coche', 'vehicle', 2.2, 0.15, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), g = C.mat('glass'), d = C.dark, m = C.mat('silver');
  p.poly([[0.2, 0.5], [0.3, 0.12], [0.68, 0.12], [0.82, 0.5]], c[2]); p.poly([[0.26, 0.48], [0.33, 0.2], [0.48, 0.2], [0.48, 0.48]], g[2]); p.poly([[0.53, 0.48], [0.53, 0.2], [0.66, 0.2], [0.76, 0.48]], g[3]);
  p.rect(0.02, 0.46, 0.96, 0.34, c[2]); p.rect(0.02, 0.46, 0.96, 0.06, c[3]); p.rect(0.02, 0.72, 0.96, 0.08, c[1]); p.rect(0, 0.66, 0.06, 0.1, m[2]); p.rect(0.94, 0.66, 0.06, 0.1, m[2]);
  for (const u of [0.24, 0.76]) { p.ell(u, 0.82, 0.11, 0.2, d); p.ell(u, 0.82, 0.05, 0.09, m[2]); }
  if (k.night) { k.lit.rect(0.93, 0.52, 0.06, 0.1, C.rgb([255, 244, 180])); k.lit.rect(0.01, 0.52, 0.04, 0.1, C.rgb([255, 70, 70])); k.glow.push(['P', [p.X(0.98), p.Y(0.56), p.X(1.5), p.Y(0.4), p.X(1.5), p.Y(1)], C.rgb([255, 244, 180])]); }
  else { p.rect(0.93, 0.52, 0.06, 0.1, C.mat('cream')[4]); p.rect(0.01, 0.52, 0.04, 0.1, C.col('red')[1]); }
});
reg('boat', 'barco', 'vehicle', 1.05, 0.34, (p, k) => {
  const C = k.C, w = C.mat('wood'), s = C.tint(k.a.color, 'white'), f = C.tint(k.a.color2, 'red');
  p.rect(0.48, 0.04, 0.04, 0.74, w[0]);
  p.poly([[0.54, 0.1], [0.94, 0.66], [0.54, 0.66]], s[3]); p.poly([[0.54, 0.4], [0.94, 0.66], [0.54, 0.66]], s[2]); p.poly([[0.46, 0.2], [0.46, 0.66], [0.1, 0.66]], s[2]); p.poly([[0.46, 0.44], [0.46, 0.66], [0.1, 0.66]], s[1]);
  p.poly([[0.5, 0.04], [0.66, 0.08], [0.5, 0.12]], f[2]);
  p.poly([[0.02, 0.72], [0.98, 0.72], [0.84, 1], [0.16, 1]], w[2]); p.rect(0.02, 0.72, 0.96, 0.06, w[3]); p.poly([[0.1, 0.88], [0.9, 0.88], [0.84, 1], [0.16, 1]], w[1]);
}, { float: 1 });
reg('rocket', 'cohete', 'vehicle', 0.5, 0.44, (p, k) => {
  const C = k.C, w = C.mat('white'), r = C.tint(k.a.color, 'red'), g = C.mat('glass'), L = k.lit;
  p.poly([[0.3, 0.6], [0.02, 0.84], [0.02, 0.92], [0.3, 0.8]], r[2]); p.poly([[0.7, 0.6], [0.98, 0.84], [0.98, 0.92], [0.7, 0.8]], r[1]);
  p.ell(0.5, 0.3, 0.22, 0.3, r[2]); p.rect(0.28, 0.3, 0.44, 0.52, w[3]); p.rect(0.28, 0.3, 0.12, 0.52, w[4]); p.rect(0.6, 0.3, 0.12, 0.52, w[1]); p.rect(0.28, 0.72, 0.44, 0.05, r[2]);
  p.ell(0.5, 0.44, 0.1, 0.06, C.mat('metal')[1]); p.ell(0.5, 0.44, 0.07, 0.04, g[3]); p.rect(0.38, 0.82, 0.24, 0.05, C.mat('darkstone')[2]);
  if (k.a.fly !== false) { L.poly([[0.36, 0.87], [0.64, 0.87], [0.58, 1.0], [0.5, 1.16], [0.42, 1.0]], C.rgb(FIRE1)); L.poly([[0.43, 0.87], [0.57, 0.87], [0.5, 1.04]], C.rgb(FIRE2)); }
}, { air: 1 });
reg('ufo', 'ovni', 'vehicle', 2.1, 0.15, (p, k) => {
  const C = k.C, m = C.tint(k.a.color, 'silver'), g = C.hsl(170, 60, 60), L = k.lit;
  p.ell(0.5, 0.36, 0.22, 0.32, g[2]); p.ell(0.44, 0.26, 0.1, 0.12, g[4], 1); p.ell(0.5, 0.62, 0.5, 0.24, m[2]); p.ell(0.5, 0.56, 0.42, 0.12, m[3], 1); p.ell(0.5, 0.74, 0.36, 0.1, m[1], 1);
  for (const [u, i] of [[0.16, 0], [0.33, 1], [0.5, 2], [0.67, 3], [0.84, 4]]) L.px(u, 0.64, C.rgb(i & 1 ? [255, 90, 200] : [120, 255, 200]));
  if (k.a.beam !== false && !k.space) k.glow.push(['P', [p.X(0.34), p.Y(0.84), p.X(0.66), p.Y(0.84), p.X(0.95), p.Y(0.84) + Math.round(p.h * 4), p.X(0.05), p.Y(0.84) + Math.round(p.h * 4)], C.rgb([150, 255, 210])]);
}, { air: 1 });
reg('plane', 'avión', 'vehicle', 2.5, 0.13, (p, k) => {
  const C = k.C, w = C.tint(k.a.color, 'white'), a = C.tint(k.a.color2, 'red');
  p.poly([[0.1, 0.5], [0.02, 0.04], [0.14, 0.04], [0.26, 0.42]], a[2]); p.ell(0.5, 0.56, 0.46, 0.2, w[3]); p.ell(0.5, 0.66, 0.42, 0.1, w[1], 1); p.rect(0.86, 0.46, 0.08, 0.1, C.mat('glass')[1], 1);
  for (let u = 0.3; u < 0.8; u += 0.09) p.px(u, 0.5, C.mat('glass')[1]);
  p.poly([[0.4, 0.6], [0.26, 1], [0.36, 1], [0.6, 0.62]], w[2]);
}, { air: 1 });
reg('balloon', 'globo aerostático', 'vehicle', 0.72, 0.36, (p, k) => {
  const C = k.C, c = C.tint(k.a.color, 'red'), w = C.tint(k.a.color2, 'yellow'), b = C.mat('wood');
  p.ell(0.5, 0.36, 0.48, 0.35, c[2]); p.poly([[0.16, 0.56], [0.84, 0.56], [0.6, 0.76], [0.4, 0.76]], c[2]);
  for (const u of [0.2, 0.52]) p.rect(u, 0, 0.14, 0.78, w[2], 1);
  p.shade(0.66, 0, 0.4, 0.8, -1); p.ell(0.32, 0.2, 0.12, 0.1, c[4], 1);
  p.line(0.42, 0.76, 0.42, 0.88, C.dark); p.line(0.58, 0.76, 0.58, 0.88, C.dark); p.rect(0.38, 0.88, 0.24, 0.12, b[2]); p.rect(0.38, 0.88, 0.24, 0.03, b[3]);
  if (k.night) k.lit.rect(0.47, 0.78, 0.06, 0.06, C.rgb(FIRE2));
}, { air: 1 });

// ---------------- Objetos ----------------
const obj = (id, name, build, ar = 1) => reg(id, name, 'object', ar, 0.16, build);
obj('sword', 'espada', (p, k) => sword(p, k, 0.5, 0.8, 0.68, 0.2), 0.5);
obj('shield', 'escudo', (p, k) => { const C = k.C, s = C.tint(k.a.color, 'blue'), g = C.mat('gold'); p.poly([[0.1, 0.06], [0.9, 0.06], [0.9, 0.56], [0.5, 0.98], [0.1, 0.56]], g[2]); p.poly([[0.18, 0.14], [0.82, 0.14], [0.82, 0.53], [0.5, 0.87], [0.18, 0.53]], s[2]); p.poly([[0.18, 0.14], [0.5, 0.14], [0.5, 0.87], [0.18, 0.53]], s[3]); p.rect(0.46, 0.14, 0.08, 0.7, g[3], 1); p.rect(0.18, 0.36, 0.64, 0.08, g[3], 1); });
obj('potion', 'poción', (p, k) => { const C = k.C, c = C.tint(k.a.color, 'red'), g = C.mat('glass'); p.rect(0.4, 0.1, 0.2, 0.26, g[3]); p.rect(0.36, 0.04, 0.28, 0.1, C.mat('wood')[2]); p.ell(0.5, 0.66, 0.36, 0.32, g[3]); p.ell(0.5, 0.7, 0.3, 0.26, c[2]); p.rect(0, 0.3, 1, 0.26, g[3], 1); p.ell(0.5, 0.56, 0.3, 0.04, c[3], 1); p.ell(0.36, 0.72, 0.08, 0.1, c[4], 1); k.lit.px(0.62, 0.62, C.rgb([255, 255, 255])); }, 0.8);
obj('heart', 'corazón', (p, k) => { const c = k.C.tint(k.a.color, 'red'); p.ell(0.28, 0.32, 0.26, 0.26, c[2]); p.ell(0.72, 0.32, 0.26, 0.26, c[2]); p.poly([[0.03, 0.42], [0.97, 0.42], [0.5, 0.98]], c[2]); p.ell(0.24, 0.26, 0.1, 0.1, c[4], 1); p.shade(0.6, 0.3, 0.4, 0.7, -1); });
obj('gem', 'gema', (p, k) => { const c = k.C.tint(k.a.color, 'cyan'); p.poly([[0.24, 0.08], [0.76, 0.08], [1, 0.36], [0.5, 0.98], [0, 0.36]], c[2]); p.poly([[0.24, 0.08], [0.76, 0.08], [0.66, 0.36], [0.34, 0.36]], c[4]); p.poly([[0, 0.36], [0.34, 0.36], [0.5, 0.98]], c[3]); p.poly([[0.66, 0.36], [1, 0.36], [0.5, 0.98]], c[1]); p.poly([[0.76, 0.08], [1, 0.36], [0.66, 0.36]], c[2]); });
obj('key', 'llave', (p, k) => { const g = k.C.tint(k.a.color, 'gold'); p.ell(0.26, 0.3, 0.22, 0.22, g[2]); p.ell(0.26, 0.3, 0.09, 0.09, -1); p.line(0.4, 0.44, 0.9, 0.94, g[2]); p.line(0.44, 0.42, 0.94, 0.92, g[3]); p.line(0.72, 0.76, 0.82, 0.66, g[2]); p.line(0.84, 0.88, 0.94, 0.78, g[2]); });
obj('crown', 'corona', (p, k) => { const C = k.C, g = C.tint(k.a.color, 'gold'); p.poly([[0.06, 0.9], [0.02, 0.24], [0.28, 0.52], [0.5, 0.1], [0.72, 0.52], [0.98, 0.24], [0.94, 0.9]], g[2]); p.rect(0.06, 0.74, 0.88, 0.16, g[1]); p.rect(0.06, 0.74, 0.88, 0.04, g[4]); p.px(0.5, 0.8, C.col('red')[3]); p.px(0.24, 0.8, C.col('blue')[3]); p.px(0.76, 0.8, C.col('green')[3]); p.shade(0.6, 0, 0.4, 1, -1); }, 1.2);
obj('coin', 'moneda', (p, k) => { const g = k.C.tint(k.a.color, 'gold'); p.ell(0.5, 0.5, 0.44, 0.44, g[1]); p.ell(0.48, 0.48, 0.38, 0.38, g[3]); p.ell(0.5, 0.5, 0.3, 0.3, g[2]); p.rect(0.44, 0.3, 0.12, 0.4, g[4]); });
obj('apple', 'manzana', (p, k) => { const C = k.C, c = C.tint(k.a.color, 'red'); p.ell(0.36, 0.6, 0.3, 0.34, c[2]); p.ell(0.64, 0.6, 0.3, 0.34, c[2]); p.shade(0.62, 0.2, 0.4, 0.8, -1); p.ell(0.28, 0.46, 0.08, 0.1, c[4], 1); p.line(0.5, 0.3, 0.56, 0.08, C.mat('bark')[1]); p.poly([[0.56, 0.16], [0.8, 0.06], [0.7, 0.24]], C.mat('leaf')[3]); });
obj('mug', 'taza', (p, k) => { const C = k.C, c = C.tint(k.a.color, 'white'); p.rect(0.12, 0.3, 0.58, 0.64, c[3]); p.rect(0.56, 0.3, 0.14, 0.64, c[1]); p.rect(0.12, 0.3, 0.58, 0.08, C.mat('wood')[0]); p.rect(0.7, 0.42, 0.2, 0.08, c[2]); p.rect(0.84, 0.42, 0.08, 0.34, c[2]); p.rect(0.7, 0.7, 0.2, 0.08, c[2]); const s = C.rgb([230, 230, 240]); p.line(0.3, 0.22, 0.34, 0.06, s); p.line(0.5, 0.22, 0.46, 0.04, s); });
obj('chest', 'cofre', (p, k) => { const C = k.C, w = C.tint(k.a.color, 'wood'), g = C.mat('gold'); p.rect(0.04, 0.4, 0.92, 0.6, w[2]); p.ell(0.5, 0.42, 0.46, 0.3, w[3]); p.rect(0.04, 0.42, 0.92, 0.58, w[2]); p.rect(0.7, 0.42, 0.26, 0.58, w[1]); p.rect(0.04, 0.5, 0.92, 0.07, g[2]); for (const u of [0.1, 0.82]) p.rect(u, 0.14, 0.08, 0.86, g[2], 1); p.rect(0.44, 0.48, 0.12, 0.16, g[3]); p.px(0.49, 0.56, C.dark); }, 1.2);
obj('skull', 'calavera', (p, k) => { const C = k.C, b = C.mat('bone'), d = C.dark; p.ell(0.5, 0.42, 0.42, 0.38, b[3]); p.rect(0.26, 0.66, 0.48, 0.26, b[2]); p.ell(0.32, 0.46, 0.12, 0.13, d); p.ell(0.68, 0.46, 0.12, 0.13, d); p.poly([[0.5, 0.56], [0.44, 0.68], [0.56, 0.68]], d); for (const u of [0.36, 0.5, 0.64]) p.rect(u, 0.8, 0.02, 0.12, d); p.shade(0.66, 0, 0.4, 1, -1); });
obj('starobj', 'estrella', (p, k) => { const g = k.C.tint(k.a.color, 'yellow'); p.poly([[0.5, 0.02], [0.62, 0.36], [0.98, 0.38], [0.7, 0.6], [0.8, 0.96], [0.5, 0.76], [0.2, 0.96], [0.3, 0.6], [0.02, 0.38], [0.38, 0.36]], g[2]); p.poly([[0.5, 0.02], [0.5, 0.76], [0.2, 0.96], [0.3, 0.6], [0.02, 0.38], [0.38, 0.36]], g[3]); });

export const CATEGORY_NAMES = { humanoid: 'Personajes', animal: 'Animales', creature: 'Criaturas', structure: 'Edificios', flora: 'Naturaleza y atrezo', vehicle: 'Vehículos', object: 'Objetos' };
