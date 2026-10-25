package com.ancylostoma.quemepongo.data

import android.content.Context
import java.io.File
import org.json.JSONObject

data class Datos(
  val prendas: List<Prenda> = emptyList(),
  val perfil: Perfil = Perfil(),
  val preferencias: Preferencias = Preferencias(),
  val looks: List<LookGuardado> = emptyList(),
)

/** Todo se guarda en el teléfono, en un solo archivo JSON. Las fotos van aparte en [carpetaFotos]. */
class Almacen(private val contexto: Context) {
  private val archivo = File(contexto.filesDir, "datos.json")
  val carpetaFotos: File = File(contexto.filesDir, "prendas").apply { mkdirs() }

  fun cargar(): Datos {
    if (!archivo.exists()) return Datos()
    return try {
      val o = JSONObject(archivo.readText())
      Datos(
        prendas = Json.prendas(o.optJSONArray("prendas")),
        perfil = o.optJSONObject("perfil")?.let { Json.perfil(it) } ?: Perfil(),
        preferencias = o.optJSONObject("preferencias")?.let { Json.preferencias(it) } ?: Preferencias(),
        looks = Json.looks(o.optJSONArray("looks")),
      )
    } catch (e: Exception) {
      // Un archivo dañado no debe impedir abrir la app; se aparta para no perderlo.
      archivo.renameTo(File(contexto.filesDir, "datos-danado-${System.currentTimeMillis()}.json"))
      Datos()
    }
  }

  @Synchronized
  fun guardar(d: Datos) {
    val o = JSONObject()
      .put("version", 1)
      .put("prendas", Json.prendas(d.prendas))
      .put("perfil", Json.perfil(d.perfil))
      .put("preferencias", Json.preferencias(d.preferencias))
      .put("looks", Json.looks(d.looks))
    // Escribir en un temporal y renombrar: si el teléfono se apaga a mitad, el archivo bueno sigue ahí.
    val tmp = File(contexto.filesDir, "datos.json.tmp")
    tmp.writeText(o.toString())
    if (!tmp.renameTo(archivo)) {
      archivo.delete()
      tmp.renameTo(archivo)
    }
  }

  fun foto(nombre: String?): File? = nombre?.let { File(carpetaFotos, it) }?.takeIf { it.exists() }

  fun borrarFoto(nombre: String?) {
    nombre?.let { File(carpetaFotos, it).delete() }
  }
}
