package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Paleta original de la marca: rosa empolvado, dorado ámbar, marrón cálido y
 * crema, tomada del catálogo impreso. Es la que viene puesta de fábrica.
 */
internal val FxColorSchemeClasico =
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

/**
 * Tema de la app. El administrador elige la paleta desde su panel y se guarda
 * en la base de datos, así que el modo oscuro del teléfono no la cambia: el
 * cliente ve siempre los colores que el estudio haya decidido.
 */
@Composable
fun MyApplicationTheme(
  temaId: String = FxTemas.CLASICO,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = FxTemas.porId(temaId).esquema,
    typography = Typography,
    content = content,
  )
}
