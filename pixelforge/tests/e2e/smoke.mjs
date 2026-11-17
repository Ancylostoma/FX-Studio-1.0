// Prueba de humo en navegador real (requiere Playwright instalado):
//   node tests/e2e/smoke.mjs [carpeta-de-capturas]
// Arranca el servidor en un puerto libre y recorre los criterios de aceptación.
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
// require() admite instalaciones globales de Playwright (NODE_PATH).
const { chromium } = createRequire(import.meta.url)('playwright');

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const shots = process.argv[2];
const PORT = 8911;
const server = spawn(process.execPath, ['server/server.mjs'], { cwd: root, env: { ...process.env, PORT: String(PORT), ANTHROPIC_API_KEY: '', ANTHROPIC_AUTH_TOKEN: '' }, stdio: 'pipe' });
await new Promise((res) => server.stdout.on('data', (d) => String(d).includes('http') && res()));

const browser = await chromium.launch();
const errors = [];
let passed = 0;
const step = async (name, fn) => { await fn(); passed++; console.log('  ✓ ' + name); };
try {
  const ctx = await browser.newContext({ viewport: { width: 1360, height: 860 }, acceptDownloads: true });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('dialog', (d) => d.accept(d.type() === 'prompt' ? 'Proyecto de prueba' : undefined));
  const sceneOf = () => page.evaluate(() => window.pixelforge.state.project.scene);

  await step('la aplicación se inicia y genera en modo offline (sin API)', async () => {
    await page.goto(`http://localhost:${PORT}/`);
    await page.waitForFunction(() => window.pixelforge?.state.project?.scene);
    const s = await sceneOf();
    assert.equal(s.v, 1); assert.ok(s.layers.length > 5);
    assert.equal(await page.locator('#aiBadge').textContent(), 'Offline');
    await page.waitForFunction(() => document.querySelector('#optMode option[value=ai]').disabled);
  });

  await step('un prompt reconocido produce una escena e informa de la interpretación', async () => {
    await page.fill('#prompt', 'Un paisaje japonés con montañas, un lago y un árbol de cerezo');
    await page.selectOption('#optRes', '96');
    await page.click('#btnGenerate');
    await page.waitForFunction(() => window.pixelforge.state.project.prompt.includes('japon'));
    assert.match(await page.locator('#interpCode').textContent(), /cherry/);
    const s = await sceneOf();
    assert.equal(s.w, 96);
  });

  await step('el escalado no desenfoca (bloques uniformes, smoothing desactivado)', async () => {
    const r = await page.evaluate(() => {
      const cv = document.querySelector('#view'), s = window.pixelforge.state.project.scene, z = cv.width / s.w;
      const ctx = cv.getContext('2d'), d = ctx.getImageData(0, 0, cv.width, cv.height).data;
      let bad = 0;
      for (let y = 0; y < s.h; y++) for (let x = 0; x < s.w; x++) {
        const a = ((y * z) * cv.width + x * z) * 4, b = ((y * z + z - 1) * cv.width + x * z + z - 1) * 4;
        if (d[a] !== d[b] || d[a + 1] !== d[b + 1] || d[a + 2] !== d[b + 2]) bad++;
      }
      return { z, bad, smooth: ctx.imageSmoothingEnabled, css: getComputedStyle(cv).imageRendering };
    });
    assert.ok(r.z >= 2, 'zoom entero > 1'); assert.equal(r.bad, 0); assert.equal(r.smooth, false); assert.match(r.css, /pixelated|crisp/);
  });

  await step('misma semilla → misma imagen; REGENERAR cambia la semilla', async () => {
    const a = JSON.stringify(await sceneOf());
    await page.check('#optLock'); await page.click('#btnGenerate'); await page.waitForTimeout(150);
    assert.equal(JSON.stringify(await sceneOf()), a);
    await page.click('#btnRegen'); await page.waitForTimeout(150);
    assert.notEqual(JSON.stringify(await sceneOf()), a);
  });

  await step('VARIAR ofrece cuatro variaciones seleccionables', async () => {
    await page.click('#btnVary');
    await page.waitForSelector('#variations canvas');
    assert.equal(await page.locator('#variations canvas').count(), 4);
    const before = JSON.stringify(await sceneOf());
    await page.locator('#variations button').nth(1).click();
    assert.notEqual(JSON.stringify(await sceneOf()), before);
  });
  if (shots) await page.screenshot({ path: path.join(shots, 'escritorio.png') });

  await step('edición por instrucción: cambia el cielo y conserva el resto', async () => {
    const before = await sceneOf();
    await page.fill('#editText', 'cambia el cielo a púrpura y agrega una luna');
    await page.click('#btnEdit'); await page.waitForTimeout(200);
    const after = await sceneOf();
    const key = (s, k) => JSON.stringify(s.layers.filter((l) => l.k === k).map((l) => l.o.length));
    assert.ok(after.layers.some((l) => l.n === 'luna'));
    assert.equal(key(after, 'props'), key(before, 'props'));
    assert.equal(key(after, 'struct:torii'), key(before, 'struct:torii'));
  });

  await step('capas: ocultar, reordenar y deshacer/rehacer', async () => {
    const n0 = (await sceneOf()).layers.length;
    await page.locator('#layerList li').first().locator('.mini').first().click();
    const s1 = await sceneOf();
    assert.equal(s1.layers[n0 - 1].v, 0);
    const second = s1.layers[n0 - 2].n;
    await page.locator('#layerList li').nth(1).locator('.mini', { hasText: '▲' }).click();
    assert.equal((await sceneOf()).layers[n0 - 1].n, second);
    await page.click('#btnUndo'); await page.click('#btnUndo');
    assert.equal((await sceneOf()).layers[n0 - 1].v, undefined);
    await page.click('#btnRedo');
    assert.equal((await sceneOf()).layers[n0 - 1].v, 0);
    await page.click('#btnUndo');
  });

  await step('paleta indexada: cambiar un color actualiza la escena', async () => {
    await page.click('#editTabs [data-tab=palette]');
    await page.locator('#swatches button').nth(2).click();
    await page.evaluate(() => { const i = document.querySelector('#colorInput'); i.value = '#ff00aa'; i.dispatchEvent(new Event('change')); });
    assert.deepEqual((await sceneOf()).pal[2], [255, 0, 170]);
    await page.selectOption('#optPalette2', 'gb4');
    assert.equal((await sceneOf()).q.length, 4);
    await page.selectOption('#optPalette2', 'auto');
  });

  await step('luz y dithering se ajustan sin regenerar', async () => {
    await page.click('#editTabs [data-tab=light]');
    await page.selectOption('#ambPreset', 'night');
    await page.selectOption('#lightDir', '1');
    const s = await sceneOf();
    assert.equal(s.fx.ld, 1); assert.equal(s.fx.amb[3], 78);
  });

  await step('lápiz: los retoques se guardan como capa M indexada', async () => {
    await page.click('[data-tool=pencil]');
    const box = await page.locator('#view').boundingBox();
    await page.mouse.move(box.x + box.width * 0.3, box.y + box.height * 0.3);
    await page.mouse.down(); await page.mouse.move(box.x + box.width * 0.4, box.y + box.height * 0.35); await page.mouse.up();
    const l = (await sceneOf()).layers.at(-1);
    assert.equal(l.k, 'user'); assert.equal(l.o[0][0], 'M');
    await page.click('[data-tool=move]');
  });

  await step('exportación PNG ampliada con dimensiones exactas', async () => {
    await page.click('#editTabs [data-tab=export]');
    const s = await sceneOf();
    const k = +(await page.locator('#expScale').inputValue());
    const [dl] = await Promise.all([page.waitForEvent('download'), page.click('#btnPng')]);
    const buf = readFileSync(await dl.path());
    assert.equal(buf.subarray(1, 4).toString(), 'PNG');
    assert.equal(buf.readUInt32BE(16), s.w * k); assert.equal(buf.readUInt32BE(20), s.h * k);
    assert.ok(k > 1);
  });

  await step('exportación de JSON PixelScene y de proyecto', async () => {
    const [d1] = await Promise.all([page.waitForEvent('download'), page.click('#btnJson')]);
    assert.equal(JSON.parse(readFileSync(await d1.path(), 'utf8')).v, 1);
    const [d2] = await Promise.all([page.waitForEvent('download'), page.click('#btnProject')]);
    assert.equal(JSON.parse(readFileSync(await d2.path(), 'utf8')).format, 'pixelforge-project');
  });

  await step('JSON inválido se rechaza con mensaje', async () => {
    await page.click('#editTabs [data-tab=json]');
    await page.fill('#jsonText', '{"v":1,"w":64,"h":64,"pal":[[0,0,0]],"bg":0,"layers":[{"o":[["R",0,0,5,5,9]]},{"o":[["X",1]]}]}');
    await page.click('#btnJsonApply');
    assert.match(await page.locator('#jsonMsg').textContent(), /inexistente|no permitida/);
  });

  await step('guardar, recargar y recuperar desde la galería (IndexedDB)', async () => {
    await page.click('#btnSave'); await page.waitForTimeout(300);
    const saved = JSON.stringify(await sceneOf());
    await page.waitForTimeout(800);
    await page.reload();
    await page.waitForFunction(() => window.pixelforge?.state.project?.scene);
    assert.equal(JSON.stringify(await sceneOf()), saved, 'el borrador se restaura');
    await page.click('#btnGallery');
    await page.waitForSelector('.gcard canvas');
    assert.match(await page.locator('.gcard .title').first().textContent(), /Proyecto de prueba/);
    if (shots) await page.screenshot({ path: path.join(shots, 'galeria.png') });
    await page.locator('.gcard .btn', { hasText: 'Duplicar' }).first().click();
    await page.waitForFunction(() => document.querySelectorAll('.gcard').length === 2);
    await page.locator('.gcard .btn', { hasText: 'Eliminar' }).first().click();
    await page.waitForFunction(() => document.querySelectorAll('.gcard').length === 1);
    await page.locator('.gcard .btn', { hasText: 'Abrir' }).first().click();
  });

  await step('la interfaz funciona en móvil (390×844)', async () => {
    const m = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
    const mp = await m.newPage();
    mp.on('pageerror', (e) => errors.push('móvil: ' + e.message));
    await mp.goto(`http://localhost:${PORT}/`);
    await mp.waitForFunction(() => window.pixelforge?.state.project?.scene);
    assert.ok(await mp.locator('#panelCreate').isVisible());
    assert.ok(!(await mp.locator('#panelEdit').isVisible()));
    if (shots) await mp.screenshot({ path: path.join(shots, 'movil-crear.png') });
    await mp.fill('#prompt', 'Un astronauta caminando sobre Marte');
    await mp.tap('#btnGenerate');
    await mp.waitForFunction(() => document.querySelector('#app').dataset.view === 'stage');
    assert.ok(await mp.locator('#view').isVisible());
    const over = await mp.evaluate(() => document.documentElement.scrollWidth - innerWidth);
    assert.ok(over <= 0, 'sin desbordamiento horizontal: ' + over);
    await mp.waitForTimeout(200);
    if (shots) await mp.screenshot({ path: path.join(shots, 'movil-lienzo.png') });
    await mp.tap('#mobileTabs [data-view=edit]');
    assert.ok(await mp.locator('#layerList').isVisible());
    if (shots) await mp.screenshot({ path: path.join(shots, 'movil-editar.png') });
    await m.close();
  });

  await step('modo híbrido (backend de IA simulado): interpreta, dibuja en local y edita por capas', async () => {
    const h = await browser.newContext({ viewport: { width: 1360, height: 860 } });
    const hp = await h.newPage();
    hp.on('pageerror', (e) => errors.push('híbrido: ' + e.message));
    const sent = [];
    const ent = (id, extra = {}) => ({ id, color: null, color2: null, n: 1, size: 0, fly: null, a: {}, ...extra });
    const base = { subjects: [ent('lion', { color: 'orange' })], structures: [ent('temple')], flora: [], features: [], biome: 'forest', time: 'sunset', weather: [], celestial: [], fx: [], themes: [], sky: {}, style: null, moonPhase: null, no: [], unknown: ['tigre → león naranja'], interpreted: 'ai' };
    await hp.route('**/api/status', (r) => r.fulfill({ json: { ai: true, model: 'simulado', interpretModel: 'simulado' } }));
    await hp.route('**/api/interpret', (r) => {
      const body = r.request().postDataJSON(); sent.push(body);
      const raw = body.raw ? { ...base, time: 'night', subjects: [ent('lion', { color: 'orange' }), ent('constructor')] } : base;
      r.fulfill({ json: { raw, usage: { input_tokens: 40, output_tokens: 90, cache_read_input_tokens: 1200 }, model: 'simulado' } });
    });
    await hp.goto(`http://localhost:${PORT}/`);
    await hp.waitForFunction(() => !document.querySelector('#optMode option[value=hybrid]').disabled && window.pixelforge?.state.project);
    await hp.selectOption('#optMode', 'hybrid');
    await hp.fill('#prompt', 'Un tigre junto a una catedral en la selva al caer la tarde');
    await hp.click('#btnGenerate');
    await hp.waitForFunction(() => window.pixelforge.state.project.raw?.interpreted === 'ai');
    const sc = () => hp.evaluate(() => window.pixelforge.state.project.scene);
    const s1 = await sc();
    assert.ok(s1.layers.some((l) => l.k === 'subj:lion') && s1.layers.some((l) => l.k === 'struct:temple'));
    assert.deepEqual(sent[0], { prompt: 'Un tigre junto a una catedral en la selva al caer la tarde' });
    assert.match(await hp.locator('#interpText').textContent(), /interpretado por IA \(1240 tokens de entrada, 90 de salida\)/);
    assert.match(await hp.locator('#interpNotes').textContent(), /tigre → león naranja/);
    if (shots) await hp.screenshot({ path: path.join(shots, 'hibrido.png') });
    await hp.fill('#editText', 'que sea de noche'); await hp.click('#btnEdit');
    await hp.waitForFunction(() => window.pixelforge.state.project.raw.time === 'night');
    assert.equal(sent[1].instruction, 'que sea de noche'); assert.equal(sent[1].raw.subjects[0].id, 'lion');
    const s2 = await sc(), sig = (s, k) => JSON.stringify(s.layers.filter((l) => l.k === k).map((l) => l.o));
    assert.equal(sig(s2, 'struct:temple').length, sig(s1, 'struct:temple').length);
    assert.equal(await hp.evaluate(() => window.pixelforge.state.project.raw.subjects.length), 1, 'el cliente descarta ids fuera del catálogo');
    await hp.click('#btnVary'); await hp.waitForSelector('#variations canvas');
    assert.equal(sent.length, 2, 'las variaciones no vuelven a llamar a la IA');
    await h.close();
  });

  assert.deepEqual(errors, [], 'errores de consola');
  console.log(`\n${passed} comprobaciones superadas, sin errores de consola.`);
} catch (e) {
  console.error('\n✗ FALLO:', e.message, '\nErrores de consola:', errors);
  process.exitCode = 1;
} finally {
  await browser.close();
  server.kill();
}
