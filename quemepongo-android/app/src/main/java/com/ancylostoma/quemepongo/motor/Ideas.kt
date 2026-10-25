package com.ancylostoma.quemepongo.motor

import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Tendencia
import java.net.URLEncoder
import java.time.LocalDate

data class IdeaCompra(val idea: IdeaPrenda, val motivo: String, val puntos: Double)

object Tendencias {
  /** Tendencias vigentes hoy en el hemisferio del usuario. */
  fun activas(todas: List<Tendencia>, hoy: LocalDate, latitud: Double?): List<Tendencia> {
    val sur = (latitud ?: 1.0) < 0
    return todas.filter { t ->
      val hemisferioOk = when (t.hemisferio) {
        "norte" -> !sur
        "sur" -> sur
        else -> true
      }
      hemisferioOk && enFecha(t.desde, t.hasta, hoy)
    }
  }

  fun enFecha(desde: String, hasta: String, hoy: LocalDate): Boolean {
    if (desde.isBlank() || hasta.isBlank()) return true
    if (desde.length == 10 && hasta.length == 10) {
      return !hoy.isBefore(LocalDate.parse(desde)) && !hoy.isAfter(LocalDate.parse(hasta))
    }
    // "MM-DD": se repite cada año y puede cruzar Año Nuevo (12-01 a 01-06).
    val d = mesDia(desde)
    val h = mesDia(hasta)
    val x = hoy.monthValue * 100 + hoy.dayOfMonth
    return if (d <= h) x in d..h else x >= d || x <= h
  }

  private fun mesDia(s: String): Int {
    val partes = s.takeLast(5).split("-")
    return partes[0].toInt() * 100 + partes[1].toInt()
  }
}

/** "Más ropa como esta": ideas de compra según gustos, tendencias y huecos del armario. */
object IdeasCompra {
  fun sugerir(
    armario: List<Prenda>,
    perfil: Perfil,
    pref: Preferencias,
    tendencias: List<Tendencia>,
    max: Int = 8,
  ): List<IdeaCompra> {
    val reales = armario.filter { !it.virtual }
    fun laTiene(i: IdeaPrenda) = reales.any { it.subtipo == i.subtipo && it.color == i.color }

    val ideas = mutableListOf<IdeaCompra>()
    for (c in listOf(Categoria.SUPERIOR, Categoria.INFERIOR, Categoria.CALZADO, Categoria.ABRIGO)) {
      if (reales.none { it.categoria == c }) {
        Catalogo.base.firstOrNull { it.subtipo.categoria == c }?.let {
          ideas.add(IdeaCompra(it, "Tu armario no tiene ${c.etiqueta.lowercase()}", 3.0))
        }
      }
    }
    for (t in tendencias) for (i in t.prendas) {
      ideas.add(IdeaCompra(i, "En tendencia: ${t.titulo}", 1.5 * t.peso * (0.5 + perfil.importanciaModa) + gusto(i, perfil, pref)))
    }
    for (i in Catalogo.base) {
      val g = gusto(i, perfil, pref)
      if (g > 0.8) ideas.add(IdeaCompra(i, "Parecida a lo que te gusta", g))
    }
    return ideas
      .filter { !laTiene(it.idea) && it.idea.color !in perfil.coloresEvitar }
      .sortedByDescending { it.puntos }
      .distinctBy { it.idea.subtipo to it.idea.color }
      .take(max)
  }

  private fun gusto(i: IdeaPrenda, perfil: Perfil, pref: Preferencias): Double {
    var s = (pref.colores[i.color] ?: 0.0) + (pref.subtipos[i.subtipo.name] ?: 0.0)
    s += i.subtipo.estilos.sumOf { pref.estilos[it.name] ?: 0.0 } / i.subtipo.estilos.size
    if (i.color in perfil.coloresFavoritos) s += 0.6
    if (i.subtipo.estilos.any { it in perfil.estilos }) s += 0.5
    return s
  }

  fun enlace(plantilla: String, idea: IdeaPrenda): String {
    val color = Colores.de(idea.color).etiqueta
    val q = if (idea.nombre.contains(color, ignoreCase = true)) idea.nombre else "${idea.nombre} $color"
    return plantilla.replace("{q}", URLEncoder.encode(q, "UTF-8"))
  }
}
