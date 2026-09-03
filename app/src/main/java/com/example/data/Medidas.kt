package com.example.data

import java.util.Locale

/**
 * Las medidas del catálogo están en centímetros ("6x8", "12x18", "24x39"),
 * que es como se piden en el laboratorio. Aquí se detectan dentro del texto
 * del paquete y se traducen a pulgadas, para el cliente que las pide así.
 *
 * No se toca el texto original: se devuelve una lista aparte con las
 * equivalencias, y la pantalla la enseña debajo de la descripción.
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
    private const val MIN_CM = 3.0
    private const val MAX_CM = 300.0

    private fun aPulgadas(cm: Double): String =
        String.format(Locale.US, "%.1f", cm / CM_POR_PULGADA)

    private fun aNumero(t: String): Double? = t.replace(',', '.').toDoubleOrNull()

    /**
     * Equivalencias encontradas en el texto, sin repetir y en el orden en que
     * aparecen. Lista vacía si no hay ninguna medida reconocible.
     */
    fun enPulgadas(texto: String): List<String> {
        val salida = LinkedHashSet<String>()
        for (m in PATRON.findAll(texto)) {
            val anchoTexto = m.groupValues[1]
            val altoTexto = m.groupValues[2]
            val ancho = aNumero(anchoTexto) ?: continue
            val alto = aNumero(altoTexto) ?: continue
            if (ancho < MIN_CM || ancho > MAX_CM) continue
            if (alto < MIN_CM || alto > MAX_CM) continue
            salida.add(
                "${anchoTexto}x$altoTexto cm  ≈  ${aPulgadas(ancho)}\" × ${aPulgadas(alto)}\""
            )
        }
        return salida.toList()
    }
}
