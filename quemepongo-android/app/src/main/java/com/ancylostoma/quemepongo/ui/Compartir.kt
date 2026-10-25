package com.ancylostoma.quemepongo.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Cuerpo
import com.ancylostoma.quemepongo.data.Fotos
import com.ancylostoma.quemepongo.data.Prenda
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * El look como imagen para WhatsApp o Instagram (formato 4:5): el maniquí a la
 * izquierda, las prendas a la derecha y el nombre de la app abajo.
 * Cada look compartido es publicidad gratis.
 */
object Compartir {
  private const val ANCHO = 1080
  private const val ALTO = 1350

  suspend fun look(contexto: Context, prendas: List<Prenda>, cuerpo: Cuerpo, foto: (String?) -> File?, titulo: String) {
    val archivo = withContext(Dispatchers.IO) { imagen(contexto, prendas, cuerpo, foto, titulo) }
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
    val enviar = Intent(Intent.ACTION_SEND)
      .setType("image/png")
      .putExtra(Intent.EXTRA_STREAM, uri)
      .putExtra(
        Intent.EXTRA_TEXT,
        contexto.getString(R.string.compartir_texto, "https://play.google.com/store/apps/details?id=${contexto.packageName}"),
      )
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    contexto.startActivity(Intent.createChooser(enviar, contexto.getString(R.string.compartir_elegir)))
  }

  private fun imagen(contexto: Context, prendas: List<Prenda>, cuerpo: Cuerpo, foto: (String?) -> File?, titulo: String): File {
    val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
    val lienzo = android.graphics.Canvas(bmp)
    lienzo.drawColor(0xFFFBF7F2.toInt())

    val serif = ResourcesCompat.getFont(contexto, R.font.dm_serif_display) ?: Typeface.SERIF
    val jost = ResourcesCompat.getFont(contexto, R.font.jost_medium) ?: Typeface.SANS_SERIF
    val tinta = 0xFF221A16.toInt()
    val suave = 0xFF6B5C54.toInt()

    lienzo.drawText(titulo, 70f, 130f, Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 76f; color = tinta })

    // Tarjeta del maniquí.
    val tarjeta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2EAE3.toInt() }
    lienzo.drawRoundRect(RectF(60f, 180f, 640f, 1230f), 48f, 48f, tarjeta)
    val fotos = prendas.mapNotNull { p -> p.foto?.let { n -> foto(n)?.let { f -> Fotos.cargar(f, 700)?.let { n to it } } } }.toMap()
    val imagenes: Map<String, ImageBitmap> = fotos.mapValues { it.value.asImageBitmap() }
    lienzo.save()
    lienzo.translate(90f, 220f)
    CanvasDrawScope().draw(Density(2.6f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(lienzo), Size(520f, 970f)) {
      dibujarManiqui(prendas, cuerpo, imagenes)
    }
    lienzo.restore()

    // Columna de prendas: foto (o color) y nombre.
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = jost; textSize = 30f; color = tinta }
    val visibles = prendas.take(5)
    val alto = 1050f / visibles.size.coerceAtLeast(1)
    visibles.forEachIndexed { i, p ->
      val y = 180f + i * alto
      val caja = RectF(680f, y + 10f, 1020f, y + alto - 60f)
      lienzo.drawRoundRect(caja, 28f, 28f, tarjeta)
      val f = p.foto?.let { fotos[it] }
      if (f != null) {
        val escala = minOf((caja.width() - 30f) / f.width, (caja.height() - 30f) / f.height)
        val w = f.width * escala
        val h = f.height * escala
        lienzo.drawBitmap(f, null, RectF(caja.centerX() - w / 2, caja.centerY() - h / 2, caja.centerX() + w / 2, caja.centerY() + h / 2), Paint(Paint.FILTER_BITMAP_FLAG))
      } else {
        val color = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colores.de(p.color).argb.toInt() }
        lienzo.drawCircle(caja.centerX(), caja.centerY(), minOf(caja.width(), caja.height()) / 3f, color)
      }
      lienzo.drawText(recortar(contexto.nombre(p), 22), 690f, y + alto - 22f, pincel)
    }

    val pie = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 44f; color = 0xFF7A4B36.toInt() }
    lienzo.drawText(contexto.getString(R.string.app_name), 70f, 1300f, pie)
    val nota = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = jost; textSize = 28f; color = suave; textAlign = Paint.Align.RIGHT }
    lienzo.drawText(contexto.getString(R.string.compartir_lema), 1010f, 1298f, nota)

    val carpeta = File(contexto.cacheDir, "compartir").apply { mkdirs() }
    val archivo = File(carpeta, "look.png")
    archivo.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return archivo
  }

  private fun recortar(texto: String, max: Int) = if (texto.length <= max) texto else texto.take(max - 1) + "…"
}
