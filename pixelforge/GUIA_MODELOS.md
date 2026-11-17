# Guía de modelos para la IA local

PixelForge no impone un modelo: lista los que tienes instalados en Ollama, recomienda uno según tu memoria y permite **medirlos** con «Evaluar este modelo».

## Qué hace el modelo

El modelo actúa como **director artístico**: lee la descripción y devuelve un plan JSON con protagonista, planos, relaciones espaciales, acción, paleta, luz y vista. Lo que no existe en el catálogo lo describe con **kits paramétricos**: un hada es un humanoide pequeño con alas y brillo; un tigre, un cuadrúpedo naranja con rayas; una catedral, un edificio de piedra con agujas y rosetón. **El modelo nunca dibuja ni genera código**: el plan se valida contra un esquema y el motor propio compone y dibuja.

Por eso basta con modelos pequeños: la tarea es entender y elegir, no pintar.

## Recomendación orientativa

| Memoria del equipo | Modelo sugerido | Descarga aprox. | Notas |
|---|---|---|---|
| Menos de 8 GB de RAM | `qwen2.5:1.5b` | 1 GB | Entiende frases sencillas; puede olvidar elementos (el modo híbrido los recupera) |
| 8–16 GB de RAM | `qwen2.5:3b` | 1,9 GB | Buen equilibrio sin GPU |
| 16 GB o más | `qwen2.5:7b` | 4,7 GB | Mejor comprensión; más lento sin GPU |
| GPU con 10 GB o más | `qwen2.5:14b` | 9 GB | La mejor opción si hay GPU |

Los tamaños son aproximados y pueden variar según la versión publicada en Ollama. Otras familias que funcionan: `qwen3`, `gemma3`, `llama3.2`, `phi4-mini`, `mistral`. Los modelos de *embeddings* (por ejemplo `nomic-embed-text`) no sirven y la aplicación los oculta.

> **Aviso:** esta tabla es una orientación basada en el tamaño y la familia de los modelos, no una medición. La forma fiable de elegir es usar «Evaluar este modelo» en tu equipo.

## «Evaluar este modelo»

Envía tres descripciones redactadas con sinónimos que el motor procedural **no** conoce («paladín», «bestia alada que escupe fuego», «felino rayado», «salto de agua», «archimago», «sala de lectura») y mide:

- **JSON válido**: proporción de respuestas que cumplen el esquema.
- **Elementos esperados encontrados**: si el plan contiene caballero, dragón, castillo, tigre, cascada, interior, mago, vela…
- **Tiempo** por descripción y **velocidad** en tokens por segundo.

Muestra a la vez el resultado del motor procedural con las mismas frases, para saber si el modelo aporta algo en tu equipo.

## Ajustes del panel

| Ajuste | Para qué sirve |
|---|---|
| Temperatura | 0–0,3 da planes estables y fieles; más alto, más variación |
| Interpretación | Fiel (solo lo pedido) · Equilibrada · Creativa (añade ambiente y efectos) |
| Tokens máx. | Límite de la respuesta; 1200 basta para escenas complejas |
| Tiempo límite | Sin GPU, la primera petición (carga del modelo) puede tardar más |

## Compatibilidad

- Con **Ollama 0.5 o superior** se usa salida estructurada con esquema JSON. Con versiones anteriores se usa el modo `json` y se valida igualmente.
- A los modelos con razonamiento previo (`thinking`, por ejemplo `qwen3`) se les desactiva para responder más rápido. A los que no lo admiten no se les envía la opción, porque Ollama la rechazaría.
- Si la respuesta no es válida, se reintenta una vez indicando el error. Si vuelve a fallar, el modo híbrido usa el motor procedural.
