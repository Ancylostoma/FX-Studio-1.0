package com.ancylostoma.quemepongo.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Modo
import com.ancylostoma.quemepongo.data.Paleta

/** Los dos tonos de cada paleta, para el modo claro y el oscuro. */
private class Tonos(val claro: Color, val claroAcento: Color, val oscuro: Color, val oscuroAcento: Color)

// Comprobados con la norma WCAG: el texto sobre los botones y los colores
// sobre el fondo se leen bien (contraste de 4,5 o más) en las dos versiones.
private fun Paleta.tonos() = when (this) {
  Paleta.CORAL -> Tonos(Color(0xFFC0391B), Color(0xFFCC441F), Color(0xFFFF9B7A), Color(0xFFFFB48F))
  Paleta.ROSA -> Tonos(Color(0xFFC2185B), Color(0xFFD6336C), Color(0xFFFF8FB6), Color(0xFFFFADC9))
  Paleta.LAVANDA -> Tonos(Color(0xFF6B3FD0), Color(0xFF8A4FE0), Color(0xFFC4A8FF), Color(0xFFDCC8FF))
  Paleta.OCEANO -> Tonos(Color(0xFF0B6A96), Color(0xFF0E7FB0), Color(0xFF7FCDF2), Color(0xFFA6DDF7))
  Paleta.TERRACOTA -> Tonos(Color(0xFF7A4B36), Color(0xFFA0563F), Color(0xFFE8B9A1), Color(0xFFF1C5AE))
  Paleta.MONOCROMO -> Tonos(Color(0xFF141414), Color(0xFF3A3A3A), Color(0xFFF2F2F2), Color(0xFFCFCFCF))
}

/** Color de muestra de cada paleta, para elegirla en Ajustes. */
fun Paleta.muestra(oscuro: Boolean): List<Color> = tonos().let { if (oscuro) listOf(it.oscuro, it.oscuroAcento) else listOf(it.claro, it.claroAcento) }

// Fondos casi neutros con un toque del color elegido; el texto, casi negro o casi blanco.
private fun esquemaClaro(p: Paleta): ColorScheme {
  val t = p.tonos()
  fun neutro(base: Long, tinte: Float) = lerp(Color(base), t.claro, tinte)
  return lightColorScheme(
    primary = t.claro,
    onPrimary = Color.White,
    primaryContainer = lerp(t.claro, Color.White, 0.85f),
    onPrimaryContainer = lerp(t.claro, Color.Black, 0.55f),
    secondary = t.claroAcento,
    onSecondary = Color.White,
    secondaryContainer = lerp(t.claroAcento, Color.White, 0.87f),
    onSecondaryContainer = lerp(t.claroAcento, Color.Black, 0.6f),
    tertiary = t.claroAcento,
    onTertiary = Color.White,
    tertiaryContainer = lerp(t.claroAcento, Color.White, 0.87f),
    onTertiaryContainer = lerp(t.claroAcento, Color.Black, 0.6f),
    background = neutro(0xFFFCFBFA, 0.03f),
    onBackground = Color(0xFF15120F),
    surface = neutro(0xFFFCFBFA, 0.03f),
    onSurface = Color(0xFF15120F),
    surfaceVariant = neutro(0xFFEDE9E5, 0.05f),
    onSurfaceVariant = Color(0xFF47413B),
    surfaceTint = t.claro,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = neutro(0xFFF7F5F3, 0.04f),
    surfaceContainer = neutro(0xFFF2EFEC, 0.05f),
    surfaceContainerHigh = neutro(0xFFEBE7E3, 0.06f),
    surfaceContainerHighest = neutro(0xFFE3DED9, 0.07f),
    outline = Color(0xFF756D66),
    outlineVariant = neutro(0xFFD3CCC5, 0.05f),
  )
}

private fun esquemaOscuro(p: Paleta): ColorScheme {
  val t = p.tonos()
  fun neutro(base: Long, tinte: Float) = lerp(Color(base), t.oscuro, tinte)
  return darkColorScheme(
    primary = t.oscuro,
    onPrimary = lerp(t.oscuro, Color.Black, 0.82f),
    primaryContainer = lerp(t.oscuro, Color.Black, 0.62f),
    onPrimaryContainer = lerp(t.oscuro, Color.White, 0.75f),
    secondary = t.oscuroAcento,
    onSecondary = lerp(t.oscuroAcento, Color.Black, 0.82f),
    secondaryContainer = lerp(t.oscuroAcento, Color.Black, 0.65f),
    onSecondaryContainer = lerp(t.oscuroAcento, Color.White, 0.75f),
    tertiary = t.oscuroAcento,
    onTertiary = lerp(t.oscuroAcento, Color.Black, 0.82f),
    tertiaryContainer = lerp(t.oscuroAcento, Color.Black, 0.65f),
    onTertiaryContainer = lerp(t.oscuroAcento, Color.White, 0.75f),
    background = neutro(0xFF0F0F10, 0.04f),
    onBackground = Color(0xFFF3F1EF),
    surface = neutro(0xFF0F0F10, 0.04f),
    onSurface = Color(0xFFF3F1EF),
    surfaceVariant = neutro(0xFF2A2A2D, 0.06f),
    onSurfaceVariant = Color(0xFFCBC5BF),
    surfaceTint = t.oscuro,
    surfaceContainerLowest = Color(0xFF0A0A0B),
    surfaceContainerLow = neutro(0xFF161618, 0.05f),
    surfaceContainer = neutro(0xFF1B1B1D, 0.06f),
    surfaceContainerHigh = neutro(0xFF252528, 0.07f),
    surfaceContainerHighest = neutro(0xFF303033, 0.08f),
    outline = Color(0xFF948D87),
    outlineVariant = neutro(0xFF423E3A, 0.05f),
  )
}

/** true si la app se está viendo en modo oscuro (por el teléfono o porque el usuario lo eligió). */
val LocalOscuro = staticCompositionLocalOf { false }

/** Títulos con aire de revista de moda. */
val Revista = FontFamily(Font(R.font.dm_serif_display))

private val Jost = FontFamily(
  Font(R.font.jost_regular, FontWeight.Normal),
  Font(R.font.jost_medium, FontWeight.Medium),
  Font(R.font.jost_semibold, FontWeight.SemiBold),
  Font(R.font.jost_bold, FontWeight.Bold),
)

private val Tipos = Typography().let { t ->
  Typography(
    displayLarge = t.displayLarge.copy(fontFamily = Revista),
    displayMedium = t.displayMedium.copy(fontFamily = Revista),
    displaySmall = t.displaySmall.copy(fontFamily = Revista),
    headlineLarge = t.headlineLarge.copy(fontFamily = Revista),
    headlineMedium = t.headlineMedium.copy(fontFamily = Revista, fontSize = 30.sp),
    headlineSmall = t.headlineSmall.copy(fontFamily = Revista),
    titleLarge = t.titleLarge.copy(fontFamily = Revista, fontSize = 24.sp),
    titleMedium = t.titleMedium.copy(fontFamily = Jost, fontWeight = FontWeight.SemiBold),
    titleSmall = t.titleSmall.copy(fontFamily = Jost, fontWeight = FontWeight.SemiBold),
    bodyLarge = t.bodyLarge.copy(fontFamily = Jost),
    bodyMedium = t.bodyMedium.copy(fontFamily = Jost),
    bodySmall = t.bodySmall.copy(fontFamily = Jost),
    labelLarge = t.labelLarge.copy(fontFamily = Jost, fontWeight = FontWeight.SemiBold),
    labelMedium = t.labelMedium.copy(fontFamily = Jost, fontWeight = FontWeight.Medium),
    labelSmall = t.labelSmall.copy(fontFamily = Jost, fontWeight = FontWeight.Medium),
  )
}

private val Formas = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(26.dp),
)

@Composable
fun TemaQueMePongo(modo: Modo = Modo.SISTEMA, paleta: Paleta = Paleta.CORAL, contenido: @Composable () -> Unit) {
  val oscuro = when (modo) {
    Modo.SISTEMA -> isSystemInDarkTheme()
    Modo.CLARO -> false
    Modo.OSCURO -> true
  }
  val esquema = remember(oscuro, paleta) { if (oscuro) esquemaOscuro(paleta) else esquemaClaro(paleta) }
  // Los íconos de la barra de estado (hora, batería) tienen que verse sobre el fondo elegido.
  val vista = LocalView.current
  if (!vista.isInEditMode) {
    SideEffect {
      val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
      WindowCompat.getInsetsController(ventana, vista).apply {
        isAppearanceLightStatusBars = !oscuro
        isAppearanceLightNavigationBars = !oscuro
      }
    }
  }
  CompositionLocalProvider(LocalOscuro provides oscuro) {
    MaterialTheme(colorScheme = esquema, typography = Tipos, shapes = Formas, content = contenido)
  }
}
