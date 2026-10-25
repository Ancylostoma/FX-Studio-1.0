package com.ancylostoma.quemepongo

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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

/** Abre la app de verdad (sin teléfono) y recorre todas las pestañas: si algo truena al arrancar, falla aquí. */
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

  @Test
  fun abreLaBienvenidaYRecorreLasPestanas() {
    esperarTexto("Empezar")
    app.onNodeWithText("Empezar").performScrollTo().performClick()
    esperarTexto("¿Qué me pongo hoy?")
    // Que el motor haya armado un look y se dibuje el maniquí.
    esperarTexto("Me lo pongo")

    for (pestana in listOf("Armario", "Probador", "Tendencias", "Perfil")) {
      app.onNodeWithText(pestana).performClick()
      app.waitForIdle()
    }
    esperarTexto("Mi estilo")

    // El formulario de prenda nueva (cámara y galería) también debe abrir.
    app.onNodeWithText("Armario").performClick()
    esperarTexto("Agregar prenda")
    app.onAllNodesWithText("Agregar prenda", useUnmergedTree = true)[0].performClick()
    esperarTexto("Nueva prenda")
  }
}
