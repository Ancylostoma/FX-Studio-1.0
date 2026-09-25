package com.example.data

/**
 * Actualización de precios del 12-9-26, sacada de los carteles del estudio.
 *
 * Los paquetes viven en la base de datos del teléfono desde la primera vez
 * que se abrió la app, así que cambiar la lista de fábrica no bastaría: los
 * que ya tienen la app instalada seguirían con los precios viejos. Por eso
 * se aplica una sola vez, al arrancar, sobre lo que ya hay guardado, y queda
 * apuntado que ya se hizo para no volver a pisar lo que el administrador
 * cambie después.
 */
object ActualizacionCatalogo {

    /** Marca que se guarda cuando ya se aplicó. */
    const val VERSION = "2026-09-12"

    /** Precio nuevo de cada paquete (vale para Álbum y para FotoBook). */
    val PRECIOS: Map<String, Double> = mapOf(
        // Primer Año
        "A1" to 138.73, "A2" to 159.67, "A3" to 180.00, "A4" to 186.67,
        "A5" to 166.83, "A6" to 187.77, "A7" to 208.10, "A8" to 214.77,
        "A9" to 170.40, "A10" to 191.33, "A11" to 211.67, "A12" to 218.33,
        "A13" to 209.50, "A14" to 260.43, "A15" to 280.77, "A16" to 287.43,
        // Bodas
        "B1" to 258.07, "B2" to 278.99, "B3" to 298.29, "B4" to 303.13,
        "B5" to 299.90, "B6" to 320.83, "B7" to 340.12, "B8" to 344.96,
        "B9" to 346.07, "B10" to 366.99, "B11" to 386.29, "B12" to 391.13
    )

    // Desde esta actualización todos los paquetes de bodas llevan traje de
    // novio y los souvenirs, no solo los tres últimos.
    const val BODAS_INCLUYE =
        "Maquillaje, Peinado, Vestido de novia, Traje de novio, 2 Tazas, 2 Llaveros, 2 Pullovers, Marco"
    private const val BODAS_FRASE =
        "Incluye maquillaje, peinado, vestido de novia y traje de novio, 2 tazas, 2 llaveros y 2 pullovers."

    /** Cambia la segunda línea de la descripción de un paquete de bodas. */
    fun descripcionBodas(actual: String): String {
        val primera = actual.lineSequence().firstOrNull().orEmpty()
        return if (primera.isBlank()) BODAS_FRASE else "$primera\n$BODAS_FRASE"
    }

    /** Pone el mismo precio a todas las variantes del paquete. */
    fun conPrecio(item: CatalogItem, precio: Double): CatalogItem {
        val variantes = item.getVariants().ifEmpty { listOf(CatalogVariant("Álbum", precio)) }
        return item.copy(
            variantsString = CatalogItem.createVariantsString(variantes.map { it.copy(price = precio) })
        )
    }

    private const val COLLAGE_INCLUYE =
        "Se incluyen maquillajes, 3 vestidos de 15, taza, llavero, pullover y transporte en exteriores de Bayamo."

    /** Los collages de ampliaciones, nuevos: para quien no quiere ni álbum ni fotos. */
    val COLLAGES: List<CatalogItem> = listOf(
        CatalogItem(
            code = "C1",
            name = "Collage C1 - 6 Fotos 8x12 enmarcadas",
            description = "Collage de 6 fotos 8x12 impresas y enmarcadas juntas, por si no " +
                "quieres ni álbum ni fotos. Ahorras 3.30 USD.\n$COLLAGE_INCLUYE",
            category = "15 años",
            variantsString = "Collage:191.60",
            includedExtras = "Impresión de 6 fotos 8x12, Marco, Maquillajes, 3 Vestidos de 15, Taza, Llavero, Pullover"
        ),
        CatalogItem(
            code = "C2",
            name = "Collage C2 - Ampliaciones 16x24 + 2 de 12x18 + 2 de 8x12",
            description = "Paquete de 5 ampliaciones: 1 de 16x24, 2 de 12x18 y 2 de 8x12. " +
                "Ahorras 3.90 USD.\n$COLLAGE_INCLUYE",
            category = "15 años",
            variantsString = "Collage:231.33",
            includedExtras = "5 Ampliaciones, Maquillajes, 3 Vestidos de 15, Taza, Llavero, Pullover"
        ),
        CatalogItem(
            code = "C3",
            name = "Collage C3 - Ampliaciones 24x32 + 2 de 16x24 + 2 de 12x18 + 2 de 8x12",
            description = "Paquete de 7 ampliaciones: 1 de 24x32, 2 de 16x24, 2 de 12x18 y 2 de 8x12. " +
                "Ahorras 3.90 USD.\n$COLLAGE_INCLUYE",
            category = "15 años",
            variantsString = "Collage:343.30",
            includedExtras = "7 Ampliaciones, Maquillajes, 3 Vestidos de 15, Taza, Llavero, Pullover"
        )
    )

    // --------------------------------------------------------------
    // Servicios sueltos. Solo hace falta para las listas que el
    // administrador ya hubiera guardado; la de fábrica ya viene al día.
    // --------------------------------------------------------------

    const val VIDEO_RESUMEN_DESC =
        "Incluye un vestido de 15 con aro o de novia y traje de novio, un maquillaje y 10 fotos digitales"
    const val VIDEO_CONTINUO_DESC =
        "Incluye un vestido de 15 con aro o de novia y traje de novio, un maquillaje y todas las fotos digitales"

    /** Ampliaciones impresas con marco: tamaño → precio nuevo. */
    val AMPLIACIONES_CON_MARCO: Map<String, Double> = mapOf(
        "16x24" to 55.60, "20x24" to 63.40, "24x32" to 78.70,
        "24x39" to 84.50, "39x58.5" to 150.00
    )

    private fun List<Carpeta>.cambiar(cambio: (Servicio) -> Servicio): List<Carpeta> =
        map { c ->
            c.copy(subcarpetas = c.subcarpetas.map { s ->
                s.copy(servicios = s.servicios.map(cambio))
            })
        }

    fun aplicarAServicios(menu: MenuServicios, carpetas: List<Carpeta>): List<Carpeta> =
        carpetas.cambiar { v ->
            when (menu) {
                MenuServicios.PROPIA -> {
                    val tam = AMPLIACIONES_CON_MARCO.keys.firstOrNull {
                        v.titulo == "Ampliación $it (Impresa + marco)"
                    }
                    val mitad = v.formato.startsWith("Solo filmación")
                    when {
                        tam != null -> v.copy(precio = AMPLIACIONES_CON_MARCO.getValue(tam))
                        v.titulo == "Videografía - Makin Off (hasta 10 min, 4K 60fps)" -> v.copy(
                            titulo = "Videografía - Video Resumen (hasta 10 min, 4K 60fps)",
                            precio = if (mitad) 32.50 else 65.00,
                            descripcion = if (mitad) v.descripcion else VIDEO_RESUMEN_DESC
                        )
                        v.titulo == "Videografía - Video continuo de 1 hora (4K 60fps)" -> v.copy(
                            precio = if (mitad) 72.50 else 145.00,
                            descripcion = if (mitad) v.descripcion else VIDEO_CONTINUO_DESC
                        )
                        else -> v
                    }
                }
                MenuServicios.EXTRAS -> {
                    val tam = AMPLIACIONES_CON_MARCO.keys.firstOrNull {
                        v.titulo == "Ampliación $it con marco"
                    }
                    when {
                        tam != null -> v.copy(precio = AMPLIACIONES_CON_MARCO.getValue(tam))
                        v.titulo == "Videografía Makin Off (hasta 10 min 4K)" -> v.copy(
                            titulo = "Video Resumen (editado hasta 10 min, 4K 60fps)",
                            formato = "Video resumen",
                            descripcion = VIDEO_RESUMEN_DESC,
                            precio = 65.00
                        )
                        v.titulo == "Video Continuo 1 hora 4K" -> v.copy(
                            titulo = "Video Continuo de 1 hora (editado 4K 60fps)",
                            formato = "Video continuo 1h",
                            descripcion = VIDEO_CONTINUO_DESC,
                            precio = 145.00
                        )
                        else -> v
                    }
                }
            }
        }
}
