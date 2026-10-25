package com.ancylostoma.quemepongo.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Las tendencias, eventos y looks de famosos viven en tendencias.json, en la
 * rama main del repositorio. La app trae una copia de fábrica y cada 12 horas
 * descarga la última: así se actualizan sin publicar otra versión en Play.
 */
class ServicioTendencias(private val contexto: Context) {
  private val cache = File(contexto.filesDir, "tendencias.json")

  /** Lo que haya en el teléfono: la última descarga o, si no hay, la copia de fábrica. */
  fun local(): CatalogoTendencias {
    val deFabrica = contexto.assets.open("tendencias.json").bufferedReader().use { Json.tendencias(it.readText(), idioma()) }
    val descargado = runCatching { Json.tendencias(cache.readText(), idioma()) }.getOrNull()
    // Si la app se actualizó y trae un archivo más nuevo que la descarga, gana el de fábrica.
    return if (descargado != null && descargado.version >= deFabrica.version) descargado else deFabrica
  }

  fun tocaActualizar() = !cache.exists() || System.currentTimeMillis() - cache.lastModified() > 12 * 3_600_000L

  suspend fun actualizar(): CatalogoTendencias? = withContext(Dispatchers.IO) {
    runCatching {
      val conexion = (URL(URL_REMOTA).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 15_000
      }
      try {
        if (conexion.responseCode != 200) return@runCatching null
        val texto = conexion.inputStream.bufferedReader().use { it.readText() }
        val catalogo = Json.tendencias(texto, idioma()) // Si no se puede leer, no se guarda.
        if (catalogo.tendencias.isEmpty()) return@runCatching null
        cache.writeText(texto)
        catalogo
      } finally {
        conexion.disconnect()
      }
    }.getOrNull()
  }

  private fun idioma() = Locale.getDefault().language

  companion object {
    const val URL_REMOTA =
      "https://raw.githubusercontent.com/Ancylostoma/FX-Studio-1.0/main/quemepongo-android/app/src/main/assets/tendencias.json"
  }
}
