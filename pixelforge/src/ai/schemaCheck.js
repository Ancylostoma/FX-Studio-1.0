// Validador estricto de JSON Schema (subconjunto que usa PixelForge).
// Sirve para comprobar si un modelo RESPETA el esquema pedido, antes de
// cualquier saneado: tipos, enumeraciones, campos obligatorios, campos no
// permitidos, elementos de listas y alternativas (anyOf).
// Devuelve la lista de infracciones con su ruta; vacía = cumple el esquema.
const typeOf = (v) => (v === null ? 'null' : Array.isArray(v) ? 'array' : Number.isInteger(v) ? 'integer' : typeof v);
const matches = (t, v) => { const a = typeOf(v); return t === a || (t === 'number' && a === 'integer'); };

export function checkSchema(schema, value, path = '$', out = []) {
  if (!schema || typeof schema !== 'object') return out;
  if (schema.anyOf) {
    if (!schema.anyOf.some((s) => checkSchema(s, value, path, []).length === 0)) out.push(`${path}: no coincide con ninguna alternativa`);
    return out;
  }
  if (schema.type) {
    const types = Array.isArray(schema.type) ? schema.type : [schema.type];
    if (!types.some((t) => matches(t, value))) { out.push(`${path}: se esperaba ${types.join('|')} y llegó ${typeOf(value)}`); return out; }
  }
  if (schema.enum && !schema.enum.includes(value)) out.push(`${path}: valor fuera de la lista permitida (${JSON.stringify(value).slice(0, 40)})`);
  if (typeOf(value) === 'object') {
    const props = schema.properties || {};
    for (const r of schema.required || []) if (!(r in value)) out.push(`${path}.${r}: falta (obligatorio)`);
    for (const [k, v] of Object.entries(value)) {
      if (props[k]) checkSchema(props[k], v, `${path}.${k}`, out);
      else if (schema.additionalProperties === false) out.push(`${path}.${k}: campo no permitido`);
    }
  }
  if (typeOf(value) === 'array' && schema.items) value.forEach((v, i) => checkSchema(schema.items, v, `${path}[${i}]`, out));
  return out;
}
