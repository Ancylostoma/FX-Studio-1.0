package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Los servicios sueltos que el cliente toca para añadir a su pedido: las
 * carpetas de "Diseña tu propia oferta" y las de "Agregar algo más".
 *
 * Antes vivían escritos en el código y solo se podían cambiar recompilando.
 * Ahora se guardan en la base de datos, en la misma tabla de ajustes que el
 * resto de la configuración, y el administrador los edita desde el panel:
 * puede cambiar precios y textos, añadir cosas nuevas (un traje nuevo, un
 * tamaño nuevo), ocultarlas o borrarlas.
 *
 * Mientras el administrador no toque una lista, se usa la de fábrica
 * ([ServiciosDeFabrica]). Al guardarse la primera vez queda la suya.
 *
 * Al vivir en la tabla de ajustes, entran solos en la copia de seguridad.
 */

/** Una cosa concreta que se puede añadir al pedido. */
data class Servicio(
    val id: String = nuevoIdServicio(),
    /** Lo que se lee en grande en la fila, y el nombre con el que entra al pedido. */
    val titulo: String,
    /** Formato, medida o variante ("5x7 / 6x8", "Editado 4K"). Puede ir vacío. */
    val formato: String = "",
    val descripcion: String = "",
    val precio: Double,
    /** Grupo con el que aparece en el pedido, el WhatsApp y el Excel. */
    val grupoPedido: String,
    /**
     * Solo cuenta en "Agregar algo más": en qué tipos de paquete aparece
     * ("Bodas", "15 años", "Primer Año"). Vacía = en todos.
     */
    val paquetes: List<String> = emptyList(),
    /** Oculto = el cliente no lo ve, pero no se pierde. */
    val visible: Boolean = true
) {
    fun apareceEn(categoriaPaquete: String?): Boolean =
        categoriaPaquete == null || paquetes.isEmpty() ||
            paquetes.any { it.equals(categoriaPaquete, ignoreCase = true) }
}

/** Un grupo dentro de una carpeta. Sin nombre = cuelga directo de la carpeta. */
data class Subcarpeta(
    val id: String = nuevoIdServicio(),
    val nombre: String = "",
    val servicios: List<Servicio> = emptyList()
)

/** Una carpeta principal: "Vestuario", "Álbumes", "Video"… */
data class Carpeta(
    val id: String = nuevoIdServicio(),
    val nombre: String,
    /** Un emoji que la identifica de un vistazo. */
    val simbolo: String = "📁",
    val subcarpetas: List<Subcarpeta> = emptyList()
) {
    val servicios: List<Servicio> get() = subcarpetas.flatMap { it.servicios }
}

/** Las dos listas que existen en la app. */
enum class MenuServicios(val clave: String, val titulo: String) {
    PROPIA("servicios_propia", "Diseña tu propia oferta"),
    EXTRAS("servicios_extras", "Agregar algo más")
}

/** Los tipos de paquete que tienen "Agregar algo más". */
val PAQUETES_CON_EXTRAS = listOf("Bodas", "15 años", "Primer Año")

fun nuevoIdServicio(): String = UUID.randomUUID().toString().take(12)

// ------------------------------------------------------------------
// Guardar y leer
// ------------------------------------------------------------------

object ServiciosJson {

    fun escribir(carpetas: List<Carpeta>): String {
        val raiz = JSONArray()
        carpetas.forEach { c ->
            val subs = JSONArray()
            c.subcarpetas.forEach { s ->
                val servs = JSONArray()
                s.servicios.forEach { v ->
                    servs.put(
                        JSONObject()
                            .put("id", v.id)
                            .put("titulo", v.titulo)
                            .put("formato", v.formato)
                            .put("descripcion", v.descripcion)
                            .put("precio", v.precio)
                            .put("grupo", v.grupoPedido)
                            .put("paquetes", JSONArray(v.paquetes))
                            .put("visible", v.visible)
                    )
                }
                subs.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("nombre", s.nombre)
                        .put("servicios", servs)
                )
            }
            raiz.put(
                JSONObject()
                    .put("id", c.id)
                    .put("nombre", c.nombre)
                    .put("simbolo", c.simbolo)
                    .put("subcarpetas", subs)
            )
        }
        return raiz.toString()
    }

    /** Devuelve null si el texto no se puede leer, para caer en la de fábrica. */
    fun leer(texto: String?): List<Carpeta>? {
        if (texto.isNullOrBlank()) return null
        return try {
            val raiz = JSONArray(texto)
            (0 until raiz.length()).map { i ->
                val c = raiz.getJSONObject(i)
                val subs = c.optJSONArray("subcarpetas") ?: JSONArray()
                Carpeta(
                    id = c.optString("id").ifBlank { nuevoIdServicio() },
                    nombre = c.optString("nombre"),
                    simbolo = c.optString("simbolo").ifBlank { "📁" },
                    subcarpetas = (0 until subs.length()).map { j ->
                        val s = subs.getJSONObject(j)
                        val servs = s.optJSONArray("servicios") ?: JSONArray()
                        Subcarpeta(
                            id = s.optString("id").ifBlank { nuevoIdServicio() },
                            nombre = s.optString("nombre"),
                            servicios = (0 until servs.length()).map { k ->
                                val v = servs.getJSONObject(k)
                                val paq = v.optJSONArray("paquetes") ?: JSONArray()
                                Servicio(
                                    id = v.optString("id").ifBlank { nuevoIdServicio() },
                                    titulo = v.optString("titulo"),
                                    formato = v.optString("formato"),
                                    descripcion = v.optString("descripcion"),
                                    precio = v.optDouble("precio", 0.0).takeUnless { it.isNaN() } ?: 0.0,
                                    grupoPedido = v.optString("grupo"),
                                    paquetes = (0 until paq.length()).map { paq.getString(it) },
                                    visible = v.optBoolean("visible", true)
                                )
                            }
                        )
                    }
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

// ------------------------------------------------------------------
// Cambios que hace el administrador. Todos devuelven una lista nueva.
// ------------------------------------------------------------------

/** Mueve un elemento una posición arriba (-1) o abajo (+1). */
private fun <T> List<T>.moverEn(indice: Int, delta: Int): List<T> {
    val destino = indice + delta
    if (indice !in indices || destino !in indices) return this
    val copia = toMutableList()
    val e = copia.removeAt(indice)
    copia.add(destino, e)
    return copia
}

/** Crea la carpeta o, si ya existe con ese id, le cambia nombre y símbolo. */
fun List<Carpeta>.guardarCarpeta(carpeta: Carpeta): List<Carpeta> =
    if (any { it.id == carpeta.id }) {
        map { if (it.id == carpeta.id) it.copy(nombre = carpeta.nombre, simbolo = carpeta.simbolo) else it }
    } else {
        this + carpeta
    }

fun List<Carpeta>.quitarCarpeta(id: String): List<Carpeta> = filterNot { it.id == id }

fun List<Carpeta>.moverCarpeta(id: String, delta: Int): List<Carpeta> =
    moverEn(indexOfFirst { it.id == id }, delta)

/** Crea la subcarpeta dentro de la carpeta, o le cambia el nombre. */
fun List<Carpeta>.guardarSubcarpeta(carpetaId: String, sub: Subcarpeta): List<Carpeta> =
    map { c ->
        if (c.id != carpetaId) c
        else if (c.subcarpetas.any { it.id == sub.id }) {
            c.copy(subcarpetas = c.subcarpetas.map { if (it.id == sub.id) it.copy(nombre = sub.nombre) else it })
        } else {
            c.copy(subcarpetas = c.subcarpetas + sub)
        }
    }

fun List<Carpeta>.quitarSubcarpeta(subId: String): List<Carpeta> =
    map { c -> c.copy(subcarpetas = c.subcarpetas.filterNot { it.id == subId }) }

fun List<Carpeta>.moverSubcarpeta(subId: String, delta: Int): List<Carpeta> =
    map { c ->
        val i = c.subcarpetas.indexOfFirst { it.id == subId }
        if (i < 0) c else c.copy(subcarpetas = c.subcarpetas.moverEn(i, delta))
    }

/**
 * Guarda un servicio en la subcarpeta indicada. Si ya existía en esa misma
 * subcarpeta se reemplaza en su sitio; si estaba en otra, se muda al final
 * de la nueva.
 */
fun List<Carpeta>.guardarServicio(subId: String, servicio: Servicio): List<Carpeta> {
    val yaEstaAqui = any { c ->
        c.subcarpetas.any { s -> s.id == subId && s.servicios.any { it.id == servicio.id } }
    }
    return map { c ->
        c.copy(subcarpetas = c.subcarpetas.map { s ->
            when {
                s.id == subId && yaEstaAqui ->
                    s.copy(servicios = s.servicios.map { if (it.id == servicio.id) servicio else it })
                s.id == subId ->
                    s.copy(servicios = s.servicios + servicio)
                else ->
                    s.copy(servicios = s.servicios.filterNot { it.id == servicio.id })
            }
        })
    }
}

fun List<Carpeta>.quitarServicio(id: String): List<Carpeta> =
    map { c ->
        c.copy(subcarpetas = c.subcarpetas.map { s ->
            s.copy(servicios = s.servicios.filterNot { it.id == id })
        })
    }

fun List<Carpeta>.moverServicio(id: String, delta: Int): List<Carpeta> =
    map { c ->
        c.copy(subcarpetas = c.subcarpetas.map { s ->
            val i = s.servicios.indexOfFirst { it.id == id }
            if (i < 0) s else s.copy(servicios = s.servicios.moverEn(i, delta))
        })
    }
