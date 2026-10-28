package com.ancylostoma.quemepongo.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/** Una ciudad encontrada. [osm]: viene de OpenStreetMap (hay que mostrar la atribución). */
data class Lugar(val nombre: String, val region: String, val latitud: Double, val longitud: Double, val osm: Boolean = false) {
  val completo: String get() = listOf(nombre, region).filter { it.isNotBlank() }.joinToString(", ")
}

/**
 * Ciudades por nombre y nombre de un punto. Primero el servicio del propio
 * Android (Geocoder); en muchos teléfonos no tiene servicio detrás y no
 * devuelve nada, así que entonces se pregunta a Photon (komoot), que busca en
 * los datos de OpenStreetMap.
 */
object Lugares {
  private const val PHOTON = "https://photon.komoot.io"
  // Photon solo traduce a estos idiomas; con los demás devuelve el nombre local.
  private val IDIOMAS_PHOTON = setOf("en", "de", "fr")

  suspend fun buscar(contexto: Context, texto: String, max: Int = 5): List<Lugar> {
    val q = texto.trim()
    if (q.isEmpty()) return emptyList()
    val delTelefono = geocoder(contexto) { g, listo -> nombre(g, q, max, listo) }
      .mapNotNull { desdeAddress(it, q) }
    return delTelefono.ifEmpty { photon("$PHOTON/api/?q=${URLEncoder.encode(q, "UTF-8")}&limit=$max&osm_tag=place${idioma()}") }
      .distinctBy { it.completo.lowercase(Locale.ROOT) }
  }

  suspend fun nombreDe(contexto: Context, lat: Double, lon: Double): String? {
    val a = geocoder(contexto) { g, listo -> punto(g, lat, lon, listo) }.firstOrNull()
    a?.let { return it.locality ?: it.subAdminArea ?: it.adminArea }
    return photon(String.format(Locale.US, "$PHOTON/reverse?lat=%.3f&lon=%.3f%s", lat, lon, idioma())).firstOrNull()?.nombre
  }

  private fun idioma(): String = Locale.getDefault().language.takeIf { it in IDIOMAS_PHOTON }?.let { "&lang=$it" }.orEmpty()

  private fun desdeAddress(a: Address, buscado: String): Lugar? {
    if (!a.hasLatitude() || !a.hasLongitude()) return null
    val nombre = a.locality ?: a.subAdminArea ?: a.featureName ?: buscado
    val region = listOfNotNull(a.adminArea?.takeIf { it != nombre }, a.countryName).joinToString(", ")
    return Lugar(nombre, region, a.latitude, a.longitude)
  }

  /** Geocoder con tiempo límite; en Android 13+ con su versión asíncrona. Si falla, lista vacía. */
  private suspend fun geocoder(contexto: Context, pedir: (Geocoder, (List<Address>) -> Unit) -> Unit): List<Address> {
    if (!Geocoder.isPresent()) return emptyList()
    val g = Geocoder(contexto, Locale.getDefault())
    return withTimeoutOrNull(8_000) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
          runCatching { pedir(g) { if (cont.isActive) cont.resume(it) } }.onFailure { if (cont.isActive) cont.resume(emptyList()) }
        }
      } else {
        withContext(Dispatchers.IO) {
          var r: List<Address> = emptyList()
          runCatching { pedir(g) { r = it } }
          r
        }
      }
    }.orEmpty()
  }

  @Suppress("DEPRECATION")
  private fun nombre(g: Geocoder, q: String, max: Int, listo: (List<Address>) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      g.getFromLocationName(q, max, object : Geocoder.GeocodeListener {
        override fun onGeocode(lista: MutableList<Address>) = listo(lista)
        override fun onError(mensaje: String?) = listo(emptyList())
      })
    } else {
      listo(g.getFromLocationName(q, max).orEmpty())
    }
  }

  @Suppress("DEPRECATION")
  private fun punto(g: Geocoder, lat: Double, lon: Double, listo: (List<Address>) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      g.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
        override fun onGeocode(lista: MutableList<Address>) = listo(lista)
        override fun onError(mensaje: String?) = listo(emptyList())
      })
    } else {
      listo(g.getFromLocation(lat, lon, 1).orEmpty())
    }
  }

  private suspend fun photon(url: String): List<Lugar> = withContext(Dispatchers.IO) {
    runCatching {
      val conexion = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", ServicioClima.AGENTE)
        connectTimeout = 8_000
        readTimeout = 10_000
      }
      try {
        if (conexion.responseCode != 200) return@runCatching emptyList()
        leerPhoton(conexion.inputStream.bufferedReader().use { it.readText() })
      } finally {
        conexion.disconnect()
      }
    }.getOrDefault(emptyList())
  }

  /** Respuesta de Photon (GeoJSON): coordenadas [lon, lat] y nombre, estado y país. */
  fun leerPhoton(texto: String): List<Lugar> {
    val lista = JSONObject(texto).optJSONArray("features") ?: return emptyList()
    return (0 until lista.length()).mapNotNull { i ->
      val f = lista.optJSONObject(i) ?: return@mapNotNull null
      val c = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@mapNotNull null
      val p = f.optJSONObject("properties") ?: JSONObject()
      val nombre = p.optString("name").ifBlank { p.optString("city") }.ifBlank { return@mapNotNull null }
      val region = listOf(p.optString("state"), p.optString("country"))
        .filter { it.isNotBlank() && it != nombre }
        .joinToString(", ")
      Lugar(nombre, region, c.optDouble(1), c.optDouble(0), osm = true)
    }
  }
}
