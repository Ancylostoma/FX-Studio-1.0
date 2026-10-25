package com.ancylostoma.quemepongo

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Abre la app de verdad (sin teléfono) y la recorre como un usuario: si algo truena, falla aquí. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArranqueTest {
  @get:Rule val app = createAndroidComposeRule<MainActivity>()

  private fun esperarTexto(texto: String) {
    try {
      app.waitUntil(15_000) { app.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: ComposeTimeoutException) {
      // Mostrar lo que sí hay en pantalla para saber qué pasó.
      throw AssertionError("No apareció \"$texto\". En pantalla:\n" + app.onRoot().printToString(maxDepth = 40), e)
    }
  }

  private fun tocar(texto: String) {
    esperarTexto(texto)
    val nodo = app.onAllNodesWithText(texto)[0]
    // Como una persona: si está más abajo, primero se desplaza (las pestañas no se desplazan).
    runCatching { nodo.performScrollTo() }
    nodo.performClick()
    app.waitForIdle()
  }

  @Test
  @Config(qualifiers = "es")
  fun recorreLaAppComoUnUsuario() {
    // Bienvenida en pasos.
    tocar("Empezar")
    tocar("Siguiente") // maniquí
    tocar("Saltar") // estilos
    tocar("Ahora no") // ciudad
    tocar("No, gracias") // aviso diario
    tocar("Lo haré luego") // primera prenda

    // Hoy: un look listo y el editor.
    esperarTexto("Me lo pongo")
    tocar("Editar")
    esperarTexto("Editar look")
    // Atrás cierra el editor y vuelve a Hoy.
    app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
    app.waitForIdle()
    esperarTexto("Tu look de hoy")

    // Agregar una prenda sin foto en tres toques.
    tocar("Agregar")
    tocar("Continuar sin foto")
    tocar("Arriba")
    tocar("Guardar")

    tocar("Armario")
    esperarTexto("1 prenda")
    tocar("Descubrir")
    esperarTexto("Lo que se lleva ahora")
    tocar("Yo")
    esperarTexto("Aún no guardaste looks")
  }

  /** La misma app en inglés (el idioma por defecto para el resto del mundo). */
  @Test
  @Config(qualifiers = "en")
  fun funcionaEnIngles() {
    tocar("Start")
    tocar("Next")
    tocar("Skip")
    tocar("Not now")
    tocar("No, thanks")
    tocar("I'll do it later")
    esperarTexto("Your look for today")
    esperarTexto("I'll wear it")
    tocar("Discover")
    esperarTexto("What's in style now")
  }
}
