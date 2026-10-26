package com.ancylostoma.quemepongo.data

import android.content.Context
import java.io.File
import org.json.JSONObject

data class Datos(
  val prendas: List<Prenda> = emptyList(),
  val perfil: Perfil = Perfil(),
  val preferencias: Preferencias = Preferencias(),
  val looks: List<LookGuardado> = emptyList(),
  /** Lo que el usuario se puso cada día, del más reciente al más viejo. */
  val historial: List<LookGuardado> = emptyList(),
  /** Premium: el plan de la semana, un look por día (la fecha va en el id: "plan-AAAA-MM-DD"). */
  val plan: List<LookGuardado> = emptyList(),
  /** Premium: el último viaje armado. */
  val viaje: Viaje? = null,
)

data class Viaje(val destino: String, val desde: String, val dias: Int, val looks: List<LookGuardado>, val llevar: List<Prenda>, val faltan: List<Prenda>)

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
        historial = Json.looks(o.optJSONArray("historial")),
        plan = Json.looks(o.optJSONArray("plan")),
        viaje = o.optJSONObject("viaje")?.let { v ->
          Viaje(
            destino = v.optString("destino"),
            desde = v.optString("desde"),
            dias = v.optInt("dias"),
            looks = Json.looks(v.optJSONArray("looks")),
            llevar = Json.prendas(v.optJSONArray("llevar")),
            faltan = Json.prendas(v.optJSONArray("faltan")),
          )
        },
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
      .put("historial", Json.looks(d.historial))
      .put("plan", Json.looks(d.plan))
      .put(
        "viaje",
        d.viaje?.let { v ->
          JSONObject()
            .put("destino", v.destino)
            .put("desde", v.desde)
            .put("dias", v.dias)
            .put("looks", Json.looks(v.looks))
            .put("llevar", Json.prendas(v.llevar))
            .put("faltan", Json.prendas(v.faltan))
        } ?: JSONObject.NULL,
      )
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
