package com.ancylostoma.quemepongo.motor

import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.CondicionClima
import com.ancylostoma.quemepongo.data.Estilo
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Subtipo
import com.ancylostoma.quemepongo.data.Tendencia
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class Contexto(
  /** null = no se sabe el tiempo; se asume un día templado. */
  val clima: CondicionClima?,
  val ocasion: Ocasion,
  val perfil: Perfil,
  val preferencias: Preferencias,
  val tendencias: List<Tendencia>,
  val ahoraMs: Long,
)

/**
 * Arma los looks. Sin red ni Android: recibe el armario y el contexto
 * (tiempo, ocasión, gustos, tendencias del momento) y devuelve los mejores
 * conjuntos con las razones de cada uno.
 */
object Motor {
  private const val DIA_MS = 24L * 60 * 60 * 1000
  private const val POR_CATEGORIA = 7

  private val climaTemplado = CondicionClima(temperatura = 21.0, sensacion = 21.0, lluvia = false)

  /** Nivel de abrigo ideal (1-5.5) para una sensación térmica. */
  fun abrigoIdeal(sensacion: Double): Double = ((30 - sensacion) / 5 + 1).coerceIn(1.0, 5.5)

  fun sugerir(armario: List<Prenda>, ctx: Contexto, cantidad: Int = 5, semilla: Long = 0L): List<Look> {
    val clima = ctx.clima ?: climaTemplado
    val ideal = abrigoIdeal(clima.sensacion)
    val azar = Random(semilla)
    val reales = armario.filter { !it.virtual }

    fun deCat(c: Categoria) = reales.filter { it.categoria == c }
    fun mejores(lista: List<Prenda>) = lista.sortedByDescending { puntuarSuelta(it, ctx) }.take(POR_CATEGORIA)

    var superiores = deCat(Categoria.SUPERIOR)
    var inferiores = deCat(Categoria.INFERIOR)
    val vestidos = deCat(Categoria.VESTIDO)
    var calzado = deCat(Categoria.CALZADO)
    var abrigos = deCat(Categoria.ABRIGO)

    // Lo que falte en el armario se completa con prendas "virtuales": ideas de compra.
    // Con solo vestidos no hace falta inventar partes de arriba ni de abajo.
    if (superiores.isEmpty() && (vestidos.isEmpty() || inferiores.isNotEmpty())) {
      superiores = Catalogo.virtuales(Categoria.SUPERIOR, ctx)
    }
    if (inferiores.isEmpty() && (vestidos.isEmpty() || superiores.isNotEmpty())) {
      inferiores = Catalogo.virtuales(Categoria.INFERIOR, ctx)
    }
    if (calzado.isEmpty()) calzado = Catalogo.virtuales(Categoria.CALZADO, ctx)
    if (abrigos.isEmpty() && (ideal >= 3.3 || clima.lluvia)) abrigos = Catalogo.virtuales(Categoria.ABRIGO, ctx)

    val bases = mutableListOf<List<Prenda>>()
    val sup = mejores(superiores)
    val inf = mejores(inferiores)
    for (s in sup) for (i in inf) bases.add(listOf(s, i))
    for (v in mejores(vestidos)) bases.add(listOf(v))

    val zapatos = mejores(calzado).take(4)
    val capas: List<Prenda?> = listOf<Prenda?>(null) + mejores(abrigos).take(4)

    data class Candidato(val prendas: List<Prenda>, val puntos: Double)
    val candidatos = ArrayList<Candidato>(bases.size * zapatos.size.coerceAtLeast(1) * capas.size)
    for (b in bases) for (z in zapatos.ifEmpty { listOf(null) }) for (a in capas) {
      val p = b + listOfNotNull(a, z)
      candidatos.add(Candidato(p, puntuar(p, ctx) + azar.nextDouble() * 0.6))
    }

    // Elegir los mejores, pero distintos entre sí.
    val elegidos = mutableListOf<Candidato>()
    val usosBase = HashMap<String, Int>()
    for (c in candidatos.sortedByDescending { it.puntos }) {
      if (elegidos.size >= cantidad) break
      val baseId = c.prendas.first().id
      if ((usosBase[baseId] ?: 0) >= 1 && elegidos.size < cantidad - 1) continue
      val ids = c.prendas.map { it.id }.toSet()
      val parecido = elegidos.any { e -> e.prendas.count { it.id in ids } >= c.prendas.size - 1 }
      if (parecido) continue
      elegidos.add(c)
      usosBase[baseId] = (usosBase[baseId] ?: 0) + 1
    }

    val accesorios = deCat(Categoria.ACCESORIO)
    return elegidos.map { c ->
      val completo = conAccesorios(c.prendas, accesorios, ctx)
      Look(
        id = completo.map { it.id }.sorted().joinToString("|"),
        prendas = completo,
        puntuacion = puntuar(completo, ctx),
        razones = explicar(completo, ctx),
        tendencia = mejorTendencia(completo, ctx)?.titulo,
      )
    }
  }

  /** El look de una tendencia, usando lo que el usuario ya tiene cuando se parece. */
  fun lookDeTendencia(t: Tendencia, armario: List<Prenda>, ctx: Contexto): Look {
    val usadas = mutableSetOf<String>()
    val prendas = t.prendas.map { idea ->
      val propia = armario.filter { !it.virtual && it.id !in usadas }
        .filter { it.subtipo == idea.subtipo || it.subtipo.forma == idea.subtipo.forma }
        .maxByOrNull { (if (it.color == idea.color) 2 else 0) + (if (it.subtipo == idea.subtipo) 1 else 0) }
        ?.takeIf { it.color == idea.color || Colores.de(it.color).neutro == Colores.de(idea.color).neutro }
      (propia ?: Catalogo.desdeIdea(idea, "t-${t.id}")).also { usadas.add(it.id) }
    }
    return Look(
      id = "t-${t.id}",
      prendas = prendas,
      puntuacion = puntuar(prendas, ctx),
      razones = listOf(t.descripcion) + prendas.filter { it.virtual }.map { "Te falta: ${it.nombre}" },
      tendencia = t.titulo,
    )
  }

  // ---- Puntuación ----

  /** Puntos de una prenda sola, para preseleccionar candidatas. */
  fun puntuarSuelta(p: Prenda, ctx: Contexto): Double {
    var s = 0.0
    s -= abs(p.formalidad - ctx.ocasion.formalidad) * 0.8
    s += puntosEstilo(listOf(p), ctx)
    s += puntosColorPersonal(listOf(p), ctx.perfil)
    s += puntosTendencia(listOf(p), ctx)
    s += puntosPreferencia(listOf(p), ctx.preferencias)
    s += puntosRotacion(p, ctx.ahoraMs)
    if (p.virtual) s -= 1.0
    val clima = ctx.clima ?: climaTemplado
    if (p.categoria == Categoria.SUPERIOR || p.categoria == Categoria.VESTIDO || p.categoria == Categoria.INFERIOR) {
      // Una prenda muy abrigada en pleno calor no tiene arreglo con capas.
      val ideal = abrigoIdeal(clima.sensacion)
      if (p.abrigo > ideal + 1) s -= (p.abrigo - ideal - 1) * 1.5
    }
    return s
  }

  fun abrigoDelLook(prendas: List<Prenda>): Double {
    val vestido = prendas.firstOrNull { it.categoria == Categoria.VESTIDO }
    val sup = prendas.firstOrNull { it.categoria == Categoria.SUPERIOR }
    val inf = prendas.firstOrNull { it.categoria == Categoria.INFERIOR }
    var base = when {
      vestido != null -> vestido.abrigo.toDouble()
      sup != null && inf != null -> sup.abrigo * 0.6 + inf.abrigo * 0.4
      else -> (sup ?: inf)?.abrigo?.toDouble() ?: 1.0
    }
    prendas.filter { it.categoria == Categoria.ABRIGO }.forEach { base += it.abrigo * 0.7 }
    prendas.filter { it.subtipo == Subtipo.BUFANDA || it.subtipo == Subtipo.GORRO }.forEach { base += 0.3 }
    return base
  }

  fun puntuar(prendas: List<Prenda>, ctx: Contexto): Double {
    val clima = ctx.clima ?: climaTemplado
    val ideal = abrigoIdeal(clima.sensacion)
    var s = 0.0

    s -= abs(abrigoDelLook(prendas) - ideal) * 2.0

    val abrigo = prendas.firstOrNull { it.categoria == Categoria.ABRIGO }
    val calzado = prendas.firstOrNull { it.categoria == Categoria.CALZADO }
    if (clima.sensacion >= 26 && abrigo != null) s -= 2.5
    if (clima.lluvia) {
      s += if (abrigo?.impermeable == true) 2.0 else if (abrigo == null) -1.5 else 0.0
      when (calzado?.subtipo?.forma) {
        Forma.SANDALIA -> s -= 3.0
        Forma.BOTA -> s += 1.0
        Forma.TACON -> s -= 1.0
        else -> {}
      }
      if (prendas.any { it.subtipo.forma == Forma.SHORT }) s -= 0.8
    }
    if (clima.nieve && calzado?.subtipo?.forma == Forma.BOTA) s += 2.0
    if (ideal >= 4 && calzado?.subtipo?.forma == Forma.SANDALIA) s -= 2.5

    val formalidad = prendas.map { it.formalidad }.average()
    s -= abs(formalidad - ctx.ocasion.formalidad) * 1.5
    if (ctx.ocasion == Ocasion.DEPORTE) {
      s += prendas.count { Estilo.DEPORTIVO in it.estilos } * 1.0
      if (calzado?.subtipo != Subtipo.TENIS) s -= 3.0
    }
    if (ctx.ocasion == Ocasion.FORMAL && calzado?.subtipo == Subtipo.TENIS) s -= 2.0

    s += Colores.armonia(prendas.map { it.color })
    s += puntosEstilo(prendas, ctx)
    s += puntosColorPersonal(prendas, ctx.perfil)
    s += puntosTendencia(prendas, ctx)
    s += puntosPreferencia(prendas, ctx.preferencias)
    prendas.forEach { s += puntosRotacion(it, ctx.ahoraMs) }
    s -= prendas.count { it.virtual } * 1.0
    return s
  }

  private fun puntosEstilo(prendas: List<Prenda>, ctx: Contexto): Double {
    val mios = ctx.perfil.estilos
    if (mios.isEmpty()) return 0.0
    return prendas.count { p -> p.estilos.any { it in mios } } * 0.6
  }

  private fun puntosColorPersonal(prendas: List<Prenda>, perfil: Perfil): Double =
    prendas.sumOf { p ->
      when (p.color) {
        in perfil.coloresEvitar -> -2.0
        in perfil.coloresFavoritos -> 0.5
        else -> 0.0
      }
    }

  private fun puntosTendencia(prendas: List<Prenda>, ctx: Contexto): Double {
    if (ctx.tendencias.isEmpty()) return 0.0
    val factor = ctx.perfil.importanciaModa * 2
    return ctx.tendencias.maxOf { t -> afinidad(prendas, t) } * factor
  }

  private fun afinidad(prendas: List<Prenda>, t: Tendencia): Double {
    var s = 0.0
    for (p in prendas) {
      if (p.color in t.colores) s += 0.4
      if (p.estilos.any { it in t.estilos }) s += 0.25
      if (t.prendas.any { it.subtipo == p.subtipo }) s += 0.35
    }
    return s * t.peso
  }

  private fun mejorTendencia(prendas: List<Prenda>, ctx: Contexto): Tendencia? =
    ctx.tendencias.maxByOrNull { afinidad(prendas, it) }?.takeIf { afinidad(prendas, it) >= 1.0 }

  private fun puntosPreferencia(prendas: List<Prenda>, pref: Preferencias): Double {
    var s = 0.0
    for (p in prendas) {
      s += (pref.colores[p.color] ?: 0.0) * 0.5
      s += p.estilos.sumOf { pref.estilos[it.name] ?: 0.0 } / p.estilos.size.coerceAtLeast(1) * 0.5
      s += (pref.subtipos[p.subtipo.name] ?: 0.0) * 0.3
      s += (pref.prendas[p.id] ?: 0.0) * 0.4
    }
    val ids = prendas.filter { !it.virtual }.map { it.id }.sorted()
    for (i in ids.indices) for (j in i + 1 until ids.size) s += (pref.pares["${ids[i]}|${ids[j]}"] ?: 0.0) * 0.6
    return s
  }

  /** Para no repetir la misma ropa todos los días. */
  private fun puntosRotacion(p: Prenda, ahora: Long): Double {
    if (p.virtual || p.ultimoUso == 0L) return 0.0
    val dias = (ahora - p.ultimoUso) / DIA_MS
    return when {
      dias < 1 -> -1.5
      dias < 3 -> -0.6
      else -> 0.0
    }
  }

  private fun conAccesorios(prendas: List<Prenda>, accesorios: List<Prenda>, ctx: Contexto): List<Prenda> {
    if (accesorios.isEmpty()) return prendas
    val clima = ctx.clima ?: climaTemplado
    val ideal = abrigoIdeal(clima.sensacion)
    val resultado = prendas.toMutableList()
    val formasPuestas = mutableSetOf<Forma>()
    val opciones = accesorios.map { a ->
      var s = when (a.subtipo) {
        Subtipo.GORRO, Subtipo.BUFANDA -> if (ideal >= 4) 1.5 else -2.0
        Subtipo.GORRA -> if (clima.soleado && clima.sensacion >= 22 &&
          (ctx.ocasion == Ocasion.CASUAL || ctx.ocasion == Ocasion.DEPORTE || ctx.ocasion == Ocasion.VIAJE)) 0.6 else -1.0
        else -> if (ctx.ocasion != Ocasion.DEPORTE) 0.5 else -1.0
      }
      s += Colores.armonia(prendas.map { it.color } + a.color) - Colores.armonia(prendas.map { it.color })
      s += puntosEstilo(listOf(a), ctx) + puntosPreferencia(listOf(a), ctx.preferencias) + puntosColorPersonal(listOf(a), ctx.perfil)
      a to s
    }.sortedByDescending { it.second }
    for ((a, s) in opciones) {
      if (resultado.size - prendas.size >= 2) break
      if (s < 0.3 || a.subtipo.forma in formasPuestas) continue
      resultado.add(a)
      formasPuestas.add(a.subtipo.forma)
    }
    return resultado
  }

  // ---- Razones ----

  fun explicar(prendas: List<Prenda>, ctx: Contexto): List<String> {
    val r = mutableListOf<String>()
    val clima = ctx.clima
    if (clima != null) {
      val t = clima.sensacion.roundToInt()
      val abrigo = prendas.firstOrNull { it.categoria == Categoria.ABRIGO }
      when {
        clima.lluvia && abrigo?.impermeable == true -> r.add("Va a llover: ${abrigo.nombre.lowercase()} y calzado cerrado")
        clima.lluvia -> r.add("Va a llover: evita el calzado abierto")
        abrigoIdeal(clima.sensacion) >= 4 -> r.add("Sensación de $t °C: capas que abrigan")
        clima.sensacion >= 26 -> r.add("Calor ($t °C): prendas frescas y sin capas")
        else -> r.add("Sensación de $t °C: abrigo justo")
      }
    }
    r.add("Formalidad ideal para: ${ctx.ocasion.etiqueta.lowercase()}")

    val claves = prendas.map { it.color }.distinct()
    if (Colores.armonia(prendas.map { it.color }) >= 0.7) {
      r.add("Colores que combinan: " + claves.take(3).joinToString(" + ") { Colores.de(it).etiqueta.lowercase() })
    }
    mejorTendencia(prendas, ctx)?.let { r.add("En tendencia: ${it.titulo}") }

    val estilosMios = ctx.perfil.estilos
    val comunes = prendas.flatMap { it.estilos }.filter { it in estilosMios }.groupingBy { it }.eachCount()
    comunes.maxByOrNull { it.value }?.let { r.add("Va con tu estilo ${it.key.etiqueta.lowercase()}") }

    if (puntosPreferencia(prendas, ctx.preferencias) > 1.0) r.add("Se parece a looks que te gustaron")
    prendas.filter { it.virtual }.forEach { r.add("Te falta: ${it.nombre.lowercase()} (idea de compra)") }
    return r
  }

  // ---- Aprendizaje ----

  /** Ajusta las preferencias con una señal: +1 me gusta, -1 no me gusta, +1.5 guardado o usado. */
  fun aprender(p: Preferencias, prendas: List<Prenda>, senal: Double): Preferencias {
    fun MutableMap<String, Double>.sumar(k: String, v: Double) {
      this[k] = ((this[k] ?: 0.0) + v).coerceIn(-3.0, 3.0)
    }
    val colores = p.colores.toMutableMap()
    val estilos = p.estilos.toMutableMap()
    val subtipos = p.subtipos.toMutableMap()
    val idPrendas = p.prendas.toMutableMap()
    val pares = p.pares.toMutableMap()
    for (x in prendas) {
      colores.sumar(x.color, 0.3 * senal)
      x.estilos.forEach { estilos.sumar(it.name, 0.15 * senal) }
      subtipos.sumar(x.subtipo.name, 0.2 * senal)
      if (!x.virtual) idPrendas.sumar(x.id, 0.4 * senal)
    }
    val ids = prendas.filter { !it.virtual }.map { it.id }.sorted()
    for (i in ids.indices) for (j in i + 1 until ids.size) pares.sumar("${ids[i]}|${ids[j]}", 0.5 * senal)
    return Preferencias(colores, estilos, subtipos, idPrendas, pares)
  }
}

/** Prendas genéricas para completar looks cuando el armario no tiene algo. */
object Catalogo {
  val base = listOf(
    IdeaPrenda(Subtipo.CAMISETA, "blanco", "Camiseta blanca"),
    IdeaPrenda(Subtipo.CAMISETA, "negro", "Camiseta negra"),
    IdeaPrenda(Subtipo.CAMISA, "celeste", "Camisa celeste"),
    IdeaPrenda(Subtipo.CAMISA, "blanco", "Camisa blanca"),
    IdeaPrenda(Subtipo.SUETER, "beige", "Suéter beige"),
    IdeaPrenda(Subtipo.SUETER, "marino", "Suéter azul marino"),
    IdeaPrenda(Subtipo.SUDADERA, "gris", "Sudadera gris"),
    IdeaPrenda(Subtipo.JEANS, "denim", "Jeans clásicos"),
    IdeaPrenda(Subtipo.PANTALON_VESTIR, "negro", "Pantalón de vestir negro"),
    IdeaPrenda(Subtipo.PANTALON_VESTIR, "beige", "Pantalón chino beige"),
    IdeaPrenda(Subtipo.JOGGER, "negro", "Jogger negro"),
    IdeaPrenda(Subtipo.SHORT, "beige", "Short beige"),
    IdeaPrenda(Subtipo.CHAQUETA, "denim", "Chaqueta de mezclilla"),
    IdeaPrenda(Subtipo.BLAZER, "marino", "Blazer azul marino"),
    IdeaPrenda(Subtipo.ABRIGO, "camel", "Abrigo camel"),
    IdeaPrenda(Subtipo.PLUMIFERO, "negro", "Plumífero negro"),
    IdeaPrenda(Subtipo.IMPERMEABLE, "beige", "Gabardina beige"),
    IdeaPrenda(Subtipo.TENIS, "blanco", "Tenis blancos"),
    IdeaPrenda(Subtipo.ZAPATOS, "negro", "Mocasines negros"),
    IdeaPrenda(Subtipo.BOTAS, "marron", "Botines marrones"),
    IdeaPrenda(Subtipo.SANDALIAS, "marron", "Sandalias de cuero"),
  )

  fun desdeIdea(idea: IdeaPrenda, prefijo: String = "v"): Prenda = Prenda(
    id = "$prefijo-${idea.subtipo.name}-${idea.color}",
    nombre = idea.nombre,
    subtipo = idea.subtipo,
    color = idea.color,
    virtual = true,
  )

  fun virtuales(c: Categoria, ctx: Contexto): List<Prenda> {
    val deModa = ctx.tendencias.flatMap { it.prendas }.filter { it.subtipo.categoria == c }
    return (deModa + base.filter { it.subtipo.categoria == c })
      .distinctBy { it.subtipo to it.color }
      .map { desdeIdea(it) }
  }
}
