// Comparación palabra a palabra (LCS) entre original y reformulado.

const TOKEN = /\s+|[\p{L}\p{N}][\p{L}\p{N}'’\-]*|[^\s\p{L}\p{N}]/gu;
export const tokenize = (s) => s.match(TOKEN) || [];

function lcsOps(a, b) {
  const n = a.length, m = b.length;
  if (n * m > 4_000_000) return [{ type: 'delete', text: a.join('') }, { type: 'insert', text: b.join('') }];
  const dp = new Uint32Array((n + 1) * (m + 1));
  const W = m + 1;
  for (let i = n - 1; i >= 0; i--) for (let j = m - 1; j >= 0; j--)
    dp[i * W + j] = a[i] === b[j] ? dp[(i + 1) * W + j + 1] + 1 : Math.max(dp[(i + 1) * W + j], dp[i * W + j + 1]);
  const ops = [];
  let i = 0, j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) { ops.push({ type: 'equal', text: a[i] }); i++; j++; }
    else if (dp[(i + 1) * W + j] >= dp[i * W + j + 1]) ops.push({ type: 'delete', text: a[i++] });
    else ops.push({ type: 'insert', text: b[j++] });
  }
  while (i < n) ops.push({ type: 'delete', text: a[i++] });
  while (j < m) ops.push({ type: 'insert', text: b[j++] });
  return ops;
}

/** Devuelve operaciones fusionadas [{type:'equal'|'insert'|'delete', text}]. */
export function diffWords(original, result) {
  const a = tokenize(original), b = tokenize(result);
  let p = 0;
  while (p < a.length && p < b.length && a[p] === b[p]) p++;
  let s = 0;
  while (s < a.length - p && s < b.length - p && a[a.length - 1 - s] === b[b.length - 1 - s]) s++;
  const mid = lcsOps(a.slice(p, a.length - s), b.slice(p, b.length - s));
  const ops = [...a.slice(0, p).map((t) => ({ type: 'equal', text: t })), ...mid, ...a.slice(a.length - s).map((t) => ({ type: 'equal', text: t }))];
  // Un espacio "igual" entre dos cambios se absorbe para que el resaltado sea legible.
  const merged = [];
  for (const op of ops) {
    const last = merged[merged.length - 1];
    if (last && last.type === op.type) last.text += op.text;
    else merged.push({ ...op });
  }
  return merged;
}

export function diffStats(ops) {
  const words = (t) => (t.match(/[\p{L}\p{N}]+/gu) || []).length;
  let eq = 0, del = 0, ins = 0;
  for (const o of ops) { const w = words(o.text); if (o.type === 'equal') eq += w; else if (o.type === 'delete') del += w; else ins += w; }
  const orig = eq + del;
  return { originalWords: orig, resultWords: eq + ins, unchangedWords: eq, removedWords: del, addedWords: ins, changedPct: orig ? Math.round((del / orig) * 100) : 0 };
}
