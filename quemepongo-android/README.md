# Qué Me Pongo — app Android

Te arma el look del día según:

- **El tiempo**: pronóstico de tu ciudad para la mañana, tarde o noche (MET Norway, gratis
  también para uso comercial). Lluvia → impermeable y calzado cerrado; calor → sin capas.
- **La ocasión**: día a día, trabajo, cita, fiesta, evento formal, deporte, viaje.
- **La moda**: tendencias de temporada, eventos (Halloween, Día de Muertos, fiestas...) y
  looks inspirados en famosos. Se actualizan solas desde `app/src/main/assets/tendencias.json`.
- **Tu estilo**: estilos y colores que te gustan, colores que evitas y cuánto te importa la moda.
- **Lo que aprende de ti**: cada "me gusta", "no me gusta", look guardado o "me lo pongo"
  ajusta las sugerencias siguientes. No repite la misma ropa días seguidos.

Fotografías tu ropa y el **maniquí** se viste capa por capa con tus prendas. Lo que te
falte para completar un look aparece con borde punteado y botón para buscarlo en tienda.

## Pantallas

| Pestaña | Qué hace |
|---|---|
| Hoy | Una línea con tiempo, ciudad, ocasión y momento (se toca para cambiarla). Looks para deslizar, corazón para guardar, X para descartar, y un botón: "Me lo pongo". |
| Armario | Tus prendas. Las fotos salen sin fondo (ML Kit) y el color se detecta solo. |
| + (centro) | Agregar prenda en 3 toques: foto → qué es → guardar. "Guardar y otra" para cargar varias seguidas. |
| Descubrir | Tendencias con su look dibujado + "Para ti": ideas de compra parecidas a lo que te gusta. |
| Yo | Looks guardados y diario de lo que te pusiste. Ajustes en el engranaje. |

Bienvenida en 6 pasos cortos (maniquí, estilos con looks de ejemplo, ciudad, aviso de las
7:30 y primera prenda). Cada look se puede **compartir como imagen** y verse como
**collage de tus fotos** o en el maniquí.

"Editar" (desde Hoy, un look guardado o una tendencia) abre el maniquí para cambiar prendas
y te dice si vas abrigado, si combinan los colores y si encaja con la ocasión.

## Idiomas

Inglés (por defecto para el resto del mundo), español, portugués, francés, alemán e italiano.

- **Textos de la app:** todos están en `traducciones/textos.py`. Para cambiar uno o
  agregar un idioma, edítalo ahí y ejecuta `python3 traducciones/generar.py`: escribe los
  `strings.xml` y avisa si a un idioma le falta un hueco (`%1$s`).
- **Tendencias:** cada texto de `tendencias.json` puede ir como `{"es": ..., "en": ...}`;
  si falta un idioma se usa el inglés.
- **Ficha de Play** en los 6 idiomas: `play-store/ficha.md`.
- En Estados Unidos la temperatura sale en °F.

## Con miles de usuarios

La app no tiene servidor propio: cada teléfono funciona solo. Lo único compartido es:

- **El tiempo (MET Norway):** pide como mucho unas 20 peticiones por segundo por app. Por eso
  el pronóstico se guarda 45 minutos, las coordenadas van con 2 decimales y el aviso de la
  mañana se reparte entre las 7:30 y las 7:50. Pasados los ~100.000 usuarios conviene
  escribirles o poner un servidor intermedio.
- **El archivo de tendencias (GitHub):** se baja cada 12 horas; con más de ~100.000 usuarios,
  moverlo a GitHub Pages o Cloudflare.

Para probar en muchos teléfonos distintos: el **informe previo al lanzamiento** de Play Console
(gratis al subir a prueba interna) y Firebase Test Lab. `MotorTest` comprueba que con 1000
prendas y un año de uso el motor sigue respondiendo rápido.

## Compilar

GitHub → **Actions** → *Build Qué Me Pongo* → **Run workflow**. Al terminar hay dos archivos:

- `quemepongo-apk`: para instalar directamente en el teléfono y probar.
- `quemepongo-aab`: el que se sube a Google Play.

Cada compilación sube el `versionCode`, así la nueva se instala encima sin perder datos.

## Actualizar tendencias sin publicar otra versión

Edita `app/src/main/assets/tendencias.json` en la rama `main`. Las apps instaladas lo
descargan en menos de 12 horas. En `tienda_url` puedes poner tu enlace de afiliado
(por ejemplo Amazon Afiliados), `{q}` se reemplaza por la prenda buscada.

## Premium

| Gratis | Premium |
|---|---|
| Look del día, aviso a las 7:30 | Aviso a la hora que elijas |
| Hasta 30 prendas y 15 looks guardados | Sin límite |
| Anuncios en Descubrir | Sin anuncios |
| — | Planear la semana, maleta de viaje, estadísticas y costo por uso |

La pantalla de Premium se abre solo al llegar a un límite o al tocar una función Premium.

**Para cobrar, en Play Console** (cuando la app esté subida, aunque sea en prueba interna):
*Monetizar → Productos → Suscripciones → Crear* con el id **`premium`** y dos planes base:
**`mensual`** (1 mes, renovación automática) y **`anual`** (1 año). En el anual agrega una
oferta de **prueba gratis de 7 días**. Los precios por país los pone Play. Para probar pagos
sin cobrar, agrega tu correo en *Configuración → Pruebas de licencias*.

**Modo prueba:** mientras la app no tenga tu llave de subida, la pantalla de Premium muestra
"Modo prueba: activar Premium" para probar todo sin pagar. Con la llave, ese botón desaparece.

La compra se comprueba en el teléfono. Para una app pequeña es suficiente; con muchos
ingresos conviene verificarla en un servidor (Google Play Developer API).

## Publicar en Google Play — lo que falta y solo puedes hacer tú

1. **Cuenta de desarrollador de Google Play** (pago único de 25 USD) en
   https://play.google.com/console. Comprueba antes que tu país puede recibir pagos de
   Google: no todos pueden.
2. **Llave de subida.** Crea una (una sola vez, y guárdala bien):

   ```
   keytool -genkeypair -v -keystore subida.jks -alias subida -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 subida.jks
   ```

   En GitHub → *Settings → Secrets and variables → Actions* crea:
   `QMP_KEYSTORE_BASE64` (el texto del base64), `QMP_STORE_PASSWORD`, `QMP_KEY_PASSWORD`
   y `QMP_KEY_ALIAS` (`subida`). Sin estos secretos el AAB sale firmado con la llave de
   pruebas y Play lo rechaza.
3. **AdMob** (https://admob.google.com): crea la app y un bloque de anuncios *banner*.
   Pon sus IDs en los secretos `QMP_ADMOB_APP_ID` y `QMP_ADMOB_BANNER`. Mientras no
   estén, la app muestra anuncios de prueba de Google, que no pagan.
4. **Política de privacidad**: completa tu correo en `PRIVACIDAD.md` y usa esta URL en
   Play: `https://github.com/Ancylostoma/FX-Studio-1.0/blob/main/quemepongo-android/PRIVACIDAD.md`.
5. **Ficha de Play**: nombre, descripción, ícono 512×512, al menos 2 capturas de pantalla
   y la imagen destacada 1024×500. Formulario de *Seguridad de los datos*: ubicación
   aproximada (no se comparte salvo con el servicio del tiempo, no se guarda en servidores),
   identificador de publicidad (AdMob), fotos (no salen del teléfono).
6. **Prueba cerrada**: las cuentas de desarrollador personales nuevas deben tener la app
   en prueba cerrada con al menos 12 personas durante 14 días antes de poder publicarla
   para todos.

## Pruebas

`gradle testDebugUnitTest` prueba el motor de sugerencias: calor, frío, lluvia, eventos
formales, armario vacío, aprendizaje, fechas de tendencias y que `tendencias.json` sea válido.
