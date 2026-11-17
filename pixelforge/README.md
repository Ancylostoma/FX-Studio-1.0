# PixelForge Studio

Generador de Pixel Art **por código**. Escribes una descripción («un dragón rojo volando sobre un castillo durante la noche»), la aplicación la convierte en una estructura compacta de datos (**PixelScene v1**: coordenadas, capas, índices de paleta) y un motor propio en JavaScript la dibuja en un Canvas. No hay filtros de pixelado ni modelos generativos de imágenes.

| | | |
|---|---|---|
| ![Dragón sobre un castillo](docs/examples/dragon-castillo.png) | ![Paisaje japonés](docs/examples/paisaje-japones.png) | ![Ciudad cyberpunk](docs/examples/cyberpunk.png) |
| ![Bosque encantado](docs/examples/bosque-encantado.png) | ![Astronauta en Marte](docs/examples/astronauta-marte.png) | ![Isla isométrica](docs/examples/isla-isometrica.png) |

Todas las imágenes de `docs/examples/` las genera el modo offline (`npm run examples`). La escena del dragón a 128×128 ocupa **5,7 KB** de JSON.

## Cómo iniciarla

Requiere Node 20 o superior. El modo offline no necesita instalar nada:

```bash
cd pixelforge
```

```bash
npm start
```

Abre <http://localhost:8787>. Los módulos ES necesitan servirse por HTTP (no funciona abriendo `index.html` con `file://`). Cualquier servidor estático sirve para el modo offline; `npm start` añade además el backend del modo IA.

Es una PWA: desde Chrome (escritorio o Android) se puede instalar con «Instalar aplicación» y, tras la primera carga, funciona sin conexión.

### Modo IA (opcional)

```bash
npm install
```

```bash
cp .env.example .env
```

Escribe tu clave en `ANTHROPIC_API_KEY` dentro de `.env` y reinicia `npm start`. El selector **Modo → IA (Claude)** se habilita solo cuando el servidor detecta la clave. Sin clave la aplicación sigue funcionando completa en offline.

- La clave vive únicamente en el servidor (`server/`); el navegador solo habla con `/api/pixelscene`.
- El modelo (por defecto `claude-opus-5-5`, configurable con `PIXELFORGE_MODEL`) **no genera imágenes**: devuelve un JSON PixelScene que el motor local dibuja.
- La respuesta se sanea y valida en el servidor y otra vez en el cliente; si no es válida se hace un único reintento con los errores.
- El endpoint solo acepta peticiones del mismo origen y está limitado a 12 peticiones por minuto e IP.

> El modo IA está implementado y sus partes deterministas (saneado, validación, petición) tienen pruebas, pero **no se ha ejecutado contra la API real** en el entorno donde se construyó, porque no había clave disponible.

## Uso

1. **Describe la imagen** y pulsa **GENERAR** (Ctrl+Intro). Debajo aparece la *interpretación*: lo que el motor entendió, en texto y en forma compacta (`SUBJECT=dragon(red);SCENE=castle;LIGHT=night…`), y las palabras que no reconoció.
2. **VARIAR** muestra cuatro variaciones con otras semillas; **REGENERAR** repite con una semilla nueva. Con «fijar» activado, GENERAR reutiliza la semilla: la misma semilla produce siempre la misma imagen.
3. **Modificar la escena** acepta instrucciones como «conserva el castillo, cambia el cielo a púrpura y agrega una luna». Solo se regeneran las partes afectadas; el resto de capas —incluidas las que hayas editado a mano— se conserva.
4. **Panel de edición**: capas (mostrar, reordenar, opacidad, contorno, sombreado, duplicar, eliminar), paleta (cambiar un color actualiza toda la imagen porque todo se referencia por índice), luz (dirección, ambiente, dithering), exportación y el JSON editable.
5. **Lápiz, borrador y cuentagotas** para retoques píxel a píxel; se guardan en una capa propia.
6. **Guardar / Galería**: proyectos en IndexedDB, en tu dispositivo. Se pueden abrir, duplicar, renombrar, eliminar y exportar.

### Exportación

- PNG a tamaño original o ampliado con factor entero (hasta 4096 px), siempre por vecino más cercano.
- PNG con fondo transparente (sin color de fondo ni capa de cielo).
- Hoja de sprites (sujetos, estructuras y sprites reutilizables) con su índice JSON; PNG de una capa suelta desde el panel de capas.
- `*.pixelscene.json` (la escena) y `*.pfproj.json` (proyecto completo: escena, prompt, opciones y especificación). Ambos se pueden volver a importar.

## PixelScene v1

```json
{"v":1,"w":64,"h":64,"pal":[[12,18,34],[35,48,80],[230,190,100]],"bg":0,
 "layers":[{"n":"sky","o":[["R",0,0,64,64,0],["E",42,12,8,8,2]]},
           {"n":"castle","o":[["R",12,30,40,22,1]]}]}
```

| Campo | Significado |
|---|---|
| `w`, `h` | Tamaño lógico, 8–256 px |
| `pal` | Paleta RGB (máx. 96). Todo color es un índice |
| `bg` | Índice de fondo, o `-1` transparente |
| `rmp` | Rampas de índices (oscuro → claro) para el sombreado |
| `q` | Paleta de cuantización final (opcional) |
| `fx` | `ld` dirección de la luz, `amb` luz ambiental `[r,g,b,%]`, `dth` dithering 0–3 |
| `sprites` | Sprites reutilizables `{id:{w,h,o}}` |
| `layers` | Capas en orden de dibujo: `n` nombre, `k` parte, `v` visible, `a` opacidad, `sh` sombreado de borde, `ol` contorno (`-2` selectivo), `nl` emisiva, `f` bruma, `rf` reflejo, `o` operaciones |

| Op | Parámetros | |
|---|---|---|
| `R` | `x,y,w,h,c` | rectángulo |
| `L` | `x1,y1,x2,y2,c` | línea |
| `E` | `cx,cy,rx,ry,c` | elipse rellena |
| `P` | `[x1,y1,…],c` | polígono relleno |
| `M` | `x,y,filas` | matriz de píxeles (`0-9a-zA-Z` = índices, `.` = vacío) |
| `S` | `id,x,y,flip` | instancia de sprite |
| `G` | `x,y,w,h,[c…],dir` | degradado tramado |
| `D` | `x,y,w,h,c,nivel` | trama de Bayer |
| `T` | `x,y,w,h,c,dens,seed` | textura de motas determinista |
| `B` | `x,y,w,h,id` | sprite en mosaico |
| `H` | `x,y,w,h,delta` | desplaza los píxeles por su rampa |

El último parámetro opcional de las operaciones de dibujo es el *modo* (1 = solo sobre píxeles opacos, 2 = solo sobre vacíos); `c = -1` borra. La definición formal y los límites están en `src/core/pixelScene.js`.

La validación comprueba JSON, dimensiones, cada índice de color, rangos de coordenadas, operaciones permitidas, estructura de capas y límites globales (8000 operaciones, 64 capas, 2 MB). No se usa `eval` ni `innerHTML` en ningún punto.

## Arquitectura

```
src/core        Motor: formato y validación, renderizador a búfer de índices,
                geometría entera, dithering de Bayer, luz por paleta, semillas
src/generator   Modo offline: vocabulario, parser, biblioteca de sprites
                procedurales, generadores de entorno, compositor por partes
src/editor      Operaciones puras de capas, color, historial y retoque
src/export      PNG (canvas.toBlob), hojas de sprites, proyectos
src/storage     IndexedDB (con respaldo en memoria)
src/ai          Especificación para el modelo y cliente del backend
src/ui          Interfaz
server          Servidor estático + adaptador de IA
tests           Pruebas unitarias (node:test) y de navegador (tests/e2e)
tools           Render a PNG desde Node, ejemplos e iconos
```

Decisiones que se apartan de la estructura propuesta, y por qué:

- **Sin Web Workers.** Renderizar 256×256 tarda unos milisegundos; un worker añadiría complejidad sin mejora medible.
- **`vocabulary.js` y `src/ai/` añadidos.** El diccionario se separa del parser para ampliarlo sin tocar lógica; la especificación del modelo la comparten cliente y servidor.
- **El núcleo no toca el DOM.** `renderer.js` devuelve RGBA puro, lo que permite probarlo en Node y generar los ejemplos sin navegador.
- **La luz y la paleta restringida se aplican al renderizar**, sobre la paleta. Cambiar de día a noche o a 4 verdes no modifica ninguna capa.

## Límites del modo offline

El motor offline **no entiende lenguaje libre**: reconoce un vocabulario cerrado en español e inglés (79 sprites procedurales, 13 entornos, relieve, hora, clima, astros, 14 colores) y lo compone con reglas. Lo que no reconoce lo ignora y lo dice; no finge haberlo dibujado. La lista completa está en la aplicación, en «¿Qué entiende el modo offline?».

- Las relaciones espaciales («detrás de», «a la izquierda») no se interpretan; la colocación sigue reglas fijas de composición.
- Los sprites son procedurales y estilizados: sirven como base, no sustituyen a un artista.
- El estilo isométrico usa una isla en perspectiva con los objetos como figuras frontales, no modelos isométricos reales.
- El borrador solo elimina retoques hechos con el lápiz, no píxeles de las capas generadas (para eso, oculta o elimina la capa).
- A 16×16 y 32×32 las escenas completas pierden legibilidad; a esos tamaños funciona mejor el estilo «Personajes y sprites».

Para ampliar el vocabulario: añade sinónimos en `src/generator/vocabulary.js`; para un objeto nuevo, registra su constructor en `src/generator/spriteLibrary.js`.

## Pruebas

```bash
npm test
```

51 pruebas unitarias: parser, validación de PixelScene, geometría, renderizador, generador (todas las combinaciones de estilo, resolución y bioma, y los 79 sprites), edición parcial, exportación y adaptador de IA.

```bash
npm run test:e2e
```

Prueba en navegador real con Playwright (debe estar instalado): arranque, generación, nitidez del escalado, semillas, variaciones, edición por instrucción, capas, paleta, luz, lápiz, exportación PNG/JSON, galería con IndexedDB y vista móvil.
