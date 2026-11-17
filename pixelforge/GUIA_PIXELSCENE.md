# Guía de formatos: PixelScene v1 y plan de escena

PixelForge usa dos formatos de datos:

1. **Plan de escena** (lo que produce el intérprete, procedural o IA): *qué* hay y *dónde*.
2. **PixelScene v1** (lo que produce el compositor y dibuja el motor): *cómo* se dibuja, píxel a píxel.

## 1. Plan de escena (salida del modelo local)

```json
{
  "subjects": [
    {"id": "dragon", "color": "black", "fly": true, "breathesFire": true, "size": 1, "relation": {"type": "on", "to": "castle"}},
    {"id": "custom", "name": "hada", "kit": {"base": "humanoid", "body": "pink", "robe": true, "wings": "fairy", "glow": true}, "size": -1, "fly": true}
  ],
  "structures": [{"id": "castle", "depth": "back"}],
  "props": [{"id": "tree", "side": "left"}],
  "biome": "town", "town": {"style": "medieval", "size": 3},
  "time": "night", "sky": ["moon"], "skyColor": "red",
  "palette": "dark", "view": "side", "light": "right", "focus": "dragon",
  "unsupported": []
}
```

| Campo | Valores |
|---|---|
| `subjects`, `structures`, `props` | Elementos del catálogo (`id`) u objetos a medida (`"id": "custom"` + `name` + `kit`) |
| Colocación | `side` left/center/right · `depth` back/front · `relation` {type: on, behind, front, beside, leftof, rightof; to: id o nombre} |
| Sujetos | `action` idle/walk/attack/cast · `facing` left/right · `fly` · `armor`, `weapon`, `shield`, `cape`, `wings`, `breathesFire` |
| Escena | `setting` outdoor/interior · `room` library/tavern/dungeon/throne/lab/room · `biome` · `town` {style, size} · `features` · `battle` |
| Ambiente | `time` · `weather` · `sky` · `effects` · `skyColor` · `planets` |
| Dirección artística | `focus` (protagonista) · `palette` (tema) · `view` side/front/iso/panorama/top · `light` left/right |

**Kits** (`kit.base`): `humanoid`, `quadruped`, `building`, `flyer`, `shapes`. Los parámetros permitidos están en `src/generator/kits.js` (`KIT_PARAMS`); cualquier otro se descarta al validar.

La definición formal es `PLAN_SCHEMA` en `src/ai/planSchema.js`. El saneado (`sanitizePlan`) comprueba cada id contra el catálogo, recorta valores, descarta relaciones con elementos inexistentes y nunca ejecuta nada de lo recibido.

## 2. PixelScene v1 (formato de dibujo)

```json
{"v":1,"w":64,"h":64,"pal":[[12,18,34],[35,48,80],[230,190,100]],"bg":0,
 "layers":[{"n":"cielo","o":[["R",0,0,64,64,0],["E",42,12,8,8,2]]},
           {"n":"castillo","o":[["R",12,30,40,22,1]]}]}
```

| Campo | Significado |
|---|---|
| `w`, `h` | Tamaño lógico, 8–256 px |
| `pal` | Paleta RGB (máx. 96). Todo color es un índice |
| `bg` | Índice de fondo, o `-1` transparente |
| `rmp` | Rampas de índices (oscuro → claro) para el sombreado |
| `q` | Paleta de cuantización final (opcional) |
| `fx` | `ld` dirección de la luz, `amb` luz ambiental `[r,g,b,%]`, `dth` dithering 0–3 |
| `an` | Animación (opcional): `n` fotogramas (2–12) y `fps` |
| `sprites` | Sprites reutilizables `{id:{w,h,o}}` |
| `layers` | Capas en orden de dibujo: `n` nombre, `k` parte, `v` visible, `a` opacidad, `sh` sombreado, `ol` contorno (`-2` selectivo), `nl` emisiva, `f` bruma, `rf` reflejo, `o` operaciones, `fo` fotogramas siguientes, `mv` desplazamiento por fotograma |

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
| `H` | `x,y,w,h,delta` | desplaza los píxeles por su rampa (luz/sombra) |

El último parámetro opcional de las operaciones de dibujo es el *modo* (1 = solo sobre píxeles opacos, 2 = solo sobre vacíos); `c = -1` borra. La validación (`validateScene` en `src/core/pixelScene.js`) comprueba el JSON, las dimensiones, cada índice de color, el rango de las coordenadas, las operaciones permitidas, la estructura de las capas y los límites globales (8000 operaciones, 64 capas, 2 MB).

## Control de calidad

Antes de mostrar una imagen, `composeChecked` (`src/generator/qualityControl.js`) la renderiza y mide sobre los píxeles reales:

| Comprobación | Medida | Corrección automática |
|---|---|---|
| Elementos principales presentes | Píxeles propios de cada sujeto y estructura | — |
| Dentro del lienzo | Fracción de la caja visible | Desplaza hacia dentro |
| Jerarquía visual | Tamaño del protagonista frente al lienzo y a los demás sujetos | Escala el protagonista |
| El fondo no lo oculta | Porcentaje de sus píxeles que quedan a la vista | Aparta el primer plano o lo desplaza |
| Contraste de la silueta | Diferencia de color en el borde con el fondo | Añade contorno |
| Solapamientos indebidos | Superposición entre sujetos sin relación | Los separa |
| Separación de planos | Lo del fondo no puede ser mayor que lo de delante | Reduce lo del fondo |
| Sombras coherentes | Lado de la sombra frente a la dirección de la luz | — |
| Paleta limitada | Nº de colores ≤ 64 | — |

Hace hasta tres pasadas. El resultado de cada comprobación se muestra en la aplicación; si alguna no se supera, se indica en lugar de ocultarlo.
