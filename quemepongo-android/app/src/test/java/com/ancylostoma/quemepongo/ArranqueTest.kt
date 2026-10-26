package com.ancylostoma.quemepongo

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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

    // Premium: sin pagar, abre la pantalla de Premium; en la compilación de prueba se activa a mano.
    tocar("Planear la semana")
    esperarTexto("Desbloquea todo")
    tocar("Modo prueba: activar Premium")
    tocar("Planear la semana")
    esperarTexto("Rehacer la semana")
    atras()
    tocar("Estadísticas del armario")
    esperarTexto("Veces usadas")
    atras()
    tocar("Maleta de viaje")
    esperarTexto("Ciudad de destino")
    // En una pantalla chica el botón queda debajo: la lista se desplaza hasta él.
    app.onNode(hasScrollToNodeAction() and hasAnyDescendant(hasText("Ciudad de destino")))
      .performScrollToNode(hasText("Armar mi maleta"))
    esperarTexto("Armar mi maleta")
    atras()
  }

  private fun atras() {
    app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
    app.waitForIdle()
  }

  /** La misma app en inglés (el idioma por defecto para el resto del mundo). */
  @Test
  @Config(qualifiers = "en")
  fun funcionaEnIngles() {
    // Las pruebas comparten los datos de la app: si la otra ya pasó la bienvenida, se salta.
    app.waitUntil(15_000) {
      app.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty() ||
        app.onAllNodesWithText("Your look for today").fetchSemanticsNodes().isNotEmpty()
    }
    if (app.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty()) {
      tocar("Start")
      tocar("Next")
      tocar("Skip")
      tocar("Not now")
      tocar("No, thanks")
      tocar("I'll do it later")
    }
    esperarTexto("Your look for today")
    esperarTexto("I'll wear it")
    tocar("Discover")
    esperarTexto("What's in style now")
  }
}
