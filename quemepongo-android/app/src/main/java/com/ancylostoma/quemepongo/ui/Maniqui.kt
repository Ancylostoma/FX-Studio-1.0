package com.ancylostoma.quemepongo.ui

import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Cuerpo
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.Fotos
import com.ancylostoma.quemepongo.data.Prenda
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fotos ya abiertas, para no leerlas del disco en cada pantalla. */
object CacheImagenes {
  val mapa = LruCache<String, ImageBitmap>(48)
}

/**
 * Maniquí que se viste capa por capa. Cada prenda se recorta con la forma de
 * su tipo (camisa, pantalón, abrigo...) y se rellena con la foto real de la
 * prenda; si no tiene foto, con su color.
 *
 * [claveAnimacion]: al cambiar, el maniquí se vuelve a vestir con animación.
 * [animarSolo]: si se indica, solo esa prenda aparece animada (la que se acaba de poner).
 */
@Composable
fun Maniqui(
  prendas: List<Prenda>,
  cuerpo: Cuerpo,
  foto: (String?) -> File?,
  modifier: Modifier = Modifier,
  claveAnimacion: Any? = null,
  animarSolo: String? = null,
  /** false en vistas de ejemplo: las prendas que no tienes no llevan borde punteado. */
  marcarIdeas: Boolean = true,
) {
  val capas = prendas.sortedBy { it.subtipo.forma.capa }
  val cargadas = remember { mutableIntStateOf(0) }
  LaunchedEffect(capas.mapNotNull { it.foto }) {
    for (p in capas) {
      val nombre = p.foto ?: continue
      if (CacheImagenes.mapa.get(nombre) != null) continue
      val bmp = withContext(Dispatchers.IO) { foto(nombre)?.let { Fotos.cargar(it, 512) } } ?: continue
      CacheImagenes.mapa.put(nombre, bmp.asImageBitmap())
      cargadas.intValue++
    }
  }
  val progreso = remember(claveAnimacion) { Animatable(0f) }
  LaunchedEffect(claveAnimacion) {
    val meta = if (animarSolo != null) 1f else capas.size.toFloat().coerceAtLeast(1f)
    val ms = if (animarSolo != null) 450 else 320 * capas.size.coerceAtLeast(1)
    progreso.animateTo(meta, tween(durationMillis = ms, easing = LinearEasing))
  }

  val tonos = if (LocalOscuro.current) TonosManiqui.Oscuro else TonosManiqui.Claro
  Canvas(modifier) {
    cargadas.intValue // Volver a dibujar cuando llegue una foto.
    val k = min(size.width / 100f, size.height / 200f)
    val g = Geometria(cuerpo, (size.width - 100f * k) / 2f, (size.height - 200f * k) / 2f, k)
    dibujarCuerpo(g, tonos)
    // Al terminar la animación se ve todo, aunque luego se agreguen prendas.
    val terminado = !progreso.isRunning && progreso.value > 0f
    capas.forEachIndexed { i, p ->
      val alfa = if (terminado) {
        1f
      } else if (animarSolo != null) {
        if (p.id == animarSolo) progreso.value else 1f
      } else {
        (progreso.value - i).coerceIn(0f, 1f)
      }
      if (alfa <= 0f) return@forEachIndexed
      val imagen = p.foto?.let { CacheImagenes.mapa.get(it) }
      translate(top = (1f - alfa) * -10f * k) {
        dibujarPrenda(g, p, imagen, alfa, tonos, marcarIdeas)
      }
    }
  }
}

/**
 * Dibuja el maniquí ya vestido de una vez, sin animación. Sirve para pintarlo
 * fuera de la pantalla, por ejemplo en la imagen para compartir.
 */
fun DrawScope.dibujarManiqui(prendas: List<Prenda>, cuerpo: Cuerpo, imagenes: Map<String, ImageBitmap>) {
  val k = min(size.width / 100f, size.height / 200f)
  val g = Geometria(cuerpo, (size.width - 100f * k) / 2f, (size.height - 200f * k) / 2f, k)
  dibujarCuerpo(g, TonosManiqui.Claro)
  prendas.sortedBy { it.subtipo.forma.capa }.forEach { p -> dibujarPrenda(g, p, p.foto?.let { imagenes[it] }, 1f, TonosManiqui.Claro, marcarIdeas = false) }
}

/** Coordenadas del maniquí en una caja de 100 x 200 unidades. */
private class Geometria(cuerpo: Cuerpo, val ox: Float, val oy: Float, val k: Float) {
  /** Medio ancho de hombros, cintura y cadera. */
  val hombro: Float
  val cintura: Float
  val cadera: Float

  init {
    when (cuerpo) {
      Cuerpo.FEMENINO -> { hombro = 15f; cintura = 10.5f; cadera = 17f }
      Cuerpo.MASCULINO -> { hombro = 19f; cintura = 14f; cadera = 15f }
      Cuerpo.NEUTRO -> { hombro = 17f; cintura = 12.5f; cadera = 16f }
    }
  }

  fun p(x: Float, y: Float) = Offset(ox + x * k, oy + y * k)

  fun rect(l: Float, t: Float, r: Float, b: Float) = Rect(p(l, t), p(r, b))

  /** Medio ancho del cuerpo a la altura [y], entre cintura y cadera. */
  fun anchoCadera(y: Float): Float = when {
    y <= 84f -> cintura
    y >= 100f -> cadera
    else -> cintura + (cadera - cintura) * (y - 84f) / 16f
  }

  /** Polígono con esquinas redondeadas: la tela no tiene picos. */
  fun forma(puntos: List<Pair<Float, Float>>, redondeo: Float = 0.25f): Path {
    val pts = puntos.map { p(it.first, it.second) }
    val n = pts.size
    val camino = Path()
    for (i in 0..n) {
      val a = pts[(i - 1 + n) % n]
      val b = pts[i % n]
      val c = pts[(i + 1) % n]
      val entrada = b + (a - b) * redondeo
      val salida = b + (c - b) * redondeo
      if (i == 0) {
        camino.moveTo(salida.x, salida.y)
        continue
      }
      camino.lineTo(entrada.x, entrada.y)
      // Curva cuadrática por el vértice, escrita como cúbica.
      val c1 = entrada + (b - entrada) * (2f / 3f)
      val c2 = salida + (b - salida) * (2f / 3f)
      camino.cubicTo(c1.x, c1.y, c2.x, c2.y, salida.x, salida.y)
    }
    camino.close()
    return camino
  }

  /** Refleja una lista de puntos del lado izquierdo al derecho. */
  fun espejo(izq: List<Pair<Float, Float>>) = izq.reversed().map { (100f - it.first) to it.second }

  fun simetrica(izq: List<Pair<Float, Float>>, redondeo: Float = 0.25f) = forma(izq + espejo(izq), redondeo)

  /** Brazo o manga desde el hombro hasta [hastaY]. [lado] = -1 izquierdo, 1 derecho. */
  fun brazo(lado: Int, hastaY: Float, holgura: Float = 0f): Path {
    val fuera = listOf(hombro - 1f to 40f, hombro + 3f to 47f, hombro + 5f to 72f, hombro + 7f to 104f)
    val dentro = listOf(hombro - 5f to 56f, hombro - 1f to 74f, hombro + 2f to 104f)
    fun corte(linea: List<Pair<Float, Float>>): Float {
      for (i in 0 until linea.size - 1) {
        val (x0, y0) = linea[i]
        val (x1, y1) = linea[i + 1]
        if (hastaY in y0..y1) return x0 + (x1 - x0) * (hastaY - y0) / (y1 - y0)
      }
      return linea.last().first
    }
    val f = fuera.filter { it.second < hastaY } + (corte(fuera) to hastaY)
    val d = listOf(corte(dentro) to hastaY) + dentro.filter { it.second < hastaY }.reversed()
    val puntos = (f.map { (it.first + holgura) to it.second } + d.map { (it.first - holgura * 0.5f) to it.second })
      .map { (50f + lado * it.first) to it.second }
    return forma(puntos, 0.2f)
  }
}

private fun unir(vararg caminos: Path): Path =
  caminos.reduce { acc, c -> Path().apply { op(acc, c, PathOperation.Union) } }

/** Colores del maniquí. En modo claro, más oscuro que el fondo; en modo oscuro, más claro. */
class TonosManiqui(val piel: Color, val borde: Color, val bordePrenda: Color, val sombra: Color) {
  companion object {
    val Claro = TonosManiqui(Color(0xFFD5C3B3), Color(0xFF6E5A4B), Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.16f))
    val Oscuro = TonosManiqui(Color(0xFFE4D6CA), Color(0xFF2B231D), Color.White.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.45f))
  }
}

private fun DrawScope.dibujarCuerpo(g: Geometria, tonos: TonosManiqui) {
  // Sombra en el suelo.
  drawOval(tonos.sombra, topLeft = g.p(26f, 189f), size = androidx.compose.ui.geometry.Size(48f * g.k, 7f * g.k))

  val torso = g.simetrica(
    listOf(46f to 30f, 46f to 39f, 50f - g.hombro + 1f to 41f, 50f - g.hombro + 4f to 56f, 50f - g.cintura to 84f, 50f - g.cadera to 100f, 49f to 108f),
    0.3f,
  )
  val piernaIzq = g.forma(
    listOf(50f - g.cadera to 98f, 50f - g.cadera + 6f to 140f, 50f - 8.5f to 178f, 50f - 3.5f to 178f, 50f - 3f to 140f, 50f - 1f to 112f, 50f to 100f),
    0.2f,
  )
  val piernaDer = g.forma(g.espejo(listOf(50f - g.cadera to 98f, 50f - g.cadera + 6f to 140f, 50f - 8.5f to 178f, 50f - 3.5f to 178f, 50f - 3f to 140f, 50f - 1f to 112f, 50f to 100f)).reversed(), 0.2f)
  val cabeza = Path().apply { addOval(g.rect(40.5f, 8f, 59.5f, 32f)) }
  val manoIzq = Path().apply { addOval(g.rect(50f - g.hombro - 8f, 103f, 50f - g.hombro - 1.5f, 114f)) }
  val manoDer = Path().apply { addOval(g.rect(50f + g.hombro + 1.5f, 103f, 50f + g.hombro + 8f, 114f)) }
  val pies = listOf(-1, 1).map { lado ->
    g.forma(listOf(10.5f to 177f, 3f to 177f, 2.5f to 186f, 4f to 191f, 11f to 191f, 12f to 186f).map { (50f + lado * it.first) to it.second }, 0.3f)
  }
  val cuerpo = unir(torso, piernaIzq, piernaDer, cabeza, g.brazo(-1, 104f), g.brazo(1, 104f), manoIzq, manoDer, pies[0], pies[1])
  drawPath(cuerpo, tonos.piel)
  val b = cuerpo.getBounds()
  drawPath(
    cuerpo,
    Brush.horizontalGradient(
      listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.10f)),
      startX = b.left,
      endX = b.right,
    ),
  )
  drawPath(cuerpo, tonos.borde, style = Stroke(width = 1.4f * density, join = StrokeJoin.Round))
}

/** Silueta de cada tipo de prenda. */
private fun formaDe(g: Geometria, f: Forma): List<Path> {
  val s = g.hombro
  fun torso(hastaY: Float, hombros: Float = 1f, cuello: Float = 44.5f): Path {
    val izq = mutableListOf(cuello to 39f, 50f - s + hombros to 41f, 50f - s + 4f to 56f, 50f - g.cintura - 0.6f to 84f)
    if (hastaY > 84f) izq.add(50f - g.anchoCadera(hastaY) - 0.8f to hastaY)
    return g.forma(izq + g.espejo(izq) + (50f to 45f), 0.22f)
  }
  fun falda(desdeY: Float, hastaY: Float, vuelo: Float): List<Pair<Float, Float>> =
    listOf(50f - g.anchoCadera(desdeY) - 0.6f to desdeY, 50f - g.cadera - 0.8f to 100f, 50f - g.cadera - vuelo to hastaY)
  fun pierna(hastaY: Float): List<Pair<Float, Float>> {
    // Contorno exterior e interior de una pernera, del lado izquierdo.
    val fueraX = if (hastaY <= 140f) g.cadera + 0.5f - 6f * (hastaY - 100f) / 40f else 9f + (g.cadera - 5.5f - 9f) * (177f - hastaY) / 37f
    val dentroX = if (hastaY <= 140f) 0.8f + 1.8f * (hastaY - 112f).coerceAtLeast(0f) / 28f else 3f
    val izq = mutableListOf(50f - g.cintura - 0.6f to 82f, 50f - g.cadera - 0.6f to 100f)
    if (hastaY > 140f) izq.add(50f - g.cadera + 5.5f to 140f)
    izq.add(50f - fueraX to hastaY)
    izq.add(50f - dentroX to hastaY)
    if (hastaY > 140f) izq.add(50f - 2.6f to 140f)
    izq.add(50f - 0.8f to 112f)
    return izq
  }
  fun zapatos(alto: Float, ancho: Float = 0f): List<Path> = listOf(-1, 1).map { lado ->
    g.forma(
      listOf(10f + ancho to alto, 3.2f to alto, 2.3f to 186f, 3.8f to 191.5f, 11.5f + ancho to 191.5f, 12.5f + ancho to 186f)
        .map { (50f + lado * it.first) to it.second },
      0.3f,
    )
  }
  return when (f) {
    Forma.TOP_SIN_MANGAS -> listOf(torso(103f, hombros = 5f, cuello = 45.5f))
    Forma.TOP_MANGA_CORTA -> listOf(unir(torso(103f), g.brazo(-1, 63f, 1.2f), g.brazo(1, 63f, 1.2f)))
    Forma.TOP_MANGA_LARGA -> listOf(unir(torso(104f), g.brazo(-1, 103f, 1.2f), g.brazo(1, 103f, 1.2f)))
    Forma.PANTALON -> listOf(g.forma(pierna(177f).let { it + g.espejo(it) }, 0.15f))
    Forma.SHORT -> listOf(g.forma(pierna(126f).let { it + g.espejo(it) }, 0.15f))
    Forma.FALDA -> listOf(g.simetrica(falda(82f, 138f, 5f), 0.2f))
    Forma.VESTIDO_CORTO -> listOf(
      g.forma(
        listOf(45f to 39f, 50f - s + 4f to 41f, 50f - s + 5f to 56f, 50f - g.cintura - 0.5f to 84f) + falda(86f, 138f, 6f).drop(1) +
          g.espejo(listOf(45f to 39f, 50f - s + 4f to 41f, 50f - s + 5f to 56f, 50f - g.cintura - 0.5f to 84f) + falda(86f, 138f, 6f).drop(1)) +
          (50f to 46f),
        0.2f,
      ),
    )
    Forma.VESTIDO_LARGO -> listOf(
      unir(
        g.forma(
          listOf(45f to 39f, 50f - s + 4f to 41f, 50f - s + 5f to 56f, 50f - g.cintura - 0.5f to 84f) + falda(86f, 174f, 10f).drop(1) +
            g.espejo(listOf(45f to 39f, 50f - s + 4f to 41f, 50f - s + 5f to 56f, 50f - g.cintura - 0.5f to 84f) + falda(86f, 174f, 10f).drop(1)) +
            (50f to 46f),
          0.2f,
        ),
        g.brazo(-1, 60f, 0.8f),
        g.brazo(1, 60f, 0.8f),
      ),
    )
    Forma.CHAQUETA, Forma.ABRIGO_LARGO -> {
      val bajo = if (f == Forma.CHAQUETA) 107f else 152f
      val anchoBajo = if (f == Forma.CHAQUETA) g.anchoCadera(bajo) + 1.8f else g.cadera + 4f
      val panel = listOf(44f to 37f, 50f - s - 1f to 40f, 50f - s + 3f to 56f, 50f - g.cintura - 1.6f to 84f, 50f - g.cadera - 1.6f to 100f, 50f - anchoBajo to bajo, 48.2f to bajo, 48.2f to 58f)
      listOf(
        unir(g.forma(panel, 0.15f), g.brazo(-1, 104f, 2f)),
        unir(g.forma(g.espejo(panel).reversed(), 0.15f), g.brazo(1, 104f, 2f)),
      )
    }
    Forma.BUFANDA -> listOf(
      unir(
        g.forma(listOf(42.5f to 35f, 57.5f to 35f, 59f to 44f, 41f to 44f), 0.35f),
        g.forma(listOf(51f to 42f, 57f to 42f, 56.5f to 74f, 51.5f to 74f), 0.2f),
      ),
    )
    Forma.GORRO -> listOf(g.forma(listOf(40f to 20f, 40.5f to 10f, 50f to 4.5f, 59.5f to 10f, 60f to 20f), 0.35f))
    Forma.GORRA -> listOf(
      unir(
        g.forma(listOf(40.5f to 15.5f, 41.5f to 8.5f, 50f to 6f, 58.5f to 8.5f, 59.5f to 15.5f), 0.35f),
        g.forma(listOf(55f to 13.5f, 69f to 15f, 69f to 17.5f, 55f to 17.5f), 0.4f),
      ),
    )
    Forma.CALZADO_DEPORTIVO -> zapatos(175f, 0.8f)
    Forma.ZAPATO -> zapatos(179f)
    Forma.TACON -> zapatos(180f, -0.6f)
    Forma.SANDALIA -> listOf(-1, 1).flatMap { lado ->
      listOf(181.5f, 186.5f).map { y ->
        g.forma(listOf(11.8f to y, 2.6f to y, 2.6f to y + 2.2f, 11.8f to y + 2.2f).map { (50f + lado * it.first) to it.second }, 0.3f)
      } + g.forma(listOf(12f to 190f, 3f to 190f, 3.5f to 192f, 11.8f to 192f).map { (50f + lado * it.first) to it.second }, 0.3f)
    }
    Forma.BOTA -> listOf(-1, 1).map { lado ->
      g.forma(
        listOf(9.8f to 156f, 2.8f to 156f, 2.3f to 186f, 3.8f to 191.5f, 11.8f to 191.5f, 12.8f to 186f, 10.6f to 176f)
          .map { (50f + lado * it.first) to it.second },
        0.2f,
      )
    }
    Forma.BOLSO -> listOf(g.forma(listOf(50f + g.cadera + 2f to 97f, 50f + g.cadera + 15f to 97f, 50f + g.cadera + 16f to 116f, 50f + g.cadera + 1f to 116f), 0.25f))
  }
}

private fun DrawScope.dibujarPrenda(g: Geometria, p: Prenda, imagen: ImageBitmap?, alfa: Float, tonos: TonosManiqui, marcarIdeas: Boolean = true) {
  val color = Color(Colores.de(p.color).argb)
  if (p.subtipo.forma == Forma.BOLSO) {
    // Correa cruzada.
    drawLine(
      color.copy(alpha = alfa),
      g.p(50f - g.hombro + 3f, 41f),
      g.p(50f + g.cadera + 8.5f, 97f),
      strokeWidth = 1.6f * g.k,
      cap = StrokeCap.Round,
    )
  }
  for (camino in formaDe(g, p.subtipo.forma)) {
    val b = camino.getBounds()
    if (imagen != null) {
      // Debajo, el color: en las fotos sin fondo los huecos transparentes no dejan ver el cuerpo.
      drawPath(camino, color, alpha = alfa)
      clipPath(camino) {
        val proporcion = (b.width / b.height).coerceAtLeast(0.05f)
        var sw = imagen.width
        var sh = (imagen.width / proporcion).toInt()
        if (sh > imagen.height) {
          sh = imagen.height
          sw = (imagen.height * proporcion).toInt()
        }
        drawImage(
          imagen,
          srcOffset = IntOffset((imagen.width - sw) / 2, (imagen.height - sh) / 2),
          srcSize = IntSize(sw.coerceAtLeast(1), sh.coerceAtLeast(1)),
          dstOffset = IntOffset(b.left.toInt(), b.top.toInt()),
          dstSize = IntSize(b.width.toInt().coerceAtLeast(1), b.height.toInt().coerceAtLeast(1)),
          alpha = alfa,
        )
      }
    } else {
      drawPath(camino, color, alpha = alfa)
      if (p.color == Colores.ESTAMPADO) dibujarEstampado(camino, b, alfa, g.k)
    }
    // Volumen: más oscuro en los costados.
    drawPath(
      camino,
      Brush.horizontalGradient(
        listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.22f)),
        startX = b.left,
        endX = b.right,
      ),
      alpha = alfa,
    )
    if (p.virtual && marcarIdeas) {
      // Las ideas de compra se ven con borde punteado para distinguirlas de lo que ya tienes.
      drawPath(
        camino,
        tonos.bordePrenda.copy(alpha = 0.9f),
        alpha = alfa,
        style = Stroke(width = 1.6f * density, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density))),
      )
    } else {
      drawPath(camino, tonos.bordePrenda, alpha = alfa, style = Stroke(width = 1.3f * density, join = StrokeJoin.Round))
    }
  }
}

private fun DrawScope.dibujarEstampado(camino: Path, b: Rect, alfa: Float, k: Float) {
  val tonos = listOf(Color(0xFFF2C94C), Color(0xFFEE9DB8), Color(0xFF2E8B57), Color(0xFFF5F4EE))
  clipPath(camino) {
    var y = b.top
    var fila = 0
    while (y < b.bottom) {
      var x = b.left + if (fila % 2 == 0) 0f else 4f * k
      var i = fila
      while (x < b.right) {
        drawCircle(tonos[i % tonos.size], radius = 1.4f * k, center = Offset(x, y), alpha = alfa)
        x += 8f * k
        i++
      }
      y += 6f * k
      fila++
    }
  }
}

/**
 * Dibujo de un tipo de prenda (la misma silueta que usa el maniquí), para
 * botones, íconos y pantallas vacías.
 */
@Composable
fun IconoForma(
  forma: Forma,
  color: Color,
  modifier: Modifier = Modifier,
  // El borde contrasta con el fondo: así una prenda negra se ve en modo oscuro y una blanca en el claro.
  borde: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
) {
  Canvas(modifier) {
    val g = Geometria(Cuerpo.NEUTRO, 0f, 0f, 1f)
    val caminos = formaDe(g, forma)
    val limites = caminos.map { it.getBounds() }.reduce { a, b ->
      Rect(min(a.left, b.left), min(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }
    val escala = min(size.width / limites.width, size.height / limites.height) * 0.92f
    val dx = (size.width - limites.width * escala) / 2f - limites.left * escala
    val dy = (size.height - limites.height * escala) / 2f - limites.top * escala
    withTransform({
      translate(dx, dy)
      scale(escala, escala, pivot = Offset.Zero)
    }) {
      caminos.forEach { c ->
        drawPath(c, color)
        drawPath(c, borde, style = Stroke(width = 1.4f * density / escala, join = StrokeJoin.Round))
      }
    }
  }
}

/** Forma con la que se representa cada categoría. */
fun formaDeCategoria(c: com.ancylostoma.quemepongo.data.Categoria): Forma = when (c) {
  com.ancylostoma.quemepongo.data.Categoria.SUPERIOR -> Forma.TOP_MANGA_CORTA
  com.ancylostoma.quemepongo.data.Categoria.INFERIOR -> Forma.PANTALON
  com.ancylostoma.quemepongo.data.Categoria.VESTIDO -> Forma.VESTIDO_CORTO
  com.ancylostoma.quemepongo.data.Categoria.ABRIGO -> Forma.CHAQUETA
  com.ancylostoma.quemepongo.data.Categoria.CALZADO -> Forma.BOTA
  com.ancylostoma.quemepongo.data.Categoria.ACCESORIO -> Forma.GORRA
}
