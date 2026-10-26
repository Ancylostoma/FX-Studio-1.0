package com.ancylostoma.quemepongo.data

import org.json.JSONArray
import org.json.JSONObject

/** Conversión a/desde JSON. Los campos desconocidos se ignoran, así los datos viejos siguen abriendo. */
object Json {
  private fun JSONArray?.textos(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

  private fun Collection<String>.aJson() = JSONArray().also { a -> forEach { a.put(it) } }

  private fun JSONObject?.mapa(): Map<String, Double> {
    if (this == null) return emptyMap()
    return keys().asSequence().associateWith { getDouble(it) }
  }

  private fun Map<String, Double>.aJson() = JSONObject().also { o -> forEach { (k, v) -> o.put(k, v) } }

  private fun estilos(a: JSONArray?) = a.textos().mapNotNull { n -> Estilo.entries.firstOrNull { it.name == n } }.toSet()

  // ---- Prendas ----

  fun prenda(p: Prenda): JSONObject = JSONObject()
    .put("id", p.id)
    .put("nombre", p.nombre)
    .put("subtipo", p.subtipo.name)
    .put("color", p.color)
    .put("estilos", p.estilos.map { it.name }.aJson())
    .put("abrigo", p.abrigo)
    .put("formalidad", p.formalidad)
    .put("impermeable", p.impermeable)
    .put("foto", p.foto ?: JSONObject.NULL)
    .put("creada", p.creada)
    .put("usos", p.usos)
    .put("ultimoUso", p.ultimoUso)
    .put("virtual", p.virtual)
    .put("precio", p.precio ?: JSONObject.NULL)

  fun prenda(o: JSONObject): Prenda? {
    val subtipo = Subtipo.porNombre(o.optString("subtipo")) ?: return null
    return Prenda(
      id = o.getString("id"),
      nombre = o.optString("nombre", ""),
      subtipo = subtipo,
      color = o.optString("color", "gris"),
      estilos = estilos(o.optJSONArray("estilos")).ifEmpty { subtipo.estilos },
      abrigo = o.optInt("abrigo", subtipo.abrigo),
      formalidad = o.optInt("formalidad", subtipo.formalidad),
      impermeable = o.optBoolean("impermeable", subtipo.impermeable),
      foto = if (o.isNull("foto")) null else o.optString("foto"),
      creada = o.optLong("creada"),
      usos = o.optInt("usos"),
      ultimoUso = o.optLong("ultimoUso"),
      virtual = o.optBoolean("virtual"),
      precio = if (o.isNull("precio") || !o.has("precio")) null else o.optDouble("precio"),
    )
  }

  fun prendas(lista: List<Prenda>) = JSONArray().also { a -> lista.forEach { a.put(prenda(it)) } }

  fun prendas(a: JSONArray?): List<Prenda> =
    if (a == null) emptyList() else (0 until a.length()).mapNotNull { prenda(a.getJSONObject(it)) }

  // ---- Perfil ----

  fun perfil(p: Perfil): JSONObject = JSONObject()
    .put("estilos", p.estilos.map { it.name }.aJson())
    .put("coloresFavoritos", p.coloresFavoritos.aJson())
    .put("coloresEvitar", p.coloresEvitar.aJson())
    .put("cuerpo", p.cuerpo.name)
    .put("importanciaModa", p.importanciaModa)
    .put("ciudad", p.ciudad ?: JSONObject.NULL)
    .put("latitud", p.latitud ?: JSONObject.NULL)
    .put("longitud", p.longitud ?: JSONObject.NULL)
    .put("bienvenidaHecha", p.bienvenidaHecha)
    .put("avisoDiario", p.avisoDiario)
    .put("vista", p.vista.name)
    .put("horaAviso", p.horaAviso)
    .put("modo", p.modo.name)
    .put("paleta", p.paleta.name)

  fun perfil(o: JSONObject): Perfil = Perfil(
    estilos = estilos(o.optJSONArray("estilos")),
    coloresFavoritos = o.optJSONArray("coloresFavoritos").textos().toSet(),
    coloresEvitar = o.optJSONArray("coloresEvitar").textos().toSet(),
    cuerpo = Cuerpo.entries.firstOrNull { it.name == o.optString("cuerpo") } ?: Cuerpo.NEUTRO,
    importanciaModa = o.optDouble("importanciaModa", 0.5),
    ciudad = if (o.isNull("ciudad")) null else o.optString("ciudad"),
    latitud = if (o.isNull("latitud")) null else o.optDouble("latitud"),
    longitud = if (o.isNull("longitud")) null else o.optDouble("longitud"),
    bienvenidaHecha = o.optBoolean("bienvenidaHecha"),
    avisoDiario = o.optBoolean("avisoDiario"),
    vista = Vista.entries.firstOrNull { it.name == o.optString("vista") } ?: Vista.AUTO,
    horaAviso = o.optString("horaAviso", "07:30").ifBlank { "07:30" },
    modo = Modo.entries.firstOrNull { it.name == o.optString("modo") } ?: Modo.SISTEMA,
    paleta = Paleta.entries.firstOrNull { it.name == o.optString("paleta") } ?: Paleta.CORAL,
  )

  // ---- Preferencias ----

  fun preferencias(p: Preferencias): JSONObject = JSONObject()
    .put("colores", p.colores.aJson())
    .put("estilos", p.estilos.aJson())
    .put("subtipos", p.subtipos.aJson())
    .put("prendas", p.prendas.aJson())
    .put("pares", p.pares.aJson())

  fun preferencias(o: JSONObject): Preferencias = Preferencias(
    colores = o.optJSONObject("colores").mapa(),
    estilos = o.optJSONObject("estilos").mapa(),
    subtipos = o.optJSONObject("subtipos").mapa(),
    prendas = o.optJSONObject("prendas").mapa(),
    pares = o.optJSONObject("pares").mapa(),
  )

  // ---- Looks guardados ----

  fun looks(lista: List<LookGuardado>) = JSONArray().also { a ->
    lista.forEach { l ->
      a.put(
        JSONObject()
          .put("id", l.id)
          .put("prendas", prendas(l.prendas))
          .put("fecha", l.fecha)
          .put("ocasion", l.ocasion.name)
          .put("nota", l.nota),
      )
    }
  }

  fun looks(a: JSONArray?): List<LookGuardado> =
    if (a == null) emptyList() else (0 until a.length()).map { i ->
      val o = a.getJSONObject(i)
      LookGuardado(
        id = o.getString("id"),
        prendas = prendas(o.optJSONArray("prendas")),
        fecha = o.optLong("fecha"),
        ocasion = Ocasion.entries.firstOrNull { it.name == o.optString("ocasion") } ?: Ocasion.CASUAL,
        nota = o.optString("nota"),
      )
    }

  // ---- Tendencias (archivo tendencias.json) ----

  /**
   * Un texto que puede venir en un solo idioma ("titulo": "...") o en varios
   * ("titulo": {"es": "...", "en": "..."}). Si falta el idioma, inglés y luego español.
   */
  private fun local(o: JSONObject, clave: String, idioma: String): String {
    val v = o.opt(clave) ?: return ""
    if (v !is JSONObject) return v.toString()
    return listOf(idioma, "en", "es").firstNotNullOfOrNull { v.optString(it).takeIf { t -> t.isNotBlank() } }
      ?: v.keys().asSequence().firstOrNull()?.let { v.optString(it) }.orEmpty()
  }

  /** [idioma]: código ISO del teléfono ("es", "en", "pt"...). */
  fun tendencias(texto: String, idioma: String = "es"): CatalogoTendencias {
    val o = JSONObject(texto)
    val lista = o.optJSONArray("tendencias") ?: JSONArray()
    return CatalogoTendencias(
      version = o.optInt("version"),
      actualizado = o.optString("actualizado"),
      tiendaUrl = o.optString("tienda_url").ifBlank { CatalogoTendencias.VACIO.tiendaUrl },
      tendencias = (0 until lista.length()).mapNotNull { i ->
        val t = lista.getJSONObject(i)
        val ideas = t.optJSONArray("prendas") ?: JSONArray()
        Tendencia(
          id = t.getString("id"),
          titulo = local(t, "titulo", idioma),
          descripcion = local(t, "descripcion", idioma),
          tipo = t.optString("tipo", "temporada"),
          fuente = local(t, "fuente", idioma),
          desde = t.optString("desde"),
          hasta = t.optString("hasta"),
          hemisferio = t.optString("hemisferio", "ambos"),
          colores = t.optJSONArray("colores").textos().toSet(),
          estilos = t.optJSONArray("estilos").textos()
            .mapNotNull { n -> Estilo.entries.firstOrNull { it.name.equals(n, ignoreCase = true) } }.toSet(),
          prendas = (0 until ideas.length()).mapNotNull { j ->
            val p = ideas.getJSONObject(j)
            val sub = Subtipo.entries.firstOrNull { it.name.equals(p.optString("subtipo"), ignoreCase = true) }
            sub?.let { IdeaPrenda(it, p.optString("color", "negro"), local(p, "nombre", idioma)) }
          },
          peso = t.optDouble("peso", 1.0),
        )
      },
    )
  }
}
