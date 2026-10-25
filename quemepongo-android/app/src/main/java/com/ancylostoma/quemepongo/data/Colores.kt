package com.ancylostoma.quemepongo.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ColorRopa(
  val clave: String,
  val etiqueta: String,
  val argb: Long,
  /** Los neutros combinan con todo. */
  val neutro: Boolean,
) {
  val rojo get() = ((argb shr 16) and 0xFF).toInt()
  val verde get() = ((argb shr 8) and 0xFF).toInt()
  val azul get() = (argb and 0xFF).toInt()
}

object Colores {
  const val ESTAMPADO = "estampado"

  val todos = listOf(
    ColorRopa("negro", "Negro", 0xFF1E1E1E, true),
    ColorRopa("blanco", "Blanco", 0xFFF5F4EE, true),
    ColorRopa("gris", "Gris", 0xFF8F8F8F, true),
    ColorRopa("beige", "Beige", 0xFFD9C8A7, true),
    ColorRopa("camel", "Camel", 0xFFB8864E, true),
    ColorRopa("marron", "Marrón", 0xFF6B4A30, true),
    ColorRopa("marino", "Azul marino", 0xFF1F2B47, true),
    ColorRopa("denim", "Denim", 0xFF4B6F9C, true),
    ColorRopa("oliva", "Verde oliva", 0xFF6B6B36, true),
    ColorRopa("azul", "Azul", 0xFF2F5FCF, false),
    ColorRopa("celeste", "Celeste", 0xFF9DC6EA, false),
    ColorRopa("verde", "Verde", 0xFF2E8B57, false),
    ColorRopa("rojo", "Rojo", 0xFFC62828, false),
    ColorRopa("borgona", "Borgoña", 0xFF7A1F35, false),
    ColorRopa("rosa", "Rosa", 0xFFEE9DB8, false),
    ColorRopa("morado", "Morado", 0xFF6A3D9A, false),
    ColorRopa("amarillo", "Amarillo", 0xFFF2C94C, false),
    ColorRopa("naranja", "Naranja", 0xFFE67E22, false),
    ColorRopa(ESTAMPADO, "Estampado", 0xFFB06AB3, false),
  )

  private val porClave = todos.associateBy { it.clave }

  fun de(clave: String): ColorRopa = porClave[clave] ?: todos[2]

  /** Color de la paleta más parecido a un RGB (distancia "redmean", cercana a la percepción). */
  fun masCercano(r: Int, g: Int, b: Int): ColorRopa =
    todos.filter { it.clave != ESTAMPADO }.minBy { distancia(r, g, b, it) }

  fun distancia(r: Int, g: Int, b: Int, c: ColorRopa): Double {
    val rm = (r + c.rojo) / 2.0
    val dr = (r - c.rojo).toDouble()
    val dg = (g - c.verde).toDouble()
    val db = (b - c.azul).toDouble()
    return sqrt((2 + rm / 256) * dr * dr + 4 * dg * dg + (2 + (255 - rm) / 256) * db * db)
  }

  /** Tono en grados (0-360). */
  fun tono(c: ColorRopa): Double {
    val r = c.rojo / 255.0
    val g = c.verde / 255.0
    val b = c.azul / 255.0
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    if (d == 0.0) return 0.0
    val h = when (mx) {
      r -> ((g - b) / d) % 6
      g -> (b - r) / d + 2
      else -> (r - g) / d + 4
    } * 60
    return if (h < 0) h + 360 else h
  }

  /**
   * Qué tan bien combinan los colores de un look, de -3 a 2.
   * Regla simple de estilistas: neutros + como mucho un color de acento,
   * o dos colores si son vecinos (análogos) u opuestos (complementarios).
   */
  fun armonia(claves: List<String>): Double {
    val estampados = claves.count { it == ESTAMPADO }
    val vivos = claves.filter { it != ESTAMPADO }.map { de(it) }.filter { !it.neutro }.distinctBy { it.clave }
    if (estampados > 1) return -2.5
    if (estampados == 1) return if (vivos.isEmpty()) 1.2 else -1.0
    return when (vivos.size) {
      0 -> 0.8
      1 -> 1.5
      2 -> {
        val dif = abs(tono(vivos[0]) - tono(vivos[1])).let { if (it > 180) 360 - it else it }
        when {
          dif < 40 -> 1.0
          dif > 150 -> 0.7
          else -> -0.7
        }
      }
      else -> -2.0
    }
  }
}
