package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
  primary = Color(0xFF7A4B36),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFF3DED2),
  onPrimaryContainer = Color(0xFF3A1E12),
  secondary = Color(0xFFB4655A),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF8DAD4),
  onSecondaryContainer = Color(0xFF41140F),
  tertiary = Color(0xFF5E6B3A),
  background = Color(0xFFFBF7F2),
  onBackground = Color(0xFF221A16),
  surface = Color(0xFFFBF7F2),
  onSurface = Color(0xFF221A16),
  surfaceVariant = Color(0xFFF1E7DF),
  onSurfaceVariant = Color(0xFF52443C),
  outline = Color(0xFF85736A),
)

private val Oscuro = darkColorScheme(
  primary = Color(0xFFE8B9A1),
  onPrimary = Color(0xFF452A1C),
  primaryContainer = Color(0xFF5E3826),
  onPrimaryContainer = Color(0xFFF3DED2),
  secondary = Color(0xFFF1B5AB),
  onSecondary = Color(0xFF5A1F18),
  secondaryContainer = Color(0xFF7A3A31),
  onSecondaryContainer = Color(0xFFF8DAD4),
  tertiary = Color(0xFFC3D19A),
  background = Color(0xFF1A1411),
  onBackground = Color(0xFFEFE0D8),
  surface = Color(0xFF1A1411),
  onSurface = Color(0xFFEFE0D8),
  surfaceVariant = Color(0xFF3A2F29),
  onSurfaceVariant = Color(0xFFD7C2B8),
  outline = Color(0xFFA08D84),
)

@Composable
fun TemaQueMePongo(contenido: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}
