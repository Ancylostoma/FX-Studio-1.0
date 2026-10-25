package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancylostoma.quemepongo.R

private val Claro = lightColorScheme(
  primary = Color(0xFF7A4B36),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFF3DED2),
  onPrimaryContainer = Color(0xFF3A1E12),
  secondary = Color(0xFFB4655A),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF8E4DF),
  onSecondaryContainer = Color(0xFF41140F),
  tertiary = Color(0xFF5E6B3A),
  background = Color(0xFFFBF7F2),
  onBackground = Color(0xFF221A16),
  surface = Color(0xFFFBF7F2),
  onSurface = Color(0xFF221A16),
  surfaceVariant = Color(0xFFF2EAE3),
  onSurfaceVariant = Color(0xFF6B5C54),
  surfaceContainer = Color(0xFFF5EEE8),
  surfaceContainerHigh = Color(0xFFF0E7E0),
  outline = Color(0xFFB3A39A),
  outlineVariant = Color(0xFFE3D8D0),
)

private val Oscuro = darkColorScheme(
  primary = Color(0xFFE8B9A1),
  onPrimary = Color(0xFF452A1C),
  primaryContainer = Color(0xFF5E3826),
  onPrimaryContainer = Color(0xFFF3DED2),
  secondary = Color(0xFFF1B5AB),
  onSecondary = Color(0xFF5A1F18),
  secondaryContainer = Color(0xFF5C2E27),
  onSecondaryContainer = Color(0xFFF8DAD4),
  tertiary = Color(0xFFC3D19A),
  background = Color(0xFF17120F),
  onBackground = Color(0xFFEFE0D8),
  surface = Color(0xFF17120F),
  onSurface = Color(0xFFEFE0D8),
  surfaceVariant = Color(0xFF2E2622),
  onSurfaceVariant = Color(0xFFCDB9AF),
  surfaceContainer = Color(0xFF221B18),
  surfaceContainerHigh = Color(0xFF2B231F),
  outline = Color(0xFF8C7A71),
  outlineVariant = Color(0xFF3F3530),
)

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
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun TemaQueMePongo(contenido: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
    typography = Tipos,
    shapes = Formas,
    content = contenido,
  )
}
