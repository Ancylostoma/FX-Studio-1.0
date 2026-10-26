package com.ancylostoma.quemepongo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Fotos de las pantallas principales en modo claro y oscuro, en un teléfono de
 * tamaño normal. GitHub Actions las sube como "capturas" para revisar el diseño
 * sin instalar la app.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CapturasTest {
  @get:Rule val app = createAndroidComposeRule<MainActivity>()

  private val carpeta = File("build/capturas").apply { mkdirs() }

  private fun hay(texto: String) = app.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()

  private fun tocar(texto: String) {
    app.waitUntil(15_000) { hay(texto) }
    val nodo = app.onAllNodesWithText(texto)[0]
    runCatching { nodo.performScrollTo() }
    nodo.performClick()
    app.waitForIdle()
  }

  private fun foto(nombre: String) {
    app.waitForIdle()
    // Que terminen de vestirse los maniquíes.
    app.mainClock.advanceTimeBy(3_000)
    app.waitForIdle()
    runCatching {
      val imagen = app.onRoot().captureToImage().asAndroidBitmap()
      File(carpeta, "$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }.onFailure { println("No se pudo guardar la captura $nombre: $it") }
  }

  private fun recorrer(sufijo: String) {
    app.waitUntil(15_000) { hay("Empezar") || hay("Tu look de hoy") }
    if (hay("Empezar")) {
      foto("01-bienvenida-$sufijo")
      tocar("Empezar")
      tocar("Siguiente")
      tocar("Saltar")
      tocar("Ahora no")
      tocar("No, gracias")
      tocar("Lo haré luego")
    }
    app.waitUntil(15_000) { hay("Me lo pongo") }
    foto("02-hoy-$sufijo")
    tocar("Armario")
    foto("03-armario-$sufijo")
    tocar("Descubrir")
    foto("04-descubrir-$sufijo")
    tocar("Yo")
    foto("05-yo-$sufijo")
    // El botón de Ajustes es un ícono: se busca por su descripción.
    app.onAllNodesWithContentDescription("Ajustes")[0].performClick()
    app.waitForIdle()
    foto("06-ajustes-$sufijo")
    app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
    tocar("Planear la semana")
    // Premium, o la semana si otra prueba ya activó el modo prueba.
    foto("07-semana-$sufijo")
  }

  @Test
  @Config(qualifiers = "es-w393dp-h852dp-xhdpi")
  fun claro() = recorrer("claro")

  @Test
  @Config(qualifiers = "es-w393dp-h852dp-xhdpi-night")
  fun oscuro() = recorrer("oscuro")
}
