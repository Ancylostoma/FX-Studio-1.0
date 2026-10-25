package com.ancylostoma.quemepongo

import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.CondicionClima
import com.ancylostoma.quemepongo.data.Json
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Subtipo
import com.ancylostoma.quemepongo.motor.Contexto
import com.ancylostoma.quemepongo.motor.IdeasCompra
import com.ancylostoma.quemepongo.motor.Motor
import com.ancylostoma.quemepongo.motor.Tendencias
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorTest {
  private fun p(id: String, s: Subtipo, color: String) = Prenda(id, s.etiqueta, s, color)

  private val armario = listOf(
    p("camiseta", Subtipo.CAMISETA, "blanco"),
    p("sueter", Subtipo.SUETER, "beige"),
    p("camisa", Subtipo.CAMISA, "celeste"),
    p("jeans", Subtipo.JEANS, "denim"),
    p("short", Subtipo.SHORT, "beige"),
    p("vestir", Subtipo.PANTALON_VESTIR, "negro"),
    p("abrigo", Subtipo.ABRIGO, "camel"),
    p("gabardina", Subtipo.IMPERMEABLE, "beige"),
    p("tenis", Subtipo.TENIS, "blanco"),
    p("sandalias", Subtipo.SANDALIAS, "marron"),
    p("zapatos", Subtipo.ZAPATOS, "negro"),
    p("bufanda", Subtipo.BUFANDA, "gris"),
  )

  private fun ctx(clima: CondicionClima?, ocasion: Ocasion = Ocasion.CASUAL, pref: Preferencias = Preferencias()) =
    Contexto(clima, ocasion, Perfil(), pref, emptyList(), ahoraMs = 1_000_000_000L)

  private fun clima(t: Double, lluvia: Boolean = false) = CondicionClima(t, t, lluvia)

  @Test
  fun conCalorNoPoneAbrigoNiSueter() {
    val look = Motor.sugerir(armario, ctx(clima(31.0))).first()
    val ids = look.prendas.map { it.id }
    assertFalse(ids.contains("abrigo"))
    assertFalse(ids.contains("sueter"))
    assertFalse(look.prendas.any { it.categoria == Categoria.ABRIGO })
  }

  @Test
  fun conFrioAbriga() {
    val look = Motor.sugerir(armario, ctx(clima(3.0))).first()
    assertTrue(look.prendas.any { it.categoria == Categoria.ABRIGO })
    assertFalse(look.prendas.any { it.id == "short" || it.id == "sandalias" })
  }

  @Test
  fun conLluviaEligeImpermeableYNoSandalias() {
    val look = Motor.sugerir(armario, ctx(clima(15.0, lluvia = true))).first()
    assertTrue(look.prendas.any { it.id == "gabardina" })
    assertFalse(look.prendas.any { it.id == "sandalias" })
  }

  @Test
  fun eventoFormalEligePiezasFormales() {
    val look = Motor.sugerir(armario, ctx(clima(20.0), Ocasion.FORMAL)).first()
    assertTrue(look.prendas.any { it.id == "vestir" })
    assertTrue(look.prendas.any { it.id == "zapatos" })
  }

  @Test
  fun armarioVacioCompletaConIdeasDeCompra() {
    val looks = Motor.sugerir(emptyList(), ctx(clima(20.0)))
    assertTrue(looks.isNotEmpty())
    assertTrue(looks.first().prendas.all { it.virtual })
    assertTrue(looks.first().razones.any { it.startsWith("Te falta") })
  }

  @Test
  fun lasSugerenciasSonDistintas() {
    val looks = Motor.sugerir(armario, ctx(clima(20.0)), cantidad = 4)
    assertEquals(looks.size, looks.map { it.id }.distinct().size)
  }

  @Test
  fun aprendeDeLosMeGusta() {
    val c = ctx(clima(20.0))
    val conjunto = listOf(armario[2], armario[5], armario[10])
    val antes = Motor.puntuar(conjunto, c)
    var pref = Preferencias()
    repeat(3) { pref = Motor.aprender(pref, conjunto, 1.0) }
    val despues = Motor.puntuar(conjunto, c.copy(preferencias = pref))
    assertTrue(despues > antes)
    var malo = Preferencias()
    repeat(3) { malo = Motor.aprender(malo, conjunto, -1.0) }
    assertTrue(Motor.puntuar(conjunto, c.copy(preferencias = malo)) < antes)
  }

  @Test
  fun armoniaDeColores() {
    assertTrue(Colores.armonia(listOf("negro", "blanco", "rojo")) > Colores.armonia(listOf("rojo", "verde", "morado")))
    assertTrue(Colores.armonia(listOf("estampado", "estampado")) < 0)
    assertEquals("rojo", Colores.masCercano(200, 30, 35).clave)
    assertEquals("negro", Colores.masCercano(10, 10, 12).clave)
  }

  @Test
  fun fechasDeTendencias() {
    assertTrue(Tendencias.enFecha("12-01", "01-06", LocalDate.of(2027, 1, 3)))
    assertTrue(Tendencias.enFecha("12-01", "01-06", LocalDate.of(2026, 12, 24)))
    assertFalse(Tendencias.enFecha("12-01", "01-06", LocalDate.of(2026, 11, 30)))
    assertTrue(Tendencias.enFecha("10-20", "10-31", LocalDate.of(2026, 10, 25)))
    assertTrue(Tendencias.enFecha("", "", LocalDate.of(2026, 3, 3)))
    assertFalse(Tendencias.enFecha("2026-01-01", "2026-01-31", LocalDate.of(2026, 2, 1)))
  }

  @Test
  fun elArchivoDeTendenciasEsValido() {
    val catalogo = Json.tendencias(File("src/main/assets/tendencias.json").readText())
    assertTrue(catalogo.tendencias.size >= 10)
    assertTrue(catalogo.tiendaUrl.contains("{q}"))
    for (t in catalogo.tendencias) {
      assertTrue("${t.id} sin prendas", t.prendas.isNotEmpty())
      t.colores.forEach { c -> assertNotNull("${t.id}: color $c", Colores.todos.firstOrNull { it.clave == c }) }
      t.prendas.forEach { i -> assertNotNull(Colores.todos.firstOrNull { it.clave == i.color }) }
      // Cada fecha debe poder leerse.
      Tendencias.enFecha(t.desde, t.hasta, LocalDate.of(2026, 10, 25))
    }
    val hoyNorte = Tendencias.activas(catalogo.tendencias, LocalDate.of(2026, 10, 25), 19.4)
    assertTrue(hoyNorte.any { it.id == "halloween" })
    assertNull(hoyNorte.firstOrNull { it.hemisferio == "sur" })
  }

  @Test
  fun lookDeTendenciaUsaLoQueYaTengo() {
    val catalogo = Json.tendencias(File("src/main/assets/tendencias.json").readText())
    val t = catalogo.tendencias.first { it.id == "blazer-oversize" }
    val look = Motor.lookDeTendencia(t, armario, ctx(clima(18.0)))
    assertTrue(look.prendas.any { it.id == "camiseta" })
    assertTrue(look.prendas.any { it.id == "jeans" })
    assertTrue(look.prendas.any { it.virtual && it.subtipo == Subtipo.BLAZER })
  }

  @Test
  fun ideasDeCompraNoRepitenLoQueTengo() {
    val catalogo = Json.tendencias(File("src/main/assets/tendencias.json").readText())
    val ideas = IdeasCompra.sugerir(armario, Perfil(), Preferencias(), catalogo.tendencias)
    assertTrue(ideas.isNotEmpty())
    assertFalse(ideas.any { i -> armario.any { it.subtipo == i.idea.subtipo && it.color == i.idea.color } })
    val url = IdeasCompra.enlace(catalogo.tiendaUrl, ideas.first().idea)
    assertFalse(url.contains("{q}"))
  }

  @Test
  fun jsonIdaYVuelta() {
    val original = armario.first().copy(foto = "x.jpg", usos = 3)
    assertEquals(original, Json.prenda(Json.prenda(original)))
    val perfil = Perfil(ciudad = "Madrid", latitud = 40.4, longitud = -3.7, bienvenidaHecha = true)
    assertEquals(perfil, Json.perfil(Json.perfil(perfil)))
    val pref = Motor.aprender(Preferencias(), armario.take(3), 1.0)
    assertEquals(pref, Json.preferencias(Json.preferencias(pref)))
  }
}
