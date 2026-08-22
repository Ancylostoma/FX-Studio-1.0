package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FxColorScheme =
  lightColorScheme(
    primary = FxRose,
    onPrimary = FxWhite,
    primaryContainer = FxRoseLight,
    onPrimaryContainer = FxRoseDeep,
    secondary = FxGold,
    onSecondary = FxGoldDark,
    secondaryContainer = FxGoldLight,
    onSecondaryContainer = FxGoldDark,
    tertiary = FxBrown,
    onTertiary = FxWhite,
    tertiaryContainer = FxBrownLight,
    onTertiaryContainer = FxBrownDeep,
    background = FxCream,
    onBackground = FxBrownDeep,
    surface = FxSurface,
    onSurface = FxBrownDeep,
    surfaceVariant = FxSurfaceVariant,
    onSurfaceVariant = FxOnSurfaceVariant,
    outline = FxOutline,
    outlineVariant = FxOutlineVariant,
    error = FxError,
    onError = FxWhite,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // El color dinámico queda desactivado para respetar la paleta del catálogo.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      // Se usa la misma paleta clara aunque el teléfono esté en modo oscuro:
      // el cliente debe ver siempre los colores de la marca.
      else -> FxColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
