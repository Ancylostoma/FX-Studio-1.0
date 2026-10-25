package com.ancylostoma.quemepongo.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.exp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class PuntoClima(
  val horaMs: Long,
  val temperatura: Double,
  val vientoMs: Double,
  val humedad: Double,
  val simbolo: String,
  val precipitacion: Double,
)

data class Clima(val lugar: String, val puntos: List<PuntoClima>, val obtenidoMs: Long) {
  /** El tiempo del día de hoy (o mañana, si esa hora ya pasó) en el momento elegido. */
  fun para(momento: Momento, ahoraMs: Long = System.currentTimeMillis()): CondicionClima? {
    if (puntos.isEmpty()) return null
    val zona = ZoneId.systemDefault()
    val ahora = Instant.ofEpochMilli(ahoraMs).atZone(zona)
    var objetivo = ahora.withHour(momento.hora).withMinute(0).withSecond(0).withNano(0)
    if (objetivo.isBefore(ahora.minusHours(2))) objetivo = objetivo.plusDays(1)
    val inicio = objetivo.toInstant().toEpochMilli() - 3_600_000L
    val fin = inicio + 4 * 3_600_000L
    val tramo = puntos.filter { it.horaMs in inicio..fin }.ifEmpty { listOf(puntos.minBy { kotlin.math.abs(it.horaMs - inicio) }) }

    val t = tramo.map { it.temperatura }.average()
    val viento = tramo.map { it.vientoMs }.average()
    val humedad = tramo.map { it.humedad }.average()
    val simbolos = tramo.map { it.simbolo }
    val nieve = simbolos.any { "snow" in it || "sleet" in it }
    val lluvia = nieve || simbolos.any { "rain" in it || "thunder" in it } || tramo.sumOf { it.precipitacion } > 0.4
    val principal = simbolos.groupingBy { it }.eachCount().maxBy { it.value }.key
    return CondicionClima(
      temperatura = t,
      sensacion = sensacionTermica(t, humedad, viento),
      lluvia = lluvia,
      nieve = nieve,
      vientoMs = viento,
      soleado = principal.startsWith("clearsky") || principal.startsWith("fair"),
      cielo = cielo(principal),
    )
  }

  companion object {
    /** Temperatura aparente de Steadman (la que usa el servicio meteorológico de Australia). */
    fun sensacionTermica(t: Double, humedad: Double, vientoMs: Double): Double {
      val e = humedad / 100 * 6.105 * exp(17.27 * t / (237.7 + t))
      return t + 0.33 * e - 0.70 * vientoMs - 4.0
    }

    fun cielo(simbolo: String): Cielo = when {
      "thunder" in simbolo -> Cielo.TORMENTA
      "sleet" in simbolo -> Cielo.AGUANIEVE
      "snow" in simbolo -> Cielo.NIEVE
      "heavyrain" in simbolo -> Cielo.LLUVIA_FUERTE
      "lightrain" in simbolo -> Cielo.LLOVIZNA
      "rain" in simbolo -> Cielo.LLUVIA
      simbolo.startsWith("fog") -> Cielo.NIEBLA
      simbolo.startsWith("clearsky") -> Cielo.DESPEJADO
      simbolo.startsWith("fair") -> Cielo.POCO_NUBOSO
      simbolo.startsWith("partlycloudy") -> Cielo.PARCIAL
      simbolo.startsWith("cloudy") -> Cielo.NUBLADO
      else -> Cielo.DESCONOCIDO
    }
  }
}

/**
 * Pronóstico del Instituto Meteorológico de Noruega (api.met.no): gratis
 * también para apps comerciales, con atribución y un User-Agent que identifique la app.
 */
object ServicioClima {
  private const val AGENTE = "QueMePongo/1.0 github.com/Ancylostoma/FX-Studio-1.0"

  private const val VIGENCIA_MS = 45 * 60_000L
  private const val VIGENCIA_SIN_RED_MS = 6 * 3_600_000L

  /**
   * Con miles de usuarios, todos piden el tiempo a la misma hora (el aviso de
   * la mañana). Para respetar el límite de MET Norway (unas 20 peticiones por
   * segundo por app) el pronóstico se guarda 45 minutos y las coordenadas van
   * con 2 decimales (~1 km): así vecinos comparten la respuesta en su caché.
   * Sin internet, sirve un pronóstico de hasta 6 horas.
   */
  suspend fun pronostico(lat: Double, lon: Double, lugar: String, carpetaCache: File): Clima = withContext(Dispatchers.IO) {
    val clave = String.format(Locale.US, "%.2f,%.2f", lat, lon)
    val cache = File(carpetaCache, "clima.json")
    val guardado = runCatching { JSONObject(cache.readText()) }.getOrNull()?.takeIf { it.optString("clave") == clave }
    val edad = guardado?.let { System.currentTimeMillis() - it.optLong("hora") } ?: Long.MAX_VALUE
    if (guardado != null && edad < VIGENCIA_MS) return@withContext Clima(lugar, leer(guardado.getString("texto")), guardado.optLong("hora"))

    try {
      val url = URL(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f", lat, lon))
      val conexion = (url.openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", AGENTE)
        connectTimeout = 10_000
        readTimeout = 15_000
      }
      try {
        if (conexion.responseCode != 200) error("MET Norway: ${conexion.responseCode}")
        val texto = conexion.inputStream.bufferedReader().use { it.readText() }
        val ahora = System.currentTimeMillis()
        runCatching { cache.writeText(JSONObject().put("clave", clave).put("hora", ahora).put("texto", texto).toString()) }
        Clima(lugar, leer(texto), ahora)
      } finally {
        conexion.disconnect()
      }
    } catch (e: Exception) {
      if (guardado != null && edad < VIGENCIA_SIN_RED_MS) Clima(lugar, leer(guardado.getString("texto")), guardado.optLong("hora")) else throw e
    }
  }

  fun leer(texto: String): List<PuntoClima> {
    val serie = JSONObject(texto).getJSONObject("properties").getJSONArray("timeseries")
    val limite = System.currentTimeMillis() + 36 * 3_600_000L
    val puntos = mutableListOf<PuntoClima>()
    for (i in 0 until serie.length()) {
      val s = serie.getJSONObject(i)
      val hora = Instant.parse(s.getString("time")).toEpochMilli()
      if (hora > limite) break
      val datos = s.getJSONObject("data")
      val inst = datos.getJSONObject("instant").getJSONObject("details")
      val prox = datos.optJSONObject("next_1_hours") ?: datos.optJSONObject("next_6_hours")
      puntos.add(
        PuntoClima(
          horaMs = hora,
          temperatura = inst.optDouble("air_temperature", 20.0),
          vientoMs = inst.optDouble("wind_speed", 0.0),
          humedad = inst.optDouble("relative_humidity", 50.0),
          simbolo = prox?.optJSONObject("summary")?.optString("symbol_code") ?: "",
          precipitacion = prox?.optJSONObject("details")?.optDouble("precipitation_amount", 0.0) ?: 0.0,
        ),
      )
    }
    return puntos
  }

  fun tienePermiso(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

  /** Ubicación aproximada (red o GPS). Solo se usa para pedir el pronóstico. */
  @SuppressLint("MissingPermission")
  suspend fun ubicacion(contexto: Context): Location? {
    if (!tienePermiso(contexto)) return null
    val lm = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val proveedores = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
      .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    val reciente = proveedores.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
      .maxByOrNull { it.time }
    if (reciente != null && System.currentTimeMillis() - reciente.time < 6 * 3_600_000L) return reciente
    val proveedor = proveedores.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return reciente
    val nueva = withTimeoutOrNull(12_000) {
      suspendCancellableCoroutine<Location?> { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          val cancelar = CancellationSignal()
          cont.invokeOnCancellation { cancelar.cancel() }
          lm.getCurrentLocation(proveedor, cancelar, ContextCompat.getMainExecutor(contexto)) { l ->
            if (cont.isActive) cont.resume(l)
          }
        } else {
          // Objeto completo: antes de Android 11 la interfaz no tenía métodos por defecto
          // y una lambda haría fallar la app cuando el sistema llamara a los otros.
          val oyente = object : LocationListener {
            override fun onLocationChanged(l: Location) {
              if (cont.isActive) cont.resume(l)
            }

            @Deprecated("Solo existe en versiones viejas de Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
          }
          cont.invokeOnCancellation { lm.removeUpdates(oyente) }
          @Suppress("DEPRECATION")
          lm.requestSingleUpdate(proveedor, oyente, Looper.getMainLooper())
        }
      }
    }
    return nueva ?: reciente
  }

  @Suppress("DEPRECATION")
  suspend fun nombreDelLugar(contexto: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching {
      Geocoder(contexto, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()
  }

  /** Busca una ciudad escrita a mano. Devuelve (nombre, latitud, longitud). */
  @Suppress("DEPRECATION")
  suspend fun buscarCiudad(contexto: Context, nombre: String): Triple<String, Double, Double>? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching {
      Geocoder(contexto, Locale.getDefault()).getFromLocationName(nombre, 1)?.firstOrNull()?.let {
        Triple(it.locality ?: it.featureName ?: nombre, it.latitude, it.longitude)
      }
    }.getOrNull()
  }
}
