package com.ancylostoma.quemepongo

import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.CondicionClima
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Subtipo
import com.ancylostoma.quemepongo.motor.Analisis
import com.ancylostoma.quemepongo.motor.DiaPlan
import com.ancylostoma.quemepongo.motor.Planificador
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {
  private fun p(id: String, s: Subtipo, color: String, usos: Int = 0, precio: Double? = null) =
    Prenda(id, id, s, color, usos = usos, precio = precio)

  private val armario = listOf(
    p("c1", Subtipo.CAMISETA, "blanco"), p("c2", Subtipo.CAMISETA, "negro"), p("c3", Subtipo.CAMISA, "celeste"),
    p("c4", Subtipo.SUETER, "beige"), p("c5", Subtipo.BLUSA, "rosa"),
    p("j1", Subtipo.JEANS, "denim"), p("j2", Subtipo.PANTALON_VESTIR, "negro"), p("j3", Subtipo.PANTALON_VESTIR, "beige"),
    p("z1", Subtipo.TENIS, "blanco"), p("z2", Subtipo.ZAPATOS, "negro"),
    p("a1", Subtipo.IMPERMEABLE, "beige"), p("a2", Subtipo.BLAZER, "marino"),
  )
  private val hoy = LocalDate.of(2026, 10, 26)
  private fun dias(n: Int, ocasion: Ocasion = Ocasion.TRABAJO, t: Double = 18.0) =
    (0 until n).map { DiaPlan(hoy.plusDays(it.toLong()), CondicionClima(t, t, lluvia = it == 2), ocasion) }

  @Test
  fun laSemanaNoRepiteLaParteDeArriba() {
    val looks = Planificador.semana(armario, dias(5), Perfil(), Preferencias(), { emptyList() })
    assertEquals(5, looks.size)
    val arriba = looks.map { l -> l.prendas.first { it.categoria == Categoria.SUPERIOR }.id }
    // Varía la parte de arriba (hay 5 y dos son camisetas, poco aptas para trabajar)
    // y nunca la repite dos días seguidos.
    assertTrue("arriba: $arriba", arriba.toSet().size >= 4)
    assertTrue("arriba: $arriba", arriba.zipWithNext().none { (a, b) -> a == b })
    // El día de lluvia lleva el impermeable.
    assertTrue(looks[2].prendas.any { it.id == "a1" })
  }

  @Test
  fun laMaletaReutilizaPrendas() {
    val m = Planificador.maleta(armario, dias(5, Ocasion.CASUAL), Perfil(), Preferencias(), { emptyList() })
    assertEquals(5, m.looks.size)
    val piezasDeLosLooks = m.looks.sumOf { l -> l.prendas.count { !it.virtual } }
    // Menos prendas en la maleta que piezas usadas en total: se repiten entre días.
    assertTrue("maleta ${m.prendas.size} de $piezasDeLosLooks", m.prendas.size < piezasDeLosLooks)
    assertTrue(m.prendas.all { !it.virtual })
  }

  @Test
  fun estadisticasDelArmario() {
    val conUso = listOf(
      p("x", Subtipo.CAMISETA, "negro", usos = 10, precio = 20.0),
      p("y", Subtipo.JEANS, "denim", usos = 2, precio = 80.0),
      p("z", Subtipo.TENIS, "blanco"),
    )
    val s = Analisis.estadisticas(conUso, emptyList(), ahora = 2_000_000_000_000L)
    assertEquals(3, s.total)
    assertEquals(100.0, s.valorTotal!!, 0.001)
    assertEquals("y", s.costoPorUso.first().first.id) // 40 por uso, el más caro
    assertEquals(2.0, s.costoPorUso.last().second, 0.001)
    assertEquals(listOf("z"), s.nuncaUsadas.map { it.id })
    assertEquals("x", s.masUsadas.first().id)
    assertNull(Analisis.estadisticas(armario, emptyList(), 0L).valorTotal)
  }
}
