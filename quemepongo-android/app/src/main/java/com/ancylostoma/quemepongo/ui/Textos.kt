package com.ancylostoma.quemepongo.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Cielo
import com.ancylostoma.quemepongo.data.Cuerpo
import com.ancylostoma.quemepongo.data.Estilo
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Momento
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Razon
import com.ancylostoma.quemepongo.data.Subtipo
import com.ancylostoma.quemepongo.motor.Motivo
import java.util.Locale
import kotlin.math.roundToInt

// Puente entre los datos del motor (que no tienen idioma) y los textos de cada idioma.

@StringRes
fun Categoria.res(): Int = when (this) {
  Categoria.SUPERIOR -> R.string.cat_superior
  Categoria.INFERIOR -> R.string.cat_inferior
  Categoria.VESTIDO -> R.string.cat_vestido
  Categoria.ABRIGO -> R.string.cat_abrigo
  Categoria.CALZADO -> R.string.cat_calzado
  Categoria.ACCESORIO -> R.string.cat_accesorio
}

@StringRes
fun Subtipo.res(): Int = when (this) {
  Subtipo.CAMISETA -> R.string.sub_camiseta
  Subtipo.TOP -> R.string.sub_top
  Subtipo.POLO -> R.string.sub_polo
  Subtipo.CAMISA -> R.string.sub_camisa
  Subtipo.BLUSA -> R.string.sub_blusa
  Subtipo.SUETER -> R.string.sub_sueter
  Subtipo.SUDADERA -> R.string.sub_sudadera
  Subtipo.JEANS -> R.string.sub_jeans
  Subtipo.PANTALON_VESTIR -> R.string.sub_pantalon_vestir
  Subtipo.JOGGER -> R.string.sub_jogger
  Subtipo.SHORT -> R.string.sub_short
  Subtipo.FALDA -> R.string.sub_falda
  Subtipo.VESTIDO -> R.string.sub_vestido
  Subtipo.VESTIDO_LARGO -> R.string.sub_vestido_largo
  Subtipo.CHAQUETA -> R.string.sub_chaqueta
  Subtipo.CAZADORA -> R.string.sub_cazadora
  Subtipo.BLAZER -> R.string.sub_blazer
  Subtipo.CARDIGAN -> R.string.sub_cardigan
  Subtipo.ABRIGO -> R.string.sub_abrigo
  Subtipo.PLUMIFERO -> R.string.sub_plumifero
  Subtipo.IMPERMEABLE -> R.string.sub_impermeable
  Subtipo.TENIS -> R.string.sub_tenis
  Subtipo.ZAPATOS -> R.string.sub_zapatos
  Subtipo.TACONES -> R.string.sub_tacones
  Subtipo.BOTAS -> R.string.sub_botas
  Subtipo.SANDALIAS -> R.string.sub_sandalias
  Subtipo.GORRO -> R.string.sub_gorro
  Subtipo.GORRA -> R.string.sub_gorra
  Subtipo.BUFANDA -> R.string.sub_bufanda
  Subtipo.BOLSO -> R.string.sub_bolso
}

@StringRes
fun Estilo.res(): Int = when (this) {
  Estilo.CASUAL -> R.string.estilo_casual
  Estilo.ELEGANTE -> R.string.estilo_elegante
  Estilo.DEPORTIVO -> R.string.estilo_deportivo
  Estilo.URBANO -> R.string.estilo_urbano
  Estilo.CLASICO -> R.string.estilo_clasico
  Estilo.MINIMALISTA -> R.string.estilo_minimalista
  Estilo.BOHEMIO -> R.string.estilo_bohemio
  Estilo.ROMANTICO -> R.string.estilo_romantico
}

@StringRes
fun Ocasion.res(): Int = when (this) {
  Ocasion.CASUAL -> R.string.ocasion_casual
  Ocasion.TRABAJO -> R.string.ocasion_trabajo
  Ocasion.CITA -> R.string.ocasion_cita
  Ocasion.FIESTA -> R.string.ocasion_fiesta
  Ocasion.FORMAL -> R.string.ocasion_formal
  Ocasion.DEPORTE -> R.string.ocasion_deporte
  Ocasion.VIAJE -> R.string.ocasion_viaje
}

@StringRes
fun Momento.res(): Int = when (this) {
  Momento.MANANA -> R.string.momento_manana
  Momento.TARDE -> R.string.momento_tarde
  Momento.NOCHE -> R.string.momento_noche
}

@StringRes
fun Cuerpo.res(): Int = when (this) {
  Cuerpo.FEMENINO -> R.string.cuerpo_femenino
  Cuerpo.MASCULINO -> R.string.cuerpo_masculino
  Cuerpo.NEUTRO -> R.string.cuerpo_neutro
}

@StringRes
fun Cielo.res(): Int = when (this) {
  Cielo.DESPEJADO -> R.string.cielo_despejado
  Cielo.POCO_NUBOSO -> R.string.cielo_poco_nuboso
  Cielo.PARCIAL -> R.string.cielo_parcial
  Cielo.NUBLADO -> R.string.cielo_nublado
  Cielo.NIEBLA -> R.string.cielo_niebla
  Cielo.LLOVIZNA -> R.string.cielo_llovizna
  Cielo.LLUVIA -> R.string.cielo_lluvia
  Cielo.LLUVIA_FUERTE -> R.string.cielo_lluvia_fuerte
  Cielo.AGUANIEVE -> R.string.cielo_aguanieve
  Cielo.NIEVE -> R.string.cielo_nieve
  Cielo.TORMENTA -> R.string.cielo_tormenta
  Cielo.DESCONOCIDO -> R.string.cielo_desconocido
}

@StringRes
fun colorRes(clave: String): Int = when (clave) {
  "negro" -> R.string.color_negro
  "blanco" -> R.string.color_blanco
  "beige" -> R.string.color_beige
  "camel" -> R.string.color_camel
  "marron" -> R.string.color_marron
  "marino" -> R.string.color_marino
  "denim" -> R.string.color_denim
  "oliva" -> R.string.color_oliva
  "azul" -> R.string.color_azul
  "celeste" -> R.string.color_celeste
  "verde" -> R.string.color_verde
  "rojo" -> R.string.color_rojo
  "borgona" -> R.string.color_borgona
  "rosa" -> R.string.color_rosa
  "morado" -> R.string.color_morado
  "amarillo" -> R.string.color_amarillo
  "naranja" -> R.string.color_naranja
  "estampado" -> R.string.color_estampado
  else -> R.string.color_gris
}

// ---- Temperatura: °F en Estados Unidos y pocos países más, °C en el resto ----

private val PAISES_FAHRENHEIT = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")

fun temperatura(celsius: Double): String =
  if (Locale.getDefault().country in PAISES_FAHRENHEIT) "${(celsius * 9 / 5 + 32).roundToInt()}°F" else "${celsius.roundToInt()}°"

// ---- Textos que dependen de los datos (con Context, para usarlos también fuera de la pantalla) ----

/** Nombre de la prenda: el que puso el usuario o "Tipo · Color" en el idioma del teléfono. */
fun Context.nombre(p: Prenda): String =
  p.nombre.ifBlank { getString(R.string.prenda_tipo_color, getString(p.subtipo.res()), getString(colorRes(p.color))) }

fun Context.nombre(i: IdeaPrenda): String =
  i.nombre.ifBlank { getString(R.string.prenda_tipo_color, getString(i.subtipo.res()), getString(colorRes(i.color))) }

/** Lo que se busca en la tienda: "Camiseta Blanco", "T-shirt White". */
fun Context.busqueda(i: IdeaPrenda): String =
  i.nombre.ifBlank { "${getString(i.subtipo.res())} ${getString(colorRes(i.color))}" }

fun Context.texto(r: Razon): String {
  val minusculas = { s: String -> s.lowercase(Locale.getDefault()) }
  return when (r) {
    is Razon.Lluvia ->
      if (r.impermeable != null) getString(R.string.razon_lluvia_con, minusculas(nombre(r.impermeable))) else getString(R.string.razon_lluvia_sin)
    is Razon.Frio -> getString(R.string.razon_frio, temperatura(r.sensacion))
    is Razon.Calor -> getString(R.string.razon_calor, temperatura(r.sensacion))
    is Razon.Templado -> getString(R.string.razon_templado, temperatura(r.sensacion))
    is Razon.ParaOcasion -> getString(R.string.razon_ocasion, minusculas(getString(r.ocasion.res())))
    is Razon.ColoresCombinan -> getString(R.string.razon_colores, r.colores.joinToString(" + ") { minusculas(getString(colorRes(it))) })
    is Razon.EnTendencia -> getString(R.string.razon_tendencia, r.titulo)
    is Razon.TuEstilo -> getString(R.string.razon_estilo, minusculas(getString(r.estilo.res())))
    Razon.Parecido -> getString(R.string.razon_parecido)
    is Razon.TeFalta -> getString(R.string.razon_falta, minusculas(nombre(r.prenda)))
    is Razon.Texto -> r.texto
  }
}

fun Context.texto(m: Motivo): String = when (m) {
  is Motivo.FaltaCategoria -> getString(R.string.motivo_falta, getString(m.categoria.res()).lowercase(Locale.getDefault()))
  is Motivo.EnTendencia -> getString(R.string.motivo_tendencia, m.titulo)
  Motivo.Gusto -> getString(R.string.motivo_gusto)
}

// ---- Atajos para la interfaz ----

@Composable
fun nombre(p: Prenda): String = LocalContext.current.nombre(p)

@Composable
fun nombre(i: IdeaPrenda): String = LocalContext.current.nombre(i)

@Composable
fun texto(r: Razon): String = LocalContext.current.texto(r)

@Composable
fun texto(m: Motivo): String = LocalContext.current.texto(m)

@Composable
fun t(@StringRes id: Int): String = stringResource(id)
