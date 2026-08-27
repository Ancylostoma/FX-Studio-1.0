package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Los cuatro temas de la app.
 *
 * El primero es el de siempre, el del catálogo impreso. Los otros tres son
 * combinaciones de rojo, blanco, negro y dorado: no un tema por color, sino
 * tres mezclas distintas de los cuatro, cambiando cuál manda en cada una.
 */

// ---------------------------------------------------------------------------
// 2. Marfil y Carmín — fondo blanco, manda el ROJO, el dorado acompaña.
// ---------------------------------------------------------------------------
private val CarminRojo = Color(0xFF9E1B32)
private val CarminRojoClaro = Color(0xFFF9DDE1)
private val CarminRojoHondo = Color(0xFF3D0711)
private val CarminOro = Color(0xFFB8912F)
private val CarminOroClaro = Color(0xFFF7ECC9)
private val CarminOroHondo = Color(0xFF3B2E00)
private val CarminNegro = Color(0xFF23201F)
private val CarminNegroClaro = Color(0xFFE4E1DE)

private val MarfilCarmin = lightColorScheme(
    primary = CarminRojo,
    onPrimary = Color.White,
    primaryContainer = CarminRojoClaro,
    onPrimaryContainer = CarminRojoHondo,
    secondary = CarminOro,
    onSecondary = Color.White,
    secondaryContainer = CarminOroClaro,
    onSecondaryContainer = CarminOroHondo,
    tertiary = CarminNegro,
    onTertiary = Color.White,
    tertiaryContainer = CarminNegroClaro,
    onTertiaryContainer = Color(0xFF1A1817),
    background = Color(0xFFFFFBF9),
    onBackground = CarminNegro,
    surface = Color(0xFFFFFFFF),
    onSurface = CarminNegro,
    surfaceVariant = Color(0xFFF3EAEA),
    onSurfaceVariant = Color(0xFF544545),
    outline = Color(0xFFB09A9A),
    outlineVariant = Color(0xFFE7DADA),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

// ---------------------------------------------------------------------------
// 3. Ónix y Oro — fondo negro, manda el DORADO, el rojo como acento.
// ---------------------------------------------------------------------------
private val OnixOro = Color(0xFFD4AF37)
private val OnixOroHondo = Color(0xFF3A2E00)
private val OnixOroSuave = Color(0xFFF6E4A6)
private val OnixRojo = Color(0xFFE5707A)
private val OnixRojoHondo = Color(0xFF5A1420)
private val OnixRojoSuave = Color(0xFFFFD9DC)

private val OnixYOro = darkColorScheme(
    primary = OnixOro,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = OnixOroHondo,
    onPrimaryContainer = OnixOroSuave,
    secondary = OnixRojo,
    onSecondary = Color(0xFF3D0711),
    secondaryContainer = OnixRojoHondo,
    onSecondaryContainer = OnixRojoSuave,
    tertiary = Color(0xFFE8E4DC),
    onTertiary = Color(0xFF23201C),
    tertiaryContainer = Color(0xFF3A3630),
    onTertiaryContainer = Color(0xFFEDE8E0),
    background = Color(0xFF121211),
    onBackground = Color(0xFFEDE8E0),
    surface = Color(0xFF1C1B1A),
    onSurface = Color(0xFFEDE8E0),
    surfaceVariant = Color(0xFF2A2724),
    onSurfaceVariant = Color(0xFFCFC7BA),
    outline = Color(0xFF8C8477),
    outlineVariant = Color(0xFF3A3630),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

// ---------------------------------------------------------------------------
// 4. Perla y Ónix — fondo blanco, manda el NEGRO, oro y rojo como acentos.
// ---------------------------------------------------------------------------
private val PerlaNegro = Color(0xFF1F1F1E)
private val PerlaNegroClaro = Color(0xFFE3E1DE)
private val PerlaOro = Color(0xFFB8912F)
private val PerlaOroClaro = Color(0xFFF5E8C8)
private val PerlaRojo = Color(0xFFA8202F)
private val PerlaRojoClaro = Color(0xFFFADCDF)

private val PerlaYOnix = lightColorScheme(
    primary = PerlaNegro,
    onPrimary = Color.White,
    primaryContainer = PerlaNegroClaro,
    onPrimaryContainer = Color(0xFF1A1A19),
    secondary = PerlaOro,
    onSecondary = Color.White,
    secondaryContainer = PerlaOroClaro,
    onSecondaryContainer = Color(0xFF3B2E00),
    tertiary = PerlaRojo,
    onTertiary = Color.White,
    tertiaryContainer = PerlaRojoClaro,
    onTertiaryContainer = Color(0xFF40060E),
    background = Color(0xFFFAFAF8),
    onBackground = PerlaNegro,
    surface = Color(0xFFFFFFFF),
    onSurface = PerlaNegro,
    surfaceVariant = Color(0xFFEFEDE9),
    onSurfaceVariant = Color(0xFF4A4741),
    outline = Color(0xFF8F8A82),
    outlineVariant = Color(0xFFDFDCD6),
    error = PerlaRojo,
    onError = Color.White,
)

/** Un tema con su nombre y los colores que se enseñan en el selector. */
data class TemaFx(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val esquema: ColorScheme,
    /** Muestras para el selector, en orden. */
    val muestras: List<Color>
)

object FxTemas {
    const val CLASICO = "clasico"
    const val MARFIL = "marfil"
    const val ONIX = "onix"
    const val PERLA = "perla"

    val TODOS: List<TemaFx> by lazy {
        listOf(
            TemaFx(
                id = CLASICO,
                nombre = "Rosa y Oro",
                descripcion = "El de siempre, el del catálogo impreso.",
                esquema = FxColorSchemeClasico,
                muestras = listOf(FxRose, FxGold, FxBrown, FxCream)
            ),
            TemaFx(
                id = MARFIL,
                nombre = "Marfil y Carmín",
                descripcion = "Fondo blanco, rojo intenso y detalles dorados.",
                esquema = MarfilCarmin,
                muestras = listOf(CarminRojo, CarminOro, CarminNegro, Color.White)
            ),
            TemaFx(
                id = ONIX,
                nombre = "Ónix y Oro",
                descripcion = "Fondo negro con dorado y toques de rojo.",
                esquema = OnixYOro,
                muestras = listOf(Color(0xFF121211), OnixOro, OnixRojo, Color(0xFFEDE8E0))
            ),
            TemaFx(
                id = PERLA,
                nombre = "Perla y Ónix",
                descripcion = "Blanco y negro sobrio, con oro y rojo de acento.",
                esquema = PerlaYOnix,
                muestras = listOf(PerlaNegro, PerlaOro, PerlaRojo, Color.White)
            )
        )
    }

    /** Devuelve el tema pedido; si el id no existe, el clásico. */
    fun porId(id: String): TemaFx = TODOS.firstOrNull { it.id == id } ?: TODOS.first()
}
