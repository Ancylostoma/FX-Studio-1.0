# Guía de instalación (Windows)

PixelForge Studio funciona en dos niveles:

1. **Motor procedural**: solo necesita Node.js. Sin internet, sin cuentas, sin claves.
2. **IA local con Ollama** (opcional, recomendada si el equipo tiene al menos 8 GB de RAM): un modelo de lenguaje en tu propio ordenador interpreta descripciones libres.

## 1. Node.js

1. Descarga la versión **LTS** desde <https://nodejs.org> e instálala con las opciones por defecto.
2. Doble clic en **`INICIAR.bat`**. Se abre el navegador en `http://localhost:8787`.
3. Para cerrarla: **`DETENER.bat`**.

No hace falta `npm install` para el motor procedural ni para Ollama. Solo el modo opcional con Claude (nube, de pago) necesita `npm install`.

## 2. Ollama (opcional)

1. Descarga Ollama desde <https://ollama.com/download> e instálalo. Queda en marcha en segundo plano (icono en la bandeja del sistema).
2. Ejecuta **`COMPROBAR_OLLAMA.bat`**: indica si Ollama responde, qué modelos tienes y cuál se sugiere para tu memoria RAM.
3. Descarga un modelo **solo si quieres**, abriendo una terminal (`cmd`) y escribiendo, por ejemplo:

   ```bat
   ollama pull qwen2.5:3b
   ```

   Son varios GB. PixelForge nunca descarga modelos por su cuenta.
4. En la aplicación, abre el panel **«IA local (Ollama)»** y pulsa **⟳**. Debe aparecer «conectado» y el modelo en la lista.
5. Elige el motor **«Híbrido automático»** (usa Ollama y, si no responde, el procedural) o **«IA local · Ollama»** (solo Ollama).

Una vez descargados Node.js, Ollama y el modelo, todo funciona **sin conexión a internet**.

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| «no detectado» en el panel | Ollama no está en marcha | Ábrelo desde el menú Inicio o ejecuta `ollama serve` |
| «conectado» pero sin modelos | No hay ningún modelo descargado | `ollama pull qwen2.5:3b` |
| La interpretación tarda más de un minuto | Modelo grande para el equipo, sin GPU | Usa un modelo más pequeño (ver GUIA_MODELOS.md) o sube el «Tiempo límite» |
| «tardó demasiado en responder» | La primera carga del modelo es lenta | Vuelve a intentarlo: la segunda vez ya está en memoria |
| El navegador no abre | El puerto 8787 está ocupado | Cierra la otra aplicación o cambia `PORT` en `.env` |

## Usar Ollama de otro ordenador de la red (opcional)

Por seguridad, PixelForge solo habla con Ollama en el propio equipo. Para usar otro ordenador de tu red local:

1. En el ordenador con Ollama, haz que escuche en la red (variable de entorno `OLLAMA_HOST=0.0.0.0` y reinicia Ollama). **No abras ese puerto a internet en el router.**
2. En el equipo con PixelForge, crea un archivo `.env` con la dirección autorizada:

   ```ini
   PIXELFORGE_OLLAMA_HOSTS=192.168.1.20:11434
   ```

3. Reinicia PixelForge y escribe `http://192.168.1.20:11434` en «Dirección del servidor».

Cualquier otra dirección se rechaza. La comunicación con Ollama no usa claves, por lo que conviene limitarla a una red de confianza.

## Móvil

La versión móvil usa el motor procedural (instala la PWA desde el navegador del móvil apuntando al ordenador, o sirve la carpeta con cualquier servidor estático). Ollama no se ejecuta en el móvil.
