package com.example.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CatalogItem
import com.example.data.CatalogVariant
import com.example.data.Medidas

/** Foto de muestra por categoría, o la del propio paquete si tiene una. */
@Composable
fun OfferPhoto(
    imageBytes: ByteArray?,
    category: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageBytes) {
        imageBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val fallback = remember(category) {
        when {
            category.contains("Primer Año", ignoreCase = true) -> R.drawable.cat_primer_ano
            category.contains("Bodas", ignoreCase = true) -> R.drawable.cat_bodas
            category.contains("15", ignoreCase = true) ||
                category.contains("quince", ignoreCase = true) -> R.drawable.cat_quince
            else -> R.drawable.cat_primer_ano
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = fallback),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/**
 * Fila de las tres categorías. La elegida queda resaltada y las otras
 * atenuadas, para que se vea claramente en qué sección se está.
 */
@Composable
fun CategoriaChips(
    seleccionada: String,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class Cat(val etiqueta: String, val valor: String, val icono: ImageVector)
    val cats = listOf(
        Cat("Bodas", "Bodas", Icons.Default.Favorite),
        Cat("Quince", "15 años", Icons.Default.Stars),
        Cat("1er Año", "Primer Año", Icons.Default.ChildCare)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cats.forEach { c ->
            val activa = c.valor == seleccionada
            MenuPrincipalBoton(
                titulo = c.etiqueta,
                icono = c.icono,
                atenuado = !activa,
                resaltado = activa,
                onClick = { onSeleccionar(c.valor) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Ofertas de una categoría en forma de lista sencilla: código, nombre y precio.
 * Sin detalles, para que el cliente compare de un vistazo y entre en la que
 * le interese.
 */
@Composable
fun OffersListScreen(
    categoria: String,
    items: List<CatalogItem>,
    cupLabelFor: (Double) -> String?,
    onAbrirOferta: (CatalogItem) -> Unit,
    onCambiarCategoria: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busqueda by remember { mutableStateOf("") }
    val visibles = remember(items, busqueda) {
        val q = busqueda.trim()
        if (q.isBlank()) items
        else items.filter {
            it.code.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(10.dp))
        CategoriaChips(seleccionada = categoria, onSeleccionar = onCambiarCategoria)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por código… (ej: B7)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { busqueda = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("catalog_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (visibles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay ofertas en esta categoría",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibles) { item ->
                    val precio = item.getVariants().minByOrNull { it.price }?.price ?: 0.0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAbrirOferta(item) }
                            .testTag("oferta_${item.code.ifBlank { item.id.toString() }}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.code.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = item.code,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name.substringAfter(" - ", item.name),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "desde $${String.format("%.2f", precio)} USD",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                cupLabelFor(precio)?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detalle de una oferta: foto grande, qué incluye, variantes con su precio y
 * el botón para añadirla al pedido, además del acceso a los extras.
 */
@Composable
fun OfferDetailScreen(
    item: CatalogItem,
    cupLabelFor: (Double) -> String?,
    onAddToCart: (CatalogItem, CatalogVariant, Int) -> Unit,
    onOpenExtras: () -> Unit,
    onVolver: () -> Unit,
    // Se llama al pulsar "Finalizar": lleva al calendario a poner la fecha.
    onFinalizar: () -> Unit,
    // true cuando se llegó aquí desde el calendario, con la fecha ya puesta.
    // Solo cambia el texto del botón, para que se entienda a dónde lleva.
    vinoDelCalendario: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val variants = remember(item) { item.getVariants() }
    val extras = remember(item) { item.getExtrasList() }
    var variantIndex by remember(item.id) { mutableStateOf(0) }
    var cantidad by remember(item.id) { mutableStateOf(1) }
    val variante = variants.getOrNull(variantIndex)
    // Mientras no se haya añadido, el botón dice "Agregar". Una vez la oferta
    // está completa pasa a decir "Finalizar" y lleva a agendar la cita.
    var yaAgregado by remember(item.id) { mutableStateOf(false) }
    // Medidas del paquete traducidas a pulgadas, para el cliente que las pide
    // en esa unidad. Salen del propio texto, no hay que teclearlas aparte.
    val medidas = remember(item) { Medidas.enPulgadas(item.name + " " + item.description) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OfferPhoto(
                imageBytes = item.imageBytes,
                category = item.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            FilledTonalButton(
                onClick = onVolver,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .testTag("btn_volver_detalle")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Volver")
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (item.code.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Paquete ${item.code}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (medidas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📏 Medidas de las fotos:",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                medidas.forEach { m ->
                    Text(
                        text = m,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (extras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✨ Incluye:",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                extras.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = e, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (variante != null) {
                Text(
                    text = "Elige el formato:",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(variants) { i, v ->
                        val sel = i == variantIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { variantIndex = i }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = v.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${String.format("%.2f", v.price)} USD",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                )
                                cupLabelFor(v.price)?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (sel) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Cantidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Cantidad:",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { if (cantidad > 1) cantidad-- },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Text("−", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = cantidad.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { cantidad++ },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Aumentar",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                val totalOferta = variante.price * cantidad

                Button(
                    onClick = {
                        if (yaAgregado) {
                            onFinalizar()
                        } else {
                            onAddToCart(item, variante, cantidad)
                            yaAgregado = true
                            Toast.makeText(
                                context,
                                "Agregado: ${item.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("btn_agregar_detalle"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (yaAgregado) Icons.Default.CalendarMonth
                        else Icons.Default.AddShoppingCart,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (yaAgregado) {
                                if (vinoDelCalendario) "Finalizar y volver a la fecha"
                                else "Finalizar y agendar la cita"
                            } else {
                                "Agregar  •  $${String.format("%.2f", totalOferta)} USD"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        // El precio también en la moneda del día, con la tasa
                        // que tenga puesta el estudio.
                        if (!yaAgregado) {
                            cupLabelFor(totalOferta)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                if (yaAgregado) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_seguir_viendo"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Seguir viendo paquetes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenExtras,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("btn_extras_detalle"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Agregar algo más",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Text(
                    text = "Sin variantes configuradas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
