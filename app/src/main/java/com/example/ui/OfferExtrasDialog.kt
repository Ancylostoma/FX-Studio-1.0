package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CatalogItem

data class ExtraOption(
    val title: String,
    val description: String,
    val category: String,
    val variantName: String,
    val price: Double
)

@Composable
fun OfferExtrasDialog(
    item: CatalogItem,
    onDismiss: () -> Unit,
    onAddExtra: (title: String, category: String, variantName: String, price: Double, quantity: Int) -> Unit
) {
    // Determine category-specific and universal extras
    val availableExtras = remember(item.category) {
        val list = mutableListOf<ExtraOption>()

        when (item.category) {
            "Primer Año" -> {
                list.add(ExtraOption("Foto Extra Primer Año (Digital)", "Foto digital editada adicional", "Primer Año", "Digital editada", 4.30))
                list.add(ExtraOption("Foto Extra Primer Año (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "Primer Año", "5x7 / 6x8", 4.70))
                list.add(ExtraOption("Foto Extra Primer Año (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "Primer Año", "8x10 / 8x12", 5.60))
                list.add(ExtraOption("Cambio de ropa adicional", "Batas, disfraces o trajecitos para niños", "Vestuario", "Niños (alquiler)", 2.00))
                list.add(ExtraOption("Maquillaje Mamá Extra", "Maquillaje para sesión primer año", "Maquillaje", "Mamá", 5.00))
                list.add(ExtraOption("Maquillaje Acompañante", "Maquillaje y peinado adicional", "Maquillaje", "Acompañante", 5.00))
                list.add(ExtraOption("Taza Personalizada Extra", "Souvenir fotográfico", "Souvenirs", "Taza sublimada", 6.00))
                list.add(ExtraOption("Pullover Personalizado Extra", "Pullover con foto impresa", "Souvenirs", "Pullover", 8.00))
                list.add(ExtraOption("Llavero Personalizado Extra", "Llavero acrílico con foto", "Souvenirs", "Llavero", 2.50))
            }
            "Bodas" -> {
                list.add(ExtraOption("Foto Extra Bodas (Digital)", "Foto digital editada adicional", "Bodas", "Digital editada", 5.40))
                list.add(ExtraOption("Foto Extra Bodas (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "Bodas", "5x7 / 6x8", 5.80))
                list.add(ExtraOption("Foto Extra Bodas (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "Bodas", "8x10 / 8x12", 6.30))
                list.add(ExtraOption("Maquillaje Bodas Extra (con pestañas)", "Maquillaje profesional con pestañas", "Maquillaje", "Bodas", 10.00))
                list.add(ExtraOption("Maquillaje Acompañantes / Damas", "Maquillaje y peinado para damas", "Maquillaje", "Acompañantes", 5.00))
                list.add(ExtraOption("Alquiler Traje para Hombre", "Traje formal de novio o caballero", "Vestuario", "Trajes hombre", 5.00))
                list.add(ExtraOption("Alquiler Vestido de Novia Adicional", "Cambio de vestido de novia", "Vestuario", "Vestido novia", 10.00))
                list.add(ExtraOption("Videografía Makin Off (hasta 10 min 4K)", "Edición completa en 4K 60fps", "Videografía", "Makin Off editado", 40.00))
                list.add(ExtraOption("Video Continuo 1 hora 4K", "Cobertura continua editada", "Videografía", "Video continuo 1h", 120.00))
            }
            "15 años" -> {
                list.add(ExtraOption("Foto Extra 15 años (Digital)", "Foto digital editada adicional", "15 años", "Digital editada", 5.70))
                list.add(ExtraOption("Foto Extra 15 años (5x7 / 6x8)", "Foto impresa 5x7 o 6x8", "15 años", "5x7 / 6x8", 6.16))
                list.add(ExtraOption("Foto Extra 15 años (8x10 / 8x12)", "Foto impresa 8x10 o 8x12", "15 años", "8x10 / 8x12", 6.70))
                list.add(ExtraOption("Vestido de 15 con Aro Adicional", "Alquiler de vestido de gala con aro", "Vestuario", "Vestido de 15 con aro", 5.00))
                list.add(ExtraOption("Vestido Sencillo Adicional", "Alquiler vestido casual", "Vestuario", "Vestido sencillo", 2.00))
                list.add(ExtraOption("Maquillaje Quinces Extra (con pestañas)", "Maquillaje y peinado Jezabelleza", "Maquillaje", "Quinces", 15.00))
                list.add(ExtraOption("Revista 20 páginas Extra", "Diseño e impresión de revista", "Impresión", "Revista 20 pág", 140.00))
                list.add(ExtraOption("Videografía Makin Off (hasta 10 min 4K)", "Edición completa en 4K 60fps", "Videografía", "Makin Off editado", 40.00))
                list.add(ExtraOption("Video Continuo 1 hora 4K", "Cobertura continua editada", "Videografía", "Video continuo 1h", 120.00))
            }
            else -> {
                list.add(ExtraOption("Foto Digital Editada", "Foto digital adicional en alta resolución", item.category, "Digital editada", 5.00))
                list.add(ExtraOption("Foto Impresa 5x7 / 6x8", "Impresión fotográfica", "Impresiones", "5x7 / 6x8", 4.70))
                list.add(ExtraOption("Foto Impresa 8x10 / 8x12", "Impresión fotográfica", "Impresiones", "8x10 / 8x12", 5.80))
            }
        }

        // Universal Ampliaciones & Albums
        list.add(ExtraOption("Ampliación 16x24 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "16x24 con marco", 51.50))
        list.add(ExtraOption("Ampliación 24x32 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "24x32 con marco", 67.50))
        list.add(ExtraOption("Ampliación 24x39 con marco", "Ampliación impresa montada en marco", "Ampliaciones", "24x39 con marco", 74.50))
        list.add(ExtraOption("Super Ampliación 39x82.67 con marco", "Super formato impreso con marco", "Ampliaciones", "39x82.67 con marco", 175.00))
        list.add(ExtraOption("Álbum 8x12 personalizado", "Álbum impreso en papel foto", "Álbum", "8x12", 10.00))
        list.add(ExtraOption("FotoBook 8x12 (20 fotos)", "FotoBook personalizado para 20 fotos", "FotoBook", "8x12 (20 fotos)", 16.40))

        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Agregar Extras a ${item.code.ifBlank { item.name }}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personaliza tu paquete con fotos, vestuario, ampliaciones o souvenirs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableExtras) { extra ->
                        var qty by remember { mutableStateOf(1) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = extra.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = extra.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format("%.2f", extra.price)} USD",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Quantity selector
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (qty > 1) qty-- },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                        Text(
                                            text = "$qty",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { qty++ },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onAddExtra(extra.title, extra.category, extra.variantName, extra.price, qty)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Añadir", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Listo / Cerrar", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
