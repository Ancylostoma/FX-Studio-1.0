// Datos del generador offline: materiales, colores, cielos, estilos y biomas.
// Todo es declarativo para poder ampliarlo sin tocar los algoritmos.

// Materiales en HSL (tono base de una rampa de 5 tonos).
export const MATERIALS = {
  stone: [225, 9, 56], darkstone: [240, 10, 34], wood: [24, 45, 38], bark: [20, 36, 28],
  leaf: [112, 44, 36], pine: [155, 42, 27], grass: [96, 48, 42], sand: [40, 56, 68],
  mars: [14, 58, 46], moon: [230, 6, 60], snow: [210, 32, 91], water: [207, 58, 47],
  gold: [44, 85, 55], silver: [215, 14, 72], metal: [215, 12, 56], skin: [24, 55, 70],
  roof: [6, 58, 44], cherry: [338, 72, 77], lava: [18, 95, 52], dirt: [28, 38, 34],
  white: [220, 16, 92], black: [250, 14, 17], cloth: [218, 52, 46], bone: [45, 24, 86],
  glass: [195, 60, 68], asphalt: [235, 12, 24], concrete: [225, 14, 40], cream: [40, 45, 82],
  ash: [260, 10, 24], cloud: [215, 30, 93], pink: [330, 72, 70], brown: [24, 48, 34],
};

// Colores con nombre que el usuario puede pedir en el prompt.
export const COLORS = {
  red: [2, 72, 48], blue: [215, 70, 50], green: [125, 52, 40], gold: [44, 85, 55],
  silver: [215, 14, 72], black: [250, 14, 17], white: [220, 16, 92], purple: [275, 55, 48],
  pink: [330, 72, 68], orange: [24, 88, 54], yellow: [52, 90, 58], brown: [24, 48, 34],
  gray: [220, 8, 52], cyan: [185, 70, 50],
};
export const COLOR_NAMES = {
  red: 'rojo', blue: 'azul', green: 'verde', gold: 'dorado', silver: 'plateado', black: 'negro', white: 'blanco',
  purple: 'púrpura', pink: 'rosa', orange: 'naranja', yellow: 'amarillo', brown: 'marrón', gray: 'gris', cyan: 'cian',
};

// Cielos: degradado (cénit → horizonte), ambiente y densidad de estrellas.
export const SKIES = {
  day: { g: ['2f74c8', '4f97dc', '7fbdea', 'b5dcf2', 'dff0f7'], amb: 'day', stars: 0, cloud: [215, 30, 93] },
  night: { g: ['070a1c', '0d1330', '162047', '233063', '34427a'], amb: 'night', stars: 1, cloud: [228, 30, 38] },
  sunset: { g: ['2a1f55', '5c2d6e', 'a8456f', 'e8744f', 'f7a94a', 'fbd77a'], amb: 'sunset', stars: 0.25, cloud: [12, 75, 68] },
  dawn: { g: ['3b4a8c', '7a6fb0', 'c98bb0', 'f2a99a', 'f9cfa0', 'fdebc4'], amb: 'dawn', stars: 0.1, cloud: [340, 70, 86] },
  storm: { g: ['232a36', '343e4c', '4a5562', '63707b', '808b93'], amb: 'storm', stars: 0, cloud: [220, 12, 36] },
  mars: { g: ['5c3028', '94523a', 'c47a50', 'dfa373', 'ecc79a'], amb: 'dawn', stars: 0, cloud: [20, 40, 78] },
  space: { g: ['030309', '060612', '0a0a1e', '100e2a'], amb: 'day', stars: 1.6, cloud: [260, 30, 40] },
};

// Estilos: cómo se reducen las rampas, contorno, dithering y densidad de detalle.
// ol: 'none' | 'dark' (contorno oscuro fijo) | 'sel' (selectivo por rampa).
export const STYLES = {
  bit16: { name: '16-bit', map: [0, 1, 2, 3, 4], ol: 'sel', sh: 1, dth: 2, detail: 1 },
  bit8: { name: 'Retro 8-bit', map: [0, 0, 2, 4, 4], ol: 'dark', sh: 0, dth: 0, detail: 0.6 },
  rpg: { name: 'RPG', map: [0, 1, 2, 3, 4], ol: 'dark', sh: 1, dth: 1, detail: 1 },
  iso: { name: 'Isométrico', map: [0, 1, 2, 3, 4], ol: 'sel', sh: 1, dth: 1, detail: 1, layout: 'iso' },
  modern: { name: 'Pixel Art moderno', map: [0, 1, 2, 3, 4], ol: 'none', sh: 1, dth: 1, detail: 1.1, sat: 1.08 },
  fantasy: { name: 'Fantasía', map: [0, 1, 2, 3, 4], ol: 'sel', sh: 1, dth: 2, detail: 1.1, hue: -8, sat: 1.15, magic: 1 },
  cyber: { name: 'Cyberpunk', map: [0, 1, 2, 3, 4], ol: 'sel', sh: 1, dth: 2, detail: 1.1, neon: 1 },
  minimal: { name: 'Minimalista', map: [1, 2, 2, 2, 3], ol: 'none', sh: 0, dth: 0, detail: 0.3, flat: 1 },
  detailed: { name: 'Pixel Art detallado', map: [0, 1, 2, 3, 4], ol: 'sel', sh: 1, dth: 3, detail: 1.6 },
  classic: { name: 'Videojuegos clásicos', map: [0, 1, 2, 3, 4], ol: 'dark', sh: 1, dth: 1, detail: 0.9, q: 'pf16' },
  landscape: { name: 'Paisajes', map: [0, 1, 2, 3, 4], ol: 'none', sh: 1, dth: 3, detail: 1.4, vista: 1 },
  sprite: { name: 'Personajes y sprites', map: [0, 1, 2, 3, 4], ol: 'dark', sh: 1, dth: 1, detail: 1, layout: 'sprite' },
};

export const DETAIL_LEVELS = { 1: 'Bajo', 2: 'Medio', 3: 'Alto', 4: 'Máximo' };
export const DETAIL_FACTOR = { 1: 0.5, 2: 1, 3: 1.4, 4: 1.9 };

// Biomas: material del suelo, horizonte relativo, cielo forzado y decorado por defecto.
// flora: [id, peso]; far: relieves lejanos por defecto.
export const BIOMES = {
  meadow: { name: 'pradera', ground: 'grass', hz: 0.62, flora: [['tree', 3], ['bush', 2], ['flower', 4], ['rock', 1]], far: ['hills'] },
  forest: { name: 'bosque', ground: 'grass', hz: 0.6, flora: [['tree', 4], ['pine', 4], ['bush', 2], ['mushroom', 2], ['flower', 1]], far: ['treeline'], dense: 1, groundL: -8 },
  mountains: { name: 'montañas', ground: 'grass', hz: 0.66, flora: [['pine', 4], ['rock', 3], ['bush', 1]], far: ['mountains'] },
  desert: { name: 'desierto', ground: 'sand', hz: 0.6, flora: [['cactus', 3], ['rock', 2], ['deadtree', 1]], far: ['dunes'] },
  mars: { name: 'Marte', ground: 'mars', hz: 0.6, flora: [['rock', 5], ['crater', 2]], far: ['mesas'], sky: 'mars' },
  moonland: { name: 'superficie lunar', ground: 'moon', hz: 0.62, flora: [['rock', 3], ['crater', 4]], far: ['mesas'], sky: 'space' },
  sea: { name: 'mar', ground: 'sand', hz: 0.56, flora: [], far: [], water: 'sea' },
  beach: { name: 'playa', ground: 'sand', hz: 0.54, flora: [['palm', 3], ['rock', 1]], far: [], water: 'beach' },
  city: { name: 'ciudad', ground: 'asphalt', hz: 0.72, flora: [['lamp', 3]], far: ['skyline'], urban: 1 },
  cyber: { name: 'ciudad cyberpunk', ground: 'asphalt', hz: 0.74, flora: [['lamp', 2]], far: ['skyline'], urban: 1, neon: 1, wet: 1 },
  snow: { name: 'paisaje nevado', ground: 'snow', hz: 0.62, flora: [['pine', 5], ['rock', 1], ['deadtree', 1]], far: ['mountains'], snowy: 1 },
  volcano: { name: 'tierra volcánica', ground: 'ash', hz: 0.64, flora: [['rock', 4], ['deadtree', 2]], far: ['volcano'] },
  space: { name: 'espacio', ground: null, hz: 1, flora: [], far: [], sky: 'space', noGround: 1 },
};
