package com.ancylostoma.quemepongo.data

// Modelos puros (sin Android) para que el motor se pueda probar en la JVM.

enum class Categoria {
  SUPERIOR,
  INFERIOR,
  VESTIDO,
  ABRIGO,
  CALZADO,
  ACCESORIO,
}

/** Cómo se dibuja la prenda sobre el maniquí. [capa] decide el orden: menor = más pegado al cuerpo. */
enum class Forma(val capa: Int) {
  PANTALON(10),
  SHORT(10),
  FALDA(10),
  TOP_SIN_MANGAS(20),
  TOP_MANGA_CORTA(20),
  TOP_MANGA_LARGA(20),
  VESTIDO_CORTO(20),
  VESTIDO_LARGO(20),
  CHAQUETA(40),
  ABRIGO_LARGO(40),
  BUFANDA(45),
  CALZADO_DEPORTIVO(50),
  ZAPATO(50),
  TACON(50),
  BOTA(50),
  SANDALIA(50),
  GORRO(60),
  GORRA(60),
  BOLSO(70),
}

enum class Estilo {
  CASUAL,
  ELEGANTE,
  DEPORTIVO,
  URBANO,
  CLASICO,
  MINIMALISTA,
  BOHEMIO,
  ROMANTICO,
}

// Los nombres que ve el usuario están en los recursos de cada idioma (ui/Textos.kt).
enum class Subtipo(
  val categoria: Categoria,
  val forma: Forma,
  /** 0 = muy fresco, 5 = muy abrigado. */
  val abrigo: Int,
  /** 1 = deporte, 5 = gala. */
  val formalidad: Int,
  val estilos: Set<Estilo>,
  val impermeable: Boolean = false,
) {
  CAMISETA(Categoria.SUPERIOR, Forma.TOP_MANGA_CORTA, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO, Estilo.URBANO, Estilo.MINIMALISTA)),
  TOP(Categoria.SUPERIOR, Forma.TOP_SIN_MANGAS, 1, 2, setOf(Estilo.CASUAL, Estilo.ROMANTICO, Estilo.BOHEMIO)),
  POLO(Categoria.SUPERIOR, Forma.TOP_MANGA_CORTA, 1, 2, setOf(Estilo.CASUAL, Estilo.CLASICO)),
  CAMISA(Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 2, 4, setOf(Estilo.CLASICO, Estilo.ELEGANTE, Estilo.MINIMALISTA)),
  BLUSA(Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 2, 3, setOf(Estilo.ROMANTICO, Estilo.ELEGANTE, Estilo.BOHEMIO)),
  SUETER(Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 3, 3, setOf(Estilo.CLASICO, Estilo.CASUAL, Estilo.MINIMALISTA)),
  SUDADERA(Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 3, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO, Estilo.CASUAL)),
  JEANS(Categoria.INFERIOR, Forma.PANTALON, 2, 2, setOf(Estilo.CASUAL, Estilo.URBANO, Estilo.CLASICO)),
  PANTALON_VESTIR(Categoria.INFERIOR, Forma.PANTALON, 2, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO, Estilo.MINIMALISTA)),
  JOGGER(Categoria.INFERIOR, Forma.PANTALON, 2, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO)),
  SHORT(Categoria.INFERIOR, Forma.SHORT, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO)),
  FALDA(Categoria.INFERIOR, Forma.FALDA, 1, 3, setOf(Estilo.ROMANTICO, Estilo.ELEGANTE, Estilo.BOHEMIO)),
  VESTIDO(Categoria.VESTIDO, Forma.VESTIDO_CORTO, 1, 3, setOf(Estilo.ROMANTICO, Estilo.CASUAL, Estilo.ELEGANTE)),
  VESTIDO_LARGO(Categoria.VESTIDO, Forma.VESTIDO_LARGO, 2, 4, setOf(Estilo.ELEGANTE, Estilo.BOHEMIO, Estilo.ROMANTICO)),
  CHAQUETA(Categoria.ABRIGO, Forma.CHAQUETA, 2, 2, setOf(Estilo.CASUAL, Estilo.URBANO)),
  CAZADORA(Categoria.ABRIGO, Forma.CHAQUETA, 2, 3, setOf(Estilo.URBANO, Estilo.ELEGANTE)),
  BLAZER(Categoria.ABRIGO, Forma.CHAQUETA, 2, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO, Estilo.MINIMALISTA)),
  CARDIGAN(Categoria.ABRIGO, Forma.CHAQUETA, 2, 2, setOf(Estilo.CASUAL, Estilo.ROMANTICO, Estilo.BOHEMIO)),
  ABRIGO(Categoria.ABRIGO, Forma.ABRIGO_LARGO, 4, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO)),
  PLUMIFERO(Categoria.ABRIGO, Forma.CHAQUETA, 4, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO, Estilo.CASUAL)),
  IMPERMEABLE(Categoria.ABRIGO, Forma.ABRIGO_LARGO, 2, 3, setOf(Estilo.CLASICO, Estilo.CASUAL), impermeable = true),
  TENIS(Categoria.CALZADO, Forma.CALZADO_DEPORTIVO, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO, Estilo.URBANO)),
  ZAPATOS(Categoria.CALZADO, Forma.ZAPATO, 1, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO)),
  TACONES(Categoria.CALZADO, Forma.TACON, 0, 5, setOf(Estilo.ELEGANTE, Estilo.ROMANTICO)),
  BOTAS(Categoria.CALZADO, Forma.BOTA, 2, 3, setOf(Estilo.CASUAL, Estilo.URBANO, Estilo.BOHEMIO)),
  SANDALIAS(Categoria.CALZADO, Forma.SANDALIA, 0, 1, setOf(Estilo.CASUAL, Estilo.BOHEMIO)),
  GORRO(Categoria.ACCESORIO, Forma.GORRO, 1, 1, setOf(Estilo.CASUAL, Estilo.URBANO)),
  GORRA(Categoria.ACCESORIO, Forma.GORRA, 0, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO)),
  BUFANDA(Categoria.ACCESORIO, Forma.BUFANDA, 1, 2, setOf(Estilo.CLASICO, Estilo.CASUAL)),
  BOLSO(Categoria.ACCESORIO, Forma.BOLSO, 0, 3, setOf(Estilo.CASUAL, Estilo.ELEGANTE));

  companion object {
    fun deCategoria(c: Categoria) = entries.filter { it.categoria == c }
    fun porNombre(n: String?): Subtipo? = entries.firstOrNull { it.name == n }
  }
}

data class Prenda(
  val id: String,
  val nombre: String,
  val subtipo: Subtipo,
  /** Clave de [Colores]. */
  val color: String,
  val estilos: Set<Estilo> = subtipo.estilos,
  val abrigo: Int = subtipo.abrigo,
  val formalidad: Int = subtipo.formalidad,
  val impermeable: Boolean = subtipo.impermeable,
  /** Nombre del archivo de la foto dentro de la carpeta de prendas. */
  val foto: String? = null,
  val creada: Long = 0L,
  val usos: Int = 0,
  val ultimoUso: Long = 0L,
  /** Prenda que el usuario no tiene: idea para completar el look (y comprarla). */
  val virtual: Boolean = false,
) {
  val categoria get() = subtipo.categoria
}

enum class Ocasion(val formalidad: Double) {
  CASUAL(2.0),
  TRABAJO(3.5),
  CITA(3.3),
  FIESTA(4.0),
  FORMAL(4.8),
  DEPORTE(1.0),
  VIAJE(1.8),
}

enum class Momento(val hora: Int) {
  MANANA(9),
  TARDE(15),
  NOCHE(21);

  companion object {
    fun deHora(h: Int) = when (h) {
      in 5..12 -> MANANA
      in 13..18 -> TARDE
      else -> NOCHE
    }
  }
}

enum class Cuerpo {
  FEMENINO,
  MASCULINO,
  NEUTRO,
}

data class Perfil(
  val estilos: Set<Estilo> = emptySet(),
  val coloresFavoritos: Set<String> = emptySet(),
  val coloresEvitar: Set<String> = emptySet(),
  val cuerpo: Cuerpo = Cuerpo.NEUTRO,
  /** 0 = me da igual la moda, 1 = quiero ir a la última. */
  val importanciaModa: Double = 0.5,
  val ciudad: String? = null,
  val latitud: Double? = null,
  val longitud: Double? = null,
  val bienvenidaHecha: Boolean = false,
  /** Notificación cada mañana con el look del día. */
  val avisoDiario: Boolean = false,
  val vista: Vista = Vista.AUTO,
)

/** Cómo se muestra un look: en el maniquí o como collage de las fotos de las prendas. */
enum class Vista { AUTO, MANIQUI, COLLAGE }

/** El tiempo que hará en el momento elegido. */
data class CondicionClima(
  val temperatura: Double,
  val sensacion: Double,
  val lluvia: Boolean,
  val nieve: Boolean = false,
  val vientoMs: Double = 0.0,
  val soleado: Boolean = false,
  val cielo: Cielo = Cielo.DESCONOCIDO,
)

/** Cómo estará el cielo (el texto en cada idioma lo pone la interfaz). */
enum class Cielo { DESPEJADO, POCO_NUBOSO, PARCIAL, NUBLADO, NIEBLA, LLOVIZNA, LLUVIA, LLUVIA_FUERTE, AGUANIEVE, NIEVE, TORMENTA, DESCONOCIDO }

/** Lo aprendido de los "me gusta", "no me gusta" y looks usados. Valores entre -3 y 3. */
data class Preferencias(
  val colores: Map<String, Double> = emptyMap(),
  val estilos: Map<String, Double> = emptyMap(),
  val subtipos: Map<String, Double> = emptyMap(),
  val prendas: Map<String, Double> = emptyMap(),
  val pares: Map<String, Double> = emptyMap(),
)

/** Prenda que no tienes. [nombre] vacío = se nombra por su tipo y color en el idioma del teléfono. */
data class IdeaPrenda(val subtipo: Subtipo, val color: String, val nombre: String = "")

data class Tendencia(
  val id: String,
  val titulo: String,
  val descripcion: String,
  /** temporada, evento o famosos. */
  val tipo: String,
  val fuente: String,
  /** "MM-DD" (se repite cada año) o "AAAA-MM-DD". Vacío = siempre. */
  val desde: String,
  val hasta: String,
  /** norte, sur o ambos. */
  val hemisferio: String,
  val colores: Set<String>,
  val estilos: Set<Estilo>,
  val prendas: List<IdeaPrenda>,
  val peso: Double,
)

data class CatalogoTendencias(
  val version: Int,
  val actualizado: String,
  /** Plantilla de búsqueda en tienda; {q} se reemplaza por la prenda. */
  val tiendaUrl: String,
  val tendencias: List<Tendencia>,
) {
  companion object {
    val VACIO = CatalogoTendencias(0, "", "https://www.google.com/search?tbm=shop&q={q}", emptyList())
  }
}

data class Look(
  val id: String,
  val prendas: List<Prenda>,
  val puntuacion: Double,
  val razones: List<Razon>,
  val tendencia: String? = null,
)

/** Por qué se eligió un look. Son datos: cada idioma los escribe a su manera. */
sealed interface Razon {
  /** Va a llover; [impermeable] es la prenda que protege, si la hay. */
  data class Lluvia(val impermeable: Prenda?) : Razon
  data class Frio(val sensacion: Double) : Razon
  data class Calor(val sensacion: Double) : Razon
  data class Templado(val sensacion: Double) : Razon
  data class ParaOcasion(val ocasion: Ocasion) : Razon
  data class ColoresCombinan(val colores: List<String>) : Razon
  data class EnTendencia(val titulo: String) : Razon
  data class TuEstilo(val estilo: Estilo) : Razon
  data object Parecido : Razon
  data class TeFalta(val prenda: Prenda) : Razon
  /** Texto ya escrito (la descripción de una tendencia). */
  data class Texto(val texto: String) : Razon
}

data class LookGuardado(
  val id: String,
  val prendas: List<Prenda>,
  val fecha: Long,
  val ocasion: Ocasion,
  val nota: String = "",
)
