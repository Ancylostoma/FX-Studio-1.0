package com.ancylostoma.quemepongo

import com.ancylostoma.quemepongo.data.Lugares
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lectura de la respuesta de Photon (OpenStreetMap), el respaldo de la búsqueda de ciudades. */
class LugaresTest {
  @Test
  fun leeCiudadesDePhoton() {
    val respuesta = """
      {"type":"FeatureCollection","features":[
        {"type":"Feature","geometry":{"type":"Point","coordinates":[-0.3763,39.4699]},
         "properties":{"osm_key":"place","osm_value":"city","name":"Valencia","state":"Comunidad Valenciana","country":"España"}},
        {"type":"Feature","geometry":{"type":"Point","coordinates":[-68.0,10.16]},
         "properties":{"name":"Valencia","state":"Carabobo","country":"Venezuela"}},
        {"type":"Feature","geometry":{"type":"Point"},"properties":{"name":"Sin coordenadas"}},
        {"type":"Feature","geometry":{"type":"Point","coordinates":[2.35,48.85]},"properties":{"country":"France"}}
      ]}
    """.trimIndent()
    val lugares = Lugares.leerPhoton(respuesta)
    assertEquals(2, lugares.size)
    assertEquals("Valencia, Comunidad Valenciana, España", lugares[0].completo)
    assertEquals("Valencia, Carabobo, Venezuela", lugares[1].completo)
    assertEquals(39.4699, lugares[0].latitud, 1e-6)
    assertEquals(-0.3763, lugares[0].longitud, 1e-6)
    assertTrue(lugares.all { it.osm })
  }

  @Test
  fun respuestaVaciaNoFalla() {
    assertEquals(0, Lugares.leerPhoton("""{"features":[]}""").size)
    assertEquals(0, Lugares.leerPhoton("{}").size)
  }
}
