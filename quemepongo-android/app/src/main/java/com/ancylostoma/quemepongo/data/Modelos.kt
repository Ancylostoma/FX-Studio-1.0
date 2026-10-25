package com.ancylostoma.quemepongo.data

// Modelos puros (sin Android) para que el motor se pueda probar en la JVM.

enum class Categoria(val etiqueta: String) {
  SUPERIOR("Arriba"),
  INFERIOR("Abajo"),
  VESTIDO("Vestidos"),
  ABRIGO("Abrigos"),
  CALZADO("Calzado"),
  ACCESORIO("Accesorios"),
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

enum class Estilo(val etiqueta: String) {
  CASUAL("Casual"),
  ELEGANTE("Elegante"),
  DEPORTIVO("Deportivo"),
  URBANO("Urbano"),
  CLASICO("Clásico"),
  MINIMALISTA("Minimalista"),
  BOHEMIO("Bohemio"),
  ROMANTICO("Romántico"),
}

enum class Subtipo(
  val etiqueta: String,
  val categoria: Categoria,
  val forma: Forma,
  /** 0 = muy fresco, 5 = muy abrigado. */
  val abrigo: Int,
  /** 1 = deporte, 5 = gala. */
  val formalidad: Int,
  val estilos: Set<Estilo>,
  val impermeable: Boolean = false,
) {
  CAMISETA("Camiseta", Categoria.SUPERIOR, Forma.TOP_MANGA_CORTA, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO, Estilo.URBANO, Estilo.MINIMALISTA)),
  TOP("Top sin mangas", Categoria.SUPERIOR, Forma.TOP_SIN_MANGAS, 1, 2, setOf(Estilo.CASUAL, Estilo.ROMANTICO, Estilo.BOHEMIO)),
  POLO("Polo", Categoria.SUPERIOR, Forma.TOP_MANGA_CORTA, 1, 2, setOf(Estilo.CASUAL, Estilo.CLASICO)),
  CAMISA("Camisa", Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 2, 4, setOf(Estilo.CLASICO, Estilo.ELEGANTE, Estilo.MINIMALISTA)),
  BLUSA("Blusa", Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 2, 3, setOf(Estilo.ROMANTICO, Estilo.ELEGANTE, Estilo.BOHEMIO)),
  SUETER("Suéter", Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 3, 3, setOf(Estilo.CLASICO, Estilo.CASUAL, Estilo.MINIMALISTA)),
  SUDADERA("Sudadera", Categoria.SUPERIOR, Forma.TOP_MANGA_LARGA, 3, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO, Estilo.CASUAL)),
  JEANS("Jeans", Categoria.INFERIOR, Forma.PANTALON, 2, 2, setOf(Estilo.CASUAL, Estilo.URBANO, Estilo.CLASICO)),
  PANTALON_VESTIR("Pantalón de vestir", Categoria.INFERIOR, Forma.PANTALON, 2, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO, Estilo.MINIMALISTA)),
  JOGGER("Jogger", Categoria.INFERIOR, Forma.PANTALON, 2, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO)),
  SHORT("Short", Categoria.INFERIOR, Forma.SHORT, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO)),
  FALDA("Falda", Categoria.INFERIOR, Forma.FALDA, 1, 3, setOf(Estilo.ROMANTICO, Estilo.ELEGANTE, Estilo.BOHEMIO)),
  VESTIDO("Vestido corto", Categoria.VESTIDO, Forma.VESTIDO_CORTO, 1, 3, setOf(Estilo.ROMANTICO, Estilo.CASUAL, Estilo.ELEGANTE)),
  VESTIDO_LARGO("Vestido largo", Categoria.VESTIDO, Forma.VESTIDO_LARGO, 2, 4, setOf(Estilo.ELEGANTE, Estilo.BOHEMIO, Estilo.ROMANTICO)),
  CHAQUETA("Chaqueta", Categoria.ABRIGO, Forma.CHAQUETA, 2, 2, setOf(Estilo.CASUAL, Estilo.URBANO)),
  CAZADORA("Cazadora de cuero", Categoria.ABRIGO, Forma.CHAQUETA, 2, 3, setOf(Estilo.URBANO, Estilo.ELEGANTE)),
  BLAZER("Blazer", Categoria.ABRIGO, Forma.CHAQUETA, 2, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO, Estilo.MINIMALISTA)),
  CARDIGAN("Cárdigan", Categoria.ABRIGO, Forma.CHAQUETA, 2, 2, setOf(Estilo.CASUAL, Estilo.ROMANTICO, Estilo.BOHEMIO)),
  ABRIGO("Abrigo largo", Categoria.ABRIGO, Forma.ABRIGO_LARGO, 4, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO)),
  PLUMIFERO("Plumífero / parka", Categoria.ABRIGO, Forma.CHAQUETA, 4, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO, Estilo.CASUAL)),
  IMPERMEABLE("Gabardina / impermeable", Categoria.ABRIGO, Forma.ABRIGO_LARGO, 2, 3, setOf(Estilo.CLASICO, Estilo.CASUAL), impermeable = true),
  TENIS("Tenis", Categoria.CALZADO, Forma.CALZADO_DEPORTIVO, 1, 1, setOf(Estilo.CASUAL, Estilo.DEPORTIVO, Estilo.URBANO)),
  ZAPATOS("Zapatos / mocasines", Categoria.CALZADO, Forma.ZAPATO, 1, 4, setOf(Estilo.ELEGANTE, Estilo.CLASICO)),
  TACONES("Tacones", Categoria.CALZADO, Forma.TACON, 0, 5, setOf(Estilo.ELEGANTE, Estilo.ROMANTICO)),
  BOTAS("Botas / botines", Categoria.CALZADO, Forma.BOTA, 2, 3, setOf(Estilo.CASUAL, Estilo.URBANO, Estilo.BOHEMIO)),
  SANDALIAS("Sandalias", Categoria.CALZADO, Forma.SANDALIA, 0, 1, setOf(Estilo.CASUAL, Estilo.BOHEMIO)),
  GORRO("Gorro de lana", Categoria.ACCESORIO, Forma.GORRO, 1, 1, setOf(Estilo.CASUAL, Estilo.URBANO)),
  GORRA("Gorra", Categoria.ACCESORIO, Forma.GORRA, 0, 1, setOf(Estilo.DEPORTIVO, Estilo.URBANO)),
  BUFANDA("Bufanda", Categoria.ACCESORIO, Forma.BUFANDA, 1, 2, setOf(Estilo.CLASICO, Estilo.CASUAL)),
  BOLSO("Bolso / mochila", Categoria.ACCESORIO, Forma.BOLSO, 0, 3, setOf(Estilo.CASUAL, Estilo.ELEGANTE));

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

enum class Ocasion(val etiqueta: String, val formalidad: Double) {
  CASUAL("Día a día", 2.0),
  TRABAJO("Trabajo", 3.5),
  CITA("Cita", 3.3),
  FIESTA("Fiesta", 4.0),
  FORMAL("Evento formal", 4.8),
  DEPORTE("Deporte", 1.0),
  VIAJE("Viaje", 1.8),
}

enum class Momento(val etiqueta: String, val hora: Int) {
  MANANA("Mañana", 9),
  TARDE("Tarde", 15),
  NOCHE("Noche", 21);

  companion object {
    fun deHora(h: Int) = when (h) {
      in 5..12 -> MANANA
      in 13..18 -> TARDE
      else -> NOCHE
    }
  }
}

enum class Cuerpo(val etiqueta: String) {
  FEMENINO("Femenino"),
  MASCULINO("Masculino"),
  NEUTRO("Neutro"),
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
)

/** El tiempo que hará en el momento elegido. */
data class CondicionClima(
  val temperatura: Double,
  val sensacion: Double,
  val lluvia: Boolean,
  val nieve: Boolean = false,
  val vientoMs: Double = 0.0,
  val soleado: Boolean = false,
  val descripcion: String = "",
)

/** Lo aprendido de los "me gusta", "no me gusta" y looks usados. Valores entre -3 y 3. */
data class Preferencias(
  val colores: Map<String, Double> = emptyMap(),
  val estilos: Map<String, Double> = emptyMap(),
  val subtipos: Map<String, Double> = emptyMap(),
  val prendas: Map<String, Double> = emptyMap(),
  val pares: Map<String, Double> = emptyMap(),
)

data class IdeaPrenda(val subtipo: Subtipo, val color: String, val nombre: String)

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
  val razones: List<String>,
  val tendencia: String? = null,
)

data class LookGuardado(
  val id: String,
  val prendas: List<Prenda>,
  val fecha: Long,
  val ocasion: Ocasion,
  val nota: String = "",
)
