package com.ancylostoma.quemepongo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ancylostoma.quemepongo.data.Fotos
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Importar una foto como lo haría la cámara o la galería. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FotosTest {
  @Test
  fun importaUnaFotoYDetectaSuColor() {
    val contexto = ApplicationProvider.getApplicationContext<Context>()
    // Una "camiseta" roja sobre fondo blanco.
    val bmp = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
    Canvas(bmp).apply {
      drawColor(Color.WHITE)
      drawRect(400f, 250f, 1200f, 950f, android.graphics.Paint().apply { color = Color.rgb(200, 30, 35) })
    }
    val origen = File(contexto.cacheDir, "foto.jpg")
    origen.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }

    val carpeta = File(contexto.filesDir, "prendas").apply { mkdirs() }
    val nombre = Fotos.importar(contexto, Uri.fromFile(origen), carpeta, "prueba")
    assertNotNull("La foto no se pudo importar", nombre)
    val guardada = File(carpeta, nombre!!)
    assertTrue(guardada.exists())
    // Se reduce a 1280 px como máximo.
    val leida = Fotos.cargar(guardada, 2000)!!
    assertTrue(maxOf(leida.width, leida.height) <= 1280)
    assertEquals("rojo", Fotos.colorPrincipal(guardada))
  }
}
