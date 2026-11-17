// Envoltorio mínimo de IndexedDB. Se guardan los DATOS de la escena (JSON
// compacto), nunca imágenes en Base64: las miniaturas se renderizan al vuelo.
const DB_NAME = 'pixelforge', DB_VERSION = 1;
export const STORES = { projects: 'projects', kv: 'kv' };

// Respaldo en memoria si IndexedDB no está disponible (modo privado, etc.).
const memory = { projects: new Map(), kv: new Map() };
let dbPromise = null;

function open() {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve) => {
    if (typeof indexedDB === 'undefined') return resolve(null);
    let req;
    try { req = indexedDB.open(DB_NAME, DB_VERSION); } catch { return resolve(null); }
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORES.projects)) db.createObjectStore(STORES.projects, { keyPath: 'id' });
      if (!db.objectStoreNames.contains(STORES.kv)) db.createObjectStore(STORES.kv);
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => resolve(null);
    req.onblocked = () => resolve(null);
  });
  return dbPromise;
}

async function run(store, mode, idbFn, memFn) {
  const db = await open();
  if (!db) return memFn(memory[store]);
  return new Promise((resolve, reject) => {
    const t = db.transaction(store, mode);
    const req = idbFn(t.objectStore(store));
    t.oncomplete = () => resolve(req.result);
    t.onerror = t.onabort = () => reject(t.error || new Error('Error de almacenamiento'));
  });
}

const clone = (v) => (v === undefined ? undefined : JSON.parse(JSON.stringify(v)));

export const isPersistent = async () => !!(await open());
export const put = (store, value, key) => run(store, 'readwrite', (s) => (key === undefined ? s.put(value) : s.put(value, key)), (m) => { m.set(key ?? value.id, clone(value)); });
export const get = (store, key) => run(store, 'readonly', (s) => s.get(key), (m) => clone(m.get(key)));
export const getAll = (store) => run(store, 'readonly', (s) => s.getAll(), (m) => [...m.values()].map(clone));
export const del = (store, key) => run(store, 'readwrite', (s) => s.delete(key), (m) => { m.delete(key); });
