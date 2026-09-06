package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * "Diseña tu propia oferta".
 *
 * Antes eran ocho tarjetas abiertas a la vez, cada una con sus chips, su
 * selector de cantidad y su botón de añadir: había que pasar por delante de
 * todo el catálogo aunque solo se quisiera una foto suelta.
 *
 * Ahora es el mismo árbol de carpetas de "Agregar algo más": una abierta a la
 * vez, el nombre de la carpeta siempre a la vista, un toque añade y el total
 * sube arriba. Las dos pantallas comparten los componentes de
 * ArbolDeOpciones.kt, así que se comportan igual.
 */

// ------------------------------------------------------------------
// El catálogo suelto, en el orden en que se arma una sesión
// ------------------------------------------------------------------

private fun opcion(
    titulo: String,
    descripcion: String,
    categoria: String,
    variante: String,
    precio: Double
) = ExtraOption(titulo, descripcion, categoria, variante, precio)

/** Fotos al momento: seis temas, cada uno con sus tres formatos. */
private fun fotosAlMomento(): Rama {
    val temas = listOf(
        "Meses" to listOf(4.02, 4.50, 5.27),
        "Primer Año (1-4 años)" to listOf(4.30, 4.70, 5.60),
        "PreQuinces (5-14 años)" to listOf(4.60, 5.00, 5.80),
        "Embarazadas" to listOf(5.10, 5.40, 6.00),
        "Bodas" to listOf(5.40, 5.80, 6.30),
        "15 años en adelante" to listOf(5.70, 6.16, 6.70)
    )
    val formatos = listOf("Digital editada", "5x7 / 6x8", "8x10 / 8x12")
    return Rama(
        "Fotos al momento", "📸",
        temas.map { (tema, precios) ->
            Subrama(
                tema,
                formatos.mapIndexed { i, formato ->
                    opcion(
                        "Foto al Momento - $tema",
                        "Te la llevas impresa el mismo día",
                        "Fotos al Momento",
                        formato,
                        precios[i]
                    )
                }
            )
        }
    )
}

/** Álbumes: cinco formatos, sin más niveles. */
private fun albumes(): Rama {
    val formatos = listOf("6x8" to 6.00, "8x10" to 10.00, "8x12" to 10.00,
        "10x15" to 13.00, "12x18" to 16.00)
    return Rama(
        "Álbumes", "📔",
        listOf(
            Subrama(
                null,
                formatos.map { (f, p) ->
                    opcion(
                        "Álbum Personalizado $f",
                        "Papel foto personalizado. El precio no incluye las fotos.",
                        "Álbumes", f, p
                    )
                }
            )
        )
    )
}

/** FotoBook: tres formatos, cada uno de 10 o de 20 fotos. */
private fun fotoBooks(): Rama {
    val formatos = listOf(
        "6x8" to listOf("10 fotos" to 6.00, "20 fotos" to 9.00),
        "8x10" to listOf("10 fotos" to 8.50, "20 fotos" to 15.40),
        "8x12" to listOf("10 fotos" to 9.00, "20 fotos" to 16.40)
    )
    return Rama(
        "FotoBooks", "📖",
        formatos.map { (formato, opciones) ->
            Subrama(
                "Formato $formato",
                opciones.map { (capacidad, precio) ->
                    opcion(
                        "FotoBook $formato ($capacidad)",
                        "Encuadernado tipo revista. El precio no incluye las fotos.",
                        "FotoBook", "$formato - $capacidad", precio
                    )
                }
            )
        }
    )
}

/** Ampliaciones: primero el acabado, después el tamaño. */
private fun ampliaciones(): Rama {
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
    fun grupo(nombre: String, etiqueta: String, conMarco: Boolean) = Subrama(
        nombre,
        tamanos.map { (tam, precios) ->
            val precio = if (conMarco) precios.second else precios.first
            opcion(
                "Ampliación $tam ($etiqueta)",
                if (conMarco) "Impresa en alta definición y montada en marco"
                else "Archivo digital editado en alta definición",
                "Ampliaciones", "$tam - $etiqueta", precio
            )
        }
    )
    return Rama(
        "Ampliaciones", "🖼️",
        listOf(
            grupo("Impresas con marco", "Impresa + marco", true),
            grupo("Solo el archivo digital", "Digital editada", false)
        )
    )
}

/** Maquillaje y peinado. */
private fun maquillaje(): Rama {
    val opciones = listOf(
        "Mamá (fotos primer año)" to 5.00,
        "Embarazadas" to 5.00,
        "Prequinces" to 2.00,
        "Bodas (incluye pestañas)" to 10.00,
        "Quinces (incluye pestañas)" to 15.00,
        "Acompañantes" to 5.00
    )
    return Rama(
        "Maquillaje y peinado", "💄",
        listOf(
            Subrama(
                null,
                opciones.map { (nombre, precio) ->
                    opcion(
                        "Maquillaje - $nombre",
                        "Estilismo profesional Jezabelleza",
                        "Maquillaje", nombre, precio
                    )
                }
            )
        )
    )
}

/** Alquiler de vestuario. */
private fun vestuario(): Rama {
    val opciones = listOf(
        "Niños (batas, disfraces, trajecitos)" to 2.00,
        "Embarazadas" to 5.00,
        "Trajes para hombre" to 5.00,
        "Vestido sencillo" to 2.00,
        "Vestidos de 15 con aro" to 5.00
    )
    return Rama(
        "Vestuario", "👗",
        listOf(
            Subrama(
                null,
                opciones.map { (nombre, precio) ->
                    opcion(
                        "Alquiler - $nombre",
                        "Alquiler para la sesión",
                        "Vestuario", nombre, precio
                    )
                }
            )
        )
    )
}

/** Impresión suelta de fotos que ya trae el cliente. */
private fun impresion(): Rama {
    val formatos = listOf("4x6" to 1.50, "5x7 / 6x8" to 1.70, "8x10 / 8x12" to 2.20)
    return Rama(
        "Impresión de fotos", "🖨️",
        listOf(
            Subrama(
                null,
                formatos.map { (f, p) ->
                    opcion(
                        "Impresión Fotográfica $f",
                        "Papel profesional de alta resolución",
                        "Impresión", f, p
                    )
                }
            )
        )
    )
}

/** Videografía: editado, o solo la filmación a mitad de precio. */
private fun videografia(): Rama {
    val servicios = listOf(
        "Makin Off (hasta 10 min, 4K 60fps)" to 40.00,
        "Video continuo de 1 hora (4K 60fps)" to 120.00
    )
    fun grupo(nombre: String, etiqueta: String, mitad: Boolean) = Subrama(
        nombre,
        servicios.map { (servicio, precio) ->
            opcion(
                "Videografía - $servicio",
                if (mitad) "Se entrega el material en bruto, sin editar"
                else "Edición completa en 4K 60fps",
                "Videografía", etiqueta, if (mitad) precio / 2.0 else precio
            )
        }
    )
    return Rama(
        "Video", "🎬",
        listOf(
            grupo("Editado", "Editado 4K 60fps", false),
            grupo("Solo filmación (50% menos)", "Solo filmación sin editar (50% desc.)", true)
        )
    )
}

/**
 * Las carpetas, en el orden en que se arma una sesión: primero las fotos,
 * después lo que se imprime con ellas, luego lo que hace falta el día de la
 * sesión, y al final el vídeo.
 */
private fun ramasDelDisenador(): List<Rama> = listOf(
    fotosAlMomento(),
    albumes(),
    fotoBooks(),
    ampliaciones(),
    impresion(),
    maquillaje(),
    vestuario(),
    videografia()
)

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun CustomOfferScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val carrito by viewModel.cart.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val ramas = remember { ramasDelDisenador() }

    // Solo una carpeta abierta a la vez. Se arranca con todas cerradas: la
    // primera pantalla que ve el cliente es la lista corta de ocho nombres,
    // no ochenta precios.
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }

    val cantidadEnPedido: (ExtraOption) -> Int = { extra ->
        carrito.firstOrNull {
            it.item.name == extra.title &&
                it.variant.name == extra.variantName &&
                it.item.category == extra.category
        }?.quantity ?: 0
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ---- Cabecera fija con el total ----
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Diseña tu propia oferta",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Abre una sección, toca lo que quieras y se va sumando.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "TU PEDIDO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    ImporteAnimado(
                        valor = total,
                        color = MaterialTheme.colorScheme.onPrimary,
                        estilo = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                viewModel.cupLabel(total)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // ---- Las carpetas ----
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ramas, key = { it.nombre }) { rama ->
                CarpetaRama(
                    rama = rama,
                    abierta = abierta == rama.nombre,
                    cantidadEnPedido = cantidadEnPedido,
                    onAbrir = {
                        abierta = if (abierta == rama.nombre) null else rama.nombre
                    },
                    onAgregar = { extra ->
                        viewModel.addCustomToCart(
                            extra.title,
                            extra.category,
                            extra.variantName,
                            extra.price,
                            1,
                            extra.description
                        )
                    },
                    onQuitar = { extra ->
                        viewModel.quitarUnoDelCarrito(
                            extra.title,
                            extra.category,
                            extra.variantName
                        )
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "FXestudio se reserva el derecho de cambiar estos precios " +
                            "sin previo aviso. Todos los precios están en USD; se acepta " +
                            "Zelle y CUP al cambio del día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
