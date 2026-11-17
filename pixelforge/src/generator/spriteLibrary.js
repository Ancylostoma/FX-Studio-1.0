// Punto de entrada de la biblioteca: sprites base + kits y sprites ampliados.
// Importa siempre desde aquí para obtener el catálogo completo.
import { LIB, CATEGORY_NAMES } from './spriteBase.js';
import './kits.js';
import './props.js';

export { LIB, CATEGORY_NAMES };
export { defOf, kitDef, KIT_PRESETS, KIT_BASES } from './kits.js';
