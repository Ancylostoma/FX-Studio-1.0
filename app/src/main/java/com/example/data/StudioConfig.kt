package com.example.data

/**
 * Tasa de una forma de pago respecto al USD, que es la moneda de referencia
 * del catálogo. `tasa` son las unidades de esa moneda por 1 USD.
 */
data class TasaPago(
    val id: String,
    val nombre: String,
    val tasa: Double,
    val visible: Boolean
) {
    /** Importe equivalente, ya formateado. */
    fun formatear(usd: Double): String {
        val v = usd * tasa
        return if (tasa >= 50) {
            // Monedas de valor bajo (CUP): sin decimales y con separador de miles.
            val entero = Math.round(v).toString()
                .reversed().chunked(3).joinToString(" ").reversed()
            "$entero $nombre"
        } else {
            "${String.format("%.2f", v)} $nombre"
        }
    }
}

/**
 * Todo lo que el administrador puede cambiar sin recompilar la app: los textos
 * de la portada, la ficha de contacto y las tasas de cambio.
 *
 * Los valores por defecto salen de [StudioInfo]; en la base de datos solo se
 * guarda lo que el administrador haya modificado.
 */
data class StudioConfig(
    // Tercio superior
    val titulo: String = StudioInfo.NOMBRE,
    val lema: String = StudioInfo.LEMA,
    // Tercio central
    val frasePortada: String = "Inmortalizamos la felicidad\ncreando recuerdos del alma.",
    // Tercio inferior: nombres de los cinco accesos
    val btnBodas: String = "Ofertas\nde Bodas",
    val btnQuince: String = "Ofertas\nde Quince",
    val btnPrimerAno: String = "Ofertas\nde 1er Año",
    val btnOfertaPropia: String = "Diseña tu\npropia oferta",
    val btnCalendario: String = "Calendario\ny reservas",
    // Ficha de contacto
    val ubicacion: String = StudioInfo.UBICACION,
    val direccion: String = StudioInfo.DIRECCION,
    val telefonos: String = "${StudioInfo.TELEFONO_1} / ${StudioInfo.TELEFONO_2}",
    val horarioSemana: String = StudioInfo.HORARIO_SEMANA,
    val horarioSabado: String = StudioInfo.HORARIO_SABADO,
    val horarioDomingo: String = StudioInfo.HORARIO_DOMINGO,
    val catalogoUrl: String = StudioInfo.CATALOGO_URL,
    val facebookUrl: String = StudioInfo.FACEBOOK_URL,
    // Formas de pago
    val tasas: List<TasaPago> = TASAS_POR_DEFECTO
) {
    /** Solo las formas de pago que el administrador dejó visibles y con tasa. */
    val tasasVisibles: List<TasaPago>
        get() = tasas.filter { it.visible && it.tasa > 0.0 }

    /** Equivalencias de un importe, para mostrarlas bajo el precio en USD. */
    fun equivalencias(usd: Double): List<String> =
        tasasVisibles.map { "≈ ${it.formatear(usd)}" }

    /** Una sola línea con todas las equivalencias, para WhatsApp. */
    fun equivalenciasLinea(usd: Double): String? {
        val e = equivalencias(usd)
        return if (e.isEmpty()) null else e.joinToString("  •  ")
    }

    companion object {
        const val ID_CUP = "cup"
        const val ID_ZELLE = "zelle"
        const val ID_TRANSFER = "transferencia"

        // En 0 no se muestran, para no enseñar una conversión inventada antes
        // de que el estudio fije las tasas reales.
        val TASAS_POR_DEFECTO = listOf(
            TasaPago(ID_CUP, "CUP", 0.0, true),
            TasaPago(ID_ZELLE, "USD Zelle", 0.0, false),
            TasaPago(ID_TRANSFER, "CUP transferencia", 0.0, false)
        )

        val NOMBRES_TASA = mapOf(
            ID_CUP to "CUP (efectivo)",
            ID_ZELLE to "Zelle",
            ID_TRANSFER to "Transferencia CUP"
        )
    }
}
