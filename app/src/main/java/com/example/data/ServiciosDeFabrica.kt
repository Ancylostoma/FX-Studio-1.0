package com.example.data

/**
 * Las dos listas de servicios con las que viene la app, tal como estaban
 * escritas antes en las pantallas. Solo se usan mientras el administrador no
 * haya guardado las suyas desde el panel.
 *
 * Los servicios que se crean aquí ocultos y a $0 son tamaños que el estudio
 * vende pero cuyo precio todavía no está en la app: aparecen en el panel
 * marcados como "falta el precio" y el cliente no los ve hasta que el
 * administrador les pone precio y los enciende.
 */
object ServiciosDeFabrica {

    private fun carpeta(nombre: String, simbolo: String, vararg subs: Subcarpeta) =
        Carpeta(nombre = nombre, simbolo = simbolo, subcarpetas = subs.toList())

    private fun sub(nombre: String, servicios: List<Servicio>) =
        Subcarpeta(nombre = nombre, servicios = servicios)

    private fun s(
        titulo: String,
        formato: String,
        descripcion: String,
        precio: Double,
        grupo: String,
        paquetes: List<String> = emptyList(),
        visible: Boolean = true
    ) = Servicio(
        titulo = titulo,
        formato = formato,
        descripcion = descripcion,
        precio = precio,
        grupoPedido = grupo,
        paquetes = paquetes,
        visible = visible
    )

    /** Un tamaño que existe pero aún no tiene precio en la app. */
    private fun sinPrecio(titulo: String, formato: String, descripcion: String, grupo: String) =
        s(titulo, formato, descripcion, 0.0, grupo, visible = false)

    // ==============================================================
    // Diseña tu propia oferta
    // ==============================================================

    fun propia(): List<Carpeta> = listOf(
        fotosAlMomento(),
        albumes(),
        fotoBooks(),
        ampliaciones(),
        impresion(),
        maquillaje(),
        vestuario(),
        video()
    )

    private fun fotosAlMomento(): Carpeta {
        val temas = listOf(
            "Meses" to listOf(4.02, 4.50, 5.27),
            "Primer Año (1-4 años)" to listOf(4.30, 4.70, 5.60),
            "PreQuinces (5-14 años)" to listOf(4.60, 5.00, 5.80),
            "Embarazadas" to listOf(5.10, 5.40, 6.00),
            "Bodas" to listOf(5.40, 5.80, 6.30),
            "15 años en adelante" to listOf(5.70, 6.16, 6.70)
        )
        val formatos = listOf("Digital editada", "5x7 / 6x8", "8x10 / 8x12")
        return Carpeta(
            nombre = "Fotos al momento",
            simbolo = "📸",
            subcarpetas = temas.map { (tema, precios) ->
                sub(tema, formatos.mapIndexed { i, formato ->
                    s(
                        "Foto al Momento - $tema", formato,
                        "Te la llevas impresa el mismo día",
                        precios[i], "Fotos al Momento"
                    )
                })
            }
        )
    }

    private const val DESC_ALBUM = "Papel foto personalizado. El precio no incluye las fotos."

    /** Álbumes: 5x7, 6x8, 8x10 y 8x12. */
    private fun albumes(): Carpeta = carpeta(
        "Álbumes", "📔",
        sub("", listOf(
            sinPrecio("Álbum Personalizado 5x7", "5x7", DESC_ALBUM, "Álbumes"),
            s("Álbum Personalizado 6x8", "6x8", DESC_ALBUM, 6.00, "Álbumes"),
            s("Álbum Personalizado 8x10", "8x10", DESC_ALBUM, 10.00, "Álbumes"),
            s("Álbum Personalizado 8x12", "8x12", DESC_ALBUM, 10.00, "Álbumes")
        ))
    )

    private const val DESC_FOTOBOOK = "Encuadernado tipo revista. El precio no incluye las fotos."

    /** FotoBook: cuatro formatos, cada uno de 10, 15 o 20 fotos. */
    private fun fotoBooks(): Carpeta {
        // null = ese tamaño existe pero aún no tiene precio en la app.
        val precios: List<Pair<String, List<Double?>>> = listOf(
            "5x7" to listOf(null, null, null),
            "6x8" to listOf(6.00, null, 9.00),
            "8x10" to listOf(8.50, null, 15.40),
            "8x12" to listOf(9.00, null, 16.40)
        )
        val capacidades = listOf("10 fotos", "15 fotos", "20 fotos")
        return Carpeta(
            nombre = "FotoBooks",
            simbolo = "📖",
            subcarpetas = precios.map { (formato, lista) ->
                sub("Formato $formato", capacidades.mapIndexed { i, capacidad ->
                    val titulo = "FotoBook $formato ($capacidad)"
                    val variante = "$formato - $capacidad"
                    val precio = lista[i]
                    if (precio == null) sinPrecio(titulo, variante, DESC_FOTOBOOK, "FotoBook")
                    else s(titulo, variante, DESC_FOTOBOOK, precio, "FotoBook")
                })
            }
        )
    }

    /** Ampliaciones: primero el acabado, después el tamaño. */
    private fun ampliaciones(): Carpeta {
        // tamaño to (digital, impresa con marco)
        val tamanos = listOf(
            "12x16" to (27.50 to 34.50),
            "12x18" to (29.50 to 37.50),
            "16x20" to (36.50 to 47.50),
            "16x24" to (38.50 to 51.50),
            "20x24" to (42.50 to 57.50),
            "24x32" to (49.50 to 67.50),
            "24x39" to (54.50 to 74.50),
            "39x58.5" to (73.50 to 120.00),
            "39x82.67" to (100.50 to 175.00)
        )
        fun grupo(nombre: String, etiqueta: String, conMarco: Boolean) = sub(
            nombre,
            tamanos.map { (tam, precios) ->
                s(
                    "Ampliación $tam ($etiqueta)",
                    "$tam - $etiqueta",
                    if (conMarco) "Impresa en alta definición y montada en marco"
                    else "Archivo digital editado en alta definición",
                    if (conMarco) precios.second else precios.first,
                    "Ampliaciones"
                )
            }
        )
        return carpeta(
            "Ampliaciones", "🖼️",
            grupo("Impresas con marco", "Impresa + marco", true),
            grupo("Solo el archivo digital", "Digital editada", false)
        )
    }

    private fun impresion(): Carpeta = carpeta(
        "Impresión de fotos", "🖨️",
        sub("", listOf("4x6" to 1.50, "5x7 / 6x8" to 1.70, "8x10 / 8x12" to 2.20).map { (f, p) ->
            s("Impresión Fotográfica $f", f, "Papel profesional de alta resolución", p, "Impresión")
        })
    )

    private fun maquillaje(): Carpeta = carpeta(
        "Maquillaje y peinado", "💄",
        sub("", listOf(
            "Mamá (fotos primer año)" to 5.00,
            "Embarazadas" to 5.00,
            "Prequinces" to 2.00,
            "Bodas (incluye pestañas)" to 10.00,
            "Quinces (incluye pestañas)" to 15.00,
            "Acompañantes" to 5.00
        ).map { (nombre, precio) ->
            s("Maquillaje - $nombre", nombre, "Estilismo profesional Jezabelleza", precio, "Maquillaje")
        })
    )

    private fun vestuario(): Carpeta = carpeta(
        "Vestuario", "👗",
        sub("", listOf(
            "Niños (batas, disfraces, trajecitos)" to 2.00,
            "Embarazadas" to 5.00,
            "Trajes para hombre" to 5.00,
            "Vestido sencillo" to 2.00,
            "Vestidos de 15 con aro" to 5.00
        ).map { (nombre, precio) ->
            s("Alquiler - $nombre", nombre, "Alquiler para la sesión", precio, "Vestuario")
        })
    )

    /** Videografía: editado, o solo la filmación a mitad de precio. */
    private fun video(): Carpeta {
        val servicios = listOf(
            "Makin Off (hasta 10 min, 4K 60fps)" to 40.00,
            "Video continuo de 1 hora (4K 60fps)" to 120.00
        )
        fun grupo(nombre: String, etiqueta: String, mitad: Boolean) = sub(
            nombre,
            servicios.map { (servicio, precio) ->
                s(
                    "Videografía - $servicio", etiqueta,
                    if (mitad) "Se entrega el material en bruto, sin editar"
                    else "Edición completa en 4K 60fps",
                    if (mitad) precio / 2.0 else precio,
                    "Videografía"
                )
            }
        )
        return carpeta(
            "Video", "🎬",
            grupo("Editado", "Editado 4K 60fps", false),
            grupo("Solo filmación (50% menos)", "Solo filmación sin editar (50% desc.)", true)
        )
    }

    // ==============================================================
    // Agregar algo más (dentro de un paquete)
    // ==============================================================

    private val PA = listOf("Primer Año")
    private val BO = listOf("Bodas")
    private val QU = listOf("15 años")

    fun extras(): List<Carpeta> = listOf(
        carpeta(
            "Fotos extra", "📸",
            sub("Digitales", listOf(
                s("Foto Extra Primer Año (Digital)", "Digital editada", "Foto digital editada adicional", 4.30, "Primer Año", PA),
                s("Foto Extra Bodas (Digital)", "Digital editada", "Foto digital editada adicional", 5.40, "Bodas", BO),
                s("Foto Extra 15 años (Digital)", "Digital editada", "Foto digital editada adicional", 5.70, "15 años", QU)
            )),
            sub("Impresas", listOf(
                s("Foto Extra Primer Año (5x7 / 6x8)", "5x7 / 6x8", "Foto impresa 5x7 o 6x8", 4.70, "Primer Año", PA),
                s("Foto Extra Primer Año (8x10 / 8x12)", "8x10 / 8x12", "Foto impresa 8x10 o 8x12", 5.60, "Primer Año", PA),
                s("Foto Extra Bodas (5x7 / 6x8)", "5x7 / 6x8", "Foto impresa 5x7 o 6x8", 5.80, "Bodas", BO),
                s("Foto Extra Bodas (8x10 / 8x12)", "8x10 / 8x12", "Foto impresa 8x10 o 8x12", 6.30, "Bodas", BO),
                s("Foto Extra 15 años (5x7 / 6x8)", "5x7 / 6x8", "Foto impresa 5x7 o 6x8", 6.16, "15 años", QU),
                s("Foto Extra 15 años (8x10 / 8x12)", "8x10 / 8x12", "Foto impresa 8x10 o 8x12", 6.70, "15 años", QU)
            ))
        ),
        carpeta(
            "Álbumes y FotoBooks", "📖",
            sub("", listOf(
                s("Álbum 8x12 personalizado", "8x12", "Álbum impreso en papel foto", 10.00, "Álbum"),
                s("FotoBook 8x12 (20 fotos)", "8x12 (20 fotos)", "FotoBook personalizado para 20 fotos", 16.40, "FotoBook")
            ))
        ),
        carpeta(
            "Ampliaciones con marco", "🖼️",
            sub("", listOf(
                s("Ampliación 16x24 con marco", "16x24 con marco", "Ampliación impresa montada en marco", 51.50, "Ampliaciones"),
                s("Ampliación 24x32 con marco", "24x32 con marco", "Ampliación impresa montada en marco", 67.50, "Ampliaciones"),
                s("Ampliación 24x39 con marco", "24x39 con marco", "Ampliación impresa montada en marco", 74.50, "Ampliaciones"),
                s("Super Ampliación 39x82.67 con marco", "39x82.67 con marco", "Super formato impreso con marco", 175.00, "Ampliaciones")
            ))
        ),
        carpeta(
            "Vestuario", "👗",
            sub("", listOf(
                s("Cambio de ropa adicional", "Niños (alquiler)", "Batas, disfraces o trajecitos para niños", 2.00, "Vestuario", PA),
                s("Alquiler Vestido de Novia Adicional", "Vestido novia", "Cambio de vestido de novia", 10.00, "Vestuario", BO),
                s("Alquiler Traje para Hombre", "Trajes hombre", "Traje formal de novio o caballero", 5.00, "Vestuario", BO),
                s("Vestido de 15 con Aro Adicional", "Vestido de 15 con aro", "Alquiler de vestido de gala con aro", 5.00, "Vestuario", QU),
                s("Vestido Sencillo Adicional", "Vestido sencillo", "Alquiler vestido casual", 2.00, "Vestuario", QU)
            ))
        ),
        carpeta(
            "Maquillaje y peinado", "💄",
            sub("", listOf(
                s("Maquillaje Mamá Extra", "Mamá", "Maquillaje para sesión primer año", 5.00, "Maquillaje", PA),
                s("Maquillaje Acompañante", "Acompañante", "Maquillaje y peinado adicional", 5.00, "Maquillaje", PA),
                s("Maquillaje Bodas Extra (con pestañas)", "Bodas", "Maquillaje profesional con pestañas", 10.00, "Maquillaje", BO),
                s("Maquillaje Acompañantes / Damas", "Acompañantes", "Maquillaje y peinado para damas", 5.00, "Maquillaje", BO),
                s("Maquillaje Quinces Extra (con pestañas)", "Quinces", "Maquillaje y peinado Jezabelleza", 15.00, "Maquillaje", QU)
            ))
        ),
        carpeta(
            "Souvenirs", "🎁",
            sub("", listOf(
                s("Taza Personalizada Extra", "Taza sublimada", "Souvenir fotográfico", 6.00, "Souvenirs", PA),
                s("Pullover Personalizado Extra", "Pullover", "Pullover con foto impresa", 8.00, "Souvenirs", PA),
                s("Llavero Personalizado Extra", "Llavero", "Llavero acrílico con foto", 2.50, "Souvenirs", PA)
            ))
        ),
        carpeta(
            "Video", "🎬",
            sub("", listOf(
                s("Videografía Makin Off (hasta 10 min 4K)", "Makin Off editado", "Edición completa en 4K 60fps", 40.00, "Videografía", BO + QU),
                s("Video Continuo 1 hora 4K", "Video continuo 1h", "Cobertura continua editada", 120.00, "Videografía", BO + QU)
            ))
        ),
        carpeta(
            "Revistas", "📰",
            sub("", listOf(
                s("Revista 20 páginas Extra", "Revista 20 pág", "Diseño e impresión de revista", 140.00, "Impresión", QU)
            ))
        )
    )

    fun para(menu: MenuServicios): List<Carpeta> = when (menu) {
        MenuServicios.PROPIA -> propia()
        MenuServicios.EXTRAS -> extras()
    }
}
