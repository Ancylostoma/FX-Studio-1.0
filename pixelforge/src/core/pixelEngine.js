// Fachada del motor: valida, renderiza y lleva el resultado a un Canvas
// escalando siempre por vecino más cercano.
import { validateScene } from './pixelScene.js';
import { renderScene } from './renderer.js';

export const MAX_EXPORT = 4096;

export function render(scene, opts) {
  const r = validateScene(scene);
  if (!r.ok) throw new Error('PixelScene inválida: ' + r.errors.join('; '));
  return renderScene(scene, opts);
}

// Factor entero máximo permitido para no superar MAX_EXPORT px por lado.
export function clampScale(scene, scale) {
  const max = Math.max(1, Math.floor(MAX_EXPORT / Math.max(scene.w, scene.h)));
  return Math.max(1, Math.min(max, Math.floor(scale) || 1));
}

export function imageToCanvas(img, canvas, scale = 1) {
  const src = (typeof OffscreenCanvas !== 'undefined') ? new OffscreenCanvas(img.w, img.h) : Object.assign(document.createElement('canvas'), { width: img.w, height: img.h });
  src.getContext('2d').putImageData(new ImageData(img.data, img.w, img.h), 0, 0);
  canvas.width = img.w * scale;
  canvas.height = img.h * scale;
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(src, 0, 0, img.w, img.h, 0, 0, canvas.width, canvas.height);
  return canvas;
}

export function drawScene(scene, canvas, scale = 1, opts) {
  return imageToCanvas(render(scene, opts), canvas, scale);
}
