package com.ancylostoma.quemepongo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Vista
import java.io.File

/** Dónde va cada pieza en el collage (fracciones del ancho y alto) y cuánto se gira. */
private data class Hueco(val x: Float, val y: Float, val ancho: Float, val alto: Float, val giro: Float)

/**
 * El look como "flat lay": las fotos sin fondo de cada prenda acomodadas como
 * sobre una mesa. Las piezas sin foto se dibujan con su silueta y su color.
 */
@Composable
fun Collage(prendas: List<Prenda>, foto: (String?) -> File?, modifier: Modifier = Modifier, claveAnimacion: Any? = null) {
  val vestido = prendas.firstOrNull { it.categoria == Categoria.VESTIDO }
  val accesorios = prendas.filter { it.categoria == Categoria.ACCESORIO }
  val huecos = buildList {
    prendas.forEach { p ->
      val h = when (p.categoria) {
        Categoria.SUPERIOR -> Hueco(0.04f, 0.02f, 0.56f, 0.46f, -3f)
        Categoria.INFERIOR -> Hueco(0.08f, 0.46f, 0.50f, 0.52f, 2f)
        Categoria.VESTIDO -> Hueco(0.03f, 0.04f, 0.58f, 0.86f, -2f)
        Categoria.ABRIGO -> if (vestido != null) Hueco(0.55f, 0.02f, 0.43f, 0.44f, 4f) else Hueco(0.54f, 0.04f, 0.44f, 0.46f, 4f)
        Categoria.CALZADO -> Hueco(0.57f, 0.70f, 0.40f, 0.27f, -5f)
        Categoria.ACCESORIO -> {
          val i = accesorios.indexOf(p)
          Hueco(0.60f + i * 0.17f, 0.50f, if (accesorios.size > 1) 0.18f else 0.30f, 0.18f, 6f)
        }
      }
      add(p to h)
    }
  }
  val aparicion = remember(claveAnimacion) { Animatable(0f) }
  LaunchedEffect(claveAnimacion) { aparicion.animateTo(1f, tween(500)) }

  BoxWithConstraints(modifier) {
    // De atrás hacia delante: abajo primero, luego arriba, abrigo y calzado encima.
    huecos.sortedBy { it.first.subtipo.forma.capa }.forEachIndexed { i, (p, h) ->
      val ancho: Dp = maxWidth * h.ancho
      val alto: Dp = maxHeight * h.alto
      Box(
        Modifier
          .offset(x = maxWidth * h.x, y = maxHeight * h.y)
          .size(ancho, alto)
          .graphicsLayer {
            rotationZ = h.giro
            val t = ((aparicion.value * huecos.size) - i).coerceIn(0f, 1f)
            alpha = t
            translationY = (1f - t) * 24f
          },
        contentAlignment = Alignment.Center,
      ) {
        val archivo = foto(p.foto)
        if (archivo != null) {
          AsyncImage(model = archivo, contentDescription = nombre(p), contentScale = ContentScale.Fit, modifier = Modifier.size(ancho, alto))
        } else {
          IconoForma(p.subtipo.forma, Color(Colores.de(p.color).argb).copy(alpha = if (p.virtual) 0.75f else 1f), Modifier.size(ancho, alto))
        }
      }
    }
  }
}

/** Collage si todas tus prendas del look tienen foto; si no, el maniquí. El usuario puede fijar una vista. */
fun usarCollage(prendas: List<Prenda>, vista: Vista): Boolean = when (vista) {
  Vista.MANIQUI -> false
  Vista.COLLAGE -> true
  Vista.AUTO -> {
    val propias = prendas.filter { !it.virtual }
    propias.size >= 2 && propias.all { it.foto != null }
  }
}

/** El look en la vista que toque: collage de fotos o maniquí. */
@Composable
fun VistaLook(
  prendas: List<Prenda>,
  cuerpo: com.ancylostoma.quemepongo.data.Cuerpo,
  vista: Vista,
  foto: (String?) -> File?,
  modifier: Modifier = Modifier,
  claveAnimacion: Any? = null,
) {
  if (usarCollage(prendas, vista)) {
    Collage(prendas, foto, modifier, claveAnimacion)
  } else {
    Maniqui(prendas, cuerpo, foto, modifier, claveAnimacion = claveAnimacion)
  }
}
