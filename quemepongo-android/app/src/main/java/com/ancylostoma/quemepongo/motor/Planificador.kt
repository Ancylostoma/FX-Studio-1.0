package com.ancylostoma.quemepongo.motor

import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.CondicionClima
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Tendencia
import java.time.LocalDate

/** Un día a planear: su fecha, el tiempo que hará (si se sabe) y para qué es. */
data class DiaPlan(val fecha: LocalDate, val clima: CondicionClima?, val ocasion: Ocasion)

data class Maleta(
  /** Un look por día de viaje. */
  val looks: List<Look>,
  /** Lo que hay que meter en la maleta, sin repetir. */
  val prendas: List<Prenda>,
  /** Lo que faltaría para completar los looks (ideas de compra). */
  val faltan: List<Prenda>,
)

/** Funciones Premium: planear varios días y armar la maleta. */
object Planificador {
  private const val DIA_MS = 24L * 60 * 60 * 1000

  private fun mediodia(f: LocalDate) = f.toEpochDay() * DIA_MS + 12 * 3_600_000L

  /**
   * Un look por día. Cada prenda elegida cuenta como "usada" ese día, así el
   * motor no la repite en los días siguientes (su penalización por rotación).
   */
  fun semana(
    armario: List<Prenda>,
    dias: List<DiaPlan>,
    perfil: Perfil,
    pref: Preferencias,
    tendencias: (LocalDate) -> List<Tendencia>,
    semilla: Long = 0L,
  ): List<Look> {
    var prendas = armario
    val veces = HashMap<String, Int>()
    var arribaDeAyer: String? = null
    return dias.mapIndexed { i, d ->
      val ahora = mediodia(d.fecha)
      val ctx = Contexto(d.clima, d.ocasion, perfil, pref, tendencias(d.fecha), ahora)
      val opciones = Motor.sugerir(prendas, ctx, cantidad = 10, semilla = semilla + i)
      // Nunca la misma parte de arriba dos días seguidos, si hay otra opción.
      val sinRepetirAyer = opciones.filter { arriba(it)?.id != arribaDeAyer }.ifEmpty { opciones }
      // Entre esas, la que menos repite lo ya planeado en la semana.
      // Zapatos, abrigos y accesorios se pueden repetir sin problema; la ropa de dentro, no.
      val look = sinRepetirAyer.maxBy { l ->
        l.puntuacion - l.prendas.sumOf { p -> (veces[p.id] ?: 0) * pesoRepetir(p) }
      }
      arribaDeAyer = arriba(look)?.takeIf { !it.virtual }?.id
      val usadas = look.prendas.filter { !it.virtual }.map { it.id }.toSet()
      usadas.forEach { veces[it] = (veces[it] ?: 0) + 1 }
      prendas = prendas.map { if (it.id in usadas) it.copy(ultimoUso = ahora) else it }
      look
    }
  }

  private fun arriba(l: Look) = l.prendas.firstOrNull { it.categoria == Categoria.SUPERIOR || it.categoria == Categoria.VESTIDO }

  private fun pesoRepetir(p: Prenda): Double = when (p.categoria) {
    Categoria.SUPERIOR, Categoria.VESTIDO -> 3.0
    Categoria.INFERIOR -> 1.5
    else -> 0.5
  }

  /**
   * La maleta mínima: para cada día elige, entre los mejores looks, el que más
   * reutiliza lo que ya está en la maleta. Pocas piezas que combinan entre sí.
   */
  fun maleta(
    armario: List<Prenda>,
    dias: List<DiaPlan>,
    perfil: Perfil,
    pref: Preferencias,
    tendencias: (LocalDate) -> List<Tendencia>,
    semilla: Long = 0L,
  ): Maleta {
    // En un viaje no importa haber usado la prenda ayer en casa.
    val limpio = armario.map { it.copy(ultimoUso = 0L) }
    val empacadas = LinkedHashMap<String, Prenda>()
    val looks = dias.mapIndexed { i, d ->
      val ctx = Contexto(d.clima, d.ocasion, perfil, pref, tendencias(d.fecha), mediodia(d.fecha))
      val opciones = Motor.sugerir(limpio, ctx, cantidad = 6, semilla = semilla + i)
      val elegido = opciones.maxBy { l -> l.puntuacion + 1.5 * l.prendas.count { it.id in empacadas } }
      elegido.prendas.filter { !it.virtual }.forEach { empacadas[it.id] = it }
      elegido
    }
    val faltan = looks.flatMap { it.prendas }.filter { it.virtual }.distinctBy { it.subtipo to it.color }
    return Maleta(looks, empacadas.values.sortedBy { it.subtipo.forma.capa }, faltan)
  }
}

data class Estadisticas(
  val total: Int,
  val usosTotales: Int,
  /** Suma de los precios que el usuario anotó; null si no anotó ninguno. */
  val valorTotal: Double?,
  val nuncaUsadas: List<Prenda>,
  val masUsadas: List<Prenda>,
  val porColor: List<Pair<String, Int>>,
  val porCategoria: List<Pair<Categoria, Int>>,
  /** Prendas con precio, de la que más cuesta por uso a la que menos. */
  val costoPorUso: List<Pair<Prenda, Double>>,
)

object Analisis {
  private const val DIA_MS = 24L * 60 * 60 * 1000

  fun estadisticas(armario: List<Prenda>, historial: List<LookGuardado>, ahora: Long): Estadisticas {
    val reales = armario.filter { !it.virtual }
    // "Nunca usada" solo si lleva al menos dos semanas en el armario.
    val nunca = reales.filter { it.usos == 0 && (it.creada == 0L || ahora - it.creada > 14 * DIA_MS) }
    val conPrecio = reales.filter { (it.precio ?: 0.0) > 0.0 }
    return Estadisticas(
      total = reales.size,
      usosTotales = maxOf(reales.sumOf { it.usos }, historial.size),
      valorTotal = conPrecio.takeIf { it.isNotEmpty() }?.sumOf { it.precio ?: 0.0 },
      nuncaUsadas = nunca,
      masUsadas = reales.filter { it.usos > 0 }.sortedByDescending { it.usos }.take(5),
      porColor = reales.groupingBy { it.color }.eachCount().toList().sortedByDescending { it.second },
      porCategoria = reales.groupingBy { it.categoria }.eachCount().toList().sortedByDescending { it.second },
      costoPorUso = conPrecio.map { it to (it.precio ?: 0.0) / maxOf(it.usos, 1) }.sortedByDescending { it.second },
    )
  }
}
