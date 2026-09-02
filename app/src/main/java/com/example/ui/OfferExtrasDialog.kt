package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CatalogItem

/** Una cosa que se puede añadir al paquete. */
data class ExtraOption(
    val title: String,
    val description: String,
    val category: String,
    val variantName: String,
    val price: Double
)

// ------------------------------------------------------------------
// El árbol: ramas, subramas y hojas
// ------------------------------------------------------------------

/** Un grupo dentro de una rama. Sin nombre = las opciones cuelgan directas. */
private data class Subrama(
    val nombre: String?,
    val opciones: List<ExtraOption>
)

/** Una carpeta principal del catálogo. */
private data class Rama(
    val nombre: String,
    val simbolo: String,
    val subramas: List<Subrama>
) {
    val opciones: List<ExtraOption> get() = subramas.flatMap { it.opciones }
}

private fun hoja(nombre: String?, vararg opciones: ExtraOption) =
    Subrama(nombre, opciones.toList())

/**
 * Las ramas, en el mismo orden en que las secciones aparecen en el catálogo
 * impreso: primero las fotos, después lo que se imprime con ellas, luego lo
 * que hace falta el día de la sesión, y al final los recuerdos y el vídeo.
 *
 * Para cambiar el orden basta mover los bloques de esta lista.
 */
private fun ramasPara(categoria: String): List<Rama> {
    // --- Fotos sueltas: cambian de precio según la categoría del paquete ---
    val fotosDigitales: List<ExtraOption>
    val fotosImpresas: List<ExtraOption>
    when (categoria) {
        "Primer Año" -> {
            fotosDigitales = listOf(
                ExtraOption("Foto Extra Primer Año (Digital)", "Foto digital editada adicional", "Primer Año", "Digital editada", 4.30)
            )
            fotosImpresas = listOf(
                ExtraOption("Foto Extra Primer Año (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "Primer Año", "5x7 / 6x8", 4.70),
                ExtraOption("Foto Extra Primer Año (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "Primer Año", "8x10 / 8x12", 5.60)
            )
        }
        "Bodas" -> {
            fotosDigitales = listOf(
                ExtraOption("Foto Extra Bodas (Digital)", "Foto digital editada adicional", "Bodas", "Digital editada", 5.40)
            )
            fotosImpresas = listOf(
                ExtraOption("Foto Extra Bodas (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "Bodas", "5x7 / 6x8", 5.80),
                ExtraOption("Foto Extra Bodas (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "Bodas", "8x10 / 8x12", 6.30)
            )
        }
        "15 años" -> {
            fotosDigitales = listOf(
                ExtraOption("Foto Extra 15 años (Digital)", "Foto digital editada adicional", "15 años", "Digital editada", 5.70)
            )
            fotosImpresas = listOf(
                ExtraOption("Foto Extra 15 años (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "15 años", "5x7 / 6x8", 6.16),
                ExtraOption("Foto Extra 15 años (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "15 años", "8x10 / 8x12", 6.70)
            )
        }
        else -> {
            fotosDigitales = listOf(
                ExtraOption("Foto Digital Editada", "Foto digital adicional en alta resolución", categoria, "Digital editada", 5.00)
            )
            fotosImpresas = listOf(
                ExtraOption("Foto Impresa 5x7 / 6x8", "Impresión fotográfica", "Impresiones", "5x7 / 6x8", 4.70),
                ExtraOption("Foto Impresa 8x10 / 8x12", "Impresión fotográfica", "Impresiones", "8x10 / 8x12", 5.80)
            )
        }
    }

    // --- Lo que cambia según el tipo de sesión ---
    val vestuario = when (categoria) {
        "Primer Año" -> listOf(
            ExtraOption("Cambio de ropa adicional", "Batas, disfraces o trajecitos para niños", "Vestuario", "Niños (alquiler)", 2.00)
        )
        "Bodas" -> listOf(
            ExtraOption("Alquiler Vestido de Novia Adicional", "Cambio de vestido de novia", "Vestuario", "Vestido novia", 10.00),
            ExtraOption("Alquiler Traje para Hombre", "Traje formal de novio o caballero", "Vestuario", "Trajes hombre", 5.00)
        )
        "15 años" -> listOf(
            ExtraOption("Vestido de 15 con Aro Adicional", "Alquiler de vestido de gala con aro", "Vestuario", "Vestido de 15 con aro", 5.00),
            ExtraOption("Vestido Sencillo Adicional", "Alquiler vestido casual", "Vestuario", "Vestido sencillo", 2.00)
        )
        else -> emptyList()
    }

    val maquillaje = when (categoria) {
        "Primer Año" -> listOf(
            ExtraOption("Maquillaje Mamá Extra", "Maquillaje para sesión primer año", "Maquillaje", "Mamá", 5.00),
            ExtraOption("Maquillaje Acompañante", "Maquillaje y peinado adicional", "Maquillaje", "Acompañante", 5.00)
        )
        "Bodas" -> listOf(
            ExtraOption("Maquillaje Bodas Extra (con pestañas)", "Maquillaje profesional con pestañas", "Maquillaje", "Bodas", 10.00),
            ExtraOption("Maquillaje Acompañantes / Damas", "Maquillaje y peinado para damas", "Maquillaje", "Acompañantes", 5.00)
        )
        "15 años" -> listOf(
            ExtraOption("Maquillaje Quinces Extra (con pestañas)", "Maquillaje y peinado Jezabelleza", "Maquillaje", "Quinces", 15.00)
        )
        else -> emptyList()
    }

    val souvenirs = if (categoria == "Primer Año") listOf(
        ExtraOption("Taza Personalizada Extra", "Souvenir fotográfico", "Souvenirs", "Taza sublimada", 6.00),
        ExtraOption("Pullover Personalizado Extra", "Pullover con foto impresa", "Souvenirs", "Pullover", 8.00),
        ExtraOption("Llavero Personalizado Extra", "Llavero acrílico con foto", "Souvenirs", "Llavero", 2.50)
    ) else emptyList()

    val video = if (categoria == "Bodas" || categoria == "15 años") listOf(
        ExtraOption("Videografía Makin Off (hasta 10 min 4K)", "Edición completa en 4K 60fps", "Videografía", "Makin Off editado", 40.00),
        ExtraOption("Video Continuo 1 hora 4K", "Cobertura continua editada", "Videografía", "Video continuo 1h", 120.00)
    ) else emptyList()

    val revistas = if (categoria == "15 años") listOf(
        ExtraOption("Revista 20 páginas Extra", "Diseño e impresión de revista", "Impresión", "Revista 20 pág", 140.00)
    ) else emptyList()

    // --- El orden del catálogo ---
    val ramas = listOf(
        Rama(
            "Fotos extra", "📸",
            listOf(
                Subrama("Digitales", fotosDigitales),
                Subrama("Impresas", fotosImpresas)
            )
        ),
        Rama(
            "Álbumes y FotoBooks", "📖",
            listOf(
                hoja(
                    null,
                    ExtraOption("Álbum 8x12 personalizado", "Álbum impreso en papel foto", "Álbum", "8x12", 10.00),
                    ExtraOption("FotoBook 8x12 (20 fotos)", "FotoBook personalizado para 20 fotos", "FotoBook", "8x12 (20 fotos)", 16.40)
                )
            )
        ),
        Rama(
            "Ampliaciones con marco", "🖼️",
            listOf(
                hoja(
                    null,
                    ExtraOption("Ampliación 16x24 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "16x24 con marco", 51.50),
                    ExtraOption("Ampliación 24x32 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "24x32 con marco", 67.50),
                    ExtraOption("Ampliación 24x39 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "24x39 con marco", 74.50),
                    ExtraOption("Super Ampliación 39x82.67 con marco", "Super formato impreso con marco", "Ampliaciones", "39x82.67 con marco", 175.00)
                )
            )
        ),
        Rama("Vestuario", "👗", listOf(Subrama(null, vestuario))),
        Rama("Maquillaje y peinado", "💄", listOf(Subrama(null, maquillaje))),
        Rama("Souvenirs", "🎁", listOf(Subrama(null, souvenirs))),
        Rama("Video", "🎬", listOf(Subrama(null, video))),
        Rama("Revistas", "📰", listOf(Subrama(null, revistas)))
    )

    // Una rama sin nada dentro no se enseña: en primer año no hay vídeo, y en
    // bodas no hay souvenirs.
    return ramas.filter { it.opciones.isNotEmpty() }
}

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun OfferExtrasDialog(
    item: CatalogItem,
    // Total del pedido completo, para que se vea crecer con cada toque.
    totalPedido: Double,
    cupLabelFor: (Double) -> String?,
    // Cuántas unidades de este extra hay ya en el pedido. Sale del carrito,
    // que es la única verdad: así el número de la ficha nunca se desfasa.
    cantidadEnPedido: (ExtraOption) -> Int,
    onAgregar: (ExtraOption) -> Unit,
    onQuitar: (ExtraOption) -> Unit,
    onDismiss: () -> Unit
) {
    val ramas = remember(item.category) { ramasPara(item.category) }
    // Solo una carpeta abierta a la vez: al abrir otra, la anterior se cierra
    // y la lista se recoloca sola. Es lo que da la sensación de que las
    // carpetas se apilan y se desplazan.
    var abierta by rememberSaveable { mutableStateOf<String?>(ramas.firstOrNull()?.nombre) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ---- Cabecera con el total que va subiendo ----
                CabeceraExtras(
                    nombrePaquete = item.code.ifBlank { item.name },
                    total = totalPedido,
                    equivalencia = cupLabelFor(totalPedido),
                    onDismiss = onDismiss
                )

                // ---- Las carpetas ----
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(ramas, key = { _, r -> r.nombre }) { _, rama ->
                        CarpetaRama(
                            rama = rama,
                            abierta = abierta == rama.nombre,
                            cantidadEnPedido = cantidadEnPedido,
                            onAbrir = {
                                abierta = if (abierta == rama.nombre) null else rama.nombre
                            },
                            onAgregar = onAgregar,
                            onQuitar = onQuitar
                        )
                    }
                }

                // ---- Cerrar ----
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .height(54.dp)
                            .testTag("btn_listo_extras"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Listo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/** Barra de arriba: el paquete y el total, que sube solo al ir tocando. */
@Composable
private fun CabeceraExtras(
    nombrePaquete: String,
    total: Double,
    equivalencia: String?,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agregar algo más",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "sobre $nombrePaquete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TOTAL DEL PEDIDO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                // El importe entra desde abajo cada vez que cambia: se ve subir.
                AnimatedContent(
                    targetState = total,
                    transitionSpec = {
                        val sube = targetState > initialState
                        val desde = if (sube) 1 else -1
                        (
                            slideInVertically(tween(260, easing = FastOutSlowInEasing)) {
                                alto -> desde * alto
                            } + fadeIn(tween(200))
                            ) togetherWith (
                            slideOutVertically(tween(260, easing = FastOutSlowInEasing)) {
                                alto -> -desde * alto
                            } + fadeOut(tween(140))
                            )
                    },
                    label = "total_extras"
                ) { valor ->
                    Text(
                        text = "$${String.format("%.2f", valor)} USD",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }

            if (equivalencia != null) {
                Text(
                    text = equivalencia,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/** Una carpeta: su nombre queda siempre visible, abierta o cerrada. */
@Composable
private fun CarpetaRama(
    rama: Rama,
    abierta: Boolean,
    cantidadEnPedido: (ExtraOption) -> Int,
    onAbrir: () -> Unit,
    onAgregar: (ExtraOption) -> Unit,
    onQuitar: (ExtraOption) -> Unit
) {
    val enPedido = rama.opciones.sumOf { cantidadEnPedido(it) }

    val giro by animateFloatAsState(
        targetValue = if (abierta) 90f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "giro_carpeta"
    )
    val fondo by animateColorAsState(
        targetValue = if (abierta) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(240),
        label = "fondo_carpeta"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = if (abierta) 6.dp else 1.dp)
    ) {
        // Cabecera de la carpeta: se queda a la vista aunque esté abierta, para
        // no perder de dónde cuelga lo que se está viendo.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAbrir() }
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag("rama_${rama.nombre}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = rama.simbolo, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = rama.nombre,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (enPedido > 0) {
                Text(
                    text = enPedido.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (abierta) "Cerrar" else "Abrir",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = giro }
            )
        }

        // El contenido se despliega hacia abajo, como una carpeta que se abre.
        AnimatedVisibility(
            visible = abierta,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rama.subramas.forEach { sub ->
                    if (sub.opciones.isEmpty()) return@forEach
                    if (sub.nombre != null) {
                        Text(
                            text = "└  ${sub.nombre}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    sub.opciones.forEach { opcion ->
                        FilaExtra(
                            opcion = opcion,
                            cantidad = cantidadEnPedido(opcion),
                            sangrado = sub.nombre != null,
                            onAgregar = { onAgregar(opcion) },
                            onQuitar = { onQuitar(opcion) }
                        )
                    }
                }
            }
        }
    }
}

/** Una cosa concreta. Un toque la añade; no hay botón aparte. */
@Composable
private fun FilaExtra(
    opcion: ExtraOption,
    cantidad: Int,
    sangrado: Boolean,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    val elegida = cantidad > 0
    val fondo by animateColorAsState(
        targetValue = if (elegida) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "fondo_extra"
    )
    val textoColor = if (elegida) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (sangrado) 14.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .clickable { onAgregar() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = opcion.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = textoColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = opcion.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (elegida) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${String.format("%.2f", opcion.price)} USD",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (elegida) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (elegida) {
            // Solo aparece cuando ya hay algo puesto, para poder deshacer un
            // toque de más. No es un botón de añadir: eso lo hace la fila.
            Text(
                text = "−",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f))
                    .clickable { onQuitar() }
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .testTag("quitar_${opcion.variantName}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "×$cantidad",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Tocar para añadir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
