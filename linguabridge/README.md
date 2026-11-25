# LinguaBridge AI · Motor de parafraseo

Motor propio de parafraseo **español ↔ español** e **inglés ↔ inglés** para textos académicos y de ciencias de la salud. Mejora la claridad, la fluidez y la naturalidad **sin cambiar el significado**, y protege citas APA 7, referencias, cifras, unidades y terminología clínica.

> No traduce entre idiomas: reformula cada texto en su propio idioma.
> No es una herramienta para eludir Turnitin, GPTZero ni otros detectores, y no incluye ninguna función con ese fin.

## Origen y licencia

Todo el código, las listas léxicas y las reglas se han escrito desde cero para este proyecto. El repositorio *Turnitout-Humanizer* tiene licencia **propietaria** («All rights reserved»): solo se consultó su archivo `LICENSE` y **no se ha leído, copiado ni adaptado nada de su código, reglas ni recursos**. Las técnicas usadas (protección de elementos, sustitución contextual, cambio de voz, reordenación de subordinadas) son conceptos generales del procesamiento lingüístico.

## Inicio rápido

- **Windows:** doble clic en `INICIAR.bat` → abre `http://127.0.0.1:8788`.
- **macOS / Linux:** `npm start` en esta carpeta.
- **Pruebas:** `npm test` · **Línea de comandos:** `node bin/linguabridge.mjs --level 4 --mode nursing --report archivo.txt`

No hace falta `npm install`: no tiene dependencias (Node 20 o superior).

## Cómo funciona

```
texto → detectar idioma → PROTEGER → reglas por oración → [modelo de lenguaje por párrafo] → restaurar → informe + comparación
                              │              │                        │
                    marcadores opacos   cada oración se verifica;   la salida del modelo se verifica;
                    (citas, cifras...)  si falla, queda la original  si falla, se usa la de reglas
```

1. **Protección** (`src/engine/protect.js`). Se sustituye por un marcador opaco: lista de referencias completa (desde «Referencias/References»), entradas de referencia sueltas, citas APA 7 parentéticas y narrativas (`(García et al., 2021)`, `López y Martín (2020)`, `n.d./s.f.`, páginas), citas textuales entre comillas, URL/DOI, estadísticos (`p < .001`, `OR = 2.3`, `IC 95 %: 1,2-3,4`), cifras con unidades y rangos (`0,5 mg/kg`, `150 a 130 mmHg`, `38,5 °C`), números en letra, terminología clínica (≈ 500 términos ES/EN), fármacos por su terminación (DCI), siglas y los términos que añada el usuario. Ningún dígito queda sin proteger.
2. **Reglas** (`src/engine/transforms/`), en una sola pasada para que unas sustituciones no deshagan otras.
3. **Verificación** (`src/engine/verify.js`). Por oración: mismos elementos protegidos, mismas negaciones, mismo grado de certeza (*podría, sugiere, solo, all, may…*), ninguna cifra nueva. Al final: informe de integridad. Si algo falla, se devuelve el texto sin cambios.

## Los cinco niveles

| Nivel | Técnicas |
|---|---|
| 1 · Mínima | Redundancias («volver a repetir», «end result», «en el año 2022»), palabras repetidas, tipografía |
| 2 · Ligera | + expresiones y conectores equivalentes con su flexión («llevaron a cabo → realizaron», «due to the fact that → because») |
| 3 · Moderada | + sinónimos de una palabra que respetan género, número y categoría |
| 4 · Alta | + reordenación de subordinadas y voz: pasiva con agente → activa; en español, perifrástica → refleja |
| 5 · Máxima | todo con más frecuencia + activa → pasiva en inglés (estilo de métodos, solo con agentes genéricos) |

## Modo enfermería y ciencias de la salud

- Protege además el proceso enfermero y sus taxonomías (valoración, diagnóstico de enfermería, plan de cuidados, NANDA-I/NIC/NOC…).
- Aplica lenguaje centrado en la persona de APA 7 («pacientes diabéticos → pacientes con diabetes», «the elderly → older adults»).
- **Avisa** (sin cambiarlas) de abreviaturas peligrosas: «U», «IU», «QD», cero final («1.0 mg»), falta de cero inicial («.5 mg»), «cc».

## Modelo de lenguaje (opcional)

- **Ollama local** (por defecto `qwen3.5:4b` en `http://127.0.0.1:11434`, configurable en `.env`). Por seguridad solo se admite Ollama en el propio equipo, salvo que se autorice en `LINGUABRIDGE_OLLAMA_HOSTS`.
- **Cualquier servidor compatible con OpenAI** (LM Studio, llama.cpp, vLLM): `LINGUABRIDGE_LLM=openai`.
- El modelo **solo ve marcadores** `⟦n⟧` en lugar de citas, cifras y términos; la lista de referencias no se le envía.
- Su respuesta se rechaza si pierde o inventa marcadores, añade cifras, citas, URL o nombres propios, cambia negaciones o certeza, pierde demasiado contenido o añade demasiado. Se reintenta una vez; si vuelve a fallar, se usa la versión de reglas y se avisa.

## Uso como módulo

```js
import { paraphrase, paraphraseRules, createOllamaProvider } from './src/engine/index.js';

const r = paraphraseRules(texto, { lang: 'auto', level: 3, mode: 'nursing', protectedTerms: ['método canguro'] });
// r.result, r.diff, r.changes, r.integrity, r.warnings, r.protected, r.stats

const r2 = await paraphrase(texto, { level: 4, llm: { provider: createOllamaProvider({ model: 'qwen3.5:4b' }) } });
```

Para ampliarlo: añadir entradas a `src/engine/lexicon/{es,en,clinical}.js` (formato documentado en cada archivo), o un proveedor nuevo con la interfaz `{ name, model, status(), generate() }`.

## Límites conocidos

- Las reglas son deliberadamente conservadoras: en el nivel 5 cambian en torno a un 15–35 % de las palabras. Las reformulaciones más profundas dependen del modelo de lenguaje.
- La voz y la reordenación solo actúan sobre oraciones simples reconocibles; el resto se deja como está.
- La ruta con Ollama está probada contra un servidor que imita su API, no contra un modelo real.
