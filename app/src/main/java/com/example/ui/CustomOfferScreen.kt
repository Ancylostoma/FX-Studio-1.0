package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomOfferScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Crea tu Oferta a la Medida",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elige fotos al momento, álbumes, ampliaciones, vestuario, maquillaje o video por separado. Se sumarán automáticamente a tu carrito.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1. Fotos al momento
        item {
            FotosAlMomentoSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Fotos impresas o digitales al momento")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 2. Álbumes
        item {
            AlbumSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Impreso en papel foto personalizado (no incluye fotos)")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. FotoBook
        item {
            FotoBookSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "FotoBook personalizado (no incluye fotos)")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 4. Ampliaciones
        item {
            AmpliacionesSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Ampliación de alta definición")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 5. Maquillaje y Peinado
        item {
            MaquillajeSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Servicio profesional Jezabelleza")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 6. Alquiler de Vestuario
        item {
            VestuarioSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Alquiler de vestuario para sesión")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 7. Servicio de Impresión
        item {
            ImpresionSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Servicio de impresión fotográfica")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 8. Videografía
        item {
            VideografiaSection(
                onAddToCart = { title, cat, variant, price, qty ->
                    viewModel.addCustomToCart(title, cat, variant, price, qty, "Videografía profesional 4K 60fps")
                    Toast.makeText(context, "Agregado: $title ($variant)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Studio disclaimer note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Nota: FXestudio se reserva el derecho de cambiar estos precios sin previo aviso. Todos los precios están expresados en USD (aceptamos Zelle y CUP al cambio del día).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// 1. Fotos al momento
// ----------------------------------------------------------------------
@Composable
private fun FotosAlMomentoSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val themes = listOf(
        "Meses" to listOf("Digital editada" to 4.02, "5x7 / 6x8" to 4.50, "8x10 / 8x12" to 5.27),
        "Primer Año (1-4 años)" to listOf("Digital editada" to 4.30, "5x7 / 6x8" to 4.70, "8x10 / 8x12" to 5.60),
        "PreQuinces (5-14 años)" to listOf("Digital editada" to 4.60, "5x7 / 6x8" to 5.00, "8x10 / 8x12" to 5.80),
        "Embarazadas" to listOf("Digital editada" to 5.10, "5x7 / 6x8" to 5.40, "8x10 / 8x12" to 6.00),
        "Bodas" to listOf("Digital editada" to 5.40, "5x7 / 6x8" to 5.80, "8x10 / 8x12" to 6.30),
        "15 años en adelante" to listOf("Digital editada" to 5.70, "5x7 / 6x8" to 6.16, "8x10 / 8x12" to 6.70)
    )

    var selectedThemeIndex by remember { mutableStateOf(0) }
    var selectedFormatIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val currentTheme = themes[selectedThemeIndex]
    val currentFormat = currentTheme.second[selectedFormatIndex]
    val totalPrice = currentFormat.second * qty

    CustomSectionCard(
        title = "Fotos al Momento",
        subtitle = "Por tema, ¡te la llevas impresa al momento!",
        icon = Icons.Default.CameraAlt
    ) {
        Text("1. Selecciona el Tema:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))

        // Theme chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.take(3).forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedThemeIndex == index,
                    onClick = { selectedThemeIndex = index },
                    label = { Text(pair.first, style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.drop(3).forEachIndexed { idx, pair ->
                val actualIndex = idx + 3
                FilterChip(
                    selected = selectedThemeIndex == actualIndex,
                    onClick = { selectedThemeIndex = actualIndex },
                    label = { Text(pair.first, style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("2. Formato de Foto:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentTheme.second.forEachIndexed { fIndex, formatPair ->
                FilterChip(
                    selected = selectedFormatIndex == fIndex,
                    onClick = { selectedFormatIndex = fIndex },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatPair.first, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Text("$${String.format("%.2f", formatPair.second)}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = currentFormat.second,
            totalPrice = totalPrice,
            onAdd = {
                onAddToCart(
                    "Foto al Momento - ${currentTheme.first}",
                    "Fotos al Momento",
                    currentFormat.first,
                    currentFormat.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 2. Álbum
// ----------------------------------------------------------------------
@Composable
private fun AlbumSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val formats = listOf(
        "6x8" to 6.00,
        "8x10" to 10.00,
        "8x12" to 10.00,
        "10x15" to 13.00,
        "12x18" to 16.00
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val selected = formats[selectedIndex]

    CustomSectionCard(
        title = "Álbum Personalizado",
        subtitle = "Impreso en papel foto personalizado (precio no incluye las fotos)",
        icon = Icons.Default.Book
    ) {
        Text("Selecciona el Formato:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formats.forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pair.first, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Text("$${String.format("%.2f", pair.second)}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = selected.second,
            totalPrice = selected.second * qty,
            onAdd = {
                onAddToCart(
                    "Álbum Personalizado ${selected.first}",
                    "Álbumes",
                    selected.first,
                    selected.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 3. FotoBook
// ----------------------------------------------------------------------
@Composable
private fun FotoBookSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val formats = listOf(
        "6x8" to listOf("10 fotos" to 6.00, "20 fotos" to 9.00),
        "8x10" to listOf("10 fotos" to 8.50, "20 fotos" to 15.40),
        "8x12" to listOf("10 fotos" to 9.00, "20 fotos" to 16.40)
    )
    var selectedFormatIndex by remember { mutableStateOf(0) }
    var selectedPhotosIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val format = formats[selectedFormatIndex]
    val option = format.second[selectedPhotosIndex]

    CustomSectionCard(
        title = "FotoBook",
        subtitle = "Encuadernación tipo revista de lujo (precio no incluye fotos)",
        icon = Icons.Default.CollectionsBookmark
    ) {
        Text("Formato y Capacidad:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formats.forEachIndexed { fIdx, fPair ->
                FilterChip(
                    selected = selectedFormatIndex == fIdx,
                    onClick = { selectedFormatIndex = fIdx },
                    label = { Text(fPair.first, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            format.second.forEachIndexed { pIdx, pPair ->
                FilterChip(
                    selected = selectedPhotosIndex == pIdx,
                    onClick = { selectedPhotosIndex = pIdx },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(pPair.first, style = MaterialTheme.typography.bodyLarge)
                            Text("$${String.format("%.2f", pPair.second)}", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = option.second,
            totalPrice = option.second * qty,
            onAdd = {
                onAddToCart(
                    "FotoBook ${format.first} (${option.first})",
                    "FotoBook",
                    "${format.first} - ${option.first}",
                    option.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 4. Ampliaciones
// ----------------------------------------------------------------------
@Composable
private fun AmpliacionesSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val sizes = listOf(
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

    var selectedSizeIndex by remember { mutableStateOf(1) } // 12x18 default
    var isImpresaConMarco by remember { mutableStateOf(true) }
    var qty by remember { mutableStateOf(1) }

    val currentSize = sizes[selectedSizeIndex]
    val price = if (isImpresaConMarco) currentSize.second.second else currentSize.second.first
    val variantName = if (isImpresaConMarco) "Impresa + marco" else "Digital editada"

    CustomSectionCard(
        title = "Ampliaciones",
        subtitle = "Desde 12x16 hasta 39x82.67 pulgadas en soporte digital o enmarcada",
        icon = Icons.Default.CropOriginal
    ) {
        Text("1. Tamaño de Ampliación:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))

        // Grid/Row of sizes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sizes.take(5).forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedSizeIndex == index,
                    onClick = { selectedSizeIndex = index },
                    label = { Text(pair.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sizes.drop(5).forEachIndexed { idx, pair ->
                val actualIndex = idx + 5
                FilterChip(
                    selected = selectedSizeIndex == actualIndex,
                    onClick = { selectedSizeIndex = actualIndex },
                    label = { Text(pair.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("2. Tipo de Acabado:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isImpresaConMarco,
                onClick = { isImpresaConMarco = true },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Impresa + Marco", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text("$${String.format("%.2f", currentSize.second.second)}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                    }
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = !isImpresaConMarco,
                onClick = { isImpresaConMarco = false },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Digital Editada", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text("$${String.format("%.2f", currentSize.second.first)}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = price,
            totalPrice = price * qty,
            onAdd = {
                onAddToCart(
                    "Ampliación ${currentSize.first} ($variantName)",
                    "Ampliaciones",
                    "${currentSize.first} - $variantName",
                    price,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 5. Maquillaje y Peinado
// ----------------------------------------------------------------------
@Composable
private fun MaquillajeSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val options = listOf(
        "Mamá (fotos primer año)" to 5.00,
        "Embarazadas" to 5.00,
        "Prequinces" to 2.00,
        "Bodas (incluye pestañas)" to 10.00,
        "Quinces (incluye pestañas)" to 15.00,
        "Acompañantes" to 5.00
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val selected = options[selectedIndex]

    CustomSectionCard(
        title = "Maquillaje y Peinado (Jezabelleza)",
        subtitle = "Estilismo profesional para resaltar en cada toma fotográfica",
        icon = Icons.Default.Face
    ) {
        Text("Selecciona el Servicio:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.chunked(2).forEach { rowList ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowList.forEach { pair ->
                        val index = options.indexOf(pair)
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pair.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                                    Text("$${String.format("%.2f", pair.second)}", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = selected.second,
            totalPrice = selected.second * qty,
            onAdd = {
                onAddToCart(
                    "Maquillaje - ${selected.first}",
                    "Maquillaje",
                    selected.first,
                    selected.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 6. Alquiler de Vestuario
// ----------------------------------------------------------------------
@Composable
private fun VestuarioSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val vestuarios = listOf(
        "Niños (batas, disfraces, trajecitos)" to 2.00,
        "Embarazadas" to 5.00,
        "Trajes para hombre" to 5.00,
        "Vestido sencillo" to 2.00,
        "Vestidos de 15 con aro" to 5.00
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val selected = vestuarios[selectedIndex]

    CustomSectionCard(
        title = "Alquiler de Vestuario",
        subtitle = "Disfraces, vestidos de gala, trajes y atuendos especiales",
        icon = Icons.Default.Checkroom
    ) {
        Text("Selecciona el Vestuario:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            vestuarios.forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair.first, style = MaterialTheme.typography.bodyLarge)
                            Text("$${String.format("%.2f", pair.second)}", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = selected.second,
            totalPrice = selected.second * qty,
            onAdd = {
                onAddToCart(
                    "Alquiler - ${selected.first}",
                    "Vestuario",
                    selected.first,
                    selected.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 7. Servicio de Impresión
// ----------------------------------------------------------------------
@Composable
private fun ImpresionSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val formats = listOf(
        "4x6" to 1.50,
        "5x7 / 6x8" to 1.70,
        "8x10 / 8x12" to 2.20
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    val selected = formats[selectedIndex]

    CustomSectionCard(
        title = "Servicio de Impresión",
        subtitle = "Impresión fotográfica de alta resolución en papel profesional",
        icon = Icons.Default.Print
    ) {
        Text("Tamaño de Impresión:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formats.forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pair.first, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text("$${String.format("%.2f", pair.second)}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = selected.second,
            totalPrice = selected.second * qty,
            onAdd = {
                onAddToCart(
                    "Impresión Fotográfica ${selected.first}",
                    "Impresión",
                    selected.first,
                    selected.second,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// 8. Videografía
// ----------------------------------------------------------------------
@Composable
private fun VideografiaSection(
    onAddToCart: (title: String, category: String, variant: String, price: Double, qty: Int) -> Unit
) {
    val services = listOf(
        "Makin Off (hasta 10 min, 4K 60fps)" to 40.00,
        "Video continuo de 1 hora (4K 60fps)" to 120.00
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var sinEdicion by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf(1) }

    val base = services[selectedIndex]
    val finalPrice = if (sinEdicion) base.second / 2.0 else base.second
    val variantStr = if (sinEdicion) "Solo filmación sin editar (50% desc.)" else "Editado 4K 60fps"

    CustomSectionCard(
        title = "Videografía (Bodas y 15 Años)",
        subtitle = "Producción audiovisual cinematográfica en 4K 60fps",
        icon = Icons.Default.Videocam
    ) {
        Text("Servicio de Video:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            services.forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair.first, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("$${String.format("%.2f", pair.second)}", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // No-edit discount switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (sinEdicion) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Solo filmación sin edición (50% de descuento)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Nota: Si el cliente solo quiere la filmación sin edición, el precio es la mitad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sinEdicion,
                    onCheckedChange = { sinEdicion = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        QuantityAndAddRow(
            qty = qty,
            onQtyChange = { qty = it },
            unitPrice = finalPrice,
            totalPrice = finalPrice * qty,
            onAdd = {
                onAddToCart(
                    "Videografía - ${base.first}",
                    "Videografía",
                    variantStr,
                    finalPrice,
                    qty
                )
            }
        )
    }
}

// ----------------------------------------------------------------------
// Common Helper Cards & Controls
// ----------------------------------------------------------------------
@Composable
private fun CustomSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            content()
        }
    }
}

@Composable
private fun QuantityAndAddRow(
    qty: Int,
    onQtyChange: (Int) -> Unit,
    unitPrice: Double,
    totalPrice: Double,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Price tag
        Column {
            Text(
                text = "Precio Unitario: $${String.format("%.2f", unitPrice)} USD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Total: $${String.format("%.2f", totalPrice)} USD",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quantity buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { if (qty > 1) onQtyChange(qty - 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Text(
                    text = "$qty",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onQtyChange(qty + 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(18.dp))
                }
            }

            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Añadir",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
