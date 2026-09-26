package com.example.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * El hueco que ocupan las barras del sistema (la de arriba y la de los
 * botones de Android o la barra de aplicaciones de la tableta), medido en
 * la pantalla principal.
 *
 * Las ventanas que se abren encima (contrato, resumen del pedido, "Agregar
 * algo más", resumen del día) son ventanas aparte, y en algunas tabletas
 * Android no les dice dónde están las barras: el último botón quedaba debajo
 * de los botones del sistema. Por eso la pantalla principal, donde la medida
 * sí llega bien, la reparte a todas por aquí.
 */
val LocalBarrasSistema = compositionLocalOf { PaddingValues(0.dp) }

/** Ventana a pantalla completa: los márgenes los pone [margenBarrasSistema]. */
val VentanaCompleta = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false
)

/**
 * Deja libre el sitio de las barras del sistema. Toma el mayor entre lo que
 * dice la propia ventana y lo que midió la pantalla principal, así sirve en
 * los dos casos sin sumar dos veces el mismo hueco.
 */
@Composable
fun Modifier.margenBarrasSistema(): Modifier {
    val propias = WindowInsets.safeDrawing.asPaddingValues()
    val app = LocalBarrasSistema.current
    val dir = LocalLayoutDirection.current
    return this.padding(
        start = maxOf(propias.calculateStartPadding(dir), app.calculateStartPadding(dir)),
        top = maxOf(propias.calculateTopPadding(), app.calculateTopPadding()),
        end = maxOf(propias.calculateEndPadding(dir), app.calculateEndPadding(dir)),
        bottom = maxOf(propias.calculateBottomPadding(), app.calculateBottomPadding())
    )
}
