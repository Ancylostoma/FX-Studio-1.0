package com.example.data

import java.util.Locale

/**
 * Las medidas del catálogo ("6x8", "16x24", "39x82.67") vienen en pulgadas,
 * que es como se piden en el laboratorio. Aquí se detectan dentro del texto
 * y se traducen a la otra unidad, para el cliente que las entiende mejor así.
 *
 * La unidad en que están escritas la elige el administrador (Panel → Ofertas
 * → Ajustes de todas las ofertas). Por defecto, pulgadas.
 *
 * No se toca el texto original: se devuelve una lista aparte con las
 * equivalencias, y la pantalla la enseña debajo.
 */
object Medidas {

    private const val CM_POR_PULGADA = 2.54

    // Un par de números separados por x. Los delimitadores de los extremos
    // evitan partir cifras más largas (una fecha, un precio con decimales).
    private val PATRON = Regex(
        """(?<![\d.,])(\d{1,3}(?:[.,]\d{1,2})?)\s*[xX×]\s*(\d{1,3}(?:[.,]\d{1,2})?)(?![\d.,])"""
    )

    // Fuera de este rango no es una medida de foto: descarta cosas como
    // "2x3 piezas" por abajo y cualquier número suelto grande por arriba.
    private const val MIN = 3.0
    private const val MAX_PULGADAS = 120.0
    private const val MAX_CM = 300.0

    private fun uno(v: Double): String = String.format(Locale.US, "%.1f", v)

    private fun aNumero(t: String): Double? = t.replace(',', '.').toDoubleOrNull()

    /** Las medidas del texto como (ancho escrito, alto escrito, ancho convertido, alto convertido). */
    private fun encontrar(texto: String, enPulgadas: Boolean): List<List<String>> {
        val max = if (enPulgadas) MAX_PULGADAS else MAX_CM
        val vistas = LinkedHashSet<String>()
        val salida = mutableListOf<List<String>>()
        for (m in PATRON.findAll(texto)) {
            val anchoTexto = m.groupValues[1]
            val altoTexto = m.groupValues[2]
            val ancho = aNumero(anchoTexto) ?: continue
            val alto = aNumero(altoTexto) ?: continue
            if (ancho < MIN || ancho > max || alto < MIN || alto > max) continue
            if (!vistas.add("${anchoTexto}x$altoTexto")) continue
            val (a, b) = if (enPulgadas) {
                ancho * CM_POR_PULGADA to alto * CM_POR_PULGADA
            } else {
                ancho / CM_POR_PULGADA to alto / CM_POR_PULGADA
            }
            salida.add(listOf(anchoTexto, altoTexto, uno(a), uno(b)))
        }
        return salida
    }

    /**
     * Una línea por medida, para la ficha del paquete:
     * `6x8 pulgadas  ≈  15.2 × 20.3 cm`. Lista vacía si no hay ninguna.
     */
    fun equivalencias(texto: String, enPulgadas: Boolean = true): List<String> =
        encontrar(texto, enPulgadas).map { (w, h, a, b) ->
            if (enPulgadas) "${w}x$h pulgadas  ≈  $a × $b cm"
            else "${w}x$h cm  ≈  $a\" × $b\""
        }

    /**
     * Versión corta en una sola línea, para las filas de los servicios:
     * `5x7" = 12.7×17.8 cm · 6x8" = 15.2×20.3 cm`. Null si no hay medidas.
     */
    fun resumen(texto: String, enPulgadas: Boolean = true): String? {
        val lista = encontrar(texto, enPulgadas).map { (w, h, a, b) ->
            if (enPulgadas) "${w}x$h\" = $a×$b cm"
            else "${w}x$h cm = $a×$b\""
        }
        return if (lista.isEmpty()) null else lista.joinToString("  ·  ")
    }
}
