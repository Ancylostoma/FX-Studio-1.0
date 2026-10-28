package com.ancylostoma.quemepongo

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Fotos de las pantallas principales en modo claro y oscuro, en un teléfono de
 * tamaño normal. GitHub Actions las guarda en play-store/capturas: sirven para
 * la ficha de Google Play y para revisar el diseño sin instalar la app.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CapturasTest {
  @get:Rule val app = createAndroidComposeRule<MainActivity>()

  private val carpeta = File("build/capturas").apply { mkdirs() }

  private fun hay(texto: String, sub: Boolean = false) = app.onAllNodesWithText(texto, substring = sub).fetchSemanticsNodes().isNotEmpty()

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
    // captureToImage no termina en Robolectric: se le pide a la ventana que se dibuje.
    runCatching { dibujarVentana() }
      .onSuccess { imagen -> File(carpeta, "$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) } }
      .onFailure { System.err.println("No se pudo guardar la captura $nombre: $it") }
  }

  /** La pantalla tal como se ve, dibujada en un bitmap. */
  private fun dibujarVentana(): Bitmap {
    var imagen: Bitmap? = null
    app.runOnUiThread {
      val vista = app.activity.window.decorView
      imagen = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888).also { vista.draw(Canvas(it)) }
    }
    return imagen!!
  }

  private fun recorrer(sufijo: String) {
    sufijoActual = sufijo
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
    if (sufijo == "claro") buscarCiudad()
    tocar("Armario")
    foto("03-armario-$sufijo")
    tocar("Descubrir")
    foto("04-descubrir-$sufijo")
    // Más abajo: categorías y la cuadrícula de prendas.
    runCatching {
      app.onNode(hasScrollToNodeAction() and hasAnyDescendant(hasText("Lo que se lleva ahora")))
        .performScrollToNode(hasText("Para ti"))
      foto("04b-tienda-$sufijo")
    }
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

  /**
   * La búsqueda de verdad (en GitHub Actions hay internet): si el teléfono no
   * tiene geocodificador, responde Photon. No falla si no hay red; solo saca la foto.
   */
  private fun buscarCiudad() = runCatching {
    app.onAllNodesWithText("Elige tu ciudad", substring = true)[0].performClick()
    app.waitForIdle()
    app.onAllNodes(hasSetTextAction())[0].performTextInput("Valencia")
    app.onAllNodes(hasSetTextAction())[0].performImeAction()
    runCatching { app.waitUntil(25_000) { hay("¿Cuál es la tuya?") || hay("No encontré", sub = true) } }
    val resultados = app.onAllNodesWithText("Valencia").fetchSemanticsNodes().size - 1
    System.err.println("Búsqueda de ciudades: $resultados resultados")
    if (resultados > 0) {
      // El primero es el campo de texto; después vienen los resultados.
      app.onAllNodesWithText("Valencia")[1].performClick()
      app.waitForIdle()
    }
    tocar("Listo")
    // El tiempo de la ciudad elegida aparece con su temperatura ("°").
    runCatching { app.waitUntil(20_000) { hay("°", sub = true) } }
    foto("08-ciudad-$sufijoActual")
  }.onFailure { System.err.println("Búsqueda de ciudades: la prueba no pudo seguir ($it)") }

  private var sufijoActual = ""

  @Test
  @Config(qualifiers = "es-w393dp-h852dp-xhdpi")
  fun claro() = recorrer("claro")

  @Test
  @Config(qualifiers = "es-w393dp-h852dp-night-xhdpi")
  fun oscuro() = recorrer("oscuro")
}
