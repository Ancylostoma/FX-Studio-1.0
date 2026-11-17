// Codificador GIF89a animado, sin dependencias. El pixel art encaja de forma
// natural en GIF: pocos colores, bordes duros y un índice de transparencia.
// Entrada: fotogramas RGBA del mismo tamaño. Salida: Uint8Array.

// Paleta global de todos los fotogramas (máx. 255 colores + 1 transparente).
// Si hubiera más, se van descartando bits de precisión hasta que quepan.
export function buildPalette(frames) {
  for (let drop = 0; drop < 8; drop++) {
    const mask = (0xff << drop) & 0xff, map = new Map();
    let ok = true;
    for (const f of frames) {
      const d = f.data;
      for (let i = 0; i < d.length && ok; i += 4) {
        if (d[i + 3] < 128) continue;
        const key = ((d[i] & mask) << 16) | ((d[i + 1] & mask) << 8) | (d[i + 2] & mask);
        if (!map.has(key)) { if (map.size >= 255) ok = false; else map.set(key, map.size + 1); }
      }
      if (!ok) break;
    }
    if (ok) return { map, mask, colors: [...map.keys()] };
  }
  throw new Error('No se pudo reducir la paleta');
}

// LZW de longitud de código variable, empaquetado en sub-bloques de 255 bytes.
function lzw(minCode, pixels, out) {
  const clear = 1 << minCode, eoi = clear + 1;
  let size = minCode + 1, next = eoi + 1, dict = new Map();
  let acc = 0, bits = 0, block = [];
  const flushBlock = () => { if (block.length) { out.push(block.length, ...block); block = []; } };
  const emit = (code) => {
    acc |= code << bits; bits += size;
    while (bits >= 8) { block.push(acc & 255); acc >>>= 8; bits -= 8; if (block.length === 255) flushBlock(); }
  };
  out.push(minCode);
  emit(clear);
  let cur = pixels[0];
  for (let i = 1; i < pixels.length; i++) {
    const k = pixels[i], key = (cur << 8) | k;
    const hit = dict.get(key);
    if (hit !== undefined) { cur = hit; continue; }
    emit(cur);
    if (next === 4096) { emit(clear); dict = new Map(); size = minCode + 1; next = eoi + 1; }
    else { if (next >= 1 << size) size++; dict.set(key, next++); }
    cur = k;
  }
  emit(cur); emit(eoi);
  if (bits > 0) { block.push(acc & 255); if (block.length === 255) flushBlock(); }
  flushBlock();
  out.push(0);
}

export function encodeGIF(frames, { fps = 8, loop = true } = {}) {
  if (!frames.length) throw new Error('Sin fotogramas');
  const { w, h } = frames[0];
  if (w > 65535 || h > 65535 || frames.some((f) => f.w !== w || f.h !== h)) throw new Error('Tamaño de fotograma inválido');
  const { map, mask, colors } = buildPalette(frames);
  let bitsPal = 1;
  while (1 << bitsPal < colors.length + 1) bitsPal++;
  const out = [];
  const u16 = (v) => out.push(v & 255, (v >> 8) & 255);
  out.push(...[...'GIF89a'].map((c) => c.charCodeAt(0)));
  u16(w); u16(h);
  out.push(0x80 | 0x70 | (bitsPal - 1), 0, 0);       // paleta global; índice 0 = transparente
  const table = new Uint8Array(3 << bitsPal);
  colors.forEach((c, i) => { table[(i + 1) * 3] = c >> 16; table[(i + 1) * 3 + 1] = (c >> 8) & 255; table[(i + 1) * 3 + 2] = c & 255; });
  out.push(...table);
  if (loop && frames.length > 1) out.push(0x21, 0xff, 11, ...[...'NETSCAPE2.0'].map((c) => c.charCodeAt(0)), 3, 1, 0, 0, 0);
  const delay = Math.max(2, Math.round(100 / Math.max(1, fps)));
  for (const f of frames) {
    const px = new Uint8Array(w * h);
    let transparent = false;
    for (let i = 0, p = 0; p < px.length; i += 4, p++) {
      const d = f.data;
      if (d[i + 3] < 128) { transparent = true; continue; }
      px[p] = map.get(((d[i] & mask) << 16) | ((d[i + 1] & mask) << 8) | (d[i + 2] & mask));
    }
    // Control gráfico: restaurar al fondo (2) para que la transparencia no acumule fotogramas.
    out.push(0x21, 0xf9, 4, (2 << 2) | (transparent ? 1 : 0)); u16(delay); out.push(0, 0);
    out.push(0x2c); u16(0); u16(0); u16(w); u16(h); out.push(0);
    lzw(Math.max(2, bitsPal), px, out);
  }
  out.push(0x3b);
  return Uint8Array.from(out);
}
