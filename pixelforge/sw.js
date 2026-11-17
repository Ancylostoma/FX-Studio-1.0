// Service worker: guarda el "cascarón" de la aplicación para que el modo
// offline funcione sin red. Las llamadas a /api/ nunca se cachean.
const CACHE = 'pixelforge-v4';
const SHELL = [
  './', 'index.html', 'styles.css', 'manifest.json', 'icons/icon-192.png', 'icons/icon-512.png',
  ...['core/dithering', 'core/geometry', 'core/lighting', 'core/paletteManager', 'core/pixelEngine', 'core/pixelScene', 'core/randomSeed', 'core/renderer',
    'generator/proceduralGenerator', 'generator/spriteBase', 'generator/kits', 'generator/props', 'generator/environments', 'generator/qualityControl', 'generator/promptParser', 'generator/sceneComposer', 'generator/spriteLibrary', 'generator/templates', 'generator/vocabulary',
    'editor/canvasEditor', 'editor/colorEditor', 'editor/history', 'editor/layerManager', 'export/pngExporter', 'export/gifEncoder', 'export/projectExporter',
    'storage/database', 'storage/projectManager', 'ai/aiClient', 'ai/specSchema', 'ai/planSchema', 'ai/localClient', 'ui/app', 'ui/controls', 'ui/gallery'].map((m) => `src/${m}.js`),
];

self.addEventListener('install', (e) => { e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting())); });
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
// Red primero (para recibir actualizaciones) y caché como respaldo sin conexión.
self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin || url.pathname.includes('/api/')) return;
  e.respondWith(
    fetch(e.request).then((res) => {
      if (res.ok) { const copy = res.clone(); caches.open(CACHE).then((c) => c.put(e.request, copy)); }
      return res;
    }).catch(() => caches.match(e.request).then((hit) => hit || caches.match('index.html'))),
  );
});
