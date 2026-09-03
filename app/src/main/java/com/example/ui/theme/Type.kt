package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Jost, la tipografía de la marca.
 *
 * Es geométrica y de trazo parejo, la misma familia de formas que el FX del
 * logotipo del estudio. Va incrustada en la app —no se descarga— así que
 * funciona sin internet y sin servicios de Google.
 *
 * Licencia SIL Open Font 1.1; la nota está en licencias/TIPOGRAFIA-JOST.md.
 */
val Jost =
  FontFamily(
    Font(R.font.jost_light, FontWeight.Light),
    Font(R.font.jost_regular, FontWeight.Normal),
    Font(R.font.jost_medium, FontWeight.Medium),
    Font(R.font.jost_semibold, FontWeight.SemiBold),
    Font(R.font.jost_bold, FontWeight.Bold),
  )

// Escala tipográfica aumentada alrededor de un 25% sobre la de Material 3.
// La app se usa de pie en el estudio y con clientes de todas las edades, así
// que se prioriza que todo se lea de un vistazo y a distancia de brazo.
//
// Jost manda en títulos, precios y botones, que es donde se reconoce la marca.
// Los párrafos largos —el contrato, las descripciones de los paquetes— se
// quedan con la letra del sistema, que está pensada para leer seguido en
// pantalla y se lee mejor a tamaño pequeño.

val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Bold,
      fontSize = 64.sp,
      lineHeight = 72.sp,
      letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Bold,
      fontSize = 52.sp,
      lineHeight = 60.sp,
    ),
    displaySmall = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Bold,
      fontSize = 42.sp,
      lineHeight = 50.sp,
    ),
    headlineLarge = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Bold,
      fontSize = 38.sp,
      lineHeight = 46.sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Bold,
      fontSize = 32.sp,
      lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.SemiBold,
      fontSize = 27.sp,
      lineHeight = 35.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.SemiBold,
      fontSize = 25.sp,
      lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Medium,
      fontSize = 20.sp,
      lineHeight = 27.sp,
      letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Medium,
      fontSize = 17.sp,
      lineHeight = 23.sp,
      letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 19.sp,
      lineHeight = 28.sp,
      letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 17.sp,
      lineHeight = 25.sp,
      letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
      fontFamily = FontFamily.Default,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.SemiBold,
      fontSize = 17.sp,
      lineHeight = 23.sp,
      letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Medium,
      fontSize = 15.sp,
      lineHeight = 21.sp,
      letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
      fontFamily = Jost,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.5.sp,
    ),
  )
