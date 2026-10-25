package com.ancylostoma.quemepongo.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Fotos {
  private const val LADO_MAXIMO = 1280

  /** Copia la foto elegida a la carpeta de la app, reducida y derecha. Devuelve el nombre del archivo. */
  fun importar(contexto: Context, uri: Uri, carpeta: File, id: String): String? {
    val resolver = contexto.contentResolver
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
    if (opciones.outWidth <= 0) return null

    var muestreo = 1
    while (opciones.outWidth / (muestreo * 2) >= LADO_MAXIMO || opciones.outHeight / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
    val decodificar = BitmapFactory.Options().apply { inSampleSize = muestreo }
    var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodificar) } ?: return null

    val giro = try {
      resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> 0f
        }
      } ?: 0f
    } catch (e: Exception) {
      0f
    }
    if (giro != 0f) {
      val m = Matrix().apply { postRotate(giro) }
      bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    val escala = LADO_MAXIMO.toFloat() / maxOf(bmp.width, bmp.height)
    if (escala < 1f) bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true)

    // Con el fondo quitado se guarda en PNG (con transparencia); si no se pudo, la foto tal cual.
    val recorte = quitarFondo(bmp)
    val nombre = "$id-${System.currentTimeMillis()}" + if (recorte != null) ".png" else ".jpg"
    FileOutputStream(File(carpeta, nombre)).use {
      if (recorte != null) recorte.compress(Bitmap.CompressFormat.PNG, 100, it) else bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
    }
    return nombre
  }

  /** Las fotos con el fondo quitado se guardan en PNG. */
  fun esRecorte(nombre: String?) = nombre?.endsWith(".png") == true

  /**
   * Quita el fondo con ML Kit (en el teléfono, sin internet una vez bajado el
   * modelo). Devuelve la prenda recortada al tamaño justo, o null si no se pudo:
   * por ejemplo, la primera vez, mientras Google Play baja el modelo.
   */
  private fun quitarFondo(bmp: Bitmap): Bitmap? = try {
    val opciones = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
    val segmentador = SubjectSegmentation.getClient(opciones)
    try {
      val resultado = Tasks.await(segmentador.process(InputImage.fromBitmap(bmp, 0)), 20, TimeUnit.SECONDS)
      resultado.foregroundBitmap?.let { recortarTransparencia(it) }
    } finally {
      segmentador.close()
    }
  } catch (e: Exception) {
    null
  }

  /** Quita los bordes transparentes, dejando un pequeño margen. */
  private fun recortarTransparencia(b: Bitmap): Bitmap? {
    val w = b.width
    val h = b.height
    val fila = IntArray(w)
    var arriba = h
    var abajo = -1
    var izquierda = w
    var derecha = -1
    for (y in 0 until h) {
      b.getPixels(fila, 0, w, 0, y, w, 1)
      for (x in 0 until w) {
        if ((fila[x] ushr 24) > 40) {
          if (y < arriba) arriba = y
          if (y > abajo) abajo = y
          if (x < izquierda) izquierda = x
          if (x > derecha) derecha = x
        }
      }
    }
    if (abajo < 0) return null
    // Si la prenda ocupa muy poco, el recorte probablemente falló.
    if ((derecha - izquierda) * (abajo - arriba) < w * h / 50) return null
    val margen = (maxOf(w, h) * 0.02f).toInt()
    val x0 = (izquierda - margen).coerceAtLeast(0)
    val y0 = (arriba - margen).coerceAtLeast(0)
    val x1 = (derecha + margen).coerceAtMost(w - 1)
    val y1 = (abajo + margen).coerceAtMost(h - 1)
    return Bitmap.createBitmap(b, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
  }

  /** Abre una foto guardada, reducida para dibujarla en pantalla. */
  fun cargar(archivo: File, ladoMaximo: Int = 600): Bitmap? {
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.path, opciones)
    if (opciones.outWidth <= 0) return null
    var muestreo = 1
    while (maxOf(opciones.outWidth, opciones.outHeight) / (muestreo * 2) >= ladoMaximo) muestreo *= 2
    return BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = muestreo })
  }

  /**
   * Color principal de la prenda: mira el centro de la foto y descarta lo que
   * se parece al fondo (las esquinas), luego elige el color de la paleta más votado.
   */
  fun colorPrincipal(archivo: File): String {
    val original = cargar(archivo, 200) ?: return "gris"
    val bmp = Bitmap.createScaledBitmap(original, 48, 48, true)
    fun rgb(x: Int, y: Int): Triple<Int, Int, Int> {
      val c = bmp.getPixel(x, y)
      return Triple((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
    }
    if (esRecorte(archivo.name)) {
      // Sin fondo: votan todos los píxeles de la prenda.
      val votos = HashMap<String, Int>()
      for (y in 0 until 48) for (x in 0 until 48) {
        if ((bmp.getPixel(x, y) ushr 24) < 200) continue
        val (r, g, b) = rgb(x, y)
        val clave = Colores.masCercano(r, g, b).clave
        votos[clave] = (votos[clave] ?: 0) + 1
      }
      if (votos.isNotEmpty()) return elegir(votos)
    }
    val esquinas = listOf(rgb(1, 1), rgb(46, 1), rgb(1, 46), rgb(46, 46))
    val fondo = Triple(esquinas.map { it.first }.average(), esquinas.map { it.second }.average(), esquinas.map { it.third }.average())

    val votos = HashMap<String, Int>()
    for (y in 12 until 36) for (x in 12 until 36) {
      val (r, g, b) = rgb(x, y)
      val pareceFondo = abs(r - fondo.first) + abs(g - fondo.second) + abs(b - fondo.third) < 45
      if (pareceFondo) continue
      val clave = Colores.masCercano(r, g, b).clave
      votos[clave] = (votos[clave] ?: 0) + 1
    }
    if (votos.isEmpty()) {
      val (r, g, b) = rgb(24, 24)
      return Colores.masCercano(r, g, b).clave
    }
    return elegir(votos)
  }

  private fun elegir(votos: Map<String, Int>): String {
    val total = votos.values.sum()
    val vivos = votos.filterKeys { !Colores.de(it).neutro }
    // Muchos colores vivos repartidos = estampado.
    if (vivos.size >= 3 && vivos.values.sorted().takeLast(3).first() > total * 0.12) return Colores.ESTAMPADO
    return votos.maxBy { it.value }.key
  }
}
